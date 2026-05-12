package openqwoutt.miniapp.textstyler.service.voice

/**
 * Service interface for speech recognition.
 */
interface SpeechRecognitionService {
    /**
     * Check if speech recognition is available on this device.
     */
    fun isAvailable(): Boolean

    /**
     * Start listening for speech.
     * @param languageCode BCP-47 language code (e.g., "ru-RU", "en-US")
     * @param onResult Callback for recognition results (interim and final)
     * @param onError Callback for errors
     * @param onEnd Callback when recognition ends
     */
    fun startListening(
        languageCode: String,
        onResult: (SpeechRecognitionResult) -> Unit,
        onError: (String) -> Unit,
        onEnd: () -> Unit
    )

    /**
     * Stop listening.
     */
    fun stopListening()

    /**
     * Release resources.
     */
    fun release()
}

/**
 * Result from speech recognition.
 */
sealed class SpeechRecognitionResult {
    /** Interim result (may change) */
    data class Interim(val text: String) : SpeechRecognitionResult()

    /** Final result (confirmed) */
    data class Final(val text: String) : SpeechRecognitionResult()

    /** Partial result (word by word) */
    data class Partial(val text: String) : SpeechRecognitionResult()
}

/**
 * Speech recognition error codes.
 */
object SpeechRecognitionError {
    const val NOT_AVAILABLE = "not_available"
    const val PERMISSION_DENIED = "permission_denied"
    const val NO_MATCH = "no_match"
    const val NETWORK_ERROR = "network_error"
    const val TIMEOUT = "timeout"
    const val UNKNOWN = "unknown"
}
