package openqwoutt.textprocessor.backend.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResolveRateLimitClientIpTest {

    @Test
    fun `ignores forwarded headers when trust false`() {
        assertEquals(
            "10.0.0.1",
            resolveRateLimitClientIp(
                remoteHost = "10.0.0.1",
                trustForwardedHeaders = false,
                xForwardedFor = "203.0.113.5, 10.0.0.2",
                xRealIp = "203.0.113.9",
            ),
        )
    }

    @Test
    fun `uses first x forwarded for when trust true`() {
        assertEquals(
            "203.0.113.5",
            resolveRateLimitClientIp(
                remoteHost = "10.0.0.1",
                trustForwardedHeaders = true,
                xForwardedFor = "203.0.113.5, 10.0.0.2",
                xRealIp = null,
            ),
        )
    }

    @Test
    fun `uses x real ip when xff empty and trust true`() {
        assertEquals(
            "198.51.100.2",
            resolveRateLimitClientIp(
                remoteHost = "10.0.0.1",
                trustForwardedHeaders = true,
                xForwardedFor = null,
                xRealIp = "198.51.100.2",
            ),
        )
    }
}
