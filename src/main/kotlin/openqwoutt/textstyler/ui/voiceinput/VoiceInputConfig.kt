package openqwoutt.miniapp.textstyler.ui.voiceinput

/**
 * Configuration for Voice Input with Translation feature.
 */
object VoiceInputConfig {
    /** Supported source languages for speech recognition */
    val supportedLanguages = listOf("ru", "en") as List<String>

    /** Default source language */
    const val defaultSourceLanguage = "ru"

    /** Show interim (partial) results during speech recognition */
    const val showInterimResults = true

    /** Show waveform visualization during recording */
    const val showWaveform = true

    /** Auto-expand panel when opened */
    const val autoExpand = false

    /** Recording timeout in milliseconds (60 seconds) */
    const val recordingTimeoutMs = 60_000L

    /** Download translation models on start */
    const val downloadModelsOnStart = true

    /** Target language codes for translation */
    const val targetLanguageRu = "ru"
    const val targetLanguageEn = "en"

    /**
     * Get target language code based on source language.
     * @param sourceLanguage Source language code (ru or en)
     * @return Target language code (en or ru)
     */
    fun getTargetLanguage(sourceLanguage: String): String {
        return when (sourceLanguage.lowercase()) {
            "ru" -> targetLanguageEn
            "en" -> targetLanguageRu
            else -> targetLanguageEn
        }
    }

    /**
     * Get display name for language code.
     */
    fun getLanguageDisplayName(code: String): String {
        return when (code.lowercase()) {
            "ru" -> "Russian"
            "en" -> "English"
            else -> code.uppercase()
        }
    }

    /**
     * Get language flag emoji.
     */
    fun getLanguageFlag(code: String): String {
        return when (code.lowercase()) {
            "ru" -> "🇷🇺"
            "en" -> "🇬🇧"
            else -> "🌐"
        }
    }
}
