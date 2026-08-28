package com.tuneflow.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackScreensaverControllerTest {
    @Test
    fun entersExactlyAtSixtySecondsOnlyWhenPlaybackEligible() {
        val clock = FakeMonotonicClock()
        val controller = PlaybackScreensaverController(clock)
        controller.onPlaybackEligibilityChanged(true)

        clock.now = 59_999L
        controller.onDeadlineReached()
        assertFalse(controller.state.value.active)

        clock.now = 60_000L
        controller.onDeadlineReached()
        assertTrue(controller.state.value.active)

        controller.onPlaybackEligibilityChanged(false)
        clock.now = 120_000L
        controller.onDeadlineReached()
        assertFalse(controller.state.value.active)
    }

    @Test
    fun eachInputCategoryResetsTimerAndDismissesImmediately() {
        UserInputCategory.entries.forEach { category ->
            val clock = FakeMonotonicClock()
            val controller = PlaybackScreensaverController(clock)
            controller.onPlaybackEligibilityChanged(true)
            clock.now = 60_000L
            controller.onDeadlineReached()
            assertTrue(controller.state.value.active)

            clock.now += 1L
            controller.onUserActivity(category)

            assertFalse(controller.state.value.active)
            assertEquals(clock.now, controller.state.value.lastUserActivityMs)
        }
    }

    @Test
    fun playbackUpdatesAndAutomaticTrackChangesDoNotResetTimer() {
        val clock = FakeMonotonicClock()
        val controller = PlaybackScreensaverController(clock)
        controller.onPlaybackEligibilityChanged(true)
        val startedAt = controller.state.value.lastUserActivityMs

        clock.now = 30_000L
        controller.onPlaybackEligibilityChanged(true)
        assertEquals(startedAt, controller.state.value.lastUserActivityMs)

        clock.now = 60_000L
        controller.onDeadlineReached()
        assertTrue(controller.state.value.active)

        clock.now = 61_000L
        controller.onPlaybackEligibilityChanged(true)
        assertTrue(controller.state.value.active)
        assertEquals(startedAt, controller.state.value.lastUserActivityMs)
    }

    @Test
    fun pauseStopSignOutOrEmptyQueueDismissesImmediately() {
        val clock = FakeMonotonicClock()
        val controller = PlaybackScreensaverController(clock)

        repeat(4) {
            controller.onPlaybackEligibilityChanged(true)
            clock.now += 60_000L
            controller.onDeadlineReached()
            assertTrue(controller.state.value.active)

            controller.onPlaybackEligibilityChanged(false)
            assertFalse(controller.state.value.active)
        }
    }

    @Test
    fun remainingDelayUsesMonotonicTime() {
        val clock = FakeMonotonicClock(now = 10_000L)
        val controller = PlaybackScreensaverController(clock)
        controller.onPlaybackEligibilityChanged(true)
        clock.now = 35_000L

        assertEquals(35_000L, controller.remainingDelayMs())
    }
}

private class FakeMonotonicClock(var now: Long = 0L) : MonotonicClock {
    override fun nowMs(): Long = now
}
