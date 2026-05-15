package openqwoutt.textprocessor.backend.nvidiaproxy

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import openqwoutt.textprocessor.backend.bootstrap.SlidingWindowRateLimiter
import openqwoutt.textprocessor.backend.bootstrap.resolveRateLimitClientIp
import openqwoutt.textprocessor.backend.security.secureEqualsUtf8Strings

private const val NVIDIA_PROXY_RATE_LIMIT_WINDOW_SEC = 60L

data class NvidiaProxyRateLimit(
    val limiter: SlidingWindowRateLimiter,
    val trustForwardedHeaders: Boolean,
)

fun Route.nvidiaProxyRoutes(service: NvidiaProxyService, apiKey: String, rateLimit: NvidiaProxyRateLimit? = null) {
    route("/api/nvidia/proxy") {
        get("/health") {
            if (!call.requireNvidiaProxyRateLimit(rateLimit)) return@get
            if (!call.requireNvidiaProxyBearer(apiKey)) return@get
            call.respond(service.health())
        }

        get("/capabilities") {
            if (!call.requireNvidiaProxyRateLimit(rateLimit)) return@get
            if (!call.requireNvidiaProxyBearer(apiKey)) return@get
            call.respond(service.capabilities())
        }

        post("/stub") {
            if (!call.requireNvidiaProxyRateLimit(rateLimit)) return@post
            if (!call.requireNvidiaProxyBearer(apiKey)) return@post
            call.respond(
                HttpStatusCode.NotImplemented,
                NvidiaProxyErrorEnvelope(
                    error =
                        NvidiaProxyErrorDetail(
                            message = "NVIDIA proxy passthrough is not implemented yet.",
                            code = "not_implemented",
                        ),
                ),
            )
        }
    }
}

suspend fun ApplicationCall.requireNvidiaProxyBearer(expectedToken: String): Boolean {
    val authHeader = request.headers[HttpHeaders.Authorization].orEmpty()
    val bearer = extractBearerToken(authHeader).orEmpty()
    if (!secureEqualsUtf8Strings(bearer, expectedToken)) {
        respond(
            HttpStatusCode.Unauthorized,
            NvidiaProxyErrorEnvelope(
                error =
                    NvidiaProxyErrorDetail(
                        message = "Unauthorized.",
                        code = "unauthorized",
                    ),
            ),
        )
        return false
    }
    return true
}

private fun extractBearerToken(authorizationHeader: String): String? {
    val schemeAndToken = authorizationHeader.trim().split(Regex("\\s+"), limit = 2)
    if (schemeAndToken.size != 2) return null
    if (!schemeAndToken[0].equals("Bearer", ignoreCase = true)) return null
    return schemeAndToken[1].trim().takeIf { it.isNotEmpty() }
}

private suspend fun ApplicationCall.requireNvidiaProxyRateLimit(rateLimit: NvidiaProxyRateLimit?): Boolean {
    rateLimit ?: return true
    val path = request.path()
    val clientIp =
        resolveRateLimitClientIp(
            remoteHost = request.local.remoteHost,
            trustForwardedHeaders = rateLimit.trustForwardedHeaders,
            xForwardedFor = request.headers["X-Forwarded-For"],
            xRealIp = request.headers["X-Real-IP"],
        )
    if (rateLimit.limiter.tryAcquire("$clientIp|$path")) {
        return true
    }
    response.headers.append(HttpHeaders.RetryAfter, NVIDIA_PROXY_RATE_LIMIT_WINDOW_SEC.toString())
    respond(
        HttpStatusCode.TooManyRequests,
        NvidiaProxyErrorEnvelope(
            error =
                NvidiaProxyErrorDetail(
                    message = "Too many requests; try again later.",
                    code = "too_many_requests",
                ),
        ),
    )
    return false
}
