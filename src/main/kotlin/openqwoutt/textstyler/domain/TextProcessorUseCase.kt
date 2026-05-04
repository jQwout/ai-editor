package openqwoutt.miniapp.textstyler.domain

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import openqwoutt.textstyler.data.settings.ApiMode
import openqwoutt.textstyler.data.settings.AppSettings

class TextProcessorUseCase(
    private val maxChars: Int = 3000,
    private val settings: AppSettings
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            engine {
                connectTimeout = 15_000
                socketTimeout = 60_000
            }
        }
    }

    suspend fun processText(inputText: String, mode: StyleMode): TextStylerResult {
        if (inputText.isBlank()) {
            return TextStylerResult.EmptyInput
        }

        val cleanedText = cleanText(inputText)
        return runCatching {
            TextStylerResult.Success(send(cleanedText, mode))
        }.getOrElse { throwable ->
            Log.e(TAG, "Processing failed", throwable)
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

    private suspend fun sendToBackend(text: String, mode: StyleMode): String {
        val endpoint = "${settings.backendUrl.trimEnd('/')}/api/text/process"
        Log.d(TAG, "-> POST $endpoint")

        val requestBody = buildJsonObject {
            put("text", text)
            put("mode", mode.id)
        }

        Log.d(TAG, "-> Request body: $requestBody")

        val response: JsonObject = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            setBody(requestBody)
        }.body()

        Log.d(TAG, "<- Response body: $response")

        return response["result"]?.toString()
            ?.trim('"')
            ?: error("Backend response missing 'result' field")
    }

    private suspend fun sendToOpenRouter(text: String, mode: StyleMode): String {
        val endpoint = "https://openrouter.ai/api/v1/chat/completions"
        Log.d(TAG, "-> POST $endpoint")
        Log.d(TAG, "-> Headers: Content-Type=application/json, Authorization=Bearer ***masked***, Model=${settings.model}")

        val requestBody = buildJsonObject {
            put("model", settings.model)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", buildSystemPrompt(mode))
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", text)
                })
            }
            put("max_tokens", 900)
            put("temperature", mode.temperature)
        }

        Log.d(TAG, "-> Request body: $requestBody")

        val response: JsonObject = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            header("Authorization", "Bearer ${settings.apiKey}")
            header("HTTP-Referer", "https://sideai.app")
            header("X-Title", "Side AI Editor")
            setBody(requestBody)
        }.body()

        Log.d(TAG, "<- Response body: $response")

        val choices = response["choices"]
            ?: error("No choices in response")

        val content = (choices as? kotlinx.serialization.json.JsonArray)
            ?.firstOrNull()
            ?.let { it as? kotlinx.serialization.json.JsonObject }
            ?.get("message")
            ?.let { it as? kotlinx.serialization.json.JsonObject }
            ?.get("content")
            ?.toString()
            ?.trim('"')
            ?.trim()
            ?: error("Invalid response structure")

        if (content.isBlank()) {
            error("OpenRouter returned empty content")
        }

        return content
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

    companion object {
        private const val TAG = "Network"
    }
}
