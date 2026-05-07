package openqwoutt.textstyler.data.settings

import android.util.Log
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

@Inject
class OpenRouterModelsRepository {

    suspend fun fetchModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://openrouter.ai/api/v1/models"
            Log.d(TAG, "-> GET $url")

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
            connection.disconnect()

            Log.d(TAG, "<- Response body: $body")

            if (responseCode !in 200..299) {
                error("HTTP $responseCode: $body")
            }

            val json = JSONObject(body)
            val data = json.getJSONArray("data")

            val models = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id", "")
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
