package openqwoutt.miniapp.textstyler.ui.voiceinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import openqwoutt.miniapp.textstyler.service.voice.AudioService
import openqwoutt.miniapp.textstyler.service.voice.AudioError
import openqwoutt.miniapp.textstyler.service.voice.SpeechRecognitionError
import openqwoutt.miniapp.textstyler.service.voice.SpeechRecognitionResult
import openqwoutt.miniapp.textstyler.service.voice.SpeechRecognitionService
import openqwoutt.miniapp.textstyler.service.voice.TranslationService

/**
 * Voice input panel states.
 */
enum class VoiceInputState {
    IDLE,
    RECORDING,
    PROCESSING,
    ERROR
}

/**
 * UI state for VoiceInputPanel.
 */
data class VoiceInputUiState(
    val state: VoiceInputState = VoiceInputState.IDLE,
    val isExpanded: Boolean = false,
    val sourceLanguage: String = VoiceInputConfig.defaultSourceLanguage,
    val targetLanguage: String = VoiceInputConfig.getTargetLanguage(VoiceInputConfig.defaultSourceLanguage),
    val originalText: String = "",
    val translatedText: String = "",
    val audioLevel: Float = 0f,
    val error: String? = null,
    val isMicPermissionGranted: Boolean = false,
    val areModelsDownloaded: Boolean = false,
    val isDownloadingModels: Boolean = false
)

/**
 * Actions for VoiceInputPanel.
 */
sealed class VoiceInputAction {
    data object ToggleRecording : VoiceInputAction()
    data object StopRecording : VoiceInputAction()
    data object Clear : VoiceInputAction()
    data object InsertTranslation : VoiceInputAction()
    data object ToggleExpanded : VoiceInputAction()
    data class SelectLanguage(val languageCode: String) : VoiceInputAction()
    data object DownloadModels : VoiceInputAction()
    data object DismissError : VoiceInputAction()
}

/**
 * ViewModel for Voice Input with Translation feature.
 */
