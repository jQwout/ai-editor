package openqwoutt.textstyler.data.settings

import kotlinx.serialization.Serializable

/**
 * Supported AI providers for text processing.
 */
@Serializable
enum class AiProvider(
    val displayName: String,
    val baseUrl: String,
    val model: String,
    val requiresApiKey: Boolean = false
) {
    OPEN_ROUTER(
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        model = "google/gemma-2-2b-it",
        requiresApiKey = true
    ),
    POLLINATIONS(
        displayName = "Pollinations AI",
        baseUrl = "https://gen.pollinations.ai/v1",
        model = "openai/gpt-4o-mini",
        requiresApiKey = false
    ),
    UNCLOSE_AI(
        displayName = "UncloseAI",
        baseUrl = "https://uncloseai.com/v1",
        model = "Qwen/Qwen2.5-7B-Instruct",
        requiresApiKey = false
    ),
    G4F(
        displayName = "G4F (Chatbot)",
        baseUrl = "https://g4f.quantumblack.io/v1",
        model = "gpt-3.5-turbo",
        requiresApiKey = false
    ),
    GROQ(
        displayName = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        model = "llama-3.1-70b-versatile",
        requiresApiKey = true
    );

    companion object {
        fun fromString(value: String): AiProvider {
            return entries.find { it.name == value } ?: OPEN_ROUTER
        }
    }
}

@Serializable
data class AppSettings(
    val backendUrl: String = "http://10.0.2.2:8080",
    val defaultMode: String = "style",
    val autoPaste: Boolean = true,
    val autoCopyResult: Boolean = true,
    val soundEffects: Boolean = false,
    val hapticFeedback: Boolean = true,
    val aiProvider: String = AiProvider.OPEN_ROUTER.name,
    val apiKey: String = ""
) {
    fun toAiProvider(): AiProvider = AiProvider.fromString(aiProvider)
}