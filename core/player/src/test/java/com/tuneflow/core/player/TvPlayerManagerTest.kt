package com.tuneflow.core.player

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPlayerManagerTest {
    @Test
    fun `persists only for seek discontinuities`() {
        assertTrue(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_SEEK))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_AUTO_TRANSITION))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_REMOVE))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_SKIP))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_INTERNAL))
    }
}
