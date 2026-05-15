package openqwoutt.textprocessor.backend.nvidiaproxy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NvidiaProxyServiceTest {
    private val cfg =
        NvidiaProxyConfig(
            apiKey = "service-key",
            upstreamBaseUrl = "https://integrate.api.nvidia.com/v1",
            requestTimeoutMs = 120_000L,
            connectTimeoutMs = 15_000L,
            rateLimitPerMinute = null,
            rateLimitTrustForwarded = false,
        )

    @Test
    fun `health reflects configured upstream url`() {
        val service = NvidiaProxyService(cfg)
        val res = service.health()
        assertEquals("ok", res.status)
        assertEquals("nvidia-proxy", res.feature)
        assertEquals(cfg.upstreamBaseUrl, res.upstreamBaseUrl)
    }

    @Test
    fun `capabilities advertises planned passthrough operation`() {
        val service = NvidiaProxyService(cfg)
        val res = service.capabilities()
        assertEquals("nvidia", res.provider)
        assertTrue(res.ready)
        assertTrue(res.implementedOperations.isEmpty())
        assertTrue(res.plannedOperations.contains("chat.completions.passthrough"))
    }
}
