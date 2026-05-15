package openqwoutt.textprocessor.backend.textprocessing.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.json.Json
import openqwoutt.textprocessor.backend.observability.recordLlmRequest
import openqwoutt.textprocessor.backend.shared.openrouter.ChatMessage
import openqwoutt.textprocessor.backend.shared.openrouter.OpenRouterChatRequest
import openqwoutt.textprocessor.backend.shared.openrouter.OpenRouterChatResponse
import openqwoutt.textprocessor.backend.textprocessing.application.LlmCompletionPort
import openqwoutt.textprocessor.backend.textprocessing.domain.LlmCompletionException
import openqwoutt.textprocessor.backend.textprocessing.domain.TextProcessingPolicy
import org.slf4j.LoggerFactory

/**
 * OpenAI-compatible `POST .../chat/completions` (OpenRouter, NVIDIA NIM, vLLM, etc.).
 */
class ChatCompletionsLlmAdapter(
    private val httpClient: HttpClient,
    private val chatCompletionsUrl: String,
    private val providerId: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val maxTokens: Int = 900,
    private val meterRegistry: MeterRegistry? = null,
) : LlmCompletionPort {
    private val logger = LoggerFactory.getLogger(ChatCompletionsLlmAdapter::class.java)

    override suspend fun complete(
        userText: String,
        taskPrompt: String,
        temperature: Double,
        routingModelId: String,
    ): String {
        val startNs = System.nanoTime()
        var success = false
        try {
            val response =
                httpClient.post(chatCompletionsUrl) {
                    setBody(
                        OpenRouterChatRequest(
                            model = routingModelId,
                            messages =
                                listOf(
                                    ChatMessage(
                                        role = "system",
                                        content = "${TextProcessingPolicy.BASE_SYSTEM_PROMPT}\n\nTask: $taskPrompt",
                                    ),
                                    ChatMessage(role = "user", content = userText),
                                ),
                            maxTokens = maxTokens,
                            temperature = temperature,
                        ),
                    )
                }

            val status = response.status
            val raw =
                response.body<ByteArray>()
                    .decodeToString()

            if (!status.isSuccess()) {
                throw LlmCompletionException(
                    message = "LLM upstream HTTP ${status.value} (provider=$providerId)",
                    httpStatus = status.value,
                    providerRaw = raw,
                    providerId = providerId,
                )
            }

            val decoded =
                runCatching { json.decodeFromString<OpenRouterChatResponse>(raw) }.getOrElse { e ->
                    throw LlmCompletionException(
                        message = "Could not parse LLM JSON (provider=$providerId): ${e.message}",
                        httpStatus = status.value,
                        providerRaw = raw,
                        providerId = providerId,
                    )
                }

            val text =
                decoded.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            if (text.isBlank()) {
                throw LlmCompletionException(
                    message = "LLM returned empty content (provider=$providerId).",
                    httpStatus = status.value,
                    providerRaw = raw,
                    providerId = providerId,
                )
            }
            val durationMs = (System.nanoTime() - startNs) / 1_000_000L
            logger.info(
                "text-process LLM ok provider={} model={} durationMs={}",
                providerId,
                routingModelId,
                durationMs,
            )
            success = true
            return text
        } catch (e: Exception) {
            val durationMs = (System.nanoTime() - startNs) / 1_000_000L
            logger.warn(
                "text-process LLM failed provider={} model={} durationMs={}: {}",
                providerId,
                routingModelId,
                durationMs,
                e.message,
            )
            throw e
        } finally {
            val durationMs = (System.nanoTime() - startNs) / 1_000_000L
            meterRegistry.recordLlmRequest(providerId, "text.process.complete", durationMs, success)
        }
    }
}
