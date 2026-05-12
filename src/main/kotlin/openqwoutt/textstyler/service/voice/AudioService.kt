package openqwoutt.miniapp.textstyler.service.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Service for microphone access and audio level visualization.
 */
class AudioService(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    /**
     * Check if microphone permission is granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start monitoring audio levels.
     * @param onLevelUpdate Callback with normalized audio level (0.0 - 1.0)
     * @param onError Callback for errors
     */
    fun startMonitoring(
        onLevelUpdate: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasPermission()) {
            onError("Microphone permission not granted")
            return
        }

        stopMonitoring()

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("Failed to initialize audio recording")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Audio record initialization failed")
                return
            }

            audioRecord?.startRecording()

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                while (isActive) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        val level = calculateRmsLevel(buffer, readCount)
                        onLevelUpdate(level.coerceIn(0f, 1f))
                    }
                    kotlinx.coroutines.delay(50) // ~20 updates per second
                }
            }
        } catch (e: SecurityException) {
            onError("Microphone permission denied")
        } catch (e: Exception) {
            onError("Audio recording error: ${e.message}")
        }
    }

    /**
     * Stop monitoring audio levels.
     */
    fun stopMonitoring() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        audioRecord = null
    }

    /**
     * Release resources.
     */
    fun release() {
        stopMonitoring()
    }

    /**
     * Calculate RMS (Root Mean Square) level from audio buffer.
     */
    private fun calculateRmsLevel(buffer: ShortArray, readCount: Int): Float {
        var sum = 0.0
        for (i in 0 until readCount) {
            sum += buffer[i].toDouble() * buffer[i].toDouble()
        }
        val rms = sqrt(sum / readCount)
        // Normalize to 0-1 range (assuming 16-bit audio)
        return (rms / 32768.0).toFloat()
    }
}

/**
 * Error codes for AudioService.
 */
object AudioError {
    const val PERMISSION_DENIED = "permission_denied"
    const val NOT_INITIALIZED = "not_initialized"
    const val RECORDING_ERROR = "recording_error"
}
