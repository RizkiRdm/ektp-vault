package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrutalistEditorialLightColorScheme = lightColorScheme(
    primary = CarbonBlack,
    onPrimary = PaperWhite,
    primaryContainer = CarbonBlack,
    onPrimaryContainer = PaperWhite,
    secondary = MintChip,
    onSecondary = CarbonBlack,
    secondaryContainer = MistGray,
    onSecondaryContainer = CarbonBlack,
    tertiary = VoltageYellow,
    onTertiary = CarbonBlack,
    error = AlertCrimson,
    onError = PaperWhite,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = AlertCrimson,
    background = WarmCanvas,
    onBackground = CarbonBlack,
    surface = PaperWhite,
    onSurface = CarbonBlack,
    surfaceVariant = MistGray,
    onSurfaceVariant = Slate,
    outline = Ash,
    outlineVariant = Smoke
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BrutalistEditorialLightColorScheme,
        typography = Typography,
        content = content
    )
}
