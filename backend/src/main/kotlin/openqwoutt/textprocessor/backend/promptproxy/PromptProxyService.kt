package openqwoutt.textprocessor.backend.promptproxy

import openqwoutt.textprocessor.backend.shared.openrouter.ChatMessage

class PromptProxyService(
    private val llm: PromptProxyLlmCompleter,
    /** Used when [PromptProxyRequest.model] is omitted or blank after trim. */
    private val defaultRoutingModel: String,
) {

    suspend fun run(request: PromptProxyRequest): PromptProxyRunOutcome {
        val styleTrim = request.style.trim()
        val promptTrim = request.prompt.trim()
        val languageTrim = request.language.trim()
        val routingModel =
            request.model?.trim()?.takeIf { it.isNotEmpty() }
                ?: defaultRoutingModel.trim().takeIf { it.isNotEmpty() }

        if (promptTrim.isBlank()) {
            return PromptProxyRunOutcome.Validation(
                PromptProxyErrorDetail(message = "`prompt` must not be blank.")
            )
        }
        if (languageTrim.isBlank()) {
            return PromptProxyRunOutcome.Validation(
                PromptProxyErrorDetail(message = "`language` must not be blank.")
            )
        }
        if (routingModel.isNullOrBlank()) {
            return PromptProxyRunOutcome.Validation(
                PromptProxyErrorDetail(
                    message =
                        "`model` must not be blank. Send a model id in the JSON body, " +
                            "or set LLM_PROMPT_PROXY_MODEL or the provider default model on the server."
                ),
            )
        }

        val systemContent = buildSystemMessage(styleTrim, languageTrim)

        val messages =
            listOf(
                ChatMessage(role = "system", content = systemContent),
                ChatMessage(role = "user", content = promptTrim),
            )

        val outcome =
            llm.completeChat(
                model = routingModel,
                messages = messages,
                temperature = DEFAULT_TEMPERATURE,
                maxTokens = DEFAULT_MAX_TOKENS,
            )

        return when (outcome) {
            is LlmAttemptResult.Success -> PromptProxyRunOutcome.Ok(outcome.text)
            is LlmAttemptResult.Failure ->
                PromptProxyRunOutcome.Provider(
                    PromptProxyErrorDetail(
                        message = outcome.message,
                        provider = outcome.provider,
                        httpStatus = outcome.httpStatus,
                        providerBody = outcome.providerBody,
                        providerRaw = outcome.providerRaw,
                    )
                )
        }
    }

    private companion object {
        const val DEFAULT_TEMPERATURE = 0.4
        const val DEFAULT_MAX_TOKENS = 2048
    }

    private fun buildSystemMessage(styleInstructions: String, language: String): String =
        buildString {
            appendLine("You help transform or rewrite user content according to clear instructions.")
            appendLine("Apply the user's modification/style instructions faithfully.")
            if (styleInstructions.isNotBlank()) {
                appendLine()
                appendLine("Modification / style instructions:")
                appendLine(styleInstructions.trim())
            }
            appendLine()
            appendLine("The output must be written in language: ${language.trim()}.")
            appendLine("Return only the final result. Do not mention these rules.")
        }
}

sealed interface PromptProxyRunOutcome {
    data class Ok(val text: String) : PromptProxyRunOutcome

    data class Validation(val detail: PromptProxyErrorDetail) : PromptProxyRunOutcome

    data class Provider(val detail: PromptProxyErrorDetail) : PromptProxyRunOutcome
}
