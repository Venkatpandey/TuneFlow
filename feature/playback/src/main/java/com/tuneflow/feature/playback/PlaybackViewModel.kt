package com.tuneflow.feature.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackPhase
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlaybackStatus
import kotlinx.coroutines.CoroutineScope
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
)

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

            combine(
                playerManager.queue,
                playerManager.isPlaying,
                playerManager.playbackStatus,
                gatedTicker,
            ) { queue, isPlaying, playbackStatus, _ ->
                NowPlayingUiState(
                    queue = queue,
                    isPlaying = isPlaying,
                    positionMs = playerManager.currentPositionMs(),
                    durationMs = playerManager.durationMs(),
                    playbackStatus = playbackStatus,
                    statusMessage = buildStatusMessage(playbackStatus, isPlaying),
                )
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
}

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
