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
import com.omniclaw.domain.repository.OmniClawRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val _theme = MutableStateFlow("System")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _shizukuEnabled = MutableStateFlow(false)
    val shizukuEnabled: StateFlow<Boolean> = _shizukuEnabled.asStateFlow()

    private val _selectedAgent = MutableStateFlow("Hermes")
    val selectedAgent: StateFlow<String> = _selectedAgent.asStateFlow()

    private val _selectedProvider = MutableStateFlow("Gemini")
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow("")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _testConnectionSuccess = MutableStateFlow<Boolean?>(null)
    val testConnectionSuccess: StateFlow<Boolean?> = _testConnectionSuccess.asStateFlow()

    // OpenClaude install state
    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    private val _installProgress = MutableStateFlow(0f)
    val installProgress: StateFlow<Float> = _installProgress.asStateFlow()

    private val _installStatus = MutableStateFlow("")
    val installStatus: StateFlow<String> = _installStatus.asStateFlow()

    private val _isInstalled = MutableStateFlow(false)
    val isInstalled: StateFlow<Boolean> = _isInstalled.asStateFlow()

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

            val model = _selectedModel.value.ifBlank {
                aiProvider.getModels(_selectedProvider.value).firstOrNull() ?: ""
            }

            val success = aiProvider.testConnection(
                provider = _selectedProvider.value,
                apiKey = _apiKey.value,
                model = model
            )

            _testConnectionSuccess.value = success
            _isTestingConnection.value = false
        }
    }

    fun installOpenClaude() {
        val targetDir = File(runtimeManager.agentsDir, "openclaude")
        val binDir = runtimeManager.binDir
        val wrapperFile = File(binDir, "openclaude")

        viewModelScope.launch {
            _isInstalling.value = true
            _installProgress.value = 0f
            _installStatus.value = "Starting installation..."
            _isInstalled.value = false

            try {
                // Step 1: Check prerequisites
                _installStatus.value = "Checking prerequisites..."
                withContext(Dispatchers.IO) {
                    // Try to detect node and npm via PATH
                    localCommandRunner.executeCommandStreamed(
                        "command -v node || echo MISSING_NODE",
                        onOutput = { }
                    )
                    localCommandRunner.executeCommandStreamed(
                        "command -v npm || echo MISSING_NPM",
                        onOutput = { }
                    )
                }

                // Step 2: Clone the repository
                _installProgress.value = 0.1f
                _installStatus.value = "Cloning OpenClaude repository..."

                // Remove existing directory if present
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }

                val cloneOutput = StringBuilder()
                val cloneResult = withContext(Dispatchers.IO) {
                    localCommandRunner.executeCommandStreamed(
                        "git clone --depth 1 https://github.com/lizhi-cs/openclaude.git ${targetDir.absolutePath} 2>&1",
                        onOutput = { line ->
                            cloneOutput.appendLine(line)
                            _installStatus.value = line.take(80)
                        }
                    )
                }

                if (cloneResult.exitCode != 0) {
                    throw Exception("Git clone failed: ${cloneOutput.toString().take(200)}")
                }

                _installProgress.value = 0.4f
                _installStatus.value = "Installing dependencies..."

                // Step 3: Install npm dependencies
                val npmOutput = StringBuilder()
                val npmResult = withContext(Dispatchers.IO) {
                    localCommandRunner.executeCommandStreamed(
                        "cd ${targetDir.absolutePath} && npm install 2>&1",
                        onOutput = { line ->
                            npmOutput.appendLine(line)
                            if (line.contains("added", ignoreCase = true) || line.contains("saved", ignoreCase = true)) {
                                _installStatus.value = line.take(80)
                            }
                        }
                    )
                }

                if (npmResult.exitCode != 0) {
                    throw Exception("npm install failed: ${npmOutput.toString().take(200)}")
                }

                _installProgress.value = 0.7f
                _installStatus.value = "Building OpenClaude..."

                // Step 4: Build
                val buildOutput = StringBuilder()
                val buildResult = withContext(Dispatchers.IO) {
                    localCommandRunner.executeCommandStreamed(
                        "cd ${targetDir.absolutePath} && npm run build 2>&1",
                        onOutput = { line ->
                            buildOutput.appendLine(line)
                            if (line.contains("success", ignoreCase = true) || line.contains("built", ignoreCase = true)) {
                                _installStatus.value = line.take(80)
                            }
                        }
                    )
                }

                if (buildResult.exitCode != 0) {
                    throw Exception("Build failed: ${buildOutput.toString().take(200)}")
                }

                _installProgress.value = 0.9f
                _installStatus.value = "Creating run script..."

                // Step 5: Create wrapper script
                val wrapperScript = """
                    #!/data/data/com.termux/files/usr/bin/bash
                    exec node ${targetDir.absolutePath}/dist/cli.js "\$@"
                """.trimIndent()

                withContext(Dispatchers.IO) {
                    wrapperFile.parentFile?.mkdirs()
                    wrapperFile.writeText(wrapperScript)
                    wrapperFile.setExecutable(true)
                }

                _installProgress.value = 1f
                _installStatus.value = "OpenClaude installed successfully!"
                _isInstalled.value = true

            } catch (e: Exception) {
                _installStatus.value = "Installation failed: ${e.message}"
                _isInstalled.value = false
            } finally {
                _isInstalling.value = false
            }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            prefsManager.setThemeMode(_theme.value)
            prefsManager.setShizukuEnabled(_shizukuEnabled.value)
            prefsManager.setSelectedAgent(_selectedAgent.value)
            prefsManager.setSelectedProvider(_selectedProvider.value)
            prefsManager.setSelectedModel(_selectedModel.value)

            // Save API key to the correct provider slot
            prefsManager.setApiKeyForProvider(_selectedProvider.value, _apiKey.value)

            val agentName = _selectedAgent.value
            val sysPrompt = when (agentName) {
                "Hermes" -> "You are Hermes, a local execution agent. You can execute shell commands locally. If the user asks you to run a command, output exactly [RUN: command] or [SUDO: command] to execute as root via Shizuku. Do not wrap in markdown, just the raw tag if you need to execute. If you just want to talk, respond normally."
                "OpenClaude" -> "You are OpenClaude, an open-source Claude integration with full tool use."
                "Claude Code" -> "You are Claude Code, a specialized coding agent with codebase awareness."
                else -> "You are an expert AI assistant."
            }

            val agent = Agent(
                id = agentName.lowercase().replace(" ", "_"),
                name = agentName,
                description = "Agent provisioned during setup",
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
