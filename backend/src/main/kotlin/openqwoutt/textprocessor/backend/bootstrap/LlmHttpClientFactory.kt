package openqwoutt.textprocessor.backend.bootstrap

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Shared outbound HTTP client for OpenRouter / NVIDIA (timeouts + JSON).
 *
 * Env:
 * - `LLM_HTTP_REQUEST_TIMEOUT_MS` (default 120000) — non-streaming `chat/completions`.
 * - `LLM_HTTP_CONNECT_TIMEOUT_MS` (default 15000).
 *
 * Streaming `chat/completions` (`stream: true`) uses a separate per-request timeout:
 * `LLM_HTTP_STREAM_REQUEST_TIMEOUT_MS` (see prompt-proxy streaming completer).
 */
fun createLlmHttpClient(
    json: Json,
    configureDefaultRequest: DefaultRequest.DefaultRequestBuilder.() -> Unit,
): HttpClient {
    val requestMs = System.getenv("LLM_HTTP_REQUEST_TIMEOUT_MS")?.toLongOrNull()?.coerceAtLeast(5_000L) ?: 120_000L
    val connectMs = System.getenv("LLM_HTTP_CONNECT_TIMEOUT_MS")?.toLongOrNull()?.coerceAtLeast(1_000L) ?: 15_000L
    return HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = requestMs
            connectTimeoutMillis = connectMs
            socketTimeoutMillis = requestMs
        }
        install(ClientContentNegotiation) {
            json(json)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            configureDefaultRequest()
        }
    }
}
