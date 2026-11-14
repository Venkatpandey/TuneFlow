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
}

@Serializable
enum class PlaybackMode {
    Default,
    Shuffle,
    Loop,
}
