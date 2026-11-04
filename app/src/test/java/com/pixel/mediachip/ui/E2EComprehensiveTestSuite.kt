package com.pixel.mediachip.ui

import android.os.Bundle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.M3Motion
import com.pixel.mediachip.SettingsProvider
import com.pixel.mediachip.ui.components.ChipPreset
import com.pixel.mediachip.ui.preview.LivePreviewMath
import com.pixel.mediachip.ui.preview.PreviewChipSettings
import com.pixel.mediachip.ui.preview.toPreviewSettings
import com.pixel.mediachip.ui.theme.ExpressiveCornerExtraLarge
import com.pixel.mediachip.ui.theme.ExpressiveCornerExtraLargeIncreased
import com.pixel.mediachip.ui.theme.ExpressiveCornerExtraSmall
import com.pixel.mediachip.ui.theme.ExpressiveCornerFull
import com.pixel.mediachip.ui.theme.ExpressiveCornerLarge
import com.pixel.mediachip.ui.theme.ExpressiveCornerLargeIncreased
import com.pixel.mediachip.ui.theme.ExpressiveCornerMedium
import com.pixel.mediachip.ui.theme.ExpressiveCornerNone
import com.pixel.mediachip.ui.theme.ExpressiveCornerSmall
import com.pixel.mediachip.ui.theme.ExpressiveEffectsSpringDamping
import com.pixel.mediachip.ui.theme.ExpressiveShapes
import com.pixel.mediachip.ui.theme.ExpressiveSpatialSpringDamping
import com.pixel.mediachip.ui.theme.ExpressiveTypography
import com.pixel.mediachip.ui.theme.HeroCardShape
import com.pixel.mediachip.ui.theme.PillShape
import com.pixel.mediachip.ui.theme.SettingsCardShape
import com.pixel.mediachip.ui.theme.calculateConcentricRadius
import com.pixel.mediachip.ui.theme.concentricShape
import com.pixel.mediachip.ui.viewmodel.ChipSettingsState
import com.pixel.mediachip.ui.viewmodel.SystemDiagnostics
import com.pixel.mediachip.ui.viewmodel.toState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Exhaustive End-to-End (E2E) Verification Test Suite for PixelMediaChip Settings UI.
 *
 * Covers Tiers 1 through 4 across all 17 configuration keys, Material 3 Expressive geometry,
 * anti-bounce discrete slider mathematics, pairwise cross-feature combinations, and
 * full real-world multi-step user workflows.
 */
class E2EComprehensiveTestSuite {

    private lateinit var chipSettings: ChipSettings

    @Before
    fun setUp() {
        chipSettings = ChipSettings.get()
        chipSettings.resetToDefaults()
    }

