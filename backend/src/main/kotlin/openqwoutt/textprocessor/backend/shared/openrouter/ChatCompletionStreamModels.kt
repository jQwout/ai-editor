package openqwoutt.textprocessor.backend.shared.openrouter

import kotlinx.serialization.Serializable

/** One SSE `data:` JSON line from `POST /chat/completions` with `stream: true`. */
@Serializable
data class ChatCompletionStreamChunk(
    val choices: List<ChatCompletionStreamChoice> = emptyList(),
)

@Serializable
data class ChatCompletionStreamChoice(
    val delta: ChatCompletionStreamDelta = ChatCompletionStreamDelta(),
)

@Serializable
data class ChatCompletionStreamDelta(
    /** Answer tokens (typical chat completion). */
    val content: String? = null,
    /** Some reasoning / thinking models stream visible reasoning separately from `content`. */
    val reasoning: String? = null,
    /** OpenAI-style refusal text when the model declines. */
    val refusal: String? = null,
)
