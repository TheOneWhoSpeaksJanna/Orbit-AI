package com.omniclaw.ui.viewmodels

import android.content.Context
import android.content.Intent
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
import com.omniclaw.domain.models.Skill
import com.omniclaw.service.DownloadForegroundService
import com.omniclaw.BuildConfig
import com.omniclaw.domain.repository.OmniClawRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private const val THEME_DEFAULT = "system"

class SettingsViewModel(
    private val appContext: Context,
    private val prefsManager: PreferencesManager,
    private val updateManager: UpdateManager,
    private val repository: OmniClawRepository
) : ViewModel() {

    private val _themeMode = MutableStateFlow(THEME_DEFAULT)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _agentPermissionLevel = MutableStateFlow("NORMAL")
    val agentPermissionLevel: StateFlow<String> = _agentPermissionLevel.asStateFlow()

    private val _agentRulesAllowed = MutableStateFlow("")
    val agentRulesAllowed: StateFlow<String> = _agentRulesAllowed.asStateFlow()

    private val _agentRulesAsk = MutableStateFlow("")
    val agentRulesAsk: StateFlow<String> = _agentRulesAsk.asStateFlow()

    private val _agentRulesDenied = MutableStateFlow("")
    val agentRulesDenied: StateFlow<String> = _agentRulesDenied.asStateFlow()

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    init {
        loadSettings()
        loadSkills()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _themeMode.value = prefsManager.themeMode.firstOrNull() ?: THEME_DEFAULT
            _agentPermissionLevel.value = prefsManager.agentPermissionLevel.firstOrNull() ?: "NORMAL"
            _agentRulesAllowed.value = prefsManager.agentRulesAllowed.firstOrNull() ?: ""
            _agentRulesAsk.value = prefsManager.agentRulesAsk.firstOrNull() ?: ""
            _agentRulesDenied.value = prefsManager.agentRulesDenied.firstOrNull() ?: ""
        }
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch { prefsManager.setThemeMode(mode) }
    }

    val appVersion: String = BuildConfig.VERSION_NAME.substringBeforeLast('-')
    val updateState: StateFlow<UpdateState> = updateManager.updateState

    fun updateAgentPermissionLevel(level: String) {
        _agentPermissionLevel.value = level
        viewModelScope.launch { prefsManager.setAgentPermissionLevel(level) }
    }

    fun updateAgentRulesAllowed(rules: String) {
        _agentRulesAllowed.value = rules
        viewModelScope.launch { prefsManager.setAgentRulesAllowed(rules) }
    }

    fun updateAgentRulesAsk(rules: String) {
        _agentRulesAsk.value = rules
        viewModelScope.launch { prefsManager.setAgentRulesAsk(rules) }
    }

    fun updateAgentRulesDenied(rules: String) {
        _agentRulesDenied.value = rules
        viewModelScope.launch { prefsManager.setAgentRulesDenied(rules) }
    }

    fun checkForUpdates() {
        viewModelScope.launch { updateManager.checkForUpdates() }
    }

    fun downloadUpdate(info: UpdateInfo) {
        val intent = Intent(appContext, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_DOWNLOAD
            putExtra(DownloadForegroundService.EXTRA_DOWNLOAD_URL, info.downloadUrl)
            putExtra(DownloadForegroundService.EXTRA_VERSION, info.latestVersion)
            putExtra(DownloadForegroundService.EXTRA_RELEASE_NOTES, info.releaseNotes)
        }
        appContext.startForegroundService(intent)
    }

    fun installUpdate(filePath: String) {
        updateManager.installApk(filePath)
    }

    fun loadSkills() {
        viewModelScope.launch {
            repository.getAllSkills().collect { list ->
                _skills.value = list
            }
        }
    }

    fun toggleSkillEnabled(skillId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSkillEnabled(skillId, enabled)
        }
    }

    fun updateSkillContent(skillId: String, content: String) {
        viewModelScope.launch {
            repository.updateSkillContent(skillId, content)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as OmniClawApplication
                return SettingsViewModel(
                    application,
                    application.container.prefsManager,
                    application.container.updateManager,
                    application.container.repository
                ) as T
            }
        }
    }
}
