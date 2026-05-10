package openqwoutt.textprocessor.backend.textprocessing.application

import openqwoutt.textprocessor.backend.textprocessing.domain.LlmCompletionException
import openqwoutt.textprocessor.backend.textprocessing.domain.StyleMode
import openqwoutt.textprocessor.backend.textprocessing.domain.TextProcessingPolicy

class ProcessTextUseCase(
    private val llm: LlmCompletionPort?,
    private val modelPromptCatalog: ModelPromptCatalog?,
    private val fallbackRoutingModelId: String,
) {
    suspend fun execute(command: ProcessTextCommand): ProcessTextOutcome {
        val llmPort = llm
            ?: return ProcessTextOutcome.Err(
                ProcessTextErrorKind.OPENROUTER_DISABLED,
                "LLM provider is disabled on this server (no API key / client not configured).",
            )

        val text = command.text.trim()
        if (text.isBlank()) {
            return ProcessTextOutcome.Err(ProcessTextErrorKind.BAD_REQUEST, "Text is required.")
        }

        val mode = StyleMode.fromId(command.modeId)
        if (mode == null) {
            return ProcessTextOutcome.Err(ProcessTextErrorKind.UNKNOWN_MODE, "Unknown mode.")
        }

        val requestedModel = command.explicitModelId?.trim()?.takeIf { it.isNotBlank() }
        if (requestedModel != null && modelPromptCatalog == null) {
            return ProcessTextOutcome.Err(
                ProcessTextErrorKind.REGISTRY_REQUIRED,
                "Prompt registry is disabled on this server. Omit \"model\" or set REPO_INDEX_ENABLED=true.",
            )
        }

        val taskPrompt: String
        val temperature: Double
        val routingModel: String
        when {
            requestedModel != null -> {
                val resolved = checkNotNull(modelPromptCatalog).resolve(requestedModel, mode.id)
                if (resolved == null) {
                    return ProcessTextOutcome.Err(
                        ProcessTextErrorKind.MODEL_PROMPT_NOT_FOUND,
                        "Unknown/disabled model or missing prompt for this mode.",
                    )
                }
                taskPrompt = resolved.promptText
                temperature = resolved.temperature
                routingModel = requestedModel
            }
            else -> {
                taskPrompt = mode.prompt
                temperature = mode.temperature
                routingModel = fallbackRoutingModelId
            }
        }

        val clipped = text.take(TextProcessingPolicy.MAX_INPUT_CHARS)

        return runCatching {
            llmPort.complete(
                userText = clipped,
                taskPrompt = taskPrompt,
                temperature = temperature,
                routingModelId = routingModel,
            )
        }.fold(
            onSuccess = { result ->
                ProcessTextOutcome.Ok(
                    ProcessTextSuccess(
                        result = result,
                        routingModelId = if (command.returnPromptDetails) routingModel else null,
                        modeId = if (command.returnPromptDetails) mode.id else null,
                        promptText = if (command.returnPromptDetails) taskPrompt else null,
                        temperature = if (command.returnPromptDetails) temperature else null,
                    ),
                )
            },
            onFailure = { err ->
                when (err) {
                    is LlmCompletionException ->
                        ProcessTextOutcome.Err(
                            kind = ProcessTextErrorKind.AI_BACKEND_FAILED,
                            message = err.message ?: "LLM request failed.",
                            cause = err,
                            providerId = err.providerId,
                            httpStatus = err.httpStatus,
                            providerRaw = ProcessTextErrorDiagnostics.truncateProviderRaw(err.providerRaw),
                            detail = null,
                        )
                    else ->
                        ProcessTextOutcome.Err(
                            kind = ProcessTextErrorKind.AI_BACKEND_FAILED,
                            message = err.message ?: (err::class.simpleName ?: "AI backend failed."),
                            cause = err,
                            httpStatus = null,
                            providerRaw = null,
                            detail = ProcessTextErrorDiagnostics.truncateProviderRaw(err.stackTraceToString()),
                        )
                }
            },
        )
    }
}
