package openqwoutt.textprocessor.backend.modelscatalog

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import openqwoutt.textprocessor.backend.limits.ADMIN_UPSERT_MAX_ITEMS
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelsCatalogRoutesTest {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        @BeforeAll
        @JvmStatic
        fun startPg() = ModelsCatalogTestSupport.initPostgres()

        @AfterAll
        @JvmStatic
        fun stopPg() = ModelsCatalogTestSupport.closePostgres()
    }

    @BeforeEach
    fun clean() = ModelsCatalogTestSupport.clearModels()

    private fun appTest(
        cfg: ModelsCatalogConfig = ModelsCatalogTestSupport.testConfig(),
        block: suspend io.ktor.client.HttpClient.() -> Unit,
    ) {
        val dao = NvidiaModelDao(ModelsCatalogTestSupport.ds)
        val service = ModelsCatalogService(dao)

        testApplication {
            application {
                install(ServerContentNegotiation) { json(json) }
                routing { modelsCatalogRoutes(service, cfg) }
            }
            val client = createClient {
                install(ContentNegotiation) { json(json) }
            }
            client.block()
        }
    }

    private suspend fun io.ktor.client.HttpClient.syncModels(
        token: String? = "secret",
        body: AdminSyncModelsRequest,
    ) = post("/models-catalog/admin/sync") {
        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        setBody(body)
    }

    @Test
    fun `health returns ok`() = appTest {
        val res = get("/models-catalog/health")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("ok", res.body<Map<String, String>>()["status"])
    }

    @Test
    fun `GET empty catalog returns nvidia provider and cache headers`() = appTest {
        val res = get("/models-catalog/models")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<ModelCatalogResponseDto>()
        assertEquals(PROVIDER_NVIDIA, body.provider)
        assertTrue(body.models.isEmpty())
        assertEquals("1970-01-01T00:00:00Z", body.updatedAt)
        assertNotNull(res.headers[HttpHeaders.ETag])
        assertEquals("\"nvidia-0-0-0\"", res.headers[HttpHeaders.ETag])
        assertEquals("public, max-age=3600", res.headers[HttpHeaders.CacheControl])
    }

    @Test
    fun `GET uses custom max-age from config`() = appTest(cfg = ModelsCatalogTestSupport.testConfig(catalogMaxAgeSec = 120)) {
        val res = get("/models-catalog/models")
        assertEquals("public, max-age=120", res.headers[HttpHeaders.CacheControl])
    }

    @Test
    fun `sync upserts models and disableMissing removes stale`() = appTest {
        syncModels(
            body =
                AdminSyncModelsRequest(
                    models =
                        listOf(
                            CatalogModelEntry(modelId = "meta/llama-3.1-70b-instruct", displayName = "Llama 3.1 70B"),
                            CatalogModelEntry(modelId = "qwen/qwen3-coder", displayName = "Qwen3 Coder"),
                        ),
                ),
        ).apply { assertEquals(HttpStatusCode.OK, status) }

        val catalog = get("/models-catalog/models").body<ModelCatalogResponseDto>()
        assertEquals(2, catalog.models.size)

        val sync2 =
            syncModels(
                body =
                    AdminSyncModelsRequest(
                        models = listOf(CatalogModelEntry(modelId = "meta/llama-3.1-70b-instruct", displayName = "Llama")),
                    ),
            )
        val syncBody = sync2.body<AdminSyncModelsResponseDto>()
        assertEquals(1, syncBody.upserted)
        assertEquals(0, syncBody.unchanged)
        assertEquals(1, syncBody.disabled)
        assertNotNull(syncBody.updatedAt)

        assertEquals(1, get("/models-catalog/models").body<ModelCatalogResponseDto>().models.size)
    }

    @Test
    fun `sync without disableMissing keeps removed models enabled`() = appTest {
        syncModels(
            body =
                AdminSyncModelsRequest(
                    models =
                        listOf(
                            CatalogModelEntry(modelId = "a", displayName = "A"),
                            CatalogModelEntry(modelId = "b", displayName = "B"),
                        ),
                ),
        )
        syncModels(
            body =
                AdminSyncModelsRequest(
                    disableMissing = false,
                    models = listOf(CatalogModelEntry(modelId = "a", displayName = "A")),
                ),
        )
        assertEquals(2, get("/models-catalog/models").body<ModelCatalogResponseDto>().models.size)
    }

    @Test
    fun `sync updates display name on conflict`() = appTest {
        syncModels(
            body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry(modelId = "m", displayName = "Old"))),
        )
        syncModels(
            body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry(modelId = "m", displayName = "New"))),
        )
        val name = get("/models-catalog/models").body<ModelCatalogResponseDto>().models.single().displayName
        assertEquals("New", name)
    }

    @Test
    fun `sync with isEnabled false excludes model from GET`() = appTest {
        syncModels(
            body =
                AdminSyncModelsRequest(
                    models =
                        listOf(
                            CatalogModelEntry(modelId = "on", displayName = "On", isEnabled = true),
                            CatalogModelEntry(modelId = "off", displayName = "Off", isEnabled = false),
                        ),
                ),
        )
        assertEquals(1, get("/models-catalog/models").body<ModelCatalogResponseDto>().models.size)
    }

    @Test
    fun `sync rejects duplicate modelId in batch`() = appTest {
        val res =
            syncModels(
                body =
                    AdminSyncModelsRequest(
                        models =
                            listOf(
                                CatalogModelEntry(modelId = "dup", displayName = "A"),
                                CatalogModelEntry(modelId = "dup", displayName = "B"),
                            ),
                    ),
            )
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertTrue(res.bodyAsText().contains("duplicate"))
    }

    @Test
    fun `sync rejects empty models`() = appTest {
        val res = syncModels(body = AdminSyncModelsRequest(models = emptyList()))
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertTrue(res.bodyAsText().contains("empty"))
    }

    @Test
    fun `sync rejects batch over limit`() = appTest {
        val models = List(ADMIN_UPSERT_MAX_ITEMS + 1) { i ->
            CatalogModelEntry(modelId = "model-$i", displayName = "Model $i")
        }
        val res = syncModels(body = AdminSyncModelsRequest(models = models))
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertTrue(res.bodyAsText().contains("maximum size"))
    }

    @Test
    fun `sync unauthorized without bearer`() = appTest {
        val res = syncModels(token = null, body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("m", "M"))))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `sync unauthorized with wrong bearer`() = appTest {
        val res = syncModels(token = "wrong", body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("m", "M"))))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `sync returns not found when admin token not configured`() = appTest(cfg = ModelsCatalogTestSupport.testConfig(adminToken = null)) {
        val res = syncModels(body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("m", "M"))))
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun `GET returns 304 when If-None-Match matches ETag`() = appTest {
        syncModels(
            body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("meta/llama", "Llama"))),
        )
        val first = get("/models-catalog/models")
        val etag = first.headers[HttpHeaders.ETag]!!
        val second =
            get("/models-catalog/models") {
                header(HttpHeaders.IfNoneMatch, etag)
            }
        assertEquals(HttpStatusCode.NotModified, second.status)
    }

    @Test
    fun `GET returns 304 for weak If-None-Match prefix`() = appTest {
        syncModels(
            body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("m", "M"))),
        )
        val etag = get("/models-catalog/models").headers[HttpHeaders.ETag]!!
        val weak = "W/$etag"
        val res =
            get("/models-catalog/models") {
                header(HttpHeaders.IfNoneMatch, weak)
            }
        assertEquals(HttpStatusCode.NotModified, res.status)
    }

    @Test
    fun `GET returns 200 when If-None-Match differs`() = appTest {
        syncModels(
            body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("m", "M"))),
        )
        val res =
            get("/models-catalog/models") {
                header(HttpHeaders.IfNoneMatch, "\"nvidia-1-99-99\"")
            }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `GET ETag changes after sync updates catalog`() = appTest {
        syncModels(
            body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("m1", "M1"))),
        )
        val etag1 = get("/models-catalog/models").headers[HttpHeaders.ETag]

        Thread.sleep(10)
        syncModels(
            body = AdminSyncModelsRequest(models = listOf(CatalogModelEntry("m2", "M2"))),
        )
        val etag2 = get("/models-catalog/models").headers[HttpHeaders.ETag]

        assertNotNull(etag1)
        assertNotNull(etag2)
        assertNotEquals(etag1, etag2)
    }

    @Test
    fun `GET lists models sorted by display name`() = appTest {
        syncModels(
            body =
                AdminSyncModelsRequest(
                    models =
                        listOf(
                            CatalogModelEntry(modelId = "z", displayName = "Zulu"),
                            CatalogModelEntry(modelId = "a", displayName = "Alpha"),
                        ),
                ),
        )
        val names = get("/models-catalog/models").body<ModelCatalogResponseDto>().models.map { it.displayName }
        assertEquals(listOf("Alpha", "Zulu"), names)
    }
}
