package com.tuneflow.core.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavidromeApiIntegrationTest {
    @Test
    fun getAlbumList_parsesResponse() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "subsonic-response": {
                    "status": "ok",
                    "version": "1.16.1",
                    "albumList": {
                      "album": [
                        {"id": "a1", "name": "One", "artist": "Artist A"},
                        {"id": "a2", "name": "Two", "artist": "Artist B"}
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        server.start()

        val api = NetworkFactory.createApi(server.url("/").toString())
        val response =
            kotlinx.coroutines.runBlocking {
                api.getAlbumList(
                    size = 20,
                    offset = 0,
                    username = "u",
                    token = "t",
                    salt = "s",
                )
            }

        val albums = response.response.albumList?.album.orEmpty()
        assertEquals(2, albums.size)
        assertEquals("a1", albums.first().id)

        val request = server.takeRequest()
        assertTrue(request.path.orEmpty().contains("/rest/getAlbumList.view"))

        server.shutdown()
    }

    @Test
    fun scrobble_sendsSubmissionWithOriginalStartTime() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "subsonic-response": {
                    "status": "ok",
                    "version": "1.16.1"
                  }
                }
                """.trimIndent(),
            ),
        )
        server.start()

        val client =
            NavidromeClient(
                SessionData(
                    serverUrl = server.url("/").toString(),
                    username = "user",
                    token = "token",
                    salt = "salt",
                ),
            )
        val result = kotlinx.coroutines.runBlocking { client.scrobble("track/string-id", 1_725_000_123_456L) }

        assertTrue(result is NetworkResult.Success)
        val requestUrl = requireNotNull(server.takeRequest().requestUrl)
        assertEquals("/rest/scrobble.view", requestUrl.encodedPath)
        assertEquals("track/string-id", requestUrl.queryParameter("id"))
        assertEquals("1725000123456", requestUrl.queryParameter("time"))
        assertEquals("true", requestUrl.queryParameter("submission"))
        assertEquals("user", requestUrl.queryParameter("u"))

        server.shutdown()
    }

    @Test
    fun scrobble_returnsServerErrorWithoutThrowing() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "subsonic-response": {
                    "status": "failed",
                    "version": "1.16.1",
                    "error": {"code": 70, "message": "Track not found"}
                  }
                }
                """.trimIndent(),
            ),
        )
        server.start()

        val client =
            NavidromeClient(
                SessionData(
                    serverUrl = server.url("/").toString(),
                    username = "user",
                    token = "token",
                    salt = "salt",
                ),
            )
        val result = kotlinx.coroutines.runBlocking { client.scrobble("missing-track", 1_725_000_123_456L) }

        assertTrue(result is NetworkResult.Error)
        assertEquals("Track not found", (result as NetworkResult.Error).message)

        server.shutdown()
    }
}
