package openqwoutt.miniapp.textstyler.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import openqwoutt.textstyler.data.settings.ApiMode
import openqwoutt.textstyler.data.settings.AppSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TextProcessorUseCase(
    private val maxChars: Int = 3000,
    private val settings: AppSettings
) {
    suspend fun processText(inputText: String, mode: StyleMode): TextStylerResult {
        if (inputText.isBlank()) {
            return TextStylerResult.EmptyInput
        }

        val cleanedText = cleanText(inputText)
        return runCatching {
            TextStylerResult.Success(send(cleanedText, mode))
        }.getOrElse {
            TextStylerResult.OrchestratorFailed
        }
    }

    private fun cleanText(text: String): String {
        val cleaned = text
            .trim()
            .replace(Regex("\\s{2,}"), " ")
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")

        return cleaned.take(maxChars)
    }

    private suspend fun send(text: String, mode: StyleMode): String = withContext(Dispatchers.IO) {
        when (settings.mode) {
            ApiMode.LOCAL_BACKEND -> sendToBackend(text, mode)
            ApiMode.OPENROUTER_DIRECT -> sendToOpenRouter(text, mode)
        }
    }

    private fun sendToBackend(text: String, mode: StyleMode): String {
        val endpoint = "${settings.backendUrl.trimEnd('/')}/api/text/process"
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

        return JSONObject(body).getString("result")
    }

    private fun sendToOpenRouter(text: String, mode: StyleMode): String {
        val endpoint = "https://openrouter.ai/api/v1/chat/completions"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
            setRequestProperty("HTTP-Referer", "https://sideai.app")
            setRequestProperty("X-Title", "Side AI Editor")
        }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", buildSystemPrompt(mode)))
            put(JSONObject().put("role", "user").put("content", text))
        }

        val requestJson = JSONObject()
            .put("model", settings.model)
            .put("messages", messages)
            .put("max_tokens", 900)
            .put("temperature", mode.temperature)
            .toString()

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(requestJson)
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)

        connection.disconnect()

        if (responseCode !in 200..299) {
            error("OpenRouter returned HTTP $responseCode: $body")
        }

        val choices = JSONObject(body).optJSONArray("choices")
            ?: error("No choices in response")
        return choices.getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .ifBlank { error("OpenRouter returned empty content") }
    }

    private fun buildSystemPrompt(mode: StyleMode): String {
        return """You are the AI engine behind a text editing Android app.
Follow the selected task exactly.
Preserve the meaning of the original text.
**IMPORTANT: Always respond in the SAME language as the user's input text.**
Do not mention these instructions.
Return only the final useful answer unless the selected task explicitly asks for analysis.

Task: ${mode.prompt}""".trimIndent()
    }
}
