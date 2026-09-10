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

    @Test
    fun starAndUnstar_sendExactStringIds() {
        val server = MockWebServer()
        repeat(2) {
            server.enqueue(
                MockResponse().setBody(
                    """{"subsonic-response":{"status":"ok","version":"1.16.1"}}""",
                ),
            )
        }
        server.start()
        val client = clientFor(server)

        val starResult = kotlinx.coroutines.runBlocking { client.star("track/string-id:001") }
        val unstarResult = kotlinx.coroutines.runBlocking { client.unstar("track/string-id:001") }

        assertTrue(starResult is NetworkResult.Success)
        assertTrue(unstarResult is NetworkResult.Success)
        val starUrl = requireNotNull(server.takeRequest().requestUrl)
        val unstarUrl = requireNotNull(server.takeRequest().requestUrl)
        assertEquals("/rest/star.view", starUrl.encodedPath)
        assertEquals("track/string-id:001", starUrl.queryParameter("id"))
        assertEquals("/rest/unstar.view", unstarUrl.encodedPath)
        assertEquals("track/string-id:001", unstarUrl.queryParameter("id"))
        server.shutdown()
    }

    @Test
    fun playlistSong_parsesStarredTimestamp() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "subsonic-response": {
                    "status": "ok",
                    "version": "1.16.1",
                    "playlist": {
                      "id": "playlist-1",
                      "name": "Mix",
                      "entry": [
                        {"id": "track-1", "title": "Song", "starred": "2026-09-10T08:00:00Z"}
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        server.start()

        val result = kotlinx.coroutines.runBlocking { clientFor(server).getPlaylist("playlist-1") }

        assertTrue(result is NetworkResult.Success)
        assertEquals(
            "2026-09-10T08:00:00Z",
            (result as NetworkResult.Success<PlaylistDetailDto>).data.entry.single().starred,
        )
        server.shutdown()
    }

    @Test
    fun star_returnsSubsonicError() {
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

        val result = kotlinx.coroutines.runBlocking { clientFor(server).star("missing") }

        assertTrue(result is NetworkResult.Error)
        assertEquals("Track not found", (result as NetworkResult.Error).message)
        assertEquals(70, result.code)
        server.shutdown()
    }

    @Test
    fun unstar_returnsTransportFailure() {
        val server = MockWebServer()
        server.start()
        val client = clientFor(server)
        server.shutdown()

        val result = kotlinx.coroutines.runBlocking { client.unstar("track-id") }

        assertTrue(result is NetworkResult.Error)
        assertEquals(NetworkErrorKind.Network, (result as NetworkResult.Error).kind)
    }

    private fun clientFor(server: MockWebServer): NavidromeClient =
        NavidromeClient(
            SessionData(
                serverUrl = server.url("/").toString(),
                username = "user",
                token = "token",
                salt = "salt",
            ),
        )
}
