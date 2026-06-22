package com.omniclaw.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniclaw.ui.viewmodels.HistoryViewModel
import com.omniclaw.ui.theme.OmniClawAccent
import com.omniclaw.ui.theme.OmniClawGlassOverlay
import com.omniclaw.ui.theme.OmniClawObsidianBase
import com.omniclaw.ui.theme.OmniClawTextPrimary
import com.omniclaw.ui.theme.OmniClawTextSecondary
import com.omniclaw.ui.theme.OmniClawTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_HISTORY_FORMAT = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

@Composable
fun HistoryScreen(
    onOpenSession: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val sessions by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniClawObsidianBase)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Session History",
            style = MaterialTheme.typography.headlineMedium,
            color = OmniClawTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Browse past conversations and command logs",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniClawTextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = OmniClawTextTertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No session history yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OmniClawTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start a conversation to see it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniClawTextTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val formattedTime = remember(session.updatedAt) {
                        DATE_HISTORY_FORMAT.format(Date(session.updatedAt))
                    }
                    SessionHistoryCard(
                        title = session.title,
                        timestamp = formattedTime,
                        onClick = { onOpenSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    title: String,
    timestamp: String,
    onClick: () -> Unit
) {
    val shape = remember { RoundedCornerShape(14.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OmniClawGlassOverlay)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OmniClawAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = OmniClawAccent
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OmniClawTextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = OmniClawTextTertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniClawTextTertiary
                    )
                }
            }
        }
    }
}
