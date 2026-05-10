package openqwoutt.textprocessor.backend.textprocessing.application

data class ProcessTextSuccess(
    val result: String,
    val routingModelId: String?,
    val modeId: String?,
    val promptText: String?,
    val temperature: Double?,
)

sealed class ProcessTextOutcome {
    data class Ok(val value: ProcessTextSuccess) : ProcessTextOutcome()

    data class Err(
        val kind: ProcessTextErrorKind,
        val message: String,
        val cause: Throwable? = null,
        /** Identifies upstream LLM HTTP provider (e.g. `openrouter`, `nvidia`). */
        val providerId: String? = null,
        /** HTTP status from the LLM upstream when available. */
        val httpStatus: Int? = null,
        /** Raw response body from the LLM upstream (may be truncated in [ProcessTextUseCase]). */
        val providerRaw: String? = null,
        /** JVM stack trace or other long diagnostic for unexpected failures. */
        val detail: String? = null,
    ) : ProcessTextOutcome()
}

enum class ProcessTextErrorKind {
    OPENROUTER_DISABLED,
    BAD_REQUEST,
    REGISTRY_REQUIRED,
    UNKNOWN_MODE,
    MODEL_PROMPT_NOT_FOUND,
    AI_BACKEND_FAILED,
}
