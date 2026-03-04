package com.example.nsddemo.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.nsddemo.presentation.screen.settings.GameLocales

private val LightColors = lightColorScheme(
    primary = ImpostlePrimary,
    onPrimary = ImpostleTextLight,
    secondary = ImpostleSecondary,
    onSecondary = ImpostleTextLight,
    error = ImpostleDanger,
    background = ImpostleBgLight,
    onBackground = ImpostleTextLight,
    surface = ImpostleCardLight,
    onSurface = ImpostleTextLight,
    surfaceVariant = ImpostleGridLight, // <-- We store the Grid color here
    outline = Color.Black // <-- Brutalist borders/shadows stay black in light mode
)

private val DarkColors = darkColorScheme(
    primary = ImpostlePrimary,
    onPrimary = ImpostleTextDark,
    secondary = ImpostleSecondary,
    onSecondary = ImpostleTextDark,
    error = ImpostleDanger,
    background = ImpostleBgDark,
    onBackground = ImpostleTextDark,
    surface = ImpostleCardDark,
    onSurface = ImpostleTextDark,
    surfaceVariant = ImpostleGridDark, // <-- We store the Grid color here
    outline = Color.Black // <-- Brutalist borders/shadows stay black in dark mode too
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    locale: GameLocales = GameLocales.English,
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    val appTypography = when (locale) {
        GameLocales.English -> englishTypography
        GameLocales.Arabic -> arabicTypography
    }

    MaterialTheme(
        colorScheme = colors,
        typography = appTypography,
        content = content
    )
}