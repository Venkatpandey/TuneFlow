package com.tuneflow.tv

import androidx.compose.runtime.saveable.listSaver

internal data class TuneFlowShellState(
    val currentSection: NavSection = NavSection.Home,
    val selectedAlbumId: String? = null,
    val selectedArtistId: String? = null,
    val albumSourceSection: NavSection = NavSection.Home,
    val artistSourceSection: NavSection = NavSection.Home,
    val preselectedPlaylistId: String? = null,
    val showNowPlaying: Boolean = false,
    val autoFocusNowPlayingTransport: Boolean = false,
    val showExitPrompt: Boolean = false,
    val lastExitPromptAt: Long = 0L,
) {
    companion object {
        val Saver =
            listSaver<TuneFlowShellState, Any?>(
                save = {
                    listOf(
                        it.currentSection.name,
                        it.selectedAlbumId,
                        it.selectedArtistId,
                        it.albumSourceSection.name,
                        it.artistSourceSection.name,
                        it.preselectedPlaylistId,
                        it.showNowPlaying,
                        it.autoFocusNowPlayingTransport,
                        it.showExitPrompt,
                        it.lastExitPromptAt,
                    )
                },
                restore = {
                    TuneFlowShellState(
                        currentSection = NavSection.valueOf(it[0] as String),
                        selectedAlbumId = it[1] as String?,
                        selectedArtistId = it[2] as String?,
                        albumSourceSection = NavSection.valueOf(it[3] as String),
                        artistSourceSection = NavSection.valueOf(it[4] as String),
                        preselectedPlaylistId = it[5] as String?,
                        showNowPlaying = it[6] as Boolean,
                        autoFocusNowPlayingTransport = it[7] as Boolean,
                        showExitPrompt = it[8] as Boolean,
                        lastExitPromptAt = it[9] as Long,
                    )
                },
            )
    }
}
