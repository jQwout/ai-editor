package openqwoutt.textprocessor.backend.modelscatalog

import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Assumptions.assumeFalse

object ModelsCatalogTestSupport {
    lateinit var ds: HikariDataSource
        private set

    val jdbcUrl: String
        get() =
            System.getenv("MODELS_CATALOG_PG_JDBC_URL_TEST")
                ?: System.getenv("PROMPT_DB_JDBC_URL_TEST")
                ?: "jdbc:postgresql://127.0.0.1:54321/repoindex"

    val pgUser: String
        get() =
            System.getenv("MODELS_CATALOG_PG_USER_TEST")
                ?: System.getenv("PROMPT_DB_USER_TEST")
                ?: "repoindex"

    val pgPassword: String
        get() =
            System.getenv("MODELS_CATALOG_PG_PASSWORD_TEST")
                ?: System.getenv("PROMPT_DB_PASSWORD_TEST")
                ?: "repoindex"

    fun initPostgres() {
        runCatching {
            val cfg = testConfig(adminToken = "secret")
            ds = ModelsCatalogDb.createDataSource(cfg)
            ModelsCatalogDb.migrate(ds)
        }.getOrElse { err ->
            assumeFalse(true) {
                "Postgres not reachable for models-catalog tests ($jdbcUrl): ${err.message}"
            }
        }
    }

    fun closePostgres() {
        runCatching {
            if (::ds.isInitialized) ds.close()
        }
    }

    fun clearModels() {
        ds.connection.use { c ->
            c.createStatement().use { st ->
                st.executeUpdate("DELETE FROM nvidia_model")
            }
        }
    }

    fun testConfig(
        adminToken: String? = "secret",
        catalogMaxAgeSec: Int = 3600,
        syncRateLimitPerMinute: Int? = null,
    ): ModelsCatalogConfig =
        ModelsCatalogConfig(
            postgresJdbcUrl = jdbcUrl,
            postgresUser = pgUser,
            postgresPassword = pgPassword,
            adminToken = adminToken,
            catalogMaxAgeSec = catalogMaxAgeSec,
            syncRateLimitPerMinute = syncRateLimitPerMinute,
            syncRateLimitTrustForwarded = false,
        )

    fun dao(): NvidiaModelDao = NvidiaModelDao(ds)

    fun service(): ModelsCatalogService = ModelsCatalogService(dao())
}
