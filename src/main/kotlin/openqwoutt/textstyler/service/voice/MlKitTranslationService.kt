package openqwoutt.miniapp.textstyler.service.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit implementation of TranslationService.
 */
class MlKitTranslationService : TranslationService {

    private var currentTranslator: Translator? = null
    private var currentSourceLang: String? = null
    private var currentTargetLang: String? = null

    override fun isAvailable(): Boolean = true

    override fun areModelsDownloaded(): Boolean = currentTranslator?.downloadIfNeeded()?.isComplete == true

    override fun downloadModels(
        sourceLanguage: String,
        targetLanguage: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val translator = getOrCreateTranslator(sourceLanguage, targetLanguage)
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.download(conditions)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                onError("${TranslationError.DOWNLOAD_FAILED}: ${e.message}")
            }
    }

    override fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (text.isBlank()) {
            onError(TranslationError.EMPTY_INPUT)
            return
        }

        val translator = getOrCreateTranslator(sourceLanguage, targetLanguage)

        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                onResult(translatedText)
            }
            .addOnFailureListener { e ->
                onError("${TranslationError.TRANSLATION_FAILED}: ${e.message}")
            }
    }

    override suspend fun translateSync(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null

        val translator = getOrCreateTranslator(sourceLanguage, targetLanguage)

        // Download model if needed
        downloadModelSync(translator)

        // Translate with timeout
        withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { continuation ->
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        continuation.resume(translatedText)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        }
    }

    override fun release() {
        currentTranslator?.close()
        currentTranslator = null
        currentSourceLang = null
        currentTargetLang = null
    }

    private fun getOrCreateTranslator(sourceLanguage: String, targetLanguage: String): Translator {
        if (currentTranslator != null && 
            currentSourceLang == sourceLanguage && 
            currentTargetLang == targetLanguage) {
            return currentTranslator!!
        }

        // Close old translator
        currentTranslator?.close()

        val mlKitSourceLang = mapToMlKitLanguage(sourceLanguage)
        val mlKitTargetLang = mapToMlKitLanguage(targetLanguage)

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitSourceLang)
            .setTargetLanguage(mlKitTargetLang)
            .build()

        currentTranslator = Translation.getClient(options)
        currentSourceLang = sourceLanguage
        currentTargetLang = targetLanguage

        return currentTranslator!!
    }

    private suspend fun downloadModelSync(translator: Translator) {
        suspendCancellableCoroutine { continuation ->
            val conditions = DownloadConditions.Builder().build()
            translator.download(conditions)
                .addOnSuccessListener {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(Unit) // Continue anyway, might work
                    }
                }
        }
    }

    private fun mapToMlKitLanguage(code: String): String {
        return when (code.lowercase()) {
            "ru" -> TranslateLanguage.RUSSIAN
            "en" -> TranslateLanguage.ENGLISH
            else -> TranslateLanguage.ENGLISH
        }
    }
}
