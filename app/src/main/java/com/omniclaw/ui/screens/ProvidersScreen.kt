package com.omniclaw.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniclaw.ui.components.BrandIcons
import com.omniclaw.ui.viewmodels.ConnectionState
import com.omniclaw.ui.viewmodels.ProviderConfig
import com.omniclaw.ui.viewmodels.ProvidersViewModel
import com.omniclaw.ui.theme.OmniClawAccent
import com.omniclaw.ui.theme.OmniClawAccentSecondary
import com.omniclaw.ui.theme.OmniClawGlassBorder
import com.omniclaw.ui.theme.OmniClawGlassOverlay
import com.omniclaw.ui.theme.OmniClawObsidianBase
import com.omniclaw.ui.theme.OmniClawObsidianSurface
import com.omniclaw.ui.theme.OmniClawSuccess
import com.omniclaw.ui.theme.OmniClawTextPrimary
import com.omniclaw.ui.theme.OmniClawTextSecondary
import com.omniclaw.ui.theme.OmniClawTextTertiary
import com.omniclaw.ui.theme.OmniClawWarning

private const val TITLE = "API Providers"
private const val SUBTITLE = "Verify connectivity and manage endpoint configurations"
private const val NO_API_KEY_LABEL = "No API key configured"
private const val CD_VERIFY = "Verify"
private const val CD_TESTING = "Testing"
private const val STATUS_NOT_VERIFIED = "Not verified"
private const val STATUS_VERIFYING = "Verifying connection..."
private const val STATUS_CONNECTED = "Connected"
private const val STATUS_OFFLINE = "Offline / No connection"

private val ERROR_RED = Color(0xFFEF4444)

private val PROVIDER_COLORS = mapOf(
    "Claude" to Color(0xFFCC7832),
    "OpenAI" to Color(0xFF10A37F),
    "Gemini" to Color(0xFF4285F4),
    "OpenRouter" to Color(0xFFFF6B35),
    "DeepSeek" to Color(0xFF4F6CF7),
    "Groq" to Color(0xFFF97316),
    "Ollama" to Color(0xFF8B5CF6)
)

private val PROVIDER_ICONS = mapOf(
    "Claude" to BrandIcons.Claude,
    "OpenAI" to BrandIcons.OpenAI,
    "Gemini" to BrandIcons.Gemini,
    "OpenRouter" to BrandIcons.OpenRouter,
    "DeepSeek" to BrandIcons.DeepSeek,
    "Groq" to BrandIcons.Groq,
    "Ollama" to BrandIcons.Ollama
)

@Composable
fun ProvidersScreen(
    viewModel: ProvidersViewModel = viewModel(factory = ProvidersViewModel.Factory)
) {
    val providers by viewModel.providers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniClawObsidianBase)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = TITLE,
                style = MaterialTheme.typography.headlineMedium,
                color = OmniClawTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniClawTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(providers, key = { it.name }) { provider ->
            ProviderHealthCard(
                provider = provider,
                onVerify = { viewModel.verifyConnection(provider.name) }
            )
        }
    }
}

@Composable
private fun ProviderHealthCard(
    provider: ProviderConfig,
    onVerify: () -> Unit
) {
    val shape = remember { RoundedCornerShape(14.dp) }

    val statusColor by animateColorAsState(
        targetValue = provider.connectionState.statusColor(),
        animationSpec = spring(dampingRatio = 0.8f),
        label = "statusColor"
    )

    val statusText = provider.connectionState.statusText()
    val isVerifying = provider.connectionState is ConnectionState.Verifying

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OmniClawGlassOverlay)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(providerAccent(provider.name).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = providerIcon(provider.name),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = providerAccent(provider.name)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = OmniClawTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniClawTextSecondary
                    )
                }
                if (!provider.apiKeyConfigured) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = NO_API_KEY_LABEL,
                        style = MaterialTheme.typography.labelSmall,
                        color = OmniClawWarning
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onVerify,
                enabled = !isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniClawObsidianSurface,
                    contentColor = OmniClawAccent
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = OmniClawAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (isVerifying) CD_TESTING else CD_VERIFY,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun providerAccent(name: String): Color =
    PROVIDER_COLORS[name] ?: OmniClawAccent

private fun providerIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector =
    PROVIDER_ICONS[name] ?: BrandIcons.OpenRouter

private fun ConnectionState.statusColor(): Color = when (this) {
    is ConnectionState.Idle -> OmniClawTextTertiary
    is ConnectionState.Verifying -> OmniClawAccent
    is ConnectionState.Connected -> OmniClawSuccess
    is ConnectionState.Unauthorized -> OmniClawWarning
    is ConnectionState.Offline -> ERROR_RED
    is ConnectionState.Error -> ERROR_RED
}

private fun ConnectionState.statusText(): String = when (this) {
    is ConnectionState.Idle -> STATUS_NOT_VERIFIED
    is ConnectionState.Verifying -> STATUS_VERIFYING
    is ConnectionState.Connected -> STATUS_CONNECTED
    is ConnectionState.Unauthorized -> this.message
    is ConnectionState.Offline -> STATUS_OFFLINE
    is ConnectionState.Error -> this.message
}
