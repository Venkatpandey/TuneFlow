@file:Suppress("MatchingDeclarationName")

package com.tuneflow.tv

import com.tuneflow.feature.browse.HomeCategoryKind

internal enum class NavSection { Home, Albums, Playlists, Search }

internal const val EXIT_CONFIRM_TIMEOUT_MS = 2000L

internal sealed interface ShellDestination {
    data object Home : ShellDestination

    data object Albums : ShellDestination

    data object Playlists : ShellDestination

    data object Search : ShellDestination

    data class HomeCategory(val category: HomeCategoryKind) : ShellDestination

    data class Album(val albumId: String) : ShellDestination

    data class Artist(val artistId: String) : ShellDestination

    data object NowPlaying : ShellDestination
}

internal fun NavSection.toDestination(): ShellDestination =
    when (this) {
        NavSection.Home -> ShellDestination.Home
        NavSection.Albums -> ShellDestination.Albums
        NavSection.Playlists -> ShellDestination.Playlists
        NavSection.Search -> ShellDestination.Search
    }
