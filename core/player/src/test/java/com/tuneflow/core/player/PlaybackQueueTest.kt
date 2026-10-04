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

    @Test
    fun insertNext_insertsImmediatelyAfterCurrentItem() {
        val queue = PlaybackQueue(items, currentIndex = 0)
        val newItem = QueueItem("4", "T4", "A", "AL", null, "https://d")
        val updated = queue.insertNext(listOf(newItem))

        assertEquals(4, updated.items.size)
        assertEquals("4", updated.items[1].id)
        assertEquals("2", updated.items[2].id)
        assertEquals(0, updated.currentIndex)
    }

    @Test
    fun appendItems_appendsAtEnd() {
        val queue = PlaybackQueue(items, currentIndex = 0)
        val newItem = QueueItem("4", "T4", "A", "AL", null, "https://d")
        val updated = queue.appendItems(listOf(newItem))

        assertEquals(4, updated.items.size)
        assertEquals("4", updated.items[3].id)
        assertEquals(0, updated.currentIndex)
    }

    @Test
    fun removeAt_preventsRemovingCurrentItem() {
        val queue = PlaybackQueue(items, currentIndex = 1)
        val updated = queue.removeAt(1)

        assertEquals(3, updated.items.size)
        assertEquals(1, updated.currentIndex)
        assertEquals("2", updated.currentItem?.id)
    }

    @Test
    fun removeAt_pastItemAdjustsCurrentIndex() {
        val queue = PlaybackQueue(items, currentIndex = 2)
        val updated = queue.removeAt(0)

        assertEquals(2, updated.items.size)
        assertEquals(1, updated.currentIndex)
        assertEquals("3", updated.currentItem?.id)
    }

    @Test
    fun removeAt_lastItemAdjustsQueueCorrectly() {
        val queue = PlaybackQueue(items, currentIndex = 1)
        val updated = queue.removeAt(2)

        assertEquals(2, updated.items.size)
        assertEquals(1, updated.currentIndex)
        assertEquals("2", updated.currentItem?.id)
    }

    @Test
    fun moveItem_preventsMovingCurrentItemOrMovingToCurrentItem() {
        val queue = PlaybackQueue(items, currentIndex = 1)
        val tryMoveCurrent = queue.moveItem(1, 2)
        assertEquals(queue, tryMoveCurrent)

        val tryMoveToCurrent = queue.moveItem(0, 1)
        assertEquals(queue, tryMoveToCurrent)
    }

    @Test
    fun moveItem_reordersUpcomingItems() {
        val queue =
            PlaybackQueue(
                items = items + listOf(QueueItem("4", "T4", "A", "AL", null, "https://d")),
                currentIndex = 0,
            )
        // Move item at index 3 to index 2
        val updated = queue.moveItem(3, 2)
        assertEquals("4", updated.items[2].id)
        assertEquals("3", updated.items[3].id)
        assertEquals(0, updated.currentIndex)
    }

    @Test
    fun clearUpcoming_removesAllUpcomingItems() {
        val queue = PlaybackQueue(items, currentIndex = 0)
        val updated = queue.clearUpcoming()

        assertEquals(1, updated.items.size)
        assertEquals("1", updated.items[0].id)
        assertEquals(0, updated.currentIndex)
    }

    @Test
    fun editedQueue_survivesSerializationRoundTrip() {
        val queue =
            PlaybackQueue(items, currentIndex = 1, currentPositionMs = 12_345L)
                .insertNext(listOf(items.first().copy(title = "Repeated")))
                .removeAt(3)
        val encoded = Json.encodeToString(PlaybackQueue.serializer(), queue)
        val restored = Json.decodeFromString(PlaybackQueue.serializer(), encoded)

        assertEquals(3, restored.items.size)
        assertEquals(1, restored.currentIndex)
        assertEquals("Repeated", restored.items[2].title)
        assertEquals(12_345L, restored.currentPositionMs)
    }

    @Test
    fun legacyQueueMarker_isIgnoredWhenRestoringSavedQueue() {
        val queue = PlaybackQueue(items, currentIndex = 1)
        val encoded =
            Json.encodeToString(PlaybackQueue.serializer(), queue)
                .replace("\"currentIndex\":1", "\"currentIndex\":1,\"upNextStartIndex\":2")

        val restored = Json { ignoreUnknownKeys = true }.decodeFromString(PlaybackQueue.serializer(), encoded)

        assertEquals(queue, restored)
    }
}
