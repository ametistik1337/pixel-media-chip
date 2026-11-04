package com.pixel.mediachip.ui

import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.components.ChipPreset
import com.pixel.mediachip.ui.viewmodel.ChipSettingsState
import com.pixel.mediachip.ui.viewmodel.SystemDiagnostics
import com.pixel.mediachip.ui.viewmodel.toState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive verification suite for [ChipSettingsState], preset applications,
 * bidirectional mapping with [ChipSettings], and round-trip consistency.
 */
class SettingsStateTest {

    private lateinit var chipSettings: ChipSettings

    @Before
    fun setUp() {
        chipSettings = ChipSettings.get()
        chipSettings.resetToDefaults()
    }

    @Test
    fun testDefaultValuesMatchChipSettings() {
        val state = ChipSettingsState()

        assertEquals(chipSettings.layoutMode, state.layoutMode)
        assertEquals(chipSettings.squishMode, state.squishMode)
        assertEquals(chipSettings.showArtist, state.showArtist)
        assertEquals(chipSettings.marqueeOneLine, state.marqueeOneLine)
        assertEquals(chipSettings.monetTint, state.monetTint)
        assertEquals(chipSettings.swipeGestures, state.swipeGestures)
        assertEquals(chipSettings.hapticEnabled, state.hapticEnabled)
        assertEquals(chipSettings.eatCamera, state.eatCamera)
        assertEquals(chipSettings.visualizerCardEnabled, state.visualizerCardEnabled)
        assertEquals(chipSettings.visualizerChipEnabled, state.visualizerChipEnabled)
        assertEquals(chipSettings.visualizerSensitivity, state.visualizerSensitivity)
        assertEquals(chipSettings.maxWidthDp, state.maxWidthDp)
        assertEquals(chipSettings.maxTextWidthDp, state.maxTextWidthDp)
        assertEquals(chipSettings.cameraEatDepthDp, state.cameraEatDepthDp)
        assertEquals(chipSettings.titleSizeSp, state.titleSizeSp, 0.001f)
        assertEquals(chipSettings.artistSizeSp, state.artistSizeSp, 0.001f)
        assertEquals(chipSettings.cornerRadiusDp, state.cornerRadiusDp)
    }

    @Test
    fun testBidirectionalMappingRoundTrip() {
        val customState = ChipSettingsState(
            layoutMode = ChipSettings.MODE_ONE_LINE,
            squishMode = ChipSettings.SQUISH_HIDE,
            showArtist = false,
            marqueeOneLine = false,
            monetTint = false,
            swipeGestures = false,
            hapticEnabled = false,
            eatCamera = true,
            visualizerCardEnabled = false,
            visualizerChipEnabled = false,
            visualizerSensitivity = 165,
            maxWidthDp = 190,
            maxTextWidthDp = 140,
            cameraEatDepthDp = 24,
            titleSizeSp = 12.5f,
            artistSizeSp = 9.5f,
            cornerRadiusDp = 20
        )

        val target = ChipSettings()
        customState.applyTo(target)

        assertEquals(customState.layoutMode, target.layoutMode)
        assertEquals(customState.squishMode, target.squishMode)
        assertEquals(customState.showArtist, target.showArtist)
        assertEquals(customState.marqueeOneLine, target.marqueeOneLine)
        assertEquals(customState.monetTint, target.monetTint)
        assertEquals(customState.swipeGestures, target.swipeGestures)
        assertEquals(customState.hapticEnabled, target.hapticEnabled)
        assertEquals(customState.eatCamera, target.eatCamera)
        assertEquals(customState.visualizerCardEnabled, target.visualizerCardEnabled)
        assertEquals(customState.visualizerChipEnabled, target.visualizerChipEnabled)
        assertEquals(customState.visualizerSensitivity, target.visualizerSensitivity)
        assertEquals(customState.maxWidthDp, target.maxWidthDp)
        assertEquals(customState.maxTextWidthDp, target.maxTextWidthDp)
        assertEquals(customState.cameraEatDepthDp, target.cameraEatDepthDp)
        assertEquals(customState.titleSizeSp, target.titleSizeSp, 0.001f)
        assertEquals(customState.artistSizeSp, target.artistSizeSp, 0.001f)
        assertEquals(customState.cornerRadiusDp, target.cornerRadiusDp)

        // Reverse back to state
        val roundTripped = target.toState()
        assertEquals(customState, roundTripped)
    }

