package com.tuneflow.core.player

import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val queue: StateFlow<PlaybackQueue>
    val isPlaying: StateFlow<Boolean>

    fun play()

    fun pause()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)

    fun currentPositionMs(): Long

    fun durationMs(): Long
}
