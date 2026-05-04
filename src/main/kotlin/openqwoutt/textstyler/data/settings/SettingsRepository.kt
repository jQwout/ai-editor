package openqwoutt.textstyler.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureStorage = SecureStorage(context)

    fun load(): AppSettings {
        return AppSettings(
            mode = ApiMode.valueOf(
                prefs.getString(KEY_MODE, ApiMode.LOCAL_BACKEND.name)!!
            ),
            apiKey = secureStorage.getApiKey(),
            model = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL,
            backendUrl = prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit {
            putString(KEY_MODE, settings.mode.name)
            putString(KEY_MODEL, settings.model)
            putString(KEY_BACKEND_URL, settings.backendUrl)
        }
        secureStorage.setApiKey(settings.apiKey)
    }

    companion object {
        private const val PREFS_NAME = "ai_editor_settings"
        private const val KEY_MODE = "mode"
        private const val KEY_MODEL = "model"
        private const val KEY_BACKEND_URL = "backend_url"

        private const val DEFAULT_MODEL = "google/gemini-2.0-flash-exp:free"
        private const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8080"
    }
}
