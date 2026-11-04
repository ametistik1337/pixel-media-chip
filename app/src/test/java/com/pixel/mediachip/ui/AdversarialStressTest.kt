package com.pixel.mediachip.ui

import android.os.Bundle
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.dp
import com.pixel.mediachip.ChipSettings
import com.pixel.mediachip.M3Motion
import com.pixel.mediachip.ui.components.ChipPreset
import com.pixel.mediachip.ui.preview.LivePreviewMath
import com.pixel.mediachip.ui.preview.toPreviewSettings
import com.pixel.mediachip.ui.theme.ExpressiveEffectsSpringDamping
import com.pixel.mediachip.ui.theme.ExpressiveSpatialSpringDamping
import com.pixel.mediachip.ui.theme.calculateConcentricRadius
import com.pixel.mediachip.ui.theme.expressiveEffectsSpring
import com.pixel.mediachip.ui.theme.expressiveFastSpring
import com.pixel.mediachip.ui.theme.expressiveSpatialSpring
import com.pixel.mediachip.ui.viewmodel.ChipSettingsState
import com.pixel.mediachip.ui.viewmodel.toState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Adversarial Stress & Robustness Challenge Test Suite for PixelMediaChip.
 *
 * Authored by Empirical Challenger to stress-test:
 * 1. Extreme boundary inputs (slider snapping, negative padding, excessive eat depths, null bundles/maps).
 * 2. Preset stability (rapid switching, custom overrides, resetToDefaults).
 * 3. Squish transitions and layout modes under all combinations (3x3x2 matrix).
 * 4. Spring parameters (damping ratio in [0.85, 1.0], positive stiffness, no parasitic oscillations).
 * 5. Concurrent state immutability stress under multi-threaded mutations.
 */
class AdversarialStressTest {

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
    // 1. EXTREME BOUNDARY INPUTS & EDGE CASES
    // =========================================================================

    @Test
    fun boundary_sliderSnapping_extremeOverflowAndUnderflow() {
        val testConfigs = listOf(
            Triple(8.0f, 14.0f, 0.5f),   // titleSizeSp
            Triple(6.0f, 12.0f, 0.5f),   // artistSizeSp
            Triple(60.0f, 220.0f, 5.0f), // maxWidthDp / maxTextWidthDp
            Triple(8.0f, 32.0f, 2.0f),   // cameraEatDepthDp
            Triple(4.0f, 24.0f, 1.0f),   // cornerRadiusDp
            Triple(50.0f, 200.0f, 5.0f)  // visualizerSensitivity
        )

        for ((min, max, step) in testConfigs) {
            // Extreme underflow must strictly clamp to min
            for (underflow in listOf(-10000.0f, -500.0f, min - 100.0f, min - 0.001f)) {
                val clamped = snapAndClamp(underflow, min, max, step)
                assertEquals("Underflow $underflow for range [$min, $max] must clamp to $min", min, clamped, 0.0001f)
            }

            // Extreme overflow must strictly clamp to max
            for (overflow in listOf(max + 0.001f, max + 100.0f, 5000.0f, 100000.0f)) {
                val clamped = snapAndClamp(overflow, min, max, step)
                assertEquals("Overflow $overflow for range [$min, $max] must clamp to $max", max, clamped, 0.0001f)
            }

            // Boundary snapping: min + epsilon should stay at min
            assertEquals(min, snapAndClamp(min + step * 0.2f, min, max, step), 0.0001f)
            // min + step * 0.6f should snap to min + step
            assertEquals(min + step, snapAndClamp(min + step * 0.6f, min, max, step), 0.0001f)
            // max - step * 0.2f should stay at max
            assertEquals(max, snapAndClamp(max - step * 0.2f, min, max, step), 0.0001f)
            // max - step * 0.6f should snap to max - step
            assertEquals(max - step, snapAndClamp(max - step * 0.6f, min, max, step), 0.0001f)
        }
    }

