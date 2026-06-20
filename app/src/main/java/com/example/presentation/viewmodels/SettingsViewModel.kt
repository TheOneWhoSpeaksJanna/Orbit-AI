package com.example.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.OrbitApplication
import com.example.data.local.prefs.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefsManager: PreferencesManager
) : ViewModel() {

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _openAiApiKey = MutableStateFlow("")
    val openAiApiKey: StateFlow<String> = _openAiApiKey.asStateFlow()

    private val _claudeApiKey = MutableStateFlow("")
    val claudeApiKey: StateFlow<String> = _claudeApiKey.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow("")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        loadApiKeys()
    }

    private fun loadApiKeys() {
        viewModelScope.launch {
            _geminiApiKey.value = prefsManager.getApiKeyForProvider("Gemini") ?: ""
            _openAiApiKey.value = prefsManager.getApiKeyForProvider("OpenAI") ?: ""
            _claudeApiKey.value = prefsManager.getApiKeyForProvider("Claude") ?: ""
            _openRouterApiKey.value = prefsManager.getApiKeyForProvider("OpenRouter") ?: ""
            _themeMode.value = prefsManager.themeMode.firstOrNull() ?: "system"
        }
    }

    fun updateGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider("Gemini", key) }
    }

    fun updateOpenAiApiKey(key: String) {
        _openAiApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider("OpenAI", key) }
    }

    fun updateClaudeApiKey(key: String) {
        _claudeApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider("Claude", key) }
    }

    fun updateOpenRouterApiKey(key: String) {
        _openRouterApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider("OpenRouter", key) }
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch { prefsManager.setThemeMode(mode) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as OrbitApplication
                return SettingsViewModel(
                    application.container.prefsManager
                ) as T
            }
        }
    }
}
