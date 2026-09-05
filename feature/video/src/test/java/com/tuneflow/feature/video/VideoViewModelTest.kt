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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
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
            val nativeBackend = FakeNativeBackend(searchDelayMs = 1_000L)
            val viewModel = createViewModel(audio, backgroundScope, nativeBackend)
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
            val nativeBackend = FakeNativeBackend(searchDelayMs = 10_000L)
            val viewModel = createViewModel(audio, backgroundScope, nativeBackend)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            audio.replaceTrack("new-track")
            runCurrent()

            assertTrue(nativeBackend.cancelled)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
        }

    @Test
    fun ambiguousSearchRequiresManualSelectionAndReturnsUpToTwentyFiveMatches() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val nativeBackend = FakeNativeBackend(resultCount = 30)
            val viewModel = createViewModel(audio, backgroundScope, nativeBackend)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()

            val state = viewModel.uiState.value as VideoUiState.Candidates
            assertEquals(25, state.candidates.size)
            assertEquals(0, audio.pauseCalls)
        }

    @Test
    fun firstUseNeverAutoplaysEvenWithOneSearchResult() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(resultCount = 1))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()

            assertTrue(viewModel.uiState.value is VideoUiState.Candidates)
            assertEquals(0, audio.pauseCalls)
        }

    @Test
    fun enteringNowPlayingPrefetchesAndMappedClickStartsWithoutAnotherLookup() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val nativeBackend = FakeNativeBackend()
            val store =
                FakePreferredVideoStore(
                    lookupResult = PreferredVideoLookupResult.Found(historyEntry("track", "mappedvid01")),
                )
            val viewModel = createViewModel(audio, backgroundScope, nativeBackend, store)
            runCurrent()

            viewModel.setNowPlayingVisible(true)
            runCurrent()

            assertTrue(viewModel.preferredVideoState.value is PreferredVideoState.Mapped)
            assertEquals(listOf("track"), store.lookupTrackIds)

            viewModel.requestVideo()
            runCurrent()

            val loading = viewModel.uiState.value as VideoUiState.Loading
            assertEquals("mappedvid01", loading.candidate.videoId)
            assertEquals(1, store.lookupTrackIds.size)
            assertEquals(0, nativeBackend.searchCalls)
        }

    @Test
    fun mappedPlaybackUpdatesRecentOnlyAfterConfirmedPlaying() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val nativePlayer = FakeNativePlayer()
            val store =
                FakePreferredVideoStore(
                    lookupResult = PreferredVideoLookupResult.Found(historyEntry("track", "mappedvid01")),
                )
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer), store)
            runCurrent()
            viewModel.setNowPlayingVisible(true)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            assertTrue(store.playedTrackIds.isEmpty())

            nativePlayer.emitReady(180_000L)
            runCurrent()
            nativePlayer.emitPlaying(0L, 180_000L)
            runCurrent()

            assertEquals(0L, nativePlayer.seekPositionMs)
            assertEquals(listOf("track"), store.playedTrackIds)
        }

    @Test
    fun videoClickWhileLookupChecksSearchesImmediatelyAndLateResultDoesNotInterruptPicker() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val nativeBackend = FakeNativeBackend()
            val store = FakePreferredVideoStore(lookupDelayMs = 10_000L)
            val viewModel = createViewModel(audio, backgroundScope, nativeBackend, store)
            runCurrent()

            viewModel.setNowPlayingVisible(true)
            runCurrent()
            assertTrue(viewModel.preferredVideoState.value is PreferredVideoState.Checking)

            viewModel.requestVideo()
            runCurrent()

            assertTrue(viewModel.uiState.value is VideoUiState.Candidates)
            assertEquals(1, store.lookupTrackIds.size)
            assertEquals(1, nativeBackend.searchCalls)

            advanceTimeBy(10_000L)
            runCurrent()

            assertTrue(viewModel.uiState.value is VideoUiState.Candidates)
            assertTrue(viewModel.preferredVideoState.value is PreferredVideoState.Unmapped)
        }

    @Test
    fun rapidNowPlayingReentryRejectsLateSameTrackLookup() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val store = RapidNavigationPreferredVideoStore()
            val viewModel = createViewModel(audio, backgroundScope, preferredVideoStore = store)
            runCurrent()

            viewModel.setNowPlayingVisible(true)
            runCurrent()
            viewModel.setNowPlayingVisible(false)
            viewModel.setNowPlayingVisible(true)
            runCurrent()

            assertTrue(viewModel.preferredVideoState.value is PreferredVideoState.Unmapped)

            advanceTimeBy(10_000L)
            runCurrent()

            assertTrue(viewModel.preferredVideoState.value is PreferredVideoState.Unmapped)
        }

    @Test
    fun selectedCandidateLoadsFullscreenAndBackMinimizesWithoutClosing() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val viewModel = createViewModel(audio, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            val candidate = (viewModel.uiState.value as VideoUiState.Candidates).candidates.first()
            viewModel.selectCandidate(candidate)

            val loading = viewModel.uiState.value as VideoUiState.Loading
            assertEquals(VideoPresentationMode.Fullscreen, loading.presentation)
            assertEquals(
                VideoTrackDetails(title = "Track", artist = "Artist", album = "Album"),
                loading.trackDetails,
            )

            viewModel.exitFullscreen()

            val minimized = viewModel.uiState.value as VideoUiState.Loading
            assertEquals(VideoPresentationMode.Mini, minimized.presentation)
            assertEquals(1L, minimized.focusRequestId)

            viewModel.enterFullscreen()

            val restored = viewModel.uiState.value as VideoUiState.Loading
            assertEquals(VideoPresentationMode.Fullscreen, restored.presentation)
        }

    @Test
    fun stoppingVideoLeavesAudioPausedAndRequestsNowPlaying() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val viewModel = createViewModel(audio, backgroundScope)
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
            val nativePlayer = FakeNativePlayer()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer))
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
            val nativePlayer = FakeNativePlayer()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer))
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
            val nativePlayer = FakeNativePlayer()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer))
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
    fun activeVideoSeekUsesRelativePositionAndClampsToDuration() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val nativePlayer = FakeNativePlayer()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            assertFalse(viewModel.seekBy(-10_000L))
            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            nativePlayer.emitPlaying(10_000L, 180_000L)
            runCurrent()

            assertTrue(viewModel.seekBy(-10_000L))
            assertEquals(0L, nativePlayer.seekPositionMs)
            assertTrue(viewModel.seekBy(200_000L))
            assertEquals(180_000L, nativePlayer.seekPositionMs)
        }

    @Test
    fun firstPlayingStateRecordsVideoOnce() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val nativePlayer = FakeNativePlayer()
            val preferredVideoStore = FakePreferredVideoStore()
            val viewModel =
                createViewModel(
                    audio,
                    backgroundScope,
                    FakeNativeBackend(nativePlayer),
                    preferredVideoStore,
                )
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            val candidate = (viewModel.uiState.value as VideoUiState.Candidates).candidates.first()
            viewModel.selectCandidate(candidate)
            assertTrue(preferredVideoStore.savedMappings.isEmpty())
            nativePlayer.emitPlaying(0L, 180_000L)
            runCurrent()
            nativePlayer.emitPlaying(1_000L, 180_000L)
            runCurrent()

            assertEquals(listOf("track" to candidate.videoId), preferredVideoStore.savedMappings)
        }

    @Test
    fun trackChangeReleasesNativeSession() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val nativePlayer = FakeNativePlayer()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            audio.replaceTrack("next")
            runCurrent()

            assertEquals(1, nativePlayer.releaseCalls)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
        }

    @Test
    fun selectingVideoFromPlaylistEnablesVideoPreference() =
        runTest {
            val audio = VideoViewModelFakeAudio(playlistQueue("track", "next"))
            val viewModel = createViewModel(audio, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())

            assertTrue(viewModel.videoPreferred.value)
            assertTrue(viewModel.uiState.value is VideoUiState.Loading)
        }

    @Test
    fun selectingVideoOutsidePlaylistKeepsAudioOnlyMode() =
        runTest {
            val audio = VideoViewModelFakeAudio()
            val viewModel = createViewModel(audio, backgroundScope)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())

            assertFalse(viewModel.videoPreferred.value)
        }

    @Test
    fun completedVideoAdvancesPlaylistAndStartsNextPreferredVideo() =
        runTest {
            val audio = VideoViewModelFakeAudio(playlistQueue("track", "next"))
            val nativePlayer = FakeNativePlayer()
            val store =
                FakePreferredVideoStore(
                    lookupResults =
                        mapOf(
                            "next" to
                                PreferredVideoLookupResult.Found(
                                    historyEntry("next", "nextvideo01"),
                                ),
                        ),
                )
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer), store)
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            nativePlayer.emitEnded()
            runCurrent()

            assertEquals("next", audio.queue.value.currentItem?.id)
            assertEquals(listOf("next"), store.lookupTrackIds)
            val loading = viewModel.uiState.value as VideoUiState.Loading
            assertEquals("nextvideo01", loading.candidate.videoId)
            assertEquals(0, audio.playCalls)
            assertTrue(viewModel.videoPreferred.value)
        }

    @Test
    fun completedVideoFallsBackToAudioWhenNextTrackHasNoPreferredVideo() =
        runTest {
            val audio = VideoViewModelFakeAudio(playlistQueue("track", "next"))
            val nativePlayer = FakeNativePlayer()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            nativePlayer.emitEnded()
            runCurrent()

            assertEquals("next", audio.queue.value.currentItem?.id)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
            assertEquals(1, audio.playCalls)
            assertTrue(audio.isPlaying.value)
            assertTrue(viewModel.videoPreferred.value)
        }

    @Test
    fun preferredVideoModeStartsMappedVideoAfterAudioAdvances() =
        runTest {
            val audio = VideoViewModelFakeAudio(playlistQueue("track", "next"))
            val store =
                FakePreferredVideoStore(
                    lookupResults =
                        mapOf(
                            "track" to PreferredVideoLookupResult.Missing,
                            "next" to
                                PreferredVideoLookupResult.Found(
                                    historyEntry("next", "nextvideo01"),
                                ),
                        ),
                )
            val viewModel = createViewModel(audio, backgroundScope, preferredVideoStore = store)
            runCurrent()

            viewModel.toggleVideoPreferredMode()
            runCurrent()
            audio.next()
            runCurrent()

            val loading = viewModel.uiState.value as VideoUiState.Loading
            assertEquals("nextvideo01", loading.candidate.videoId)
            assertEquals(listOf("track", "next"), store.lookupTrackIds)
            assertTrue(viewModel.videoPreferred.value)
        }

    @Test
    fun disablingVideoPreferenceSwitchesActiveVideoBackToAudioPosition() =
        runTest {
            val audio = VideoViewModelFakeAudio(playlistQueue("track", "next"))
            val nativePlayer = FakeNativePlayer()
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer))
            runCurrent()

            viewModel.requestVideo()
            runCurrent()
            viewModel.selectCandidate((viewModel.uiState.value as VideoUiState.Candidates).candidates.first())
            nativePlayer.emitPlaying(positionMs = 42_000L, durationMs = 180_000L)
            runCurrent()

            viewModel.toggleVideoPreferredMode()

            assertFalse(viewModel.videoPreferred.value)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
            assertEquals(42_000L, audio.seekPositionMs)
            assertEquals(1, audio.playCalls)
            assertTrue(audio.isPlaying.value)
        }

    @Test
    fun automaticPreferredVideoFailureFallsBackToAudio() =
        runTest {
            val audio = VideoViewModelFakeAudio(playlistQueue("track", "next"))
            val nativePlayer = FakeNativePlayer()
            val store =
                FakePreferredVideoStore(
                    lookupResult =
                        PreferredVideoLookupResult.Found(
                            historyEntry("track", "mappedvid01"),
                        ),
                )
            val viewModel = createViewModel(audio, backgroundScope, FakeNativeBackend(nativePlayer), store)
            runCurrent()

            viewModel.toggleVideoPreferredMode()
            runCurrent()
            nativePlayer.emitError("Native resolver failed.")
            runCurrent()

            assertTrue(viewModel.videoPreferred.value)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
            assertEquals(1, audio.playCalls)
            assertTrue(audio.isPlaying.value)
        }

    @Test
    fun disabledVideoPreferenceLeavesFollowingPlaylistTracksOnAudio() =
        runTest {
            val audio = VideoViewModelFakeAudio(playlistQueue("track", "next"))
            val store = FakePreferredVideoStore()
            val viewModel = createViewModel(audio, backgroundScope, preferredVideoStore = store)
            runCurrent()

            viewModel.toggleVideoPreferredMode()
            runCurrent()
            viewModel.toggleVideoPreferredMode()
            audio.next()
            runCurrent()

            assertFalse(viewModel.videoPreferred.value)
            assertTrue(viewModel.uiState.value is VideoUiState.Idle)
            assertEquals(listOf("track"), store.lookupTrackIds)
        }

    private fun createViewModel(
        audio: VideoViewModelFakeAudio,
        scope: CoroutineScope,
        nativeBackend: NativeVideoBackend = FakeNativeBackend(),
        preferredVideoStore: PreferredVideoStore = FakePreferredVideoStore(),
    ): VideoViewModel =
        VideoViewModel(
            audio = audio,
            consentStore = AcceptedConsentStore,
            nativeBackend = nativeBackend,
            preferredVideoStore = preferredVideoStore,
            scopeOverride = scope,
            diagnostic = {},
        )
}

