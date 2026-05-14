package openqwoutt.textprocessor.backend.promptproxy

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import openqwoutt.textprocessor.backend.shared.openrouter.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptProxyRoutesTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private companion object {
        const val TEST_PROXY_KEY: String = "test-prompt-proxy-api-key"
    }

    private class FakeCompleter(
        private val result: LlmAttemptResult,
        private val streamFlow: Flow<ProxyStreamFrame> = emptyFlow(),
        var lastSeenModelId: String? = null,
    ) : PromptProxyLlmCompleter {
        override suspend fun completeChat(
            model: String,
            messages: List<ChatMessage>,
            temperature: Double,
            maxTokens: Int,
        ): LlmAttemptResult {
            lastSeenModelId = model
            return result
        }

        override fun streamChat(
            model: String,
            messages: List<ChatMessage>,
            temperature: Double,
            maxTokens: Int,
        ): Flow<ProxyStreamFrame> {
            lastSeenModelId = model
            return streamFlow
        }
    }

    private fun testRouting(
        completerResult: LlmAttemptResult,
        serverDefaultModel: String = "test/default-model",
        streamFlow: Flow<ProxyStreamFrame> = emptyFlow(),
        block: suspend io.ktor.client.HttpClient.(FakeCompleter) -> Unit,
    ) {
        testApplication {
            val completer = FakeCompleter(completerResult, streamFlow = streamFlow)
            application {
                install(ServerContentNegotiation) { json(json) }
                routing {
                    val service = PromptProxyService(completer, defaultRoutingModel = serverDefaultModel)
                    promptProxyRoutes(service, TEST_PROXY_KEY)
                }
            }
            val client = createClient {
                install(ContentNegotiation) { json(json) }
            }
            client.block(completer)
        }
    }

    @Test
    fun `bad request when prompt blank`() = testRouting(completerResult = LlmAttemptResult.Success("")) {
            _ ->
        val res =
            post("/api/prompt/proxy") {
                header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                contentType(ContentType.Application.Json)
                setBody(
                    PromptProxyRequest(
                        style = "any",
                        prompt = "   ",
                        language = "en",
                    )
                )
            }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        val envelope = res.body<PromptProxyErrorEnvelope>()
        assertTrue(envelope.error.message.contains("prompt", ignoreCase = true))
    }

    @Test
    fun `bad request when language blank`() = testRouting(completerResult = LlmAttemptResult.Success("")) {
            _ ->
        val res =
            post("/api/prompt/proxy") {
                header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                contentType(ContentType.Application.Json)
                setBody(
                    PromptProxyRequest(
                        style = "bold",
                        prompt = "hello",
                        language = "",
                    )
                )
            }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        val envelope = res.body<PromptProxyErrorEnvelope>()
        assertTrue(envelope.error.message.contains("language", ignoreCase = true))
    }

    @Test
    fun `ok returns model text`() =
        testRouting(completerResult = LlmAttemptResult.Success("DONE")) {
                completer ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "make it short",
                            prompt = "hello world",
                            language = "en",
                        )
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<PromptProxySuccessResponse>()
            assertEquals("DONE", body.result)
            assertEquals("test/default-model", completer.lastSeenModelId)
        }

    @Test
    fun `provider failure returns envelope with body`() =
        testRouting(
            completerResult =
                LlmAttemptResult.Failure(
                    message = "fail",
                    provider = "openrouter",
                    httpStatus = 429,
                    providerBody = buildJsonObject { put("detail", JsonPrimitive("rate")) },
                    providerRaw = null,
                )
        ) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "hello",
                            language = "ru",
                        )
                    )
                }
            assertEquals(HttpStatusCode.BadGateway, res.status)
            val envelope = res.body<PromptProxyErrorEnvelope>()
            assertEquals("openrouter", envelope.error.provider)
            assertEquals(429, envelope.error.httpStatus)
            val detail =
                checkNotNull(envelope.error.providerBody).jsonObject["detail"]
            assertEquals(JsonPrimitive("rate"), detail)
            assertNull(envelope.error.providerRaw)
        }

    @Test
    fun `invalid json returns 400`() =
        testRouting(completerResult = LlmAttemptResult.Success("")) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody("{ not json")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            val envelope = res.body<PromptProxyErrorEnvelope>()
            assertTrue(envelope.error.message.contains("JSON", ignoreCase = true))
        }

    @Test
    fun `request model overrides server default`() =
        testRouting(
            completerResult = LlmAttemptResult.Success("ok"),
            serverDefaultModel = "server/model",
        ) {
                completer ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "hi",
                            language = "en",
                            model = "  client/model  ",
                        )
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("ok", res.body<PromptProxySuccessResponse>().result)
            assertEquals("client/model", completer.lastSeenModelId)
        }

    @Test
    fun `bad request when model missing and no server default`() =
        testRouting(completerResult = LlmAttemptResult.Success("x"), serverDefaultModel = "") {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "hi",
                            language = "en",
                        )
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            val envelope = res.body<PromptProxyErrorEnvelope>()
            assertTrue(envelope.error.message.contains("model", ignoreCase = true))
        }

    @Test
    fun `stream true returns sse chunks and done`() =
        testRouting(
            completerResult = LlmAttemptResult.Success("unused"),
            streamFlow =
                flowOf(
                    ProxyStreamFrame.Delta("hel"),
                    ProxyStreamFrame.Delta("lo"),
                ),
        ) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "s",
                            prompt = "x",
                            language = "en",
                            stream = true,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertTrue(res.headers["Content-Type"]?.startsWith("text/event-stream") == true)
            val text = res.bodyAsText()
            assertTrue(text.contains(""""text":"hel""""))
            assertTrue(text.contains(""""text":"lo""""))
            assertTrue(text.contains("event: done"))
        }

    @Test
    fun `stream true emits error frame when upstream fails`() =
        testRouting(
            completerResult = LlmAttemptResult.Success("unused"),
            streamFlow =
                flowOf(
                    ProxyStreamFrame.Failed(
                        PromptProxyErrorDetail(
                            message = "upstream",
                            provider = "openrouter",
                            httpStatus = 500,
                            providerRaw = """{"err":true}""",
                        ),
                    ),
                ),
        ) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "x",
                            language = "en",
                            stream = true,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val text = res.bodyAsText()
            assertTrue(text.contains("event: error"))
            assertTrue(text.contains("upstream"))
            assertTrue(text.contains("providerRaw"))
            assertTrue(!text.contains("event: done"))
        }

    @Test
    fun `stream true blank prompt returns 400 json not sse`() =
        testRouting(completerResult = LlmAttemptResult.Success("x")) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "a",
                            prompt = "   ",
                            language = "en",
                            stream = true,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertTrue(
                res.headers["Content-Type"]?.contains("application/json", ignoreCase = true) == true,
                "validation errors must stay JSON, not SSE",
            )
            val envelope = res.body<PromptProxyErrorEnvelope>()
            assertTrue(envelope.error.message.contains("prompt", ignoreCase = true))
        }

    @Test
    fun `stream false explicit uses json sync response`() =
        testRouting(completerResult = LlmAttemptResult.Success("SYNC")) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "ok",
                            language = "en",
                            stream = false,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("SYNC", res.body<PromptProxySuccessResponse>().result)
        }

    @Test
    fun `stream true empty upstream flow still ends with done`() =
        testRouting(
            completerResult = LlmAttemptResult.Success("unused"),
            streamFlow = emptyFlow(),
        ) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "x",
                            prompt = "y",
                            language = "en",
                            stream = true,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val text = res.bodyAsText()
            assertTrue(text.contains("event: done"))
        }

    @Test
    fun `stream true uses client model id`() =
        testRouting(
            completerResult = LlmAttemptResult.Success("unused"),
            streamFlow = flowOf(ProxyStreamFrame.Delta("a")),
            serverDefaultModel = "server/m",
        ) {
                completer ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "p",
                            language = "en",
                            model = "  my/model  ",
                            stream = true,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("my/model", completer.lastSeenModelId)
        }

    @Test
    fun `stream true then failed does not append done`() =
        testRouting(
            completerResult = LlmAttemptResult.Success("unused"),
            streamFlow =
                flowOf(
                    ProxyStreamFrame.Delta("x"),
                    ProxyStreamFrame.Failed(
                        PromptProxyErrorDetail(message = "mid", provider = "p", httpStatus = 400, providerRaw = "raw"),
                    ),
                ),
        ) {
                _ ->
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer $TEST_PROXY_KEY")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "p",
                            language = "en",
                            stream = true,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val text = res.bodyAsText()
            assertTrue(text.contains(""""text":"x""""))
            assertTrue(text.contains("event: error"))
            assertTrue(!text.contains("event: done"))
        }

    @Test
    fun `unauthorized when prompt proxy bearer missing`() =
        testRouting(completerResult = LlmAttemptResult.Success("should-not-run")) {
            val res =
                post("/api/prompt/proxy") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "hello",
                            language = "en",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
            val env = res.body<PromptProxyErrorEnvelope>()
            assertTrue(env.error.message.contains("Unauthorized", ignoreCase = true))
        }

    @Test
    fun `unauthorized when prompt proxy bearer wrong`() =
        testRouting(completerResult = LlmAttemptResult.Success("should-not-run")) {
            val res =
                post("/api/prompt/proxy") {
                    header(HttpHeaders.Authorization, "Bearer wrong-key")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PromptProxyRequest(
                            style = "",
                            prompt = "hello",
                            language = "en",
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
            val env = res.body<PromptProxyErrorEnvelope>()
            assertTrue(env.error.message.contains("Unauthorized", ignoreCase = true))
        }
}
