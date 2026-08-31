package com.tuneflow.tv

import com.tuneflow.feature.browse.BrowseFocusTarget
import com.tuneflow.feature.browse.BrowseFocusTargetKind
import com.tuneflow.feature.browse.HomeCategoryKind

internal fun TuneFlowShellState.openSection(section: NavSection): TuneFlowShellState =
    copy(
        backStack = listOf(ShellStackEntry(section.toDestination())),
        preselectedPlaylistId = null,
        autoFocusNowPlayingTransport = false,
        pendingFocusRestore = null,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openAlbum(albumId: String): TuneFlowShellState =
    copy(
        backStack =
            backStack +
                ShellStackEntry(
                    destination = ShellDestination.Album(albumId),
                    returnFocus = BrowseFocusTarget(BrowseFocusTargetKind.Album, albumId),
                ),
        autoFocusNowPlayingTransport = false,
        pendingFocusRestore = null,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openArtist(artistId: String): TuneFlowShellState =
    copy(
        backStack =
            backStack +
                ShellStackEntry(
                    destination = ShellDestination.Artist(artistId),
                    returnFocus = BrowseFocusTarget(BrowseFocusTargetKind.Artist, artistId),
                ),
        autoFocusNowPlayingTransport = false,
        pendingFocusRestore = null,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openNowPlaying(): TuneFlowShellState =
    if (currentDestination == ShellDestination.NowPlaying) {
        this
    } else {
        copy(
            backStack = backStack + ShellStackEntry(ShellDestination.NowPlaying),
            autoFocusNowPlayingTransport = false,
            pendingFocusRestore = null,
            showExitPrompt = false,
        )
    }

internal fun TuneFlowShellState.openVideoHistory(): TuneFlowShellState =
    if (currentDestination == ShellDestination.VideoHistory) {
        this
    } else {
        copy(
            backStack = backStack + ShellStackEntry(ShellDestination.VideoHistory),
            autoFocusNowPlayingTransport = false,
            pendingFocusRestore = null,
            showExitPrompt = false,
        )
    }

internal fun TuneFlowShellState.enableNowPlayingTransportFocus(): TuneFlowShellState =
    openNowPlaying().copy(autoFocusNowPlayingTransport = true)

internal fun TuneFlowShellState.openHomeCategory(category: HomeCategoryKind): TuneFlowShellState =
    copy(
        backStack =
            backStack +
                ShellStackEntry(
                    destination = ShellDestination.HomeCategory(category),
                    returnFocus = BrowseFocusTarget(BrowseFocusTargetKind.HomeCategory, category.name),
                ),
        preselectedPlaylistId = null,
        autoFocusNowPlayingTransport = false,
        pendingFocusRestore = null,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openPlaylist(playlistId: String?): TuneFlowShellState =
    copy(
        backStack =
            if (currentDestination == ShellDestination.Playlists) {
                backStack
            } else {
                backStack +
                    ShellStackEntry(
                        destination = ShellDestination.Playlists,
                        returnFocus = playlistId?.let { BrowseFocusTarget(BrowseFocusTargetKind.Playlist, it) },
                    )
            },
        preselectedPlaylistId = playlistId,
        autoFocusNowPlayingTransport = false,
        pendingFocusRestore = null,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.popDestination(): TuneFlowShellState {
    if (backStack.size <= 1) return this
    val poppedEntry = backStack.last()
    return copy(
        backStack = backStack.dropLast(1),
        preselectedPlaylistId = null,
        autoFocusNowPlayingTransport = false,
        pendingFocusRestore = poppedEntry.returnFocus,
        showExitPrompt = false,
    )
}

internal fun TuneFlowShellState.goHome(): TuneFlowShellState = openSection(NavSection.Home)

internal fun TuneFlowShellState.consumeFocusRestore(): TuneFlowShellState = copy(pendingFocusRestore = null)