private class FakeNativeBackend(
    override val player: NativeVideoPlayer = FakeNativePlayer(),
    private val searchDelayMs: Long = 0L,
    private val resultCount: Int = 2,
) : NativeVideoBackend {
    var cancelled = false
    var searchCalls = 0

    override suspend fun search(query: VideoTrackQuery): List<VideoCandidate> {
        searchCalls += 1
        try {
            delay(searchDelayMs)
        } catch (error: CancellationException) {
            cancelled = true
            throw error
        }
        return List(resultCount) { index -> nativeCandidate(index.toString(), query) }
    }

    private fun nativeCandidate(
        id: String,
        query: VideoTrackQuery,
    ) = VideoCandidate(
        videoId = id,
        title = "${query.artist} ${query.title} official music video",
        publisher = query.artist,
        thumbnailUrl = null,
        durationMs = query.durationMs,
        musicCategory = true,
    )
}

private fun historyEntry(
    trackId: String,
    videoId: String,
) = VideoHistoryEntry(
    trackId = trackId,
    provider = "youtube",
    videoId = videoId,
    title = "Artist Track official music video",
    publisher = "Artist",
    thumbnailUrl = null,
    durationMs = 180_000L,
    viewCount = 42L,
    mappingUpdatedAt = "2026-09-01T09:00:00Z",
    lastPlayedAt = "2026-09-01T10:00:00Z",
)

