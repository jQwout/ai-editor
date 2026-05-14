package openqwoutt.textstyler.data.settings

import android.content.SharedPreferences
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named

@Inject
class SettingsRepository(
    @Named("settingsPrefs") private val prefs: SharedPreferences,
) {

    fun load(): AppSettings {
        val defaults = AppSettings()
        val apiKey = prefs.getString(KEY_API_KEY, defaults.apiKey) ?: defaults.apiKey

        val app =  AppSettings(
            backendUrl = prefs.getString(KEY_BACKEND_URL, defaults.backendUrl)
                ?: defaults.backendUrl,
            defaultMode = prefs.getString(KEY_DEFAULT_MODE, defaults.defaultMode)
                ?: defaults.defaultMode,
            autoPaste = prefs.getBoolean(KEY_AUTO_PASTE, defaults.autoPaste),
            autoCopyResult = prefs.getBoolean(KEY_AUTO_COPY, defaults.autoCopyResult),
            soundEffects = prefs.getBoolean(KEY_SOUND, defaults.soundEffects),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, defaults.hapticFeedback),
            saveHistory = prefs.getBoolean(KEY_SAVE_HISTORY, defaults.saveHistory),
            useBackend = prefs.getBoolean(KEY_USE_BACKEND, defaults.useBackend),
            aiProvider = prefs.getString(KEY_AI_PROVIDER, defaults.aiProvider)
                ?: defaults.aiProvider,
            aiModel = prefs.getString(KEY_AI_MODEL, defaults.aiModel)
                ?: defaults.aiModel,
            apiKey = apiKey,
            language = prefs.getString(KEY_LANGUAGE, defaults.language)
                ?: defaults.language
        )

        return app
    }

    fun save(settings: AppSettings) {
        prefs.edit().apply {
            putString(KEY_BACKEND_URL, settings.backendUrl)
            putString(KEY_DEFAULT_MODE, settings.defaultMode)
            putBoolean(KEY_AUTO_PASTE, settings.autoPaste)
            putBoolean(KEY_AUTO_COPY, settings.autoCopyResult)
            putBoolean(KEY_SOUND, settings.soundEffects)
            putBoolean(KEY_HAPTIC, settings.hapticFeedback)
            putBoolean(KEY_SAVE_HISTORY, settings.saveHistory)
            putBoolean(KEY_USE_BACKEND, settings.useBackend)
            putString(KEY_AI_PROVIDER, settings.aiProvider)
            putString(KEY_AI_MODEL, settings.aiModel)
            putString(KEY_API_KEY, settings.apiKey)
            putString(KEY_LANGUAGE, settings.language)
            apply()
        }
    }

    companion object {
        const val PREFS_NAME = "textstyler_settings"

        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DEFAULT_MODE = "default_mode"
        private const val KEY_AUTO_PASTE = "auto_paste"
        private const val KEY_AUTO_COPY = "auto_copy"
        private const val KEY_SOUND = "sound_effects"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_SAVE_HISTORY = "save_history"
        private const val KEY_USE_BACKEND = "use_backend"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_LANGUAGE = "language"
    }
}
