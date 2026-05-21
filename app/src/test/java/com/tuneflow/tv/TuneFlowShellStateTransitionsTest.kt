package com.tuneflow.tv

import com.tuneflow.feature.browse.HomeCategoryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TuneFlowShellStateTransitionsTest {
    @Test
    fun openHomeCategory_setsHomeCategoryScreenKey() {
        val state = TuneFlowShellState().openHomeCategory(HomeCategoryKind.Albums)

        assertEquals(HomeCategoryKind.Albums, state.selectedHomeCategory)
        assertEquals(
            homeCategoryScreenKey(HomeCategoryKind.Albums),
            shellScreenKey(
                currentSection = state.currentSection,
                selectedHomeCategory = state.selectedHomeCategory,
                selectedAlbumId = state.selectedAlbumId,
                selectedArtistId = state.selectedArtistId,
                showNowPlaying = state.showNowPlaying,
            ),
        )
    }

    @Test
    fun backFromAlbumOpenedInsideHomeCategory_returnsToCategory() {
        val categoryState = TuneFlowShellState().openHomeCategory(HomeCategoryKind.Playlists)
        val albumState = categoryState.openAlbum(albumId = "album-1", source = NavSection.Home)

        val closedState = albumState.closeAlbum()

        assertEquals(NavSection.Home, closedState.currentSection)
        assertEquals(HomeCategoryKind.Playlists, closedState.selectedHomeCategory)
    }

    @Test
    fun openPlaylistInsideHomeCategory_preservesCategoryContext() {
        val categoryState = TuneFlowShellState().openHomeCategory(HomeCategoryKind.Playlists)

        val playlistState = categoryState.openPlaylist("playlist-42")

        assertEquals(NavSection.Playlists, playlistState.currentSection)
        assertEquals(HomeCategoryKind.Playlists, playlistState.selectedHomeCategory)
        assertEquals("playlist-42", playlistState.preselectedPlaylistId)
        assertEquals(
            NavSection.Playlists.name,
            shellScreenKey(
                currentSection = playlistState.currentSection,
                selectedHomeCategory = playlistState.selectedHomeCategory,
                selectedAlbumId = playlistState.selectedAlbumId,
                selectedArtistId = playlistState.selectedArtistId,
                showNowPlaying = playlistState.showNowPlaying,
            ),
        )
    }

    @Test
    fun returnToHomeCategory_fromPlaylist_keepsCategoryFilterContext() {
        val playlistState =
            TuneFlowShellState()
                .openHomeCategory(HomeCategoryKind.Playlists)
                .openPlaylist("playlist-42")

        val returnedState = playlistState.returnToHomeCategory()

        assertEquals(NavSection.Home, returnedState.currentSection)
        assertEquals(HomeCategoryKind.Playlists, returnedState.selectedHomeCategory)
        assertNull(returnedState.preselectedPlaylistId)
    }

    @Test
    fun goHome_clearsSelectedHomeCategory() {
        val state = TuneFlowShellState().openHomeCategory(HomeCategoryKind.Favorites)

        val homeState = state.goHome()

        assertNull(homeState.selectedHomeCategory)
        assertEquals(NavSection.Home, homeState.currentSection)
    }
}
