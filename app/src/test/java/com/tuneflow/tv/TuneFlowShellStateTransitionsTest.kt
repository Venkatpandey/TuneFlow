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
    fun goHome_clearsSelectedHomeCategory() {
        val state = TuneFlowShellState().openHomeCategory(HomeCategoryKind.Favorites)

        val homeState = state.goHome()

        assertNull(homeState.selectedHomeCategory)
        assertEquals(NavSection.Home, homeState.currentSection)
    }
}
