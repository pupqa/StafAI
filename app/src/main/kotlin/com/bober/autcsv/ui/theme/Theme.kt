package com.bober.autcsv.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CYellow,
    secondary = CYellowLight,
    tertiary = CYellowDark,
    background = DarkBackground,
    surface = CBlackLight,
    onPrimary = CBlack,
    onSecondary = CBlack,
    onTertiary = CBlack,
    onBackground = White,
    onSurface = White,
    error = ErrorLight,
    onError = White,
    surfaceVariant = CBlackLight,
    onSurfaceVariant = Gray,

    surfaceContainer = CGrayDarkTheme,
)

private val LightColorScheme = lightColorScheme(
    primary = CYellow,
    secondary = CYellowDark,
    tertiary = CYellowLight,
    background = LightBackground,
    surface = White,
    onPrimary = CBlack,
    onSecondary = CBlack,
    onTertiary = CBlack,
    onBackground = CBlack,
    onSurface = CBlack,
    error = ErrorLight,
    onError = White,
    surfaceVariant = Gray,
    onSurfaceVariant = CBlack,

    surfaceContainer = CGrayLightTheme,
)

@Composable
fun AutCSVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Динамические цвета доступны на Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}