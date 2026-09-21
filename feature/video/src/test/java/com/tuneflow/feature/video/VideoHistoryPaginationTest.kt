package com.tuneflow.feature.video

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoHistoryPaginationTest {
    @Test
    fun loadsHistoryLargerThanSingleResponseLimit() =
        runTest {
            MockWebServer().use { server ->
                val entries = (0 until 1500).map(::entry)
                assertTrue(Json.encodeToString(entries).length > 256 * 1024)
                entries.chunked(100).forEachIndexed { index, page ->
                    server.enqueue(pageResponse(page, ((index + 1) * 100).takeIf { it < entries.size }))
                }
                val store = RemotePreferredVideoStore(server.url("/").toString())

                assertTrue(store.refreshHistory())

                assertEquals(entries, store.history.value)
                assertEquals(15, server.requestCount)
                repeat(15) { index ->
                    assertEquals("/v1/videos/recent?limit=100&offset=${index * 100}", server.takeRequest().path)
                }
            }
        }

    @Test
    fun explicitLimitBoundsFinalPage() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(pageResponse((0 until 100).map(::entry), 100))
                server.enqueue(pageResponse((100 until 150).map(::entry), 150))
                val store = RemotePreferredVideoStore(server.url("/").toString())

                assertTrue(store.refreshHistory(150))

                assertEquals(150, store.history.value.size)
                assertEquals(2, server.requestCount)
                server.takeRequest()
                assertEquals("/v1/videos/recent?limit=50&offset=100", server.takeRequest().path)
            }
        }

    @Test
    fun failedLaterPagePreservesPreviousHistory() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(pageResponse(listOf(entry(999))))
                server.enqueue(pageResponse((0 until 100).map(::entry), 100))
                server.enqueue(MockResponse().setResponseCode(500))
                val store = RemotePreferredVideoStore(server.url("/").toString())
                assertTrue(store.refreshHistory())

                assertFalse(store.refreshHistory())

                assertEquals(listOf(entry(999)), store.history.value)
            }
        }

    @Test
    fun invalidContinuationDoesNotLoopOrReplaceHistory() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(pageResponse(listOf(entry(999))))
                server.enqueue(pageResponse(listOf(entry(0)), 0))
                val store = RemotePreferredVideoStore(server.url("/").toString())
                assertTrue(store.refreshHistory())

                assertFalse(store.refreshHistory())

                assertEquals(2, server.requestCount)
                assertEquals(listOf(entry(999)), store.history.value)
            }
        }

    @Test
    fun legacyResponseWithoutContinuationStopsAfterOnePage() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(pageResponse((0 until 100).map(::entry)))
                val store = RemotePreferredVideoStore(server.url("/").toString())

                assertTrue(store.refreshHistory())

                assertEquals(1, server.requestCount)
                assertEquals(100, store.history.value.size)
            }
        }

    private fun pageResponse(
        entries: List<VideoHistoryEntry>,
        nextOffset: Int? = null,
    ): MockResponse {
        val continuation = nextOffset?.let { ",\"nextOffset\":$it" }.orEmpty()
        return MockResponse().setBody("{\"apiVersion\":\"v1\",\"videos\":${Json.encodeToString(entries)}$continuation}")
    }

    private fun entry(index: Int) =
        VideoHistoryEntry(
            trackId = "track-$index",
            provider = "youtube",
            videoId = "video${index.toString().padStart(6, '0')}",
            title = "Video $index",
            publisher = "Artist",
            thumbnailUrl = "https://example.test/$index.jpg",
            durationMs = 180_000L,
            viewCount = 42L,
            mappingUpdatedAt = "2026-09-01T09:00:00Z",
            lastPlayedAt = "2026-09-01T10:00:00Z",
        )
}
