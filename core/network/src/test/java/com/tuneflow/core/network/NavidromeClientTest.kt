package com.tuneflow.core.network

import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URI

class NavidromeClientTest {
    @Test
    fun directStreamUrl_requestsRawWithoutBitrateCap() {
        val client =
            NavidromeClient(
                SessionData(
                    serverUrl = "https://music.example.com",
                    username = "demo",
                    token = "token123",
                    salt = "salt456",
                ),
            )

        val url = client.streamOptions("track-1").directUrl
        val query = URI(url).rawQuery.orEmpty()

        assertFalse(query.contains("maxBitRate="))
        org.junit.Assert.assertTrue(query.contains("format=raw"))
    }

    @Test
    fun fallbackStreamUrl_requestsMp3WithoutBitrateCap() {
        val client =
            NavidromeClient(
                SessionData(
                    serverUrl = "https://music.example.com",
                    username = "demo",
                    token = "token123",
                    salt = "salt456",
                ),
            )

        val url = client.streamOptions("track-1").fallbackMp3Url
        val query = URI(url).rawQuery.orEmpty()

        org.junit.Assert.assertTrue(query.contains("maxBitRate=0"))
        org.junit.Assert.assertTrue(query.contains("format=mp3"))
    }
}
