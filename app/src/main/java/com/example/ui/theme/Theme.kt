package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeverhackLightColorScheme = lightColorScheme(
    primary = SovereignInk,
    onPrimary = SignalWhite,
    primaryContainer = SovereignInk,
    onPrimaryContainer = SignalWhite,
    secondary = SovereignViolet,
    onSecondary = SignalWhite,
    secondaryContainer = Color(0xFFF0EBFD),
    onSecondaryContainer = SovereignViolet,
    tertiary = CyberCyan,
    onTertiary = SovereignInk,
    error = AlertCrimson,
    onError = SignalWhite,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = AlertCrimson,
    background = MistSurface,
    onBackground = SovereignInk,
    surface = SignalWhite,
    onSurface = SovereignInk,
    surfaceVariant = MistSurface,
    onSurfaceVariant = CarbonGray,
    outline = CoolHairline,
    outlineVariant = ShadowLichen
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeverhackLightColorScheme,
        typography = Typography,
        content = content
    )
}
