package openqwoutt.miniapp.textstyler.service.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SpeechRecognitionService interface and result types.
 */
class SpeechRecognitionServiceTest {

    @Test
    fun interimResult_containsText() {
        val result = SpeechRecognitionResult.Interim("hello")
        assertEquals("hello", result.text)
    }

    @Test
    fun finalResult_containsText() {
        val result = SpeechRecognitionResult.Final("hello world")
        assertEquals("hello world", result.text)
    }

    @Test
    fun partialResult_containsText() {
        val result = SpeechRecognitionResult.Partial("hel")
        assertEquals("hel", result.text)
    }

    @Test
    fun speechRecognitionErrors_areDefined() {
        assertEquals("not_available", SpeechRecognitionError.NOT_AVAILABLE)
        assertEquals("permission_denied", SpeechRecognitionError.PERMISSION_DENIED)
        assertEquals("no_match", SpeechRecognitionError.NO_MATCH)
        assertEquals("network_error", SpeechRecognitionError.NETWORK_ERROR)
        assertEquals("timeout", SpeechRecognitionError.TIMEOUT)
        assertEquals("unknown", SpeechRecognitionError.UNKNOWN)
    }
}
