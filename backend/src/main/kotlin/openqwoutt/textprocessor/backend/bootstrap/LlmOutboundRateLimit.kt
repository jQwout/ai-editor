package openqwoutt.textprocessor.backend.bootstrap

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
import openqwoutt.textprocessor.backend.promptproxy.PromptProxyErrorDetail
import openqwoutt.textprocessor.backend.promptproxy.PromptProxyErrorEnvelope
import openqwoutt.textprocessor.backend.textprocessing.adapter.http.ErrorResponseDto

private val rateLimitedPaths =
    setOf(
        "/api/text/process",
        "/api/prompt/proxy",
    )

private const val RATE_LIMIT_WINDOW_SEC = 60L

/**
 * Optional per-IP rate limit for outbound LLM HTTP entrypoints.
 *
 * Env:
 * - `LLM_RATE_LIMIT_REQUESTS_PER_MINUTE` — if unset or non-positive, no limit is installed.
 * - `LLM_RATE_LIMIT_TRUST_FORWARDED=true` — use `X-Forwarded-For` / `X-Real-IP` (only behind a **trusted** proxy).
 */
fun Application.installLlmOutboundRateLimitFromEnvironment() {
    val perMinute =
        System.getenv("LLM_RATE_LIMIT_REQUESTS_PER_MINUTE")?.toIntOrNull()?.takeIf { it > 0 }
            ?: return
    val trustForwarded =
        System.getenv("LLM_RATE_LIMIT_TRUST_FORWARDED")?.equals("true", ignoreCase = true) == true
    val limiter = SlidingWindowRateLimiter(maxRequests = perMinute, windowMillis = RATE_LIMIT_WINDOW_SEC * 1000L)
    log.info(
        "LLM rate limit enabled: $perMinute requests/minute per client for $rateLimitedPaths " +
            "(trustForwarded=$trustForwarded)",
    )
    intercept(ApplicationCallPipeline.Call) {
        if (call.request.httpMethod != HttpMethod.Post) {
            return@intercept
        }
        val path = call.request.path()
        if (path !in rateLimitedPaths) {
            return@intercept
        }
        val clientIp =
            resolveRateLimitClientIp(
                remoteHost = call.request.local.remoteHost,
                trustForwardedHeaders = trustForwarded,
                xForwardedFor = call.request.headers["X-Forwarded-For"],
                xRealIp = call.request.headers["X-Real-IP"],
            )
        if (!limiter.tryAcquire("$clientIp|$path")) {
            call.response.headers.append(HttpHeaders.RetryAfter, RATE_LIMIT_WINDOW_SEC.toString())
            when (path) {
                "/api/prompt/proxy" ->
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        PromptProxyErrorEnvelope(
                            error =
                                PromptProxyErrorDetail(
                                    message = "Too many requests; try again later.",
                                ),
                        ),
                    )
                else ->
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ErrorResponseDto(error = "Too many requests; try again later."),
                    )
            }
            finish()
        }
    }
}
