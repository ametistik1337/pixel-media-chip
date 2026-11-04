package com.pixel.mediachip.ui.viewmodel

import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.components.ChipPreset
import com.pixel.mediachip.ui.preview.PreviewChipSettings

/**
 * Immutable State representation of all 17 configuration keys in [ChipSettings].
 *
 * Provides bidirectional mapping to/from [ChipSettings], conversion to [PreviewChipSettings],
 * and preset application helpers.
 *
 * Zero hardcoded colors, zero emojis.
 */
data class ChipSettingsState(
    val layoutMode: Int = ChipSettings.MODE_TWO_LINES,
    val squishMode: Int = ChipSettings.SQUISH_TO_ICON,
    val showArtist: Boolean = true,
    val marqueeOneLine: Boolean = true,
    val monetTint: Boolean = true,
    val swipeGestures: Boolean = true,
    val hapticEnabled: Boolean = true,
    val eatCamera: Boolean = false,
    val visualizerCardEnabled: Boolean = true,
    val visualizerChipEnabled: Boolean = true,
    val visualizerSensitivity: Int = 100,
    val maxWidthDp: Int = 105,
    val maxTextWidthDp: Int = 105,
    val cameraEatDepthDp: Int = 16,
    val titleSizeSp: Float = 10.0f,
    val artistSizeSp: Float = 8.0f,
    val cornerRadiusDp: Int = 14
) {
    /**
     * Applies one of the 4 defined presets:
     * - [ChipPreset.PIXEL_DEFAULT]
     * - [ChipPreset.DYNAMIC_ISLAND]
     * - [ChipPreset.COMPACT]
     * - [ChipPreset.EXPANDED]
     */
    fun applyPreset(preset: ChipPreset): ChipSettingsState {
        return when (preset) {
            ChipPreset.PIXEL_DEFAULT -> resetToDefaults()
            ChipPreset.DYNAMIC_ISLAND -> copy(
                layoutMode = ChipSettings.MODE_ONE_LINE,
                eatCamera = true,
                maxTextWidthDp = 125,
                cameraEatDepthDp = 20,
                showArtist = true,
                marqueeOneLine = true,
                cornerRadiusDp = 16,
                titleSizeSp = 10.5f
            )
            ChipPreset.COMPACT -> copy(
                layoutMode = ChipSettings.MODE_ICON_ONLY,
                eatCamera = false,
                cornerRadiusDp = 14,
                squishMode = ChipSettings.SQUISH_DISABLED
            )
            ChipPreset.EXPANDED -> copy(
                layoutMode = ChipSettings.MODE_TWO_LINES,
                eatCamera = false,
                maxWidthDp = 180,
                showArtist = true,
                marqueeOneLine = true,
                cornerRadiusDp = 16,
                titleSizeSp = 11.0f,
                artistSizeSp = 8.5f
            )
        }
    }

    /**
     * Resets all parameters to official Pixel default settings.
     */
    fun resetToDefaults(): ChipSettingsState = ChipSettingsState()

    /**
     * Copies all 17 state values into the target mutable [ChipSettings] instance.
     */
    fun applyTo(settings: ChipSettings) {
        settings.layoutMode = layoutMode
        settings.squishMode = squishMode
        settings.showArtist = showArtist
        settings.marqueeOneLine = marqueeOneLine
        settings.monetTint = monetTint
        settings.swipeGestures = swipeGestures
        settings.hapticEnabled = hapticEnabled
        settings.eatCamera = eatCamera
        settings.visualizerCardEnabled = visualizerCardEnabled
        settings.visualizerChipEnabled = visualizerChipEnabled
        settings.visualizerSensitivity = visualizerSensitivity
        settings.maxWidthDp = maxWidthDp
        settings.maxTextWidthDp = maxTextWidthDp
        settings.cameraEatDepthDp = cameraEatDepthDp
        settings.titleSizeSp = titleSizeSp
        settings.artistSizeSp = artistSizeSp
        settings.cornerRadiusDp = cornerRadiusDp
    }

    /**
     * Converts to mutable [ChipSettings] instance.
     */
    fun toChipSettings(): ChipSettings {
        val settings = ChipSettings()
        applyTo(settings)
        return settings
    }

    /**
     * Maps to immutable [PreviewChipSettings] for rendering inside the Live Preview canvas.
     */
    fun toPreviewSettings(): PreviewChipSettings = PreviewChipSettings(
        layoutMode = layoutMode,
        squishMode = squishMode,
        showArtist = showArtist,
        marqueeOneLine = marqueeOneLine,
        monetTint = monetTint,
        swipeGestures = swipeGestures,
        hapticEnabled = hapticEnabled,
        eatCamera = eatCamera,
        visualizerCardEnabled = visualizerCardEnabled,
        visualizerChipEnabled = visualizerChipEnabled,
        visualizerSensitivity = visualizerSensitivity,
        maxWidthDp = maxWidthDp,
        maxTextWidthDp = maxTextWidthDp,
        cameraEatDepthDp = cameraEatDepthDp,
        titleSizeSp = titleSizeSp,
        artistSizeSp = artistSizeSp,
        cornerRadiusDp = cornerRadiusDp
    )
}

/**
 * Extension function to map mutable [ChipSettings] to immutable [ChipSettingsState].
 */
fun ChipSettings.toState(): ChipSettingsState = ChipSettingsState(
    layoutMode = layoutMode,
    squishMode = squishMode,
    showArtist = showArtist,
    marqueeOneLine = marqueeOneLine,
    monetTint = monetTint,
    swipeGestures = swipeGestures,
    hapticEnabled = hapticEnabled,
    eatCamera = eatCamera,
    visualizerCardEnabled = visualizerCardEnabled,
    visualizerChipEnabled = visualizerChipEnabled,
    visualizerSensitivity = visualizerSensitivity,
    maxWidthDp = maxWidthDp,
    maxTextWidthDp = maxTextWidthDp,
    cameraEatDepthDp = cameraEatDepthDp,
    titleSizeSp = titleSizeSp,
    artistSizeSp = artistSizeSp,
    cornerRadiusDp = cornerRadiusDp
)
