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

    fun playQueue(
        items: List<QueueItem>,
        startIndex: Int = 0,
        sourcePlaylistId: String? = null,
        sourcePlaylistName: String? = null,
        playWhenReady: Boolean = true,
    )

    fun playFromIndex(
        index: Int,
        playWhenReady: Boolean = true,
    )

    fun retryCurrent()

    fun stopAndClear()

    fun currentPositionMs(): Long

    fun durationMs(): Long

    fun cyclePlaybackMode()
}
