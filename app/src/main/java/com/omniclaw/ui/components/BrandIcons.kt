package com.omniclaw.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val DARK_ACCENT = Color(0xFF1A1A2E)

object BrandIcons {

    val Claude: ImageVector = ImageVector.Builder(
        name = "Claude",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            lineTo(21f, 7f)
            lineTo(21f, 17f)
            lineTo(12f, 22f)
            lineTo(3f, 17f)
            lineTo(3f, 7f)
            close()
        }
        path(fill = SolidColor(DARK_ACCENT)) {
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
            moveTo(12f, 3f)
            lineTo(15f, 12f)
            lineTo(12f, 21f)
            lineTo(9f, 12f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
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
        path(fill = SolidColor(DARK_ACCENT)) {
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
        path(fill = SolidColor(Color.White)) {
            moveTo(5f, 16f)
            lineTo(5f, 12f)
            lineTo(8f, 12f)
            lineTo(8f, 9f)
            lineTo(11f, 9f)
            lineTo(11f, 8f)
            lineTo(16f, 8f)
            lineTo(16f, 10f)
            lineTo(19f, 10f)
            lineTo(19f, 16f)
            lineTo(16f, 16f)
            lineTo(16f, 14f)
            lineTo(13f, 14f)
            lineTo(13f, 15f)
            lineTo(8f, 15f)
            lineTo(8f, 16f)
            close()
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
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 3f)
            lineTo(20f, 10f)
            lineTo(20f, 17f)
            lineTo(12f, 21f)
            lineTo(4f, 17f)
            lineTo(4f, 10f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 6f)
            lineTo(3f, 2f)
            lineTo(9f, 5f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 6f)
            lineTo(21f, 2f)
            lineTo(15f, 5f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 12f)
            lineTo(10f, 12f)
            lineTo(10f, 14f)
            lineTo(8f, 14f)
            close()
        }
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
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 3f)
            lineTo(20f, 7f)
            lineTo(20f, 17f)
            lineTo(12f, 21f)
            lineTo(4f, 17f)
            lineTo(4f, 7f)
            close()
        }
        path(fill = SolidColor(DARK_ACCENT)) {
            moveTo(12f, 8f)
            lineTo(16f, 10f)
            lineTo(16f, 14f)
            lineTo(12f, 16f)
            lineTo(8f, 14f)
            lineTo(8f, 10f)
            close()
        }
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
