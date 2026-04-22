package com.tuneflow.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test
    fun songToTrack_fillsMissingFields() {
        val dto =
            SongDto(
                id = "s1",
                title = "Track 1",
                artist = null,
                album = null,
                duration = null,
                coverArt = null,
            )

        val track = dto.toTrack()

        assertEquals("Unknown Artist", track.artist)
        assertEquals("Unknown Album", track.album)
        assertEquals(0, track.durationSec)
    }

    @Test
    fun playlistMapping_keepsTrackCount() {
        val dto =
            PlaylistDetailDto(
                id = "p1",
                name = "Favorites",
                entry =
                    listOf(
                        SongDto("1", "A"),
                        SongDto("2", "B"),
                    ),
            )

        val mapped = dto.toDetail()
        assertEquals(2, mapped.tracks.size)
    }
}
