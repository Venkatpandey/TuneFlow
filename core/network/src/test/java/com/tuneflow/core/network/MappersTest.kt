package com.tuneflow.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {
    @Test
    fun songStarred_isMappedToFavoriteBoolean() {
        val track = SongDto(id = "song-1", title = "Song", starred = "2026-09-10T08:00:00Z").toTrack()

        assertTrue(track.isFavorite)
    }

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
                contentType = "audio/flac",
                suffix = "flac",
            )

        val track = dto.toTrack()

        assertEquals("Unknown Artist", track.artist)
        assertEquals("Unknown Album", track.album)
        assertEquals(0, track.durationSec)
        assertEquals("audio/flac", track.contentType)
        assertEquals("flac", track.suffix)
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

    @Test
    fun artistDtoToSummary_mapsCoverArtAndArtistImageUrl() {
        val dto =
            ArtistDto(
                id = "art-1",
                name = "Coldplay",
                coverArt = "ar-coldplay",
                artistImageUrl = "https://images.example.com/coldplay.jpg",
                albumCount = 9,
            )

        val summary = dto.toSummary()

        assertEquals("art-1", summary.id)
        assertEquals("Coldplay", summary.name)
        assertEquals(9, summary.albumCount)
        assertEquals("ar-coldplay", summary.coverArtId)
        assertEquals("https://images.example.com/coldplay.jpg", summary.artistImageUrl)
    }

    @Test
    fun artistDetailDtoToDetail_mapsCoverArtAndArtistImageUrl() {
        val dto =
            ArtistDetailDto(
                id = "art-2",
                name = "Radiohead",
                coverArt = "ar-radiohead",
                artistImageUrl = "https://images.example.com/radiohead.jpg",
                albumCount = 9,
                album = listOf(AlbumDto(id = "alb-1", name = "OK Computer")),
            )

        val detail = dto.toDetail()

        assertEquals("art-2", detail.id)
        assertEquals("Radiohead", detail.name)
        assertEquals(9, detail.albumCount)
        assertEquals("ar-radiohead", detail.coverArtId)
        assertEquals("https://images.example.com/radiohead.jpg", detail.artistImageUrl)
        assertEquals(1, detail.albums.size)
    }
}
