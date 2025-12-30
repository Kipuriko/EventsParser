package ru.purebytestudio.eventparser.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletPrimaryDark,
    onPrimaryContainer = Color.White,

    secondary = ElectricCyan,
    onSecondary = Color.Black,
    secondaryContainer = ElectricCyan.copy(alpha = 0.2f),
    onSecondaryContainer = ElectricCyan,

    tertiary = ElectricPink,
    onTertiary = Color.White,
    tertiaryContainer = ElectricPink.copy(alpha = 0.2f),
    onTertiaryContainer = ElectricPink,

    background = DarkBackground,
    onBackground = TextPrimaryDark,

    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,

    error = ErrorColor,
    onError = Color.White,
    errorContainer = ErrorColor.copy(alpha = 0.2f),
    onErrorContainer = ErrorColor,

    outline = TextTertiaryDark,
    outlineVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletPrimaryLight.copy(alpha = 0.2f),
    onPrimaryContainer = VioletPrimaryDark,

    secondary = ElectricCyan,
    onSecondary = Color.White,
    secondaryContainer = ElectricCyan.copy(alpha = 0.1f),
    onSecondaryContainer = ElectricCyan,

    tertiary = ElectricPink,
    onTertiary = Color.White,
    tertiaryContainer = ElectricPink.copy(alpha = 0.1f),
    onTertiaryContainer = ElectricPink,

    background = LightBackground,
    onBackground = TextPrimaryLight,

    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,

    error = ErrorColor,
    onError = Color.White,
    errorContainer = ErrorColor.copy(alpha = 0.1f),
    onErrorContainer = ErrorColor,

    outline = TextTertiaryLight,
    outlineVariant = LightSurfaceVariant
)

@Suppress("DEPRECATION")
@Composable
fun EventParserTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = darkTheme ?: systemInDarkTheme
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(
                window,
                view
            ).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}