    @Test
    fun boundary_sliderSnapping_fuzzRandomInputsWithinAndOutsideRange() {
        val min = 60.0f
        val max = 220.0f
        val step = 5.0f
        val rng = Random(42)

        for (i in 0 until 10_000) {
            val raw = rng.nextFloat() * 400.0f - 50.0f // [-50.0, 350.0]
            val clamped = snapAndClamp(raw, min, max, step)

            assertTrue("Clamped value $clamped must be >= min $min", clamped >= min)
            assertTrue("Clamped value $clamped must be <= max $max", clamped <= max)

            // Step alignment check
            val stepOffset = clamped - min
            val stepMultiple = (stepOffset / step).roundToInt()
            assertEquals("Clamped $clamped must align to step $step from $min", stepOffset, stepMultiple * step, 0.001f)
        }
    }

    @Test
    fun boundary_negativeRadiusAndPadding_concentricMath() {
        // Kotlin Compose calculateConcentricRadius
        // R_inner = max(0, R_outer - padding)
        assertEquals(0.dp, calculateConcentricRadius(0.dp, 10.dp))
        assertEquals(0.dp, calculateConcentricRadius((-5).dp, 10.dp))
        assertEquals(0.dp, calculateConcentricRadius(10.dp, 15.dp))
        assertEquals(0.dp, calculateConcentricRadius(10.dp, 10.dp))
        assertEquals(26.dp, calculateConcentricRadius(16.dp, (-10).dp))

        // Java M3Motion parity
        assertEquals(0, M3Motion.calculateConcentricRadius(0, 10))
        assertEquals(0, M3Motion.calculateConcentricRadius(-5, 10))
        assertEquals(0, M3Motion.calculateConcentricRadius(10, 15))
        assertEquals(0, M3Motion.calculateConcentricRadius(10, 10))
        assertEquals(26, M3Motion.calculateConcentricRadius(16, -10))
    }

    @Test
    fun boundary_excessiveCameraEatDepths_safetyCalculations() {
        // Excessive camera depth (e.g. 1000dp or -100dp)
        val excessiveDepth = 1000
        val safePadding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = excessiveDepth,
            basePaddingDp = 6
        )
        assertEquals(1006, safePadding)

