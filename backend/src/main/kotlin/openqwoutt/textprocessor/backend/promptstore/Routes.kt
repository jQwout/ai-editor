package openqwoutt.textprocessor.backend.promptstore

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.json.JsonObject

fun Route.promptStoreRoutes(service: PromptStoreService, cfg: PromptStoreConfig) {
    post("/admin/prompt") {
        if (!call.requirePromptAdmin(cfg)) return@post
        val body = call.receive<AdminUpsertPromptRequest>()
        val validated = body.validateOrRespond(call) ?: return@post
        val res = service.upsertPrompt(validated)
        call.respond(
            AdminUpsertPromptResponse(
                promptId = res.id,
                upserted = res.upserted,
            )
        )
    }

    post("/admin/prompts") {
        if (!call.requirePromptAdmin(cfg)) return@post
        val body = call.receive<AdminUpsertPromptsRequest>()
        val validated =
            buildList(capacity = body.prompts.size) {
                for (p in body.prompts) {
                    val merged = p.mergeDefaults(origin = body.origin, raw = body.raw)
                    val v = merged.validateOrRespond(call) ?: return@post
                    add(v)
                }
            }
        val results = service.upsertPromptBatch(validated)
        call.respond(AdminUpsertPromptsResponse(upserted = results.size))
    }
}

private suspend fun ApplicationCall.requirePromptAdmin(cfg: PromptStoreConfig): Boolean {
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

private suspend fun AdminUpsertPromptRequest.validateOrRespond(call: ApplicationCall): AdminUpsertPromptRequest? {
    val modeId = modeId.trim()
    val modelId = modelId?.trim()?.takeIf { it.isNotBlank() }
    val promptText = promptText.trim()

    when {
        modeId.isBlank() -> {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "modeId is required"))
            return null
        }
        promptText.isBlank() -> {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "promptText is required"))
            return null
        }
        temperature.isNaN() || temperature < 0.0 || temperature > 2.0 -> {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "temperature must be between 0 and 2"))
            return null
        }
        else -> return copy(
            modeId = modeId,
            modelId = modelId,
            promptText = promptText,
            tags = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        )
    }
}

private fun AdminUpsertPromptRequest.mergeDefaults(origin: JsonObject, raw: JsonObject): AdminUpsertPromptRequest {
    val effOrigin = if (this.origin.isEmpty() && origin.isNotEmpty()) origin else this.origin
    val effRaw = if (this.raw.isEmpty() && raw.isNotEmpty()) raw else this.raw
    return copy(origin = effOrigin, raw = effRaw)
}
