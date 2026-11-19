package com.tuneflow.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneFlowMotionTest {
    @Test
    fun cardsKeepFixedBoundsWhenFocused() {
        assertEquals(1f, TuneFlowMotion().cardFocusScale, 0f)
    }

    @Test
    fun reducedMotion_disablesAnimationsAndZerosDurations() {
        val reduced = TuneFlowMotion.reduced()
        assertFalse(reduced.enabled)
        assertEquals(0, reduced.focusGainDurationMs)
        assertEquals(0, reduced.focusLossDurationMs)
        assertEquals(0, reduced.screenForwardDurationMs)
        assertEquals(0, reduced.screenBackDurationMs)
        assertEquals(0, reduced.nowPlayingOpenDurationMs)
        assertEquals(0, reduced.nowPlayingCloseDurationMs)
        assertEquals(0, reduced.dialogDurationMs)
        assertEquals(1f, reduced.cardFocusScale, 0f)
        assertEquals(1f, reduced.buttonFocusScale, 0f)
        assertEquals(1f, reduced.fieldFocusScale, 0f)
    }

    @Test
    fun defaultMotion_hasAnimationsEnabled() {
        val motion = TuneFlowMotion()
        assertTrue(motion.enabled)
        assertTrue(motion.focusGainDurationMs > 0)
        assertEquals(1.05f, motion.buttonFocusScale, 0f)
    }

    @Test
    fun reducedMotionMode_resolution() {
        assertEquals(ReducedMotionMode.System, ReducedMotionMode.fromId("system"))
        assertEquals(ReducedMotionMode.Always, ReducedMotionMode.fromId("always"))
        assertEquals(ReducedMotionMode.Never, ReducedMotionMode.fromId("never"))
        assertEquals(ReducedMotionMode.Always, ReducedMotionMode.fromId("ALWAYS"))
        assertEquals(ReducedMotionMode.System, ReducedMotionMode.fromId(null))
        assertEquals(ReducedMotionMode.System, ReducedMotionMode.fromId("invalid"))
    }
}
