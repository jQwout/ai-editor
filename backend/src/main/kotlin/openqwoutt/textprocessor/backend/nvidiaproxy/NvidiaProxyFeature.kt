package openqwoutt.textprocessor.backend.nvidiaproxy

import io.ktor.server.application.Application
import io.ktor.server.application.log
import openqwoutt.textprocessor.backend.bootstrap.SlidingWindowRateLimiter
import io.ktor.server.routing.routing

object NvidiaProxyFeature {
    private const val RATE_LIMIT_WINDOW_MS = 60_000L

    fun install(
        app: Application,
        env: (String) -> String? = { name -> System.getenv(name) },
    ) {
        val config = NvidiaProxyConfig.fromEnvironment(env)
        if (config == null) {
            app.log.info("NVIDIA proxy disabled (set NVIDIA_PROXY_ENABLED=true to enable).")
            return
        }

        val rateLimit =
            config.rateLimitPerMinute?.let { perMinute ->
                app.log.info(
                    "NVIDIA proxy rate limit enabled: $perMinute requests/minute per client " +
                        "(trustForwarded=${config.rateLimitTrustForwarded})",
                )
                NvidiaProxyRateLimit(
                    limiter = SlidingWindowRateLimiter(maxRequests = perMinute, windowMillis = RATE_LIMIT_WINDOW_MS),
                    trustForwardedHeaders = config.rateLimitTrustForwarded,
                )
            }

        val service = NvidiaProxyService(config)
        app.routing {
            nvidiaProxyRoutes(service = service, apiKey = config.apiKey, rateLimit = rateLimit)
        }
    }
}
