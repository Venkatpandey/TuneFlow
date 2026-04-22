package com.tuneflow.core.player

import org.junit.Assert.assertEquals
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
        val queue = PlaybackQueue(items, currentIndex = 1).replace(emptyList())
        assertNull(queue.currentItem)
        assertEquals(0, queue.currentIndex)
    }
}
