package com.tuneflow.core.youtubenative

import com.google.android.exoplayer2.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeNativeLogicTest {
    @Test
    fun mapsSmartTubeSearchMetadata() {
        val mapped =
            mapSmartTubeFields(
                videoId = "abc",
                title = "Artist - Song",
                author = "fallback",
                secondTitle = "Artist VEVO • 1.2M views • 3 years ago",
                thumbnailUrl = "https://example.test/thumb.jpg",
                durationMs = 180_000L,
                isLive = false,
                isShort = false,
            )
        assertEquals("abc", mapped?.videoId)
        assertEquals("Artist VEVO", mapped?.channel)
        assertEquals(1_200_000L, mapped?.viewCount)
        assertEquals(180_000L, mapped?.durationMs)
    }

    @Test
    fun sourcePriorityIsDashThenSabrThenHlsThenDirect() {
        assertEquals(YouTubeSourceKind.Dash, selectSourceKind(true, true, true, true))
        assertEquals(YouTubeSourceKind.Sabr, selectSourceKind(false, true, true, true))
        assertEquals(YouTubeSourceKind.Hls, selectSourceKind(false, false, true, true))
        assertEquals(YouTubeSourceKind.Direct, selectSourceKind(false, false, false, true))
        assertNull(selectSourceKind(false, false, false, false))
    }

    @Test
    fun highestSupportedUsesDisplayHardwareAndCodecPreference() {
        val formats =
            listOf(
                format("avc-1080", 1920, 1080, CODEC_AVC, true),
                format("vp9-1080", 1920, 1080, CODEC_VP9, true),
                format("vp9-4k", 3840, 2160, CODEC_VP9, true),
            )
        val selected =
            YouTubeQualitySelector.highestSupported(
                formats,
                YouTubePlaybackCapabilities(1920, 1080, setOf(CODEC_AVC, CODEC_VP9), 30),
            )
        assertEquals("vp9-1080", selected?.id)
    }

    @Test
    fun api25AvoidsSoftwareAv1AndFallsBackToAvc() {
        val selected =
            YouTubeQualitySelector.highestSupported(
                listOf(
                    format("av1", 1920, 1080, CODEC_AV1, false),
                    format("avc", 1920, 1080, CODEC_AVC, true),
                ),
                YouTubePlaybackCapabilities(1920, 1080, setOf(CODEC_AV1, CODEC_AVC), 25),
            )
        assertEquals("avc", selected?.id)
    }

    @Test
    fun mapsReadyPlayingBufferingAndEndedStates() {
        val preparing = YouTubeNativePlayerState.Preparing("id", YouTubeSourceKind.Dash)
        assertEquals(YouTubeNativePlayerState.Ready("id", 100L), mapPlayerState("id", Player.STATE_READY, false, 0L, 100L, preparing))
        assertEquals(YouTubeNativePlayerState.Playing("id", 5L, 100L), mapPlayerState("id", Player.STATE_READY, true, 5L, 100L, preparing))
        assertEquals(
            YouTubeNativePlayerState.Buffering("id", 5L, 100L),
            mapPlayerState("id", Player.STATE_BUFFERING, true, 5L, 100L, preparing),
        )
        assertEquals(
            YouTubeNativePlayerState.Ended("id", 100L, 100L),
            mapPlayerState("id", Player.STATE_ENDED, false, 100L, 100L, preparing),
        )
    }

    @Test
    fun aspectFitPreservesVideoRatio() {
        assertEquals(1920 to 800, aspectFitSize(1920, 1080, 2.4f))
        assertEquals(608 to 1080, aspectFitSize(1920, 1080, 9f / 16f))
    }

    @Test
    fun parsesLocalizedViewCounts() {
        assertEquals(1_200_000L, parseYouTubeViewCount("1,2 Mio. Aufrufe"))
        assertEquals(950_000L, parseYouTubeViewCount("950K views"))
        assertEquals(1_234_567L, parseYouTubeViewCount("1.234.567 Aufrufe"))
        assertEquals(1_234L, parseYouTubeViewCount("1,234 views"))
    }

    private fun format(
        id: String,
        width: Int,
        height: Int,
        codec: String,
        hardware: Boolean,
    ) = YouTubeVideoFormat(id, width, height, 30f, 1_000, "video/$codec", codec, hardware)
}
