package com.omniclaw.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "omniclaw_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getApiKey(providerName: String): String? =
        prefs.getString("${providerName.lowercase()}_api_key", null)

    fun setApiKey(providerName: String, key: String) {
        prefs.edit().putString("${providerName.lowercase()}_api_key", key).apply()
    }

    fun clearApiKey(providerName: String) {
        prefs.edit().remove("${providerName.lowercase()}_api_key").apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
