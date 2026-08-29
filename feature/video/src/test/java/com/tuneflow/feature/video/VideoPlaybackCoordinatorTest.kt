package com.tuneflow.feature.video

import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackMode
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlaybackStatus
import com.tuneflow.core.player.QueueItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackCoordinatorTest {
    @Test
    fun handoffPausesAudioThenRestoresVideoPositionAndPlayingState() {
        val audio = FakePlaybackController(positionMs = 20_000L, playing = true)
        val coordinator = VideoPlaybackCoordinator(audio)

        assertEquals(20_000L, coordinator.startVideo("track"))
        assertTrue(audio.pauseCalls == 1)

        coordinator.returnToAudio(providerPositionMs = 40_000L, resumeAudio = true)

        assertEquals(40_000L, audio.seekPositionMs)
        assertTrue(audio.isPlaying.value)
        assertEquals(1, audio.playCalls)
        assertEquals(PlaybackMode.Loop, audio.playbackMode.value)
    }

    @Test
    fun restoreIsExactlyOnceAndClampsToAudioDuration() {
        val audio = FakePlaybackController(positionMs = 20_000L, playing = false)
        val coordinator = VideoPlaybackCoordinator(audio)

        coordinator.startVideo("track")
        coordinator.returnToAudio(providerPositionMs = 999_000L, resumeAudio = false)
        coordinator.returnToAudio(providerPositionMs = 10_000L, resumeAudio = true)

        assertEquals(180_000L, audio.seekPositionMs)
        assertEquals(0, audio.playCalls)
        assertFalse(audio.isPlaying.value)
    }

    @Test
    fun stopBeforeVideoHandoffStillPausesAudio() {
        val audio = FakePlaybackController(positionMs = 20_000L, playing = true)
        val coordinator = VideoPlaybackCoordinator(audio)

        coordinator.returnToAudio(providerPositionMs = null, resumeAudio = false)

        assertFalse(audio.isPlaying.value)
        assertEquals(1, audio.pauseCalls)
        assertEquals(0, audio.playCalls)
    }
}

private class FakePlaybackController(
    positionMs: Long,
    playing: Boolean,
) : PlaybackController {
    private val queueState =
        MutableStateFlow(
            PlaybackQueue(
                items =
                    listOf(
                        QueueItem(
                            id = "track",
                            title = "Track",
                            artist = "Artist",
                            album = "Album",
                            streamUrl = "stream",
                            durationMs = 180_000L,
                        ),
                    ),
            ),
        )
    private val playingState = MutableStateFlow(playing)
    private val statusState = MutableStateFlow(PlaybackStatus())
    private val modeState = MutableStateFlow(PlaybackMode.Loop)
    private var currentPosition = positionMs

    var pauseCalls = 0
    var playCalls = 0
    var seekPositionMs: Long? = null

    override val queue: StateFlow<PlaybackQueue> = queueState
    override val isPlaying: StateFlow<Boolean> = playingState
    override val playbackStatus: StateFlow<PlaybackStatus> = statusState
    override val playbackMode: StateFlow<PlaybackMode> = modeState

    override fun play() {
        playCalls += 1
        playingState.value = true
    }

    override fun pause() {
        pauseCalls += 1
        playingState.value = false
    }

    override fun next() = Unit

    override fun previous() = Unit

    override fun seekTo(positionMs: Long) {
        currentPosition = positionMs
        seekPositionMs = positionMs
    }

    override fun playFromIndex(index: Int) = Unit

    override fun retryCurrent() = Unit

    override fun stopAndClear() = Unit

    override fun currentPositionMs(): Long = currentPosition

    override fun durationMs(): Long = 180_000L

    override fun cyclePlaybackMode() = Unit
}
