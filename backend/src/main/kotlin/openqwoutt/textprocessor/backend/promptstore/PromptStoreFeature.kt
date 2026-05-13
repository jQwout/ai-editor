package openqwoutt.textprocessor.backend.promptstore

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

val PromptStoreServiceKey = AttributeKey<PromptStoreService>("PromptStoreService")

object PromptStoreFeature {
    fun install(app: Application, json: Json) {
        val enabled = System.getenv("PROMPT_STORE_ENABLED")?.equals("true", ignoreCase = true) == true
        if (!enabled) {
            app.log.info("Prompt store disabled (set PROMPT_STORE_ENABLED=true to enable).")
            return
        }

        val cfg = PromptStoreConfig.fromEnv()
        val ds: HikariDataSource = PromptStoreDb.createDataSource(cfg)
        PromptStoreDb.migrate(ds)

        val dao = PromptStoreDao(ds, json)
        val service = PromptStoreService(dao)
        app.attributes.put(PromptStoreServiceKey, service)

        app.routing {
            promptStoreRoutes(service, cfg)
        }

        app.monitor.subscribe(ApplicationStopped) {
            runCatching { ds.close() }
        }
    }
}