class VoiceInputViewModel(
    private val speechService: SpeechRecognitionService,
    private val translationService: TranslationService,
    private val audioService: AudioService
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceInputUiState())
    val uiState: StateFlow<VoiceInputUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var timeoutJob: Job? = null
    private var accumulatedText = StringBuilder()

    /**
     * Check initial permissions and download models.
     */
    fun initialize() {
        val hasMicPermission = audioService.hasPermission()
        _uiState.update { it.copy(isMicPermissionGranted = hasMicPermission) }

        // Try to download models in background
        downloadModelsIfNeeded()
    }

    /**
     * Handle user actions.
     */
    fun handle(action: VoiceInputAction) {
        when (action) {
            is VoiceInputAction.ToggleRecording -> toggleRecording()
            is VoiceInputAction.StopRecording -> stopRecording()
            is VoiceInputAction.Clear -> clear()
            is VoiceInputAction.InsertTranslation -> { /* Handled by UI */ }
            is VoiceInputAction.ToggleExpanded -> toggleExpanded()
            is VoiceInputAction.SelectLanguage -> selectLanguage(action.languageCode)
            is VoiceInputAction.DownloadModels -> downloadModelsIfNeeded()
            is VoiceInputAction.DismissError -> dismissError()
        }
    }

    /**
     * Get translation result for insertion.
     */
    fun getTranslationForInsert(): String = _uiState.value.translatedText

    private fun toggleRecording() {
        if (_uiState.value.state == VoiceInputState.RECORDING) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (!_uiState.value.isMicPermissionGranted) {
            _uiState.update { it.copy(error = "Microphone permission required", state = VoiceInputState.ERROR) }
            return
        }

        accumulatedText.clear()
        _uiState.update {
            it.copy(
                state = VoiceInputState.RECORDING,
                originalText = "",
                translatedText = "",
                error = null
            )
        }

        // Start audio level monitoring
        audioService.startMonitoring(
            onLevelUpdate = { level ->
                _uiState.update { it.copy(audioLevel = level) }
            },
            onError = { error ->
                _uiState.update { it.copy(error = error, state = VoiceInputState.ERROR) }
            }
        )

        // Start speech recognition
        val languageCode = _uiState.value.sourceLanguage
        speechService.startListening(
            languageCode = languageCode,
            onResult = { result ->
                handleSpeechResult(result)
            },
            onError = { error ->
                handleSpeechError(error)
            },
            onEnd = {
                onRecordingEnd()
            }
        )

        // Start timeout timer
        startRecordingTimeout()
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        timeoutJob?.cancel()
        audioService.stopMonitoring()
        speechService.stopListening()

        _uiState.update {
            it.copy(
                state = VoiceInputState.IDLE,
                audioLevel = 0f
            )
        }
    }

    private fun handleSpeechResult(result: SpeechRecognitionResult) {
        when (result) {
            is SpeechRecognitionResult.Partial -> {
                accumulatedText.clear()
                accumulatedText.append(result.text)
                _uiState.update { it.copy(originalText = result.text) }
                translateText(result.text)
            }
            is SpeechRecognitionResult.Final -> {
                accumulatedText.clear()
                accumulatedText.append(result.text)
                _uiState.update { it.copy(originalText = result.text) }
                translateText(result.text)
            }
            is SpeechRecognitionResult.Interim -> {
                // Handle interim results if needed
            }
        }
    }

    private fun handleSpeechError(error: String) {
        when (error) {
            SpeechRecognitionError.NO_MATCH -> {
                // No speech detected - this is normal at end of recording
                if (_uiState.value.state == VoiceInputState.RECORDING) {
                    stopRecording()
                }
            }
            SpeechRecognitionError.PERMISSION_DENIED -> {
                _uiState.update {
                    it.copy(
                        error = "Microphone permission denied",
                        state = VoiceInputState.ERROR,
                        isMicPermissionGranted = false
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        error = "Speech recognition error: $error",
                        state = VoiceInputState.ERROR
                    )
                }
            }
        }
    }

    private fun onRecordingEnd() {
        audioService.stopMonitoring()
        _uiState.update {
            it.copy(
                state = VoiceInputState.IDLE,
                audioLevel = 0f
            )
        }
    }

    private fun startRecordingTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(VoiceInputConfig.recordingTimeoutMs)
            if (_uiState.value.state == VoiceInputState.RECORDING) {
                stopRecording()
            }
        }
    }

    private fun translateText(text: String) {
        if (text.isBlank()) return

        _uiState.update { it.copy(state = VoiceInputState.PROCESSING) }

        val sourceLanguage = _uiState.value.sourceLanguage
        val targetLanguage = _uiState.value.targetLanguage

        translationService.translate(
            text = text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onResult = { translatedText ->
                _uiState.update {
                    it.copy(
                        translatedText = translatedText,
                        state = VoiceInputState.RECORDING
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        error = "Translation error: $error",
                        state = VoiceInputState.RECORDING
                    )
                }
            }
        )
    }

    private fun clear() {
        accumulatedText.clear()
        _uiState.update {
            it.copy(
                originalText = "",
                translatedText = "",
                error = null
            )
        }
    }

    private fun toggleExpanded() {
        _uiState.update { it.copy(isExpanded = !it.isExpanded) }
    }

    private fun selectLanguage(languageCode: String) {
        if (languageCode !in VoiceInputConfig.supportedLanguages) return

        val targetLanguage = VoiceInputConfig.getTargetLanguage(languageCode)
        _uiState.update {
            it.copy(
                sourceLanguage = languageCode,
                targetLanguage = targetLanguage
            )
        }
    }

    private fun downloadModelsIfNeeded() {
        if (_uiState.value.areModelsDownloaded || _uiState.value.isDownloadingModels) return

        _uiState.update { it.copy(isDownloadingModels = true) }

        val sourceLanguage = _uiState.value.sourceLanguage
        val targetLanguage = _uiState.value.targetLanguage

        translationService.downloadModels(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onProgress = { /* Progress tracking if needed */ },
            onComplete = {
                _uiState.update {
                    it.copy(
                        areModelsDownloaded = true,
                        isDownloadingModels = false
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isDownloadingModels = false,
                        error = "Failed to download translation models: $error"
                    )
                }
            }
        )
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
        speechService.release()
        audioService.release()
        translationService.release()
    }
}
