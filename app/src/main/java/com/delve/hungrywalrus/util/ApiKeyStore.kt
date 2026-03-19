package com.delve.hungrywalrus.util

import android.content.SharedPreferences
import android.util.Log
import com.delve.hungrywalrus.di.NetworkModule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes the USDA API key from EncryptedSharedPreferences.
 * Returns null if no key is stored or if keystore decryption fails.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    private val encryptedPrefs: SharedPreferences,
) {
    companion object {
        private const val TAG = "ApiKeyStore"
    }

    fun getApiKey(): String? {
        return try {
            val key = encryptedPrefs.getString(NetworkModule.USDA_API_KEY_PREF, null)
            if (key.isNullOrBlank()) null else key
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read API key from EncryptedSharedPreferences", e)
            try {
                encryptedPrefs.edit().remove(NetworkModule.USDA_API_KEY_PREF).apply()
            } catch (clearException: Exception) {
                Log.e(TAG, "Failed to clear corrupted API key", clearException)
            }
            null
        }
    }

    fun saveApiKey(key: String) {
        encryptedPrefs.edit()
            .putString(NetworkModule.USDA_API_KEY_PREF, key)
            .apply()
    }

    fun clearApiKey() {
        encryptedPrefs.edit()
            .remove(NetworkModule.USDA_API_KEY_PREF)
            .apply()
    }

    fun hasApiKey(): Boolean = getApiKey() != null
}
