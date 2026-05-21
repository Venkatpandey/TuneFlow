package com.tuneflow.tv

import androidx.compose.runtime.saveable.listSaver
import com.tuneflow.feature.browse.HomeCategoryKind

internal data class TuneFlowShellState(
    val currentSection: NavSection = NavSection.Home,
    val selectedHomeCategory: HomeCategoryKind? = null,
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
                        it.selectedHomeCategory?.name,
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
                        selectedHomeCategory = (it[1] as String?)?.let(HomeCategoryKind::valueOf),
                        selectedAlbumId = it[2] as String?,
                        selectedArtistId = it[3] as String?,
                        albumSourceSection = NavSection.valueOf(it[4] as String),
                        artistSourceSection = NavSection.valueOf(it[5] as String),
                        preselectedPlaylistId = it[6] as String?,
                        showNowPlaying = it[7] as Boolean,
                        autoFocusNowPlayingTransport = it[8] as Boolean,
                        showExitPrompt = it[9] as Boolean,
                        lastExitPromptAt = it[10] as Long,
                    )
                },
            )
    }
}
