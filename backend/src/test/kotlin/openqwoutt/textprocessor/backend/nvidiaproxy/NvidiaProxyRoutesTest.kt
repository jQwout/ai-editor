package openqwoutt.textprocessor.backend.nvidiaproxy

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NvidiaProxyRoutesTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val testConfig =
        NvidiaProxyConfig(
            apiKey = "route-test-key",
            upstreamBaseUrl = "https://integrate.api.nvidia.com/v1",
            requestTimeoutMs = 120_000L,
            connectTimeoutMs = 15_000L,
            rateLimitPerMinute = null,
            rateLimitTrustForwarded = false,
        )

    private fun testRouting(
        rateLimit: NvidiaProxyRateLimit? = null,
        block: suspend io.ktor.client.HttpClient.() -> Unit,
    ) {
        testApplication {
            application {
                install(ContentNegotiation) { json(json) }
                routing {
                    nvidiaProxyRoutes(
                        service = NvidiaProxyService(testConfig),
                        apiKey = testConfig.apiKey,
                        rateLimit = rateLimit,
                    )
                }
            }
            client.block()
        }
    }

    @Test
    fun `health unauthorized when bearer missing`() =
        testRouting {
            val res = get("/api/nvidia/proxy/health")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
            val body = res.body<NvidiaProxyErrorEnvelope>()
            assertEquals("unauthorized", body.error.code)
        }

    @Test
    fun `capabilities unauthorized when bearer invalid`() =
        testRouting {
            val res =
                get("/api/nvidia/proxy/capabilities") {
                    header(HttpHeaders.Authorization, "Bearer wrong")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
            val body = res.body<NvidiaProxyErrorEnvelope>()
            assertTrue(body.error.message.contains("Unauthorized", ignoreCase = true))
        }

    @Test
    fun `capabilities allows bearer prefix with different casing`() =
        testRouting {
            val res =
                get("/api/nvidia/proxy/capabilities") {
                    header(HttpHeaders.Authorization, "bearer ${testConfig.apiKey}")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<NvidiaProxyCapabilitiesResponse>()
            assertEquals("nvidia", body.provider)
        }

    @Test
    fun `health returns ok with valid bearer`() =
        testRouting {
            val res =
                get("/api/nvidia/proxy/health") {
                    header(HttpHeaders.Authorization, "Bearer ${testConfig.apiKey}")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<NvidiaProxyHealthResponse>()
            assertEquals("ok", body.status)
            assertEquals("nvidia-proxy", body.feature)
            assertEquals(testConfig.upstreamBaseUrl, body.upstreamBaseUrl)
        }

    @Test
    fun `stub returns not implemented with valid bearer`() =
        testRouting {
            val res =
                post("/api/nvidia/proxy/stub") {
                    header(HttpHeaders.Authorization, "Bearer ${testConfig.apiKey}")
                    contentType(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.NotImplemented, res.status)
            val body = res.body<NvidiaProxyErrorEnvelope>()
            assertEquals("not_implemented", body.error.code)
        }

    @Test
    fun `capabilities returns planned operation with valid bearer`() =
        testRouting {
            val res =
                get("/api/nvidia/proxy/capabilities") {
                    header(HttpHeaders.Authorization, "Bearer ${testConfig.apiKey}")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<NvidiaProxyCapabilitiesResponse>()
            assertEquals("nvidia", body.provider)
            assertTrue(body.ready)
            assertTrue(body.implementedOperations.isEmpty())
            assertTrue(body.plannedOperations.contains("chat.completions.passthrough"))
        }

    @Test
    fun `stub unauthorized when bearer missing`() =
        testRouting {
            val res =
                post("/api/nvidia/proxy/stub") {
                    contentType(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
            val body = res.body<NvidiaProxyErrorEnvelope>()
            assertEquals("unauthorized", body.error.code)
        }

    @Test
    fun `nvidia proxy rate limit blocks repeated attempts before auth`() =
        testRouting(
            rateLimit =
                NvidiaProxyRateLimit(
                    limiter = openqwoutt.textprocessor.backend.bootstrap.SlidingWindowRateLimiter(1, 60_000L),
                    trustForwardedHeaders = false,
                ),
        ) {
            val first = get("/api/nvidia/proxy/health")
            assertEquals(HttpStatusCode.Unauthorized, first.status)

            val second = get("/api/nvidia/proxy/health")
            assertEquals(HttpStatusCode.TooManyRequests, second.status)
            assertEquals("60", second.headers[HttpHeaders.RetryAfter])
            val body = second.body<NvidiaProxyErrorEnvelope>()
            assertEquals("too_many_requests", body.error.code)
        }
}
