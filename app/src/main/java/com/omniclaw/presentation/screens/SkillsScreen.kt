package com.omniclaw.presentation.screens

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniclaw.presentation.viewmodels.SkillsViewModel
import com.omniclaw.ui.theme.OmniClawAccent
import com.omniclaw.ui.theme.OmniClawAccentSecondary
import com.omniclaw.ui.theme.OmniClawGlassOverlay
import com.omniclaw.ui.theme.OmniClawObsidianBase
import com.omniclaw.ui.theme.OmniClawSuccess
import com.omniclaw.ui.theme.OmniClawTextPrimary
import com.omniclaw.ui.theme.OmniClawTextSecondary
import com.omniclaw.ui.theme.OmniClawTextTertiary
import com.omniclaw.ui.theme.OmniClawWarning

@Composable
fun SkillsScreen(
    viewModel: SkillsViewModel = viewModel(factory = SkillsViewModel.Factory)
) {
    val agents by viewModel.agents.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniClawObsidianBase)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Active Capabilities",
                style = MaterialTheme.typography.headlineMedium,
                color = OmniClawTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Installed agents, tools, and extension modules",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniClawTextSecondary
            )
        }

        item {
            Text(
                text = "Agents",
                style = MaterialTheme.typography.titleMedium,
                color = OmniClawAccent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(agents) { agent ->
            CapabilityCard(
                name = agent.name,
                description = agent.systemPrompt?.take(80) ?: "General-purpose agent",
                icon = Icons.Default.Memory,
                accentColor = OmniClawAccent,
                status = "Active"
            )
        }

        if (agents.isEmpty()) {
            item {
                CapabilityCard(
                    name = "Default Agent",
                    description = "General-purpose AI orchestration agent",
                    icon = Icons.Default.Memory,
                    accentColor = OmniClawAccent,
                    status = "Ready"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registered Tools",
                style = MaterialTheme.typography.titleMedium,
                color = OmniClawAccentSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            val defaultTools = remember {
                listOf(
                    "Execute Command" to "Run shell commands via terminal",
                    "Read File" to "Read file contents from the filesystem",
                    "Glob Search" to "Pattern-based file discovery"
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                defaultTools.forEach { (name, desc) ->
                    CapabilityCard(
                        name = name,
                        description = desc,
                        icon = Icons.Default.Build,
                        accentColor = OmniClawAccentSecondary,
                        status = "Available"
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityCard(
    name: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    status: String
) {
    val shape = remember { RoundedCornerShape(14.dp) }
    val statusColor = when (status.lowercase()) {
        "active", "enabled", "ready", "available" -> OmniClawSuccess
        else -> OmniClawWarning
    }

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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = accentColor
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OmniClawTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniClawTextTertiary
                )
            }
        }
    }
}
