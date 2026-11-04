package com.pixel.mediachip.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Standard M3 default color schemes without hardcoded colors.
 * Used as fallback on Android < 12 (SDK < 31) where dynamic Monet is not available.
 */
val DefaultDarkColorScheme: ColorScheme = darkColorScheme()
val DefaultLightColorScheme: ColorScheme = lightColorScheme()

/**
 * Semantic container color tokens adhering strictly to Material 3 Expressive.
 * Zero hardcoded hex colors - all colors resolve dynamically from the active ColorScheme.
 */
val ColorScheme.settingsCardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = surfaceContainerLow

val ColorScheme.heroCardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = surfaceContainer

val ColorScheme.previewStatusBarBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = surfaceContainerLowest

val ColorScheme.sliderActiveTrack: Color
    @Composable
    @ReadOnlyComposable
    get() = primary

val ColorScheme.sliderInactiveTrack: Color
    @Composable
    @ReadOnlyComposable
    get() = surfaceContainerHighest

val ColorScheme.chipMonetBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = primaryContainer

val ColorScheme.chipMonetContent: Color
    @Composable
    @ReadOnlyComposable
    get() = onPrimaryContainer