        // When eatCamera is false, excessive camera depth MUST be ignored
        val ignoredPadding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = false,
            cameraEatDepthDp = excessiveDepth,
            basePaddingDp = 6
        )
        assertEquals(6, ignoredPadding)

        // Negative depth must clamp safely to 0 + base
        val negativePadding = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = -500,
            basePaddingDp = 6
        )
        assertEquals(6, negativePadding)

        // Negative base padding must also clamp safely to 0
        val negativeBase = LivePreviewMath.calculateCameraSafeTextPadding(
            eatCamera = true,
            cameraEatDepthDp = 16,
            basePaddingDp = -10
        )
        assertEquals(16, negativeBase)

        // Width expansion with excessive camera depth
        val expandedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 105,
            maxTextWidthDp = 105,
            eatCamera = true,
            cameraEatDepthDp = excessiveDepth,
            isSquished = false,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(1105, expandedWidth)

        // BUT when squished to icon, even excessive eat depth MUST compress strictly to 28dp
        val squishedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 105,
            maxTextWidthDp = 105,
            eatCamera = true,
            cameraEatDepthDp = excessiveDepth,
            isSquished = true,
            squishMode = ChipSettings.SQUISH_TO_ICON
        )
        assertEquals(28, squishedWidth)

        // And when squished to hide, MUST compress to 0dp
        val squishedHideWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
            layoutMode = ChipSettings.MODE_TWO_LINES,
            maxWidthDp = 105,
            maxTextWidthDp = 105,
            eatCamera = true,
            cameraEatDepthDp = excessiveDepth,
            isSquished = true,
            squishMode = ChipSettings.SQUISH_HIDE
        )
        assertEquals(0, squishedHideWidth)
    }

    @Test
    fun boundary_nullAndEmptyBundleMapHandling_noCorruptions() {
        val fresh = ChipSettings()
        fresh.layoutMode = ChipSettings.MODE_ONE_LINE
        fresh.maxWidthDp = 150
        fresh.titleSizeSp = 12.5f

        // Null checks
        fresh.fromBundle(null)
        assertEquals(ChipSettings.MODE_ONE_LINE, fresh.layoutMode)
        assertEquals(150, fresh.maxWidthDp)
        assertEquals(12.5f, fresh.titleSizeSp, 0.001f)

        fresh.fromMap(null)
        assertEquals(ChipSettings.MODE_ONE_LINE, fresh.layoutMode)
        assertEquals(150, fresh.maxWidthDp)
        assertEquals(12.5f, fresh.titleSizeSp, 0.001f)

        fresh.copyFrom(null)
        assertEquals(ChipSettings.MODE_ONE_LINE, fresh.layoutMode)
        assertEquals(150, fresh.maxWidthDp)
        assertEquals(12.5f, fresh.titleSizeSp, 0.001f)

        // Empty bundle / map
        fresh.fromBundle(Bundle())
        assertEquals(ChipSettings.MODE_ONE_LINE, fresh.layoutMode)
        assertEquals(150, fresh.maxWidthDp)

        fresh.fromMap(emptyMap())
        assertEquals(ChipSettings.MODE_ONE_LINE, fresh.layoutMode)
        assertEquals(150, fresh.maxWidthDp)

        // Partial map update
        val partialMap = mapOf<String, Any>(
            ChipSettings.KEY_MAX_WIDTH_DP to 195,
            ChipSettings.KEY_EAT_CAMERA to true
        )
        fresh.fromMap(partialMap)
        assertEquals(195, fresh.maxWidthDp)
        assertTrue(fresh.eatCamera)
        // Untouched keys must remain intact
        assertEquals(ChipSettings.MODE_ONE_LINE, fresh.layoutMode)
        assertEquals(12.5f, fresh.titleSizeSp, 0.001f)
    }

    // =========================================================================
    // 2. PRESET STABILITY & RECOVERY HARNESS
    // =========================================================================

    @Test
    fun preset_rapidSequentialSwitchingCycle_1000Iterations() {
        val state = ChipSettingsState()
        var current = state

        val presets = listOf(
            ChipPreset.PIXEL_DEFAULT,
            ChipPreset.DYNAMIC_ISLAND,
            ChipPreset.COMPACT,
            ChipPreset.EXPANDED
        )

        for (i in 0 until 1000) {
            val preset = presets[i % presets.size]
            current = current.applyPreset(preset)

            when (preset) {
                ChipPreset.PIXEL_DEFAULT -> {
                    assertEquals(ChipSettings.MODE_TWO_LINES, current.layoutMode)
                    assertFalse(current.eatCamera)
                    assertEquals(105, current.maxWidthDp)
                }
                ChipPreset.DYNAMIC_ISLAND -> {
                    assertEquals(ChipSettings.MODE_ONE_LINE, current.layoutMode)
                    assertTrue(current.eatCamera)
                    assertEquals(125, current.maxTextWidthDp)
                    assertEquals(20, current.cameraEatDepthDp)
                    assertEquals(16, current.cornerRadiusDp)
                    assertEquals(10.5f, current.titleSizeSp, 0.001f)
                }
                ChipPreset.COMPACT -> {
                    assertEquals(ChipSettings.MODE_ICON_ONLY, current.layoutMode)
                    assertFalse(current.eatCamera)
                    assertEquals(ChipSettings.SQUISH_DISABLED, current.squishMode)
                }
                ChipPreset.EXPANDED -> {
                    assertEquals(ChipSettings.MODE_TWO_LINES, current.layoutMode)
                    assertFalse(current.eatCamera)
                    assertEquals(180, current.maxWidthDp)
                    assertEquals(11.0f, current.titleSizeSp, 0.001f)
                    assertEquals(8.5f, current.artistSizeSp, 0.001f)
                }
            }
        }

        // Final reset must strictly restore all 17 default parameters
        val restored = current.resetToDefaults()
        assertEquals(ChipSettingsState(), restored)
    }

    @Test
    fun preset_customOverridesFollowedByPresetThenReset() {
        // 1. Start with arbitrary dirty overrides
        val custom = ChipSettingsState(
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
            maxWidthDp = 215,
            maxTextWidthDp = 210,
            cameraEatDepthDp = 30,
            titleSizeSp = 13.5f,
            artistSizeSp = 11.5f,
            cornerRadiusDp = 22
        )

        // 2. Apply Dynamic Island preset on top of custom
        val island = custom.applyPreset(ChipPreset.DYNAMIC_ISLAND)
        assertEquals(ChipSettings.MODE_ONE_LINE, island.layoutMode)
        assertTrue(island.eatCamera)
        assertEquals(125, island.maxTextWidthDp)
        assertEquals(20, island.cameraEatDepthDp)
        assertEquals(16, island.cornerRadiusDp)
        assertEquals(10.5f, island.titleSizeSp, 0.001f)
        // User flags not overridden by dynamic island should remain intact
        assertFalse(island.monetTint)
        assertFalse(island.hapticEnabled)

        // 3. Reset to defaults
        val reset = island.resetToDefaults()
        assertEquals(ChipSettingsState(), reset)
    }

    // =========================================================================
    // 3. SQUISH TRANSITIONS & LAYOUT MODES MATRIX (3x3x2 COMBINATIONS)
    // =========================================================================

    @Test
    fun matrix_squishTransitionsAndLayoutModes_all18Combinations() {
        val squishModes = listOf(
            ChipSettings.SQUISH_TO_ICON,
            ChipSettings.SQUISH_HIDE,
            ChipSettings.SQUISH_DISABLED
        )
        val layoutModes = listOf(
            ChipSettings.MODE_TWO_LINES,
            ChipSettings.MODE_ONE_LINE,
            ChipSettings.MODE_ICON_ONLY
        )
        val eatCameraOptions = listOf(false, true)

        val maxWidth = 120
        val maxTextWidth = 130
        val cameraEatDepth = 18

        for (squishMode in squishModes) {
            for (layoutMode in layoutModes) {
                for (eatCamera in eatCameraOptions) {
                    val extraEat = if (eatCamera) cameraEatDepth else 0
                    val expectedNormalWidth = when (layoutMode) {
                        ChipSettings.MODE_ICON_ONLY -> 28
                        ChipSettings.MODE_ONE_LINE -> maxOf(28, maxTextWidth + extraEat)
                        ChipSettings.MODE_TWO_LINES -> maxOf(28, maxWidth + extraEat)
                        else -> 28
                    }

                    // --- Test Unsquished State (isSquished = false) ---
                    val normalWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
                        layoutMode = layoutMode,
                        maxWidthDp = maxWidth,
                        maxTextWidthDp = maxTextWidth,
                        eatCamera = eatCamera,
                        cameraEatDepthDp = cameraEatDepth,
                        isSquished = false,
                        squishMode = squishMode
                    )
                    assertEquals(
                        "Unsquished width mismatch for squishMode=$squishMode, layoutMode=$layoutMode, eatCamera=$eatCamera",
                        expectedNormalWidth,
                        normalWidth
                    )
                    assertTrue(
                        "Unsquished chip must always be visible",
                        LivePreviewMath.isChipVisibleWhenSquished(false, squishMode)
                    )

                    // --- Test Squished State (isSquished = true) ---
                    val squishedWidth = LivePreviewMath.calculateEffectiveChipWidthDp(
                        layoutMode = layoutMode,
                        maxWidthDp = maxWidth,
                        maxTextWidthDp = maxTextWidth,
                        eatCamera = eatCamera,
                        cameraEatDepthDp = cameraEatDepth,
                        isSquished = true,
                        squishMode = squishMode
                    )

                    when (squishMode) {
                        ChipSettings.SQUISH_TO_ICON -> {
                            assertEquals(
                                "SQUISH_TO_ICON must compress to 28dp for layout=$layoutMode, eatCamera=$eatCamera",
                                28,
                                squishedWidth
                            )
                            assertTrue(LivePreviewMath.isChipVisibleWhenSquished(true, squishMode))
                            assertTrue(LivePreviewMath.isChipInIconOnlyState(layoutMode, true, squishMode))
                        }
                        ChipSettings.SQUISH_HIDE -> {
                            assertEquals(
                                "SQUISH_HIDE must compress to 0dp for layout=$layoutMode, eatCamera=$eatCamera",
                                0,
                                squishedWidth
                            )
                            assertFalse(LivePreviewMath.isChipVisibleWhenSquished(true, squishMode))
                        }
                        ChipSettings.SQUISH_DISABLED -> {
                            assertEquals(
                                "SQUISH_DISABLED must retain normal width for layout=$layoutMode, eatCamera=$eatCamera",
                                expectedNormalWidth,
                                squishedWidth
                            )
                            assertTrue(LivePreviewMath.isChipVisibleWhenSquished(true, squishMode))
                            assertEquals(
                                layoutMode == ChipSettings.MODE_ICON_ONLY,
                                LivePreviewMath.isChipInIconOnlyState(layoutMode, true, squishMode)
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. SPRING PHYSICS & DAMPING AUDIT
    // =========================================================================

    @Test
    fun spring_verifyDampingRatioStrictlyBetween85And100Percent() {
        // 1. Verify constant tokens
        assertTrue(
            "Spatial spring damping ratio $ExpressiveSpatialSpringDamping must be >= 0.85f",
            ExpressiveSpatialSpringDamping >= 0.85f
        )
        assertTrue(
            "Spatial spring damping ratio $ExpressiveSpatialSpringDamping must be <= 1.0f",
            ExpressiveSpatialSpringDamping <= 1.0f
        )

        assertTrue(
            "Effects spring damping ratio $ExpressiveEffectsSpringDamping must be >= 0.85f",
            ExpressiveEffectsSpringDamping >= 0.85f
        )
        assertTrue(
            "Effects spring damping ratio $ExpressiveEffectsSpringDamping must be <= 1.0f",
            ExpressiveEffectsSpringDamping <= 1.0f
        )

        // 2. Verify expressiveSpatialSpring spec builder
        val spatialSpec = expressiveSpatialSpring<Float>()
        assertTrue("Spatial spring damping ratio must be >= 0.85f", spatialSpec.dampingRatio >= 0.85f)
        assertTrue("Spatial spring damping ratio must be <= 1.0f", spatialSpec.dampingRatio <= 1.0f)
        assertTrue("Spatial spring stiffness must be positive", spatialSpec.stiffness > 0f)

        // 3. Verify expressiveEffectsSpring spec builder
        val effectsSpec = expressiveEffectsSpring<Float>()
        assertTrue("Effects spring damping ratio must be >= 0.85f", effectsSpec.dampingRatio >= 0.85f)
        assertTrue("Effects spring damping ratio must be <= 1.0f", effectsSpec.dampingRatio <= 1.0f)
        assertTrue("Effects spring stiffness must be positive", effectsSpec.stiffness > 0f)

        // 4. Verify expressiveFastSpring spec builder
        val fastSpec = expressiveFastSpring<Float>()
        assertTrue("Fast spring damping ratio must be >= 0.85f", fastSpec.dampingRatio >= 0.85f)
        assertTrue("Fast spring damping ratio must be <= 1.0f", fastSpec.dampingRatio <= 1.0f)
        assertTrue("Fast spring stiffness must be positive", fastSpec.stiffness > 0f)

        // 5. Verify no negative damping anywhere
        assertFalse("Damping ratio cannot be negative", spatialSpec.dampingRatio < 0f)
        assertFalse("Damping ratio cannot be negative", effectsSpec.dampingRatio < 0f)
        assertFalse("Damping ratio cannot be negative", fastSpec.dampingRatio < 0f)
    }

    // =========================================================================
    // 5. CONCURRENT IMMUTABILITY STRESS
    // =========================================================================

    @Test
    fun concurrency_settingsStateImmutabilityStress() {
        val baseState = ChipSettingsState()
        val executor = Executors.newFixedThreadPool(8)
        val iterations = 500
        val tasks = mutableListOf<Callable<Boolean>>()

        for (i in 0 until 8) {
            tasks.add(Callable {
                var localState = baseState
                for (j in 0 until iterations) {
                    val preset = when (j % 4) {
                        0 -> ChipPreset.PIXEL_DEFAULT
                        1 -> ChipPreset.DYNAMIC_ISLAND
                        2 -> ChipPreset.COMPACT
                        else -> ChipPreset.EXPANDED
                    }
                    localState = localState.applyPreset(preset)
                    val preview = localState.toPreviewSettings()
                    assertNotNull(preview)
                    assertTrue(preview.maxWidthDp in 60..220)
                }
                true
            })
        }

        val futures = executor.invokeAll(tasks)
        executor.shutdown()
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))

        for (future in futures) {
            assertTrue("Thread execution must succeed without race conditions", future.get())
        }

        // Base state must have remained completely unmodified
        assertEquals(ChipSettingsState(), baseState)
    }
}
