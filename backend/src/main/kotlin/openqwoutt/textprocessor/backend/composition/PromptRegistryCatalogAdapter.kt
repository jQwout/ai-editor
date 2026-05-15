package openqwoutt.textprocessor.backend.composition

import openqwoutt.textprocessor.backend.repoindex.PromptRegistryService
import openqwoutt.textprocessor.backend.textprocessing.application.ModelPromptCatalog
import openqwoutt.textprocessor.backend.textprocessing.application.RegistryResolvedPrompt

class PromptRegistryCatalogAdapter(
    private val registry: PromptRegistryService,
) : ModelPromptCatalog {
    override fun resolve(modelId: String, modeId: String): RegistryResolvedPrompt? =
        registry.resolveEffectivePrompt(modelId, modeId)?.let {
            RegistryResolvedPrompt(promptText = it.promptText, temperature = it.temperature)
        }
}
