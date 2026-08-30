package com.tuneflow.feature.video

import android.content.Context
import android.view.View
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
    fun ambiguousSearchRequiresManualSelectionAndReturnsUpToTwentyFiveMatches() =
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
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 2)
            val viewModel = createViewModel(audio, provider, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            val candidate = (viewModel.uiState.value as VideoUiState.Candidates).candidates.first()
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
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 2)
            val viewModel = createViewModel(audio, provider, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            val candidate = (viewModel.uiState.value as VideoUiState.Candidates).candidates.first()
            viewModel.selectCandidate(candidate)
            viewModel.stopVideo()

            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
            assertFalse(audio.isPlaying.value)
            assertEquals(0, audio.playCalls)
            assertEquals(Unit, viewModel.returnToNowPlayingEvents.first())
        }

    @Test
    fun nativeSelectionStopsAudioAndAlwaysStartsVideoAtZero() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 2)
            val nativePlayer = FakeNativeSurfacePlayer()
            val viewModel = createViewModel(audio, provider, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            assertEquals(1, audio.pauseCalls)
            nativePlayer.emitReady(180_000L)
            runCurrent()
            nativePlayer.emitPlaying(10_000L, 180_000L)
            runCurrent()

            assertEquals(0L, nativePlayer.seekPositionMs)
            assertEquals(1, nativePlayer.playCalls)
            assertEquals(1, audio.pauseCalls)
            viewModel.onAppBackgrounded()
            assertEquals(1, nativePlayer.pauseCalls)
        }

    @Test
    fun nativeFailureStaysNativeAndDoesNotResumeAudio() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 2)
            val nativePlayer = FakeNativeSurfacePlayer()
            val viewModel = createViewModel(audio, provider, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            nativePlayer.emitReady(180_000L)
            runCurrent()
            nativePlayer.emitError("Native resolver failed.")
            runCurrent()

            assertEquals(nativePlayer, viewModel.surfacePlayer.value)
            assertTrue(viewModel.uiState.value is VideoUiState.Error)
            assertEquals(0, audio.playCalls)
            assertTrue(nativePlayer.releaseCalls >= 1)
        }

    @Test
    fun loadingVideoConsumesPlayPauseUntilVideoStops() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 2)
            val nativePlayer = FakeNativeSurfacePlayer()
            val viewModel = createViewModel(audio, provider, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())

            assertTrue(viewModel.togglePlayPause())
            assertEquals(1, nativePlayer.playCalls)
            assertEquals(0, audio.playCalls)

            viewModel.stopVideo()

            assertFalse(viewModel.togglePlayPause())
        }

    @Test
    fun firstPlayingStateRecordsVideoOnce() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 2)
            val nativePlayer = FakeNativeSurfacePlayer()
            val historyStore = RecordingVideoHistoryStore()
            val viewModel =
                createViewModel(
                    audio,
                    provider,
                    backgroundScope,
                    FakeNativeBackend(nativePlayer),
                    historyStore,
                )
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            val candidate = (viewModel.uiState.value as VideoUiState.Candidates).candidates.first()
            viewModel.selectCandidate(candidate)
            nativePlayer.emitPlaying(0L, 180_000L)
            runCurrent()
            nativePlayer.emitPlaying(1_000L, 180_000L)
            runCurrent()

            assertEquals(listOf(candidate.videoId), historyStore.recordedIds)
        }

    @Test
    fun trackChangeReleasesNativeSession() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val provider = FakeVideoProvider(searchDelayMs = 0L, resultCount = 2)
            val nativePlayer = FakeNativeSurfacePlayer()
            val viewModel = createViewModel(audio, provider, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            audio.replaceTrack("next")
            runCurrent()

            assertEquals(1, nativePlayer.releaseCalls)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
        }

    private fun createViewModel(
        audio: VideoViewModelFakeAudio,
        provider: VideoProvider,
        scope: CoroutineScope,
        nativeBackend: ExperimentalNativeVideoBackend = FakeNativeBackend(FakeNativeSurfacePlayer(), provider),
        historyStore: VideoHistoryStore = EmptyVideoHistoryStore,
    ): VideoViewModel =
        VideoViewModel(
            providers = VideoProviderRegistry(listOf(provider)),
            audio = audio,
            consentStore = AcceptedConsentStore,
            nativeBackend = nativeBackend,
            historyStore = historyStore,
            scopeOverride = scope,
        )
}

private class FakeNativeBackend(
    override val player: VideoSurfacePlayer,
    private val searchProvider: VideoProvider? = null,
) : ExperimentalNativeVideoBackend {
    override suspend fun search(query: VideoTrackQuery): List<VideoCandidate> =
        searchProvider?.search(query)
            ?: listOf(
                nativeCandidate("native-1", query),
                nativeCandidate("native-2", query),
            )

    private fun nativeCandidate(
        id: String,
        query: VideoTrackQuery,
    ) = VideoCandidate(
        providerId = VideoProviderId.YouTube,
        videoId = id,
        title = "${query.artist} ${query.title} official music video",
        publisher = query.artist,
        thumbnailUrl = null,
        durationMs = query.durationMs,
        musicCategory = true,
    )
}

private class FakeNativeSurfacePlayer : VideoSurfacePlayer {
    override val isNative = true
    private val mutableState = MutableStateFlow<EmbeddedVideoPlayerState>(EmbeddedVideoPlayerState.Idle)
    override val state: StateFlow<EmbeddedVideoPlayerState> = mutableState
    private var session: VideoSessionKey? = null
    var seekPositionMs: Long? = null
    var playCalls = 0
    var pauseCalls = 0
    var releaseCalls = 0

    override fun prepare(
        session: VideoSessionKey,
        spec: EmbeddedVideoPlayerSpec,
    ) {
        this.session = session
        mutableState.value = EmbeddedVideoPlayerState.Loading(session)
    }

    override fun play() {
        playCalls += 1
    }

    override fun pause() {
        pauseCalls += 1
    }

    override fun seekTo(positionMs: Long) {
        seekPositionMs = positionMs
    }

    override fun release() {
        releaseCalls += 1
        mutableState.value = EmbeddedVideoPlayerState.Idle
    }

    override fun createSurfaceView(context: Context): View = error("Not used in unit tests")

    override fun disposeSurfaceView(view: View) = Unit

    override fun focusPlayer() = Unit

    override fun clearPlayerFocus() = Unit

    override fun adjustVolume(delta: Int) = Unit

    fun emitReady(durationMs: Long) {
        mutableState.value = EmbeddedVideoPlayerState.Ready(requireNotNull(session), durationMs)
    }

    fun emitError(message: String) {
        mutableState.value = EmbeddedVideoPlayerState.Error(requireNotNull(session), message)
    }

    fun emitPlaying(
        positionMs: Long,
        durationMs: Long,
    ) {
        mutableState.value = EmbeddedVideoPlayerState.Playing(requireNotNull(session), positionMs, durationMs)
    }
}

private class RecordingVideoHistoryStore : VideoHistoryStore {
    override val history: StateFlow<List<VideoHistoryEntry>> = MutableStateFlow(emptyList())
    val recordedIds = mutableListOf<String>()

    override fun record(candidate: VideoCandidate) {
        recordedIds += candidate.videoId
    }
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
