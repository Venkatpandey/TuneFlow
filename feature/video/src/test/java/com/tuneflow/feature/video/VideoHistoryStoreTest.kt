package com.tuneflow.feature.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoHistoryStoreTest {
    @Test
    fun repeatedVideoMovesToFrontWithoutDuplication() {
        val first = historyEntry("first", 1L)
        val second = historyEntry("second", 2L)

        val updated = updatedVideoHistory(listOf(first, second), first.copy(playedAtEpochMs = 3L))

        assertEquals(listOf("first", "second"), updated.map(VideoHistoryEntry::videoId))
        assertEquals(3L, updated.first().playedAtEpochMs)
    }

    @Test
    fun historyKeepsOnlyTwentyNewestVideos() {
        val existing = (0 until VIDEO_HISTORY_LIMIT).map { historyEntry("video-$it", it.toLong()) }

        val updated = updatedVideoHistory(existing, historyEntry("new", 100L))

        assertEquals(VIDEO_HISTORY_LIMIT, updated.size)
        assertEquals("new", updated.first().videoId)
        assertEquals("video-18", updated.last().videoId)
    }

    private fun historyEntry(
        id: String,
        playedAt: Long,
    ) = VideoHistoryEntry(
        videoId = id,
        title = "Title $id",
        publisher = "Publisher",
        thumbnailUrl = null,
        durationMs = 180_000L,
        viewCount = 1L,
        playedAtEpochMs = playedAt,
    )
}
