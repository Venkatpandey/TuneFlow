package com.tuneflow.feature.playback

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingNavigationTest {
    @Test
    fun backWithOpenQueue_closesQueue() {
        val action =
            resolveNowPlayingEscapeAction(
                showQueue = true,
                isKeyDown = true,
                keyCode = KeyEvent.KEYCODE_BACK,
            )

        assertEquals(NowPlayingEscapeAction.CloseQueue, action)
    }

    @Test
    fun backWithClosedQueue_propagatesToShell() {
        val action =
            resolveNowPlayingEscapeAction(
                showQueue = false,
                isKeyDown = true,
                keyCode = KeyEvent.KEYCODE_BACK,
            )

        assertEquals(NowPlayingEscapeAction.Propagate, action)
    }

    @Test
    fun queueExitTarget_matchesFocusedQueueRegion() {
        assertEquals(QueueExitTarget.StreamControls, resolveQueueExitTarget(focusedIndex = 1, itemCount = 6))
        assertEquals(QueueExitTarget.TransportControls, resolveQueueExitTarget(focusedIndex = 5, itemCount = 6))
    }
}
