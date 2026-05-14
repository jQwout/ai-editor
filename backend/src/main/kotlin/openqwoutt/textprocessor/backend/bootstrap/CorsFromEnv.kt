package openqwoutt.textprocessor.backend.bootstrap

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.cors.routing.CORS
import java.net.URI

/**
 * Production-safe CORS:
 * - Default: **disabled** (no plugin) — native Android clients do not need CORS.
 * - `CORS_ALLOW_ANY=true` — previous behaviour (`anyHost()`); log warning.
 * - `CORS_ALLOWED_ORIGINS` — comma-separated absolute origins, e.g. `https://app.example.com,https://admin.example.com`
 */
fun Application.installCorsFromEnvironment() {
    val logger = log
    val allowAny = System.getenv("CORS_ALLOW_ANY")?.equals("true", ignoreCase = true) == true
    val raw = System.getenv("CORS_ALLOWED_ORIGINS")?.trim().orEmpty()

    if (!allowAny && raw.isEmpty()) {
        logger.info("CORS disabled (set CORS_ALLOWED_ORIGINS or CORS_ALLOW_ANY=true for browser clients).")
        return
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true

        if (allowAny) {
            anyHost()
            logger.warn("CORS: anyHost() enabled (CORS_ALLOW_ANY=true) — unsafe on the public Internet.")
        } else {
            raw.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { origin ->
                    val uri =
                        runCatching { URI.create(origin) }.getOrElse {
                            logger.error("CORS: skip invalid origin `$origin`")
                            return@forEach
                        }
                    val scheme = uri.scheme ?: "https"
                    val host = uri.host
                    if (host.isNullOrBlank()) {
                        logger.error("CORS: skip origin without host: `$origin`")
                        return@forEach
                    }
                    val port = uri.port.takeIf { it > 0 }
                    if (port == null) {
                        allowHost(host, schemes = listOf(scheme))
                    } else {
                        allowHost("$host:$port", schemes = listOf(scheme))
                    }
                }
            logger.info("CORS enabled for ${raw.split(',').count { it.trim().isNotEmpty() }} configured origin(s).")
        }
    }
}
