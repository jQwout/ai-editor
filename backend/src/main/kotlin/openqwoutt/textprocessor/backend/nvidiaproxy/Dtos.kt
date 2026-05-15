package openqwoutt.textprocessor.backend.nvidiaproxy

import kotlinx.serialization.Serializable

@Serializable
data class NvidiaProxyHealthResponse(
    val status: String,
    val feature: String,
    val upstreamBaseUrl: String,
)

@Serializable
data class NvidiaProxyCapabilitiesResponse(
    val provider: String,
    val ready: Boolean,
    val implementedOperations: List<String>,
    val plannedOperations: List<String>,
)

@Serializable
data class NvidiaProxyErrorEnvelope(
    val error: NvidiaProxyErrorDetail,
)

@Serializable
data class NvidiaProxyErrorDetail(
    val message: String,
    val code: String? = null,
)
