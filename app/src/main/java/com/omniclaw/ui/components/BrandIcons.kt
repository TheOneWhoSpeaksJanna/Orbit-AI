package com.omniclaw.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object BrandIcons {

    val Claude: ImageVector = ImageVector.Builder(
        name = "Claude",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Outer hexagon
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            lineTo(21f, 7f)
            lineTo(21f, 17f)
            lineTo(12f, 22f)
            lineTo(3f, 17f)
            lineTo(3f, 7f)
            close()
        }
        // Inner cutout
        path(fill = SolidColor(Color(0xFF1A1A2E))) {
            moveTo(12f, 6f)
            lineTo(6f, 10f)
            lineTo(6f, 14f)
            lineTo(12f, 18f)
            lineTo(18f, 14f)
            lineTo(18f, 10f)
            close()
        }
    }.build()

    val Gemini: ImageVector = ImageVector.Builder(
        name = "Gemini",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // Vertical diamond
            moveTo(12f, 3f)
            lineTo(15f, 12f)
            lineTo(12f, 21f)
            lineTo(9f, 12f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            // Horizontal diamond
            moveTo(3f, 12f)
            lineTo(12f, 9f)
            lineTo(21f, 12f)
            lineTo(12f, 15f)
            close()
        }
    }.build()

    val OpenAI: ImageVector = ImageVector.Builder(
        name = "OpenAI",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // Octagon — simplified compass mark
            moveTo(12f, 3f)
            lineTo(18f, 6f)
            lineTo(18f, 12f)
            lineTo(18f, 18f)
            lineTo(12f, 21f)
            lineTo(6f, 18f)
            lineTo(6f, 12f)
            lineTo(6f, 6f)
            close()
        }
        // Center dot
        path(fill = SolidColor(Color(0xFF1A1A2E))) {
            moveTo(11f, 11f)
            lineTo(13f, 11f)
            lineTo(13f, 13f)
            lineTo(11f, 13f)
            close()
        }
    }.build()

    val DeepSeek: ImageVector = ImageVector.Builder(
        name = "DeepSeek",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            stroke = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        ) {
            // Simplified "S" curve using line segments
            moveTo(6f, 16f)
            lineTo(6f, 14f)
            lineTo(10f, 14f)
            lineTo(10f, 10f)
            lineTo(6f, 10f)
            moveTo(18f, 8f)
            lineTo(14f, 8f)
            lineTo(14f, 12f)
            lineTo(18f, 12f)
            lineTo(18f, 16f)
            lineTo(14f, 16f)
        }
    }.build()

    val Groq: ImageVector = ImageVector.Builder(
        name = "Groq",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // Lightning bolt — Groq's brand mark
            moveTo(13f, 2f)
            lineTo(7f, 13f)
            lineTo(12f, 13f)
            lineTo(10f, 22f)
            lineTo(19f, 10f)
            lineTo(14f, 10f)
            close()
        }
    }.build()

    val Ollama: ImageVector = ImageVector.Builder(
        name = "Ollama",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Face — llama/alpaca silhouette
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 3f)
            lineTo(20f, 10f)
            lineTo(20f, 17f)
            lineTo(12f, 21f)
            lineTo(4f, 17f)
            lineTo(4f, 10f)
            close()
        }
        // Left ear
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 6f)
            lineTo(3f, 2f)
            lineTo(9f, 5f)
            close()
        }
        // Right ear
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 6f)
            lineTo(21f, 2f)
            lineTo(15f, 5f)
            close()
        }
        // Left eye — small square
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 12f)
            lineTo(10f, 12f)
            lineTo(10f, 14f)
            lineTo(8f, 14f)
            close()
        }
        // Right eye — small square
        path(fill = SolidColor(Color.White)) {
            moveTo(14f, 12f)
            lineTo(16f, 12f)
            lineTo(16f, 14f)
            lineTo(14f, 14f)
            close()
        }
    }.build()

    val OpenRouter: ImageVector = ImageVector.Builder(
        name = "OpenRouter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Outer hexagon — network node
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 3f)
            lineTo(20f, 7f)
            lineTo(20f, 17f)
            lineTo(12f, 21f)
            lineTo(4f, 17f)
            lineTo(4f, 7f)
            close()
        }
        // Inner hexagon — routing center
        path(fill = SolidColor(Color(0xFF1A1A2E))) {
            moveTo(12f, 8f)
            lineTo(16f, 10f)
            lineTo(16f, 14f)
            lineTo(12f, 16f)
            lineTo(8f, 14f)
            lineTo(8f, 10f)
            close()
        }
        // Corner dots (small squares)
        path(fill = SolidColor(Color.White)) {
            moveTo(4.5f, 7.5f)
            lineTo(5.5f, 7.5f)
            lineTo(5.5f, 8.5f)
            lineTo(4.5f, 8.5f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(18.5f, 7.5f)
            lineTo(19.5f, 7.5f)
            lineTo(19.5f, 8.5f)
            lineTo(18.5f, 8.5f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(4.5f, 15.5f)
            lineTo(5.5f, 15.5f)
            lineTo(5.5f, 16.5f)
            lineTo(4.5f, 16.5f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(18.5f, 15.5f)
            lineTo(19.5f, 15.5f)
            lineTo(19.5f, 16.5f)
            lineTo(18.5f, 16.5f)
            close()
        }
    }.build()
}
