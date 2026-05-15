package openqwoutt.textprocessor.backend.modelscatalog

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import openqwoutt.textprocessor.backend.bootstrap.SlidingWindowRateLimiter
import openqwoutt.textprocessor.backend.bootstrap.resolveRateLimitClientIp

private const val SYNC_PATH = "/models-catalog/admin/sync"
private const val RATE_LIMIT_WINDOW_SEC = 60L

/**
 * Optional per-IP rate limit for catalog admin sync.
 *
 * Env: `MODELS_CATALOG_SYNC_RATE_LIMIT_PER_MINUTE` — if unset or non-positive, no limit.
 * `MODELS_CATALOG_SYNC_RATE_LIMIT_TRUST_FORWARDED=true` — trust X-Forwarded-For (only behind a trusted proxy).
 */
fun Application.installModelsCatalogSyncRateLimit(cfg: ModelsCatalogConfig) {
    val perMinute = cfg.syncRateLimitPerMinute ?: return
    val trustForwarded = cfg.syncRateLimitTrustForwarded
    val limiter = SlidingWindowRateLimiter(maxRequests = perMinute, windowMillis = RATE_LIMIT_WINDOW_SEC * 1000L)
    log.info(
        "Models catalog sync rate limit: $perMinute requests/minute per client " +
            "(trustForwarded=$trustForwarded)",
    )
    intercept(ApplicationCallPipeline.Call) {
        if (call.request.httpMethod != HttpMethod.Post || call.request.path() != SYNC_PATH) {
            return@intercept
        }
        val clientIp =
            resolveRateLimitClientIp(
                remoteHost = call.request.local.remoteHost,
                trustForwardedHeaders = trustForwarded,
                xForwardedFor = call.request.headers["X-Forwarded-For"],
                xRealIp = call.request.headers["X-Real-IP"],
            )
        if (!limiter.tryAcquire("$clientIp|$SYNC_PATH")) {
            call.response.headers.append(HttpHeaders.RetryAfter, RATE_LIMIT_WINDOW_SEC.toString())
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf("error" to "Too many sync requests; try again later."),
            )
            finish()
        }
    }
}
