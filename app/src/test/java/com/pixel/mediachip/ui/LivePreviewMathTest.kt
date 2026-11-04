package com.pixel.mediachip.ui

import androidx.compose.ui.unit.dp
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.ui.preview.LivePreviewMath
import com.pixel.mediachip.ui.preview.PreviewChipSettings
import com.pixel.mediachip.ui.preview.toPreviewSettings
import com.pixel.mediachip.ui.theme.calculateConcentricRadius
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying camera safe margins, chip width calculations,
 * squish state branching, and preset math consistency for Live Preview.
 */
class LivePreviewMathTest {

    // --- Safe Camera Margins Tests ---

    @Test
    fun cameraSafePadding_eatCameraDisabled_returnsBasePadding() {
        val padding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = false,
            cameraEatDepthDp = 16,
            basePaddingDp = 6
        )
        assertEquals(6, padding)
    }

    @Test
    fun cameraSafePadding_eatCameraEnabled_returnsDepthPlusBase() {
        val depth16 = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = 16,
            basePaddingDp = 6
        )
        assertEquals(22, depth16)

        val depth20 = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = 20,
            basePaddingDp = 6
        )
        assertEquals(26, depth20)

        val depth32 = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = 32,
            basePaddingDp = 6
        )
        assertEquals(38, depth32)
    }

    @Test
    fun cameraSafePadding_negativeDepth_clampsToZeroPlusBase() {
        val padding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = -10,
            basePaddingDp = 6
        )
        assertEquals(6, padding)
    }

    // --- Width Constraints Tests ---

    @Test
    fun chipWidth_modeTwoLines_noEatCamera_returnsMaxWidth() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 105,
            maxTextWidthDp = 105,
            eatCamera = false,
            cameraEatDepthDp = 16,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(105, width)
    }

    @Test
    fun chipWidth_modeTwoLines_eatCamera_returnsMaxWidthPlusDepth() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 105,
            maxTextWidthDp = 105,
            eatCamera = true,
            cameraEatDepthDp = 16,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(121, width)
    }

    @Test
    fun chipWidth_modeOneLine_noEatCamera_returnsMaxTextWidth() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_ONE_LINE,
            maxWidthDp = 105,
            maxTextWidthDp = 125,
            eatCamera = false,
            cameraEatDepthDp = 20,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(125, width)
    }

    @Test
    fun chipWidth_modeOneLine_eatCamera_returnsMaxTextWidthPlusDepth() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_ONE_LINE,
            maxWidthDp = 105,
            maxTextWidthDp = 125,
            eatCamera = true,
            cameraEatDepthDp = 20,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(145, width)
    }

    @Test
    fun chipWidth_modeIconOnly_alwaysReturnsIconSize() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_ICON_ONLY,
            maxWidthDp = 180,
            maxTextWidthDp = 180,
            eatCamera = true,
            cameraEatDepthDp = 32,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(28, width)
    }

    // --- Squish Branching Tests ---

    @Test
    fun squishBranching_squishToIcon_compressesTo28dp() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 180,
            maxTextWidthDp = 180,
            eatCamera = false,
            cameraEatDepthDp = 16,
            isSquished = true,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(28, width)
        assertTrue(LivePreviewMath.isChipVisibleWhenSquished(isSquished = true, squishMode = ChipSettings.SQUISH_TO_ICON))
        assertTrue(LivePreviewMath.isChipInIconOnlyState(layoutMode = ChipSettings.MODE_TWO_LINES, isSquished = true, squishMode = ChipSettings.SQUISH_TO_ICON))
    }

    @Test
    fun squishBranching_squishHide_compressesTo0dpAndInvisible() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 180,
            maxTextWidthDp = 180,
            eatCamera = false,
            cameraEatDepthDp = 16,
            isSquished = true,
            squishMode = ChipSettings.SQUISH_HIDE
        )
        assertEquals(0, width)
        assertFalse(LivePreviewMath.isChipVisibleWhenSquished(isSquished = true, squishMode = ChipSettings.SQUISH_HIDE))
    }

    @Test
    fun squishBranching_squishDisabled_preservesNormalWidth() {
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 180,
            maxTextWidthDp = 180,
            eatCamera = false,
            cameraEatDepthDp = 16,
            isSquished = true,
            squishMode = ChipSettings.SQUISH_DISABLED
        )
        assertEquals(180, width)
        assertTrue(LivePreviewMath.isChipVisibleWhenSquished(isSquished = true, squishMode = ChipSettings.SQUISH_DISABLED))
        assertFalse(LivePreviewMath.isChipInIconOnlyState(layoutMode = ChipSettings.MODE_TWO_LINES, isSquished = true, squishMode = ChipSettings.SQUISH_DISABLED))
    }

    // --- Presets Math Verification ---

    @Test
    fun presetPixelDefault_mathVerification() {
        val settings = ChipSettings()
        settings.applyPresetPixelDefault()
        val preview = settings.toPreviewSettings()

        assertEquals(ChipSettings.MODE_TWO_LINES, preview.layoutMode)
        assertEquals(105, preview.maxWidthDp)
        assertFalse(preview.eatCamera)

        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = preview.layoutMode,
            maxWidthDp = preview.maxWidthDp,
            maxTextWidthDp = preview.maxTextWidthDp,
            eatCamera = preview.eatCamera,
            cameraEatDepthDp = preview.cameraEatDepthDp,
            isSquished = false,
            squishMode = preview.squishMode
        )
        assertEquals(105, width)

        val padding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = preview.eatCamera,
            cameraEatDepthDp = preview.cameraEatDepthDp
        )
        assertEquals(6, padding)
    }

    @Test
    fun presetDynamicIsland_mathVerification() {
        val settings = ChipSettings()
        settings.applyPresetDynamicIsland()
        val preview = settings.toPreviewSettings()

        assertEquals(ChipSettings.MODE_ONE_LINE, preview.layoutMode)
        assertEquals(125, preview.maxTextWidthDp)
        assertEquals(20, preview.cameraEatDepthDp)
        assertTrue(preview.eatCamera)

        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = preview.layoutMode,
            maxWidthDp = preview.maxWidthDp,
            maxTextWidthDp = preview.maxTextWidthDp,
            eatCamera = preview.eatCamera,
            cameraEatDepthDp = preview.cameraEatDepthDp,
            isSquished = false,
            squishMode = preview.squishMode
        )
        assertEquals(145, width)

        val padding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = preview.eatCamera,
            cameraEatDepthDp = preview.cameraEatDepthDp
        )
        assertEquals(26, padding)
    }

    @Test
    fun presetCompact_mathVerification() {
        val settings = ChipSettings()
        settings.applyPresetCompact()
        val preview = settings.toPreviewSettings()

        assertEquals(ChipSettings.MODE_ICON_ONLY, preview.layoutMode)
        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = preview.layoutMode,
            maxWidthDp = preview.maxWidthDp,
            maxTextWidthDp = preview.maxTextWidthDp,
            eatCamera = preview.eatCamera,
            cameraEatDepthDp = preview.cameraEatDepthDp,
            isSquished = false,
            squishMode = preview.squishMode
        )
        assertEquals(28, width)
        assertTrue(LivePreviewMath.isChipInIconOnlyState(preview.layoutMode, false, preview.squishMode))
    }

    @Test
    fun presetExpanded_mathVerification() {
        val settings = ChipSettings()
        settings.applyPresetExpanded()
        val preview = settings.toPreviewSettings()

        assertEquals(ChipSettings.MODE_TWO_LINES, preview.layoutMode)
        assertEquals(180, preview.maxWidthDp)
        assertFalse(preview.eatCamera)

        val width = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = preview.layoutMode,
            maxWidthDp = preview.maxWidthDp,
            maxTextWidthDp = preview.maxTextWidthDp,
            eatCamera = preview.eatCamera,
            cameraEatDepthDp = preview.cameraEatDepthDp,
            isSquished = false,
            squishMode = preview.squishMode
        )
        assertEquals(180, width)
    }

    @Test
    fun livePreviewHeroCard_concentricCornersVerification() {
        val heroOuterRadius = 28.dp
        val heroPadding = 16.dp
        val innerConcentricRadius = calculateConcentricRadius(heroOuterRadius, heroPadding)
        assertEquals(12.dp, innerConcentricRadius)
    }
}