    @Test
    fun testToChipSettingsHelper() {
        val original = ChipSettingsState(
            layoutMode = ChipSettings.MODE_ICON_ONLY,
            maxWidthDp = 80,
            titleSizeSp = 11.5f,
            hapticEnabled = false
        )

        val converted = original.toChipSettings()
        assertEquals(ChipSettings.MODE_ICON_ONLY, converted.layoutMode)
        assertEquals(80, converted.maxWidthDp)
        assertEquals(11.5f, converted.titleSizeSp, 0.001f)
        assertFalse(converted.hapticEnabled)
    }

    @Test
    fun testPresetPixelDefault() {
        val base = ChipSettingsState(
            layoutMode = ChipSettings.MODE_ICON_ONLY,
            maxWidthDp = 200,
            eatCamera = true
        )

        val result = base.applyPreset(ChipPreset.PIXEL_DEFAULT)
        val expected = ChipSettingsState()

        assertEquals(expected, result)
    }

    @Test
    fun testPresetDynamicIslandMatchesJavaImplementation() {
        val javaSettings = ChipSettings()
        javaSettings.applyPresetDynamicIsland()

        val kotlinState = ChipSettingsState().applyPreset(ChipPreset.DYNAMIC_ISLAND)

        assertEquals(javaSettings.layoutMode, kotlinState.layoutMode)
        assertEquals(javaSettings.eatCamera, kotlinState.eatCamera)
        assertEquals(javaSettings.maxTextWidthDp, kotlinState.maxTextWidthDp)
        assertEquals(javaSettings.cameraEatDepthDp, kotlinState.cameraEatDepthDp)
        assertEquals(javaSettings.showArtist, kotlinState.showArtist)
        assertEquals(javaSettings.marqueeOneLine, kotlinState.marqueeOneLine)
        assertEquals(javaSettings.cornerRadiusDp, kotlinState.cornerRadiusDp)
        assertEquals(javaSettings.titleSizeSp, kotlinState.titleSizeSp, 0.001f)
    }

    @Test
    fun testPresetCompactMatchesJavaImplementation() {
        val javaSettings = ChipSettings()
        javaSettings.applyPresetCompact()

        val kotlinState = ChipSettingsState().applyPreset(ChipPreset.COMPACT)

        assertEquals(javaSettings.layoutMode, kotlinState.layoutMode)
        assertEquals(javaSettings.eatCamera, kotlinState.eatCamera)
        assertEquals(javaSettings.cornerRadiusDp, kotlinState.cornerRadiusDp)
        assertEquals(javaSettings.squishMode, kotlinState.squishMode)
    }

    @Test
    fun testPresetExpandedMatchesJavaImplementation() {
        val javaSettings = ChipSettings()
        javaSettings.applyPresetExpanded()

        val kotlinState = ChipSettingsState().applyPreset(ChipPreset.EXPANDED)

        assertEquals(javaSettings.layoutMode, kotlinState.layoutMode)
        assertEquals(javaSettings.eatCamera, kotlinState.eatCamera)
        assertEquals(javaSettings.maxWidthDp, kotlinState.maxWidthDp)
        assertEquals(javaSettings.showArtist, kotlinState.showArtist)
        assertEquals(javaSettings.marqueeOneLine, kotlinState.marqueeOneLine)
        assertEquals(javaSettings.cornerRadiusDp, kotlinState.cornerRadiusDp)
        assertEquals(javaSettings.titleSizeSp, kotlinState.titleSizeSp, 0.001f)
        assertEquals(javaSettings.artistSizeSp, kotlinState.artistSizeSp, 0.001f)
    }

    @Test
    fun testResetToDefaultsRestoresAll17Keys() {
        val dirty = ChipSettingsState(
            layoutMode = ChipSettings.MODE_ICON_ONLY,
            squishMode = ChipSettings.SQUISH_DISABLED,
            showArtist = false,
            marqueeOneLine = false,
            monetTint = false,
            swipeGestures = false,
            hapticEnabled = false,
            eatCamera = true,
            visualizerCardEnabled = false,
            visualizerChipEnabled = false,
            visualizerSensitivity = 180,
            maxWidthDp = 220,
            maxTextWidthDp = 180,
            cameraEatDepthDp = 30,
            titleSizeSp = 14.0f,
            artistSizeSp = 12.0f,
            cornerRadiusDp = 24
        )

        val clean = dirty.resetToDefaults()
        val defaultState = ChipSettingsState()

        assertEquals(defaultState, clean)
        assertEquals(ChipSettings.MODE_TWO_LINES, clean.layoutMode)
        assertEquals(ChipSettings.SQUISH_TO_ICON, clean.squishMode)
        assertTrue(clean.showArtist)
        assertTrue(clean.marqueeOneLine)
        assertTrue(clean.monetTint)
        assertTrue(clean.swipeGestures)
        assertTrue(clean.hapticEnabled)
        assertFalse(clean.eatCamera)
        assertTrue(clean.visualizerCardEnabled)
        assertTrue(clean.visualizerChipEnabled)
        assertEquals(100, clean.visualizerSensitivity)
        assertEquals(105, clean.maxWidthDp)
        assertEquals(105, clean.maxTextWidthDp)
        assertEquals(16, clean.cameraEatDepthDp)
        assertEquals(10.0f, clean.titleSizeSp, 0.001f)
        assertEquals(8.0f, clean.artistSizeSp, 0.001f)
        assertEquals(14, clean.cornerRadiusDp)
    }