    private fun snapAndClamp(rawVal: Float, min: Float, max: Float, step: Float): Float {
        val snapped = ((rawVal - min) / step).roundToInt() * step + min
        return snapped.coerceIn(min, max)
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (HAPPY PATH)
    // =========================================================================

    @Test
    fun tier1_f3_concentricGeometry_standardHierarchyCalculations() {
        // R_inner = max(0, R_outer - padding)
        assertEquals(12.dp, calculateConcentricRadius(28.dp, 16.dp))
        assertEquals(4.dp, calculateConcentricRadius(16.dp, 12.dp))
        assertEquals(8.dp, calculateConcentricRadius(24.dp, 16.dp))
        assertEquals(12.dp, calculateConcentricRadius(20.dp, 8.dp))
        assertEquals(28.dp, calculateConcentricRadius(28.dp, 0.dp))
        assertEquals(16.dp, calculateConcentricRadius(16.dp, 0.dp))

        // Parity with M3Motion Java implementation
        assertEquals(12, M3Motion.calculateConcentricRadius(28, 16))
        assertEquals(4, M3Motion.calculateConcentricRadius(16, 12))
        assertEquals(8, M3Motion.calculateConcentricRadius(24, 16))
        assertEquals(28, M3Motion.calculateConcentricRadius(28, 0))
    }

    @Test
    fun tier1_f3_concentricShape_producesAccurateRoundedCornerShape() {
        val outer = 28.dp
        val padding = 16.dp
        val shape = concentricShape(outer, padding)
        val expected = RoundedCornerShape(12.dp)
        assertEquals(expected, shape)

        val cardShape = concentricShape(16.dp, 12.dp)
        assertEquals(RoundedCornerShape(4.dp), cardShape)
    }

    @Test
    fun tier1_f3_shapeHierarchyTokens_matchM3ExpressiveSpecifications() {
        assertEquals(0.dp, ExpressiveCornerNone)
        assertEquals(4.dp, ExpressiveCornerExtraSmall)
        assertEquals(8.dp, ExpressiveCornerSmall)
        assertEquals(12.dp, ExpressiveCornerMedium)
        assertEquals(16.dp, ExpressiveCornerLarge)
        assertEquals(20.dp, ExpressiveCornerLargeIncreased)
        assertEquals(28.dp, ExpressiveCornerExtraLarge)
        assertEquals(32.dp, ExpressiveCornerExtraLargeIncreased)
        assertEquals(9999.dp, ExpressiveCornerFull)

        assertEquals(RoundedCornerShape(ExpressiveCornerExtraLarge), HeroCardShape)
        assertEquals(RoundedCornerShape(ExpressiveCornerLarge), SettingsCardShape)
        assertEquals(CircleShape, PillShape)

        assertEquals(RoundedCornerShape(4.dp), ExpressiveShapes.extraSmall)
        assertEquals(RoundedCornerShape(8.dp), ExpressiveShapes.small)
        assertEquals(RoundedCornerShape(12.dp), ExpressiveShapes.medium)
        assertEquals(RoundedCornerShape(16.dp), ExpressiveShapes.large)
        assertEquals(RoundedCornerShape(28.dp), ExpressiveShapes.extraLarge)
    }

    @Test
    fun tier1_f2_typographyTokens_strictM3ExpressiveScale() {
        // titleMedium used for settings card headers
        assertEquals(16.sp, ExpressiveTypography.titleMedium.fontSize)
        assertEquals(FontWeight.SemiBold, ExpressiveTypography.titleMedium.fontWeight)

        // bodyLarge used for slider labels & switch labels
        assertEquals(16.sp, ExpressiveTypography.bodyLarge.fontSize)
        assertEquals(FontWeight.Medium, ExpressiveTypography.bodyLarge.fontWeight)

        // labelLarge used for slider numeric badge values
        assertEquals(14.sp, ExpressiveTypography.labelLarge.fontSize)
        assertEquals(FontWeight.SemiBold, ExpressiveTypography.labelLarge.fontWeight)

        // labelMedium used for preset button pills
        assertEquals(12.sp, ExpressiveTypography.labelMedium.fontSize)
        assertEquals(FontWeight.Medium, ExpressiveTypography.labelMedium.fontWeight)

        // labelSmall used for squish simulation chip badge
        assertEquals(11.sp, ExpressiveTypography.labelSmall.fontSize)
        assertEquals(FontWeight.Medium, ExpressiveTypography.labelSmall.fontWeight)
    }

    @Test
    fun tier1_f8_springPhysicsTokens_dampingRatiosWithinOrganicRange() {
        // M3 Expressive spatial spring damping: 0.88 (soft, organic, no bounce)
        assertEquals(0.88f, ExpressiveSpatialSpringDamping, 0.001f)
        // Effects spring: 1.0 (critically damped)
        assertEquals(1.0f, ExpressiveEffectsSpringDamping, 0.001f)
    }

    @Test
    fun tier1_f14_all17ConfigurationKeys_matchOfficialDefaults() {
        val state = ChipSettingsState()

        assertEquals(ChipSettings.MODE_TWO_LINES, state.layoutMode)
        assertEquals(ChipSettings.SQUISH_TO_ICON, state.squishMode)
        assertTrue(state.showArtist)
        assertTrue(state.marqueeOneLine)
        assertTrue(state.monetTint)
        assertTrue(state.swipeGestures)
        assertTrue(state.hapticEnabled)
        assertFalse(state.eatCamera)
        assertTrue(state.visualizerCardEnabled)
        assertTrue(state.visualizerChipEnabled)
        assertEquals(100, state.visualizerSensitivity)
        assertEquals(105, state.maxWidthDp)
        assertEquals(105, state.maxTextWidthDp)
        assertEquals(16, state.cameraEatDepthDp)
        assertEquals(10.0f, state.titleSizeSp, 0.001f)
        assertEquals(8.0f, state.artistSizeSp, 0.001f)
        assertEquals(14, state.cornerRadiusDp)
    }

    @Test
    fun tier1_f14_all17ConfigurationKeyConstants_areProperlyDefined() {
        val expectedKeys = listOf(
            ChipSettings.KEY_LAYOUT_MODE to "layout_mode",
            ChipSettings.KEY_SQUISH_MODE to "squish_mode",
            ChipSettings.KEY_SHOW_ARTIST to "show_artist",
            ChipSettings.KEY_MARQUEE_ONE_LINE to "marquee_one_line",
            ChipSettings.KEY_MONET_TINT to "monet_tint",
            ChipSettings.KEY_SWIPE_GESTURES to "swipe_gestures",
            ChipSettings.KEY_HAPTIC_ENABLED to "haptic_enabled",
            ChipSettings.KEY_MAX_WIDTH_DP to "max_width_dp",
            ChipSettings.KEY_TITLE_SIZE_SP to "title_size_sp",
            ChipSettings.KEY_ARTIST_SIZE_SP to "artist_size_sp",
            ChipSettings.KEY_CORNER_RADIUS_DP to "corner_radius_dp",
            ChipSettings.KEY_EAT_CAMERA to "eat_camera",
            ChipSettings.KEY_MAX_TEXT_WIDTH_DP to "max_text_width_dp",
            ChipSettings.KEY_CAMERA_EAT_DEPTH_DP to "camera_eat_depth_dp",
            ChipSettings.KEY_VISUALIZER_CARD_ENABLED to "visualizer_card_enabled",
            ChipSettings.KEY_VISUALIZER_CHIP_ENABLED to "visualizer_chip_enabled",
            ChipSettings.KEY_VISUALIZER_SENSITIVITY to "visualizer_sensitivity"
        )

        assertEquals(17, expectedKeys.size)
        expectedKeys.forEach { (constantVal, expectedStr) ->
            assertEquals(expectedStr, constantVal)
        }
    }

    @Test
    fun tier1_f14_stateToChipSettingsAndReverse_preservesAll17Keys() {
        val initial = ChipSettingsState(
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
            visualizerSensitivity = 175,
            maxWidthDp = 185,
            maxTextWidthDp = 135,
            cameraEatDepthDp = 26,
            titleSizeSp = 12.0f,
            artistSizeSp = 9.0f,
            cornerRadiusDp = 22
        )

        val settings = initial.toChipSettings()
        val restored = settings.toState()

        assertEquals(initial, restored)
    }

    @Test
    fun tier1_f10_livePreviewMath_cameraSafeTextPadding_happyPath() {
        // When eatCamera = false, padding is base padding
        val noEat = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = false,
            cameraEatDepthDp = 16,
            basePaddingDp = 6
        )
        assertEquals(6, noEat)

        // When eatCamera = true, padding is cameraEatDepthDp + basePaddingDp
        val eat16 = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = 16,
            basePaddingDp = 6
        )
        assertEquals(22, eat16)

