package openqwoutt.textprocessor.backend.modelscatalog

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import io.micrometer.core.instrument.MeterRegistry

val ModelsCatalogServiceKey = AttributeKey<ModelsCatalogService>("ModelsCatalogService")

object ModelsCatalogFeature {
    fun install(app: Application, meterRegistry: MeterRegistry? = null) {
        val enabled = System.getenv("MODELS_CATALOG_ENABLED")?.equals("true", ignoreCase = true) == true
        if (!enabled) {
            app.log.info("Models catalog disabled (set MODELS_CATALOG_ENABLED=true to enable).")
            return
        }

        val cfg = ModelsCatalogConfig.fromEnv()
        val ds: HikariDataSource = ModelsCatalogDb.createDataSource(cfg)
        ModelsCatalogDb.migrate(ds)

        val dao = NvidiaModelDao(ds)
        val service = ModelsCatalogService(dao, meterRegistry)
        app.attributes.put(ModelsCatalogServiceKey, service)

        app.installModelsCatalogSyncRateLimit(cfg)

        app.routing {
            modelsCatalogRoutes(service, cfg)
        }

        app.monitor.subscribe(ApplicationStopped) {
            runCatching { ds.close() }
        }
    }
}
