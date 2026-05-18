package openqwoutt.textstyler.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.emit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import openqwoutt.textprocessor.app.BuildConfig
import openqwoutt.textstyler.data.settings.AiProvider
import java.io.IOException

/**
 * Ktor-based HTTP client for AI API calls.
 */
class AiApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d(TAG, message)
                    }
                }
                level = LogLevel.ALL
            }
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            retryOnException(maxRetries = 2, retryOnTimeout = true)
            exponentialDelay()
        }
        engine {
            config {
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    /**
     * Stream chat completion as a Flow of token strings.
     * Returns tokens as they arrive from the SSE stream.
     */
    fun streamChatCompletion(
        provider: AiProvider,
        model: String,
        messages: List<ChatMessage>,
        apiKey: String? = null,
        temperature: Double = 0.7,
        topP: Double = 0.8,
        maxTokens: Int = 1000
    ): Flow<String> = flow {
        require(!provider.requiresApiKey || !apiKey.isNullOrBlank()) {
            "API key is required for ${provider.displayName}"
        }

        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            maxTokens = maxTokens,
            temperature = temperature,
            topP = topP,
            stream = true
        )

        val response = client.post("${provider.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Text.EventStream)
            if (!apiKey.isNullOrBlank() && provider.requiresApiKey) {
                header("Authorization", "Bearer $apiKey")
            }
            setBody(request)
        }

        val channel = response.bodyAsChannel()
        val buffer = ByteArray(8192)
        var partialLine = ""

        while (!channel.isClosedForRead) {
            val bytesRead = channel.readAvailable(buffer)
            if (bytesRead > 0) {
                val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                val lines = (partialLine + chunk).split("\n")
                partialLine = lines.last()

                for (i in 0 until lines.size - 1) {
                    val line = lines[i]
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        if (data == "[DONE]") return@flow
                        parseSseData(data)?.let { token -> emit(token) }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseSseData(data: String): String? {
        return try {
            val chunk = json.decodeFromString<StreamingChoice>(data)
            chunk.delta?.content
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Send chat completion request to OpenAI-compatible API.
     */
    suspend fun chatCompletion(
        provider: AiProvider,
        model: String,
        messages: List<ChatMessage>,
        apiKey: String? = null,
        temperature: Double = 0.7,
        topP: Double = 0.8,
        maxTokens: Int = 1000
    ): ChatCompletionResponse {
        require(!provider.requiresApiKey || !apiKey.isNullOrBlank()) {
            "API key is required for ${provider.displayName}"
        }

        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            maxTokens = maxTokens,
            temperature = temperature,
            topP = topP,
            stream = false
        )

        val response = client.post("${provider.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            if (!apiKey.isNullOrBlank() && provider.requiresApiKey) {
                header("Authorization", "Bearer $apiKey")
            }
            setBody(request)
        }

        ensureSuccess(response.status.value, provider.displayName) { response.bodyAsText() }
        return response.body()
    }

    /**
     * Send request to local backend.
     */
    suspend fun processText(backendUrl: String, text: String, mode: String): BackendResponse {
        val response = client.post("$backendUrl/api/text/process") {
            contentType(ContentType.Application.Json)
            setBody(BackendRequest(text = text, mode = mode))
        }

        ensureSuccess(response.status.value, "Local backend") { response.bodyAsText() }
        return response.body()
    }

    /**
     * Fetch available models from backend.
     * Returns null if backend is unreachable (timeout or error).
     */
    suspend fun fetchModelsFromBackend(backendUrl: String): List<BackendModelInfo>? {
        return try {
            val response = client.get("$backendUrl/api/models") {
                accept(ContentType.Application.Json)
            }
            if (response.status.value == 200) {
                response.body<BackendModelsResponse>().models
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch models from backend: ${e.message}")
            null
        }
    }

    private suspend fun ensureSuccess(statusCode: Int, source: String, bodyProvider: suspend () -> String) {
        if (statusCode in 200..299) return

        val safeMessage = when (statusCode) {
            401, 403 -> "$source rejected the request (HTTP $statusCode). Check API key and selected model in Settings."
            404 -> "$source could not find the selected model. Pick a different model in Settings."
            429 -> "$source rate limit reached. Try again later."
            in 500..599 -> "$source is temporarily unavailable. Try again later."
            else -> "$source request failed with HTTP $statusCode."
        }
        val body = runCatching { bodyProvider() }.getOrDefault("")
        Log.w(TAG, "HTTP $statusCode from $source. Body: ${body.take(2000)}")
        throw AiApiException(safeMessage)
    }

    companion object {
        private const val TAG = "AiApiClient"
    }
}

class AiApiException(message: String) : IOException(message)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7,
    @SerialName("top_p")
    val topP: Double = 0.8,
    val stream: Boolean = false
)

@Serializable
data class StreamingChoice(
    val delta: StreamingDelta? = null
)

@Serializable
data class StreamingDelta(
    val content: String? = null
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList()
) {
    @Serializable
    data class Choice(
        val message: ChatMessage? = null
    )

    fun firstContent(): String? {
        return choices.firstOrNull()?.message?.content
    }
}

@Serializable
data class BackendRequest(
    val text: String,
    val mode: String
)

@Serializable
data class BackendResponse(
    val result: String
)

@Serializable
data class BackendModelsResponse(
    val models: List<BackendModelInfo>
)

@Serializable
data class BackendModelInfo(
    val id: String,
    val displayName: String,
    val provider: String
)
