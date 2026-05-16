package openqwoutt.textprocessor.backend.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecureCompareTest {

    @Test
    fun `secure equals matches identical strings`() {
        assertTrue(secureEqualsUtf8Strings("same-token", "same-token"))
    }

    @Test
    fun `secure equals rejects different strings`() {
        assertFalse(secureEqualsUtf8Strings("a", "b"))
        assertFalse(secureEqualsUtf8Strings("secret", "Secret"))
    }
}
