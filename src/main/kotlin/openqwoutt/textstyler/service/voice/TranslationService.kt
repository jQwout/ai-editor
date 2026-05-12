package openqwoutt.miniapp.textstyler.service.voice

/**
 * Service interface for text translation.
 */
interface TranslationService {
    /**
     * Check if translation is available (models downloaded).
     */
    fun isAvailable(): Boolean

    /**
     * Check if translation models are downloaded.
     */
    fun areModelsDownloaded(): Boolean

    /**
     * Download translation models.
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @param onProgress Callback for download progress (0.0 - 1.0)
     * @param onComplete Callback when download complete
     * @param onError Callback for errors
     */
    fun downloadModels(
        sourceLanguage: String,
        targetLanguage: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Translate text.
     * @param text Text to translate
     * @param sourceLanguage Source language code
     * @param targetLanguage Target language code
     * @param onResult Callback with translated text
     * @param onError Callback for errors
     */
    fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Translate text synchronously (blocking).
     * @return Translated text or null if failed
     */
    suspend fun translateSync(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String?

    /**
     * Release resources.
     */
    fun release()
}

/**
 * Translation error codes.
 */
object TranslationError {
    const val NOT_AVAILABLE = "not_available"
    const val MODELS_NOT_DOWNLOADED = "models_not_downloaded"
    const val DOWNLOAD_FAILED = "download_failed"
    const val TRANSLATION_FAILED = "translation_failed"
    const val EMPTY_INPUT = "empty_input"
    const val UNKNOWN = "unknown"
}
