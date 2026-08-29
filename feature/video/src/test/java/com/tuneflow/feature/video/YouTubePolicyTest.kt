package com.tuneflow.feature.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue(url.contains("safeSearch=none"))
        assertFalse(url.contains("safeSearch=strict"))
        assertTrue(url.contains("videoEmbeddable=true"))
        assertTrue(url.contains("videoSyndicated=true"))
        assertTrue(url.contains("maxResults=25"))
        assertTrue(url.contains("regionCode=DE"))
        assertFalse(url.contains("key="))
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

    @Test
    fun softwareAv1CompatibilityScriptIsLimitedToOfficialPlayerOrigins() {
        assertTrue(AV1_CODEC_COMPATIBILITY_SCRIPT.contains("MediaSource.isTypeSupported"))
        assertTrue(AV1_CODEC_COMPATIBILITY_SCRIPT.contains("mediaCapabilities.decodingInfo"))
        assertTrue(
            YOUTUBE_PLAYER_ORIGINS ==
                setOf(
                    "https://www.youtube.com",
                    "https://www.youtube-nocookie.com",
                ),
        )
        assertFalse(YOUTUBE_PLAYER_ORIGINS.contains("*"))
    }
}
