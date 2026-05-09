package openqwoutt.textstyler.data.settings

import android.content.SharedPreferences
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named

@Inject
class SecureStorage(
    @Named("securePrefs") private val prefs: SharedPreferences,
) {

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        const val PREFS_NAME = "ai_editor_secure"

        private const val KEY_API_KEY = "api_key"
    }
}
