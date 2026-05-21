package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.TrackSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCategoryUiStateTest {
    @Test
    fun filteredFavorites_matchesAlbumsAndTracks() {
        val state =
            HomeCategoryUiState(
                category = HomeCategoryKind.Favorites,
                query = "night",
                favorites =
                    FavoritesBundle(
                        albums =
                            listOf(
                                AlbumSummary(id = "a1", title = "Night Drive", artist = "Chromatic", coverArtId = null),
                                AlbumSummary(id = "a2", title = "Morning Light", artist = "Aurora", coverArtId = null),
                            ),
                        tracks =
                            listOf(
                                TrackSummary(
                                    id = "t1",
                                    title = "Midnight Run",
                                    artist = "Neon",
                                    album = "Night Drive",
                                    durationSec = 200,
                                    coverArtId = null,
                                ),
                                TrackSummary(
                                    id = "t2",
                                    title = "Sunrise",
                                    artist = "Daybreak",
                                    album = "Morning Light",
                                    durationSec = 180,
                                    coverArtId = null,
                                ),
                            ),
                    ),
            )

        assertEquals(listOf("a1"), state.filteredFavorites.albums.map { it.id })
        assertEquals(listOf("t1"), state.filteredFavorites.tracks.map { it.id })
    }

    @Test
    fun filteredArtists_matchesNameIgnoringCase() {
        val state =
            HomeCategoryUiState(
                category = HomeCategoryKind.Artists,
                query = "pho",
                artists =
                    listOf(
                        ArtistSummary(id = "ar1", name = "Phoenix", albumCount = 6),
                        ArtistSummary(id = "ar2", name = "Daft Punk", albumCount = 4),
                    ),
            )

        assertEquals(listOf("ar1"), state.filteredArtists.map { it.id })
    }

    @Test
    fun filteredPlaylists_matchesPlaylistName() {
        val state =
            HomeCategoryUiState(
                category = HomeCategoryKind.Playlists,
                query = "focus",
                playlists =
                    listOf(
                        PlaylistSummary(id = "p1", name = "Deep Focus", songCount = 12, durationSec = 1200),
                        PlaylistSummary(id = "p2", name = "Workout Mix", songCount = 18, durationSec = 2200),
                    ),
            )

        assertEquals(listOf("p1"), state.filteredPlaylists.map { it.id })
    }
}
