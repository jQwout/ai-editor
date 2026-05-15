package openqwoutt.textprocessor.backend.nvidiaproxy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NvidiaProxyConfigTest {
    @Test
    fun `fromEnvironment returns null when feature disabled`() {
        val cfg = NvidiaProxyConfig.fromEnvironment { _ -> null }
        assertNull(cfg)
    }

    @Test
    fun `fromEnvironment returns null when enabled flag is false`() {
        val env = mapOf("NVIDIA_PROXY_ENABLED" to "false")
        val cfg = NvidiaProxyConfig.fromEnvironment { name -> env[name] }
        assertNull(cfg)
    }

    @Test
    fun `fromEnvironment uses defaults when optional vars missing`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "true",
                "NVIDIA_PROXY_API_KEY" to "proxy-key",
            )
        val cfg = checkNotNull(NvidiaProxyConfig.fromEnvironment { name -> env[name] })
        assertEquals("proxy-key", cfg.apiKey)
        assertEquals(NvidiaProxyConfig.DEFAULT_UPSTREAM_BASE_URL, cfg.upstreamBaseUrl)
        assertEquals(NvidiaProxyConfig.DEFAULT_REQUEST_TIMEOUT_MS, cfg.requestTimeoutMs)
        assertEquals(NvidiaProxyConfig.DEFAULT_CONNECT_TIMEOUT_MS, cfg.connectTimeoutMs)
        assertNull(cfg.rateLimitPerMinute)
        assertEquals(false, cfg.rateLimitTrustForwarded)
    }

    @Test
    fun `fromEnvironment reads overrides`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "TrUe",
                "NVIDIA_PROXY_API_KEY" to " secret-key ",
                "NVIDIA_PROXY_UPSTREAM_BASE_URL" to " https://custom.nvidia/v1 ",
                "NVIDIA_PROXY_REQUEST_TIMEOUT_MS" to "90000",
                "NVIDIA_PROXY_CONNECT_TIMEOUT_MS" to "8000",
                "NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE" to "42",
                "NVIDIA_PROXY_RATE_LIMIT_TRUST_FORWARDED" to "true",
            )
        val cfg = checkNotNull(NvidiaProxyConfig.fromEnvironment { name -> env[name] })
        assertEquals("secret-key", cfg.apiKey)
        assertEquals("https://custom.nvidia/v1", cfg.upstreamBaseUrl)
        assertEquals(90_000L, cfg.requestTimeoutMs)
        assertEquals(8_000L, cfg.connectTimeoutMs)
        assertEquals(42, cfg.rateLimitPerMinute)
        assertEquals(true, cfg.rateLimitTrustForwarded)
    }

    @Test
    fun `fromEnvironment requires api key when enabled`() {
        val env = mapOf("NVIDIA_PROXY_ENABLED" to "true")
        assertThrows<IllegalStateException> {
            NvidiaProxyConfig.fromEnvironment { name -> env[name] }
        }
    }

    @Test
    fun `fromEnvironment rejects blank api key when enabled`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "true",
                "NVIDIA_PROXY_API_KEY" to "   ",
            )
        assertThrows<IllegalStateException> {
            NvidiaProxyConfig.fromEnvironment { name -> env[name] }
        }
    }

    @Test
    fun `fromEnvironment falls back when upstream base url blank`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "true",
                "NVIDIA_PROXY_API_KEY" to "k",
                "NVIDIA_PROXY_UPSTREAM_BASE_URL" to "   ",
            )
        val cfg = checkNotNull(NvidiaProxyConfig.fromEnvironment { name -> env[name] })
        assertEquals(NvidiaProxyConfig.DEFAULT_UPSTREAM_BASE_URL, cfg.upstreamBaseUrl)
    }

    @Test
    fun `fromEnvironment uses defaults for invalid timeout values`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "true",
                "NVIDIA_PROXY_API_KEY" to "k",
                "NVIDIA_PROXY_REQUEST_TIMEOUT_MS" to "not-a-number",
                "NVIDIA_PROXY_CONNECT_TIMEOUT_MS" to "NaN",
            )
        val cfg = checkNotNull(NvidiaProxyConfig.fromEnvironment { name -> env[name] })
        assertEquals(NvidiaProxyConfig.DEFAULT_REQUEST_TIMEOUT_MS, cfg.requestTimeoutMs)
        assertEquals(NvidiaProxyConfig.DEFAULT_CONNECT_TIMEOUT_MS, cfg.connectTimeoutMs)
    }

    @Test
    fun `fromEnvironment clamps too small timeout values`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "true",
                "NVIDIA_PROXY_API_KEY" to "k",
                "NVIDIA_PROXY_REQUEST_TIMEOUT_MS" to "100",
                "NVIDIA_PROXY_CONNECT_TIMEOUT_MS" to "0",
            )
        val cfg = checkNotNull(NvidiaProxyConfig.fromEnvironment { name -> env[name] })
        assertEquals(5_000L, cfg.requestTimeoutMs)
        assertEquals(1_000L, cfg.connectTimeoutMs)
    }

    @Test
    fun `fromEnvironment rejects non-positive nvidia proxy rate limit`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "true",
                "NVIDIA_PROXY_API_KEY" to "k",
                "NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE" to "0",
            )
        assertThrows<IllegalStateException> {
            NvidiaProxyConfig.fromEnvironment { name -> env[name] }
        }
    }

    @Test
    fun `fromEnvironment rejects non-integer nvidia proxy rate limit`() {
        val env =
            mapOf(
                "NVIDIA_PROXY_ENABLED" to "true",
                "NVIDIA_PROXY_API_KEY" to "k",
                "NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE" to "abc",
            )
        assertThrows<IllegalStateException> {
            NvidiaProxyConfig.fromEnvironment { name -> env[name] }
        }
    }
}
