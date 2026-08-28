package com.tuneflow.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsApiIntegrationTest {
    @Test
    fun authenticatedLyricsEndpoints_parseStructuredAndLegacyResponses() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                responseBody(
                    """
                    "openSubsonicExtensions": [
                      {"name": "songLyrics", "versions": [1]}
                    ]
                    """.trimIndent(),
                ),
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                responseBody(
                    """
                    "lyricsList": {
                      "structuredLyrics": [
                        {
                          "kind": "main",
                          "lang": "eng",
                          "offset": 125,
                          "synced": true,
                          "line": [
                            {"start": 1000, "value": "First"},
                            {"start": 2000, "value": "Second"}
                          ]
                        },
                        {
                          "kind": "main",
                          "lang": "eng",
                          "synced": false,
                          "line": [{"value": "Plain"}]
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                responseBody(
                    """
                    "lyrics": {
                      "artist": "Artist",
                      "title": "Title",
                      "value": "Line one\nLine two"
                    }
                    """.trimIndent(),
                ),
            ),
        )
        server.start()

        try {
            val client = client(server)
            val extensions = runBlocking { client.getOpenSubsonicExtensions() } as NetworkResult.Success
            val structured = runBlocking { client.getLyricsBySongId("song id") } as NetworkResult.Success
            val legacy = runBlocking { client.getLyrics("Artist", "Title") } as NetworkResult.Success

            assertEquals("songLyrics", extensions.data.single().name)
            assertEquals(2, structured.data.size)
            assertEquals(125L, structured.data.first().offset)
            assertEquals(1_000L, structured.data.first().line.first().start)
            assertEquals("Line one\nLine two", legacy.data?.value)

            repeat(3) {
                val path = server.takeRequest().path.orEmpty()
                assertTrue(path.contains("u=user"))
                assertTrue(path.contains("t=token"))
                assertTrue(path.contains("s=salt"))
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun failedAndMalformedResponses_keepErrorDetailsDistinct() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "subsonic-response": {
                    "status": "failed",
                    "version": "1.16.1",
                    "error": {"code": 70, "message": "Lyrics not found"}
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(MockResponse().setBody("{"))
        server.start()

        try {
            val client = client(server)
            val failed = runBlocking { client.getLyricsBySongId("song-1") } as NetworkResult.Error
            val malformed = runBlocking { client.getLyricsBySongId("song-1") } as NetworkResult.Error

            assertEquals(70, failed.code)
            assertEquals(NetworkErrorKind.Server, failed.kind)
            assertEquals(NetworkErrorKind.Parsing, malformed.kind)
        } finally {
            server.shutdown()
        }
    }

    private fun client(server: MockWebServer): NavidromeClient =
        NavidromeClient(
            SessionData(
                serverUrl = server.url("/").toString(),
                username = "user",
                token = "token",
                salt = "salt",
            ),
        )

    private fun responseBody(content: String): String =
        """
        {
          "subsonic-response": {
            "status": "ok",
            "version": "1.16.1",
            $content
          }
        }
        """.trimIndent()
}
