package openqwoutt.textprocessor.backend.textprocessing.application

data class RegistryResolvedPrompt(
    val promptText: String,
    val temperature: Double,
)

fun interface ModelPromptCatalog {
    fun resolve(modelId: String, modeId: String): RegistryResolvedPrompt?
}
