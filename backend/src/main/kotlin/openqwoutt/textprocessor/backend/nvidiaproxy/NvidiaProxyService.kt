package openqwoutt.textprocessor.backend.nvidiaproxy

class NvidiaProxyService(
    private val config: NvidiaProxyConfig,
) {
    fun health(): NvidiaProxyHealthResponse =
        NvidiaProxyHealthResponse(
            status = "ok",
            feature = "nvidia-proxy",
            upstreamBaseUrl = config.upstreamBaseUrl,
        )

    fun capabilities(): NvidiaProxyCapabilitiesResponse =
        NvidiaProxyCapabilitiesResponse(
            provider = "nvidia",
            ready = true,
            implementedOperations = emptyList(),
            plannedOperations = listOf("chat.completions.passthrough"),
        )
}
