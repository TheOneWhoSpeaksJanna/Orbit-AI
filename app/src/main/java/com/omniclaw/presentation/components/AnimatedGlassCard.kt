package com.omniclaw.presentation.components

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

@Composable
fun AnimatedGlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Int = 16,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleTarget = if (isPressed) 0.95f else 1.0f
    val animatedScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 450f),
        label = "CardPressBounce"
    )

    val animatedElevation by animateFloatAsState(
        targetValue = if (isPressed) 0f else 8f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "CardElevation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                shadowAlpha = 0.4f
                translationZ = animatedElevation
                clip = true
            }
            .clip(RoundedCornerShape(radius.dp))
            .background(
                if (isPressed) OmniClawGlassOverlayPressed
                else OmniClawGlassOverlay
            )
            .border(1.dp, OmniClawGlassBorder, RoundedCornerShape(radius.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        content = content
    )
}
