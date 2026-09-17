package com.tuneflow.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class CinematicPlaybackScreensaverTest {
    @Test
    fun burnInOffset_movesOncePerMinuteAcrossFourPositions() {
        assertEquals(AmbientBurnInOffset(-8, -4), ambientBurnInOffset(0L))
        assertEquals(AmbientBurnInOffset(8, -4), ambientBurnInOffset(60_000L))
        assertEquals(AmbientBurnInOffset(8, 4), ambientBurnInOffset(120_000L))
        assertEquals(AmbientBurnInOffset(-8, 4), ambientBurnInOffset(180_000L))
        assertEquals(AmbientBurnInOffset(-8, -4), ambientBurnInOffset(240_000L))
    }

    @Test
    fun burnInOffset_clampsNegativePosition() {
        assertEquals(AmbientBurnInOffset(-8, -4), ambientBurnInOffset(-1L))
    }
}
