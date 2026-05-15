package openqwoutt.textprocessor.backend.admin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import openqwoutt.textprocessor.backend.limits.ADMIN_UPSERT_MAX_ITEMS
import openqwoutt.textprocessor.backend.security.secureEqualsUtf8Strings

suspend fun ApplicationCall.requireAdminBearerToken(adminToken: String?): Boolean {
    val expected = adminToken
    if (expected == null) {
        respond(HttpStatusCode.NotFound, mapOf("error" to "Admin API disabled"))
        return false
    }
    val header = request.headers[HttpHeaders.Authorization].orEmpty()
    val token = header.removePrefix("Bearer ").trim()
    if (!secureEqualsUtf8Strings(token, expected)) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        return false
    }
    return true
}

suspend fun ApplicationCall.requireAdminBatchLimit(count: Int, label: String): Boolean {
    if (count <= ADMIN_UPSERT_MAX_ITEMS) return true
    respond(
        HttpStatusCode.BadRequest,
        mapOf(
            "error" to
                "$label exceeds maximum size ($ADMIN_UPSERT_MAX_ITEMS). Split into smaller requests.",
        ),
    )
    return false
}
