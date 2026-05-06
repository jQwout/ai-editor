package openqwoutt.miniapp.textstyler.domain

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import openqwoutt.miniapp.textstyler.data.prompts.PromptTemplate
import openqwoutt.textprocessor.app.BuildConfig
import openqwoutt.textstyler.data.settings.AppSettings
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Inject
class TextProcessorUseCase(
    private val maxChars: Int = 3000,
    private val backendUrl: String = BuildConfig.AI_BACKEND_URL,
    settings: AppSettings? = null
) {
    private val effectiveBackendUrl = settings?.backendUrl?.takeIf { it.isNotBlank() } ?: backendUrl

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
            TextStylerResult.Success(sendToBackend(textWithTemplate, mode))
        }.getOrElse {
            TextStylerResult.OrchestratorFailed
        }
    }

    /**
     * Apply prompt template to text before sending to backend.
     */
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
}
