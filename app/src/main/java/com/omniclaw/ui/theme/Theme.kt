package com.omniclaw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class OmniClawThemeMode {
    SYSTEM, LIGHT, DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = OmniClawPrimary,
    onPrimary = OmniClawObsidianBase,
    primaryContainer = OmniClawPrimaryGlow,
    onPrimaryContainer = OmniClawPrimaryLight,
    secondary = OmniClawSecondary,
    onSecondary = OmniClawObsidianSurface,
    secondaryContainer = OmniClawSecondaryGlow,
    onSecondaryContainer = OmniClawSecondaryLight,
    tertiary = OmniClawTertiaryLight,
    onTertiary = OmniClawObsidianSurface,
    tertiaryContainer = OmniClawTertiaryGlow,
    onTertiaryContainer = OmniClawTertiaryLight,
    background = OmniClawObsidianSurface,
    onBackground = OmniClawTextPrimary,
    surface = OmniClawSurfaceDark,
    onSurface = OmniClawTextPrimary,
    surfaceVariant = OmniClawSurfaceElevated,
    onSurfaceVariant = OmniClawTextSecondary,
    surfaceTint = OmniClawPrimary,
    inverseSurface = OmniClawSurfaceLight,
    inverseOnSurface = OmniClawTextPrimaryLight,
    outline = OmniClawGlassBorder,
    outlineVariant = OmniClawSurfaceElevated,
    error = OmniClawError,
    onError = OmniClawTextPrimary,
    errorContainer = Color(0x40EF4444),
    onErrorContainer = OmniClawError,
    scrim = OmniClawObsidianBase,
)

private val LightColorScheme = lightColorScheme(
    primary = OmniClawPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEEAFF),
    onPrimaryContainer = OmniClawPrimaryDark,
    secondary = OmniClawSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FF),
    onSecondaryContainer = OmniClawSecondaryDark,
    tertiary = OmniClawTertiaryDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDE8FF),
    onTertiaryContainer = OmniClawTertiaryDark,
    background = OmniClawBgLight,
    onBackground = OmniClawTextPrimaryLight,
    surface = OmniClawSurfaceLight,
    onSurface = OmniClawTextPrimaryLight,
    surfaceVariant = OmniClawSurfaceLightElevated,
    onSurfaceVariant = OmniClawTextSecondaryLight,
    surfaceTint = OmniClawPrimaryDark,
    inverseSurface = OmniClawObsidianSurface,
    inverseOnSurface = OmniClawTextPrimary,
    outline = OmniClawGlassBorderLight,
    outlineVariant = Color(0xFFF1F5F9),
    error = OmniClawError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    scrim = Color.Black,
)

@Composable
fun OmniClawTheme(
    themeMode: OmniClawThemeMode = OmniClawThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        OmniClawThemeMode.SYSTEM -> isSystemInDarkTheme()
        OmniClawThemeMode.LIGHT -> false
        OmniClawThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
