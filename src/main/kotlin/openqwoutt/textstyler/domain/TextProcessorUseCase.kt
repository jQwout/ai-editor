package openqwoutt.miniapp.textstyler.domain

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import openqwoutt.miniapp.textstyler.data.prompts.PromptTemplate
import openqwoutt.textprocessor.app.BuildConfig
import openqwoutt.textstyler.data.settings.AiProvider
import openqwoutt.textstyler.data.settings.AppSettings
import openqwoutt.textstyler.network.AiApiClient
import openqwoutt.textstyler.network.ChatMessage

@Inject
class TextProcessorUseCase(
    private val maxChars: Int = 3000,
    private val backendUrl: String = BuildConfig.AI_BACKEND_URL,
    settings: AppSettings? = null
) {
    private val effectiveBackendUrl = settings?.backendUrl?.takeIf { it.isNotBlank() } ?: backendUrl
    private val settings_ = settings
    // Use singleton to avoid HttpClient leaks
    private val apiClient get() = AiApiClient

    suspend fun processText(
        inputText: String,
        mode: StyleMode,
        template: PromptTemplate? = null
    ): TextStylerResult {
        if (inputText.isBlank()) {
            return TextStylerResult.EmptyInput
        }

        val cleanedText = cleanText(inputText)
        val textWithTemplate = applyTemplate(cleanedText, template)
        
        return runCatching {
            val result = if (settings_?.useBackend == true) {
                sendToBackend(textWithTemplate, mode)
            } else {
                sendToOpenAiCompatibleApi(textWithTemplate, mode)
            }
            TextStylerResult.Success(result)
        }.getOrElse {
            TextStylerResult.OrchestratorFailed
        }
    }

    private fun applyTemplate(text: String, template: PromptTemplate?): String {
        return if (template != null) {
            "${template.prompt}$text"
        } else {
            text
        }
    }

    private fun cleanText(text: String): String {
        val cleaned = text
            .trim()
            .replace(Regex("\\s{2,}"), " ")
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")

        return cleaned.take(maxChars)
    }


    private suspend fun sendToBackend(text: String, mode: StyleMode): String = withContext(Dispatchers.IO) {
        val response = apiClient.processText(effectiveBackendUrl, text, mode.id)
        response.result
    }

    private suspend fun sendToOpenAiCompatibleApi(text: String, mode: StyleMode): String = withContext(Dispatchers.IO) {
        val provider = settings_?.toAiProvider() ?: AiProvider.NVIDIA
        val model = settings_?.effectiveModel() ?: provider.model
        val apiKey = settings_?.apiKey

        val response = apiClient.chatCompletion(
            provider = provider,
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = mode.prompt),
                ChatMessage(role = "user", content = text)
            ),
            apiKey = apiKey,
            temperature = mode.temperature
        )

        response.firstContent() ?: error("No response from API")
    }

    fun close() {
        apiClient.close()
    }
}
