package com.tuneflow.feature.video

import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackMode
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlaybackStatus
import com.tuneflow.core.player.QueueItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {
    @Test
    fun explicitRequestKeepsAudioPlayingWhileSearchRuns() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 1_000L)
            val viewModel = createViewModel(audio, provider, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()

            assertTrue(viewModel.uiState.value is VideoUiState.Searching)
            assertTrue(audio.isPlaying.value)
            advanceTimeBy(1_000L)
            runCurrent()

            assertTrue(viewModel.uiState.value is VideoUiState.Candidates)
            assertEquals(0, audio.pauseCalls)
        }

    @Test
    fun trackChangeCancelsStaleSearchAndReturnsToIdle() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 10_000L)
            val viewModel = createViewModel(audio, provider, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            audio.replaceTrack("new-track")
            runCurrent()

            assertTrue(provider.cancelled)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
        }

    @Test
    fun searchAlwaysRequiresManualSelectionAndReturnsUpToTwentyFiveMatches() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 30)
            val viewModel = createViewModel(audio, provider, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()

            val state = viewModel.uiState.value as VideoUiState.Candidates
            assertEquals(25, state.candidates.size)
            assertEquals(0, audio.pauseCalls)
        }

    @Test
    fun selectedCandidateLoadsFullscreenAndBackMinimizesWithoutClosing() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 1)
            val viewModel = createViewModel(audio, provider, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            val candidate = (viewModel.uiState.value as VideoUiState.Candidates).candidates.single()
            viewModel.selectCandidate(candidate)

            val loading = viewModel.uiState.value as VideoUiState.Loading
            assertEquals(VideoPresentationMode.Fullscreen, loading.presentation)

            viewModel.exitFullscreen()

            val minimized = viewModel.uiState.value as VideoUiState.Loading
            assertEquals(VideoPresentationMode.Mini, minimized.presentation)
            assertEquals(1L, minimized.focusRequestId)
        }

    @Test
    fun stoppingVideoLeavesAudioPausedAndRequestsNowPlaying() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 1)
            val viewModel = createViewModel(audio, provider, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            val candidate = (viewModel.uiState.value as VideoUiState.Candidates).candidates.single()
            viewModel.selectCandidate(candidate)
            viewModel.stopVideo()

            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
            assertFalse(audio.isPlaying.value)
            assertEquals(0, audio.playCalls)
            assertEquals(Unit, viewModel.returnToNowPlayingEvents.first())
        }

    private fun createViewModel(
        audio: VideoViewModelFakeAudio,
        provider: VideoProvider,
        scope: CoroutineScope,
    ): VideoViewModel =
        VideoViewModel(
            providers = VideoProviderRegistry(listOf(provider)),
            audio = audio,
            youtubePlayer = YouTubeEmbeddedPlayer(),
            coordinator = VideoPlaybackCoordinator(audio),
            consentStore = AcceptedConsentStore,
            scopeOverride = scope,
        )
}

private class FakeVideoProvider(
    private val searchDelayMs: Long,
    private val resultCount: Int = 2,
) : VideoProvider {
    var cancelled = false

    override val id = VideoProviderId.YouTube
    override val configured = true
    override val capabilities = VideoProviderCapabilities(supportsSeeking = true, usesAdaptiveQuality = true)

    override suspend fun search(query: VideoTrackQuery): List<VideoCandidate> {
        try {
            delay(searchDelayMs)
        } catch (error: CancellationException) {
            cancelled = true
            throw error
        }
        return List(resultCount) { index -> candidate(index.toString()) }
    }

    override fun createPlayerSpec(candidate: VideoCandidate) = EmbeddedVideoPlayerSpec(id, candidate.videoId)

    private fun candidate(id: String) =
        VideoCandidate(
            providerId = VideoProviderId.YouTube,
            videoId = id,
            title = "Artist Track official music video",
            publisher = "Publisher $id",
            thumbnailUrl = null,
            durationMs = 180_000L,
            musicCategory = true,
        )
}

private object AcceptedConsentStore : VideoConsentStore {
    override fun isAccepted() = true

    override fun accept() = Unit
}

private class VideoViewModelFakeAudio : PlaybackController {
    private val queueState = MutableStateFlow(queueFor("track"))
    private val playingState = MutableStateFlow(true)
    private val statusState = MutableStateFlow(PlaybackStatus())
    private val modeState = MutableStateFlow(PlaybackMode.Default)

    var pauseCalls = 0
    var playCalls = 0

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

    override fun seekTo(positionMs: Long) = Unit

    override fun playFromIndex(index: Int) = Unit

    override fun retryCurrent() = Unit

    override fun stopAndClear() = Unit

    override fun currentPositionMs() = 10_000L

    override fun durationMs() = 180_000L

    override fun cyclePlaybackMode() = Unit

    fun replaceTrack(id: String) {
        queueState.value = queueFor(id)
    }

    private companion object {
        fun queueFor(id: String) =
            PlaybackQueue(
                items =
                    listOf(
                        QueueItem(
                            id = id,
                            title = "Track",
                            artist = "Artist",
                            album = "Album",
                            streamUrl = "stream",
                            durationMs = 180_000L,
                        ),
                    ),
            )
    }
}
