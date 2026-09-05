package com.tuneflow.feature.video

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

class VideoViewModel(
    private val audio: PlaybackController,
    private val consentStore: VideoConsentStore,
    private val nativeBackend: NativeVideoBackend,
    private val preferredVideoStore: PreferredVideoStore = UnavailablePreferredVideoStore,
    private val scopeOverride: CoroutineScope? = null,
    private val diagnostic: (String) -> Unit = { message -> Log.w(VIDEO_LOG_TAG, message) },
) : ViewModel() {
    private val scope: CoroutineScope
        get() = scopeOverride ?: viewModelScope

    private val _uiState =
        kotlinx.coroutines.flow.MutableStateFlow<VideoUiState>(VideoUiState.Idle)
    val uiState: kotlinx.coroutines.flow.StateFlow<VideoUiState> = _uiState
    private val _surfacePlayer = kotlinx.coroutines.flow.MutableStateFlow(nativeBackend.player)
    val surfacePlayer: kotlinx.coroutines.flow.StateFlow<NativeVideoPlayer> = _surfacePlayer
    private val _videoPreferred = kotlinx.coroutines.flow.MutableStateFlow(false)
    val videoPreferred: kotlinx.coroutines.flow.StateFlow<Boolean> = _videoPreferred
    private val preferredLookup =
        PreferredVideoLookupCoordinator(
            store = preferredVideoStore,
            scope = scope,
            canLookup = { nowPlayingVisible || _videoPreferred.value },
            currentPosition = { audio.queue.value.toVideoQueuePosition() },
            resumeAudio = audio::play,
            onResolved = ::applyVideoPreference,
        )
    val preferredVideoState: kotlinx.coroutines.flow.StateFlow<PreferredVideoState> = preferredLookup.state
    private val returnToNowPlayingChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val returnToNowPlayingEvents: Flow<Unit> = returnToNowPlayingChannel.receiveAsFlow()

    private var searchJob: Job? = null
    private val persistenceMutex = Mutex()
    private var generation = 0L
    private var nowPlayingVisible = false
    private var activeCandidate: VideoCandidate? = null
    private var sessionAudioTrackId: String? = null
    private var sessionQueuePosition: VideoQueuePosition? = null
    private var automaticVideoSession = false
    private var resumeAudioForNextQueuePosition = false
    private var disclosureAction = DisclosureAction.RequestVideo
    private var recordedVideoIdForSession: String? = null
    private var playbackPersistenceAction: PlaybackPersistenceAction = PlaybackPersistenceAction.None
    private var lastCandidates: List<VideoCandidate> = emptyList()

    init {
        scope.launch {
            audio.queue
                .map(PlaybackQueue::toVideoQueuePosition)
                .distinctUntilChanged()
                .collect(::onQueuePositionChanged)
        }
        scope.launch {
            nativeBackend.player.state.collect { onPlayerState(nativeBackend.player, it) }
        }
    }

    fun setNowPlayingVisible(visible: Boolean) {
        if (nowPlayingVisible == visible) return
        nowPlayingVisible = visible
        preferredLookup.restart(audio.queue.value.toVideoQueuePosition())
    }

    @Suppress("ReturnCount")
    fun toggleVideoPreferredMode() {
        if (_videoPreferred.value) {
            disableVideoPreferredMode()
            return
        }
        val position = audio.queue.value.toVideoQueuePosition()?.takeIf { it.isPlaylist } ?: return
        if (!consentStore.isAccepted()) {
            generation += 1
            disclosureAction = DisclosureAction.EnableVideoPreferred
            _uiState.value = VideoUiState.ConsentRequired(position.trackId, generation)
            return
        }
        enableVideoPreferredMode(position)
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
        val track = audio.queue.value.currentItem ?: return
        generation += 1
        val requestGeneration = generation
        if (!consentStore.isAccepted()) {
            disclosureAction = DisclosureAction.RequestVideo
            _uiState.value = VideoUiState.ConsentRequired(track.id, requestGeneration)
            return
        }
        startKnownVideoOrSearch(track.id, requestGeneration)
    }

    @Suppress("ReturnCount")
    fun acceptDisclosure() {
        val state = _uiState.value as? VideoUiState.ConsentRequired ?: return
        if (audio.queue.value.currentItem?.id != state.trackId) {
            disclosureAction = DisclosureAction.RequestVideo
            return
        }
        consentStore.accept()
        val acceptedAction = disclosureAction
        disclosureAction = DisclosureAction.RequestVideo
        when (acceptedAction) {
            DisclosureAction.RequestVideo -> startKnownVideoOrSearch(state.trackId, state.generation)
            DisclosureAction.EnableVideoPreferred -> {
                val position = audio.queue.value.toVideoQueuePosition()?.takeIf { it.isPlaylist } ?: return
                enableVideoPreferredMode(position)
            }
        }
    }

    fun cancelDisclosure() {
        if (_uiState.value is VideoUiState.ConsentRequired) {
            disclosureAction = DisclosureAction.RequestVideo
            _uiState.value = availableIdleState()
        }
    }

    @Suppress("ReturnCount")
    fun selectCandidate(candidate: VideoCandidate) {
        val track = audio.queue.value.currentItem ?: return
        startCandidate(
            candidate = candidate,
            trackId = track.id,
            boundAudioTrackId = track.id,
            persistenceAction = PlaybackPersistenceAction.SaveMapping(track.id),
            enableVideoPreferred = true,
        )
    }

    fun playHistory(entry: VideoHistoryEntry) {
        val audioTrackId = audio.queue.value.currentItem?.id
        startCandidate(
            candidate = entry.toVideoCandidate(),
            trackId = entry.trackId,
            boundAudioTrackId = audioTrackId,
            persistenceAction = PlaybackPersistenceAction.MarkPlayed(entry.trackId),
        )
    }

    private fun startKnownVideoOrSearch(
        trackId: String,
        requestGeneration: Long,
    ) {
        val preferred = preferredVideoState.value
        if (preferred is PreferredVideoState.Mapped && preferred.trackId == trackId) {
            startCandidate(
                candidate = preferred.candidate,
                trackId = trackId,
                boundAudioTrackId = trackId,
                persistenceAction = PlaybackPersistenceAction.MarkPlayed(trackId),
                enableVideoPreferred = true,
            )
        } else {
            search(trackId, requestGeneration)
        }
    }

    private fun startCandidate(
        candidate: VideoCandidate,
        trackId: String,
        boundAudioTrackId: String?,
        persistenceAction: PlaybackPersistenceAction,
        enableVideoPreferred: Boolean = false,
        automatic: Boolean = false,
    ) {
        if (_uiState.value.isVideoSessionActive) releaseVideoPlayer()
        generation += 1
        val queuePosition = audio.queue.value.toVideoQueuePosition()
        if (
            enableVideoPreferred &&
            queuePosition?.isPlaylist == true &&
            queuePosition.trackId == trackId
        ) {
            _videoPreferred.value = true
        }
        audio.pause()
        activeCandidate = candidate
        sessionAudioTrackId = boundAudioTrackId
        sessionQueuePosition = queuePosition
        automaticVideoSession = automatic
        recordedVideoIdForSession = null
        playbackPersistenceAction = persistenceAction
        val session = VideoSessionKey(trackId, generation)
        val trackDetails =
            audio.queue.value.items
                .firstOrNull { it.id == trackId }
                ?.toVideoTrackDetails()
                ?: VideoTrackDetails(
                    title = candidate.title,
                    artist = candidate.publisher,
                    album = "",
                )
        _uiState.value =
            VideoUiState.Loading(
                trackId = trackId,
                generation = generation,
                candidate = candidate,
                trackDetails = trackDetails,
                presentation = VideoPresentationMode.Fullscreen,
            )
        nativeBackend.player.prepare(session, NativeVideoSpec(candidate.videoId))
    }

    fun chooseAnother() {
        val trackId = _uiState.value.trackId ?: return
        val candidates = lastCandidates
        if (candidates.isEmpty()) {
            releaseVideoPlayer()
            generation += 1
            activeCandidate = null
            sessionAudioTrackId = audio.queue.value.currentItem?.id
            recordedVideoIdForSession = null
            playbackPersistenceAction = PlaybackPersistenceAction.None
            search(trackId, generation)
            return
        }
        releaseVideoPlayer()
        generation += 1
        activeCandidate = null
        sessionAudioTrackId = audio.queue.value.currentItem?.id
        recordedVideoIdForSession = null
        playbackPersistenceAction = PlaybackPersistenceAction.None
        _uiState.value = VideoUiState.Candidates(trackId, generation, candidates)
    }

    fun enterFullscreen() {
        when (val state = _uiState.value) {
            is VideoUiState.Loading -> _uiState.value = state.copy(presentation = VideoPresentationMode.Fullscreen)
            is VideoUiState.Playing -> _uiState.value = state.copy(presentation = VideoPresentationMode.Fullscreen)
            else -> Unit
        }
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
        val state = _uiState.value
        if (!state.isVideoSessionActive) return false
        if ((state as? VideoUiState.Playing)?.isPlaying == true) activePlayer().pause() else activePlayer().play()
        return true
    }

    fun play(): Boolean {
        if (!_uiState.value.isVideoSessionActive) return false
        activePlayer().play()
        return true
    }

    fun pause(): Boolean {
        if (!_uiState.value.isVideoSessionActive) return false
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
        releaseVideoPlayer()
        activeCandidate = null
        sessionAudioTrackId = null
        sessionQueuePosition = null
        automaticVideoSession = false
        recordedVideoIdForSession = null
        playbackPersistenceAction = PlaybackPersistenceAction.None
        lastCandidates = emptyList()
        generation += 1
        _uiState.value = availableIdleState()
        if (hadActiveSession) returnToNowPlayingChannel.trySend(Unit)
    }

    fun nextTrack(): Boolean = moveFromVideoToQueue(audio::next)

    fun previousTrack(): Boolean = moveFromVideoToQueue(audio::previous)

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
                    val discovered = nativeBackend.search(query)
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
                        _uiState.value = VideoUiState.Candidates(trackId, requestGeneration, ranked)
                    }
                } catch (_: TimeoutCancellationException) {
                    publishSearchError(trackId, requestGeneration, "YouTube search timed out.")
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

    private fun onQueuePositionChanged(position: VideoQueuePosition?) {
        val videoWasActive = _uiState.value.isVideoSessionActive
        val requestTrackChanged =
            sessionQueuePosition == null &&
                _uiState.value.trackId?.let { it != position?.trackId } == true
        val sessionTrackChanged =
            sessionQueuePosition?.representsSameTrack(position) == false
        if (sessionTrackChanged || requestTrackChanged) {
            clearVideoSession()
        }
        val resumeAudio = resumeAudioForNextQueuePosition || (videoWasActive && _videoPreferred.value)
        resumeAudioForNextQueuePosition = false
        if (position?.isPlaylist != true) _videoPreferred.value = false
        if (_videoPreferred.value && position != null) {
            preferredLookup.restart(position, resumeAudioIfMissing = resumeAudio)
        } else {
            if (resumeAudio) audio.play()
            if (nowPlayingVisible) {
                preferredLookup.restart(position)
            } else {
                preferredLookup.cancelAndReset()
            }
        }
    }

    private fun clearVideoSession() {
        searchJob?.cancel()
        releaseVideoPlayer()
        activeCandidate = null
        sessionAudioTrackId = null
        sessionQueuePosition = null
        automaticVideoSession = false
        recordedVideoIdForSession = null
        playbackPersistenceAction = PlaybackPersistenceAction.None
        lastCandidates = emptyList()
        generation += 1
        _uiState.value = availableIdleState()
    }

    private fun onPlayerState(
        source: NativeVideoPlayer,
        playerState: NativeVideoPlayerState,
    ) {
        if (source !== activePlayer()) return
        when (playerState) {
            is NativeVideoPlayerState.Ready -> onPlayerReady(playerState)
            is NativeVideoPlayerState.Playing -> updatePlayingState(playerState, isPlaying = true)
            is NativeVideoPlayerState.Paused -> updatePlayingState(playerState, isPlaying = false)
            is NativeVideoPlayerState.Buffering -> updatePlayingState(playerState, isPlaying = currentVideoWasPlaying())
            is NativeVideoPlayerState.Ended -> onPlayerEnded(playerState)
            is NativeVideoPlayerState.Error -> onPlayerError(source, playerState)
            NativeVideoPlayerState.Idle,
            is NativeVideoPlayerState.Loading,
            -> Unit
        }
    }

    private fun onPlayerReady(state: NativeVideoPlayerState.Ready) {
        if (!isCurrentSession(state.session)) return
        activePlayer().seekTo(0L)
        activePlayer().play()
    }

    @Suppress("ReturnCount")
    private fun updatePlayingState(
        state: NativeVideoPlayerState,
        isPlaying: Boolean,
    ) {
        val session = state.sessionOrNull() ?: return
        if (!isCurrentSession(session)) return
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
        val trackDetails = current.activeTrackDetails ?: return
        _uiState.value =
            VideoUiState.Playing(
                trackId = session.trackId,
                generation = session.generation,
                candidate = candidate,
                trackDetails = trackDetails,
                presentation = presentation,
                positionMs = state.positionMsOrZero(),
                durationMs = state.durationMsOrZero().takeIf { it > 0L } ?: candidate.durationMs,
                isPlaying = isPlaying,
                focusRequestId = focusRequestId,
            )
        if (isPlaying && recordedVideoIdForSession != candidate.videoId) {
            recordedVideoIdForSession = candidate.videoId
            persistSuccessfulPlayback(candidate, playbackPersistenceAction)
        }
    }

    private fun persistSuccessfulPlayback(
        candidate: VideoCandidate,
        action: PlaybackPersistenceAction,
    ) {
        scope.launch {
            persistenceMutex.withLock {
                val success =
                    when (action) {
                        is PlaybackPersistenceAction.SaveMapping ->
                            preferredVideoStore.savePreferredVideo(action.trackId, candidate)
                        is PlaybackPersistenceAction.MarkPlayed -> preferredVideoStore.markPlayed(action.trackId)
                        PlaybackPersistenceAction.None -> true
                    }
                if (!success) {
                    diagnostic("Preferred-video service write failed; playback continues.")
                    return@withLock
                }
                if (
                    action is PlaybackPersistenceAction.SaveMapping &&
                    nowPlayingVisible &&
                    audio.queue.value.currentItem?.id == action.trackId
                ) {
                    preferredLookup.publishMapped(action.trackId, candidate)
                }
            }
        }
    }

    private fun onPlayerEnded(state: NativeVideoPlayerState.Ended) {
        if (!isCurrentSession(state.session)) return
        val shouldContinuePlaylist =
            _videoPreferred.value &&
                sessionAudioTrackId == audio.queue.value.currentItem?.id &&
                audio.queue.value.toVideoQueuePosition()?.isPlaylist == true
        if (shouldContinuePlaylist) {
            moveFromVideoToQueue(audio::next)
        } else {
            clearVideoSession()
            returnToNowPlayingChannel.trySend(Unit)
        }
    }

    @Suppress("ReturnCount")
    private fun onPlayerError(
        source: NativeVideoPlayer,
        state: NativeVideoPlayerState.Error,
    ) {
        if (!isCurrentSession(state.session)) return
        val fallbackToAudio =
            automaticVideoSession &&
                _videoPreferred.value &&
                sessionAudioTrackId == audio.queue.value.currentItem?.id
        source.release()
        activeCandidate = null
        sessionAudioTrackId = null
        sessionQueuePosition = null
        automaticVideoSession = false
        recordedVideoIdForSession = null
        playbackPersistenceAction = PlaybackPersistenceAction.None
        if (fallbackToAudio) {
            generation += 1
            _uiState.value = availableIdleState()
            audio.play()
        } else {
            _uiState.value = VideoUiState.Error(state.session.trackId, state.session.generation, state.message)
        }
    }

    private fun releaseVideoPlayer() {
        searchJob?.cancel()
        activePlayer().release()
    }

    private fun currentVideoWasPlaying(): Boolean =
        (activePlayer().state.value is NativeVideoPlayerState.Playing) ||
            ((_uiState.value as? VideoUiState.Playing)?.isPlaying == true)

    private fun activePlayer(): NativeVideoPlayer = _surfacePlayer.value

    private fun isCurrent(
        trackId: String,
        requestGeneration: Long,
    ): Boolean = generation == requestGeneration && audio.queue.value.currentItem?.id == trackId

    private fun isCurrentSession(session: VideoSessionKey): Boolean =
        generation == session.generation && _uiState.value.trackId == session.trackId

    private fun availableIdleState(): VideoUiState = VideoUiState.Idle

    private fun enableVideoPreferredMode(position: VideoQueuePosition) {
        _videoPreferred.value = true
        _uiState.value = availableIdleState()
        val preferred = preferredVideoState.value
        if (preferred is PreferredVideoState.Mapped && preferred.trackId == position.trackId) {
            applyVideoPreference(position, preferred, resumeAudioIfMissing = false)
        } else {
            preferredLookup.restart(position)
        }
    }

    private fun disableVideoPreferredMode() {
        _videoPreferred.value = false
        val resumeAfterLookup = preferredLookup.cancelAndTakeAudioResume()
        resumeAudioForNextQueuePosition = false
        val state = _uiState.value
        val positionMs = (state as? VideoUiState.Playing)?.positionMs ?: 0L
        val resumeCurrentAudio =
            sessionAudioTrackId == audio.queue.value.currentItem?.id &&
                audio.queue.value.toVideoQueuePosition()?.isPlaylist == true
        if (state.isVideoSessionActive || resumeCurrentAudio) clearVideoSession()
        if (resumeCurrentAudio) {
            audio.seekTo(positionMs)
            audio.play()
            returnToNowPlayingChannel.trySend(Unit)
        } else if (resumeAfterLookup) {
            audio.play()
        }
        if (nowPlayingVisible) preferredLookup.restart(audio.queue.value.toVideoQueuePosition())
    }

    private fun applyVideoPreference(
        position: VideoQueuePosition,
        preferred: PreferredVideoState,
        resumeAudioIfMissing: Boolean,
    ) {
        val canStartVideo =
            _videoPreferred.value &&
                position.isPlaylist &&
                audio.queue.value.toVideoQueuePosition() == position &&
                _uiState.value is VideoUiState.Idle
        if (canStartVideo && preferred is PreferredVideoState.Mapped) {
            startCandidate(
                candidate = preferred.candidate,
                trackId = position.trackId,
                boundAudioTrackId = position.trackId,
                persistenceAction = PlaybackPersistenceAction.MarkPlayed(position.trackId),
                automatic = true,
            )
        } else if (resumeAudioIfMissing) {
            audio.play()
        }
    }

    private fun moveFromVideoToQueue(move: () -> Unit): Boolean {
        if (!_uiState.value.isVideoSessionActive) return false
        val before = audio.queue.value.toVideoQueuePosition()
        val continueVideoPreference = _videoPreferred.value && before?.isPlaylist == true
        clearVideoSession()
        resumeAudioForNextQueuePosition = continueVideoPreference
        move()
        if (audio.queue.value.toVideoQueuePosition() == before) {
            resumeAudioForNextQueuePosition = false
            returnToNowPlayingChannel.trySend(Unit)
        }
        return true
    }
}

