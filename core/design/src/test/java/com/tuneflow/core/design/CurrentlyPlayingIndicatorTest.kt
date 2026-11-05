package com.tuneflow.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentlyPlayingIndicatorTest {
    @Test
    fun defaultBarCountIsThree() {
        assertEquals(3, CurrentlyPlayingIndicatorDefaults.DEFAULT_BAR_COUNT)
    }

    @Test
    fun barFractionsAreWithinValidRange() {
        val minList = CurrentlyPlayingIndicatorDefaults.BarMinFractions
        val maxList = CurrentlyPlayingIndicatorDefaults.BarMaxFractions

        assertEquals(minList.size, maxList.size)
        for (i in minList.indices) {
            val min = minList[i]
            val max = maxList[i]
            assertTrue("Min fraction must be >= 0f", min >= 0f)
            assertTrue("Max fraction must be <= 1f", max <= 1f)
            assertTrue("Min fraction must be less than max fraction", min < max)
        }
    }

    @Test
    fun restingFractionsAreWithinValidRange() {
        for (fraction in CurrentlyPlayingIndicatorDefaults.RestingFractions) {
            assertTrue("Resting fraction must be between 0f and 1f", fraction in 0f..1f)
        }
    }

    @Test
    fun durationsArePositive() {
        for (duration in CurrentlyPlayingIndicatorDefaults.BarDurations) {
            assertTrue("Animation duration must be positive", duration > 0)
        }
    }

    @Test
    fun barWidthAndSpacingFitTotalWidthExactly() {
        val totalWidth = 60f
        val barCount = 3
        val spacingRatio = 0.6f

        val barWidth = CurrentlyPlayingIndicatorDefaults.calculateBarWidth(totalWidth, barCount, spacingRatio)
        val spacing = CurrentlyPlayingIndicatorDefaults.calculateSpacing(barWidth, spacingRatio)

        val totalUsedWidth = barWidth * barCount + spacing * (barCount - 1)
        assertEquals(totalWidth, totalUsedWidth, 0.001f)
    }

    @Test
    fun zeroOrNegativeDimensionsHandledSafely() {
        assertEquals(0f, CurrentlyPlayingIndicatorDefaults.calculateBarWidth(0f, 3), 0.001f)
        assertEquals(0f, CurrentlyPlayingIndicatorDefaults.calculateBarWidth(-10f, 3), 0.001f)
        assertEquals(0f, CurrentlyPlayingIndicatorDefaults.calculateBarWidth(60f, 0), 0.001f)
        assertEquals(0f, CurrentlyPlayingIndicatorDefaults.calculateBarWidth(60f, -1), 0.001f)

        assertEquals(0f, CurrentlyPlayingIndicatorDefaults.calculateBarHeight(0f, 0.5f, 10f), 0.001f)
        assertEquals(0f, CurrentlyPlayingIndicatorDefaults.calculateBarHeight(-20f, 0.5f, 10f), 0.001f)
    }

    @Test
    fun barHeightCoercionEnsuresMinimumAndMaximum() {
        val totalHeight = 100f
        val minHeight = 15f

        // Very low fraction should clamp to minHeight
        val clampedLow = CurrentlyPlayingIndicatorDefaults.calculateBarHeight(totalHeight, 0.05f, minHeight)
        assertEquals(minHeight, clampedLow, 0.001f)

        // Normal fraction should be exact
        val normal = CurrentlyPlayingIndicatorDefaults.calculateBarHeight(totalHeight, 0.5f, minHeight)
        assertEquals(50f, normal, 0.001f)

        // Oversized fraction should clamp to totalHeight
        val clampedHigh = CurrentlyPlayingIndicatorDefaults.calculateBarHeight(totalHeight, 1.5f, minHeight)
        assertEquals(totalHeight, clampedHigh, 0.001f)
    }
}
