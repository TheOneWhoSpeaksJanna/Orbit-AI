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
import kotlinx.coroutines.launch

class SetupViewModel(
    private val prefsManager: PreferencesManager
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _theme = MutableStateFlow("System")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _shizukuEnabled = MutableStateFlow(false)
    val shizukuEnabled: StateFlow<Boolean> = _shizukuEnabled.asStateFlow()

    private val _selectedAgent = MutableStateFlow("Hermes")
    val selectedAgent: StateFlow<String> = _selectedAgent.asStateFlow()

    private val _selectedProvider = MutableStateFlow("Claude")
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow("")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _testConnectionSuccess = MutableStateFlow<Boolean?>(null)
    val testConnectionSuccess: StateFlow<Boolean?> = _testConnectionSuccess.asStateFlow()

    fun nextStep() {
        _currentStep.value += 1
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun setTheme(mode: String) {
        _theme.value = mode
    }

    fun setShizukuEnabled(enabled: Boolean) {
        _shizukuEnabled.value = enabled
    }

    fun setSelectedAgent(agent: String) {
        _selectedAgent.value = agent
    }

    fun setSelectedProvider(provider: String) {
        _selectedProvider.value = provider
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testConnectionSuccess.value = null
            // Simulate network test
            kotlinx.coroutines.delay(1500)
            _testConnectionSuccess.value = _apiKey.value.isNotBlank()
            _isTestingConnection.value = false
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            prefsManager.setThemeMode(_theme.value)
            prefsManager.setShizukuEnabled(_shizukuEnabled.value)
            prefsManager.setSelectedAgent(_selectedAgent.value)
            prefsManager.setSelectedProvider(_selectedProvider.value)
            prefsManager.setSelectedModel(_selectedModel.value)
            prefsManager.setGeminiApiKey(_apiKey.value)
            prefsManager.setOnboardingComplete(true)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as OrbitApplication
                return SetupViewModel(application.container.prefsManager) as T
            }
        }
    }
}
