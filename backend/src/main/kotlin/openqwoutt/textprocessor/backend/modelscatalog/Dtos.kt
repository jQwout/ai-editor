package openqwoutt.textprocessor.backend.modelscatalog

import kotlinx.serialization.Serializable

@Serializable
data class CatalogModelDto(
    val modelId: String,
    val displayName: String,
)

@Serializable
data class ModelCatalogResponseDto(
    val provider: String = PROVIDER_NVIDIA,
    val updatedAt: String,
    val models: List<CatalogModelDto>,
)

@Serializable
data class CatalogModelEntry(
    val modelId: String,
    val displayName: String,
    val isEnabled: Boolean = true,
)

@Serializable
data class AdminSyncModelsRequest(
    val disableMissing: Boolean = true,
    val models: List<CatalogModelEntry>,
)

@Serializable
data class AdminSyncModelsResponseDto(
    val status: String = "ok",
    val upserted: Int,
    val unchanged: Int = 0,
    val disabled: Int,
    val updatedAt: String,
)

const val PROVIDER_NVIDIA: String = "nvidia"
