package com.bober.autcsv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Набор кастомных цветовых токенов приложения. Экраны обращаются к ним
 * через [AppColors] (например `AppColors.Accent`) — значение всегда
 * соответствует активной теме, светлой или тёмной.
 */
class AppPalette(
    val Background: Color,
    val BackgroundSoft: Color,
    val Surface: Color,
    val Surface2: Color,
    val Surface3: Color,
    val Border: Color,
    val BorderSoft: Color,
    val Accent: Color,
    val AccentDim: Color,
    val AccentInk: Color,
    val Mint: Color,
    val MintSoft: Color,
    val TextPrimary: Color,
    val TextDim: Color,
    val TextFaint: Color,
    val Success: Color,
    val SuccessSoft: Color,
    val AmberSoft: Color,
    val Danger: Color,
    val DangerSoft: Color,
)

/** Тёмная палитра «эспрессо + золото» — базовые значения из [Color.kt]. */
val DarkAppPalette = AppPalette(
    Background = Background,
    BackgroundSoft = BackgroundSoft,
    Surface = Surface,
    Surface2 = Surface2,
    Surface3 = Surface3,
    Border = Border,
    BorderSoft = BorderSoft,
    Accent = Accent,
    AccentDim = AccentDim,
    AccentInk = AccentInk,
    Mint = Mint,
    MintSoft = MintSoft,
    TextPrimary = TextPrimary,
    TextDim = TextDim,
    TextFaint = TextFaint,
    Success = Success,
    SuccessSoft = SuccessSoft,
    AmberSoft = AmberSoft,
    Danger = Danger,
    DangerSoft = DangerSoft,
)

/** Светлая палитра: кремовые поверхности, монохромный акцент (чернильный). */
val LightAppPalette = AppPalette(
    Background = LightBackground,
    BackgroundSoft = LightBackgroundSoft,
    Surface = LightSurface,
    Surface2 = LightSurface2,
    Surface3 = LightSurface3,
    Border = LightBorder,
    BorderSoft = LightBorderSoft,
    Accent = LightAccent,
    AccentDim = LightAccentDim,
    AccentInk = LightAccentInk,
    Mint = LightMint,
    MintSoft = LightMintSoft,
    TextPrimary = LightTextPrimary,
    TextDim = LightTextDim,
    TextFaint = LightTextFaint,
    Success = LightSuccess,
    SuccessSoft = LightSuccessSoft,
    AmberSoft = LightAmberSoft,
    Danger = LightDanger,
    DangerSoft = LightDangerSoft,
)

val LocalAppPalette = staticCompositionLocalOf { DarkAppPalette }

/**
 * Компоузебл-доступ к токенам палитры из любой точки композиции.
 * Аналог MaterialTheme.colorScheme для кастомных цветов приложения.
 */
object AppColors {
    val Background: Color @Composable get() = LocalAppPalette.current.Background
    val BackgroundSoft: Color @Composable get() = LocalAppPalette.current.BackgroundSoft
    val Surface: Color @Composable get() = LocalAppPalette.current.Surface
    val Surface2: Color @Composable get() = LocalAppPalette.current.Surface2
    val Surface3: Color @Composable get() = LocalAppPalette.current.Surface3
    val Border: Color @Composable get() = LocalAppPalette.current.Border
    val BorderSoft: Color @Composable get() = LocalAppPalette.current.BorderSoft
    val Accent: Color @Composable get() = LocalAppPalette.current.Accent
    val AccentDim: Color @Composable get() = LocalAppPalette.current.AccentDim
    val AccentInk: Color @Composable get() = LocalAppPalette.current.AccentInk
    val Mint: Color @Composable get() = LocalAppPalette.current.Mint
    val MintSoft: Color @Composable get() = LocalAppPalette.current.MintSoft
    val TextPrimary: Color @Composable get() = LocalAppPalette.current.TextPrimary
    val TextDim: Color @Composable get() = LocalAppPalette.current.TextDim
    val TextFaint: Color @Composable get() = LocalAppPalette.current.TextFaint
    val Success: Color @Composable get() = LocalAppPalette.current.Success
    val SuccessSoft: Color @Composable get() = LocalAppPalette.current.SuccessSoft
    val AmberSoft: Color @Composable get() = LocalAppPalette.current.AmberSoft
    val Danger: Color @Composable get() = LocalAppPalette.current.Danger
    val DangerSoft: Color @Composable get() = LocalAppPalette.current.DangerSoft
}