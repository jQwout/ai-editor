package openqwoutt.miniapp.textstyler.service.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TranslationService interface and error types.
 */
class TranslationServiceTest {

    @Test
    fun translationErrors_areDefined() {
        assertEquals("not_available", TranslationError.NOT_AVAILABLE)
        assertEquals("models_not_downloaded", TranslationError.MODELS_NOT_DOWNLOADED)
        assertEquals("download_failed", TranslationError.DOWNLOAD_FAILED)
        assertEquals("translation_failed", TranslationError.TRANSLATION_FAILED)
        assertEquals("empty_input", TranslationError.EMPTY_INPUT)
        assertEquals("unknown", TranslationError.UNKNOWN)
    }

    @Test
    fun translationResult_withDifferentTypes() {
        // Test sealed class behavior
        val result1 = TranslationResult.Success("hello")
        val result2 = TranslationResult.Error("error message")
        
        assertTrue(result1 is TranslationResult.Success)
        assertTrue(result2 is TranslationResult.Error)
        assertFalse(result1 is TranslationResult.Error)
        assertFalse(result2 is TranslationResult.Success)
    }

    @Test
    fun successResult_containsText() {
        val result = TranslationResult.Success("Привет")
        assertEquals("Привет", result.translatedText)
    }

    @Test
    fun errorResult_containsMessage() {
        val result = TranslationResult.Error("Network error")
        assertEquals("Network error", result.message)
    }
}

/**
 * Result wrapper for translation operations.
 */
sealed class TranslationResult {
    data class Success(val translatedText: String) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}
