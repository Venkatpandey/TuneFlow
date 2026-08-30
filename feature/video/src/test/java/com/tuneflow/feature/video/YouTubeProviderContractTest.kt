package com.tuneflow.feature.video

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI

class YouTubeProviderContractTest {
    @Test
    fun officialApiFixturesMapPublicEmbeddableNonLiveVideosIncludingAgeRestricted() =
        runBlocking {
            val bodies = ArrayDeque(listOf(SEARCH_FIXTURE, DETAILS_FIXTURE))
            val connections = mutableListOf<FakeHttpConnection>()
            val provider =
                YouTubeVideoProvider(
                    apiKey = "test-key",
                    packageName = "com.tuneflow.tv",
                    certificateSha1 = "ABC123",
                    connectionFactory = { url ->
                        FakeHttpConnection(url, bodies.removeFirst()).also(connections::add)
                    },
                )

            val candidates = provider.search(QUERY)

            assertEquals(listOf("safe", "age"), candidates.map(VideoCandidate::videoId))
            val safeCandidate = candidates.first()
            assertEquals("Artist & Song", safeCandidate.title)
            assertEquals(250_000L, safeCandidate.durationMs)
            assertEquals(123_456_789L, safeCandidate.viewCount)
            assertTrue(safeCandidate.musicCategory)
            assertEquals(0L, candidates.last().viewCount)
            assertEquals("test-key", connections.first().getRequestProperty("X-goog-api-key"))
            assertEquals("com.tuneflow.tv", connections.first().getRequestProperty("X-Android-Package"))
            assertEquals("ABC123", connections.first().getRequestProperty("X-Android-Cert"))
        }

    private companion object {
        val QUERY =
            VideoTrackQuery(
                trackId = "track",
                title = "Song",
                artist = "Artist",
                album = "Album",
                durationMs = 250_000L,
                regionCode = "DE",
                languageCode = "en",
            )
        val SEARCH_FIXTURE =
            """
            {"items":[
              {"id":{"videoId":"safe"}},
              {"id":{"videoId":"age"}},
              {"id":{"videoId":"live"}},
              {"id":{"videoId":"blocked-embed"}}
            ]}
            """.trimIndent()
        val DETAILS_FIXTURE =
            """
            {"items":[
              {
                "id":"safe",
                "snippet":{"title":"Artist &amp; Song","channelTitle":"Publisher","categoryId":"10","liveBroadcastContent":"none","thumbnails":{"high":{"url":"https://i.ytimg.com/safe.jpg"}}},
                "contentDetails":{"duration":"PT4M10S","contentRating":{},"regionRestriction":{}},
                "status":{"embeddable":true,"privacyStatus":"public"},
                "statistics":{"viewCount":"123456789"}
              },
              {
                "id":"age",
                "snippet":{"title":"Age restricted","channelTitle":"Publisher","categoryId":"10","liveBroadcastContent":"none"},
                "contentDetails":{"duration":"PT4M10S","contentRating":{"ytRating":"ytAgeRestricted"}},
                "status":{"embeddable":true,"privacyStatus":"public"},
                "statistics":{"viewCount":"unknown"}
              },
              {
                "id":"live",
                "snippet":{"title":"Live","channelTitle":"Publisher","categoryId":"10","liveBroadcastContent":"live"},
                "contentDetails":{"duration":"PT4M10S"},
                "status":{"embeddable":true,"privacyStatus":"public"}
              },
              {
                "id":"blocked-embed",
                "snippet":{"title":"Blocked","channelTitle":"Publisher","categoryId":"10","liveBroadcastContent":"none"},
                "contentDetails":{"duration":"PT4M10S"},
                "status":{"embeddable":false,"privacyStatus":"public"}
              }
            ]}
            """.trimIndent()
    }
}

private class FakeHttpConnection(
    url: String,
    private val body: String,
) : HttpURLConnection(URI(url).toURL()) {
    override fun connect() = Unit

    override fun disconnect() = Unit

    override fun usingProxy() = false

    override fun getResponseCode() = HTTP_OK

    override fun getInputStream() = ByteArrayInputStream(body.toByteArray())
}
