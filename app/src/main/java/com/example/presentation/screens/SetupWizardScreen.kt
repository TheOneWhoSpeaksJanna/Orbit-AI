package com.example.presentation.screens

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.viewmodels.SetupViewModel

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
                if (currentStep < 5) {
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
                    2 -> ShizukuStep(viewModel)
                    3 -> AgentSelectionStep(viewModel)
                    4 -> ProviderSetupStep(viewModel)
                    5 -> RuntimeSetupStep() // Bundled Runtime & Final Summary
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        onClick = null // null recommended for accessibility with parent select
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
fun ShizukuStep(viewModel: SetupViewModel) {
    val shizukuEnabled by viewModel.shizukuEnabled.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Elevated Permissions (Optional)", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Orbit AI can use Shizuku to perform system-level actions (like securely managing apps). This does not claim root access unless your device is rooted.",
            style = MaterialTheme.typography.bodyMedium
        )
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
                    onCheckedChange = { viewModel.setShizukuEnabled(it) }
                )
            }
        }
    }
}

@Composable
fun AgentSelectionStep(viewModel: SetupViewModel) {
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val agents = listOf("Hermes", "OpenClaude")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select Default Agent", style = MaterialTheme.typography.headlineMedium)
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
                            val desc = if (agent == "Hermes") "General purpose coding and logic via Gemini API." else "Expert context handling via Anthropic."
                            Text(text = desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderSetupStep(viewModel: SetupViewModel) {
    val agent by viewModel.selectedAgent.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val success by viewModel.testConnectionSuccess.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Provider Setup: $agent", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.setApiKey(it) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        
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
                Text("Bootstrapping complete.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text("No external apps required. The python server bridge was prepared.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Summary:", style = MaterialTheme.typography.titleMedium)
        Text("- UI configured", style = MaterialTheme.typography.bodyMedium)
        Text("- AI Agent selected", style = MaterialTheme.typography.bodyMedium)
        Text("- API keys active", style = MaterialTheme.typography.bodyMedium)
        Text("- Command runner ready", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You are ready to enter the workspace.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
