package com.tuneflow.feature.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackMode
import com.tuneflow.core.player.PlaybackPhase
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

data class NowPlayingUiState(
    val queue: PlaybackQueue = PlaybackQueue(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackStatus: PlaybackStatus = PlaybackStatus(),
    val statusMessage: String? = null,
    val playbackMode: PlaybackMode = PlaybackMode.Default,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModel(
    private val playerManager: PlaybackController,
    private val positionTicker: Flow<Unit> = defaultTickerFlow(),
    private val scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()
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

    fun playFromIndex(index: Int) = playerManager.playFromIndex(index)

    fun retry() = playerManager.retryCurrent()

    fun cyclePlaybackMode() = playerManager.cyclePlaybackMode()
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
        statusMessage = buildStatusMessage(playbackStatus, isPlaying),
        playbackMode = playbackMode,
    )

private fun buildStatusMessage(
    playbackStatus: PlaybackStatus,
    isPlaying: Boolean,
): String? {
    playbackStatus.errorMessage?.let { return it }

    return when {
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
