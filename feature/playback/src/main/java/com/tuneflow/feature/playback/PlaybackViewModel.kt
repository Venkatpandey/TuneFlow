package com.tuneflow.feature.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackMode
import com.tuneflow.core.player.PlaybackPhase
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlaybackStatus
import com.tuneflow.core.player.QueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

data class NowPlayingUiState(
    val queue: PlaybackQueue = PlaybackQueue(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackStatus: PlaybackStatus = PlaybackStatus(),
    val statusMessage: String? = null,
    val canRetry: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.Default,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModel(
    private val playerManager: PlaybackController,
    private val lyricsProvider: LyricsProvider = EmptyLyricsProvider,
    private val positionTicker: Flow<Unit> = defaultTickerFlow(),
    private val scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()
    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()
    private val isActive = MutableStateFlow(false)

    init {
        val scope = scopeOverride ?: viewModelScope
        scope.launch {
            val gatedTicker =
                isActive.flatMapLatest { active ->
                    if (active) {
                        positionTicker.onStart { emit(Unit) }
                    } else {
                        emptyFlow()
                    }
                }

            val playbackSnapshot =
                combine(
                    playerManager.queue,
                    playerManager.isPlaying,
                    playerManager.playbackStatus,
                    playerManager.playbackMode,
                ) { queue, isPlaying, playbackStatus, playbackMode ->
                    PlaybackSnapshot(
                        queue = queue,
                        isPlaying = isPlaying,
                        playbackStatus = playbackStatus,
                        playbackMode = playbackMode,
                    )
                }

            combine(
                playbackSnapshot,
                gatedTicker.onStart { emit(Unit) },
            ) { snapshot, _ ->
                snapshot.toUiState(playerManager)
            }.collect {
                _uiState.value = it
            }
        }
        scope.launch {
            playerManager.queue
                .map { it.currentItem }
                .distinctUntilChangedBy { it?.id }
                .collectLatest { track ->
                    if (track == null) {
                        _lyricsState.value = LyricsUiState.Idle
                        return@collectLatest
                    }

                    _lyricsState.value = LyricsUiState.Loading(track.id)
                    _lyricsState.value = lyricsProvider.load(track).toUiState(track.id)
                }
        }
    }

    fun setActive(active: Boolean) {
        isActive.value = active
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            playerManager.pause()
        } else {
            playerManager.play()
        }
    }

    fun play() = playerManager.play()

    fun pause() = playerManager.pause()

    fun next() = playerManager.next()

    fun previous() = playerManager.previous()

    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    fun playFromIndex(
        index: Int,
        playWhenReady: Boolean = true,
    ) = playerManager.playFromIndex(index, playWhenReady)

    fun retry() = playerManager.retryCurrent()

    fun cyclePlaybackMode() = playerManager.cyclePlaybackMode()

    // ── Queue mutation actions ────────────────────────────────────────────────

    private val _queueToast = MutableStateFlow<String?>(null)
    private var queueToastClearJob: Job? = null

    /** Short-lived non-modal announcement after a queue mutation (auto-clears after 3 s). */
    val queueToast: StateFlow<String?> = _queueToast.asStateFlow()

    fun addToQueueNext(items: List<QueueItem>) {
        if (items.isEmpty()) return
        playerManager.addToQueueNext(items)
        showQueueToast("Added to Up Next")
    }

    fun addToQueueEnd(items: List<QueueItem>) {
        if (items.isEmpty()) return
        playerManager.addToQueueEnd(items)
        showQueueToast(if (items.size == 1) "Added to queue" else "Added ${items.size} tracks to queue")
    }

    fun removeFromQueue(index: Int) {
        val queue = playerManager.queue.value
        if (index !in queue.items.indices || index == queue.currentIndex) return
        playerManager.removeFromQueue(index)
        showQueueToast("Removed from queue")
    }

    fun moveInQueue(
        from: Int,
        to: Int,
    ) {
        val queue = playerManager.queue.value
        if (from == to || from <= queue.currentIndex || to <= queue.currentIndex ||
            from !in queue.items.indices || to !in queue.items.indices
        ) {
            return
        }
        playerManager.moveInQueue(from, to)
        showQueueToast("Queue order updated")
    }

    fun clearUpcoming() {
        val queue = playerManager.queue.value
        if (queue.currentIndex >= queue.items.lastIndex) return
        playerManager.clearUpcoming()
        showQueueToast("Up Next cleared")
    }

    private fun showQueueToast(message: String) {
        queueToastClearJob?.cancel()
        _queueToast.value = message
        val scope = scopeOverride ?: viewModelScope
        queueToastClearJob =
            scope.launch {
                delay(3_000L)
                _queueToast.compareAndSet(message, null)
            }
    }
}

private fun LyricsLoadResult.toUiState(trackId: String): LyricsUiState =
    when (this) {
        is LyricsLoadResult.Available -> LyricsUiState.Available(trackId, lyrics)
        LyricsLoadResult.Empty -> LyricsUiState.Empty(trackId)
        LyricsLoadResult.Unsupported -> LyricsUiState.Unsupported(trackId)
        is LyricsLoadResult.NetworkFailure -> LyricsUiState.NetworkFailure(trackId, message)
        is LyricsLoadResult.ParsingFailure -> LyricsUiState.ParsingFailure(trackId, message)
    }

private data class PlaybackSnapshot(
    val queue: PlaybackQueue,
    val isPlaying: Boolean,
    val playbackStatus: PlaybackStatus,
    val playbackMode: PlaybackMode,
)

private fun PlaybackSnapshot.toUiState(playerManager: PlaybackController): NowPlayingUiState =
    NowPlayingUiState(
        queue = queue,
        isPlaying = isPlaying,
        positionMs = playerManager.currentPositionMs(),
        durationMs = playerManager.durationMs().takeIf { it > 0L } ?: queue.currentItem?.durationMs ?: 0L,
        playbackStatus = playbackStatus,
        statusMessage = buildStatusMessage(playbackStatus, isPlaying).takeIf { queue.currentItem != null },
        canRetry = queue.currentItem != null && !isPlaying && playbackStatus.errorCategory != null,
        playbackMode = playbackMode,
    )

private fun buildStatusMessage(
    playbackStatus: PlaybackStatus,
    isPlaying: Boolean,
): String? {
    return when {
        isPlaying -> null
        playbackStatus.errorMessage != null -> playbackStatus.errorMessage
        playbackStatus.expectedToPlay && !isPlaying && playbackStatus.phase == PlaybackPhase.Buffering ->
            "Buffering audio stream..."
        playbackStatus.expectedToPlay && !isPlaying && playbackStatus.phase == PlaybackPhase.Ready ->
            "Playback is ready but audio has not started."
        else -> null
    }
}

private fun defaultTickerFlow(intervalMs: Long = 1000L): Flow<Unit> =
    flow {
        while (true) {
            emit(Unit)
            delay(intervalMs)
        }
    }
