package com.omniclaw.ui.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.omniclaw.OmniClawApplication
import com.omniclaw.R
import com.omniclaw.data.local.prefs.PreferencesManager
import com.omniclaw.data.local.runner.LocalCommandRunner
import com.omniclaw.data.local.runtime.OmniClawRuntimeManager
import com.omniclaw.domain.models.Agent
import com.omniclaw.domain.api.AiProvider
import com.omniclaw.domain.api.AiResult
import com.omniclaw.domain.repository.OmniClawRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

private const val DEFAULT_THEME = "System"
private const val DEFAULT_AGENT = "Hermes"
private const val DEFAULT_PROVIDER = "Gemini"
private const val AGENT_HERMES = "Hermes"
private const val AGENT_OPENCLAUDE = "OpenClaude"
private const val AGENT_CLAUDE_CODE = "Claude Code"
private const val GITHUB_REPO_URL = "https://github.com/Gitlawb/openclaude.git"
private const val CONNECT_TIMEOUT_MS = 15000
private const val READ_TIMEOUT_MS = 60000
private const val AGENT_DESC = "Agent provisioned during setup"
private const val STATUS_STARTING = "Starting installation..."
private const val STATUS_CHECKING = "Checking prerequisites..."
private const val STATUS_DOWNLOADING = "Downloading "
private const val STATUS_INSTALLING_DEPS = "Installing dependencies..."
private const val STATUS_BUILDING = "Building "
private const val STATUS_CREATING_SCRIPT = "Creating run script..."
private const val STATUS_INSTALLED = " installed successfully!"
private const val STATUS_FAILED = "Installation failed: "

private val DIST_CANDIDATES = listOf("dist/cli.js", "dist/index.js", "cli.js", "index.js", "bin/cli.js")

private val AGENT_INSTALL_DIRS = mapOf(
    AGENT_HERMES to "hermes",
    AGENT_OPENCLAUDE to "openclaude",
    AGENT_CLAUDE_CODE to "claude_code"
)

private val AGENT_WRAPPER_NAMES = mapOf(
    AGENT_CLAUDE_CODE to "claude-code"
)

private val SYSTEM_PROMPTS = mapOf(
    AGENT_HERMES to "You are Hermes, a local execution agent. You can execute shell commands locally. If the user asks you to run a command, output exactly [RUN: command] or [SUDO: command] to execute as root via Shizuku. Do not wrap in markdown, just the raw tag if you need to execute. If you just want to talk, respond normally.",
    AGENT_OPENCLAUDE to "You are OpenClaude, an open-source Claude integration with full tool use.",
    AGENT_CLAUDE_CODE to "You are Claude Code, a specialized coding agent with codebase awareness."
)

enum class SetupStep(@StringRes val labelResId: Int) {
    Welcome(R.string.step_welcome),
    Theme(R.string.step_theme),
    Agent(R.string.step_agent),
    Provider(R.string.step_provider),
    Shizuku(R.string.step_shizuku),
    Summary(R.string.step_summary);
}

