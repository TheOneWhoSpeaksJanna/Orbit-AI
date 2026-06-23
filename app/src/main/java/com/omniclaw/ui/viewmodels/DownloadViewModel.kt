package com.omniclaw.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.omniclaw.OmniClawApplication
import com.omniclaw.data.local.runner.LocalCommandRunner
import com.omniclaw.data.local.runtime.OmniClawRuntimeManager
import com.omniclaw.data.local.prefs.PreferencesManager
import com.omniclaw.domain.models.DownloadState
import com.omniclaw.domain.models.DownloadableAgent
import com.omniclaw.domain.models.toAgent
import com.omniclaw.domain.repository.OmniClawRepository
import com.omniclaw.domain.repository.OpenCodeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DOWNLOAD_DELAY_MS = 300L
private const val ERROR_OUTPUT_TRUNCATE = 200
private const val INSTALL_FAILED_PREFIX = "Install failed: "
private const val INSTALL_ERROR_PREFIX = "Install error: "
private const val UNKNOWN_ERROR = "Unknown error"

data class DownloadableAgentUi(
    val agent: DownloadableAgent,
    val downloadState: DownloadState = DownloadState.Idle
)

class DownloadViewModel(
    private val openCodeRepository: OpenCodeRepository,
    private val repository: OmniClawRepository,
    private val prefsManager: PreferencesManager,
    private val localCommandRunner: LocalCommandRunner,
    private val runtimeManager: OmniClawRuntimeManager
) : ViewModel() {

    private val _agents = MutableStateFlow<List<DownloadableAgentUi>>(emptyList())
    val agents: StateFlow<List<DownloadableAgentUi>> = _agents.asStateFlow()

    init {
        viewModelScope.launch {
            openCodeRepository.getAvailableAgents().collect { catalog ->
                val current = _agents.value.toMutableList()
                val existingIds = current.map { it.agent.id }.toSet()
                val merged = catalog.map { agent ->
                    val existing = current.find { it.agent.id == agent.id }
                    existing ?: DownloadableAgentUi(agent)
                }
                _agents.value = merged
            }
        }
    }

    fun downloadAgent(agentId: String) {
        val index = _agents.value.indexOfFirst { it.agent.id == agentId }
        if (index == -1) return

        val entry = _agents.value[index]
        if (entry.downloadState !is DownloadState.Idle &&
            entry.downloadState !is DownloadState.Error
        ) return

        viewModelScope.launch {
            updateAgentState(agentId, DownloadState.Requesting)
            delay(DOWNLOAD_DELAY_MS)

            if (entry.agent.installCommand.isNotBlank()) {
                try {
                    val result = localCommandRunner.executeCommand(entry.agent.installCommand)
                    if (result.exitCode != 0) {
                        updateAgentState(agentId, DownloadState.Error(
                            "$INSTALL_FAILED_PREFIX${result.output.take(ERROR_OUTPUT_TRUNCATE)}"
                        ))
                        return@launch
                    }
                } catch (e: Exception) {
                    updateAgentState(agentId, DownloadState.Error(
                        "$INSTALL_ERROR_PREFIX${e.message ?: UNKNOWN_ERROR}"
                    ))
                    return@launch
                }
            }

            repository.insertAgent(entry.agent.toAgent())
            prefsManager.setSelectedAgent(entry.agent.id)
            updateAgentState(agentId, DownloadState.Complete(""))
        }
    }

    private fun updateAgentState(agentId: String, state: DownloadState) {
        val idx = _agents.value.indexOfFirst { it.agent.id == agentId }
        if (idx == -1) return
        _agents.value = _agents.value.toMutableList().also { list ->
            list[idx] = list[idx].copy(downloadState = state)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[APPLICATION_KEY]) as OmniClawApplication
                val container = app.container
                return DownloadViewModel(
                    openCodeRepository = container.openCodeRepository,
                    repository = container.repository,
                    prefsManager = container.prefsManager,
                    localCommandRunner = container.localCommandRunner,
                    runtimeManager = container.runtimeManager
                ) as T
            }
        }
    }
}
