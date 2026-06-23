package com.omniclaw.ui.screens

import com.omniclaw.R
import com.omniclaw.data.local.updater.UpdateState
import com.omniclaw.data.local.updater.UpdateInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniclaw.ui.viewmodels.SettingsViewModel

private const val GITHUB_TOKEN_LABEL = "GitHub Token"
private const val THEME_SYSTEM = "System"
private const val THEME_DARK = "Dark"
private const val THEME_LIGHT = "Light"
private const val SECTION_UPDATES = "Updates"
private const val VERSION_PREFIX = "Version "
private const val BTN_CHECK_UPDATES = "Check for Updates"
private const val BTN_CHECKING = "Checking for updates..."
private const val BTN_DOWNLOAD = "Download Update"
private const val BTN_CHECK_AGAIN = "Check Again"
private const val BTN_INSTALL = "Install Update"
private const val BTN_RETRY = "Retry"
private const val MSG_UP_TO_DATE = "You're up to date!"
private const val MSG_DOWNLOAD_COMPLETE = "Download complete!"
private const val MSG_DOWNLOADING = "Downloading... "

private val THEME_OPTIONS = listOf(THEME_SYSTEM, THEME_DARK, THEME_LIGHT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val openAiApiKey by viewModel.openAiApiKey.collectAsState()
    val claudeApiKey by viewModel.claudeApiKey.collectAsState()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val appVersion = viewModel.appVersion

    var geminiVisible by remember { mutableStateOf(false) }
    var openAiVisible by remember { mutableStateOf(false) }
    var claudeVisible by remember { mutableStateOf(false) }
    var openRouterVisible by remember { mutableStateOf(false) }
    var githubTokenVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text(stringResource(R.string.ai_provider_config), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.api_key_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ApiKeyField(
                        value = geminiApiKey,
                        onValueChange = { viewModel.updateGeminiApiKey(it) },
                        label = stringResource(R.string.api_key_gemini),
                        visible = geminiVisible,
                        onToggleVisibility = { geminiVisible = !geminiVisible }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ApiKeyField(
                        value = openAiApiKey,
                        onValueChange = { viewModel.updateOpenAiApiKey(it) },
                        label = stringResource(R.string.api_key_openai),
                        visible = openAiVisible,
                        onToggleVisibility = { openAiVisible = !openAiVisible }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ApiKeyField(
                        value = claudeApiKey,
                        onValueChange = { viewModel.updateClaudeApiKey(it) },
                        label = stringResource(R.string.api_key_claude),
                        visible = claudeVisible,
                        onToggleVisibility = { claudeVisible = !claudeVisible }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ApiKeyField(
                        value = openRouterApiKey,
                        onValueChange = { viewModel.updateOpenRouterApiKey(it) },
                        label = stringResource(R.string.api_key_openrouter),
                        visible = openRouterVisible,
                        onToggleVisibility = { openRouterVisible = !openRouterVisible }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ApiKeyField(
                        value = githubToken,
                        onValueChange = { viewModel.updateGithubToken(it) },
                        label = GITHUB_TOKEN_LABEL,
                        visible = githubTokenVisible,
                        onToggleVisibility = { githubTokenVisible = !githubTokenVisible }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = themeMode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.theme_mode)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            THEME_OPTIONS.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        viewModel.updateThemeMode(selection)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text(SECTION_UPDATES, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$VERSION_PREFIX$appVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    when (val state = updateState) {
                        is UpdateState.Idle -> {
                            Button(
                                onClick = { viewModel.checkForUpdates() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(BTN_CHECK_UPDATES)
                            }
                        }
                        is UpdateState.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(BTN_CHECKING, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is UpdateState.Available -> {
                            Text(
                                "Update v${state.info.latestVersion} available",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (state.info.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    state.info.releaseNotes.take(200),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.downloadUpdate(state.info) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(BTN_DOWNLOAD)
                            }
                        }
                        is UpdateState.UpToDate -> {
                            Text(
                                MSG_UP_TO_DATE,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.checkForUpdates() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(BTN_CHECK_AGAIN)
                            }
                        }
                        is UpdateState.Downloading -> {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$MSG_DOWNLOADING${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is UpdateState.Downloaded -> {
                            Text(
                                MSG_DOWNLOAD_COMPLETE,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.installUpdate(state.filePath) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(BTN_INSTALL)
                            }
                        }
                        is UpdateState.Failed -> {
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.checkForUpdates() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(BTN_RETRY)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) stringResource(R.string.hide) else stringResource(R.string.show)
                )
            }
        }
    )
}
