package openqwoutt.textprocessor.backend.textprocessing.infrastructure

data class OpenRouterConnectionConfig(
    val apiKey: String,
    val defaultRoutingModelId: String,
    val appTitle: String,
    val httpReferer: String?,
) {
    companion object {
        /** [env] defaults to [System.getenv]; tests may pass a stub map lookup. */
        fun fromEnvironment(env: (String) -> String? = { name -> System.getenv(name) }): OpenRouterConnectionConfig {
            val apiKey = env("OPENROUTER_API_KEY") ?: error("OPENROUTER_API_KEY is required")
            return OpenRouterConnectionConfig(
                apiKey = apiKey,
                defaultRoutingModelId = env("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini",
                appTitle = env("OPENROUTER_APP_TITLE") ?: "Side AI Editor",
                httpReferer = env("OPENROUTER_HTTP_REFERER"),
            )
        }
    }
}
