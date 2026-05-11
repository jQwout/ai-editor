package openqwoutt.textprocessor.backend.shared.openrouter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Double,
    /** OpenAI-compatible streaming; when true the response is `text/event-stream` (SSE lines). */
    val stream: Boolean = false,
)

@Serializable
data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice> = emptyList(),
)

@Serializable
data class OpenRouterChoice(
    val message: ChatMessage,
)
