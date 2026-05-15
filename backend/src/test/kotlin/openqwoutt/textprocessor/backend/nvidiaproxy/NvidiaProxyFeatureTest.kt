package openqwoutt.textprocessor.backend.nvidiaproxy

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class NvidiaProxyFeatureTest {
    @Test
    fun `nvidia proxy routes absent when feature disabled by default`() =
        testApplication {
            application {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; explicitNulls = false })
                }
                NvidiaProxyFeature.install(this)
            }
            val health = client.get("/api/nvidia/proxy/health")
            val capabilities = client.get("/api/nvidia/proxy/capabilities")
            assertEquals(HttpStatusCode.NotFound, health.status)
            assertEquals(HttpStatusCode.NotFound, capabilities.status)
        }

    @Test
    fun `nvidia proxy feature fails fast when enabled without api key`() =
        testApplication {
            assertThrows<IllegalStateException> {
                application {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
                    }
                    NvidiaProxyFeature.install(this) { name ->
                        when (name) {
                            "NVIDIA_PROXY_ENABLED" -> "true"
                            else -> null
                        }
                    }
                }
            }
        }
}
