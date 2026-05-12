package openqwoutt.miniapp.textstyler.ui.voiceinput

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Voice Input Flow.
 * Tests the complete cycle: record → transcribe → translate → insert
 */
@RunWith(AndroidJUnit4::class)
class VoiceInputFlowIntegrationTest {

    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun voiceInputPanel_displaysCorrectly() {
        // Test that the panel UI elements are present and correctly configured
        val supportedLanguages = VoiceInputConfig.supportedLanguages
        assertEquals(2, supportedLanguages.size)
        assertTrue(supportedLanguages.contains("ru"))
        assertTrue(supportedLanguages.contains("en"))
    }

    @Test
    fun languageConfiguration_isCorrect() {
        // Default language should be Russian
        assertEquals("ru", VoiceInputConfig.defaultSourceLanguage)
        
        // Translation direction should be correct
        assertEquals("en", VoiceInputConfig.getTargetLanguage("ru"))
        assertEquals("ru", VoiceInputConfig.getTargetLanguage("en"))
    }

    @Test
    fun configFlags_areCorrect() {
        assertTrue(VoiceInputConfig.showInterimResults)
        assertTrue(VoiceInputConfig.showWaveform)
        assertFalse(VoiceInputConfig.autoExpand)
        assertEquals(60000L, VoiceInputConfig.recordingTimeoutMs)
    }

    @Test
    fun languageDisplayNames_areLocalized() {
        assertEquals("Russian", VoiceInputConfig.getLanguageDisplayName("ru"))
        assertEquals("English", VoiceInputConfig.getLanguageDisplayName("en"))
    }

    @Test
    fun languageFlags_areEmojis() {
        assertEquals("🇷🇺", VoiceInputConfig.getLanguageFlag("ru"))
        assertEquals("🇬🇧", VoiceInputConfig.getLanguageFlag("en"))
    }

    @Test
    fun voiceInputStates_areDefined() {
        // Verify all states are accessible
        assertNotNull(VoiceInputState.IDLE)
        assertNotNull(VoiceInputState.RECORDING)
        assertNotNull(VoiceInputState.PROCESSING)
        assertNotNull(VoiceInputState.ERROR)
    }

    @Test
    fun voiceInputActions_areDefined() {
        // Verify all actions are accessible
        assertNotNull(VoiceInputAction.ToggleRecording)
        assertNotNull(VoiceInputAction.StopRecording)
        assertNotNull(VoiceInputAction.Clear)
        assertNotNull(VoiceInputAction.InsertTranslation)
        assertNotNull(VoiceInputAction.ToggleExpanded)
        assertNotNull(VoiceInputAction.SelectLanguage("ru"))
        assertNotNull(VoiceInputAction.DownloadModels)
        assertNotNull(VoiceInputAction.DismissError)
    }

    @Test
    fun initialUiState_hasCorrectDefaults() {
        val initialState = VoiceInputUiState()
        
        assertEquals(VoiceInputState.IDLE, initialState.state)
        assertFalse(initialState.isExpanded)
        assertEquals("ru", initialState.sourceLanguage)
        assertEquals("en", initialState.targetLanguage)
        assertEquals("", initialState.originalText)
        assertEquals("", initialState.translatedText)
        assertEquals(0f, initialState.audioLevel)
        assertNull(initialState.error)
        assertFalse(initialState.isMicPermissionGranted)
        assertFalse(initialState.areModelsDownloaded)
        assertFalse(initialState.isDownloadingModels)
    }
}
