package openqwoutt.textprocessor.backend.textprocessing.domain

/**
 * Thrown when an OpenAI-compatible chat completion HTTP call fails or returns an unusable body.
 * [providerId] identifies the upstream (e.g. `openrouter`, `nvidia`) for logging and API responses.
 */
class LlmCompletionException(
    message: String,
    val httpStatus: Int? = null,
    val providerRaw: String? = null,
    val providerId: String,
) : RuntimeException(message)
