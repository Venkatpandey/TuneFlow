package com.tuneflow.feature.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class YouTubePolicyTest {
    private val query =
        VideoTrackQuery(
            trackId = "1",
            title = "Song & Dance",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            regionCode = "de",
            languageCode = "en",
        )

    @Test
    fun searchRequestIncludesRestrictedPublicYouTubeResultsThatCanPlayEmbedded() {
        val url = YouTubeRequests.search(query)

        assertTrue(url.startsWith("https://www.googleapis.com/youtube/v3/search?"))
        assertTrue(url.contains("type=video"))
        assertTrue(url.contains("order=viewCount"))
        assertTrue(url.contains("safeSearch=none"))
        assertFalse(url.contains("safeSearch=strict"))
        assertTrue(url.contains("videoEmbeddable=true"))
        assertTrue(url.contains("videoSyndicated=true"))
        assertTrue(url.contains("maxResults=25"))
        assertTrue(url.contains("regionCode=DE"))
        assertFalse(url.contains("key="))
        assertEquals("Song & Dance Artist", url.queryParameter("q"))
        assertFalse(url.contains("official+music+video"))
        assertTrue(YouTubeRequests.details(listOf("video")).contains("statistics"))
    }

    @Test
    fun durationParserHandlesHoursMinutesAndSeconds() {
        assertTrue(parseIso8601DurationMs("PT1H2M3S") == 3_723_000L)
        assertTrue(parseIso8601DurationMs("PT4M10S") == 250_000L)
    }

    @Test
    fun domainPolicyAllowsPlayerResourcesButBlocksArbitraryBrowsing() {
        assertTrue(VideoDomainPolicy.isAllowedResourceHost("r3---sn.googlevideo.com"))
        assertTrue(VideoDomainPolicy.isApprovedExternalHost("music.youtube.com"))
        assertFalse(VideoDomainPolicy.isAllowedResourceHost("adult.example"))
        assertFalse(VideoDomainPolicy.isApprovedExternalHost("example.com"))
    }
}

private fun String.queryParameter(name: String): String? =
    URI(this).rawQuery
        .split('&')
        .map { it.split('=', limit = 2) }
        .firstOrNull { it.firstOrNull() == name }
        ?.getOrNull(1)
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
