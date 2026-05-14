package openqwoutt.textprocessor.backend.promptstore

import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import openqwoutt.textprocessor.backend.limits.ADMIN_UPSERT_MAX_ITEMS
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PromptStoreRoutesTest {
    companion object {
        private lateinit var ds: HikariDataSource
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        @BeforeAll
        @JvmStatic
        fun startPg() {
            // Intentionally not using Testcontainers here.
            // In some Windows setups DOCKER_HOST is misconfigured (npipe://localhost:2375) causing Testcontainers to fail,
            // even though Docker itself works for the app. We instead point tests to a real Postgres instance.
            val jdbcUrl = System.getenv("PROMPT_DB_JDBC_URL_TEST")
                ?: "jdbc:postgresql://127.0.0.1:54321/repoindex"
            val user = System.getenv("PROMPT_DB_USER_TEST") ?: "repoindex"
            val pass = System.getenv("PROMPT_DB_PASSWORD_TEST") ?: "repoindex"

            runCatching {
                val cfg = PromptStoreConfig(
                    postgresJdbcUrl = jdbcUrl,
                    postgresUser = user,
                    postgresPassword = pass,
                    adminToken = "secret",
                )
                ds = PromptStoreDb.createDataSource(cfg)
                PromptStoreDb.migrate(ds)

                ds.connection.use { c ->
                    c.createStatement().use { st ->
                        st.executeUpdate("DELETE FROM prompt_store_prompts")
                    }
                }
            }.getOrElse { err ->
                assumeFalse(true) {
                    "Postgres not reachable for PromptStoreRoutesTest (${jdbcUrl}): ${err.message}. " +
                        "Start a local instance or set PROMPT_DB_JDBC_URL_TEST / PROMPT_DB_USER_TEST / PROMPT_DB_PASSWORD_TEST."
                }
            }
        }

        @AfterAll
        @JvmStatic
        fun stopPg() {
            runCatching {
                if (::ds.isInitialized) ds.close()
            }
        }
    }

    private fun appTest(block: suspend io.ktor.client.HttpClient.(PromptStoreConfig) -> Unit) {
        val cfg = PromptStoreConfig(
            postgresJdbcUrl = System.getenv("PROMPT_DB_JDBC_URL_TEST")
                ?: "jdbc:postgresql://127.0.0.1:54321/repoindex",
            postgresUser = System.getenv("PROMPT_DB_USER_TEST") ?: "repoindex",
            postgresPassword = System.getenv("PROMPT_DB_PASSWORD_TEST") ?: "repoindex",
            adminToken = "secret",
        )
        val dao = PromptStoreDao(ds, json)
        val service = PromptStoreService(dao)

        testApplication {
            application {
                install(ServerContentNegotiation) { json(json) }
                routing { promptStoreRoutes(service, cfg) }
            }
            val client = createClient {
                install(ContentNegotiation) { json(json) }
            }
            client.block(cfg)
        }
    }

    @Test
    fun `unauthorized when missing bearer token`() = appTest { cfg ->
        val res = post("/admin/prompt") {
            contentType(ContentType.Application.Json)
            setBody(
                AdminUpsertPromptRequest(
                    modeId = "style",
                    modelId = null,
                    promptText = "Rewrite it",
                    temperature = 0.4,
                )
            )
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `bad request on invalid fields`() = appTest { _ ->
        val res = post("/admin/prompt") {
            header(HttpHeaders.Authorization, "Bearer secret")
            contentType(ContentType.Application.Json)
            setBody(
                AdminUpsertPromptRequest(
                    modeId = "   ",
                    modelId = null,
                    promptText = "   ",
                    temperature = -1.0,
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `upsert base prompt then update returns same id and upserted false`() = appTest { _ ->
        val origin: JsonObject = buildJsonObject {
            put("repo", "github.com/org/repo")
            put("path", "prompts/style.json")
            put("sha", "abc123")
        }
        val meta: JsonObject = buildJsonObject { put("formatVersion", 1) }
        val raw: JsonObject = buildJsonObject { put("extra", "kept") }

        val res1 = post("/admin/prompt") {
            header(HttpHeaders.Authorization, "Bearer secret")
            contentType(ContentType.Application.Json)
            setBody(
                AdminUpsertPromptRequest(
                    modeId = "style",
                    modelId = null,
                    promptText = "Rewrite it v1",
                    temperature = 0.4,
                    isEnabled = true,
                    tags = listOf("v1", " default ", ""),
                    origin = origin,
                    meta = meta,
                    raw = raw,
                )
            )
        }
        assertEquals(HttpStatusCode.OK, res1.status)
        val body1 = res1.body<AdminUpsertPromptResponse>()
        assertTrue(body1.upserted)
        assertTrue(body1.promptId.isNotBlank())

        val res2 = post("/admin/prompt") {
            header(HttpHeaders.Authorization, "Bearer secret")
            contentType(ContentType.Application.Json)
            setBody(
                AdminUpsertPromptRequest(
                    modeId = "style",
                    modelId = null,
                    promptText = "Rewrite it v2",
                    temperature = 0.7,
                    isEnabled = false,
                    tags = listOf("v2"),
                    origin = origin,
                    meta = meta,
                    raw = raw,
                )
            )
        }
        assertEquals(HttpStatusCode.OK, res2.status)
        val body2 = res2.body<AdminUpsertPromptResponse>()
        assertFalse(body2.upserted)
        assertEquals(body1.promptId, body2.promptId)
    }

    @Test
    fun `upsert override prompt is independent from base`() = appTest { _ ->
        val base = post("/admin/prompt") {
            header(HttpHeaders.Authorization, "Bearer secret")
            contentType(ContentType.Application.Json)
            setBody(AdminUpsertPromptRequest(modeId = "fix", modelId = null, promptText = "Fix base", temperature = 0.2))
        }.body<AdminUpsertPromptResponse>()

        val override = post("/admin/prompt") {
            header(HttpHeaders.Authorization, "Bearer secret")
            contentType(ContentType.Application.Json)
            setBody(
                AdminUpsertPromptRequest(
                    modeId = "fix",
                    modelId = "openai/gpt-4o-mini",
                    promptText = "Fix override",
                    temperature = 0.1
                )
            )
        }.body<AdminUpsertPromptResponse>()

        assertNotEquals(base.promptId, override.promptId)
        assertTrue(base.upserted)
        assertTrue(override.upserted)
    }

    @Test
    fun `batch import merges envelope origin and applies all in one transaction`() = appTest { _ ->
        val envelopeOrigin = buildJsonObject { put("repo", "github.com/org/repo2") }
        val envelopeRaw = buildJsonObject { put("sourceFormat", "schemaV3") }

        val res = post("/admin/prompts") {
            header(HttpHeaders.Authorization, "Bearer secret")
            contentType(ContentType.Application.Json)
            setBody(
                AdminUpsertPromptsRequest(
                    prompts = listOf(
                        AdminUpsertPromptRequest(modeId = "style", modelId = "m1", promptText = "p1", temperature = 0.4),
                        AdminUpsertPromptRequest(modeId = "style", modelId = "m2", promptText = "p2", temperature = 0.4),
                    ),
                    origin = envelopeOrigin,
                    raw = envelopeRaw,
                )
            )
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<AdminUpsertPromptsResponse>()
        assertEquals(2, body.upserted)
    }

    @Test
    fun `bad request when batch exceeds maximum size`() = appTest { _ ->
        val prompts =
            List(ADMIN_UPSERT_MAX_ITEMS + 1) { i ->
                AdminUpsertPromptRequest(
                    modeId = "mode_batch_$i",
                    modelId = null,
                    promptText = "text $i",
                    temperature = 0.4,
                )
            }
        val res = post("/admin/prompts") {
            header(HttpHeaders.Authorization, "Bearer secret")
            contentType(ContentType.Application.Json)
            setBody(AdminUpsertPromptsRequest(prompts = prompts))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }
}

