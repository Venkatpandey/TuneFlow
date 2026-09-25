package com.tuneflow.core.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPlayerManagerTest {
    @Test
    fun `flac mime type is preserved for extensionless stream url`() {
        val item =
            QueueItem(
                id = "flac-1",
                title = "Track",
                artist = "Artist",
                album = "Album",
                streamUrl = "https://music.example/rest/stream.view?id=flac-1&format=raw",
                streamFormatLabel = "FLAC",
                streamMimeType = FLAC_AUDIO_MIME_TYPE,
            )

        assertEquals(FLAC_AUDIO_MIME_TYPE, item.resolvedStreamMimeType())
    }

    @Test
    fun `legacy queue item infers flac mime type from format label`() {
        val item = QueueItem("flac-1", "Track", "Artist", "Album", streamUrl = "stream")

        assertEquals(FLAC_AUDIO_MIME_TYPE, item.resolvedStreamMimeType())
    }

    @Test
    fun `paused standby audio cannot switch to fallback and start playing`() {
        val item = QueueItem("track", "Track", "Artist", "Album", streamUrl = "direct", fallbackStreamUrl = "mp3")

        assertNull(item.audioFallback(expectedToPlay = false))
        val fallback = requireNotNull(item.audioFallback(expectedToPlay = true))
        assertEquals("mp3", fallback.streamUrl)
        assertEquals(MPEG_AUDIO_MIME_TYPE, fallback.streamMimeType)
        assertNull(fallback.audioFallback(expectedToPlay = true))
    }

    @Test
    fun `persists only for seek discontinuities`() {
        assertTrue(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_SEEK))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_AUTO_TRANSITION))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_REMOVE))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_SKIP))
        assertFalse(shouldPersistOnPositionDiscontinuity(Player.DISCONTINUITY_REASON_INTERNAL))
    }
}
