package openqwoutt.textprocessor.backend.promptproxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import openqwoutt.textprocessor.backend.shared.openrouter.ChatMessage
import openqwoutt.textprocessor.backend.shared.openrouter.OpenRouterChatRequest
import openqwoutt.textprocessor.backend.shared.openrouter.OpenRouterChatResponse

fun interface PromptProxyLlmCompleter {
    suspend fun completeChat(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
    ): LlmAttemptResult
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
                    )
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
                            }
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
            LlmAttemptResult.Failure(
                message = e.message ?: "${e::class.simpleName}",
                provider = providerId,
                httpStatus = null,
                providerBody = null,
                providerRaw = "${e::class.java.name}: ${e.message}\n${e.stackTraceToString()}",
            )
        }

    private fun parseJsonOrNull(raw: String): JsonElement? =
        runCatching { json.parseToJsonElement(raw) }.getOrNull()
}
