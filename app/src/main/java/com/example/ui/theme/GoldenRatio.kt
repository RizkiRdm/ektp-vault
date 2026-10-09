package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Golden Ratio (φ ≈ 1.61803398875) Typography & Spatial System for LOKER.
 * Governs modular typographic scale, harmonic Fibonacci padding, and proportional layouts.
 */
object GoldenRatio {
    const val PHI = 1.61803398875f
    const val INVERSE_PHI = 0.61803398875f

    // Modular Golden Ratio Typographic Scale
    val TextMicro: TextUnit = 10.sp       // 16 / φ
    val TextSmall: TextUnit = 13.sp       // Harmonic sub-base
    val TextBase: TextUnit = 16.sp        // Base body
    val TextMedium: TextUnit = 21.sp      // 13 * φ
    val TextLarge: TextUnit = 26.sp       // 16 * φ
    val TextDisplay: TextUnit = 34.sp     // 21 * φ
    val TextHero: TextUnit = 42.sp        // 26 * φ

    // Spatial Fibonacci & Golden Ratio Grid (dp)
    val SpaceMicro: Dp = 5.dp             // 8 / φ
    val SpaceSmall: Dp = 8.dp             // Compact gap
    val SpaceBase: Dp = 13.dp             // Primary gutter (8 * φ)
    val SpaceLarge: Dp = 21.dp            // Section padding (13 * φ)
    val SpaceXLarge: Dp = 34.dp           // Hero breathing room (21 * φ)
    val SpaceMacro: Dp = 55.dp            // Screen partition (34 * φ)

    // Golden Ratio Aspect Ratios
    const val AspectRatio: Float = 1.618f
    const val InverseAspectRatio: Float = 0.618f
}
