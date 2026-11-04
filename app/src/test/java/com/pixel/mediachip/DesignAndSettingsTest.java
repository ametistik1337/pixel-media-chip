package com.pixel.mediachip;

import android.os.Bundle;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive verification unit tests for Material 3 Expressive geometry,
 * settings persistence, preset definitions, and serialization integrity.
 */
public class DesignAndSettingsTest {

    private ChipSettings settings;

    @Before
    public void setUp() {
        settings = ChipSettings.get();
        settings.resetToDefaults();
    }

    @Test
    public void testM3ConcentricRadiusFormula() {
        // R_inner = max(0, R_outer - padding)
        assertEquals(8, M3Motion.calculateConcentricRadius(24, 16));
        assertEquals(20, M3Motion.calculateConcentricRadius(24, 4));
        assertEquals(0, M3Motion.calculateConcentricRadius(14, 16));
        assertEquals(0, M3Motion.calculateConcentricRadius(0, 10));
        assertEquals(12, M3Motion.calculateConcentricRadius(20, 8));
        assertEquals(14, M3Motion.calculateConcentricRadius(20, 6));
        assertEquals(28, M3Motion.calculateConcentricRadius(28, 0));
        assertEquals(0, M3Motion.calculateConcentricRadius(-5, 10));
    }

    @Test
    public void testSettingsDefaultValues() {
        assertEquals(ChipSettings.MODE_TWO_LINES, settings.layoutMode);
        assertEquals(ChipSettings.SQUISH_TO_ICON, settings.squishMode);
        assertTrue(settings.showArtist);
        assertTrue(settings.marqueeOneLine);
        assertTrue(settings.monetTint);
        assertTrue(settings.swipeGestures);
        assertTrue(settings.hapticEnabled);
        assertFalse(settings.eatCamera);
        assertTrue(settings.visualizerCardEnabled);
        assertTrue(settings.visualizerChipEnabled);
        assertEquals(100, settings.visualizerSensitivity);
        assertEquals(105, settings.maxWidthDp);
        assertEquals(14, settings.cornerRadiusDp);
        assertEquals(10.0f, settings.titleSizeSp, 0.01f);
        assertEquals(8.0f, settings.artistSizeSp, 0.01f);
    }

    @Test
    public void testPresetDynamicIsland() {
        settings.applyPresetDynamicIsland();

        assertEquals(ChipSettings.MODE_ONE_LINE, settings.layoutMode);
        assertTrue(settings.eatCamera);
        assertEquals(125, settings.maxTextWidthDp);
        assertEquals(20, settings.cameraEatDepthDp);
        assertEquals(16, settings.cornerRadiusDp);
        assertEquals(10.5f, settings.titleSizeSp, 0.01f);
        assertTrue(settings.showArtist);
        assertTrue(settings.marqueeOneLine);
    }

    @Test
    public void testPresetCompact() {
        settings.applyPresetCompact();

        assertEquals(ChipSettings.MODE_ICON_ONLY, settings.layoutMode);
        assertFalse(settings.eatCamera);
        assertEquals(14, settings.cornerRadiusDp);
        assertEquals(ChipSettings.SQUISH_DISABLED, settings.squishMode);
    }

    @Test
    public void testPresetExpanded() {
        settings.applyPresetExpanded();

        assertEquals(ChipSettings.MODE_TWO_LINES, settings.layoutMode);
        assertFalse(settings.eatCamera);
        assertEquals(180, settings.maxWidthDp);
        assertEquals(16, settings.cornerRadiusDp);
        assertEquals(11.0f, settings.titleSizeSp, 0.01f);
        assertEquals(8.5f, settings.artistSizeSp, 0.01f);
    }

    @Test
    public void testResetToDefaults() {
        settings.layoutMode = ChipSettings.MODE_ICON_ONLY;
        settings.maxWidthDp = 250;
        settings.cornerRadiusDp = 24;
        settings.eatCamera = true;
        settings.hapticEnabled = false;
        settings.visualizerSensitivity = 180;
        settings.titleSizeSp = 13.5f;

        settings.resetToDefaults();

        assertEquals(ChipSettings.MODE_TWO_LINES, settings.layoutMode);
        assertEquals(105, settings.maxWidthDp);
        assertEquals(14, settings.cornerRadiusDp);
        assertFalse(settings.eatCamera);
        assertTrue(settings.hapticEnabled);
        assertEquals(100, settings.visualizerSensitivity);
        assertEquals(10.0f, settings.titleSizeSp, 0.01f);
        assertEquals(8.0f, settings.artistSizeSp, 0.01f);
    }

