package openqwoutt.textstyler.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Builds [EncryptedSharedPreferences] with a sane fallback to a regular private
 * [SharedPreferences] file if the secure variant cannot be initialized
 * (e.g. corrupted keystore on rare devices).
 */
internal object EncryptedPrefs {

    private const val TAG = "EncryptedPrefs"

    fun create(context: Context, name: String): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also {
            Log.i(TAG, "EncryptedSharedPreferences initialized for '$name'")
        }
    } catch (e: Exception) {
        Log.e(TAG, "EncryptedSharedPreferences failed for '$name'; using private SharedPreferences", e)
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
}
