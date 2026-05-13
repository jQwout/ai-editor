package openqwoutt.textprocessor.backend.promptstore

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object PromptStoreDb {
    fun createDataSource(cfg: PromptStoreConfig): HikariDataSource {
        val hc = HikariConfig().apply {
            jdbcUrl = cfg.postgresJdbcUrl
            username = cfg.postgresUser
            password = cfg.postgresPassword
            maximumPoolSize = 5
            isAutoCommit = true
        }
        return HikariDataSource(hc)
    }

    fun migrate(ds: DataSource) {
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/promptstore")
            // Separate Flyway history table to avoid version clashes with repoindex migrations
            .table("flyway_promptstore_schema_history")
            // RepoIndex may already have created tables in public; we still want independent history for promptstore.
            .baselineOnMigrate(true)
            // Baseline at 0 so V1 still runs even if public schema isn't empty.
            .baselineVersion("0")
            .load()
            .migrate()
    }
}
