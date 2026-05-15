package openqwoutt.textprocessor.backend.modelscatalog

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModelsCatalogFeatureTest {
    @Test
    fun `catalog routes absent when feature disabled by default`() =
        testApplication {
            application {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; explicitNulls = false })
                }
                ModelsCatalogFeature.install(this)
            }
            val res = client.get("/models-catalog/models")
            assertEquals(HttpStatusCode.NotFound, res.status)
        }
}
