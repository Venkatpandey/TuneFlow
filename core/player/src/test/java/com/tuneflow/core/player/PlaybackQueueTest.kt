package com.tuneflow.core.player

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueTest {
    private val items =
        listOf(
            QueueItem("1", "T1", "A", "AL", null, "https://a"),
            QueueItem("2", "T2", "A", "AL", null, "https://b"),
            QueueItem("3", "T3", "A", "AL", null, "https://c"),
        )

    @Test
    fun replace_clampsStartIndex() {
        val queue = PlaybackQueue().replace(items, 99)
        assertEquals(2, queue.currentIndex)
        assertEquals("3", queue.currentItem?.id)
    }

    @Test
    fun replace_keepsPlaylistIdentityAcrossQueueNavigation() {
        val queue =
            PlaybackQueue().replace(
                items,
                sourcePlaylistId = "playlist-1",
                sourcePlaylistName = "Evening Mix",
            )

        assertEquals("playlist-1", queue.sourcePlaylistId)
        assertEquals("Evening Mix", queue.sourcePlaylistName)
        assertEquals("playlist-1", queue.next().sourcePlaylistId)
        assertEquals("Evening Mix", queue.next().sourcePlaylistName)
        assertEquals("playlist-1", queue.next().previous().sourcePlaylistId)
        assertEquals("Evening Mix", queue.next().previous().sourcePlaylistName)
    }

    @Test
    fun next_stopsAtLastItem() {
        val queue = PlaybackQueue(items, currentIndex = 2)
        val next = queue.next()
        assertEquals(2, next.currentIndex)
    }

    @Test
    fun previous_stopsAtZero() {
        val queue = PlaybackQueue(items, currentIndex = 0)
        val prev = queue.previous()
        assertEquals(0, prev.currentIndex)
    }

    @Test
    fun replace_emptyClearsQueue() {
        val queue =
            PlaybackQueue(
                items = items,
                currentIndex = 1,
                sourcePlaylistId = "playlist-1",
                sourcePlaylistName = "Evening Mix",
            ).replace(emptyList())
        assertNull(queue.currentItem)
        assertEquals(0, queue.currentIndex)
        assertNull(queue.sourcePlaylistId)
        assertNull(queue.sourcePlaylistName)
    }

    @Test
    fun favoriteFlag_isNotPersistedAsLocalCache() {
        val queue = PlaybackQueue(items = listOf(items.first().copy(isFavorite = true)))

        val encoded = Json.encodeToString(PlaybackQueue.serializer(), queue)
        val restored = Json.decodeFromString(PlaybackQueue.serializer(), encoded)

        assertFalse(encoded.contains("isFavorite"))
        assertFalse(requireNotNull(restored.currentItem).isFavorite)
    }
}