private class FakeNativePlayer : NativeVideoPlayer {
    private val mutableState = MutableStateFlow<NativeVideoPlayerState>(NativeVideoPlayerState.Idle)
    override val state: StateFlow<NativeVideoPlayerState> = mutableState
    private var session: VideoSessionKey? = null
    var seekPositionMs: Long? = null
    var playCalls = 0
    var pauseCalls = 0
    var releaseCalls = 0

    override fun prepare(
        session: VideoSessionKey,
        spec: NativeVideoSpec,
    ) {
        this.session = session
        mutableState.value = NativeVideoPlayerState.Loading(session)
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
        mutableState.value = NativeVideoPlayerState.Idle
    }

    override fun createSurfaceView(context: Context): View = error("Not used in unit tests")

    override fun disposeSurfaceView(view: View) = Unit

    override fun focusPlayer() = Unit

    override fun clearPlayerFocus() = Unit

    override fun adjustVolume(delta: Int) = Unit

    fun emitReady(durationMs: Long) {
        mutableState.value = NativeVideoPlayerState.Ready(requireNotNull(session), durationMs)
    }

    fun emitError(message: String) {
        mutableState.value = NativeVideoPlayerState.Error(requireNotNull(session), message)
    }

    fun emitPlaying(
        positionMs: Long,
        durationMs: Long,
    ) {
        mutableState.value = NativeVideoPlayerState.Playing(requireNotNull(session), positionMs, durationMs)
    }

