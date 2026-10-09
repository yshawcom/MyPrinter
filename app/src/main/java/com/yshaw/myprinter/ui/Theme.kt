package com.yshaw.myprinter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palette shared by the Compose screens. It mirrors res/values(-night)/colors.xml and follows the
 * system night mode, so both the screens and the Material components switch together.
 */
data class MyPrinterColors(
    val isDark: Boolean,
    val primary: Color,
    val primaryDark: Color,
    val pageBackground: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val fieldStroke: Color,
    val hint: Color,
    val previewBackground: Color,
    val warningBackground: Color,
    val warningText: Color,
    val secondaryAction: Color,
    val onAccent: Color,
)

private val LightColors = MyPrinterColors(
    isDark = false,
    primary = Color(0xFF155EEF),
    primaryDark = Color(0xFF0B4AC4),
    pageBackground = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF162033),
    textSecondary = Color(0xFF667085),
    divider = Color(0xFFE7ECF4),
    fieldStroke = Color(0xFFD0D5DD),
    hint = Color(0xFF98A2B3),
    previewBackground = Color(0xFFF8FAFC),
    warningBackground = Color(0xFFFFF4E5),
    warningText = Color(0xFF9A5B00),
    secondaryAction = Color(0xFF3B82F6),
    onAccent = Color(0xFFFFFFFF),
)

private val DarkColors = MyPrinterColors(
    isDark = true,
    primary = Color(0xFF8AB4FF),
    primaryDark = Color(0xFF4C7DFF),
    pageBackground = Color(0xFF101319),
    surface = Color(0xFF1A1E26),
    textPrimary = Color(0xFFE8EAF0),
    textSecondary = Color(0xFF9BA4B4),
    divider = Color(0xFF2A2F3A),
    fieldStroke = Color(0xFF3B4250),
    hint = Color(0xFF7B8496),
    previewBackground = Color(0xFF141821),
    warningBackground = Color(0xFF3A2E17),
    warningText = Color(0xFFF0B45C),
    secondaryAction = Color(0xFF8AB4FF),
    onAccent = Color(0xFF0B1220),
)

private val LocalMyPrinterColors = staticCompositionLocalOf { LightColors }

private val LightScheme = lightColorScheme(
    primary = LightColors.primary,
    onPrimary = LightColors.onAccent,
    secondary = LightColors.secondaryAction,
    onSecondary = LightColors.onAccent,
    background = LightColors.pageBackground,
    onBackground = LightColors.textPrimary,
    surface = LightColors.surface,
    onSurface = LightColors.textPrimary,
    surfaceVariant = LightColors.surface,
    onSurfaceVariant = LightColors.textSecondary,
    outline = LightColors.divider,
    error = Color(0xFFD92D20),
)

private val DarkScheme = darkColorScheme(
    primary = DarkColors.primary,
    onPrimary = DarkColors.onAccent,
    secondary = DarkColors.secondaryAction,
    onSecondary = DarkColors.onAccent,
    background = DarkColors.pageBackground,
    onBackground = DarkColors.textPrimary,
    surface = DarkColors.surface,
    onSurface = DarkColors.textPrimary,
    surfaceVariant = DarkColors.surface,
    onSurfaceVariant = DarkColors.textSecondary,
    outline = DarkColors.divider,
    error = Color(0xFFF97066),
)

/** The current palette; use the short accessors below inside composables. */
val myPrinterColors: MyPrinterColors
    @Composable get() = LocalMyPrinterColors.current

val Primary: Color @Composable get() = myPrinterColors.primary
val PrimaryDark: Color @Composable get() = myPrinterColors.primaryDark
val PageBackground: Color @Composable get() = myPrinterColors.pageBackground
val SurfaceColor: Color @Composable get() = myPrinterColors.surface
val TextPrimary: Color @Composable get() = myPrinterColors.textPrimary
val TextSecondary: Color @Composable get() = myPrinterColors.textSecondary
val DividerColor: Color @Composable get() = myPrinterColors.divider
val FieldStroke: Color @Composable get() = myPrinterColors.fieldStroke
val HintColor: Color @Composable get() = myPrinterColors.hint
val PreviewBackground: Color @Composable get() = myPrinterColors.previewBackground
val WarningBackground: Color @Composable get() = myPrinterColors.warningBackground
val WarningText: Color @Composable get() = myPrinterColors.warningText
val SecondaryAction: Color @Composable get() = myPrinterColors.secondaryAction
val OnAccent: Color @Composable get() = myPrinterColors.onAccent

@Composable
fun MyPrinterTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkColors else LightColors
    CompositionLocalProvider(LocalMyPrinterColors provides colors) {
        MaterialTheme(
            colorScheme = if (isDark) DarkScheme else LightScheme,
            content = content,
        )
    }
}
