package com.tuneflow.core.design

import org.junit.Assert.assertEquals
import org.junit.Test

class TuneFlowMotionTest {
    @Test
    fun cardsKeepFixedBoundsWhenFocused() {
        assertEquals(1f, TuneFlowMotion().cardFocusScale, 0f)
    }
}