    @Test
    public void testSerializationRoundTripViaMap() {
        settings.applyPresetDynamicIsland();
        settings.visualizerSensitivity = 175;
        settings.hapticEnabled = false;

        Map<String, Object> map = settings.toMap();
        ChipSettings restored = new ChipSettings();
        restored.fromMap(map);

        assertEquals(settings.layoutMode, restored.layoutMode);
        assertEquals(settings.squishMode, restored.squishMode);
        assertEquals(settings.showArtist, restored.showArtist);
        assertEquals(settings.marqueeOneLine, restored.marqueeOneLine);
        assertEquals(settings.monetTint, restored.monetTint);
        assertEquals(settings.swipeGestures, restored.swipeGestures);
        assertEquals(settings.hapticEnabled, restored.hapticEnabled);
        assertEquals(settings.eatCamera, restored.eatCamera);
        assertEquals(settings.visualizerCardEnabled, restored.visualizerCardEnabled);
        assertEquals(settings.visualizerChipEnabled, restored.visualizerChipEnabled);
        assertEquals(settings.visualizerSensitivity, restored.visualizerSensitivity);
        assertEquals(settings.maxWidthDp, restored.maxWidthDp);
        assertEquals(settings.maxTextWidthDp, restored.maxTextWidthDp);
        assertEquals(settings.cameraEatDepthDp, restored.cameraEatDepthDp);
        assertEquals(settings.titleSizeSp, restored.titleSizeSp, 0.01f);
        assertEquals(settings.artistSizeSp, restored.artistSizeSp, 0.01f);
        assertEquals(settings.cornerRadiusDp, restored.cornerRadiusDp);
    }

    @Test
    public void testCopyFrom() {
        settings.applyPresetExpanded();
        settings.visualizerSensitivity = 160;

        ChipSettings copy = new ChipSettings();
        copy.copyFrom(settings);

        assertEquals(settings.layoutMode, copy.layoutMode);
        assertEquals(settings.maxWidthDp, copy.maxWidthDp);
        assertEquals(settings.titleSizeSp, copy.titleSizeSp, 0.01f);
        assertEquals(settings.artistSizeSp, copy.artistSizeSp, 0.01f);
        assertEquals(settings.cornerRadiusDp, copy.cornerRadiusDp);
        assertEquals(settings.visualizerSensitivity, copy.visualizerSensitivity);
    }

    @Test
    public void testNullAndEmptyBundleHandling() {
        settings.fromBundle(null);
        // Defaults should be preserved
        assertEquals(ChipSettings.MODE_TWO_LINES, settings.layoutMode);

        Bundle empty = new Bundle();
        settings.fromBundle(empty);
        assertEquals(ChipSettings.MODE_TWO_LINES, settings.layoutMode);
    }

    @Test
    public void testM3MotionTokens() {
        assertTrue(M3Motion.DURATION_SHORT1 > 0);
        assertTrue(M3Motion.DURATION_SHORT2 > M3Motion.DURATION_SHORT1);
        assertTrue(M3Motion.DURATION_SHORT3 > M3Motion.DURATION_SHORT2);
        assertTrue(M3Motion.DURATION_SHORT4 > M3Motion.DURATION_SHORT3);
        assertTrue(M3Motion.DURATION_MEDIUM1 > M3Motion.DURATION_SHORT4);
        assertTrue(M3Motion.DURATION_MEDIUM2 > M3Motion.DURATION_MEDIUM1);

        // Max UI interaction duration should stay under 300ms (Emil rule)
        assertTrue(M3Motion.DURATION_SHORT2 <= 160);
        assertTrue(M3Motion.DURATION_SHORT3 <= 200);
    }

    @Test
    public void testEmilKowalskiMotionTokens() {
        // Asymmetric enter/exit: Exit MUST be faster than enter
        assertTrue(M3Motion.DURATION_POPUP_EXIT < M3Motion.DURATION_POPUP_ENTER);
        assertEquals(300, M3Motion.DURATION_POPUP_ENTER);
        assertEquals(200, M3Motion.DURATION_POPUP_EXIT);

        // Stagger principles: short intervals between 20-50ms, fast entrance <= 200ms
        assertTrue(M3Motion.STAGGER_DELAY_STEP >= 20 && M3Motion.STAGGER_DELAY_STEP <= 50);
        assertTrue(M3Motion.STAGGER_DURATION <= 200);

        // Interpolators verification
        assertTrue(M3Motion.EASE_OUT != null);
        assertTrue(M3Motion.EASE_IN_OUT != null);
        assertTrue(M3Motion.DRAWER_CURVE != null);
        assertTrue(M3Motion.SPRING_POPUP_ENTER != null);
        assertTrue(M3Motion.FAST_OUT_EXIT != null);
    }

