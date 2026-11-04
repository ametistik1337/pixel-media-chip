package com.pixel.mediachip.ui

import androidx.compose.ui.unit.dp
import com.pixel.mediachip.ui.theme.calculateConcentricRadius
import org.junit.Assert.assertEquals
import org.junit.Test

class ConcentricRadiusTest {

    @Test
    fun testConcentricRadius_normalPadding() {
        val outer = 28.dp
        val padding = 16.dp
        val inner = calculateConcentricRadius(outer, padding)
        assertEquals(12.dp, inner)
    }

    @Test
    fun testConcentricRadius_settingsCardPadding() {
        val outer = 16.dp
        val padding = 12.dp
        val inner = calculateConcentricRadius(outer, padding)
        assertEquals(4.dp, inner)
    }

    @Test
    fun testConcentricRadius_paddingExceedsOuterRadius_clampsToZero() {
        val outer = 16.dp
        val padding = 20.dp
        val inner = calculateConcentricRadius(outer, padding)
        assertEquals(0.dp, inner)
    }

    @Test
    fun testConcentricRadius_zeroPadding_equalsOuter() {
        val outer = 16.dp
        val padding = 0.dp
        val inner = calculateConcentricRadius(outer, padding)
        assertEquals(16.dp, inner)
    }

    @Test
    fun testConcentricRadius_exactMatchPadding_yieldsZero() {
        val outer = 16.dp
        val padding = 16.dp
        val inner = calculateConcentricRadius(outer, padding)
        assertEquals(0.dp, inner)
    }
}
