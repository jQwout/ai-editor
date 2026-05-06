package openqwoutt.miniapp.textstyler.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import openqwoutt.textprocessor.app.BuildConfig
import openqwoutt.textstyler.data.prompts.PromptTemplate
import openqwoutt.textstyler.data.settings.AiProvider
import openqwoutt.textstyler.data.settings.AppSettings
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TextProcessorUseCase(
    private val maxChars: Int = 3000,
    private val backendUrl: String = BuildConfig.AI_BACKEND_URL,
    settings: AppSettings? = null
) {
    private val effectiveBackendUrl = settings?.backendUrl?.takeIf { it.isNotBlank() } ?: backendUrl
    private val settings_ = settings

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
            val result = if (settings_?.aiProvider != null && settings_?.aiProvider != AiProvider.OPEN_ROUTER.name) {
                sendToOpenAiCompatibleApi(textWithTemplate, mode)
            } else {
                sendToBackend(textWithTemplate, mode)
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
        val endpoint = "${effectiveBackendUrl.trimEnd('/')}/api/text/process"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        val requestJson = JSONObject()
            .put("text", text)
            .put("mode", mode.id)
            .toString()

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(requestJson)
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)

        connection.disconnect()

        if (responseCode !in 200..299) {
            error("Backend returned HTTP $responseCode: $body")
        }

        JSONObject(body).getString("result")
    }

    private suspend fun sendToOpenAiCompatibleApi(text: String, mode: StyleMode): String = withContext(Dispatchers.IO) {
        val provider = settings_?.toAiProvider() ?: AiProvider.POLLINATIONS
        
        val connection = (URL("${provider.baseUrl}/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            
            if (provider.requiresApiKey && !settings_.apiKey.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer ${settings_.apiKey}")
            }
        }

        val messages = JSONObject()
            .put("role", "user")
            .put("content", buildPrompt(text, mode))
        
        val requestJson = JSONObject()
            .put("model", provider.model)
            .put("messages", arrayOf(messages))
            .put("max_tokens", 1000)
            .toString()

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(requestJson)
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)

        connection.disconnect()

        if (responseCode !in 200..299) {
            error("API returned HTTP $responseCode: $body")
        }

        val response = JSONObject(body)
        val choices = response.getJSONArray("choices")
        if (choices.length() == 0) {
            error("No response from API")
        }
        choices.getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun buildPrompt(text: String, mode: StyleMode): String {
        return when (mode.id) {
            "style" -> "Improve this text with better style, clarity and grammar: $text"
            "shorten" -> "Summarize this text concisely: $text"
            "expand" -> "Expand this text with more detail: $text"
            "translate" -> "Translate this text to the target language: $text"
            "analyze" -> "Analyze this text: $text"
            "screenshot" -> "Describe what's in this screenshot: $text"
            else -> "Process this text: $text"
        }
    }
}