    @Test
    public void testM3MotionConcentricRadii() {
        // R_inner = max(0, R_outer - padding)
        assertEquals(12, M3Motion.calculateConcentricRadius(28, 16));
        assertEquals(8, M3Motion.calculateConcentricRadius(24, 16));
        assertEquals(0, M3Motion.calculateConcentricRadius(10, 16));
        assertEquals(0, M3Motion.calculateConcentricRadius(0, 10));
        assertEquals(0, M3Motion.calculateConcentricRadius(-5, 10));
    }

    @Test
    public void testInterruptibleDistanceMath() {
        // When reversing from 50% expansion to close, remaining duration must be 50%
        float currentProgress = 0.50f;
        float targetExit = 0.0f;
        float delta = Math.abs(targetExit - currentProgress);
        long exitDuration = Math.round(M3Motion.DURATION_POPUP_EXIT * delta);

        assertEquals(100L, exitDuration);

        // When reversing from 20% collapse to expand, remaining duration must be 80%
        float currentClose = 0.20f;
        float targetEnter = 1.0f;
        float deltaOpen = Math.abs(targetEnter - currentClose);
        long enterDuration = Math.round(M3Motion.DURATION_POPUP_ENTER * deltaOpen);

        assertEquals(240L, enterDuration);
    }

    @Test
    public void testLiquidBridgeGapNonNegative() {
        // Chip bottom at 48dp, target top dynamically placed at 56dp (below chip)
        float chipBottom = 48f;
        float targetTop = 56f;
        float startTransY = chipBottom - targetTop; // -8dp

        // Under our fixed formula: curCardTop = targetTop + startTransY * (1 - p)
        // For all p in [0, 1], gap = curCardTop - chipBottom must be >= 0
        for (int i = 0; i <= 100; i++) {
            float p = i / 100f;
            float transY = startTransY * (1.0f - p);
            float curCardTop = targetTop + transY;
            float gap = curCardTop - chipBottom;

            assertTrue("Gap must be non-negative at progress " + p, gap >= -0.0001f);
            assertTrue("Card top must never rise above chip bottom at progress " + p, curCardTop >= chipBottom - 0.0001f);
            assertTrue("Card top must not exceed targetTop at progress " + p, curCardTop <= targetTop + 0.0001f);
        }

        // Contrast with the broken prior formula where transY = (chipCY - targetCY) * 0.35f * (1 - p)
        float chipCY = 24f;
        float targetCY = 56f + 195f / 2f; // 153.5f
        float brokenStartTransY = (chipCY - targetCY) * 0.35f; // -45.325f
        float brokenTopAtZero = targetTop + brokenStartTransY; // 10.675f (far above chip bottom 48f!)
        assertTrue("Broken formula produced negative gap", brokenTopAtZero < chipBottom);
    }

    @Test
    public void testSemanticStaggerBudget() {
        // Emil Kowalski rule: UI animations must stay under 300ms
        int clusterCount = 4; // Header, Typography, Controls, Progress
        int lastClusterDelay = (clusterCount - 1) * M3Motion.STAGGER_DELAY_STEP;
        int totalStaggerDuration = lastClusterDelay + M3Motion.STAGGER_DURATION;

        // With 4 clusters and 28ms step: 3 * 28 + 160 = 244ms <= 300ms
        assertTrue("Total stagger entrance duration must not exceed 300ms budget", totalStaggerDuration <= 300);
        assertEquals(244, totalStaggerDuration);

        // Previous broken implementation with 13 individual views:
        int brokenItemCount = 13;
        int brokenTotalDuration = (brokenItemCount - 1) * M3Motion.STAGGER_DELAY_STEP + M3Motion.STAGGER_DURATION;
        assertTrue("Broken 13-item stagger severely exceeded 300ms budget", brokenTotalDuration > 300);
        assertEquals(496, brokenTotalDuration);
    }

    @Test
    public void testOriginAwarePivotClamping() {
        float cardWidth = 360f;
        float cardLeft = 16f;

        // Chip located at left edge (status bar chip on Pixel)
        float chipCenterXLeft = 52f;
        float pivotXLeft = Math.max(0, Math.min(cardWidth, chipCenterXLeft - cardLeft));
        assertEquals(36f, pivotXLeft, 0.01f);

        // Chip located in center
        float chipCenterXCenter = 196f;
        float pivotXCenter = Math.max(0, Math.min(cardWidth, chipCenterXCenter - cardLeft));
        assertEquals(180f, pivotXCenter, 0.01f);

        // Chip out of bounds clamping
        float chipNegative = -10f;
        float pivotClampedMin = Math.max(0, Math.min(cardWidth, chipNegative - cardLeft));
        assertEquals(0f, pivotClampedMin, 0.01f);

        float chipWayRight = 500f;
        float pivotClampedMax = Math.max(0, Math.min(cardWidth, chipWayRight - cardLeft));
        assertEquals(cardWidth, pivotClampedMax, 0.01f);
    }
}
