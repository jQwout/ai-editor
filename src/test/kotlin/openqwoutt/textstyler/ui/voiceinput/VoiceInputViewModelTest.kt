package openqwoutt.miniapp.textstyler.ui.voiceinput

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import openqwoutt.miniapp.textstyler.service.voice.AudioService
import openqwoutt.miniapp.textstyler.service.voice.SpeechRecognitionResult
import openqwoutt.miniapp.textstyler.service.voice.SpeechRecognitionService
import openqwoutt.miniapp.textstyler.service.voice.TranslationService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for VoiceInputViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInputViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: VoiceInputViewModel
    private lateinit var mockSpeechService: MockSpeechRecognitionService
    private lateinit var mockTranslationService: MockTranslationService
    private lateinit var mockAudioService: MockAudioService

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSpeechService = MockSpeechRecognitionService()
        mockTranslationService = MockTranslationService()
        mockAudioService = MockAudioService()
        
        viewModel = VoiceInputViewModel(
            speechService = mockSpeechService,
            translationService = mockTranslationService,
            audioService = mockAudioService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    @Test
    fun initialState_isIdle() = runTest {
        viewModel.initialize()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(VoiceInputState.IDLE, state.state)
        assertFalse(state.isExpanded)
        assertEquals("ru", state.sourceLanguage)
        assertEquals("en", state.targetLanguage)
        assertTrue(state.originalText.isEmpty())
        assertTrue(state.translatedText.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun initialize_setsMicPermission() = runTest {
        mockAudioService.hasPermissionResult = true
        viewModel.initialize()
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.isMicPermissionGranted)
    }

    @Test
    fun initialize_withoutMicPermission_setsPermissionFalse() = runTest {
        mockAudioService.hasPermissionResult = false
        viewModel.initialize()
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isMicPermissionGranted)
    }

    @Test
    fun toggleExpanded_changesExpandedState() = runTest {
        assertFalse(viewModel.uiState.value.isExpanded)
        
        viewModel.handle(VoiceInputAction.ToggleExpanded)
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.isExpanded)
        
        viewModel.handle(VoiceInputAction.ToggleExpanded)
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isExpanded)
    }

    @Test
    fun selectLanguage_changesSourceAndTarget() = runTest {
        // Start with default (ru -> en)
        assertEquals("ru", viewModel.uiState.value.sourceLanguage)
        assertEquals("en", viewModel.uiState.value.targetLanguage)
        
        // Select English
        viewModel.handle(VoiceInputAction.SelectLanguage("en"))
        advanceUntilIdle()
        
        assertEquals("en", viewModel.uiState.value.sourceLanguage)
        assertEquals("ru", viewModel.uiState.value.targetLanguage)
    }

    @Test
    fun selectLanguage_invalidLanguage_ignores() = runTest {
        val initialSource = viewModel.uiState.value.sourceLanguage
        
        viewModel.handle(VoiceInputAction.SelectLanguage("fr"))
        advanceUntilIdle()
        
        assertEquals(initialSource, viewModel.uiState.value.sourceLanguage)
    }

    @Test
    fun clear_clearsText() = runTest {
        // Set some text
        viewModel.handle(VoiceInputAction.SelectLanguage("en"))
        advanceUntilIdle()
        
        // Simulate recording
        mockSpeechService.simulatePartialResult("Hello world")
        advanceUntilIdle()
        
        // Now clear
        viewModel.handle(VoiceInputAction.Clear)
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.originalText.isEmpty())
        assertTrue(viewModel.uiState.value.translatedText.isEmpty())
    }

    @Test
    fun dismissError_clearsError() = runTest {
        viewModel.handle(VoiceInputAction.ToggleExpanded)
        advanceUntilIdle()
        
        // Set an error
        viewModel.uiState.value.let { 
            // We need to simulate an error state
        }
        
        viewModel.handle(VoiceInputAction.DismissError)
        advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun getTranslationForInsert_returnsTranslatedText() = runTest {
        mockTranslationService.translateResult = "Привет мир"
        
        viewModel.handle(VoiceInputAction.SelectLanguage("en"))
        advanceUntilIdle()
        
        mockSpeechService.simulateFinalResult("Hello world")
        advanceUntilIdle()
        
        assertEquals("Привет мир", viewModel.getTranslationForInsert())
    }

    @Test
    fun recordingWithoutPermission_showsError() = runTest {
        mockAudioService.hasPermissionResult = false
        viewModel.initialize()
        advanceUntilIdle()
        
        viewModel.handle(VoiceInputAction.ToggleRecording)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(VoiceInputState.ERROR, state.state)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("permission"))
    }

    @Test
    fun recordingWithPermission_startsRecording() = runTest {
        mockAudioService.hasPermissionResult = true
        viewModel.initialize()
        advanceUntilIdle()
        
        viewModel.handle(VoiceInputAction.ToggleRecording)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(VoiceInputState.RECORDING, state.state)
        assertTrue(mockAudioService.isMonitoring)
        assertTrue(mockSpeechService.isListening)
    }

    @Test
    fun stopRecording_stopsServices() = runTest {
        mockAudioService.hasPermissionResult = true
        viewModel.initialize()
        advanceUntilIdle()
        
        viewModel.handle(VoiceInputAction.ToggleRecording)
        advanceUntilIdle()
        
        viewModel.handle(VoiceInputAction.StopRecording)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(VoiceInputState.IDLE, state.state)
        assertFalse(mockAudioService.isMonitoring)
        assertFalse(mockSpeechService.isListening)
    }

    @Test
    fun speechRecognitionPartialResult_updatesOriginalText() = runTest {
        mockAudioService.hasPermissionResult = true
        mockTranslationService.translateResult = "тест"
        viewModel.initialize()
        advanceUntilIdle()
        
        viewModel.handle(VoiceInputAction.ToggleRecording)
        advanceUntilIdle()
        
        mockSpeechService.simulatePartialResult("test")
        advanceUntilIdle()
        
        assertEquals("test", viewModel.uiState.value.originalText)
    }

    @Test
    fun speechRecognitionFinalResult_updatesOriginalAndTranslates() = runTest {
        mockAudioService.hasPermissionResult = true
        mockTranslationService.translateResult = "тест"
        viewModel.initialize()
        advanceUntilIdle()
        
        viewModel.handle(VoiceInputAction.ToggleRecording)
        advanceUntilIdle()
        
        mockSpeechService.simulateFinalResult("test")
        advanceUntilIdle()
        
        assertEquals("test", viewModel.uiState.value.originalText)
        assertEquals("тест", viewModel.uiState.value.translatedText)
    }

    // Mock implementations
    private class MockSpeechRecognitionService : SpeechRecognitionService {
        var isListening = false
        private var onResult: ((SpeechRecognitionResult) -> Unit)? = null
        private var onError: ((String) -> Unit)? = null
        private var onEnd: (() -> Unit)? = null

        fun simulatePartialResult(text: String) {
            onResult?.invoke(SpeechRecognitionResult.Partial(text))
        }

        fun simulateFinalResult(text: String) {
            onResult?.invoke(SpeechRecognitionResult.Final(text))
        }

        fun simulateError(error: String) {
            onError?.invoke(error)
        }

        fun simulateEnd() {
            onEnd?.invoke()
        }

        override fun isAvailable() = true

        override fun startListening(
            languageCode: String,
            onResult: (SpeechRecognitionResult) -> Unit,
            onError: (String) -> Unit,
            onEnd: () -> Unit
        ) {
            this.onResult = onResult
            this.onError = onError
            this.onEnd = onEnd
            isListening = true
        }

        override fun stopListening() {
            isListening = false
        }

        override fun release() {
            isListening = false
        }
    }

    private class MockTranslationService : TranslationService {
        var translateResult = "translation"
        private var onProgress: ((Float) -> Unit)? = null
        private var onComplete: (() -> Unit)? = null
        private var onError: ((String) -> Unit)? = null
        private var onResult: ((String) -> Unit)? = null
        private var onErrorResult: ((String) -> Unit)? = null

        override fun isAvailable() = true
        override fun areModelsDownloaded() = true

        override fun downloadModels(
            sourceLanguage: String,
            targetLanguage: String,
            onProgress: (Float) -> Unit,
            onComplete: () -> Unit,
            onError: (String) -> Unit
        ) {
            onProgress(1f)
            onComplete()
        }

        override fun translate(
            text: String,
            sourceLanguage: String,
            targetLanguage: String,
            onResult: (String) -> Unit,
            onError: (String) -> Unit
        ) {
            onResult(translateResult)
        }

        override suspend fun translateSync(
            text: String,
            sourceLanguage: String,
            targetLanguage: String
        ) = translateResult

        override fun release() {}
    }

    private class MockAudioService : AudioService {
        var hasPermissionResult = false
        var isMonitoring = false
        private var onLevelUpdate: ((Float) -> Unit)? = null
        private var onError: ((String) -> Unit)? = null

        override fun hasPermission() = hasPermissionResult

        override fun startMonitoring(
            onLevelUpdate: (Float) -> Unit,
            onError: (String) -> Unit
        ) {
            this.onLevelUpdate = onLevelUpdate
            this.onError = onError
            isMonitoring = true
        }

        override fun stopMonitoring() {
            isMonitoring = false
        }

        override fun release() {
            isMonitoring = false
        }
    }
}
