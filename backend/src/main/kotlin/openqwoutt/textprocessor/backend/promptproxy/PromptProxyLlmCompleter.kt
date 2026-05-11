package openqwoutt.textprocessor.backend.promptproxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import openqwoutt.textprocessor.backend.shared.openrouter.ChatCompletionStreamChunk
import openqwoutt.textprocessor.backend.shared.openrouter.ChatCompletionStreamDelta
import openqwoutt.textprocessor.backend.shared.openrouter.ChatMessage
import openqwoutt.textprocessor.backend.shared.openrouter.OpenRouterChatRequest
import openqwoutt.textprocessor.backend.shared.openrouter.OpenRouterChatResponse
import org.slf4j.LoggerFactory

interface PromptProxyLlmCompleter {
    suspend fun completeChat(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
    ): LlmAttemptResult

    fun streamChat(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
    ): Flow<ProxyStreamFrame>
}

sealed interface ProxyStreamFrame {
    data class Delta(val text: String) : ProxyStreamFrame

    data class Failed(val detail: PromptProxyErrorDetail) : ProxyStreamFrame
}

sealed interface LlmAttemptResult {
    data class Success(val text: String) : LlmAttemptResult

    data class Failure(
        val message: String,
        val provider: String,
        val httpStatus: Int?,
        val providerBody: JsonElement?,
        val providerRaw: String?,
    ) : LlmAttemptResult
}

