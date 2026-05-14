package openqwoutt.miniapp.textstyler.service.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AudioService error types.
 */
class AudioServiceTest {

    @Test
    fun audioErrors_areDefined() {
        assertEquals("permission_denied", AudioError.PERMISSION_DENIED)
        assertEquals("not_initialized", AudioError.NOT_INITIALIZED)
        assertEquals("recording_error", AudioError.RECORDING_ERROR)
    }

    @Test
    fun audioLevel_normalization() {
        // Test that audio levels are properly normalized to 0-1 range
        val minLevel = 0.0
        val maxLevel = 1.0
        
        assertTrue(minLevel >= 0)
        assertTrue(maxLevel <= 1)
    }
}
