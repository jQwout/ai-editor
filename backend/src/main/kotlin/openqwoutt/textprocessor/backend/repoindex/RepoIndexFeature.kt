package openqwoutt.textprocessor.backend.repoindex

import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json

val PromptRegistryServiceKey = AttributeKey<PromptRegistryService>("PromptRegistryService")
val PublicPromptServiceKey = AttributeKey<PublicPromptService>("PublicPromptService")
val PublicRepoIngestorKey = AttributeKey<PublicRepoIngestor>("PublicRepoIngestor")
val PublicRepoSourceServiceKey = AttributeKey<PublicRepoSourceService>("PublicRepoSourceService")
val PublicRepoRefresherKey = AttributeKey<PublicRepoRefresher>("PublicRepoRefresher")

object RepoIndexFeature {
    fun install(app: Application) {
        val enabled = System.getenv("REPO_INDEX_ENABLED")?.equals("true", ignoreCase = true) == true
        if (!enabled) {
            app.log.info("Prompt registry disabled (set REPO_INDEX_ENABLED=true to enable).")
            return
        }

        val cfg = RepoIndexConfig.fromEnv()
        val ds: HikariDataSource = RepoIndexDb.createDataSource(cfg)
        RepoIndexDb.migrate(ds)

        val dao = PromptRegistryDao(ds)
        val service = PromptRegistryService(dao)
        app.attributes.put(PromptRegistryServiceKey, service)

        val publicPromptDao = PublicPromptDao(ds)
        val publicPromptService = PublicPromptService(publicPromptDao)
        app.attributes.put(PublicPromptServiceKey, publicPromptService)

        val repoSourceDao = PublicRepoSourceDao(ds)
        val repoSourceService = PublicRepoSourceService(repoSourceDao)
        app.attributes.put(PublicRepoSourceServiceKey, repoSourceService)

        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val http = HttpClient(CIO) {
            install(ClientContentNegotiation) { json(json) }
        }
        val ingestor = PublicRepoIngestor(http = http, cfg = cfg, publicPromptService = publicPromptService)
        app.attributes.put(PublicRepoIngestorKey, ingestor)

        val refresher = PublicRepoRefresher(repoSourceService = repoSourceService, ingestor = ingestor)
        app.attributes.put(PublicRepoRefresherKey, refresher)

        val schedulerJob = PublicRepoScheduler.start(app, refresher = refresher)

        app.routing {
            repoIndexRoutes(service, cfg)
            publicPromptRoutes(publicPromptService, cfg)
            publicRepoIngestRoutes(ingestor, cfg)
            publicRepoSourceRoutes(repoSourceService, cfg)
            publicRepoRefreshRoutes(refresher, cfg)
        }

        app.monitor.subscribe(ApplicationStopped) {
            runCatching { ds.close() }
            runCatching { http.close() }
            runCatching { schedulerJob?.cancel() }
        }
    }
}
