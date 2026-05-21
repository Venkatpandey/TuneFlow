package com.tuneflow.tv

import com.tuneflow.feature.browse.HomeCategoryKind

internal fun TuneFlowShellState.openSection(section: NavSection): TuneFlowShellState =
    copy(
        currentSection = section,
        selectedHomeCategory = null,
        selectedAlbumId = null,
        selectedArtistId = null,
        preselectedPlaylistId = null,
        showNowPlaying = false,
        autoFocusNowPlayingTransport = false,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openAlbum(
    albumId: String,
    source: NavSection,
): TuneFlowShellState =
    copy(
        currentSection = source,
        albumSourceSection = source,
        selectedAlbumId = albumId,
        selectedArtistId = null,
        showNowPlaying = false,
        autoFocusNowPlayingTransport = false,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openArtist(
    artistId: String,
    source: NavSection,
): TuneFlowShellState =
    copy(
        currentSection = source,
        artistSourceSection = source,
        selectedArtistId = artistId,
        selectedAlbumId = null,
        showNowPlaying = false,
        autoFocusNowPlayingTransport = false,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openNowPlaying(): TuneFlowShellState =
    copy(showNowPlaying = true, autoFocusNowPlayingTransport = false, showExitPrompt = false)

internal fun TuneFlowShellState.enableNowPlayingTransportFocus(): TuneFlowShellState =
    copy(showNowPlaying = true, autoFocusNowPlayingTransport = true, showExitPrompt = false)

internal fun TuneFlowShellState.openHomeCategory(category: HomeCategoryKind): TuneFlowShellState =
    copy(
        currentSection = NavSection.Home,
        selectedHomeCategory = category,
        selectedAlbumId = null,
        selectedArtistId = null,
        preselectedPlaylistId = null,
        showNowPlaying = false,
        autoFocusNowPlayingTransport = false,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.openPlaylist(playlistId: String?): TuneFlowShellState =
    copy(
        currentSection = NavSection.Playlists,
        preselectedPlaylistId = playlistId,
        selectedAlbumId = null,
        selectedArtistId = null,
        showNowPlaying = false,
        autoFocusNowPlayingTransport = false,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.closeNowPlaying(): TuneFlowShellState = copy(showNowPlaying = false)

internal fun TuneFlowShellState.closeAlbum(): TuneFlowShellState = copy(selectedAlbumId = null, currentSection = albumSourceSection)

internal fun TuneFlowShellState.closeArtist(): TuneFlowShellState = copy(selectedArtistId = null, currentSection = artistSourceSection)

internal fun TuneFlowShellState.returnToHomeCategory(): TuneFlowShellState =
    copy(
        currentSection = NavSection.Home,
        preselectedPlaylistId = null,
        selectedAlbumId = null,
        selectedArtistId = null,
        showNowPlaying = false,
        autoFocusNowPlayingTransport = false,
        showExitPrompt = false,
    )

internal fun TuneFlowShellState.goHome(): TuneFlowShellState = copy(currentSection = NavSection.Home, selectedHomeCategory = null)
