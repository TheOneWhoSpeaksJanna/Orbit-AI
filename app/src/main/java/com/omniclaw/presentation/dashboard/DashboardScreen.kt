package com.omniclaw.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniclaw.presentation.components.AnimatedGlassCard
import com.omniclaw.ui.theme.OmniClawAccentSecondary
import com.omniclaw.ui.theme.OmniClawObsidianBase
import com.omniclaw.ui.theme.OmniClawGlassBorder
import com.omniclaw.ui.theme.OmniClawTextSecondary

private data class DashboardTile(
    val title: String,
    val subtitle: String,
    val accentColor: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToTermux: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Staggered entry animation
    val infiniteTransition = rememberInfiniteTransition(label = "dashboardGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeaderGlow"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val tiles = remember {
        listOf(
            DashboardTile(
                title = "AI Orchestration Terminal",
                subtitle = "Access unified conversational language engine providers and continuous background streams.",
                accentColor = primaryColor,
                onClick = onNavigateToChat
            ),
            DashboardTile(
                title = "Alpine Shell Instance",
                subtitle = "Execute localized subsystem environment scripts, compile modules, and review system execution tasks.",
                accentColor = OmniClawAccentSecondary,
                onClick = onNavigateToTermux
            ),
            DashboardTile(
                title = "System Framework Settings",
                subtitle = "Configure custom provider models, authentication keys, and toggle core appearance options.",
                accentColor = surfaceVariantColor,
                onClick = onNavigateToSettings
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniClawObsidianBase)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = "Orbit Control Center",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer { alpha = glowAlpha }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "System active and synchronized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmniClawTextSecondary
                    )
                }
            }

            itemsIndexed(tiles) { index, tile ->
                StaggeredGlassTile(
                    index = index,
                    tile = tile
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Orbit v1.0 · Runtime Nominal",
                        style = MaterialTheme.typography.labelSmall,
                        color = OmniClawTextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredGlassTile(
    index: Int,
    tile: DashboardTile
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        visible = true
    }

    val offsetFraction by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "StaggerEntry${index}"
    )

    Box(
        modifier = Modifier
            .alpha(offsetFraction)
            .graphicsLayer {
                translationY = (1f - offsetFraction) * 40f
            }
    ) {
        AnimatedGlassCard(
            onClick = tile.onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = tile.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tile.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniClawTextSecondary,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }
    }
}