private class PreferredVideoLookupCoordinator(
    private val store: PreferredVideoStore,
    private val scope: CoroutineScope,
    private val canLookup: () -> Boolean,
    private val currentPosition: () -> VideoQueuePosition?,
    private val resumeAudio: () -> Unit,
    private val onResolved: (VideoQueuePosition, PreferredVideoState, Boolean) -> Unit,
) {
    private val mutableState =
        kotlinx.coroutines.flow.MutableStateFlow<PreferredVideoState>(PreferredVideoState.BackendUnavailable)
    val state: kotlinx.coroutines.flow.StateFlow<PreferredVideoState> = mutableState

    private var job: Job? = null
    private var generation = 0L
    private var pendingAudioResume = false

    fun restart(
        position: VideoQueuePosition?,
        resumeAudioIfMissing: Boolean = false,
    ) {
        val carryAudioResume = resumeAudioIfMissing || pendingAudioResume
        cancelCurrent()
        if (!canLookup() || position == null) {
            pendingAudioResume = false
            mutableState.value = PreferredVideoState.BackendUnavailable
            if (carryAudioResume) resumeAudio()
            return
        }
        pendingAudioResume = carryAudioResume
        mutableState.value = PreferredVideoState.Checking(position.trackId)
        val requestGeneration = generation
        job =
            scope.launch {
                val result = store.lookup(position.trackId)
                if (
                    requestGeneration != generation ||
                    !canLookup() ||
                    currentPosition() != position
                ) {
                    return@launch
                }
                pendingAudioResume = false
                val preferredState = result.toPreferredVideoState(position.trackId)
                mutableState.value = preferredState
                onResolved(position, preferredState, carryAudioResume)
            }
    }

    fun cancelAndTakeAudioResume(): Boolean {
        val resume = pendingAudioResume
        cancelCurrent()
        pendingAudioResume = false
        return resume
    }

    fun cancelAndReset() {
        cancelCurrent()
        pendingAudioResume = false
        mutableState.value = PreferredVideoState.BackendUnavailable
    }

    fun publishMapped(
        trackId: String,
        candidate: VideoCandidate,
    ) {
        mutableState.value = PreferredVideoState.Mapped(trackId, candidate)
    }

    private fun cancelCurrent() {
        job?.cancel()
        generation += 1L
    }
}