    fun emitEnded() {
        mutableState.value = NativeVideoPlayerState.Ended(requireNotNull(session), 180_000L, 180_000L)
    }
}

private class FakePreferredVideoStore(
    var lookupResult: PreferredVideoLookupResult = PreferredVideoLookupResult.Missing,
    private val lookupDelayMs: Long = 0L,
    private val lookupResults: Map<String, PreferredVideoLookupResult> = emptyMap(),
) : PreferredVideoStore {
    override val history: StateFlow<List<VideoHistoryEntry>> = MutableStateFlow(emptyList())
    val lookupTrackIds = mutableListOf<String>()
    val savedMappings = mutableListOf<Pair<String, String>>()
    val playedTrackIds = mutableListOf<String>()

    override suspend fun lookup(trackId: String): PreferredVideoLookupResult {
        lookupTrackIds += trackId
        delay(lookupDelayMs)
        return lookupResults[trackId] ?: lookupResult
    }

    override suspend fun savePreferredVideo(
        trackId: String,
        candidate: VideoCandidate,
    ): Boolean {
        savedMappings += trackId to candidate.videoId
        return true
    }

    override suspend fun markPlayed(trackId: String): Boolean {
        playedTrackIds += trackId
        return true
    }

    override suspend fun deletePreferredVideo(trackId: String) = true

    override suspend fun refreshHistory(limit: Int) = true
}