class SetupViewModel(
    private val prefsManager: PreferencesManager,
    private val repository: OmniClawRepository,
    private val aiProvider: AiProvider,
    private val localCommandRunner: LocalCommandRunner,
    private val runtimeManager: OmniClawRuntimeManager
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _theme = MutableStateFlow(DEFAULT_THEME)
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _shizukuEnabled = MutableStateFlow(false)
    val shizukuEnabled: StateFlow<Boolean> = _shizukuEnabled.asStateFlow()

    private val _selectedAgent = MutableStateFlow(DEFAULT_AGENT)
    val selectedAgent: StateFlow<String> = _selectedAgent.asStateFlow()

    private val _selectedProvider = MutableStateFlow(DEFAULT_PROVIDER)
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow("")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _testConnectionSuccess = MutableStateFlow<Boolean?>(null)
    val testConnectionSuccess: StateFlow<Boolean?> = _testConnectionSuccess.asStateFlow()

    private val _testConnectionError = MutableStateFlow<String?>(null)
    val testConnectionError: StateFlow<String?> = _testConnectionError.asStateFlow()

    data class AgentInstallState(
        val isInstalling: Boolean = false,
        val progress: Float = 0f,
        val status: String = "",
        val isInstalled: Boolean = false
    )

    private val _agentInstallStates = MutableStateFlow(
        mapOf(
            AGENT_HERMES to AgentInstallState(),
            AGENT_OPENCLAUDE to AgentInstallState(),
            AGENT_CLAUDE_CODE to AgentInstallState()
        )
    )
    val agentInstallStates: StateFlow<Map<String, AgentInstallState>> = _agentInstallStates.asStateFlow()

    val canAdvance: Boolean
        get() {
            val step = SetupStep.entries[_currentStep.value]
            return when (step) {
                SetupStep.Welcome -> true
                SetupStep.Theme -> _theme.value.isNotBlank()
                SetupStep.Agent -> _selectedAgent.value.isNotBlank()
                SetupStep.Provider -> _apiKey.value.isNotBlank()
                SetupStep.Shizuku -> true
                SetupStep.Summary -> true
            }
        }

    fun nextStep() { _currentStep.value += 1 }

    fun previousStep() {
        if (_currentStep.value > 0) _currentStep.value -= 1
    }

    fun setTheme(mode: String) {
        _theme.value = mode
        viewModelScope.launch { prefsManager.setThemeMode(mode) }
    }
    fun setShizukuEnabled(enabled: Boolean) { _shizukuEnabled.value = enabled }
    fun setSelectedAgent(agent: String) { _selectedAgent.value = agent }
    fun setSelectedProvider(provider: String) { _selectedProvider.value = provider }
    fun setSelectedModel(model: String) { _selectedModel.value = model }
    fun setApiKey(key: String) { _apiKey.value = key }

    fun testConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testConnectionSuccess.value = null
            _testConnectionError.value = null

            val model = _selectedModel.value.ifBlank {
                aiProvider.getModels(_selectedProvider.value).firstOrNull() ?: ""
            }

            if (_apiKey.value.isBlank()) {
                _testConnectionSuccess.value = false
                _testConnectionError.value = "API key is blank"
                _isTestingConnection.value = false
                return@launch
            }

            val result = aiProvider.generateContent(
                prompt = "Reply with exactly: ok",
                apiKey = _apiKey.value,
                provider = _selectedProvider.value,
                model = model
            )

            when (result) {
                is AiResult.Success -> {
                    _testConnectionSuccess.value = true
                    _testConnectionError.value = null
                }
                is AiResult.Error -> {
                    _testConnectionSuccess.value = false
                    _testConnectionError.value = result.message
                }
            }
            _isTestingConnection.value = false
        }
    }

    fun installOpenClaude() = installAgent(AGENT_OPENCLAUDE)
    fun installHermes() = installAgent(AGENT_HERMES)
    fun installClaudeCode() = installAgent(AGENT_CLAUDE_CODE)

    fun installAgent(agentName: String) {
        val targetDirName = AGENT_INSTALL_DIRS[agentName] ?: agentName.lowercase().replace(" ", "_")
        val targetDir = File(runtimeManager.agentsDir, targetDirName)
        val binDir = runtimeManager.binDir
        val wrapperName = AGENT_WRAPPER_NAMES[agentName] ?: agentName.lowercase()
        val wrapperFile = File(binDir, wrapperName)

        viewModelScope.launch {
            _agentInstallStates.value = _agentInstallStates.value + (agentName to AgentInstallState(
                isInstalling = true,
                progress = 0f,
                status = STATUS_STARTING,
                isInstalled = false
            ))

            try {
                updateInstallState(agentName, status = STATUS_CHECKING)
                withContext(Dispatchers.IO) {
                    localCommandRunner.executeCommand(
                        "mkdir -p ${runtimeManager.agentsDir.absolutePath} ${binDir.absolutePath}"
                    )
                }

                updateInstallState(agentName, progress = 0.1f, status = "$STATUS_DOWNLOADING$agentName...")

                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }

                withContext(Dispatchers.IO) {
                    val zipUrl = GITHUB_REPO_URL
                        .removeSuffix(".git") + "/archive/refs/heads/main.zip"

                    val connection = URL(zipUrl).openConnection()
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS

                    val tempDir = File(runtimeManager.tmpDir, "agent_$targetDirName")
                    tempDir.deleteRecursively()
                    tempDir.mkdirs()

                    ZipInputStream(connection.getInputStream()).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val entryFile = File(tempDir, entry.name)
                            if (entry.isDirectory) {
                                entryFile.mkdirs()
                            } else {
                                entryFile.parentFile?.mkdirs()
                                entryFile.outputStream().use { output ->
                                    zis.copyTo(output)
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }

                    val rootDir = tempDir.listFiles()?.firstOrNull { it.isDirectory }
                    if (rootDir != null) {
                        rootDir.copyRecursively(targetDir, overwrite = true)
                    } else {
                        tempDir.copyRecursively(targetDir, overwrite = true)
                    }
                    tempDir.deleteRecursively()
                }

                updateInstallState(agentName, progress = 0.4f, status = STATUS_INSTALLING_DEPS)

                withContext(Dispatchers.IO) {
                    try {
                        localCommandRunner.executeCommand(
                            "cd ${targetDir.absolutePath} && npm install --production 2>/dev/null || true"
                        )
                    } catch (_: Exception) { }
                }

                updateInstallState(agentName, progress = 0.7f, status = "$STATUS_BUILDING$agentName...")

                withContext(Dispatchers.IO) {
                    try {
                        localCommandRunner.executeCommand(
                            "cd ${targetDir.absolutePath} && npm run build 2>/dev/null || true"
                        )
                    } catch (_: Exception) { }
                }

                updateInstallState(agentName, progress = 0.9f, status = STATUS_CREATING_SCRIPT)

                val entryPoint = DIST_CANDIDATES.firstOrNull { File(targetDir, it).exists() } ?: "index.js"

                val wrapperScript = """
                    #!/data/data/com.termux/files/usr/bin/bash
                    exec node ${targetDir.absolutePath}/${entryPoint} "${'$'}@"
                """.trimIndent()

                withContext(Dispatchers.IO) {
                    wrapperFile.parentFile?.mkdirs()
                    wrapperFile.writeText(wrapperScript)
                    wrapperFile.setExecutable(true)
                }

                updateInstallState(agentName, progress = 1f, status = "$agentName$STATUS_INSTALLED", isInstalled = true)

            } catch (e: Exception) {
                updateInstallState(agentName, status = "$STATUS_FAILED${e.message}", isInstalled = false)
            } finally {
                _agentInstallStates.value = _agentInstallStates.value + (agentName to _agentInstallStates.value[agentName]!!.copy(isInstalling = false))
            }
        }
    }

    private fun updateInstallState(agentName: String, progress: Float? = null, status: String? = null, isInstalled: Boolean? = null) {
        val current = _agentInstallStates.value[agentName] ?: AgentInstallState()
        _agentInstallStates.value = _agentInstallStates.value + (agentName to current.copy(
            progress = progress ?: current.progress,
            status = status ?: current.status,
            isInstalled = isInstalled ?: current.isInstalled
        ))
    }

    fun completeSetup() {
        viewModelScope.launch {
            prefsManager.setThemeMode(_theme.value)
            prefsManager.setShizukuEnabled(_shizukuEnabled.value)
            prefsManager.setSelectedAgent(_selectedAgent.value)
            prefsManager.setSelectedProvider(_selectedProvider.value)
            prefsManager.setSelectedModel(_selectedModel.value)

            prefsManager.setApiKeyForProvider(_selectedProvider.value, _apiKey.value)

            val agentName = _selectedAgent.value
            val sysPrompt = SYSTEM_PROMPTS[agentName] ?: "You are an expert AI assistant."

            val agent = Agent(
                id = agentName.lowercase().replace(" ", "_"),
                name = agentName,
                description = AGENT_DESC,
                systemPrompt = sysPrompt
            )
            repository.insertAgent(agent)
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            prefsManager.setOnboardingComplete(true)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as OmniClawApplication
                return SetupViewModel(
                    application.container.prefsManager,
                    application.container.repository,
                    application.container.aiProvider,
                    application.container.localCommandRunner,
                    application.container.runtimeManager
                ) as T
            }
        }
    }
}
