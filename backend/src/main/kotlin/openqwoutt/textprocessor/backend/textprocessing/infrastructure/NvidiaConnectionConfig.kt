package openqwoutt.textprocessor.backend.textprocessing.infrastructure

/**
 * NVIDIA API catalog / NIM OpenAI-compatible chat completions.
 *
 * Env:
 * - `NVIDIA_API_KEY` — required when the server is started with NVIDIA as the active LLM provider
 * - `NVIDIA_CHAT_COMPLETIONS_URL` — optional, default NVIDIA integrate endpoint
 * - `NVIDIA_MODEL` — default model id when the client does not send `model` (fallback routing id)
 */
data class NvidiaConnectionConfig(
    val apiKey: String,
    val chatCompletionsUrl: String,
    val defaultModelId: String,
) {
    companion object {
        const val DEFAULT_CHAT_URL: String = "https://integrate.api.nvidia.com/v1/chat/completions"

        /** [env] defaults to [System.getenv]; tests may pass a stub map lookup. */
        fun fromEnvironment(env: (String) -> String? = { name -> System.getenv(name) }): NvidiaConnectionConfig {
            val apiKey = env("NVIDIA_API_KEY") ?: error("NVIDIA_API_KEY is required")
            return NvidiaConnectionConfig(
                apiKey = apiKey,
                chatCompletionsUrl =
                    env("NVIDIA_CHAT_COMPLETIONS_URL")?.trim()?.takeIf { it.isNotEmpty() }
                        ?: DEFAULT_CHAT_URL,
                defaultModelId =
                    env("NVIDIA_MODEL")?.trim()?.takeIf { it.isNotEmpty() }
                        ?: "meta/llama-3.1-8b-instruct",
            )
        }
    }
}
