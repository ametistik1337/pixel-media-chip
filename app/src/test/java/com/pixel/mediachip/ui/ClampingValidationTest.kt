package com.pixel.mediachip.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class ClampingValidationTest {

    private fun snapAndClamp(rawVal: Float, min: Float, max: Float, step: Float): Float {
        val snapped = ((rawVal - min) / step).roundToInt() * step + min
        return snapped.coerceIn(min, max)
    }

    @Test
    fun testTitleSizeSp_stepHalfSp() {
        val min = 8.0f
        val max = 14.0f
        val step = 0.5f

        assertEquals(8.0f, snapAndClamp(8.1f, min, max, step), 0.001f)
        assertEquals(8.5f, snapAndClamp(8.4f, min, max, step), 0.001f)
        assertEquals(8.5f, snapAndClamp(8.6f, min, max, step), 0.001f)
        assertEquals(9.0f, snapAndClamp(8.8f, min, max, step), 0.001f)
        assertEquals(14.0f, snapAndClamp(15.5f, min, max, step), 0.001f)
        assertEquals(8.0f, snapAndClamp(6.0f, min, max, step), 0.001f)
    }

    @Test
    fun testMaxWidthDp_stepFiveDp() {
        val min = 60.0f
        val max = 220.0f
        val step = 5.0f

        assertEquals(60.0f, snapAndClamp(62.0f, min, max, step), 0.001f)
        assertEquals(65.0f, snapAndClamp(63.0f, min, max, step), 0.001f)
        assertEquals(105.0f, snapAndClamp(104.9f, min, max, step), 0.001f)
        assertEquals(220.0f, snapAndClamp(230.0f, min, max, step), 0.001f)
        assertEquals(60.0f, snapAndClamp(50.0f, min, max, step), 0.001f)
    }

    @Test
    fun testCameraEatDepthDp_stepTwoDp() {
        val min = 8.0f
        val max = 32.0f
        val step = 2.0f

        assertEquals(8.0f, snapAndClamp(8.4f, min, max, step), 0.001f)
        assertEquals(10.0f, snapAndClamp(9.5f, min, max, step), 0.001f)
        assertEquals(20.0f, snapAndClamp(20.2f, min, max, step), 0.001f)
    }

    @Test
    fun testCornerRadiusDp_stepOneDp() {
        val min = 4.0f
        val max = 24.0f
        val step = 1.0f

        assertEquals(14.0f, snapAndClamp(14.2f, min, max, step), 0.001f)
        assertEquals(16.0f, snapAndClamp(15.8f, min, max, step), 0.001f)
    }

    @Test
    fun testVisualizerSensitivity_stepFivePercent() {
        val min = 50.0f
        val max = 200.0f
        val step = 5.0f

        assertEquals(100.0f, snapAndClamp(102.0f, min, max, step), 0.001f)
        assertEquals(105.0f, snapAndClamp(103.5f, min, max, step), 0.001f)
    }

    @Test
    fun testAntiDoubleBounce_singleHapticTriggerPerStep() {
        val min = 8.0f
        val max = 14.0f
        val step = 0.5f

        var lastHapticStep = ((8.0f - min) / step).roundToInt()
        var hapticCount = 0

        val microWobbleInputs = listOf(8.0f, 8.05f, 8.1f, 8.15f, 8.18f, 8.22f)
        for (input in microWobbleInputs) {
            val clamped = snapAndClamp(input, min, max, step)
            val currentStep = ((clamped - min) / step).roundToInt()
            if (currentStep != lastHapticStep) {
                lastHapticStep = currentStep
                hapticCount++
            }
        }
        assertEquals(0, hapticCount)

        val crossingInputs = listOf(8.3f, 8.5f, 8.6f)
        for (input in crossingInputs) {
            val clamped = snapAndClamp(input, min, max, step)
            val currentStep = ((clamped - min) / step).roundToInt()
            if (currentStep != lastHapticStep) {
                lastHapticStep = currentStep
                hapticCount++
            }
        }
        assertEquals(1, hapticCount)
    }
}
