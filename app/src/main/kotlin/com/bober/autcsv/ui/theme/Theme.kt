package com.bober.autcsv.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = Surface3,
    tertiary = AccentDim,
    background = Background,
    surface = Surface,
    onPrimary = AccentInk,
    onSecondary = TextPrimary,
    onTertiary = TextDim,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Danger,
    onError = White,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,

    surfaceContainer = Surface3,
)

private val LightColorScheme = lightColorScheme(
    primary = LightAppPalette.Accent,
    secondary = LightAppPalette.Surface3,
    tertiary = LightAppPalette.AccentDim,
    background = LightAppPalette.Background,
    surface = LightAppPalette.Surface,
    onPrimary = LightAppPalette.AccentInk,
    onSecondary = LightAppPalette.TextPrimary,
    onTertiary = LightAppPalette.TextDim,
    onBackground = LightAppPalette.TextPrimary,
    onSurface = LightAppPalette.TextPrimary,
    error = LightAppPalette.Danger,
    onError = White,
    surfaceVariant = LightAppPalette.Surface2,
    onSurfaceVariant = LightAppPalette.TextDim,

    surfaceContainer = LightAppPalette.Surface3,
)

/**
 * Тема приложения: автоматически следует системной (светлая/тёмная).
 * Кастомные токены (Background, Accent, TextPrimary…) доступны через
 * [AppColors] и [LocalAppPalette], Material-цвета — через colorScheme.
 */
@Composable
fun AutCSVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = if (darkTheme) DarkAppPalette else LightAppPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
