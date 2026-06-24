package com.omniclaw.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.omniclaw.OmniClawApplication
import com.omniclaw.data.local.prefs.PreferencesManager
import com.omniclaw.data.local.updater.UpdateManager
import com.omniclaw.data.local.updater.UpdateInfo
import com.omniclaw.data.local.updater.UpdateState
import com.omniclaw.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private const val PROVIDER_GEMINI = "Gemini"
private const val PROVIDER_OPENAI = "OpenAI"
private const val PROVIDER_CLAUDE = "Claude"
private const val PROVIDER_OPENROUTER = "OpenRouter"
private const val THEME_DEFAULT = "system"

class SettingsViewModel(
    private val prefsManager: PreferencesManager,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _openAiApiKey = MutableStateFlow("")
    val openAiApiKey: StateFlow<String> = _openAiApiKey.asStateFlow()

    private val _claudeApiKey = MutableStateFlow("")
    val claudeApiKey: StateFlow<String> = _claudeApiKey.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow("")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _githubToken = MutableStateFlow("")
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    private val _themeMode = MutableStateFlow(THEME_DEFAULT)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _agentPermissionLevel = MutableStateFlow("NORMAL")
    val agentPermissionLevel: StateFlow<String> = _agentPermissionLevel.asStateFlow()

    private val _agentRules = MutableStateFlow("")
    val agentRules: StateFlow<String> = _agentRules.asStateFlow()

    init {
        loadApiKeys()
    }

    private fun loadApiKeys() {
        viewModelScope.launch {
            _geminiApiKey.value = prefsManager.getApiKeyForProvider(PROVIDER_GEMINI).firstOrNull() ?: ""
            _openAiApiKey.value = prefsManager.getApiKeyForProvider(PROVIDER_OPENAI).firstOrNull() ?: ""
            _claudeApiKey.value = prefsManager.getApiKeyForProvider(PROVIDER_CLAUDE).firstOrNull() ?: ""
            _openRouterApiKey.value = prefsManager.getApiKeyForProvider(PROVIDER_OPENROUTER).firstOrNull() ?: ""
            _githubToken.value = prefsManager.githubToken.firstOrNull() ?: ""
            _themeMode.value = prefsManager.themeMode.firstOrNull() ?: THEME_DEFAULT
            _agentPermissionLevel.value = prefsManager.agentPermissionLevel.firstOrNull() ?: "NORMAL"
            _agentRules.value = prefsManager.agentRules.firstOrNull() ?: ""
        }
    }

    fun updateGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider(PROVIDER_GEMINI, key) }
    }

    fun updateOpenAiApiKey(key: String) {
        _openAiApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider(PROVIDER_OPENAI, key) }
    }

    fun updateClaudeApiKey(key: String) {
        _claudeApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider(PROVIDER_CLAUDE, key) }
    }

    fun updateOpenRouterApiKey(key: String) {
        _openRouterApiKey.value = key
        viewModelScope.launch { prefsManager.setApiKeyForProvider(PROVIDER_OPENROUTER, key) }
    }

    fun updateGithubToken(token: String) {
        _githubToken.value = token
        viewModelScope.launch { prefsManager.setGithubToken(token) }
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch { prefsManager.setThemeMode(mode) }
    }

    val appVersion: String = BuildConfig.VERSION_NAME
    val updateState: StateFlow<UpdateState> = updateManager.updateState

    fun updateAgentPermissionLevel(level: String) {
        _agentPermissionLevel.value = level
        viewModelScope.launch { prefsManager.setAgentPermissionLevel(level) }
    }

    fun updateAgentRules(rules: String) {
        _agentRules.value = rules
        viewModelScope.launch { prefsManager.setAgentRules(rules) }
    }

    fun checkForUpdates() {
        viewModelScope.launch { updateManager.checkForUpdates() }
    }

    fun downloadUpdate(info: UpdateInfo) {
        viewModelScope.launch { updateManager.downloadUpdate(info) }
    }

    fun installUpdate(filePath: String) {
        updateManager.installApk(filePath)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as OmniClawApplication
                return SettingsViewModel(
                    application.container.prefsManager,
                    application.container.updateManager
                ) as T
            }
        }
    }
}
