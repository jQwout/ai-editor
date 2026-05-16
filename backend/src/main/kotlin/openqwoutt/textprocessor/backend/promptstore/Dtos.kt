package openqwoutt.textprocessor.backend.promptstore

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AdminUpsertPromptRequest(
    val modeId: String,
    val modelId: String? = null,
    val promptText: String,
    val temperature: Double,
    val isEnabled: Boolean = true,
    val tags: List<String> = emptyList(),
    val origin: JsonObject = JsonObject(emptyMap()),
    val meta: JsonObject = JsonObject(emptyMap()),
    val raw: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class AdminUpsertPromptsRequest(
    val prompts: List<AdminUpsertPromptRequest>,
    val origin: JsonObject = JsonObject(emptyMap()),
    val raw: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class AdminUpsertPromptResponse(
    val status: String = "ok",
    val promptId: String,
    val upserted: Boolean,
)

@Serializable
data class AdminUpsertPromptsResponse(
    val status: String = "ok",
    /** Number of prompts successfully applied (insert or update); batch is one atomic transaction. */
    val upserted: Int,
)