private class RapidNavigationPreferredVideoStore : PreferredVideoStore {
    override val history: StateFlow<List<VideoHistoryEntry>> = MutableStateFlow(emptyList())
    private var lookupCount = 0

    override suspend fun lookup(trackId: String): PreferredVideoLookupResult {
        lookupCount += 1
        return if (lookupCount == 1) {
            withContext(NonCancellable) {
                delay(10_000L)
                PreferredVideoLookupResult.Found(historyEntry(trackId, "oldmapping1"))
            }
        } else {
            PreferredVideoLookupResult.Missing
        }
    }

    override suspend fun savePreferredVideo(
        trackId: String,
        candidate: VideoCandidate,
    ) = true

    override suspend fun markPlayed(trackId: String) = true

    override suspend fun deletePreferredVideo(trackId: String) = true

    override suspend fun refreshHistory(limit: Int) = true
}

private object AcceptedConsentStore : VideoConsentStore {
    override fun isAccepted() = true

    override fun accept() = Unit
}

private class VideoViewModelFakeAudio(
    initialQueue: PlaybackQueue = queueFor("track"),
) : PlaybackController {
    private val queueState = MutableStateFlow(initialQueue)
    private val playingState = MutableStateFlow(true)
    private val statusState = MutableStateFlow(PlaybackStatus())
    private val modeState = MutableStateFlow(PlaybackMode.Default)

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

    override fun next() {
        queueState.value = queueState.value.next()
    }

    override fun previous() {
        queueState.value = queueState.value.previous()
    }

    override fun seekTo(positionMs: Long) {
        seekPositionMs = positionMs
    }

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

private fun playlistQueue(vararg trackIds: String): PlaybackQueue =
    PlaybackQueue(
        items =
            trackIds.map { id ->
                QueueItem(
                    id = id,
                    title = "Track $id",
                    artist = "Artist",
                    album = "Album",
                    streamUrl = "stream/$id",
                    durationMs = 180_000L,
                )
            },
        sourcePlaylistName = "Playlist",
    )