    @Test
    fun testToPreviewSettingsPreservesAllFields() {
        val state = ChipSettingsState(
            layoutMode = ChipSettings.MODE_ONE_LINE,
            squishMode = ChipSettings.SQUISH_HIDE,
            showArtist = false,
            marqueeOneLine = true,
            monetTint = true,
            swipeGestures = false,
            hapticEnabled = true,
            eatCamera = true,
            visualizerCardEnabled = true,
            visualizerChipEnabled = false,
            visualizerSensitivity = 150,
            maxWidthDp = 160,
            maxTextWidthDp = 130,
            cameraEatDepthDp = 22,
            titleSizeSp = 11.0f,
            artistSizeSp = 7.5f,
            cornerRadiusDp = 18
        )

        val preview = state.toPreviewSettings()

        assertEquals(state.layoutMode, preview.layoutMode)
        assertEquals(state.squishMode, preview.squishMode)
        assertEquals(state.showArtist, preview.showArtist)
        assertEquals(state.marqueeOneLine, preview.marqueeOneLine)
        assertEquals(state.monetTint, preview.monetTint)
        assertEquals(state.swipeGestures, preview.swipeGestures)
        assertEquals(state.hapticEnabled, preview.hapticEnabled)
        assertEquals(state.eatCamera, preview.eatCamera)
        assertEquals(state.visualizerCardEnabled, preview.visualizerCardEnabled)
        assertEquals(state.visualizerChipEnabled, preview.visualizerChipEnabled)
        assertEquals(state.visualizerSensitivity, preview.visualizerSensitivity)
        assertEquals(state.maxWidthDp, preview.maxWidthDp)
        assertEquals(state.maxTextWidthDp, preview.maxTextWidthDp)
        assertEquals(state.cameraEatDepthDp, preview.cameraEatDepthDp)
        assertEquals(state.titleSizeSp, preview.titleSizeSp, 0.001f)
        assertEquals(state.artistSizeSp, preview.artistSizeSp, 0.001f)
        assertEquals(state.cornerRadiusDp, preview.cornerRadiusDp)
    }

    @Test
    fun testDiagnosticsDataClassIntegrity() {
        val diagnostics = SystemDiagnostics(
            isRootAvailable = true,
            isRecordAudioGranted = true,
            isOverlayGranted = false
        )

        assertTrue(diagnostics.isRootAvailable)
        assertTrue(diagnostics.isRecordAudioGranted)
        assertFalse(diagnostics.isOverlayGranted)

        val copied = diagnostics.copy(isOverlayGranted = true)
        assertTrue(copied.isOverlayGranted)
        assertEquals(
            SystemDiagnostics(
                isRootAvailable = true,
                isRecordAudioGranted = true,
                isOverlayGranted = true
            ),
            copied
        )
    }

