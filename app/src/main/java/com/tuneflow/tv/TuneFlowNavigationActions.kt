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

    fun openAlbum(albumId: String) {
        updateShellState { it.openAlbum(albumId) }
    }

    fun openArtist(artistId: String) {
        updateShellState { it.openArtist(artistId) }
    }

    fun openNowPlaying() {
        updateShellState { it.openNowPlaying() }
    }

    fun openNowPlayingWithTransportFocus() {
        updateShellState { it.enableNowPlayingTransportFocus() }
    }

    fun openHomeCategory(category: HomeCategoryKind) {
        updateShellState { it.openHomeCategory(category) }
    }

    fun openPlaylist(playlistId: String?) {
        updateShellState { it.openPlaylist(playlistId) }
    }

    fun popDestination() {
        updateShellState { it.popDestination() }
    }

    fun goHome() {
        updateShellState { it.goHome() }
    }
}

internal enum class ShellBackAction {
    PopDestination,
    GoHome,
    RequestExit,
}

internal fun resolveShellBackAction(state: TuneFlowShellState): ShellBackAction =
    when {
        state.backStack.size > 1 -> ShellBackAction.PopDestination
        state.currentDestination != ShellDestination.Home -> ShellBackAction.GoHome
        else -> ShellBackAction.RequestExit
    }

@Composable
internal fun ShellBackHandler(
    state: TuneFlowShellState,
    onPopDestination: () -> Unit,
    onGoHome: () -> Unit,
    onRequestExit: () -> Unit,
) {
    BackHandler {
        when (resolveShellBackAction(state)) {
            ShellBackAction.PopDestination -> onPopDestination()
            ShellBackAction.GoHome -> onGoHome()
            ShellBackAction.RequestExit -> onRequestExit()
        }
    }
}
