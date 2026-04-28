package com.covildev.pulso.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