        val eat20 = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = 20,
            basePaddingDp = 6
        )
        assertEquals(26, eat20)
    }

    @Test
    fun tier1_f11_livePreviewMath_effectiveChipWidth_happyPath() {
        // Two lines normal
        val widthTwoLines = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 105,
            maxTextWidthDp = 105,
            eatCamera = false,
            cameraEatDepthDp = 16,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(105, widthTwoLines)

        // One line normal
        val widthOneLine = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_ONE_LINE,
            maxWidthDp = 105,
            maxTextWidthDp = 125,
            eatCamera = false,
            cameraEatDepthDp = 16,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(125, widthOneLine)

        // Icon only normal
        val widthIcon = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_ICON_ONLY,
            maxWidthDp = 105,
            maxTextWidthDp = 125,
            eatCamera = false,
            cameraEatDepthDp = 16,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(28, widthIcon)
    }

    @Test
    fun tier1_f13_presetApplication_allFourPresetsHappyPath() {
        val base = ChipSettingsState()

        // 1. Pixel Default
        val defaultPreset = base.applyPreset(ChipPreset.PIXEL_DEFAULT)
        assertEquals(ChipSettings.MODE_TWO_LINES, defaultPreset.layoutMode)
        assertEquals(105, defaultPreset.maxWidthDp)
        assertFalse(defaultPreset.eatCamera)

        // 2. Dynamic Island
        val dynamicIsland = base.applyPreset(ChipPreset.DYNAMIC_ISLAND)
        assertEquals(ChipSettings.MODE_ONE_LINE, dynamicIsland.layoutMode)
        assertTrue(dynamicIsland.eatCamera)
        assertEquals(125, dynamicIsland.maxTextWidthDp)
        assertEquals(20, dynamicIsland.cameraEatDepthDp)
        assertEquals(16, dynamicIsland.cornerRadiusDp)
        assertEquals(10.5f, dynamicIsland.titleSizeSp, 0.001f)

        // 3. Compact
        val compact = base.applyPreset(ChipPreset.COMPACT)
        assertEquals(ChipSettings.MODE_ICON_ONLY, compact.layoutMode)
        assertFalse(compact.eatCamera)
        assertEquals(ChipSettings.SQUISH_DISABLED, compact.squishMode)
        assertEquals(14, compact.cornerRadiusDp)

        // 4. Expanded
        val expanded = base.applyPreset(ChipPreset.EXPANDED)
        assertEquals(ChipSettings.MODE_TWO_LINES, expanded.layoutMode)
        assertFalse(expanded.eatCamera)
        assertEquals(180, expanded.maxWidthDp)
        assertEquals(16, expanded.cornerRadiusDp)
        assertEquals(11.0f, expanded.titleSizeSp, 0.001f)
        assertEquals(8.5f, expanded.artistSizeSp, 0.001f)
    }

    @Test
    fun tier1_f15_settingsProviderConstantsAndMapRoundTrip() {
        assertEquals("com.pixel.mediachip.provider", SettingsProvider.AUTHORITY)
        assertEquals("getSettings", SettingsProvider.METHOD_GET_SETTINGS)
        assertEquals("saveSettings", SettingsProvider.METHOD_SAVE_SETTINGS)
        assertEquals("com.pixel.mediachip.ACTION_SETTINGS_CHANGED", ChipSettings.ACTION_SETTINGS_CHANGED)

        // Map serialization round trip
        chipSettings.applyPresetDynamicIsland()
        chipSettings.visualizerSensitivity = 160
        chipSettings.hapticEnabled = false

        val map = chipSettings.toMap()
        assertEquals(17, map.size)

        val restored = ChipSettings()
        restored.fromMap(map)

        assertEquals(chipSettings.layoutMode, restored.layoutMode)
        assertEquals(chipSettings.eatCamera, restored.eatCamera)
        assertEquals(chipSettings.visualizerSensitivity, restored.visualizerSensitivity)
        assertEquals(chipSettings.hapticEnabled, restored.hapticEnabled)
        assertEquals(chipSettings.cameraEatDepthDp, restored.cameraEatDepthDp)
        assertEquals(chipSettings.maxTextWidthDp, restored.maxTextWidthDp)
        assertEquals(chipSettings.titleSizeSp, restored.titleSizeSp, 0.001f)
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES
    // =========================================================================

    @Test
    fun tier2_f7_sliderClamping_titleSizeSp_minMaxAndSteps() {
        val min = 8.0f
        val max = 14.0f
        val step = 0.5f

        // Minimum limit
        assertEquals(8.0f, snapAndClamp(8.0f, min, max, step), 0.001f)
        assertEquals(8.0f, snapAndClamp(7.9f, min, max, step), 0.001f)
        assertEquals(8.0f, snapAndClamp(0.0f, min, max, step), 0.001f)
        assertEquals(8.0f, snapAndClamp(-10.0f, min, max, step), 0.001f)

        // Maximum limit
        assertEquals(14.0f, snapAndClamp(14.0f, min, max, step), 0.001f)
        assertEquals(14.0f, snapAndClamp(14.1f, min, max, step), 0.001f)
        assertEquals(14.0f, snapAndClamp(25.0f, min, max, step), 0.001f)

        // Half-sp steps
        assertEquals(8.0f, snapAndClamp(8.24f, min, max, step), 0.001f)
        assertEquals(8.5f, snapAndClamp(8.26f, min, max, step), 0.001f)
        assertEquals(10.0f, snapAndClamp(9.8f, min, max, step), 0.001f)
        assertEquals(10.5f, snapAndClamp(10.4f, min, max, step), 0.001f)
    }

    @Test
    fun tier2_f7_sliderClamping_artistSizeSp_minMaxAndSteps() {
        val min = 6.0f
        val max = 12.0f
        val step = 0.5f

        assertEquals(6.0f, snapAndClamp(5.5f, min, max, step), 0.001f)
        assertEquals(12.0f, snapAndClamp(15.0f, min, max, step), 0.001f)
        assertEquals(6.5f, snapAndClamp(6.3f, min, max, step), 0.001f)
        assertEquals(8.0f, snapAndClamp(8.1f, min, max, step), 0.001f)
    }

    @Test
    fun tier2_f7_sliderClamping_maxWidthDp_stepFiveDp() {
        val min = 60.0f
        val max = 220.0f
        val step = 5.0f

        // Underflow & Overflow
        assertEquals(60.0f, snapAndClamp(20.0f, min, max, step), 0.001f)
        assertEquals(220.0f, snapAndClamp(350.0f, min, max, step), 0.001f)

        // Step snapping
        assertEquals(60.0f, snapAndClamp(62.4f, min, max, step), 0.001f)
        assertEquals(65.0f, snapAndClamp(62.6f, min, max, step), 0.001f)
        assertEquals(105.0f, snapAndClamp(104.0f, min, max, step), 0.001f)
        assertEquals(105.0f, snapAndClamp(107.4f, min, max, step), 0.001f)
        assertEquals(110.0f, snapAndClamp(107.6f, min, max, step), 0.001f)
    }

    @Test
    fun tier2_f7_sliderClamping_cameraEatDepthDp_stepTwoDp() {
        val min = 8.0f
        val max = 32.0f
        val step = 2.0f

        assertEquals(8.0f, snapAndClamp(2.0f, min, max, step), 0.001f)
        assertEquals(32.0f, snapAndClamp(45.0f, min, max, step), 0.001f)
        assertEquals(16.0f, snapAndClamp(15.5f, min, max, step), 0.001f)
        assertEquals(18.0f, snapAndClamp(17.1f, min, max, step), 0.001f)
    }

    @Test
    fun tier2_f7_sliderClamping_cornerRadiusDp_stepOneDp() {
        val min = 4.0f
        val max = 24.0f
        val step = 1.0f

        assertEquals(4.0f, snapAndClamp(0.0f, min, max, step), 0.001f)
        assertEquals(24.0f, snapAndClamp(50.0f, min, max, step), 0.001f)
        assertEquals(14.0f, snapAndClamp(14.4f, min, max, step), 0.001f)
        assertEquals(15.0f, snapAndClamp(14.6f, min, max, step), 0.001f)
    }

    @Test
    fun tier2_f7_sliderClamping_visualizerSensitivity_stepFivePercent() {
        val min = 50.0f
        val max = 200.0f
        val step = 5.0f

        assertEquals(50.0f, snapAndClamp(10.0f, min, max, step), 0.001f)
        assertEquals(200.0f, snapAndClamp(300.0f, min, max, step), 0.001f)
        assertEquals(100.0f, snapAndClamp(101.5f, min, max, step), 0.001f)
        assertEquals(105.0f, snapAndClamp(103.0f, min, max, step), 0.001f)
    }

    @Test
    fun tier2_f3_concentricRadius_extremeAndNegativeBoundaries() {
        // Zero outer radius
        assertEquals(0.dp, calculateConcentricRadius(0.dp, 10.dp))
        // Negative outer radius
        assertEquals(0.dp, calculateConcentricRadius((-5).dp, 10.dp))
        // Padding exceeds outer radius
        assertEquals(0.dp, calculateConcentricRadius(10.dp, 20.dp))
        // Exact match
        assertEquals(0.dp, calculateConcentricRadius(16.dp, 16.dp))

        // Parity in M3Motion Java
        assertEquals(0, M3Motion.calculateConcentricRadius(0, 10))
        assertEquals(0, M3Motion.calculateConcentricRadius(-5, 10))
        assertEquals(0, M3Motion.calculateConcentricRadius(10, 20))
        assertEquals(0, M3Motion.calculateConcentricRadius(16, 16))
    }

    @Test
    fun tier2_f10_livePreviewMath_negativeDepthClamping() {
        // Negative cameraEatDepthDp clamped to 0 + base
        val padding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = -25,
            basePaddingDp = 6
        )
        assertEquals(6, padding)

        // Negative basePaddingDp clamped to 0
        val paddingNegativeBase = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = false,
            cameraEatDepthDp = 16,
            basePaddingDp = -8
        )
        assertEquals(0, paddingNegativeBase)
    }

    @Test
    fun tier2_f11_livePreviewMath_effectiveWidthNegativeInputsClamping() {
        // Extreme negative width should clamp to icon size (28dp)
        val clampedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = -100,
            maxTextWidthDp = -100,
            eatCamera = false,
            cameraEatDepthDp = -50,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(28, clampedWidth)
    }

    @Test
    fun tier2_f15_nullAndEmptyBundleAndMapSafety() {
        // Null bundle
        chipSettings.fromBundle(null)
        assertEquals(ChipSettings.MODE_TWO_LINES, chipSettings.layoutMode)

        // Empty bundle
        chipSettings.fromBundle(Bundle())
        assertEquals(ChipSettings.MODE_TWO_LINES, chipSettings.layoutMode)

        // Null map
        chipSettings.fromMap(null)
        assertEquals(ChipSettings.MODE_TWO_LINES, chipSettings.layoutMode)

        // Empty map
        chipSettings.fromMap(emptyMap())
        assertEquals(ChipSettings.MODE_TWO_LINES, chipSettings.layoutMode)

        // Copy from null
        chipSettings.copyFrom(null)
        assertEquals(ChipSettings.MODE_TWO_LINES, chipSettings.layoutMode)
    }

    @Test
    fun tier2_f7_antiDoubleBounceStateMachine_discreteTransitionsOnly() {
        val min = 8.0f
        val max = 14.0f
        val step = 0.5f

        var lastHapticStep = ((8.0f - min) / step).roundToInt()
        var hapticTicks = 0

        // Micro-wobbles around 8.0f without crossing the 8.25f threshold
        val microWobbles = listOf(8.0f, 8.02f, 8.08f, 8.12f, 8.19f, 8.24f, 8.15f, 8.05f)
        for (wobble in microWobbles) {
            val clamped = snapAndClamp(wobble, min, max, step)
            val currentStep = ((clamped - min) / step).roundToInt()
            if (currentStep != lastHapticStep) {
                lastHapticStep = currentStep
                hapticTicks++
            }
        }
        assertEquals(0, hapticTicks)

        // Cross exactly one boundary to 8.5f (at or above 8.25f)
        val crossingList = listOf(8.26f, 8.35f, 8.48f, 8.52f, 8.65f)
        for (crossing in crossingList) {
            val clamped = snapAndClamp(crossing, min, max, step)
            val currentStep = ((clamped - min) / step).roundToInt()
            if (currentStep != lastHapticStep) {
                lastHapticStep = currentStep
                hapticTicks++
            }
        }
        assertEquals(1, hapticTicks)
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS (PAIRWISE INTERACTIONS)
    // =========================================================================

    @Test
    fun tier3_pairwise_eatCameraAndSquishModes_allCombinationsMatrix() {
        val testModes = listOf(
            ChipSettings.SQUISH_TO_ICON,
            ChipSettings.SQUISH_HIDE,
            ChipSettings.SQUISH_DISABLED
        )
        val eatCameraOptions = listOf(false, true)

        for (eat in eatCameraOptions) {
            for (squish in testModes) {
                val normalWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
                    layoutMode = ChipSettings.MODE_TWO_LINES,
                    maxWidthDp = 105,
                    maxTextWidthDp = 105,
                    eatCamera = eat,
                    cameraEatDepthDp = 16,
                    isSquished = false,
                    squishMode = squish
                )

                val squishedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
                    layoutMode = ChipSettings.MODE_TWO_LINES,
                    maxWidthDp = 105,
                    maxTextWidthDp = 105,
                    eatCamera = eat,
                    cameraEatDepthDp = 16,
                    isSquished = true,
                    squishMode = squish
                )

                val isVisible = LivePreviewMath.isChipVisibleWhenSquished(isSquished = true, squishMode = squish)

                when (squish) {
                    ChipSettings.SQUISH_TO_ICON -> {
                        assertEquals(if (eat) 121 else 105, normalWidth)
                        assertEquals(28, squishedWidth)
                        assertTrue(isVisible)
                    }
                    ChipSettings.SQUISH_HIDE -> {
                        assertEquals(if (eat) 121 else 105, normalWidth)
                        assertEquals(0, squishedWidth)
                        assertFalse(isVisible)
                    }
                    ChipSettings.SQUISH_DISABLED -> {
                        assertEquals(if (eat) 121 else 105, normalWidth)
                        assertEquals(if (eat) 121 else 105, squishedWidth)
                        assertTrue(isVisible)
                    }
                }
            }
        }
    }

    @Test
    fun tier3_pairwise_dynamicIslandPresetWithCustomTypographyOverrides() {
        val baseState = ChipSettingsState().applyPreset(ChipPreset.DYNAMIC_ISLAND)

        assertEquals(ChipSettings.MODE_ONE_LINE, baseState.layoutMode)
        assertTrue(baseState.eatCamera)
        assertEquals(10.5f, baseState.titleSizeSp, 0.001f)

        // User overrides typography and text width
        val customized = baseState.copy(
            titleSizeSp = 13.5f,
            artistSizeSp = 11.0f,
            maxTextWidthDp = 165
        )

        // Verified preserved Dynamic Island features
        assertTrue(customized.eatCamera)
        assertEquals(20, customized.cameraEatDepthDp)
        assertEquals(16, customized.cornerRadiusDp)

        // Verified effective width math adapts to custom width + eatCamera depth
        val effectiveWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = customized.layoutMode,
            maxWidthDp = customized.maxWidthDp,
            maxTextWidthDp = customized.maxTextWidthDp,
            eatCamera = customized.eatCamera,
            cameraEatDepthDp = customized.cameraEatDepthDp,
            isSquished = false,
            squishMode = customized.squishMode
        )
        assertEquals(165 + 20, effectiveWidth)

        // Camera safe text padding remains intact
        val safePadding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = customized.eatCamera,
            cameraEatDepthDp = customized.cameraEatDepthDp
        )
        assertEquals(26, safePadding)
    }

    @Test
    fun tier3_pairwise_compactPresetWithVisualizerFlags() {
        val compactState = ChipSettingsState().applyPreset(ChipPreset.COMPACT)
        assertEquals(ChipSettings.MODE_ICON_ONLY, compactState.layoutMode)
        assertEquals(ChipSettings.SQUISH_DISABLED, compactState.squishMode)

        // Toggle visualizer flags
        val modified = compactState.copy(
            visualizerChipEnabled = false,
            visualizerCardEnabled = true,
            visualizerSensitivity = 180
        )

        assertFalse(modified.visualizerChipEnabled)
        assertTrue(modified.visualizerCardEnabled)
        assertEquals(180, modified.visualizerSensitivity)

        // Icon only width is always 28dp regardless of visualizer settings
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = modified.layoutMode,
            maxWidthDp = modified.maxWidthDp,
            maxTextWidthDp = modified.maxTextWidthDp,
            eatCamera = modified.eatCamera,
            cameraEatDepthDp = modified.cameraEatDepthDp,
            isSquished = false,
            squishMode = modified.squishMode
        )
        assertEquals(28, width)
        assertTrue(LivePreviewMath.isChipInIconOnlyState(modified.layoutMode, false, modified.squishMode))
    }

    @Test
    fun tier3_pairwise_monetTintAndSquishTransitions() {
        var state = ChipSettingsState(monetTint = true, squishMode = ChipSettings.SQUISH_TO_ICON)

        // Squished state with Monet tint
        var preview = state.toPreviewSettings()
        assertTrue(preview.monetTint)
        assertTrue(LivePreviewMath.isChipVisibleWhenSquished(isSquished = true, squishMode = preview.squishMode))

        // Toggle Monet tint off while squished
        state = state.copy(monetTint = false)
        preview = state.toPreviewSettings()
        assertFalse(preview.monetTint)
        assertEquals(ChipSettings.SQUISH_TO_ICON, preview.squishMode)
    }

    @Test
    fun tier3_pairwise_expandedPresetWithEatCameraEnabled() {
        val expanded = ChipSettingsState().applyPreset(ChipPreset.EXPANDED)
        assertEquals(ChipSettings.MODE_TWO_LINES, expanded.layoutMode)
        assertFalse(expanded.eatCamera)
        assertEquals(180, expanded.maxWidthDp)

        // User now enables eatCamera on Expanded preset
        val expandedWithCamera = expanded.copy(
            eatCamera = true,
            cameraEatDepthDp = 24
        )

        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = expandedWithCamera.layoutMode,
            maxWidthDp = expandedWithCamera.maxWidthDp,
            maxTextWidthDp = expandedWithCamera.maxTextWidthDp,
            eatCamera = expandedWithCamera.eatCamera,
            cameraEatDepthDp = expandedWithCamera.cameraEatDepthDp,
            isSquished = false,
            squishMode = expandedWithCamera.squishMode
        )
        assertEquals(180 + 24, width)

        val padding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = expandedWithCamera.eatCamera,
            cameraEatDepthDp = expandedWithCamera.cameraEatDepthDp
        )
        assertEquals(24 + 6, padding)
    }

    // =========================================================================
    // TIER 4: REAL-WORLD SCENARIOS (END-TO-END WORKFLOWS)
    // =========================================================================

    @Test
    fun tier4_scenario1_defaultToDynamicIsland_customFont_squish_resetToDefault() {
        // Step 1: User starts at Default Pixel settings
        var state = ChipSettingsState()
        assertEquals(ChipSettings.MODE_TWO_LINES, state.layoutMode)
        assertEquals(105, state.maxWidthDp)
        assertFalse(state.eatCamera)

        // Step 2: User switches to Dynamic Island preset
        state = state.applyPreset(ChipPreset.DYNAMIC_ISLAND)
        assertEquals(ChipSettings.MODE_ONE_LINE, state.layoutMode)
        assertTrue(state.eatCamera)
        assertEquals(125, state.maxTextWidthDp)
        assertEquals(20, state.cameraEatDepthDp)

        // Step 3: User customizes font and dimensions
        state = state.copy(
            titleSizeSp = 12.0f,
            artistSizeSp = 9.5f,
            cornerRadiusDp = 20
        )
        assertEquals(12.0f, state.titleSizeSp, 0.001f)
        assertEquals(9.5f, state.artistSizeSp, 0.001f)
        assertEquals(20, state.cornerRadiusDp)

        // Step 4: User tests Live Preview squish simulation
        val squishedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = state.layoutMode,
            maxWidthDp = state.maxWidthDp,
            maxTextWidthDp = state.maxTextWidthDp,
            eatCamera = state.eatCamera,
            cameraEatDepthDp = state.cameraEatDepthDp,
            isSquished = true,
            squishMode = state.squishMode
        )
        assertEquals(28, squishedWidth)
        assertTrue(LivePreviewMath.isChipInIconOnlyState(state.layoutMode, true, state.squishMode))

        // Step 5: User clicks Reset to defaults
        state = state.resetToDefaults()
        val defaultState = ChipSettingsState()
        assertEquals(defaultState, state)
    }

    @Test
    fun tier4_scenario2_compactUserFastPresetHoppingFlow() {
        var state = ChipSettingsState()

        // Hop 1: Switch to Compact
        state = state.applyPreset(ChipPreset.COMPACT)
        assertEquals(ChipSettings.MODE_ICON_ONLY, state.layoutMode)
        assertEquals(ChipSettings.SQUISH_DISABLED, state.squishMode)

        // Hop 2: Switch to Expanded
        state = state.applyPreset(ChipPreset.EXPANDED)
        assertEquals(ChipSettings.MODE_TWO_LINES, state.layoutMode)
        assertEquals(180, state.maxWidthDp)
        assertEquals(11.0f, state.titleSizeSp, 0.001f)

        // Hop 3: Switch to Dynamic Island
        state = state.applyPreset(ChipPreset.DYNAMIC_ISLAND)
        assertEquals(ChipSettings.MODE_ONE_LINE, state.layoutMode)
        assertTrue(state.eatCamera)
        assertEquals(125, state.maxTextWidthDp)

        // Hop 4: Switch back to Pixel Default
        state = state.applyPreset(ChipPreset.PIXEL_DEFAULT)
        assertEquals(ChipSettingsState(), state)
    }

    @Test
    fun tier4_scenario3_audioEnthusiastCustomizationFlow() {
        var state = ChipSettingsState()

        // 1. Configure audio visualizer parameters
        state = state.copy(
            visualizerSensitivity = 185,
            marqueeOneLine = false,
            showArtist = false
        )
        assertEquals(185, state.visualizerSensitivity)
        assertFalse(state.marqueeOneLine)
        assertFalse(state.showArtist)

        // 2. Adjust layout to one line with high text width and camera eating
        state = state.copy(
            layoutMode = ChipSettings.MODE_ONE_LINE,
            maxTextWidthDp = 150,
            eatCamera = true,
            cameraEatDepthDp = 28,
            squishMode = ChipSettings.SQUISH_HIDE
        )

        // Normal effective width: 150 + 28 = 178dp
        val normalWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = state.layoutMode,
            maxWidthDp = state.maxWidthDp,
            maxTextWidthDp = state.maxTextWidthDp,
            eatCamera = state.eatCamera,
            cameraEatDepthDp = state.cameraEatDepthDp,
            isSquished = false,
            squishMode = state.squishMode
        )
        assertEquals(178, normalWidth)

        // Squished effective width under SQUISH_HIDE: 0dp (hidden)
        val squishedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = state.layoutMode,
            maxWidthDp = state.maxWidthDp,
            maxTextWidthDp = state.maxTextWidthDp,
            eatCamera = state.eatCamera,
            cameraEatDepthDp = state.cameraEatDepthDp,
            isSquished = true,
            squishMode = state.squishMode
        )
        assertEquals(0, squishedWidth)
        assertFalse(LivePreviewMath.isChipVisibleWhenSquished(isSquished = true, squishMode = state.squishMode))

        // Return from squished
        val unsquishedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = state.layoutMode,
            maxWidthDp = state.maxWidthDp,
            maxTextWidthDp = state.maxTextWidthDp,
            eatCamera = state.eatCamera,
            cameraEatDepthDp = state.cameraEatDepthDp,
            isSquished = false,
            squishMode = state.squishMode
        )
        assertEquals(178, unsquishedWidth)
    }

    @Test
    fun tier4_scenario4_extremeBoundaryTuningWorkflow() {
        // Step 1: User pushes all sliders to absolute minimums
        val minState = ChipSettingsState(
            titleSizeSp = 8.0f,
            artistSizeSp = 6.0f,
            maxWidthDp = 60,
            maxTextWidthDp = 60,
            cameraEatDepthDp = 8,
            cornerRadiusDp = 4,
            visualizerSensitivity = 50
        )

        // Concentric inner radius at 4dp outer and 12dp padding clamps to 0dp
        assertEquals(0.dp, calculateConcentricRadius(minState.cornerRadiusDp.dp, 12.dp))

        // Step 2: User pushes all sliders to absolute maximums
        val maxState = ChipSettingsState(
            titleSizeSp = 14.0f,
            artistSizeSp = 12.0f,
            maxWidthDp = 220,
            maxTextWidthDp = 220,
            cameraEatDepthDp = 32,
            cornerRadiusDp = 24,
            visualizerSensitivity = 200,
            eatCamera = true
        )

        // Effective width with maximums: 220 + 32 = 252dp
        val maxEffectiveWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = maxState.layoutMode,
            maxWidthDp = maxState.maxWidthDp,
            maxTextWidthDp = maxState.maxTextWidthDp,
            eatCamera = maxState.eatCamera,
            cameraEatDepthDp = maxState.cameraEatDepthDp,
            isSquished = false,
            squishMode = maxState.squishMode
        )
        assertEquals(252, maxEffectiveWidth)

        // Camera safe text padding: 32 + 6 = 38dp
        val maxSafePadding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = maxState.eatCamera,
            cameraEatDepthDp = maxState.cameraEatDepthDp
        )
        assertEquals(38, maxSafePadding)

        // Inner concentric radius at 24dp outer and 16dp padding = 8dp
        assertEquals(8.dp, calculateConcentricRadius(maxState.cornerRadiusDp.dp, 16.dp))

        // Step 3: Map serialization preserves extremes perfectly
        val target = maxState.toChipSettings()
        val map = target.toMap()
        val restoredTarget = ChipSettings()
        restoredTarget.fromMap(map)
        assertEquals(maxState, restoredTarget.toState())
    }

    @Test
    fun tier4_scenario5_multiStepPersistenceRoundTripFlow() {
        // Multi-session simulation:
        // Session 1: User configures Dynamic Island with tweaks
        val session1State = ChipSettingsState().applyPreset(ChipPreset.DYNAMIC_ISLAND).copy(
            monetTint = false,
            hapticEnabled = false,
            visualizerSensitivity = 145
        )

        val session1Settings = session1State.toChipSettings()
        val session1Map = session1Settings.toMap()

        // Session 2: App starts fresh and reloads settings
        val session2Settings = ChipSettings()
        session2Settings.fromMap(session1Map)
        assertEquals(session1State, session2Settings.toState())

        // Session 2 changes: User enables swipe gestures and adjusts corner radius
        val session2State = session2Settings.toState().copy(
            swipeGestures = true,
            cornerRadiusDp = 18,
            titleSizeSp = 11.5f
        )
        val session2Map = session2State.toChipSettings().toMap()

        // Session 3: Reload and verify
        val session3Settings = ChipSettings()
        session3Settings.fromMap(session2Map)
        val session3State = session3Settings.toState()

        assertEquals(session2State, session3State)
        assertEquals(18, session3State.cornerRadiusDp)
        assertEquals(11.5f, session3State.titleSizeSp, 0.001f)
        assertFalse(session3State.monetTint)
        assertFalse(session3State.hapticEnabled)

        // Session 4: User resets to defaults and saves
        val finalState = session3State.resetToDefaults()
        val finalMap = finalState.toChipSettings().toMap()

        val cleanSettings = ChipSettings()
        cleanSettings.fromMap(finalMap)
        assertEquals(ChipSettingsState(), cleanSettings.toState())
    }
}
