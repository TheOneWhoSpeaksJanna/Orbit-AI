package com.omniclaw.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val DARK_ACCENT = Color(0xFF1A1A2E)

object BrandIcons {

    /** Anthropic Claude — hexagonal gem with diamond inset */
    val Claude: ImageVector = ImageVector.Builder(
        name = "Claude",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFFD4A574))) {
            moveTo(12f, 1.5f)
            lineTo(22f, 6.5f)
            lineTo(22f, 17.5f)
            lineTo(12f, 22.5f)
            lineTo(2f, 17.5f)
            lineTo(2f, 6.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFF8B5E3C))) {
            moveTo(12f, 5f)
            lineTo(7f, 8.5f)
            lineTo(7f, 15.5f)
            lineTo(12f, 19f)
            lineTo(17f, 15.5f)
            lineTo(17f, 8.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFF8F0))) {
            moveTo(12f, 8f)
            lineTo(9.5f, 10.5f)
            lineTo(9.5f, 13.5f)
            lineTo(12f, 16f)
            lineTo(14.5f, 13.5f)
            lineTo(14.5f, 10.5f)
            close()
        }
    }.build()

    /** Google Gemini — four-pointed sparkle star (Google's actual Gemini icon) */
    val Gemini: ImageVector = ImageVector.Builder(
        name = "Gemini",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Top point (stretched)
        path(fill = SolidColor(Color(0xFF4285F4))) {
            moveTo(12f, 2f)
            curveTo(12.8f, 5f, 14f, 7f, 15.5f, 8.5f)
            curveTo(17f, 10f, 19f, 11.2f, 22f, 12f)
            curveTo(19f, 12.8f, 17f, 14f, 15.5f, 15.5f)
            curveTo(14f, 17f, 12.8f, 19f, 12f, 22f)
            curveTo(11.2f, 19f, 10f, 17f, 8.5f, 15.5f)
            curveTo(7f, 14f, 5f, 12.8f, 2f, 12f)
            curveTo(5f, 11.2f, 7f, 10f, 8.5f, 8.5f)
            curveTo(10f, 7f, 11.2f, 5f, 12f, 2f)
            close()
        }
    }.build()

    /** OpenAI / ChatGPT — circular knot with center dot */
    val OpenAI: ImageVector = ImageVector.Builder(
        name = "OpenAI",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF10A37F))) {
            // Outer circle
            moveTo(12f, 3f)
            curveTo(17f, 3f, 21f, 7f, 21f, 12f)
            curveTo(21f, 17f, 17f, 21f, 12f, 21f)
            curveTo(7f, 21f, 3f, 17f, 3f, 12f)
            curveTo(3f, 7f, 7f, 3f, 12f, 3f)
            close()
        }
        path(fill = SolidColor(Color(0xFF0B8A6B))) {
            // Inner flower petal 1
            moveTo(12f, 7f)
            curveTo(11f, 7f, 10f, 7.5f, 9.5f, 8.2f)
            lineTo(12f, 10.5f)
            curveTo(12.5f, 10f, 13.5f, 10f, 14f, 10.5f)
            lineTo(16.5f, 8.2f)
            curveTo(16f, 7.5f, 15f, 7f, 14f, 7f)
            close()
        }
        path(fill = SolidColor(Color(0xFF0B8A6B))) {
            // Inner flower petal 2
            moveTo(7f, 12f)
            curveTo(7f, 11f, 7.5f, 10f, 8.2f, 9.5f)
            lineTo(10.5f, 12f)
            curveTo(10f, 12.5f, 10f, 13.5f, 10.5f, 14f)
            lineTo(8.2f, 16.5f)
            curveTo(7.5f, 16f, 7f, 15f, 7f, 14f)
            close()
        }
        path(fill = SolidColor(Color(0xFF0B8A6B))) {
            // Center dot
            moveTo(11f, 11f)
            lineTo(13f, 11f)
            lineTo(13f, 13f)
            lineTo(11f, 13f)
            close()
        }
    }.build()

    /** DeepSeek — stylized whale silhouette */
    val DeepSeek: ImageVector = ImageVector.Builder(
        name = "DeepSeek",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF4F6CF7))) {
            moveTo(4f, 14f)
            curveTo(5f, 10f, 8f, 7f, 12f, 7f)
            curveTo(16f, 7f, 19f, 10f, 20f, 14f)
            curveTo(21f, 17f, 20f, 19f, 20f, 19f)
            lineTo(18f, 17f)
            curveTo(16.5f, 18.5f, 14f, 19f, 12f, 19f)
            curveTo(10f, 19f, 7.5f, 18.5f, 6f, 17f)
            lineTo(4f, 19f)
            curveTo(4f, 19f, 3f, 17f, 4f, 14f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            // Tail
            moveTo(19f, 12f)
            lineTo(21f, 10f)
            lineTo(21f, 13f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            // Eye
            moveTo(9f, 12f)
            lineTo(10f, 12f)
            lineTo(10f, 13f)
            lineTo(9f, 13f)
            close()
        }
    }.build()

    /** Groq — lightning bolt in a hexagon (LPU brand mark) */
    val Groq: ImageVector = ImageVector.Builder(
        name = "Groq",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFFF97316))) {
            moveTo(12f, 3f)
            lineTo(20f, 7.5f)
            lineTo(20f, 16.5f)
            lineTo(12f, 21f)
            lineTo(4f, 16.5f)
            lineTo(4f, 7.5f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFB74D))) {
            moveTo(11f, 6f)
            lineTo(8f, 13f)
            lineTo(11f, 13f)
            lineTo(10f, 18f)
            lineTo(16f, 11f)
            lineTo(13f, 11f)
            close()
        }
    }.build()

    /** Ollama — stylized llama/alpaca head */
    val Ollama: ImageVector = ImageVector.Builder(
        name = "Ollama",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF8B5CF6))) {
            // Head
            moveTo(8f, 8f)
            curveTo(8f, 6f, 10f, 4f, 12f, 4f)
            curveTo(14f, 4f, 16f, 6f, 16f, 8f)
            lineTo(16f, 13f)
            curveTo(16f, 15f, 14f, 17f, 12f, 17f)
            curveTo(10f, 17f, 8f, 15f, 8f, 13f)
            close()
        }
        path(fill = SolidColor(Color(0xFFA78BFA))) {
            // Ears
            moveTo(9f, 5f)
            lineTo(7f, 2f)
            lineTo(10f, 4f)
            close()
            moveTo(15f, 5f)
            lineTo(17f, 2f)
            lineTo(14f, 4f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            // Eyes
            moveTo(10f, 9f)
            lineTo(11f, 9f)
            lineTo(11f, 10f)
            lineTo(10f, 10f)
            close()
            moveTo(13f, 9f)
            lineTo(14f, 9f)
            lineTo(14f, 10f)
            lineTo(13f, 10f)
            close()
        }
        path(fill = SolidColor(Color(0xFF6D28D9))) {
            // Nose/mouth
            moveTo(11f, 12f)
            lineTo(13f, 12f)
            lineTo(13f, 13f)
            lineTo(11f, 13f)
            close()
        }
    }.build()

    /** OpenRouter — network routing node with connections */
    val OpenRouter: ImageVector = ImageVector.Builder(
        name = "OpenRouter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFFFF6B35))) {
            // Central node
            moveTo(10f, 10f)
            lineTo(14f, 10f)
            lineTo(14f, 14f)
            lineTo(10f, 14f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFF6B35))) {
            // Outer ring
            moveTo(12f, 5f)
            curveTo(16f, 5f, 19f, 8f, 19f, 12f)
            curveTo(19f, 16f, 16f, 19f, 12f, 19f)
            curveTo(8f, 19f, 5f, 16f, 5f, 12f)
            curveTo(5f, 8f, 8f, 5f, 12f, 5f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFF9B6B))) {
            // Connection dots
            moveTo(12f, 4f)
            lineTo(12f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 4f)
            close()
            moveTo(12f, 19f)
            lineTo(12f, 20f)
            lineTo(13f, 20f)
            lineTo(13f, 19f)
            close()
            moveTo(4f, 12f)
            lineTo(5f, 12f)
            lineTo(5f, 11f)
            lineTo(4f, 11f)
            close()
            moveTo(19f, 12f)
            lineTo(20f, 12f)
            lineTo(20f, 11f)
            lineTo(19f, 11f)
            close()
        }
    }.build()

    /** GitHub — Octocat silhouette */
    val GitHub: ImageVector = ImageVector.Builder(
        name = "GitHub",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF24292F))) {
            // Octocat body
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 16.42f, 4.87f, 20.17f, 8.84f, 21.5f)
            curveTo(9.34f, 21.58f, 9.5f, 21.27f, 9.5f, 21f)
            curveTo(9.5f, 20.77f, 9.49f, 20.14f, 9.49f, 19.31f)
            curveTo(6.73f, 19.91f, 6.14f, 17.97f, 6.14f, 17.97f)
            curveTo(5.68f, 16.81f, 5.03f, 16.5f, 5.03f, 16.5f)
            curveTo(4.12f, 15.88f, 5.1f, 15.9f, 5.1f, 15.9f)
            curveTo(6.1f, 15.97f, 6.63f, 16.93f, 6.63f, 16.93f)
            curveTo(7.5f, 18.45f, 8.97f, 18f, 9.54f, 17.76f)
            curveTo(9.63f, 17.11f, 9.89f, 16.67f, 10.17f, 16.42f)
            curveTo(7.95f, 16.17f, 5.62f, 15.31f, 5.62f, 11.5f)
            curveTo(5.62f, 10.39f, 6f, 9.5f, 6.58f, 8.79f)
            curveTo(6.49f, 8.54f, 6.15f, 7.53f, 6.64f, 6.15f)
            curveTo(6.64f, 6.15f, 7.47f, 5.88f, 9.5f, 7.17f)
            curveTo(10.29f, 6.95f, 11.15f, 6.84f, 12f, 6.84f)
            curveTo(12.85f, 6.84f, 13.71f, 6.95f, 14.5f, 7.17f)
            curveTo(16.53f, 5.88f, 17.36f, 6.15f, 17.36f, 6.15f)
            curveTo(17.85f, 7.53f, 17.51f, 8.54f, 17.42f, 8.79f)
            curveTo(18f, 9.5f, 18.38f, 10.39f, 18.38f, 11.5f)
            curveTo(18.38f, 15.31f, 16.05f, 16.17f, 13.83f, 16.42f)
            curveTo(14.17f, 16.72f, 14.5f, 17.33f, 14.5f, 18.26f)
            curveTo(14.5f, 19.56f, 14.49f, 20.63f, 14.49f, 21f)
            curveTo(14.49f, 21.27f, 14.66f, 21.59f, 15.17f, 21.5f)
            curveTo(19.13f, 20.17f, 22f, 16.42f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
        }
    }.build()
}