    @Test
    fun testDefaultConstantsMatchInstanceDefaults() {
        assertEquals(ChipSettings.DEFAULT_LAYOUT_MODE, chipSettings.layoutMode)
        assertEquals(ChipSettings.DEFAULT_SQUISH_MODE, chipSettings.squishMode)
        assertEquals(ChipSettings.DEFAULT_SHOW_ARTIST, chipSettings.showArtist)
        assertEquals(ChipSettings.DEFAULT_MARQUEE_ONE_LINE, chipSettings.marqueeOneLine)
        assertEquals(ChipSettings.DEFAULT_MONET_TINT, chipSettings.monetTint)
        assertEquals(ChipSettings.DEFAULT_SWIPE_GESTURES, chipSettings.swipeGestures)
        assertEquals(ChipSettings.DEFAULT_HAPTIC_ENABLED, chipSettings.hapticEnabled)
        assertEquals(ChipSettings.DEFAULT_EAT_CAMERA, chipSettings.eatCamera)
        assertEquals(ChipSettings.DEFAULT_VISUALIZER_CARD_ENABLED, chipSettings.visualizerCardEnabled)
        assertEquals(ChipSettings.DEFAULT_VISUALIZER_CHIP_ENABLED, chipSettings.visualizerChipEnabled)
        assertEquals(ChipSettings.DEFAULT_VISUALIZER_SENSITIVITY, chipSettings.visualizerSensitivity)
        assertEquals(ChipSettings.DEFAULT_MAX_WIDTH_DP, chipSettings.maxWidthDp)
        assertEquals(ChipSettings.DEFAULT_MAX_TEXT_WIDTH_DP, chipSettings.maxTextWidthDp)
        assertEquals(ChipSettings.DEFAULT_CAMERA_EAT_DEPTH_DP, chipSettings.cameraEatDepthDp)
        assertEquals(ChipSettings.DEFAULT_TITLE_SIZE_SP, chipSettings.titleSizeSp, 0.001f)
        assertEquals(ChipSettings.DEFAULT_ARTIST_SIZE_SP, chipSettings.artistSizeSp, 0.001f)
        assertEquals(ChipSettings.DEFAULT_CORNER_RADIUS_DP, chipSettings.cornerRadiusDp)
    }

    @Test
    fun testFromMapSafeWithBoxedNumbersAndBooleans() {
        val map = HashMap<String, Any>()
        map[ChipSettings.KEY_LAYOUT_MODE] = java.lang.Long.valueOf(1L)
        map[ChipSettings.KEY_SQUISH_MODE] = java.lang.Integer.valueOf(2)
        map[ChipSettings.KEY_SHOW_ARTIST] = java.lang.Boolean.FALSE
        map[ChipSettings.KEY_MARQUEE_ONE_LINE] = java.lang.Boolean.FALSE
        map[ChipSettings.KEY_MONET_TINT] = java.lang.Boolean.FALSE
        map[ChipSettings.KEY_SWIPE_GESTURES] = java.lang.Boolean.FALSE
        map[ChipSettings.KEY_HAPTIC_ENABLED] = java.lang.Boolean.FALSE
        map[ChipSettings.KEY_EAT_CAMERA] = java.lang.Boolean.TRUE
        map[ChipSettings.KEY_VISUALIZER_CARD_ENABLED] = java.lang.Boolean.FALSE
        map[ChipSettings.KEY_VISUALIZER_CHIP_ENABLED] = java.lang.Boolean.FALSE
        map[ChipSettings.KEY_VISUALIZER_SENSITIVITY] = java.lang.Double.valueOf(140.0)
        map[ChipSettings.KEY_MAX_WIDTH_DP] = java.lang.Long.valueOf(175L)
        map[ChipSettings.KEY_MAX_TEXT_WIDTH_DP] = java.lang.Integer.valueOf(135)
        map[ChipSettings.KEY_CAMERA_EAT_DEPTH_DP] = java.lang.Long.valueOf(22L)
        map[ChipSettings.KEY_TITLE_SIZE_SP] = java.lang.Double.valueOf(11.5)
        map[ChipSettings.KEY_ARTIST_SIZE_SP] = java.lang.Double.valueOf(9.0)
        map[ChipSettings.KEY_CORNER_RADIUS_DP] = java.lang.Long.valueOf(18L)

        val target = ChipSettings()
        target.fromMap(map)

        assertEquals(1, target.layoutMode)
        assertEquals(2, target.squishMode)
        assertFalse(target.showArtist)
        assertFalse(target.marqueeOneLine)
        assertFalse(target.monetTint)
        assertFalse(target.swipeGestures)
        assertFalse(target.hapticEnabled)
        assertTrue(target.eatCamera)
        assertFalse(target.visualizerCardEnabled)
        assertFalse(target.visualizerChipEnabled)
        assertEquals(140, target.visualizerSensitivity)
        assertEquals(175, target.maxWidthDp)
        assertEquals(135, target.maxTextWidthDp)
        assertEquals(22, target.cameraEatDepthDp)
        assertEquals(11.5f, target.titleSizeSp, 0.001f)
        assertEquals(9.0f, target.artistSizeSp, 0.001f)
        assertEquals(18, target.cornerRadiusDp)
    }
}
