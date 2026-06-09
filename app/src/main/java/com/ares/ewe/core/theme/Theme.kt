package com.ares.ewe.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DobbyColors.Primary,
    onPrimary = DobbyColors.OnPrimary,
    primaryContainer = DobbyPureScale.Mist,
    onPrimaryContainer = DobbyColors.TextPrimary,
    secondary = DobbyColors.Accent,
    onSecondary = DobbyPureScale.Pure,
    tertiary = DobbyColors.Warning,
    onTertiary = DobbyColors.TextPrimary,
    background = DobbyColors.ScreenBackground,
    onBackground = DobbyColors.TextPrimary,
    surface = DobbyColors.CardSurface,
    onSurface = DobbyColors.TextPrimary,
    surfaceVariant = DobbyPureScale.Fog,
    onSurfaceVariant = DobbyColors.TextSecondary,
    outline = DobbyColors.IconBorder,
    outlineVariant = DobbyPureScale.Mist,
)

@Composable
fun DobbyTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
