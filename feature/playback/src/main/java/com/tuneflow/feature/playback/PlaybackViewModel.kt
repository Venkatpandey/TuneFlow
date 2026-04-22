package com.tuneflow.feature.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

data class NowPlayingUiState(
    val queue: PlaybackQueue = PlaybackQueue(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

class PlaybackViewModel(
    private val playerManager: PlaybackController,
    private val positionTicker: Flow<Unit> = defaultTickerFlow(),
    private val scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    init {
        val scope = scopeOverride ?: viewModelScope
        scope.launch {
            combine(playerManager.queue, playerManager.isPlaying) { queue, isPlaying ->
                NowPlayingUiState(
                    queue = queue,
                    isPlaying = isPlaying,
                    positionMs = playerManager.currentPositionMs(),
                    durationMs = playerManager.durationMs(),
                )
            }.collect {
                _uiState.value = it
            }
        }

        scope.launch {
            positionTicker.collect {
                val current = _uiState.value
                _uiState.value =
                    current.copy(
                        positionMs = playerManager.currentPositionMs(),
                        durationMs = playerManager.durationMs(),
                    )
            }
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            playerManager.pause()
        } else {
            playerManager.play()
        }
    }

    fun next() = playerManager.next()

    fun previous() = playerManager.previous()

    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
}

private fun defaultTickerFlow(intervalMs: Long = 1000L): Flow<Unit> =
    flow {
        while (true) {
            emit(Unit)
            delay(intervalMs)
        }
    }
