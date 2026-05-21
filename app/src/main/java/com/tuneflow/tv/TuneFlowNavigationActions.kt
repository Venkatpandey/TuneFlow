package com.tuneflow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.tuneflow.feature.browse.HomeCategoryKind

internal class TuneFlowNavigationActions(
    private val clearPlaylistSelection: () -> Unit,
    private val updateShellState: ((TuneFlowShellState) -> TuneFlowShellState) -> Unit,
) {
    fun openSection(section: NavSection) {
        clearPlaylistSelection()
        updateShellState { it.openSection(section) }
    }

    fun openAlbum(
        albumId: String,
        source: NavSection,
    ) {
        updateShellState { it.openAlbum(albumId, source) }
    }

    fun openArtist(
        artistId: String,
        source: NavSection,
    ) {
        updateShellState { it.openArtist(artistId, source) }
    }

    fun openNowPlaying() {
        updateShellState { it.openNowPlaying() }
    }

    fun openHomeCategory(category: HomeCategoryKind) {
        updateShellState { it.openHomeCategory(category) }
    }

    fun openPlaylist(playlistId: String?) {
        updateShellState { it.openPlaylist(playlistId) }
    }

    fun closeNowPlaying() {
        updateShellState { it.closeNowPlaying() }
    }

    fun closeAlbum() {
        updateShellState { it.closeAlbum() }
    }

    fun closeArtist() {
        updateShellState { it.closeArtist() }
    }

    fun goHome() {
        updateShellState { it.goHome() }
    }

    fun returnToHomeCategory() {
        updateShellState { it.returnToHomeCategory() }
    }
}

@Composable
internal fun ShellBackHandler(
    showNowPlaying: Boolean,
    selectedHomeCategory: HomeCategoryKind?,
    selectedAlbumId: String?,
    selectedArtistId: String?,
    currentSection: NavSection,
    onCloseNowPlaying: () -> Unit,
    onCloseAlbum: () -> Unit,
    onCloseArtist: () -> Unit,
    onReturnToHomeCategory: () -> Unit,
    onGoHome: () -> Unit,
    onRequestExit: () -> Unit,
) {
    BackHandler {
        when {
            showNowPlaying -> onCloseNowPlaying()
            selectedAlbumId != null -> onCloseAlbum()
            selectedArtistId != null -> onCloseArtist()
            selectedHomeCategory != null && currentSection != NavSection.Home -> onReturnToHomeCategory()
            selectedHomeCategory != null -> onGoHome()
            currentSection != NavSection.Home -> onGoHome()
            else -> onRequestExit()
        }
    }
}

internal fun shellScreenKey(
    currentSection: NavSection,
    selectedHomeCategory: HomeCategoryKind?,
    selectedAlbumId: String?,
    selectedArtistId: String?,
    showNowPlaying: Boolean,
): String {
    return when {
        showNowPlaying -> NOW_PLAYING_SCREEN_KEY
        selectedAlbumId != null -> "album:$selectedAlbumId"
        selectedArtistId != null -> "artist:$selectedArtistId"
        currentSection != NavSection.Home -> currentSection.name
        selectedHomeCategory != null -> homeCategoryScreenKey(selectedHomeCategory)
        else -> NavSection.Home.name
    }
}
