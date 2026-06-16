package com.example.presentation.screens

import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.viewmodels.SetupViewModel
import rikka.shizuku.Shizuku

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupWizardScreen(
    onFinishSetup: () -> Unit,
    viewModel: SetupViewModel = viewModel(factory = SetupViewModel.Factory)
) {
    val currentStep by viewModel.currentStep.collectAsState()

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                if (currentStep > 0) {
                    TextButton(onClick = { viewModel.previousStep() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (currentStep < 6) {
                    Button(onClick = { viewModel.nextStep() }) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                } else {
                    Button(onClick = {
                        viewModel.completeSetup()
                        onFinishSetup()
                    }) {
                        Text("Finish Setup")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                }, label = "SetupWizardTransition"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep()
                    1 -> ThemeSelectionStep(viewModel)
                    2 -> AgentSelectionStep(viewModel)
                    3 -> ProviderSelectionStep(viewModel)
                    4 -> ApiModelConfigStep(viewModel)
                    5 -> ShizukuStep(viewModel)
                    6 -> RuntimeSetupStep() // Bundled Runtime & Final Summary
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Orbit AI", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your powerful on-device agent workspace. Let's get everything configured securely.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ThemeSelectionStep(viewModel: SetupViewModel) {
    val theme by viewModel.theme.collectAsState()
    val themes = listOf("System", "Dark", "Light")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Appearance", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(Modifier.selectableGroup()) {
            themes.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (text == theme),
                            onClick = { viewModel.setTheme(text) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (text == theme),
                        onClick = null 
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentSelectionStep(viewModel: SetupViewModel) {
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val agents = listOf("Hermes", "OpenClaude", "Claude Code")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select Agent", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(Modifier.selectableGroup()) {
            agents.forEach { agent ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectable(
                            selected = (agent == selectedAgent),
                            onClick = { viewModel.setSelectedAgent(agent) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (agent == selectedAgent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder(agent == selectedAgent)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (agent == selectedAgent),
                            onClick = null
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(text = agent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val desc = when (agent) {
                                "Hermes" -> "General purpose logic and local orchestration."
                                "OpenClaude" -> "Open-source Claude integration with full tool use."
                                "Claude Code" -> "Specialized coding agent with codebase awareness."
                                else -> "Expert context handling."
                            }
                            Text(text = desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderSelectionStep(viewModel: SetupViewModel) {
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val agent by viewModel.selectedAgent.collectAsState()
    val providers = listOf("Claude", "OpenRouter", "OpenAI", "Gemini")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select Provider for $agent", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(Modifier.selectableGroup()) {
            providers.forEach { provider ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectable(
                            selected = (provider == selectedProvider),
                            onClick = { viewModel.setSelectedProvider(provider) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (provider == selectedProvider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder(provider == selectedProvider)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (provider == selectedProvider),
                            onClick = null
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(text = provider, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApiModelConfigStep(viewModel: SetupViewModel) {
    val provider by viewModel.selectedProvider.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val model by viewModel.selectedModel.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val success by viewModel.testConnectionSuccess.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Configure $provider", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.setApiKey(it) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = model,
            onValueChange = { viewModel.setSelectedModel(it) },
            label = { Text("Model Name (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { viewModel.testConnection() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTesting
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Test Connection")
            }
        }
        
        if (success != null) {
            Spacer(modifier = Modifier.height(8.dp))
            if (success == true) {
                Text("Connection successful!", color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Connection failed. Check your API key.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ShizukuStep(viewModel: SetupViewModel) {
    val shizukuEnabled by viewModel.shizukuEnabled.collectAsState()
    val context = LocalContext.current
    var shizukuStatus by remember { mutableStateOf("Checking...") }
    var hasPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val isInstalled = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        
        if (isInstalled) {
            if (Shizuku.pingBinder()) {
                hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                shizukuStatus = if (hasPermission) "Shizuku is running and permission granted." else "Shizuku running, but permission denied."
            } else {
                shizukuStatus = "Shizuku is installed but NOT running."
            }
        } else {
            shizukuStatus = "Shizuku is not installed on this device."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Elevated Permissions", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Orbit AI can use Shizuku to perform system-level actions (like securely managing apps). This does not claim root access unless your device is rooted.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status: $shizukuStatus", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (!hasPermission && Shizuku.pingBinder()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        try {
                            Shizuku.requestPermission(1000)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Text("Request Permission")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Shizuku Integration", style = MaterialTheme.typography.titleMedium)
                    Text("Requires Shizuku app to be running", style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = shizukuEnabled,
                    onCheckedChange = { viewModel.setShizukuEnabled(it) },
                    enabled = hasPermission
                )
            }
        }
    }
}

@Composable
fun RuntimeSetupStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Local Runtime Setup", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Orbit AI uses a companion service to execute local shell commands natively on your device.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Native runtime active.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text("No external apps like Termux are required. The internal command runner is ready.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Summary:", style = MaterialTheme.typography.titleMedium)
        Text("- UI configured", style = MaterialTheme.typography.bodyMedium)
        Text("- AI Agent and Provider selected", style = MaterialTheme.typography.bodyMedium)
        Text("- API keys active", style = MaterialTheme.typography.bodyMedium)
        Text("- Command runner ready", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You are ready to enter the workspace.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
