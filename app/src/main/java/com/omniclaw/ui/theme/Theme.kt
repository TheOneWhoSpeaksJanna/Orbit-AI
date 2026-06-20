package com.omniclaw.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OmniClawDarkScheme = darkColorScheme(
    primary = OmniClawAccent,
    onPrimary = Color.White,
    primaryContainer = OmniClawAccent.copy(alpha = 0.12f),
    onPrimaryContainer = OmniClawAccent,
    secondary = OmniClawAccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = OmniClawAccentSecondary.copy(alpha = 0.12f),
    onSecondaryContainer = OmniClawAccentSecondary,
    tertiary = OmniClawSuccess,
    onTertiary = OmniClawBgDark,
    tertiaryContainer = OmniClawSuccess.copy(alpha = 0.12f),
    onTertiaryContainer = OmniClawSuccess,
    background = OmniClawBgDark,
    onBackground = OmniClawTextPrimary,
    surface = OmniClawSurfaceDark,
    onSurface = OmniClawTextPrimary,
    surfaceVariant = OmniClawSurfaceElevated,
    onSurfaceVariant = OmniClawTextSecondary,
    outline = OmniClawTextTertiary.copy(alpha = 0.5f),
    outlineVariant = OmniClawTextTertiary.copy(alpha = 0.25f),
    error = OmniClawError,
    onError = OmniClawBgDark,
    errorContainer = OmniClawError.copy(alpha = 0.12f),
    onErrorContainer = OmniClawError,
    inverseSurface = OmniClawTextPrimary,
    inverseOnSurface = OmniClawBgDark,
    inversePrimary = OmniClawAccent.copy(alpha = 0.8f),
    surfaceTint = OmniClawAccent
)

private val OmniClawLightScheme = lightColorScheme(
    primary = OmniClawAccent,
    onPrimary = Color.White,
    primaryContainer = OmniClawAccent.copy(alpha = 0.12f),
    onPrimaryContainer = OmniClawAccent,
    secondary = OmniClawAccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = OmniClawAccentSecondary.copy(alpha = 0.12f),
    onSecondaryContainer = OmniClawAccentSecondary,
    tertiary = OmniClawSuccess,
    onTertiary = Color.White,
    tertiaryContainer = OmniClawSuccess.copy(alpha = 0.12f),
    onTertiaryContainer = Color(0xFF065F46),
    background = OmniClawBgLight,
    onBackground = OmniClawTextPrimaryLight,
    surface = OmniClawSurfaceLight,
    onSurface = OmniClawTextPrimaryLight,
    surfaceVariant = OmniClawSurfaceLightElevated,
    onSurfaceVariant = OmniClawTextSecondaryLight,
    outline = OmniClawTextTertiaryLight.copy(alpha = 0.5f),
    outlineVariant = OmniClawTextTertiaryLight.copy(alpha = 0.25f),
    error = OmniClawError,
    onError = Color.White,
    errorContainer = OmniClawError.copy(alpha = 0.12f),
    onErrorContainer = Color(0xFF93000A),
    inverseSurface = OmniClawBgDark,
    inverseOnSurface = OmniClawTextPrimary,
    inversePrimary = OmniClawAccent.copy(alpha = 0.8f),
    surfaceTint = OmniClawAccent
)

@Composable
fun OmniClawTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OmniClawDarkScheme
        else -> OmniClawLightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
