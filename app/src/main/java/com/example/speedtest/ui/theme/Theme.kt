package com.example.speedtest.ui.theme

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

/**
 * ═══════════════════════════════════════════════════════════════
 *  Theme.kt — Material 3 Theming untuk Speed Test App
 *  Mendukung Light/Dark mode secara otomatis berdasarkan
 *  system preference pengguna.
 * ═══════════════════════════════════════════════════════════════
 */

val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = CyanPrimaryDark,
    secondary = IndigoPrimary,
    onSecondary = Color.White,
    secondaryContainer = IndigoDark,
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextOnDarkSecondary,
    outline = DarkSurfaceVariant
)

val LightColorScheme = lightColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = CyanLight,
    secondary = IndigoPrimary,
    onSecondary = Color.White,
    secondaryContainer = IndigoLight,
    background = LightBackground,
    onBackground = TextOnLight,
    surface = LightSurface,
    onSurface = TextOnLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextOnLightSecondary,
    outline = LightSurfaceVariant
)

@Composable
fun SpeedTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic Color tersedia di Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Set status bar color sesuai theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpeedTestTypography,
        content = content
    )
}
