package openqwoutt.textstyler.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import openqwoutt.textstyler.data.settings.AiProvider

/**
 * Ktor-based HTTP client for AI API calls.
 * Singleton to avoid HttpClient leaks.
 */
object AiApiClient {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            config {
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    /**
     * Send chat completion request to OpenAI-compatible API.
     */
    suspend fun chatCompletion(
        provider: AiProvider,
        model: String,
        messages: List<ChatMessage>,
        apiKey: String? = null
    ): ChatCompletionResponse {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            maxTokens = 1000
        )

        val response = client.post("${provider.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            if (!apiKey.isNullOrBlank() && provider.requiresApiKey) {
                headers {
                    append("Authorization", "Bearer $apiKey")
                }
            }
            setBody(request)
        }

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

        return response.body()
    }

    fun close() {
        client.close()
    }
}

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
    val maxTokens: Int = 1000
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList()
) {
    @Serializable
    data class Choice(
        val message: ChatMessage? = null,
        val delta: ChatMessage? = null
    )

    fun firstContent(): String? {
        return choices.firstOrNull()?.message?.content
            ?: choices.firstOrNull()?.delta?.content
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