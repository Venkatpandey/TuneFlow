package com.tuneflow.core.network

import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URI

class NavidromeClientTest {
    @Test
    fun streamUrl_doesNotAddTranscodingParameters() {
        val client =
            NavidromeClient(
                SessionData(
                    serverUrl = "https://music.example.com",
                    username = "demo",
                    token = "token123",
                    salt = "salt456",
                ),
            )

        val url = client.streamUrl("track-1")
        val query = URI(url).rawQuery.orEmpty()

        assertFalse(query.contains("maxBitRate="))
        assertFalse(query.contains("format="))
    }
}
