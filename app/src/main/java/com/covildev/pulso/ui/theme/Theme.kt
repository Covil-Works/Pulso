package com.covildev.pulso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = PureWhite,
    secondary = SecondaryBlue,
    onSecondary = PureWhite,
    tertiary = SecondaryBlue,
    onTertiary = PureWhite,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceContainerLow = DarkSurfaceContainerLow,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outlineVariant = DarkOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = PureWhite,
    secondary = SecondaryBlue,
    onSecondary = PureWhite,
    tertiary = SecondaryBlue,
    onTertiary = PureWhite,
    background = PureWhite,
    surface = PureWhite,
    surfaceVariant = LightSectionBackground,
    surfaceBright = PureWhite,
    surfaceDim = LightSectionBackground,
    surfaceContainer = LightSectionBackground,
    surfaceContainerHigh = LightSectionBackground,
    surfaceContainerHighest = LightSectionBackground,
    surfaceContainerLow = LightSectionBackground,
    surfaceContainerLowest = PureWhite,
    onBackground = SecondaryBlue,
    onSurface = SecondaryBlue,
)

@Composable
fun PulsoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Kept for API stability, but ignored to preserve brand palette in all modes.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
