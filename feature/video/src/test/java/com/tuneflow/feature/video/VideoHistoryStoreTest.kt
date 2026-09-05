package com.tuneflow.feature.video

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoHistoryStoreTest {
    @Test
    fun `runtime service URL enables downloaded app without rebuild`() =
        runTest {
            val server = MockWebServer()
            server.enqueue(MockResponse().setResponseCode(404))
            server.start()
            try {
                val store = RemotePreferredVideoStore("")
                assertEquals(PreferredVideoLookupResult.BackendUnavailable, store.lookup("track"))

                store.updateServiceUrl(server.url("/").toString())

                assertEquals(PreferredVideoLookupResult.Missing, store.lookup("track"))
                assertEquals("/v1/tracks/track/preferred-video", server.takeRequest().path)
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun historyRequestsUpToOneHundredVideos() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setBody("""{"apiVersion":"v1","videos":[]}"""),
                )
                val store = RemotePreferredVideoStore(server.url("/").toString())

                assertTrue(store.refreshHistory())
                assertEquals("/v1/videos/recent?limit=100", server.takeRequest().path)
            }
        }

    @Test
    fun repeatedTrackMovesToFrontWithoutDuplication() {
        val first = historyEntry("track-1", "aaaaaaaaaaa", "2026-09-01T10:00:00Z")
        val second = historyEntry("track-2", "bbbbbbbbbbb", "2026-09-01T11:00:00Z")

        val updated = updatedRemoteHistory(listOf(first, second), first.copy(lastPlayedAt = "2026-09-01T12:00:00Z"))

        assertEquals(listOf("track-1", "track-2"), updated.map(VideoHistoryEntry::trackId))
        assertEquals("2026-09-01T12:00:00Z", updated.first().lastPlayedAt)
    }

    @Test
    fun inMemoryHistoryKeepsOnlyOneHundredNewestMappings() {
        val existing =
            (0 until VIDEO_HISTORY_LIMIT).map {
                historyEntry("track-$it", "video${it.toString().padStart(6, '0')}", "2026-09-01T10:00:00Z")
            }

        val updated =
            updatedRemoteHistory(
                existing,
                historyEntry("new", "newvideo001", "2026-09-01T12:00:00Z"),
            )

        assertEquals(100, updated.size)
        assertEquals("new", updated.first().trackId)
        assertEquals("track-98", updated.last().trackId)
    }

    @Test
    fun lookupDecodesMappedVideoAndEncodesTrackAsOnePathSegment() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setResponseCode(200).setBody(videoEnvelope("track/1", "aaaaaaaaaaa")))
                val store = RemotePreferredVideoStore(server.url("/").toString())

                val result = store.lookup("track/1")

                assertTrue(result is PreferredVideoLookupResult.Found)
                assertEquals("aaaaaaaaaaa", (result as PreferredVideoLookupResult.Found).video.videoId)
                assertEquals("/v1/tracks/track%2F1/preferred-video", server.takeRequest().path)
            }
        }

    @Test
    fun lookupDistinguishesMissingFromUnavailable() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
                server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
                val store = RemotePreferredVideoStore(server.url("/").toString())

                assertEquals(PreferredVideoLookupResult.Missing, store.lookup("track-1"))
                assertEquals(PreferredVideoLookupResult.BackendUnavailable, store.lookup("track-1"))
            }
        }

    @Test
    fun successfulWriteUpdatesOnlyInMemoryHistoryAfterServerConfirmation() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setResponseCode(200).setBody(videoEnvelope("track-1", "aaaaaaaaaaa")))
                val store = RemotePreferredVideoStore(server.url("/").toString())

                val success = store.savePreferredVideo("track-1", candidate("aaaaaaaaaaa"))

                assertTrue(success)
                assertEquals(listOf("track-1"), store.history.value.map(VideoHistoryEntry::trackId))
                val request = server.takeRequest()
                assertEquals("PUT", request.method)
                assertTrue(request.body.readUtf8().contains("\"videoId\":\"aaaaaaaaaaa\""))
            }
        }

    private fun historyEntry(
        trackId: String,
        videoId: String,
        playedAt: String,
    ) = VideoHistoryEntry(
        trackId = trackId,
        provider = "youtube",
        videoId = videoId,
        title = "Title $videoId",
        publisher = "Publisher",
        thumbnailUrl = null,
        durationMs = 180_000L,
        viewCount = 1L,
        mappingUpdatedAt = "2026-09-01T09:00:00Z",
        lastPlayedAt = playedAt,
    )

    private fun candidate(videoId: String) =
        VideoCandidate(
            videoId = videoId,
            title = "Title",
            publisher = "Artist",
            thumbnailUrl = null,
            durationMs = 180_000L,
            musicCategory = true,
            viewCount = 42L,
        )

    private fun videoEnvelope(
        trackId: String,
        videoId: String,
    ): String =
        """
        {
          "apiVersion": "v1",
          "preferredVideo": {
            "trackId": "$trackId",
            "provider": "youtube",
            "videoId": "$videoId",
            "title": "Title",
            "publisher": "Artist",
            "thumbnailUrl": null,
            "durationMs": 180000,
            "viewCount": 42,
            "mappingUpdatedAt": "2026-09-01T09:00:00Z",
            "lastPlayedAt": "2026-09-01T10:00:00Z"
          }
        }
        """.trimIndent()
}
