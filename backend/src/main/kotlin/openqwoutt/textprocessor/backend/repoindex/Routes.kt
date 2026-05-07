package openqwoutt.textprocessor.backend.repoindex

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.repoIndexRoutes(service: PromptRegistryService, cfg: RepoIndexConfig) {
    get("/repoindex/health") {
        call.respond(mapOf("status" to "ok"))
    }

    get("/repoindex/models") {
        call.respond(
            service.listEnabledModels().map {
                AiModelDto(modelId = it.modelId, displayName = it.displayName)
            }
        )
    }

    get("/repoindex/prompts") {
        val modelId = call.request.queryParameters["model"]?.trim().orEmpty()
        if (modelId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "model query parameter is required"))
            return@get
        }
        val prompts = service.listEffectivePromptsForModel(modelId)
        if (prompts.isEmpty()) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Unknown or disabled model"))
            return@get
        }
        call.respond(
            PromptsForModelResponseDto(
                modelId = modelId,
                prompts = prompts.map {
                    EffectivePromptDto(modeId = it.modeId, promptText = it.promptText, temperature = it.temperature)
                },
            )
        )
    }

    get("/repoindex/prompt") {
        val modelId = call.request.queryParameters["model"]?.trim().orEmpty()
        val modeId = call.request.queryParameters["mode"]?.trim().orEmpty()
        if (modelId.isBlank() || modeId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "model and mode are required"))
            return@get
        }
        val eff = service.getEffectivePrompt(modelId, modeId)
        if (eff == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Unknown model/mode or model disabled"))
            return@get
        }
        call.respond(
            SinglePromptResponseDto(
                modelId = modelId,
                modeId = modeId,
                promptText = eff.promptText,
                temperature = eff.temperature,
            )
        )
    }

    post("/repoindex/admin/models") {
        if (!call.requireAdmin(cfg)) return@post
        val body = call.receive<AdminUpsertModelsRequest>()
        service.upsertModels(body.models)
        call.respond(mapOf("status" to "ok", "upserted" to body.models.size))
    }

    post("/repoindex/admin/prompts") {
        if (!call.requireAdmin(cfg)) return@post
        val body = call.receive<AdminUpsertPromptsRequest>()
        service.upsertPrompts(body)
        call.respond(
            mapOf(
                "status" to "ok",
                "basePrompts" to body.basePrompts.size,
                "overrides" to body.overrides.size,
            )
        )
    }
}

fun Route.publicPromptRoutes(publicPromptService: PublicPromptService, cfg: RepoIndexConfig) {
    get("/repoindex/public/prompts") {
        val source = call.request.queryParameters["source"]?.trim()?.takeIf { it.isNotBlank() }
        val tag = call.request.queryParameters["tag"]?.trim()?.takeIf { it.isNotBlank() }
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val prompts = publicPromptService.listPublicPrompts(source = source, tag = tag, limit = limit)
        call.respond(
            PublicPromptsResponseDto(
                prompts = prompts.map {
                    PublicPromptDto(
                        source = it.source,
                        path = it.path,
                        sha = it.sha,
                        itemKey = it.itemKey,
                        tags = it.tags,
                        promptText = it.promptText,
                    )
                }
            )
        )
    }

    post("/repoindex/admin/public-prompts") {
        if (!call.requireAdmin(cfg)) return@post
        val body = call.receive<AdminUpsertPublicPromptsRequest>()
        val upserted = publicPromptService.upsertPrompts(
            body.prompts.map {
                PublicPromptRow(
                    source = it.source.trim(),
                    path = it.path.trim(),
                    sha = it.sha.trim(),
                    itemKey = it.itemKey.trim(),
                    tags = it.tags.map { t -> t.trim() }.filter { it.isNotBlank() }.distinct(),
                    promptText = it.promptText,
                )
            }
        )
        call.respond(mapOf("status" to "ok", "upserted" to upserted))
    }
}

fun Route.publicRepoIngestRoutes(ingestor: PublicRepoIngestor, cfg: RepoIndexConfig) {
    post("/repoindex/admin/public-repos/ingest") {
        if (!call.requireAdmin(cfg)) return@post
        val body = call.receive<AdminIngestPublicReposRequest>()
        val results = buildList {
            for (repoUrl in body.repoUrls) {
                add(ingestor.ingestGithubRepo(repoUrl))
            }
        }
        call.respond(AdminIngestResultsResponseDto(results = results))
    }
}

fun Route.publicRepoSourceRoutes(repoSourceService: PublicRepoSourceService, cfg: RepoIndexConfig) {
    post("/repoindex/admin/public-repos") {
        if (!call.requireAdmin(cfg)) return@post
        val body = call.receive<AdminUpsertPublicReposRequest>()
        repoSourceService.upsertGithubRepos(body.repoUrls)
        call.respond(AdminUpsertedResponseDto(upserted = body.repoUrls.size))
    }

    get("/repoindex/admin/public-repos") {
        if (!call.requireAdmin(cfg)) return@get
        call.respond(
            AdminPublicReposResponseDto(
                repos = repoSourceService.listEnabled().map {
                    PublicRepoStateDto(
                        repoUrl = it.repoUrl,
                        provider = it.provider,
                        defaultBranch = it.defaultBranch,
                        lastSeenCommit = it.lastSeenCommit,
                        lastIngestedCommit = it.lastIngestedCommit,
                    )
                }
            )
        )
    }
}

fun Route.publicRepoRefreshRoutes(refresher: PublicRepoRefresher, cfg: RepoIndexConfig) {
    post("/repoindex/admin/public-repos/refresh") {
        if (!call.requireAdmin(cfg)) return@post
        val results = refresher.refreshOnce(call.application)
        call.respond(AdminRefreshResultsResponseDto(results = results))
    }
}

private suspend fun ApplicationCall.requireAdmin(cfg: RepoIndexConfig): Boolean {
    val expected = cfg.adminToken
    if (expected == null) {
        respond(HttpStatusCode.NotFound, mapOf("error" to "Admin API disabled"))
        return false
    }
    val header = request.headers[HttpHeaders.Authorization].orEmpty()
    val token = header.removePrefix("Bearer ").trim()
    if (token != expected) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        return false
    }
    return true
}
