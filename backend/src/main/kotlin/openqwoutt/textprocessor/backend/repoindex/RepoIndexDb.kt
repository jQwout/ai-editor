package openqwoutt.textprocessor.backend.repoindex

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object RepoIndexDb {
    fun createDataSource(cfg: RepoIndexConfig): HikariDataSource {
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
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}

