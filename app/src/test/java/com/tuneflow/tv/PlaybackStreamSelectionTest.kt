package com.tuneflow.tv

import com.tuneflow.core.network.TrackStreamOptions
import com.tuneflow.core.network.TrackSummary
import com.tuneflow.core.player.FLAC_AUDIO_MIME_TYPE
import com.tuneflow.core.player.MPEG_AUDIO_MIME_TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStreamSelectionTest {
    private val options = TrackStreamOptions(directUrl = "direct", fallbackMp3Url = "fallback")

    @Test
    fun flacMetadataIsCarriedIntoDirectQueueItem() {
        val track = track(contentType = "audio/flac", suffix = "flac")

        val item = track.toQueueItem(options, preferDirectWithFallback = true)

        assertEquals("direct", item.streamUrl)
        assertEquals("fallback", item.fallbackStreamUrl)
        assertEquals("FLAC", item.streamFormatLabel)
        assertEquals(FLAC_AUDIO_MIME_TYPE, item.streamMimeType)
        assertEquals(FLAC_AUDIO_MIME_TYPE, item.directStreamMimeType)
    }

    @Test
    fun mp3FallbackDeclaresMpegMimeTypeAndKeepsDirectMetadata() {
        val track = track(contentType = "audio/x-flac", suffix = "flac")

        val item = track.toQueueItem(options, preferDirectWithFallback = false)

        assertEquals("fallback", item.streamUrl)
        assertNull(item.fallbackStreamUrl)
        assertEquals("MP3", item.streamFormatLabel)
        assertEquals(MPEG_AUDIO_MIME_TYPE, item.streamMimeType)
        assertEquals(FLAC_AUDIO_MIME_TYPE, item.directStreamMimeType)
    }

    private fun track(
        contentType: String?,
        suffix: String?,
    ) = TrackSummary(
        id = "track-1",
        title = "Track",
        artist = "Artist",
        album = "Album",
        durationSec = 180,
        coverArtId = null,
        contentType = contentType,
        suffix = suffix,
    )
}
