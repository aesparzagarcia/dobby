package com.ares.ewe.core.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DobbyColors.Primary,
    onPrimary = Color.White,
    primaryContainer = DobbyColors.Light,
    onPrimaryContainer = DobbyColors.Dark,
    secondary = DobbyColors.Accent,
    onSecondary = Color.White,
    tertiary = DobbyColors.Warning,
    onTertiary = DobbyColors.Dark,
    background = Color.White,
    onBackground = DobbyColors.Dark,
    surface = Color.White,
    onSurface = DobbyColors.Dark,
    surfaceVariant = DobbyColors.Light,
    onSurfaceVariant = DobbyColors.Dark.copy(alpha = 0.72f),
)

@Composable
fun DobbyTheme(
    // Dynamic color is available on Android 12+ (always light palette)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
