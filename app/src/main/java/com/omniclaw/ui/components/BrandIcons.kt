package com.omniclaw.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.omniclaw.R

/**
 * Official brand icons for all supported AI providers.
 * Loaded from VectorDrawable XML resources.
 */
object BrandIcons {
    val Claude: Painter
        @Composable get() = painterResource(R.drawable.ic_claude)
    val OpenAI: Painter
        @Composable get() = painterResource(R.drawable.ic_openai)
    val Gemini: Painter
        @Composable get() = painterResource(R.drawable.ic_gemini)
    val OpenRouter: Painter
        @Composable get() = painterResource(R.drawable.ic_openrouter)
    val DeepSeek: Painter
        @Composable get() = painterResource(R.drawable.ic_deepseek)
    val Groq: Painter
        @Composable get() = painterResource(R.drawable.ic_groq)
    val Ollama: Painter
        @Composable get() = painterResource(R.drawable.ic_ollama)
}
