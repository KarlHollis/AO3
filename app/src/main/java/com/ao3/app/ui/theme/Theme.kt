package com.ao3.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Ao3Red,
    onPrimary = White,
    primaryContainer = LightRedContainer,
    onPrimaryContainer = DarkRedText,
    secondary = WarmBrown,
    onSecondary = White,
    background = WarmBackground,
    onBackground = DarkText,
    surface = WarmBackground,
    onSurface = DarkText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = MediumText,
)

private val DarkColorScheme = darkColorScheme(
    primary = LightRed,
    onPrimary = DarkRedText,
    primaryContainer = DarkRedContainer,
    onPrimaryContainer = LightRedContainer,
    secondary = LightWarmBrown,
    onSecondary = DarkBrownText,
    background = DarkBackground,
    onBackground = LightText,
    surface = DarkBackground,
    onSurface = LightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightMediumText,
)

@Composable
fun Ao3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