/** OpenAI-compatible `POST chat/completions` (OpenRouter, NVIDIA NIM, etc.). */
class ChatCompletionsPromptProxyCompleter(
    private val httpClient: HttpClient,
    private val json: Json,
    private val chatCompletionsUrl: String,
    private val providerId: String,
) : PromptProxyLlmCompleter {

    private val logger = LoggerFactory.getLogger(ChatCompletionsPromptProxyCompleter::class.java)

    override suspend fun completeChat(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
    ): LlmAttemptResult =
        try {
            val response = httpClient.post(chatCompletionsUrl) {
                setBody(
                    OpenRouterChatRequest(
                        model = model,
                        messages = messages,
                        maxTokens = maxTokens,
                        temperature = temperature,
                        stream = false,
                    ),
                )
            }
            val status = response.status
            val raw =
                response.body<ByteArray>()
                    .decodeToString()

            when {
                status.isSuccess() -> {
                    runCatching { json.decodeFromString(OpenRouterChatResponse.serializer(), raw) }
                        .fold(
                            onSuccess = { decoded ->
                                val text =
                                    decoded.choices.firstOrNull()?.message?.content?.trim().orEmpty()
                                if (text.isNotBlank()) {
                                    LlmAttemptResult.Success(text)
                                } else {
                                    LlmAttemptResult.Failure(
                                        message = "Model returned empty content.",
                                        provider = providerId,
                                        httpStatus = status.value,
                                        providerBody = parseJsonOrNull(raw),
                                        providerRaw = raw,
                                    )
                                }
                            },
                            onFailure = { err ->
                                LlmAttemptResult.Failure(
                                    message =
                                        "Could not decode chat/completions response (provider=$providerId): ${err.message}",
                                    provider = providerId,
                                    httpStatus = status.value,
                                    providerBody = null,
                                    providerRaw = raw,
                                )
                            },
                        )
                }
                else -> {
                    val element = parseJsonOrNull(raw)
                    LlmAttemptResult.Failure(
                        message = "LLM upstream HTTP ${status.value} (provider=$providerId).",
                        provider = providerId,
                        httpStatus = status.value,
                        providerBody = element,
                        providerRaw = raw,
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Prompt proxy completeChat failed (provider=$providerId)", e)
            LlmAttemptResult.Failure(
                message = e.message ?: "${e::class.simpleName}",
                provider = providerId,
                httpStatus = null,
                providerBody = null,
                providerRaw = null,
            )
        }

    override fun streamChat(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
    ): Flow<ProxyStreamFrame> =
        flow {
            try {
                val streamTimeout = streamRequestTimeoutMillis()
                val response =
                    httpClient.post(chatCompletionsUrl) {
                        timeout {
                            requestTimeoutMillis = streamTimeout
                            socketTimeoutMillis = streamTimeout
                        }
                        header(HttpHeaders.Accept, "text/event-stream")
                        setBody(
                            OpenRouterChatRequest(
                                model = model,
                                messages = messages,
                                maxTokens = maxTokens,
                                temperature = temperature,
                                stream = true,
                            ),
                        )
                    }
                val status = response.status
                if (!status.isSuccess()) {
                    val raw = response.body<ByteArray>().decodeToString()
                    emit(
                        ProxyStreamFrame.Failed(
                            PromptProxyErrorDetail(
                                message = "LLM upstream HTTP ${status.value} (provider=$providerId).",
                                provider = providerId,
                                httpStatus = status.value,
                                providerBody = parseJsonOrNull(raw),
                                providerRaw = raw,
                            ),
                        ),
                    )
                    return@flow
                }

                val channel = response.bodyAsChannel()
                var malformedStreak = 0
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val payload = extractSseDataPayload(trimmed) ?: continue
                    if (payload == "[DONE]") break
                    when (val parsed = parseStreamDataPayload(payload)) {
                        is ParsedStreamPayload.Delta -> {
                            malformedStreak = 0
                            emit(ProxyStreamFrame.Delta(parsed.text))
                        }
                        ParsedStreamPayload.EmptyDelta -> {
                            // Normal heartbeat / role-only chunk
                        }
                        ParsedStreamPayload.ParseError -> {
                            malformedStreak++
                            logger.warn(
                                "Prompt proxy stream: invalid SSE JSON line (provider=$providerId, streak=$malformedStreak): {}",
                                payload.take(500),
                            )
                            if (malformedStreak >= MAX_MALFORMED_SSE_LINES) {
                                emit(
                                    ProxyStreamFrame.Failed(
                                        PromptProxyErrorDetail(
                                            message =
                                                "Upstream stream repeated invalid JSON lines " +
                                                    "(provider=$providerId).",
                                            provider = providerId,
                                            httpStatus = null,
                                            providerBody = null,
                                            providerRaw = null,
                                        ),
                                    ),
                                )
                                return@flow
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Prompt proxy streamChat failed (provider=$providerId)", e)
                emit(
                    ProxyStreamFrame.Failed(
                        PromptProxyErrorDetail(
                            message = e.message ?: "${e::class.simpleName}",
                            provider = providerId,
                            httpStatus = null,
                            providerBody = null,
                            providerRaw = null,
                        ),
                    ),
                )
            }
        }

    private fun parseStreamDataPayload(jsonLine: String): ParsedStreamPayload {
        val chunk =
            runCatching { json.decodeFromString(ChatCompletionStreamChunk.serializer(), jsonLine) }
                .getOrNull()
                ?: return ParsedStreamPayload.ParseError
        val choice = chunk.choices.firstOrNull() ?: return ParsedStreamPayload.EmptyDelta
        val text = concatDeltaText(choice.delta)
        return if (text.isNotEmpty()) ParsedStreamPayload.Delta(text) else ParsedStreamPayload.EmptyDelta
    }

    private fun concatDeltaText(delta: ChatCompletionStreamDelta): String =
        buildString {
            delta.reasoning?.let { append(it) }
            delta.content?.let { append(it) }
            delta.refusal?.let { append(it) }
        }

    private fun extractSseDataPayload(line: String): String? {
        val t = line.trim()
        if (!t.startsWith("data:", ignoreCase = true)) return null
        return t.substring(5).trim().takeIf { it.isNotEmpty() }
    }

    private fun parseJsonOrNull(raw: String): JsonElement? =
        runCatching { json.parseToJsonElement(raw) }.getOrNull()

    private sealed interface ParsedStreamPayload {
        data class Delta(val text: String) : ParsedStreamPayload

        data object EmptyDelta : ParsedStreamPayload

        data object ParseError : ParsedStreamPayload
    }

    private companion object {
        const val MAX_MALFORMED_SSE_LINES = 5

        /** Long-lived SSE reads; overrides shorter client defaults from `createLlmHttpClient`. */
        fun streamRequestTimeoutMillis(): Long =
            System.getenv("LLM_HTTP_STREAM_REQUEST_TIMEOUT_MS")?.toLongOrNull()?.coerceAtLeast(10_000L)
                ?: 600_000L
    }
}
