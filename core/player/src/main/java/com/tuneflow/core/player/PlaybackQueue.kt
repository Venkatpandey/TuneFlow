package com.tuneflow.core.player

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

const val FLAC_AUDIO_MIME_TYPE = "audio/flac"
const val MPEG_AUDIO_MIME_TYPE = "audio/mpeg"

@Serializable
data class QueueItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artUrl: String? = null,
    val streamUrl: String,
    val fallbackStreamUrl: String? = null,
    val streamFormatLabel: String = "FLAC",
    val streamBitrateLabel: String = "Original",
    val durationMs: Long = 0L,
    val streamMimeType: String? = null,
    val directStreamMimeType: String? = null,
    val directStreamFormatLabel: String = "FLAC",
    @Transient val isFavorite: Boolean = false,
)

@Serializable
data class PlaybackQueue(
    val items: List<QueueItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val sourcePlaylistId: String? = null,
    val sourcePlaylistName: String? = null,
) {
    val currentItem: QueueItem?
        get() = items.getOrNull(currentIndex)

    fun replace(
        items: List<QueueItem>,
        startIndex: Int = 0,
        sourcePlaylistId: String? = null,
        sourcePlaylistName: String? = null,
    ): PlaybackQueue {
        if (items.isEmpty()) return PlaybackQueue()
        val clamped = startIndex.coerceIn(0, items.lastIndex)
        return copy(
            items = items,
            currentIndex = clamped,
            currentPositionMs = 0L,
            sourcePlaylistId = sourcePlaylistId?.trim()?.takeIf(String::isNotEmpty),
            sourcePlaylistName = sourcePlaylistName?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    fun next(positionMs: Long = 0L): PlaybackQueue {
        if (items.isEmpty()) return this
        val next = (currentIndex + 1).coerceAtMost(items.lastIndex)
        return copy(currentIndex = next, currentPositionMs = positionMs)
    }

    fun previous(positionMs: Long = 0L): PlaybackQueue {
        if (items.isEmpty()) return this
        val prev = (currentIndex - 1).coerceAtLeast(0)
        return copy(currentIndex = prev, currentPositionMs = positionMs)
    }

    fun seek(positionMs: Long): PlaybackQueue = copy(currentPositionMs = positionMs.coerceAtLeast(0L))

    /** Inserts [newItems] immediately after the current item (play next). */
    fun insertNext(newItems: List<QueueItem>): PlaybackQueue =
        when {
            newItems.isEmpty() -> this
            items.isEmpty() -> appendItems(newItems)
            else -> {
                val insertAt = currentIndex + 1
                val merged = items.toMutableList().also { it.addAll(insertAt, newItems) }
                copy(items = merged)
            }
        }

    /** Appends [newItems] at the end of the queue. */
    fun appendItems(newItems: List<QueueItem>): PlaybackQueue {
        if (newItems.isEmpty()) return this
        return copy(items = items + newItems)
    }

    /**
     * Removes the item at [index]. Rejects the request if [index] == [currentIndex].
     * Adjusts [currentIndex] to keep the active item selected.
     * Returns unchanged queue if the index is invalid or is the current item.
     */
    fun removeAt(index: Int): PlaybackQueue {
        if (index !in items.indices || index == currentIndex) return this
        val newItems = items.toMutableList().also { it.removeAt(index) }
        val newCurrentIndex = if (index < currentIndex) currentIndex - 1 else currentIndex
        return copy(
            items = newItems,
            currentIndex = newCurrentIndex,
        )
    }

    /**
     * Moves the item at [from] to [to]. Both indices must be valid and neither can be
     * the [currentIndex].
     * Returns unchanged queue if constraints are violated.
     */
    fun moveItem(
        from: Int,
        to: Int,
    ): PlaybackQueue {
        if (!canMoveItem(from, to)) return this
        val newItems = items.toMutableList()
        val item = newItems.removeAt(from)
        newItems.add(to, item)
        return copy(items = newItems)
    }

    private fun canMoveItem(
        from: Int,
        to: Int,
    ): Boolean {
        if (from == to || from <= currentIndex || to <= currentIndex) return false
        return from in items.indices && to in items.indices
    }

    /** Removes all items after the current item (clears upcoming items). */
    fun clearUpcoming(): PlaybackQueue {
        if (currentIndex >= items.lastIndex) return this
        return copy(
            items = items.subList(0, currentIndex + 1),
        )
    }
}

@Serializable
enum class PlaybackMode {
    Default,
    Shuffle,
    Loop,
}
