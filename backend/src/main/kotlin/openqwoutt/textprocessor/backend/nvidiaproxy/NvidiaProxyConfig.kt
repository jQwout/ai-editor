package openqwoutt.textprocessor.backend.nvidiaproxy

/**
 * Infrastructure config for standalone NVIDIA proxy feature.
 *
 * Env:
 * - `NVIDIA_PROXY_ENABLED` — enable routes when `true`
 * - `NVIDIA_PROXY_API_KEY` — required bearer secret when feature is enabled
 * - `NVIDIA_PROXY_UPSTREAM_BASE_URL` — optional base URL for future passthrough routes
 * - `NVIDIA_PROXY_REQUEST_TIMEOUT_MS` — optional request/socket timeout for future upstream calls
 * - `NVIDIA_PROXY_CONNECT_TIMEOUT_MS` — optional connect timeout for future upstream calls
 * - `NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE` — optional per-IP limit for infra routes
 * - `NVIDIA_PROXY_RATE_LIMIT_TRUST_FORWARDED` — trust X-Forwarded-For / X-Real-IP (only behind trusted proxy)
 */
data class NvidiaProxyConfig(
    val apiKey: String,
    val upstreamBaseUrl: String,
    val requestTimeoutMs: Long,
    val connectTimeoutMs: Long,
    val rateLimitPerMinute: Int?,
    val rateLimitTrustForwarded: Boolean,
) {
    companion object {
        const val DEFAULT_UPSTREAM_BASE_URL: String = "https://integrate.api.nvidia.com/v1"
        const val DEFAULT_REQUEST_TIMEOUT_MS: Long = 120_000L
        const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 15_000L

        /**
         * Returns null when feature is disabled.
         * Throws when feature is enabled but required envs are invalid.
         */
        fun fromEnvironment(env: (String) -> String? = { name -> System.getenv(name) }): NvidiaProxyConfig? {
            val enabled = env("NVIDIA_PROXY_ENABLED")?.equals("true", ignoreCase = true) == true
            if (!enabled) return null

            val apiKey =
                env("NVIDIA_PROXY_API_KEY")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error("NVIDIA_PROXY_API_KEY is required when NVIDIA_PROXY_ENABLED=true")

            val upstreamBaseUrl =
                env("NVIDIA_PROXY_UPSTREAM_BASE_URL")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_UPSTREAM_BASE_URL

            val requestTimeoutMs =
                env("NVIDIA_PROXY_REQUEST_TIMEOUT_MS")
                    ?.toLongOrNull()
                    ?.coerceAtLeast(5_000L)
                    ?: DEFAULT_REQUEST_TIMEOUT_MS

            val connectTimeoutMs =
                env("NVIDIA_PROXY_CONNECT_TIMEOUT_MS")
                    ?.toLongOrNull()
                    ?.coerceAtLeast(1_000L)
                    ?: DEFAULT_CONNECT_TIMEOUT_MS

            val rateLimitPerMinute =
                env("NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { raw ->
                        val parsed =
                            raw.toIntOrNull()
                                ?: error("NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE must be an integer > 0")
                        if (parsed <= 0) {
                            error("NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE must be an integer > 0")
                        }
                        parsed
                    }

            val rateLimitTrustForwarded =
                env("NVIDIA_PROXY_RATE_LIMIT_TRUST_FORWARDED")?.equals("true", ignoreCase = true) == true

            return NvidiaProxyConfig(
                apiKey = apiKey,
                upstreamBaseUrl = upstreamBaseUrl,
                requestTimeoutMs = requestTimeoutMs,
                connectTimeoutMs = connectTimeoutMs,
                rateLimitPerMinute = rateLimitPerMinute,
                rateLimitTrustForwarded = rateLimitTrustForwarded,
            )
        }
    }
}
