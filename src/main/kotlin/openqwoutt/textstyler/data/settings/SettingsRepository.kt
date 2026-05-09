package openqwoutt.textstyler.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(
    context: Context,
    private val secureStorage: SecureStorage
) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also {
            Log.i(TAG, "EncryptedSharedPreferences initialized for settings")
        }
    } catch (e: Exception) {
        Log.e(TAG, "EncryptedSharedPreferences failed for settings; using private SharedPreferences", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun load(): AppSettings {
        val secureApiKey = secureStorage.getApiKey()
        val legacyApiKey = prefs.getString(KEY_API_KEY, AppSettings().apiKey) ?: AppSettings().apiKey

        return AppSettings(
            backendUrl = prefs.getString(KEY_BACKEND_URL, AppSettings().backendUrl)
                ?: AppSettings().backendUrl,
            defaultMode = prefs.getString(KEY_DEFAULT_MODE, AppSettings().defaultMode)
                ?: AppSettings().defaultMode,
            autoPaste = prefs.getBoolean(KEY_AUTO_PASTE, AppSettings().autoPaste),
            autoCopyResult = prefs.getBoolean(KEY_AUTO_COPY, AppSettings().autoCopyResult),
            soundEffects = prefs.getBoolean(KEY_SOUND, AppSettings().soundEffects),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, AppSettings().hapticFeedback),
            saveHistory = prefs.getBoolean(KEY_SAVE_HISTORY, AppSettings().saveHistory),
            useBackend = prefs.getBoolean(KEY_USE_BACKEND, AppSettings().useBackend),
            aiProvider = prefs.getString(KEY_AI_PROVIDER, AppSettings().aiProvider)
                ?: AppSettings().aiProvider,
            aiModel = prefs.getString(KEY_AI_MODEL, AppSettings().aiModel)
                ?: AppSettings().aiModel,
            apiKey = secureApiKey.ifBlank { legacyApiKey }
        )
    }

    fun save(settings: AppSettings) {
        if (settings.apiKey.isBlank()) {
            secureStorage.clearApiKey()
        } else {
            secureStorage.setApiKey(settings.apiKey)
        }

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
            remove(KEY_API_KEY)
            apply()
        }
    }

    companion object {
        private const val TAG = "SettingsRepository"
        private const val PREFS_NAME = "textstyler_settings"
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
    }
}
