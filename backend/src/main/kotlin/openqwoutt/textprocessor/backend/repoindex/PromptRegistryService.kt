package openqwoutt.textprocessor.backend.repoindex

class PromptRegistryService(
    private val dao: PromptRegistryDao,
) {
    fun listEnabledModels(): List<AiModelRow> = dao.listEnabledModels()

    fun resolveEffectivePrompt(modelId: String, modeId: String): EffectivePromptRow? =
        dao.resolveEffectivePrompt(modelId, modeId)

    fun listEffectivePromptsForModel(modelId: String): List<EffectivePromptEntry> {
        val model = dao.findEnabledModelByModelId(modelId) ?: return emptyList()
        val base = dao.listBasePrompts()
        return base.map { p ->
            val eff = dao.resolveEffectivePrompt(model.modelId, p.modeId)
                ?: error("resolveEffectivePrompt returned null for existing model/mode")
            EffectivePromptEntry(
                modeId = p.modeId,
                promptText = eff.promptText,
                temperature = eff.temperature,
            )
        }
    }

    fun getEffectivePrompt(modelId: String, modeId: String): EffectivePromptRow? =
        dao.resolveEffectivePrompt(modelId, modeId)

    fun upsertModels(models: List<AdminModelEntry>) {
        for (m in models) {
            dao.upsertModel(m.modelId, m.displayName, m.isEnabled)
        }
    }

    fun upsertPrompts(body: AdminUpsertPromptsRequest) {
        for (p in body.basePrompts) {
            dao.upsertBasePrompt(p.modeId, p.promptText, p.temperature)
        }
        for (o in body.overrides) {
            dao.upsertModelPromptOverride(
                openRouterModelId = o.modelId,
                modeId = o.modeId,
                promptTextOverride = o.promptTextOverride,
                temperatureOverride = o.temperatureOverride,
            )
        }
    }
}

data class EffectivePromptEntry(
    val modeId: String,
    val promptText: String,
    val temperature: Double,
)
