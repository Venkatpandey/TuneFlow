package com.tuneflow.tv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenOnPolicyTest {
    @Test
    fun keepsScreenOnForPlayingOrBufferingAudio() {
        assertTrue(
            shouldKeepScreenOn(
                audioTrackPresent = true,
                audioPlaying = true,
                audioExpectedToPlay = true,
                videoVisible = false,
            ),
        )
        assertTrue(
            shouldKeepScreenOn(
                audioTrackPresent = true,
                audioPlaying = false,
                audioExpectedToPlay = true,
                videoVisible = false,
            ),
        )
    }

    @Test
    fun visibleVideoKeepsScreenOn() {
        assertTrue(
            shouldKeepScreenOn(
                audioTrackPresent = false,
                audioPlaying = false,
                audioExpectedToPlay = false,
                videoVisible = true,
            ),
        )
    }

    @Test
    fun idleOrPausedAudioAllowsSystemScreensaver() {
        assertFalse(
            shouldKeepScreenOn(
                audioTrackPresent = false,
                audioPlaying = false,
                audioExpectedToPlay = true,
                videoVisible = false,
            ),
        )
        assertFalse(
            shouldKeepScreenOn(
                audioTrackPresent = true,
                audioPlaying = false,
                audioExpectedToPlay = false,
                videoVisible = false,
            ),
        )
    }
}
