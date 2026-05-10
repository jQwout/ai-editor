package openqwoutt.textprocessor.backend.promptproxy

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
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

    private class FakeCompleter(
        private val result: LlmAttemptResult,
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
    }

    private fun testRouting(
        completerResult: LlmAttemptResult,
        serverDefaultModel: String = "test/default-model",
        block: suspend io.ktor.client.HttpClient.(FakeCompleter) -> Unit,
    ) {
        testApplication {
            val completer = FakeCompleter(completerResult)
            application {
                install(ServerContentNegotiation) { json(json) }
                routing {
                    val service = PromptProxyService(completer, defaultRoutingModel = serverDefaultModel)
                    promptProxyRoutes(service)
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
}
