package com.tuneflow.tv

import com.tuneflow.feature.browse.BrowseFocusTarget
import com.tuneflow.feature.browse.BrowseFocusTargetKind
import com.tuneflow.feature.browse.HomeCategoryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneFlowShellStateTransitionsTest {
    @Test
    fun searchArtistAlbum_backPopsOneLayerAtATime() {
        val albumState =
            TuneFlowShellState()
                .openSection(NavSection.Search)
                .openArtist("artist-1")
                .openAlbum("album-1")

        assertEquals(ShellDestination.Album("album-1"), albumState.currentDestination)
        assertEquals(ShellBackAction.PopDestination, resolveShellBackAction(albumState))

        val artistState = albumState.popDestination()
        assertEquals(ShellDestination.Artist("artist-1"), artistState.currentDestination)
        assertEquals(BrowseFocusTarget(BrowseFocusTargetKind.Album, "album-1"), artistState.pendingFocusRestore)

        val searchState = artistState.popDestination()
        assertEquals(ShellDestination.Search, searchState.currentDestination)
        assertEquals(BrowseFocusTarget(BrowseFocusTargetKind.Artist, "artist-1"), searchState.pendingFocusRestore)
    }

    @Test
    fun nowPlaying_backReturnsToExactUnderlyingDetail() {
        val artistState =
            TuneFlowShellState()
                .openSection(NavSection.Search)
                .openArtist("artist-1")

        val nowPlayingState = artistState.openNowPlaying()
        val returnedState = nowPlayingState.popDestination()

        assertEquals(ShellDestination.Artist("artist-1"), returnedState.currentDestination)
        assertEquals(artistState.backStack, returnedState.backStack)
    }

    @Test
    fun nowPlayingTransportFocusCanBeRequestedWhenAlreadyOpen() {
        val state = TuneFlowShellState().openNowPlaying().enableNowPlayingTransportFocus()

        assertTrue(state.showNowPlaying)
        assertTrue(state.autoFocusNowPlayingTransport)
    }

    @Test
    fun playlistOpenedFromHomeCategory_returnsToCategory() {
        val playlistState =
            TuneFlowShellState()
                .openHomeCategory(HomeCategoryKind.Playlists)
                .openPlaylist("playlist-42")

        assertEquals(ShellDestination.Playlists, playlistState.currentDestination)
        assertEquals(NavSection.Playlists, playlistState.currentSection)

        val categoryState = playlistState.popDestination()
        assertEquals(ShellDestination.HomeCategory(HomeCategoryKind.Playlists), categoryState.currentDestination)
        assertEquals(BrowseFocusTarget(BrowseFocusTargetKind.Playlist, "playlist-42"), categoryState.pendingFocusRestore)
    }

    @Test
    fun homeCategory_backRestoresItsShowAllAction() {
        val categoryState = TuneFlowShellState().openHomeCategory(HomeCategoryKind.Albums)

        val homeState = categoryState.popDestination()

        assertEquals(ShellDestination.Home, homeState.currentDestination)
        assertEquals(
            BrowseFocusTarget(BrowseFocusTargetKind.HomeCategory, HomeCategoryKind.Albums.name),
            homeState.pendingFocusRestore,
        )
    }

    @Test
    fun videoHistoryOpensFromHomeAndBackReturnsHome() {
        val historyState = TuneFlowShellState().openVideoHistory()

        assertEquals(ShellDestination.VideoHistory, historyState.currentDestination)
        assertEquals(NavSection.Home, historyState.currentSection)
        assertEquals(ShellDestination.Home, historyState.popDestination().currentDestination)
    }

    @Test
    fun topLevelSection_backGoesHome_thenRequestsExit() {
        val albumsState = TuneFlowShellState().openSection(NavSection.Albums)

        assertEquals(ShellBackAction.GoHome, resolveShellBackAction(albumsState))

        val homeState = albumsState.goHome()
        assertEquals(ShellDestination.Home, homeState.currentDestination)
        assertEquals(ShellBackAction.RequestExit, resolveShellBackAction(homeState))
    }

    @Test
    fun topLevelSelection_resetsNestedStack() {
        val nestedState =
            TuneFlowShellState()
                .openSection(NavSection.Search)
                .openArtist("artist-1")
                .openAlbum("album-1")

        val playlistsState = nestedState.openSection(NavSection.Playlists)

        assertEquals(listOf(ShellStackEntry(ShellDestination.Playlists)), playlistsState.backStack)
        assertNull(playlistsState.pendingFocusRestore)
    }

    @Test
    fun stackEntryEncoding_roundTripsIdsAndFocus() {
        val entry =
            ShellStackEntry(
                destination = ShellDestination.Album("album:1 / favorite"),
                returnFocus = BrowseFocusTarget(BrowseFocusTargetKind.Album, "album:1 / favorite"),
            )

        assertEquals(entry, ShellStackEntry.decode(entry.encode()))
    }
}
