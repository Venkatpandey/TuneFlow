package com.tuneflow.core.player

import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val queue: StateFlow<PlaybackQueue>
    val isPlaying: StateFlow<Boolean>
    val playbackStatus: StateFlow<PlaybackStatus>
    val playbackMode: StateFlow<PlaybackMode>

    fun play()

    fun pause()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)

    fun playFromIndex(index: Int)

    fun retryCurrent()

    fun stopAndClear()

    fun currentPositionMs(): Long

    fun durationMs(): Long

    fun cyclePlaybackMode()
}
