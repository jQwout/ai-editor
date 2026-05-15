package openqwoutt.textprocessor.backend.textprocessing.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import openqwoutt.textprocessor.backend.textprocessing.domain.LlmCompletionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChatCompletionsLlmAdapterTest {

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private fun client(mockEngine: MockEngine): HttpClient =
        HttpClient(mockEngine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }

    @Test
    fun `success returns assistant content`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content =
                            """{"choices":[{"message":{"role":"assistant","content":"  trimmed-out  "}}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val adapter =
                ChatCompletionsLlmAdapter(
                    httpClient = client(engine),
                    chatCompletionsUrl = "https://mock.example/v1/chat/completions",
                    providerId = "test-provider",
                    json = json,
                    maxTokens = 123,
                )
            val out =
                adapter.complete(
                    userText = "user",
                    taskPrompt = "task",
                    temperature = 0.2,
                    routingModelId = "model-x",
                )
            assertEquals("trimmed-out", out)
            assertEquals(1, engine.requestHistory.size)
        }

    @Test
    fun `http error throws with status and raw body`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"detail":"quota exceeded"}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val adapter =
                ChatCompletionsLlmAdapter(
                    httpClient = client(engine),
                    chatCompletionsUrl = "https://mock.example/v1/chat/completions",
                    providerId = "nvidia",
                    json = json,
                )
            val ex =
                assertThrows<LlmCompletionException> {
                    adapter.complete("u", "t", 0.1, "m")
                }
            assertEquals(429, ex.httpStatus)
            assertEquals("nvidia", ex.providerId)
            assertTrue(ex.providerRaw!!.contains("quota"))
            assertTrue(ex.message!!.contains("429"))
        }

    @Test
    fun `200 invalid json throws with raw body`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "not-json{",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val adapter =
                ChatCompletionsLlmAdapter(
                    httpClient = client(engine),
                    chatCompletionsUrl = "https://mock.example/v1/chat/completions",
                    providerId = "openrouter",
                    json = json,
                )
            val ex =
                assertThrows<LlmCompletionException> {
                    adapter.complete("u", "t", 0.1, "m")
                }
            assertEquals(200, ex.httpStatus)
            assertTrue(ex.providerRaw!!.contains("not-json"))
        }

    @Test
    fun `200 empty assistant content throws`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"choices":[{"message":{"role":"assistant","content":"   "}}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val adapter =
                ChatCompletionsLlmAdapter(
                    httpClient = client(engine),
                    chatCompletionsUrl = "https://mock.example/v1/chat/completions",
                    providerId = "p",
                    json = json,
                )
            assertThrows<LlmCompletionException> {
                adapter.complete("u", "t", 0.1, "m")
            }
        }
}
