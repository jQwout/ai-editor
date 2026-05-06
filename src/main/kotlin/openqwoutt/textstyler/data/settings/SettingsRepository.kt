package openqwoutt.textstyler.data.settings

import android.content.Context
import android.content.SharedPreferences
import dev.zacsweers.metro.Inject

@Inject
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): AppSettings {
        return AppSettings(
            backendUrl = prefs.getString(KEY_BACKEND_URL, null)
                ?: "http://10.0.2.2:8080",
            defaultMode = prefs.getString(KEY_DEFAULT_MODE, null)
                ?: "style",
            autoPaste = prefs.getBoolean(KEY_AUTO_PASTE, true),
            autoCopyResult = prefs.getBoolean(KEY_AUTO_COPY, true),
            soundEffects = prefs.getBoolean(KEY_SOUND, false),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            saveHistory = prefs.getBoolean(KEY_SAVE_HISTORY, true),
            mode = try {
                ApiMode.valueOf(prefs.getString(KEY_API_MODE, ApiMode.LOCAL_BACKEND.name)
                    ?: ApiMode.LOCAL_BACKEND.name)
            } catch (_: Exception) {
                ApiMode.LOCAL_BACKEND
            },
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, "google/gemini-2.0-flash-exp:free")
                ?: "google/gemini-2.0-flash-exp:free"
        )
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
            putString(KEY_API_MODE, settings.mode.name)
            putString(KEY_API_KEY, settings.apiKey)
            putString(KEY_MODEL, settings.model)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "textstyler_settings"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DEFAULT_MODE = "default_mode"
        private const val KEY_AUTO_PASTE = "auto_paste"
        private const val KEY_AUTO_COPY = "auto_copy"
        private const val KEY_SOUND = "sound_effects"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_SAVE_HISTORY = "save_history"
        private const val KEY_API_MODE = "api_mode"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
    }
}
