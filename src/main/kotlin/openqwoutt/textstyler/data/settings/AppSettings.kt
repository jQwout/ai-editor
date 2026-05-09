package openqwoutt.textstyler.data.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
        model = "openai/gpt-4o-mini",
        requiresApiKey = true
    ),
    NVIDIA(
        displayName = "NVIDIA NIM",
        baseUrl = "https://integrate.api.nvidia.com/v1",
        model = "meta/llama-3.1-70b-instruct",
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
    val aiModel: String = "",
    @Transient
    val apiKey: String = "",
    val saveHistory: Boolean = true,
    val useBackend: Boolean = false
) {
    fun toAiProvider(): AiProvider = AiProvider.fromString(aiProvider)

    fun effectiveModel(): String {
        return aiModel.takeIf { it.isNotBlank() } ?: toAiProvider().model
    }
}
