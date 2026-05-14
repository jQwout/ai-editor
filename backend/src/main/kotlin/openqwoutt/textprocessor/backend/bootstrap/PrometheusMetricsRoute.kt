package openqwoutt.textprocessor.backend.bootstrap

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import openqwoutt.textprocessor.backend.security.secureEqualsUtf8Strings

/**
 * Optional Prometheus scrape endpoint for Micrometer metrics.
 *
 * - `METRICS_PROMETHEUS_ENABLED=true` — registers [PrometheusMeterRegistry] and exposes `GET /metrics/prometheus`.
 * - `METRICS_SCRAPE_BEARER_TOKEN` — if set, requires `Authorization: Bearer <token>` (constant-time compare).
 */
fun createOptionalPrometheusRegistry(): PrometheusMeterRegistry? {
    if (System.getenv("METRICS_PROMETHEUS_ENABLED")?.equals("true", ignoreCase = true) != true) {
        return null
    }
    return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}

fun Application.installPrometheusScrapeRoute(registry: PrometheusMeterRegistry) {
    environment.log.info("Prometheus metrics enabled at GET /metrics/prometheus")
    routing {
        get("/metrics/prometheus") {
            val expected = System.getenv("METRICS_SCRAPE_BEARER_TOKEN")?.trim()?.takeIf { it.isNotEmpty() }
            if (expected != null) {
                val bearer = call.request.header(HttpHeaders.Authorization).orEmpty().removePrefix("Bearer ").trim()
                if (!secureEqualsUtf8Strings(bearer, expected)) {
                    call.respondText("Unauthorized", ContentType.Text.Plain, HttpStatusCode.Unauthorized)
                    return@get
                }
            }
            val body = registry.scrape()
            call.respondText(body, ContentType.parse("text/plain; version=0.0.4; charset=utf-8"))
        }
    }
}
