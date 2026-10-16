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

    /**
     * Inserts [items] immediately after the current item without interrupting playback.
     * Has no effect if the queue is empty.
     */
    fun addToQueueNext(items: List<QueueItem>)

    /**
     * Appends [items] at the end of the queue without interrupting playback.
     */
    fun addToQueueEnd(items: List<QueueItem>)

    /**
     * Removes the item at [index] from the queue without interrupting playback.
     * Has no effect if [index] is the current item or out of bounds.
     */
    fun removeFromQueue(index: Int)

    /**
     * Moves the upcoming item at [from] to position [to] in the queue.
     * Has no effect if either index is the current item or out of bounds.
     */
    fun moveInQueue(
        from: Int,
        to: Int,
    )

    /**
     * Removes all items after the current item (clears upcoming items).
     * Current item, position, and play state are unaffected.
     */
    fun clearUpcoming()
}
