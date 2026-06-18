package com.example.ui.theme

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

private val LunelDarkScheme = darkColorScheme(
    primary = LunelAccent,
    onPrimary = Color.White,
    primaryContainer = LunelAccent.copy(alpha = 0.12f),
    onPrimaryContainer = LunelAccent,
    secondary = LunelAccentMuted,
    onSecondary = Color.White,
    secondaryContainer = LunelAccentMuted.copy(alpha = 0.12f),
    onSecondaryContainer = LunelAccentMuted,
    tertiary = LunelSuccess,
    onTertiary = LunelDarkBgBase,
    tertiaryContainer = LunelSuccess.copy(alpha = 0.12f),
    onTertiaryContainer = LunelSuccess,
    background = LunelDarkBgBase,
    onBackground = LunelFgDefault,
    surface = LunelDarkBgRaised,
    onSurface = LunelFgDefault,
    surfaceVariant = LunelDarkBgElevated,
    onSurfaceVariant = LunelFgMuted,
    outline = LunelFgSubtle.copy(alpha = 0.5f),
    outlineVariant = LunelFgSubtle.copy(alpha = 0.25f),
    error = LunelError,
    onError = LunelDarkBgBase,
    errorContainer = LunelError.copy(alpha = 0.12f),
    onErrorContainer = LunelError,
    inverseSurface = LunelFgDefault,
    inverseOnSurface = LunelDarkBgBase,
    inversePrimary = LunelAccent.copy(alpha = 0.8f),
    surfaceTint = LunelAccent
)

private val LunelLightScheme = lightColorScheme(
    primary = LunelAccent,
    onPrimary = Color.White,
    primaryContainer = LunelAccent.copy(alpha = 0.12f),
    onPrimaryContainer = LunelAccent,
    secondary = LunelAccentMuted,
    onSecondary = Color.White,
    secondaryContainer = LunelAccentMuted.copy(alpha = 0.12f),
    onSecondaryContainer = LunelAccentMuted,
    tertiary = LunelSuccess,
    onTertiary = Color.White,
    tertiaryContainer = LunelSuccess.copy(alpha = 0.12f),
    onTertiaryContainer = Color(0xFF065F46),
    background = LunelLightBgBase,
    onBackground = LunelLightFgDefault,
    surface = LunelLightBgRaised,
    onSurface = LunelLightFgDefault,
    surfaceVariant = LunelLightBgElevated,
    onSurfaceVariant = LunelLightFgMuted,
    outline = LunelLightFgSubtle.copy(alpha = 0.5f),
    outlineVariant = LunelLightFgSubtle.copy(alpha = 0.25f),
    error = LunelError,
    onError = Color.White,
    errorContainer = LunelError.copy(alpha = 0.12f),
    onErrorContainer = Color(0xFF93000A),
    inverseSurface = LunelDarkBgBase,
    inverseOnSurface = LunelFgDefault,
    inversePrimary = LunelAccent.copy(alpha = 0.8f),
    surfaceTint = LunelAccent
)

@Composable
fun OrbitTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LunelDarkScheme
        else -> LunelLightScheme
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
