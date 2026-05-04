package openqwoutt.textstyler.data.settings

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenRouterModelsRepository {

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
                socketTimeout = 15_000
            }
        }
    }

    suspend fun fetchModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://openrouter.ai/api/v1/models"
            Log.d(TAG, "-> GET $url")

            val response: JsonObject = client.get(url) {
                header("Accept", "application/json")
            }.body()

            Log.d(TAG, "<- Response body: $response")

            val data = response["data"]
                ?.jsonArray
                ?: error("Missing 'data' array in response")

            val models = mutableListOf<String>()
            for (element in data) {
                val id = element.jsonObject["id"]
                    ?.jsonPrimitive
                    ?.content
                    ?: continue
                if (id.endsWith(":free")) {
                    models.add(id)
                }
            }
            models
        }
    }

    companion object {
        private const val TAG = "OpenRouterModels"
    }
}
