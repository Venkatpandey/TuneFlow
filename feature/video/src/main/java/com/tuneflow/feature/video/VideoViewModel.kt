package com.tuneflow.feature.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.player.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VideoViewModel(
    providers: VideoProviderRegistry,
    private val audio: PlaybackController,
    val youtubePlayer: YouTubeEmbeddedPlayer,
    private val coordinator: VideoPlaybackCoordinator,
    private val consentStore: VideoConsentStore,
    private val scopeOverride: CoroutineScope? = null,
    private val nativeBackend: ExperimentalNativeVideoBackend? = null,
) : ViewModel() {
    private val scope: CoroutineScope
        get() = scopeOverride ?: viewModelScope

    private val provider = providers.provider(VideoProviderId.YouTube)
    private val _uiState =
        kotlinx.coroutines.flow.MutableStateFlow<VideoUiState>(
            if (provider == null && nativeBackend == null) {
                VideoUiState.Unavailable("Set TUNEFLOW_YOUTUBE_API_KEY to enable video.")
            } else {
                VideoUiState.Idle
            },
        )
    val uiState: kotlinx.coroutines.flow.StateFlow<VideoUiState> = _uiState
    private val _surfacePlayer = kotlinx.coroutines.flow.MutableStateFlow<VideoSurfacePlayer>(youtubePlayer)
    val surfacePlayer: kotlinx.coroutines.flow.StateFlow<VideoSurfacePlayer> = _surfacePlayer
    private val returnToNowPlayingChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val returnToNowPlayingEvents: Flow<Unit> = returnToNowPlayingChannel.receiveAsFlow()

    private var searchJob: Job? = null
    private var generation = 0L
    private var activeCandidate: VideoCandidate? = null
    private var lastCandidates: List<VideoCandidate> = emptyList()

    init {
        scope.launch {
            audio.queue
                .map { it.currentItem?.id }
                .distinctUntilChanged()
                .collect { trackId -> onTrackChanged(trackId) }
        }
        scope.launch {
            youtubePlayer.state.collect { onPlayerState(youtubePlayer, it) }
        }
        nativeBackend?.player?.let { nativePlayer ->
            scope.launch {
                nativePlayer.state.collect { onPlayerState(nativePlayer, it) }
            }
        }
    }

    fun onVideoAction() {
        when (val state = _uiState.value) {
            VideoUiState.Idle,
            is VideoUiState.Error,
            -> requestVideo()
            is VideoUiState.Playing ->
                if (state.presentation == VideoPresentationMode.Mini) {
                    _uiState.value = state.copy(presentation = VideoPresentationMode.Fullscreen)
                }
            else -> Unit
        }
    }

    @Suppress("ReturnCount")
    fun requestVideo() {
        if (provider == null && nativeBackend == null) return
        val track = audio.queue.value.currentItem ?: return
        generation += 1
        val requestGeneration = generation
        if (!consentStore.isAccepted()) {
            _uiState.value = VideoUiState.ConsentRequired(track.id, requestGeneration)
            return
        }
        search(track.id, requestGeneration)
    }

    @Suppress("ReturnCount")
    fun acceptDisclosure() {
        val state = _uiState.value as? VideoUiState.ConsentRequired ?: return
        if (audio.queue.value.currentItem?.id != state.trackId) return
        consentStore.accept()
        if (provider == null && nativeBackend == null) return
        search(state.trackId, state.generation)
    }

    fun cancelDisclosure() {
        if (_uiState.value is VideoUiState.ConsentRequired) {
            _uiState.value = availableIdleState()
        }
    }

    @Suppress("ReturnCount")
    fun selectCandidate(candidate: VideoCandidate) {
        val track = audio.queue.value.currentItem ?: return
        if (candidate.providerId != VideoProviderId.YouTube) return
        activeCandidate = candidate
        val session = VideoSessionKey(track.id, generation)
        _uiState.value =
            VideoUiState.Loading(
                trackId = track.id,
                generation = generation,
                candidate = candidate,
                presentation = VideoPresentationMode.Fullscreen,
            )
        val selectedPlayer = nativeBackend?.player ?: youtubePlayer
        _surfacePlayer.value = selectedPlayer
        selectedPlayer.prepare(session, EmbeddedVideoPlayerSpec(VideoProviderId.YouTube, candidate.videoId))
    }

    fun chooseAnother() {
        val trackId = _uiState.value.trackId ?: return
        val candidates = lastCandidates
        if (candidates.isEmpty()) {
            stopVideo()
            requestVideo()
            return
        }
        returnToAudio(resumeAudio = false)
        generation += 1
        activeCandidate = null
        _uiState.value = VideoUiState.Candidates(trackId, generation, candidates)
    }

    fun enterFullscreen() {
        val state = _uiState.value as? VideoUiState.Playing ?: return
        _uiState.value = state.copy(presentation = VideoPresentationMode.Fullscreen)
    }

    fun exitFullscreen() {
        when (val state = _uiState.value) {
            is VideoUiState.Loading -> {
                if (state.presentation == VideoPresentationMode.Fullscreen) {
                    _uiState.value =
                        state.copy(
                            presentation = VideoPresentationMode.Mini,
                            focusRequestId = state.focusRequestId + 1L,
                        )
                }
            }
            is VideoUiState.Playing -> {
                if (state.presentation == VideoPresentationMode.Fullscreen) {
                    _uiState.value =
                        state.copy(
                            presentation = VideoPresentationMode.Mini,
                            focusRequestId = state.focusRequestId + 1L,
                        )
                }
            }
            else -> Unit
        }
    }

    fun togglePlayPause(): Boolean {
        val state = _uiState.value as? VideoUiState.Playing ?: return false
        if (state.isPlaying) activePlayer().pause() else activePlayer().play()
        return true
    }

    fun play(): Boolean {
        if (_uiState.value !is VideoUiState.Playing) return false
        activePlayer().play()
        return true
    }

    fun pause(): Boolean {
        if (_uiState.value !is VideoUiState.Playing) return false
        activePlayer().pause()
        return true
    }

    fun seekBy(deltaMs: Long): Boolean {
        val state = _uiState.value as? VideoUiState.Playing ?: return false
        activePlayer().seekTo((state.positionMs + deltaMs).coerceIn(0L, state.durationMs.coerceAtLeast(0L)))
        return true
    }

    fun adjustVolume(delta: Int): Boolean {
        if (_uiState.value !is VideoUiState.Playing) return false
        activePlayer().adjustVolume(delta)
        return true
    }

    fun stopVideo() {
        val hadActiveSession = _uiState.value.isVideoSessionActive
        returnToAudio(resumeAudio = false)
        activeCandidate = null
        lastCandidates = emptyList()
        generation += 1
        _uiState.value = availableIdleState()
        if (hadActiveSession) returnToNowPlayingChannel.trySend(Unit)
    }

    fun closeForQueueChange() {
        returnToAudio(resumeAudio = false)
        activeCandidate = null
        lastCandidates = emptyList()
        generation += 1
        _uiState.value = availableIdleState()
    }

    fun onAppBackgrounded() {
        if (_uiState.value is VideoUiState.Playing) activePlayer().pause()
    }

    private fun search(
        trackId: String,
        requestGeneration: Long,
    ) {
        val track = audio.queue.value.currentItem?.takeIf { it.id == trackId } ?: return
        searchJob?.cancel()
        _uiState.value = VideoUiState.Searching(trackId, requestGeneration)
        val locale = Locale.getDefault()
        val query =
            track.toVideoTrackQuery(
                regionCode = locale.country.takeIf { it.length == 2 },
                languageCode = locale.language.takeIf(String::isNotBlank),
            )
        searchJob =
            scope.launch {
                try {
                    val discovered =
                        nativeBackend?.let { backend ->
                            runCatching { backend.search(query) }.getOrElse {
                                provider?.search(query) ?: throw YouTubeSearchException("Native YouTube search failed.")
                            }
                        } ?: provider?.search(query).orEmpty()
                    val ranked =
                        VideoCandidateRanker
                            .rank(query, filterUnwantedVideoCandidates(query, discovered))
                            .take(YOUTUBE_SEARCH_RESULT_LIMIT)
                    if (!isCurrent(trackId, requestGeneration)) return@launch
                    lastCandidates = ranked
                    if (ranked.isEmpty()) {
                        _uiState.value =
                            VideoUiState.Error(
                                trackId,
                                requestGeneration,
                                "No playable YouTube match found.",
                            )
                    } else {
                        if (VideoCandidateRanker.shouldAutoplay(ranked)) {
                            selectCandidate(ranked.first())
                        } else {
                            _uiState.value = VideoUiState.Candidates(trackId, requestGeneration, ranked)
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    publishSearchError(trackId, requestGeneration, "YouTube search timed out.")
                } catch (error: YouTubeSearchException) {
                    publishSearchError(trackId, requestGeneration, error.message ?: "YouTube search failed.")
                } catch (_: Exception) {
                    publishSearchError(trackId, requestGeneration, "Could not reach YouTube.")
                }
            }
    }

    private fun publishSearchError(
        trackId: String,
        requestGeneration: Long,
        message: String,
    ) {
        if (isCurrent(trackId, requestGeneration)) {
            _uiState.value = VideoUiState.Error(trackId, requestGeneration, message)
        }
    }

    private fun onTrackChanged(trackId: String?) {
        val activeTrackId = _uiState.value.trackId
        if (activeTrackId == null || activeTrackId == trackId) return
        searchJob?.cancel()
        activePlayer().release()
        _surfacePlayer.value = youtubePlayer
        coordinator.discard()
        activeCandidate = null
        lastCandidates = emptyList()
        generation += 1
        _uiState.value = availableIdleState()
    }

    private fun onPlayerState(
        source: VideoSurfacePlayer,
        playerState: EmbeddedVideoPlayerState,
    ) {
        if (source !== activePlayer()) return
        when (playerState) {
            is EmbeddedVideoPlayerState.Ready -> onPlayerReady(playerState)
            is EmbeddedVideoPlayerState.Playing -> updatePlayingState(playerState, isPlaying = true)
            is EmbeddedVideoPlayerState.Paused -> updatePlayingState(playerState, isPlaying = false)
            is EmbeddedVideoPlayerState.Buffering -> updatePlayingState(playerState, isPlaying = currentVideoWasPlaying())
            is EmbeddedVideoPlayerState.Ended -> onPlayerEnded(playerState)
            is EmbeddedVideoPlayerState.Error -> onPlayerError(source, playerState)
            EmbeddedVideoPlayerState.Idle,
            is EmbeddedVideoPlayerState.Loading,
            -> Unit
        }
    }

    private fun onPlayerReady(state: EmbeddedVideoPlayerState.Ready) {
        if (!isCurrent(state.session)) return
        val startPosition = coordinator.startVideo(state.session.trackId)
        if (startPosition == null) {
            onPlayerError(
                activePlayer(),
                EmbeddedVideoPlayerState.Error(
                    state.session,
                    "The audio track changed before YouTube was ready.",
                ),
            )
            return
        }
        val position = startPosition.coerceIn(0L, state.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE)
        activePlayer().seekTo(position)
        activePlayer().play()
    }

    @Suppress("ReturnCount")
    private fun updatePlayingState(
        state: EmbeddedVideoPlayerState,
        isPlaying: Boolean,
    ) {
        val session = state.sessionOrNull() ?: return
        if (!isCurrent(session)) return
        val candidate = activeCandidate ?: return
        val current = _uiState.value
        val presentation =
            when (current) {
                is VideoUiState.Loading -> current.presentation
                is VideoUiState.Playing -> current.presentation
                else -> VideoPresentationMode.Fullscreen
            }
        val focusRequestId =
            when (current) {
                is VideoUiState.Loading -> current.focusRequestId
                is VideoUiState.Playing -> current.focusRequestId
                else -> 0L
            }
        _uiState.value =
            VideoUiState.Playing(
                trackId = session.trackId,
                generation = session.generation,
                candidate = candidate,
                presentation = presentation,
                positionMs = state.positionMsOrZero(),
                durationMs = state.durationMsOrZero().takeIf { it > 0L } ?: candidate.durationMs,
                isPlaying = isPlaying,
                focusRequestId = focusRequestId,
            )
    }

    private fun onPlayerEnded(state: EmbeddedVideoPlayerState.Ended) {
        if (!isCurrent(state.session)) return
        coordinator.returnToAudio(state.positionMs, resumeAudio = false)
        activePlayer().release()
        _surfacePlayer.value = youtubePlayer
        activeCandidate = null
        lastCandidates = emptyList()
        generation += 1
        _uiState.value = availableIdleState()
        returnToNowPlayingChannel.trySend(Unit)
    }

    @Suppress("ReturnCount")
    private fun onPlayerError(
        source: VideoSurfacePlayer,
        state: EmbeddedVideoPlayerState.Error,
    ) {
        if (!isCurrent(state.session)) return
        coordinator.restoreAfterFailure()
        source.release()
        if (source.isNative) {
            _surfacePlayer.value = youtubePlayer
            val candidate = activeCandidate ?: return
            _uiState.value =
                VideoUiState.Loading(
                    trackId = state.session.trackId,
                    generation = state.session.generation,
                    candidate = candidate,
                    presentation = VideoPresentationMode.Fullscreen,
                )
            youtubePlayer.prepare(
                state.session,
                EmbeddedVideoPlayerSpec(VideoProviderId.YouTube, candidate.videoId),
            )
            return
        }
        activeCandidate = null
        _uiState.value = VideoUiState.Error(state.session.trackId, state.session.generation, state.message)
    }

    private fun returnToAudio(resumeAudio: Boolean) {
        searchJob?.cancel()
        val player = activePlayer()
        val playerState = player.state.value
        coordinator.returnToAudio(playerState.positionMsOrNull(), resumeAudio)
        player.release()
        _surfacePlayer.value = youtubePlayer
    }

    private fun currentVideoWasPlaying(): Boolean =
        (activePlayer().state.value is EmbeddedVideoPlayerState.Playing) ||
            ((_uiState.value as? VideoUiState.Playing)?.isPlaying == true)

    private fun activePlayer(): VideoSurfacePlayer = _surfacePlayer.value

    private fun isCurrent(
        trackId: String,
        requestGeneration: Long,
    ): Boolean = generation == requestGeneration && audio.queue.value.currentItem?.id == trackId

    private fun isCurrent(session: VideoSessionKey): Boolean = isCurrent(session.trackId, session.generation)

    private fun availableIdleState(): VideoUiState =
        if (provider == null && nativeBackend == null) {
            VideoUiState.Unavailable("Set TUNEFLOW_YOUTUBE_API_KEY to enable video.")
        } else {
            VideoUiState.Idle
        }
}

private fun EmbeddedVideoPlayerState.sessionOrNull(): VideoSessionKey? =
    when (this) {
        is EmbeddedVideoPlayerState.Loading -> session
        is EmbeddedVideoPlayerState.Ready -> session
        is EmbeddedVideoPlayerState.Playing -> session
        is EmbeddedVideoPlayerState.Paused -> session
        is EmbeddedVideoPlayerState.Buffering -> session
        is EmbeddedVideoPlayerState.Ended -> session
        is EmbeddedVideoPlayerState.Error -> session
        EmbeddedVideoPlayerState.Idle -> null
    }

private fun EmbeddedVideoPlayerState.positionMsOrNull(): Long? =
    when (this) {
        is EmbeddedVideoPlayerState.Playing -> positionMs
        is EmbeddedVideoPlayerState.Paused -> positionMs
        is EmbeddedVideoPlayerState.Buffering -> positionMs
        is EmbeddedVideoPlayerState.Ended -> positionMs
        else -> null
    }

private fun EmbeddedVideoPlayerState.positionMsOrZero(): Long = positionMsOrNull() ?: 0L

private fun EmbeddedVideoPlayerState.durationMsOrZero(): Long =
    when (this) {
        is EmbeddedVideoPlayerState.Ready -> durationMs
        is EmbeddedVideoPlayerState.Playing -> durationMs
        is EmbeddedVideoPlayerState.Paused -> durationMs
        is EmbeddedVideoPlayerState.Buffering -> durationMs
        is EmbeddedVideoPlayerState.Ended -> durationMs
        else -> 0L
    }
