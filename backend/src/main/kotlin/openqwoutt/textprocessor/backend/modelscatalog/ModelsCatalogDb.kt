package openqwoutt.textprocessor.backend.modelscatalog

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object ModelsCatalogDb {
    fun createDataSource(cfg: ModelsCatalogConfig): HikariDataSource {
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
            .locations("classpath:db/modelscatalog")
            .table("flyway_modelscatalog_schema_history")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate()
    }
}
