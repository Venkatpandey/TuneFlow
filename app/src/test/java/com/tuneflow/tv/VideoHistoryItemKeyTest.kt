package com.tuneflow.tv

import com.tuneflow.feature.video.VideoHistoryEntry
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VideoHistoryItemKeyTest {
    @Test
    fun tracksSharingPreferredVideoStillHaveUniqueUiKeys() {
        val first = videoHistoryEntry(trackId = "track-1")
        val second = videoHistoryEntry(trackId = "track-2")

        assertNotEquals(first.videoHistoryItemKey(), second.videoHistoryItemKey())
    }
}

private fun videoHistoryEntry(trackId: String) =
    VideoHistoryEntry(
        trackId = trackId,
        provider = "youtube",
        videoId = "shared-video",
        title = "Title",
        publisher = "Publisher",
        thumbnailUrl = null,
        durationMs = 1L,
        viewCount = 1L,
        mappingUpdatedAt = "2026-09-16T00:00:00Z",
        lastPlayedAt = "2026-09-16T00:00:00Z",
    )
