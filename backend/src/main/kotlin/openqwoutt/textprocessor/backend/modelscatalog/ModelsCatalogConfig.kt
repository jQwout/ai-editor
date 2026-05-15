package openqwoutt.textprocessor.backend.modelscatalog

data class ModelsCatalogConfig(
    val postgresJdbcUrl: String,
    val postgresUser: String,
    val postgresPassword: String,
    val adminToken: String?,
    val catalogMaxAgeSec: Int,
    val syncRateLimitPerMinute: Int?,
    val syncRateLimitTrustForwarded: Boolean,
) {
    companion object {
        fun fromEnv(): ModelsCatalogConfig {
            val allowPgFallback =
                System.getenv("MODELS_CATALOG_DEV_FALLBACK_PG")?.equals("true", ignoreCase = true) == true

            return ModelsCatalogConfig(
                postgresJdbcUrl =
                    envRequired("MODELS_CATALOG_PG_JDBC_URL", "PG_JDBC_URL", allowPgFallback),
                postgresUser = envRequired("MODELS_CATALOG_PG_USER", "PG_USER", allowPgFallback),
                postgresPassword =
                    envRequired("MODELS_CATALOG_PG_PASSWORD", "PG_PASSWORD", allowPgFallback),
                adminToken =
                    System.getenv("MODELS_CATALOG_ADMIN_TOKEN")?.trim()?.takeIf { it.isNotBlank() },
                catalogMaxAgeSec =
                    System.getenv("MODELS_CATALOG_MAX_AGE_SEC")?.toIntOrNull()?.coerceAtLeast(0) ?: 3600,
                syncRateLimitPerMinute =
                    System.getenv("MODELS_CATALOG_SYNC_RATE_LIMIT_PER_MINUTE")
                        ?.toIntOrNull()
                        ?.takeIf { it > 0 },
                syncRateLimitTrustForwarded =
                    System.getenv("MODELS_CATALOG_SYNC_RATE_LIMIT_TRUST_FORWARDED")
                        ?.equals("true", ignoreCase = true) == true,
            )
        }

        private fun envRequired(primary: String, fallback: String, allowFallback: Boolean): String {
            System.getenv(primary)?.takeIf { it.isNotBlank() }?.let { return it }
            if (allowFallback) {
                System.getenv(fallback)?.takeIf { it.isNotBlank() }?.let { return it }
            }
            return error(
                if (allowFallback) {
                    "Missing env $primary (or fallback $fallback)"
                } else {
                    "Missing env $primary (set MODELS_CATALOG_DEV_FALLBACK_PG=true to allow $fallback on dev)"
                },
            )
        }
    }
}
