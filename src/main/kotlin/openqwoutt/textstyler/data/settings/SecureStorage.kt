package openqwoutt.textstyler.data.settings

import android.content.Context
import android.content.SharedPreferences
import dev.zacsweers.metro.Inject

@Inject
class SecureStorage(context: Context) {

    private val securePrefs: SharedPreferences = context.getSharedPreferences(
        SECURE_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getApiKey(): String = securePrefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun clearApiKey() {
        securePrefs.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        private const val SECURE_PREFS_NAME = "ai_editor_secure"
        private const val KEY_API_KEY = "api_key"
    }
}
