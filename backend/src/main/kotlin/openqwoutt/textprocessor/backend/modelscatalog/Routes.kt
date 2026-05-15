package openqwoutt.textprocessor.backend.modelscatalog

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import openqwoutt.textprocessor.backend.admin.requireAdminBatchLimit
import openqwoutt.textprocessor.backend.admin.requireAdminBearerToken
import java.time.format.DateTimeFormatter

fun Route.modelsCatalogRoutes(service: ModelsCatalogService, cfg: ModelsCatalogConfig) {
    get("/models-catalog/health") {
        call.respond(mapOf("status" to "ok"))
    }

    get("/models-catalog/models") {
        call.respondNvidiaCatalog(service, cfg)
    }

    post("/models-catalog/admin/sync") {
        if (!call.requireAdminBearerToken(cfg.adminToken)) return@post
        val body = call.receive<AdminSyncModelsRequest>()
        if (!call.requireAdminBatchLimit(body.models.size, "models")) return@post
        if (body.models.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "models must not be empty"))
            return@post
        }
        val result =
            runCatching {
                service.sync(models = body.models, disableMissing = body.disableMissing)
            }.getOrElse { err ->
                if (err is IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (err.message ?: "sync failed")))
                } else {
                    call.application.log.error("models-catalog sync failed", err)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "sync failed"))
                }
                return@post
            }
        call.respond(
            AdminSyncModelsResponseDto(
                upserted = result.upserted,
                unchanged = result.unchanged,
                disabled = result.disabled,
                updatedAt = DateTimeFormatter.ISO_INSTANT.format(result.updatedAt),
            ),
        )
    }
}

private suspend fun ApplicationCall.respondNvidiaCatalog(
    service: ModelsCatalogService,
    cfg: ModelsCatalogConfig,
) {
    val snapshot = service.catalog()
    val etag = catalogEtag(snapshot.revision)
    val maxAge = cfg.catalogMaxAgeSec

    response.headers.append(HttpHeaders.ETag, etag)
    response.headers.append(HttpHeaders.CacheControl, "public, max-age=$maxAge")

    val ifNoneMatch = request.headers[HttpHeaders.IfNoneMatch]
    if (ifNoneMatchSatisfies(ifNoneMatch, etag)) {
        respond(HttpStatusCode.NotModified)
        return
    }

    respond(
        ModelCatalogResponseDto(
            updatedAt = DateTimeFormatter.ISO_INSTANT.format(snapshot.updatedAt),
            models =
                snapshot.models.map {
                    CatalogModelDto(modelId = it.modelId, displayName = it.displayName)
                },
        ),
    )
}
