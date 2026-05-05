package openqwoutt.textstyler.data.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): AppSettings {
        return AppSettings(
            backendUrl = prefs.getString(KEY_BACKEND_URL, AppSettings().backendUrl)
                ?: AppSettings().backendUrl,
            defaultMode = prefs.getString(KEY_DEFAULT_MODE, AppSettings().defaultMode)
                ?: AppSettings().defaultMode,
            autoPaste = prefs.getBoolean(KEY_AUTO_PASTE, AppSettings().autoPaste),
            autoCopyResult = prefs.getBoolean(KEY_AUTO_COPY, AppSettings().autoCopyResult),
            soundEffects = prefs.getBoolean(KEY_SOUND, AppSettings().soundEffects),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, AppSettings().hapticFeedback)
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
    }
}