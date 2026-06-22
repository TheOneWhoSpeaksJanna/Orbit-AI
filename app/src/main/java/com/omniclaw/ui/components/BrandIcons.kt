package com.omniclaw.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object BrandIcons {

    val Claude: ImageVector = ImageVector.Builder(
        name = "Claude",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd
        ) {
            // Anthropic hexagonal diamond
            moveTo(12f, 1f)
            lineTo(22f, 7.5f)
            lineTo(22f, 16.5f)
            lineTo(12f, 23f)
            lineTo(2f, 16.5f)
            lineTo(2f, 7.5f)
            close()
            // Inner cutout creates the ring effect
            moveTo(12f, 5f)
            lineTo(5f, 10f)
            lineTo(5f, 14.5f)
            lineTo(12f, 19.5f)
            lineTo(19f, 14.5f)
            lineTo(19f, 10f)
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
            // Four-pointed diamond star — Google Gemini
            moveTo(12f, 2f)
            quadraticTo(12f, 10f, 22f, 12f)
            quadraticTo(12f, 14f, 12f, 22f)
            quadraticTo(10f, 12f, 2f, 12f)
            quadraticTo(12f, 10f, 12f, 2f)
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
            // Six-petal flower / compass — OpenAI brand
            val cx = 12f; val cy = 12f; val r = 9.5f
            for (i in 0 until 6) {
                val a1 = i * 60.0
                val a2 = (i * 60.0 + 30.0)
                val a3 = (i * 60.0 + 60.0)
                val rad1 = a1 * PI / 180.0
                val rad3 = a3 * PI / 180.0

                val p1x = cx + r * cos(rad1).toFloat()
                val p1y = cy + r * sin(rad1).toFloat()
                val p3x = cx + r * cos(rad3).toFloat()
                val p3y = cy + r * sin(rad3).toFloat()

                if (i == 0) moveTo(cx, cy)
                lineTo(p1x, p1y)

                val midR = a2 * PI / 180.0
                val cpx = cx + (r * 0.5f) * cos(midR).toFloat()
                val cpy = cy + (r * 0.5f) * sin(midR).toFloat()
                quadraticTo(cpx, cpy, p3x, p3y)

                lineTo(cx, cy)
            }
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
            fill = null,
            stroke = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        ) {
            // Stylized sine-wave / S-curve — DeepSeek brand
            moveTo(4f, 16f)
            cubicTo(4f, 7f, 10f, 7f, 12f, 12f)
            cubicTo(14f, 17f, 20f, 17f, 20f, 8f)
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
            // Lightning bolt — Groq's mark
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
            moveTo(12f, 2f)
            lineTo(21f, 10f)
            lineTo(21f, 17f)
            lineTo(12f, 22f)
            lineTo(3f, 17f)
            lineTo(3f, 10f)
            close()
        }
        // Left ear
        path(fill = SolidColor(Color.White)) {
            moveTo(5f, 6f)
            lineTo(2f, 1f)
            lineTo(8f, 4.5f)
            close()
        }
        // Right ear
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 6f)
            lineTo(22f, 1f)
            lineTo(16f, 4.5f)
            close()
        }
        // Eyes
        path(fill = SolidColor(Color.White)) {
            addOval(Rect(7.5f, 12f, 9.5f, 14f))
        }
        path(fill = SolidColor(Color.White)) {
            addOval(Rect(14.5f, 12f, 16.5f, 14f))
        }
    }.build()

    val OpenRouter: ImageVector = ImageVector.Builder(
        name = "OpenRouter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Outer hexagon — network node / routing
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            lineTo(21f, 7f)
            lineTo(21f, 17f)
            lineTo(12f, 22f)
            lineTo(3f, 17f)
            lineTo(3f, 7f)
            close()
        }
        // Inner node — routing center
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 8f)
            lineTo(16f, 10f)
            lineTo(16f, 14f)
            lineTo(12f, 16f)
            lineTo(8f, 14f)
            lineTo(8f, 10f)
            close()
        }
        // Corner connection dots
        path(fill = SolidColor(Color.White)) {
            addOval(Rect(4.5f, 7.5f, 5.5f, 8.5f))
        }
        path(fill = SolidColor(Color.White)) {
            addOval(Rect(18.5f, 7.5f, 19.5f, 8.5f))
        }
        path(fill = SolidColor(Color.White)) {
            addOval(Rect(4.5f, 15.5f, 5.5f, 16.5f))
        }
        path(fill = SolidColor(Color.White)) {
            addOval(Rect(18.5f, 15.5f, 19.5f, 16.5f))
        }
    }.build()
}