private data class VideoQueuePosition(
    val trackId: String,
    val index: Int,
    val sourcePlaylistName: String?,
    val queueTrackIds: List<String>,
) {
    val isPlaylist: Boolean = !sourcePlaylistName.isNullOrBlank()

    fun representsSameTrack(other: VideoQueuePosition?): Boolean =
        trackId == other?.trackId && sourcePlaylistName == other.sourcePlaylistName
}

private fun PlaybackQueue.toVideoQueuePosition(): VideoQueuePosition? {
    val track = currentItem ?: return null
    return VideoQueuePosition(
        trackId = track.id,
        index = currentIndex,
        sourcePlaylistName = sourcePlaylistName,
        queueTrackIds = items.map { it.id },
    )
}

private fun PreferredVideoLookupResult.toPreferredVideoState(trackId: String): PreferredVideoState =
    when (this) {
        is PreferredVideoLookupResult.Found ->
            if (video.trackId == trackId && video.provider == YOUTUBE_PROVIDER) {
                PreferredVideoState.Mapped(trackId, video.toVideoCandidate())
            } else {
                PreferredVideoState.BackendUnavailable
            }
        PreferredVideoLookupResult.Missing -> PreferredVideoState.Unmapped(trackId)
        PreferredVideoLookupResult.BackendUnavailable -> PreferredVideoState.BackendUnavailable
    }

