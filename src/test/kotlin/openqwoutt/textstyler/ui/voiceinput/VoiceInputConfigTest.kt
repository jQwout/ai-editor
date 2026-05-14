package openqwoutt.miniapp.textstyler.ui.voiceinput

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for VoiceInputConfig.
 */
class VoiceInputConfigTest {

    @Test
    fun supportedLanguages_containsRuAndEn() {
        assertTrue(VoiceInputConfig.supportedLanguages.contains("ru"))
        assertTrue(VoiceInputConfig.supportedLanguages.contains("en"))
        assertEquals(2, VoiceInputConfig.supportedLanguages.size)
    }

    @Test
    fun defaultSourceLanguage_isRu() {
        assertEquals("ru", VoiceInputConfig.defaultSourceLanguage)
    }

    @Test
    fun getTargetLanguage_ru_returnsEn() {
        assertEquals("en", VoiceInputConfig.getTargetLanguage("ru"))
    }

    @Test
    fun getTargetLanguage_RU_uppercase_returnsEn() {
        assertEquals("en", VoiceInputConfig.getTargetLanguage("RU"))
    }

    @Test
    fun getTargetLanguage_en_returnsRu() {
        assertEquals("ru", VoiceInputConfig.getTargetLanguage("en"))
    }

    @Test
    fun getTargetLanguage_unknown_returnsEn() {
        assertEquals("en", VoiceInputConfig.getTargetLanguage("unknown"))
    }

    @Test
    fun getLanguageDisplayName_ru_returnsRussian() {
        assertEquals("Russian", VoiceInputConfig.getLanguageDisplayName("ru"))
    }

    @Test
    fun getLanguageDisplayName_en_returnsEnglish() {
        assertEquals("English", VoiceInputConfig.getLanguageDisplayName("en"))
    }

    @Test
    fun getLanguageDisplayName_unknown_returnsCodeUppercase() {
        assertEquals("FR", VoiceInputConfig.getLanguageDisplayName("fr"))
    }

    @Test
    fun getLanguageFlag_ru_returnsRussianFlag() {
        assertEquals("🇷🇺", VoiceInputConfig.getLanguageFlag("ru"))
    }

    @Test
    fun getLanguageFlag_en_returnsBritishFlag() {
        assertEquals("🇬🇧", VoiceInputConfig.getLanguageFlag("en"))
    }

    @Test
    fun getLanguageFlag_unknown_returnsGlobe() {
        assertEquals("🌐", VoiceInputConfig.getLanguageFlag("unknown"))
    }

    @Test
    fun recordingTimeout_is60Seconds() {
        assertEquals(60000L, VoiceInputConfig.recordingTimeoutMs)
    }

    @Test
    fun showInterimResults_isTrue() {
        assertTrue(VoiceInputConfig.showInterimResults)
    }

    @Test
    fun showWaveform_isTrue() {
        assertTrue(VoiceInputConfig.showWaveform)
    }
}
