package openqwoutt.textstyler.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {

    private val securePrefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "EncryptedSharedPreferences failed for secure storage; using private SharedPreferences", e)
        context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(): String = securePrefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun clearApiKey() {
        securePrefs.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        private const val TAG = "SecureStorage"
        private const val SECURE_PREFS_NAME = "ai_editor_secure"
        private const val KEY_API_KEY = "api_key"
    }
}