private fun NativeVideoPlayerState.sessionOrNull(): VideoSessionKey? =
    when (this) {
        is NativeVideoPlayerState.Loading -> session
        is NativeVideoPlayerState.Ready -> session
        is NativeVideoPlayerState.Playing -> session
        is NativeVideoPlayerState.Paused -> session
        is NativeVideoPlayerState.Buffering -> session
        is NativeVideoPlayerState.Ended -> session
        is NativeVideoPlayerState.Error -> session
        NativeVideoPlayerState.Idle -> null
    }

private fun NativeVideoPlayerState.positionMsOrNull(): Long? =
    when (this) {
        is NativeVideoPlayerState.Playing -> positionMs
        is NativeVideoPlayerState.Paused -> positionMs
        is NativeVideoPlayerState.Buffering -> positionMs
        is NativeVideoPlayerState.Ended -> positionMs
        else -> null
    }

private fun NativeVideoPlayerState.positionMsOrZero(): Long = positionMsOrNull() ?: 0L

private fun NativeVideoPlayerState.durationMsOrZero(): Long =
    when (this) {
        is NativeVideoPlayerState.Ready -> durationMs
        is NativeVideoPlayerState.Playing -> durationMs
        is NativeVideoPlayerState.Paused -> durationMs
        is NativeVideoPlayerState.Buffering -> durationMs
        is NativeVideoPlayerState.Ended -> durationMs
        else -> 0L
    }

private sealed interface PlaybackPersistenceAction {
    data object None : PlaybackPersistenceAction

    data class SaveMapping(val trackId: String) : PlaybackPersistenceAction

    data class MarkPlayed(val trackId: String) : PlaybackPersistenceAction
}

private enum class DisclosureAction {
    RequestVideo,
    EnableVideoPreferred,
}

private const val YOUTUBE_SEARCH_RESULT_LIMIT = 25
private const val VIDEO_LOG_TAG = "TuneFlowVideo"
