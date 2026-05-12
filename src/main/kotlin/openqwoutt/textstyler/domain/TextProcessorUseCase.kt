package openqwoutt.miniapp.textstyler.domain

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import openqwoutt.miniapp.textstyler.data.prompts.PromptTemplate
import openqwoutt.textprocessor.app.BuildConfig
import openqwoutt.textstyler.data.settings.AppSettings
import openqwoutt.textstyler.data.settings.SettingsRepository
import openqwoutt.textstyler.network.AiApiClient
import openqwoutt.textstyler.network.AiApiException
import openqwoutt.textstyler.network.ChatMessage

class TextProcessorUseCase(
    private val settingsRepository: SettingsRepository,
    private val apiClient: AiApiClient,
    private val maxChars: Int = 3000,
    private val backendUrl: String = BuildConfig.AI_BACKEND_URL
) {

    /**
     * Stream text processing result as a Flow of tokens.
     * Returns the accumulated text when complete.
     */
    fun processTextStreaming(
        inputText: String,
        mode: StyleMode,
        template: PromptTemplate? = null
    ): Flow<StreamingResult> {
        if (inputText.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(StreamingResult.Error("Add text or paste screenshot OCR first"))
        }

        val cleanedText = cleanText(inputText)
        val textWithTemplate = applyTemplate(cleanedText, template)
        val settings = settingsRepository.load()

        if (settings.useBackend) {
            // Backend doesn't support streaming, fall back to non-streaming
            return kotlinx.coroutines.flow.flow {
                emit(StreamingResult.Started)
                try {
                    val result = sendToBackend(textWithTemplate, mode, settings.backendUrl)
                    emit(StreamingResult.Token(result))
                    emit(StreamingResult.Done(result))
                } catch (e: Exception) {
                    emit(StreamingResult.Error(e.message ?: "Could not process this text. Try again."))
                }
            }
        }

        val provider = settings.toAiProvider()
        val configuredModel = settings.effectiveModel()
        val apiKey = settings.apiKey

        if (provider.requiresApiKey && apiKey.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(
                StreamingResult.Error("Add ${provider.displayName} API key in Settings.")
            )
        }

        val model = if (provider.looksLikeOwnModel(configuredModel)) {
            configuredModel
        } else {
            Log.w(TAG, "Configured model '$configuredModel' does not look like a ${provider.displayName} model; falling back to ${provider.model}.")
            provider.model
        }

        Log.i(TAG, "Streaming from ${provider.displayName} model=$model")

        return apiClient.streamChatCompletion(
            provider = provider,
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = mode.prompt),
                ChatMessage(role = "user", content = textWithTemplate)
            ),
            apiKey = apiKey,
            temperature = mode.temperature
        ).onStart { emit(StreamingResult.Started) }
          .onEach { /* individual tokens emitted via map below */ }
          .map { token -> StreamingResult.Token(token) as StreamingResult }
          .onEach { /* handle completion below */ }
    }

    /**
     * Stream text processing with completion tracking.
     * Emits: Started, then N x Token, then Done.
     */
    fun processTextStreamingComplete(
        inputText: String,
        mode: StyleMode,
        template: PromptTemplate? = null
    ): Flow<StreamingResult> = processTextStreaming(inputText, mode, template)

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
        val settings = settingsRepository.load()

        return runCatching {
            val result = if (settings.useBackend) {
                sendToBackend(textWithTemplate, mode, settings.backendUrl)
            } else {
                sendToOpenAiCompatibleApi(textWithTemplate, mode)
            }
            TextStylerResult.Success(result)
        }.getOrElse { throwable ->
            Log.e(TAG, "Text processing failed", throwable)
            TextStylerResult.Failure(
                throwable.message ?: "Could not process this text. Try again."
            )
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


    private suspend fun sendToBackend(text: String, mode: StyleMode, configuredBackendUrl: String): String = withContext(Dispatchers.IO) {
        val effectiveBackendUrl = configuredBackendUrl.takeIf { it.isNotBlank() } ?: backendUrl
        val response = apiClient.processText(effectiveBackendUrl, text, mode.id)
        response.result
    }

    private suspend fun sendToOpenAiCompatibleApi(text: String, mode: StyleMode): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.load()
        val provider = settings.toAiProvider()
        val configuredModel = settings.effectiveModel()
        val apiKey = settings.apiKey

        if (provider.requiresApiKey && apiKey.isBlank()) {
            error("Add ${provider.displayName} API key in Settings.")
        }

        val model = if (provider.looksLikeOwnModel(configuredModel)) {
            configuredModel
        } else {
            Log.w(
                TAG,
                "Configured model '$configuredModel' does not look like a ${provider.displayName} model; " +
                    "falling back to ${provider.model}. Pick a matching model in Settings."
            )
            provider.model
        }

        Log.i(
            TAG,
            "Sending to ${provider.displayName} (${provider.baseUrl}) model=$model " +
                "apiKey=${maskKey(apiKey)} (len=${apiKey.length})"
        )

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

    private fun maskKey(key: String): String {
        if (key.length <= 8) return "***"
        return key.take(6) + "..." + key.takeLast(4)
    }

    companion object {
        private const val TAG = "TextProcessorUseCase"
    }
}
