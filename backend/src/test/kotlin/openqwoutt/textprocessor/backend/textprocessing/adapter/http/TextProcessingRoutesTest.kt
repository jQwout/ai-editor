package openqwoutt.textprocessor.backend.textprocessing.adapter.http

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import openqwoutt.textprocessor.backend.textprocessing.application.LlmCompletionPort
import openqwoutt.textprocessor.backend.textprocessing.application.ModelPromptCatalog
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextUseCase
import openqwoutt.textprocessor.backend.textprocessing.application.RegistryResolvedPrompt
import openqwoutt.textprocessor.backend.textprocessing.domain.LlmCompletionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextProcessingRoutesTest {

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private val routeTestCatalog =
        ModelPromptCatalog { modelId, modeId ->
            if (modelId == "registry-model" && modeId == "style") {
                RegistryResolvedPrompt(promptText = "from-registry", temperature = 0.55)
            } else {
                null
            }
        }

    private class FixedLlm(private val out: String) : LlmCompletionPort {
        override suspend fun complete(
            userText: String,
            taskPrompt: String,
            temperature: Double,
            routingModelId: String,
        ): String = out
    }

    private class ThrowingLlm(private val ex: Throwable) : LlmCompletionPort {
        override suspend fun complete(
            userText: String,
            taskPrompt: String,
            temperature: Double,
            routingModelId: String,
        ): String {
            throw ex
        }
    }

    private fun testWithUseCase(
        useCase: ProcessTextUseCase,
        block: suspend io.ktor.client.HttpClient.() -> Unit,
    ) {
        testApplication {
            application {
                install(ServerContentNegotiation) { json(json) }
                routing {
                    textProcessingRoutes(useCase)
                }
            }
            val client = createClient { install(ContentNegotiation) { json(json) } }
            runBlocking { client.block() }
        }
    }

    @Test
    fun `post process returns result`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("HELLO"), null, "m")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "input", mode = "style"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<ProcessTextResponseDto>()
            assertEquals("HELLO", body.result)
        }

    @Test
    fun `post process returns full error envelope on llm failure`() =
        testWithUseCase(
            ProcessTextUseCase(
                ThrowingLlm(
                    LlmCompletionException(
                        message = "upstream-down",
                        httpStatus = 503,
                        providerRaw = """{"code":9}""",
                        providerId = "nvidia",
                    ),
                ),
                null,
                "m",
            ),
        ) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "x", mode = "style"))
                }
            assertEquals(HttpStatusCode.BadGateway, res.status)
            val err = res.body<ErrorResponseDto>()
            assertEquals("upstream-down", err.error)
            assertEquals("nvidia", err.providerId)
            assertEquals(503, err.httpStatus)
            assertNotNull(err.providerRaw)
            assertEquals(9, err.providerBody!!.jsonObject["code"]!!.jsonPrimitive.int)
            assertNull(err.detail)
        }

    @Test
    fun `unknown mode is bad request`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("x"), null, "m")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "x", mode = "___unknown___"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            val err = res.body<ErrorResponseDto>()
            assertTrue(err.error.contains("Unknown", ignoreCase = true))
            assertNull(err.providerId)
        }

    @Test
    fun `disabled llm is 503`() =
        testWithUseCase(ProcessTextUseCase(null, null, "")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "x", mode = "style"))
                }
            assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        }

    @Test
    fun `blank text is bad request`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("x"), null, "m")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "   \n", mode = "style"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            val err = res.body<ErrorResponseDto>()
            assertTrue(err.error.contains("required", ignoreCase = true))
        }

    @Test
    fun `model without registry is 503`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("x"), null, "fallback")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "hi", mode = "style", model = "any"))
                }
            assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
            val err = res.body<ErrorResponseDto>()
            assertTrue(err.error.contains("registry", ignoreCase = true))
        }

    @Test
    fun `unknown model in registry is bad request`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("x"), routeTestCatalog, "fb")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "hi", mode = "style", model = "no-such-model"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `explicit registry model succeeds and returnPrompt exposes fields`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("REG-OUT"), routeTestCatalog, "fb")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProcessTextRequestDto(
                            text = "t",
                            mode = "style",
                            model = "registry-model",
                            returnPrompt = true,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<ProcessTextResponseDto>()
            assertEquals("REG-OUT", body.result)
            assertEquals("registry-model", body.model)
            assertEquals("style", body.mode)
            assertEquals("from-registry", body.promptText)
            assertEquals(0.55, body.temperature!!, 0.001)
        }

    @Test
    fun `returnPrompt false omits routing metadata in ok response`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("OK"), null, "fb-model")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "x", mode = "style", returnPrompt = false))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<ProcessTextResponseDto>()
            assertEquals("OK", body.result)
            assertNull(body.model)
            assertNull(body.mode)
            assertNull(body.promptText)
            assertNull(body.temperature)
        }

    @Test
    fun `invalid json body is bad request`() =
        testWithUseCase(ProcessTextUseCase(FixedLlm("x"), null, "m")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody("{not-json")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            val err = res.body<ErrorResponseDto>()
            assertTrue(err.error.contains("Invalid JSON", ignoreCase = true))
        }

    @Test
    fun `non llm exception returns bad gateway with detail`() =
        testWithUseCase(ProcessTextUseCase(ThrowingLlm(RuntimeException("internal-boom")), null, "m")) {
            val res =
                post("/api/text/process") {
                    contentType(ContentType.Application.Json)
                    setBody(ProcessTextRequestDto(text = "x", mode = "style"))
                }
            assertEquals(HttpStatusCode.BadGateway, res.status)
            val err = res.body<ErrorResponseDto>()
            assertEquals("internal-boom", err.error)
            assertNotNull(err.detail)
            assertTrue(err.detail!!.contains("RuntimeException"))
        }
}
