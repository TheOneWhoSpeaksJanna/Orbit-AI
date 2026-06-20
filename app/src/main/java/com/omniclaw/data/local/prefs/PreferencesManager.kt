package com.omniclaw.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("omniclaw_prefs")

class PreferencesManager(
    private val context: Context,
    private val credentialsStore: CredentialsStore
) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        val SHIZUKU_ENABLED = booleanPreferencesKey("shizuku_enabled")
        val SELECTED_AGENT = stringPreferencesKey("selected_agent")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    val themeMode: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE]
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_ONBOARDING_COMPLETE] ?: false
    }

    val shizukuEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHIZUKU_ENABLED] ?: false
    }

    val selectedAgent: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_AGENT]
    }

    val selectedProvider: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_PROVIDER]
    }

    val selectedModel: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_MODEL]
    }

    suspend fun getApiKeyForProvider(providerName: String): String? =
        credentialsStore.getApiKey(providerName)

    suspend fun setApiKeyForProvider(providerName: String, key: String) {
        credentialsStore.setApiKey(providerName, key)
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setShizukuEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHIZUKU_ENABLED] = enabled
        }
    }

    suspend fun setSelectedAgent(agentId: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_AGENT] = agentId
        }
    }

    suspend fun setSelectedProvider(provider: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_PROVIDER] = provider
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL] = model
        }
    }
}
