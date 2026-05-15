package openqwoutt.textprocessor.backend.repoindex

import kotlinx.serialization.Serializable

@Serializable
data class AdminUpsertedResponseDto(
    val status: String = "ok",
    val upserted: Int,
)

@Serializable
data class AdminIngestResultsResponseDto(
    val status: String = "ok",
    val results: List<IngestResult>,
)

@Serializable
data class AdminRefreshResultsResponseDto(
    val status: String = "ok",
    val results: List<RepoRefreshResult>,
)

@Serializable
data class PublicRepoStateDto(
    val repoUrl: String,
    val provider: String,
    val defaultBranch: String? = null,
    val lastSeenCommit: String? = null,
    val lastIngestedCommit: String? = null,
)

@Serializable
data class AdminPublicReposResponseDto(
    val status: String = "ok",
    val repos: List<PublicRepoStateDto>,
)

@Serializable
data class AiModelDto(
    val modelId: String,
    val displayName: String,
)

@Serializable
data class EffectivePromptDto(
    val modeId: String,
    val promptText: String,
    val temperature: Double,
)

@Serializable
data class PromptsForModelResponseDto(
    val modelId: String,
    val prompts: List<EffectivePromptDto>,
)

@Serializable
data class SinglePromptResponseDto(
    val modelId: String,
    val modeId: String,
    val promptText: String,
    val temperature: Double,
)

@Serializable
data class AdminUpsertModelsRequest(
    val models: List<AdminModelEntry>,
)

@Serializable
data class AdminModelEntry(
    val modelId: String,
    val displayName: String,
    val isEnabled: Boolean = true,
)

@Serializable
data class AdminUpsertPromptsRequest(
    val basePrompts: List<AdminBasePromptEntry> = emptyList(),
    val overrides: List<AdminOverrideEntry> = emptyList(),
)

@Serializable
data class AdminBasePromptEntry(
    val modeId: String,
    val promptText: String,
    val temperature: Double,
)

@Serializable
data class AdminOverrideEntry(
    val modelId: String,
    val modeId: String,
    val promptTextOverride: String? = null,
    val temperatureOverride: Double? = null,
)

@Serializable
data class PublicPromptDto(
    val source: String,
    val path: String,
    val sha: String,
    val itemKey: String = "",
    val tags: List<String> = emptyList(),
    val promptText: String,
)

@Serializable
data class PublicPromptsResponseDto(
    val prompts: List<PublicPromptDto>,
)

@Serializable
data class AdminUpsertPublicPromptsRequest(
    val prompts: List<PublicPromptDto>,
)

@Serializable
data class AdminIngestPublicReposRequest(
    val repoUrls: List<String>,
)

@Serializable
data class AdminUpsertPublicReposRequest(
    val repoUrls: List<String>,
)
