package openqwoutt.textprocessor.backend.textprocessing.adapter.http

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ProcessTextRequestDto(
    val text: String,
    val mode: String,
    val model: String? = null,
    /** If true, include the resolved prompt + routing model in response. */
    val returnPrompt: Boolean? = null,
)

@Serializable
data class ProcessTextResponseDto(
    val result: String,
    val model: String? = null,
    val mode: String? = null,
    val promptText: String? = null,
    val temperature: Double? = null,
)

@Serializable
data class ErrorResponseDto(
    val error: String,
    /** Upstream LLM id, e.g. `openrouter`, `nvidia`. */
    val providerId: String? = null,
    val httpStatus: Int? = null,
    /** Raw HTTP body from the AI provider when available. */
    val providerRaw: String? = null,
    /** Parsed JSON of [providerRaw] when it is valid JSON. */
    val providerBody: JsonElement? = null,
    /** Stack trace or other diagnostic text for unexpected failures. */
    val detail: String? = null,
)
