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
}
