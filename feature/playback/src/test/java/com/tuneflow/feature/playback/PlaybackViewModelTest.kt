package com.tuneflow.feature.playback

import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlaybackStatus
import com.tuneflow.core.player.QueueItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelTest {
    @Test
    fun togglePlayPause_callsPauseWhenPlaying() =
        runTest {
            val fake =
                FakeController(
                    isPlaying = true,
                    queue =
                        PlaybackQueue(
                            items = listOf(QueueItem("1", "Track", "Artist", "Album", streamUrl = "s")),
                        ),
                )

            val vm =
                PlaybackViewModel(
                    fake,
                    positionTicker = flowOf(Unit),
                    scopeOverride = backgroundScope,
                )
            vm.setActive(true)
            runCurrent()
            vm.togglePlayPause()

            assertTrue(fake.pauseCalled)
            assertFalse(fake.playCalled)
        }

    @Test
    fun uiState_reflectsQueue() =
        runTest {
            val fake =
                FakeController(
                    isPlaying = false,
                    queue =
                        PlaybackQueue(
                            items = listOf(QueueItem("1", "Track A", "Artist", "Album", streamUrl = "s")),
                            currentIndex = 0,
                            currentPositionMs = 500L,
                        ),
                )

            val vm =
                PlaybackViewModel(
                    fake,
                    positionTicker = flowOf(Unit),
                    scopeOverride = backgroundScope,
                )
            vm.setActive(true)
            runCurrent()

            assertEquals("1", vm.uiState.value.queue.currentItem?.id)
        }

    @Test
    fun positionTicker_pausesWhenInactive() =
        runTest {
            val fake =
                FakeController(
                    isPlaying = false,
                    queue = PlaybackQueue(),
                )
            var tickerCollected = false
            val ticker =
                flow {
                    tickerCollected = true
                    emit(Unit)
                }

            val vm =
                PlaybackViewModel(
                    fake,
                    positionTicker = ticker,
                    scopeOverride = backgroundScope,
                )
            runCurrent()

            assertFalse(tickerCollected)

            vm.setActive(true)
            runCurrent()

            assertTrue(tickerCollected)
        }
}

private class FakeController(
    isPlaying: Boolean,
    queue: PlaybackQueue,
) : PlaybackController {
    private val queueState = MutableStateFlow(queue)
    private val playingState = MutableStateFlow(isPlaying)
    private val statusState = MutableStateFlow(PlaybackStatus())

    var playCalled = false
    var pauseCalled = false

    override val queue: StateFlow<PlaybackQueue> = queueState
    override val isPlaying: StateFlow<Boolean> = playingState
    override val playbackStatus: StateFlow<PlaybackStatus> = statusState

    override fun play() {
        playCalled = true
        playingState.value = true
    }

    override fun pause() {
        pauseCalled = true
        playingState.value = false
    }

    override fun next() = Unit

    override fun previous() = Unit

    override fun seekTo(positionMs: Long) = Unit

    override fun playFromIndex(index: Int) = Unit

    override fun retryCurrent() = Unit

    override fun stopAndClear() = Unit

    override fun currentPositionMs(): Long = queueState.value.currentPositionMs

    override fun durationMs(): Long = 10_000L
}
