package com.tuneflow.tv

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoViewportBoundsTest {
    @Test
    fun sixteenByNineContainerIsUnchanged() {
        val container = IntRect(0, 0, 1_920, 1_080)

        assertEquals(container, fitYouTubePlayerBounds(container))
    }

    @Test
    fun wideContainerAddsEqualSideBars() {
        assertEquals(
            IntRect(111, 0, 1_888, 1_000),
            fitYouTubePlayerBounds(IntRect(0, 0, 2_000, 1_000)),
        )
    }

    @Test
    fun tallContainerAddsEqualTopAndBottomBars() {
        assertEquals(
            IntRect(0, 219, 1_000, 781),
            fitYouTubePlayerBounds(IntRect(0, 0, 1_000, 1_000)),
        )
    }

    @Test
    fun fittedBoundsPreserveContainerOffset() {
        assertEquals(
            IntRect(100, 87, 500, 312),
            fitYouTubePlayerBounds(IntRect(100, 50, 500, 350)),
        )
    }
}
