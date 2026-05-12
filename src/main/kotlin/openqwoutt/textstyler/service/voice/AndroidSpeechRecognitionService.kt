package openqwoutt.miniapp.textstyler.service.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Android implementation of SpeechRecognitionService using Android SpeechRecognizer API.
 */
class AndroidSpeechRecognitionService(private val context: Context) : SpeechRecognitionService {

    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun startListening(
        languageCode: String,
        onResult: (SpeechRecognitionResult) -> Unit,
        onError: (String) -> Unit,
        onEnd: () -> Unit
    ) {
        if (isListening) {
            stopListening()
        }

        mainHandler.post {
            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorMessage = when (error) {

                            SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionError.UNKNOWN
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionError.PERMISSION_DENIED
                            SpeechRecognizer.ERROR_NETWORK -> SpeechRecognitionError.NETWORK_ERROR
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechRecognitionError.TIMEOUT
                            SpeechRecognizer.ERROR_NO_MATCH -> SpeechRecognitionError.NO_MATCH
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognitionError.UNKNOWN
                            SpeechRecognizer.ERROR_SERVER -> SpeechRecognitionError.NETWORK_ERROR
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionError.TIMEOUT
                            else -> SpeechRecognitionError.UNKNOWN
                        }
                        onError(errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onResult(SpeechRecognitionResult.Final(text))
                        }
                        onEnd()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onResult(SpeechRecognitionResult.Partial(text))
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, mapToLocale(languageCode))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                recognizer?.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                onError(SpeechRecognitionError.NOT_AVAILABLE)
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            isListening = false
            recognizer?.stopListening()
            recognizer?.cancel()
        }
    }

    override fun release() {
        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
            isListening = false
        }
    }

    private fun mapToLocale(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "ru" -> "ru-RU"
            "en" -> "en-US"
            else -> Locale.getDefault().toString()
        }
    }
}
