package openqwoutt.textstyler.data.settings

import android.util.Log
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import openqwoutt.textprocessor.app.BuildConfig
import java.util.concurrent.TimeUnit

@Inject
class OpenRouterModelsRepository {

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
                level = LogLevel.INFO
            }
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryOnException(maxRetries = 2, retryOnTimeout = true)
            exponentialDelay()
        }
        engine {
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
            }
        }
    }

    suspend fun fetchModels(): Result<List<String>> = runCatching {
        Log.d(TAG, "-> GET $MODELS_URL")

        val response = client.get(MODELS_URL) {
            accept(ContentType.Application.Json)
        }

        if (!response.status.isSuccess()) {
            val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
            error("HTTP ${response.status.value}: $errorBody")
        }

        val payload: ModelsResponse = response.body()
        payload.data
            .mapNotNull { it.id }
            .filter { it.endsWith(":free") }
    }

    @Serializable
    private data class ModelsResponse(
        val data: List<ModelEntry> = emptyList()
    )

    @Serializable
    private data class ModelEntry(
        val id: String? = null
    )

    companion object {
        private const val TAG = "OpenRouterModels"
        private const val MODELS_URL = "https://openrouter.ai/api/v1/models"
    }
}
