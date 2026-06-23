package com.omniclaw.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.omniclaw.ui.theme.*

private const val DEFAULT_RADIUS = 16
private const val BORDER_DP = 1
private const val SCALE_PRESSED = 0.95f
private const val SCALE_NORMAL = 1.0f
private const val DAMPING_PRESS = 0.6f
private const val STIFFNESS_PRESS = 450f

@Composable
fun AnimatedGlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Int = DEFAULT_RADIUS,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleTarget = if (isPressed) SCALE_PRESSED else SCALE_NORMAL
    val animatedScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(dampingRatio = DAMPING_PRESS, stiffness = STIFFNESS_PRESS),
        label = "CardPressBounce"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                clip = true
            }
            .clip(RoundedCornerShape(radius.dp))
            .background(
                if (isPressed) OmniClawGlassOverlayPressed
                else OmniClawGlassOverlay
            )
            .border(BORDER_DP.dp, OmniClawGlassBorder, RoundedCornerShape(radius.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        content = content
    )
}
