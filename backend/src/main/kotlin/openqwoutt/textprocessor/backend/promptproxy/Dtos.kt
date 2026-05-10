package openqwoutt.textprocessor.backend.promptproxy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PromptProxyRequest(
    /** Free-form modification / style instructions for the model. */
    val style: String,
    /** User content to transform (input text). */
    val prompt: String,
    /** Desired language of the model output (e.g. `ru`, `en`). */
    val language: String,
    /**
     * OpenAI-compatible model id (OpenRouter slug, NVIDIA id, …).
     * If null or blank, uses the server default (`LLM_PROMPT_PROXY_MODEL` / provider default).
     */
    val model: String? = null,
)

@Serializable
data class PromptProxySuccessResponse(
    val result: String,
)

@Serializable
data class PromptProxyErrorEnvelope(
    val error: PromptProxyErrorDetail,
)

@Serializable
data class PromptProxyErrorDetail(
    val message: String,
    val provider: String? = null,
    val httpStatus: Int? = null,
    val providerBody: JsonElement? = null,
    val providerRaw: String? = null,
)
