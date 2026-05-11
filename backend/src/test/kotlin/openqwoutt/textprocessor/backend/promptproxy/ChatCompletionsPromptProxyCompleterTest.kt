package openqwoutt.textprocessor.backend.promptproxy

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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import openqwoutt.textprocessor.backend.shared.openrouter.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatCompletionsPromptProxyCompleterTest {

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private val chatUrl = "http://mock.example/v1/chat/completions"

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }

    private val messages =
        listOf(
            ChatMessage(role = "system", content = "sys"),
            ChatMessage(role = "user", content = "hi"),
        )

    private fun completer(engine: MockEngine) =
        ChatCompletionsPromptProxyCompleter(
            httpClient = httpClient(engine),
            json = json,
            chatCompletionsUrl = chatUrl,
            providerId = "test-provider",
        )

    @Test
    fun `completeChat returns trimmed assistant text`() =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = """{"choices":[{"message":{"role":"assistant","content":"  OUT  "}}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val r =
                completer(engine).completeChat(
                    model = "m",
                    messages = messages,
                    temperature = 0.4,
                    maxTokens = 100,
                )
            assertTrue(r is LlmAttemptResult.Success)
            assertEquals("OUT", (r as LlmAttemptResult.Success).text)
        }

    @Test
    fun `completeChat empty assistant message returns failure with raw body`() =
        runBlocking {
            val raw = """{"choices":[{"message":{"role":"assistant","content":"   "}}]}"""
            val engine =
                MockEngine { _ ->
                    respond(
                        content = raw,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val r = completer(engine).completeChat("m", messages, 0.4, 100)
            assertTrue(r is LlmAttemptResult.Failure)
            r as LlmAttemptResult.Failure
            assertTrue(r.message.contains("empty", ignoreCase = true))
            assertEquals(raw, r.providerRaw)
        }

    @Test
    fun `completeChat 200 unparseable json returns decode failure`() =
        runBlocking {
            val raw = """not-json"""
            val engine =
                MockEngine { _ ->
                    respond(
                        content = raw,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val r = completer(engine).completeChat("m", messages, 0.4, 100)
            assertTrue(r is LlmAttemptResult.Failure)
            assertTrue((r as LlmAttemptResult.Failure).message.contains("decode", ignoreCase = true))
            assertEquals(raw, r.providerRaw)
        }

    @Test
    fun `completeChat http error includes provider json when valid`() =
        runBlocking {
            val raw = """{"error":"limit"}"""
            val engine =
                MockEngine { _ ->
                    respond(
                        content = raw,
                        status = HttpStatusCode.PaymentRequired,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val r = completer(engine).completeChat("m", messages, 0.4, 100)
            assertTrue(r is LlmAttemptResult.Failure)
            r as LlmAttemptResult.Failure
            assertEquals(402, r.httpStatus)
            assertEquals(raw, r.providerRaw)
            assertTrue(r.message.contains("402"))
        }

    @Test
    fun `completeChat transport failure returns failure without raw`() =
        runBlocking {
            val engine =
                MockEngine {
                    error("network boom")
                }
            val r = completer(engine).completeChat("m", messages, 0.4, 100)
            assertTrue(r is LlmAttemptResult.Failure)
            r as LlmAttemptResult.Failure
            assertNull(r.providerRaw)
            assertTrue(r.message!!.contains("boom"))
        }

    @Test
    fun `streamChat http error emits failed frame with full raw body`() =
        runBlocking {
            val raw = """{"x":1}"""
            val engine =
                MockEngine { req ->
                    assertEquals("text/event-stream", req.headers[HttpHeaders.Accept])
                    respond(
                        content = raw,
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val frames =
                completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(1, frames.size)
            val f = frames.single()
            assertTrue(f is ProxyStreamFrame.Failed)
            val detail = (f as ProxyStreamFrame.Failed).detail
            assertEquals(401, detail.httpStatus)
            assertEquals(raw, detail.providerRaw)
        }

    @Test
    fun `streamChat parses sse deltas and done`() =
        runBlocking {
            val sse =
                """
                data: {"choices":[{"delta":{"content":"Hel"}}]}

                data: {"choices":[{"delta":{"content":"lo"}}]}

                data: [DONE]

                """.trimIndent()
            val engine =
                MockEngine { _ ->
                    respond(
                        content = sse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
                    )
                }
            val frames =
                completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(
                listOf(ProxyStreamFrame.Delta("Hel"), ProxyStreamFrame.Delta("lo")),
                frames,
            )
        }

    @Test
    fun `streamChat concatenates content and refusal in delta order`() =
        runBlocking {
            val sse =
                """
                data: {"choices":[{"delta":{"content":"yes","refusal":"no"}}]}
                data: [DONE]
                """.trimIndent()
            val engine =
                MockEngine { _ ->
                    respond(content = sse, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/event-stream"))
                }
            val frames = completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(listOf(ProxyStreamFrame.Delta("yesno")), frames)
        }

    @Test
    fun `streamChat emits reasoning from delta`() =
        runBlocking {
            val sse =
                """
                data: {"choices":[{"delta":{"reasoning":"R"}}]}
                data: [DONE]
                """.trimIndent()
            val engine =
                MockEngine { _ ->
                    respond(content = sse, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/event-stream"))
                }
            val frames = completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(listOf(ProxyStreamFrame.Delta("R")), frames)
        }

    @Test
    fun `streamChat skips empty deltas then emits content`() =
        runBlocking {
            val sse =
                """
                data: {"choices":[{"delta":{}}]}
                data: {"choices":[]}
                data: {"choices":[{"delta":{"content":"x"}}]}
                data: [DONE]
                """.trimIndent()
            val engine =
                MockEngine { _ ->
                    respond(content = sse, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/event-stream"))
                }
            val frames = completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(listOf(ProxyStreamFrame.Delta("x")), frames)
        }

    @Test
    fun `streamChat five malformed sse json lines emits upstream repeated invalid failure`() =
        runBlocking {
            val sse =
                buildString {
                    repeat(5) {
                        appendLine("data: {{{bad")
                        appendLine()
                    }
                }
            val engine =
                MockEngine { _ ->
                    respond(content = sse, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/event-stream"))
                }
            val frames = completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(1, frames.size)
            assertTrue(frames.single() is ProxyStreamFrame.Failed)
            assertTrue(
                (frames.single() as ProxyStreamFrame.Failed).detail.message.contains(
                    "repeated invalid",
                    ignoreCase = true,
                ),
            )
        }

    @Test
    fun `streamChat four malformed then valid delta succeeds without invalid failure`() =
        runBlocking {
            val sse =
                buildString {
                    repeat(4) {
                        appendLine("data: {{{bad")
                        appendLine()
                    }
                    appendLine("""data: {"choices":[{"delta":{"content":"ok"}}]}""")
                    appendLine()
                    appendLine("data: [DONE]")
                }
            val engine =
                MockEngine { _ ->
                    respond(content = sse, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "text/event-stream"))
                }
            val frames = completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(listOf(ProxyStreamFrame.Delta("ok")), frames)
        }

    @Test
    fun `streamChat engine exception emits failed frame`() =
        runBlocking {
            val engine =
                MockEngine {
                    error("broken pipe")
                }
            val frames = completer(engine).streamChat("m", messages, 0.4, 2048).toList()
            assertEquals(1, frames.size)
            assertTrue(frames.single() is ProxyStreamFrame.Failed)
            assertTrue((frames.single() as ProxyStreamFrame.Failed).detail.message.contains("broken pipe"))
        }
}
