package com.tuneflow.tv

import androidx.compose.runtime.saveable.listSaver
import com.tuneflow.feature.browse.BrowseFocusTarget
import com.tuneflow.feature.browse.BrowseFocusTargetKind
import com.tuneflow.feature.browse.HomeCategoryKind
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class TuneFlowShellState(
    val backStack: List<ShellStackEntry> = listOf(ShellStackEntry(ShellDestination.Home)),
    val preselectedPlaylistId: String? = null,
    val autoFocusNowPlayingTransport: Boolean = false,
    val pendingFocusRestore: BrowseFocusTarget? = null,
    val showExitPrompt: Boolean = false,
    val lastExitPromptAt: Long = 0L,
) {
    val currentDestination: ShellDestination
        get() = backStack.lastOrNull()?.destination ?: ShellDestination.Home

    val currentSection: NavSection
        get() =
            backStack.asReversed().firstNotNullOfOrNull { entry ->
                when (entry.destination) {
                    ShellDestination.Home,
                    is ShellDestination.HomeCategory,
                    ShellDestination.VideoHistory,
                    -> NavSection.Home
                    ShellDestination.Albums -> NavSection.Albums
                    ShellDestination.Playlists -> NavSection.Playlists
                    ShellDestination.Search -> NavSection.Search
                    is ShellDestination.Album,
                    is ShellDestination.Artist,
                    ShellDestination.NowPlaying,
                    -> null
                }
            } ?: NavSection.Home

    val showNowPlaying: Boolean
        get() = currentDestination == ShellDestination.NowPlaying

    companion object {
        val Saver =
            listSaver<TuneFlowShellState, Any?>(
                save = {
                    listOf(
                        it.backStack.map(ShellStackEntry::encode),
                        it.preselectedPlaylistId,
                        it.autoFocusNowPlayingTransport,
                        it.pendingFocusRestore?.kind?.name,
                        it.pendingFocusRestore?.id,
                        it.showExitPrompt,
                        it.lastExitPromptAt,
                    )
                },
                restore = {
                    @Suppress("UNCHECKED_CAST")
                    val restoredBackStack = (it[0] as List<String>).map(ShellStackEntry::decode)
                    TuneFlowShellState(
                        backStack = restoredBackStack.ifEmpty { listOf(ShellStackEntry(ShellDestination.Home)) },
                        preselectedPlaylistId = it[1] as String?,
                        autoFocusNowPlayingTransport = it[2] as Boolean,
                        pendingFocusRestore =
                            (it[3] as String?)?.let { kind ->
                                BrowseFocusTarget(BrowseFocusTargetKind.valueOf(kind), it[4] as String)
                            },
                        showExitPrompt = it[5] as Boolean,
                        lastExitPromptAt = it[6] as Long,
                    )
                },
            )
    }
}

internal data class ShellStackEntry(
    val destination: ShellDestination,
    val returnFocus: BrowseFocusTarget? = null,
) {
    fun encode(): String {
        val destinationValue =
            when (destination) {
                ShellDestination.Home -> "home"
                ShellDestination.Albums -> "albums"
                ShellDestination.Playlists -> "playlists"
                ShellDestination.Search -> "search"
                is ShellDestination.HomeCategory -> "homeCategory:${destination.category.name}"
                is ShellDestination.Album -> "album:${destination.albumId.urlEncode()}"
                is ShellDestination.Artist -> "artist:${destination.artistId.urlEncode()}"
                ShellDestination.VideoHistory -> "videoHistory"
                ShellDestination.NowPlaying -> "nowPlaying"
            }
        val focusValue = returnFocus?.let { "${it.kind.name}:${it.id.urlEncode()}" }.orEmpty()
        return "$destinationValue|$focusValue"
    }

    companion object {
        fun decode(value: String): ShellStackEntry {
            val parts = value.split('|', limit = 2)
            val destinationValue = parts.first()
            val destination =
                when {
                    destinationValue == "home" -> ShellDestination.Home
                    destinationValue == "albums" -> ShellDestination.Albums
                    destinationValue == "playlists" -> ShellDestination.Playlists
                    destinationValue == "search" -> ShellDestination.Search
                    destinationValue == "videoHistory" -> ShellDestination.VideoHistory
                    destinationValue == "nowPlaying" -> ShellDestination.NowPlaying
                    destinationValue.startsWith("homeCategory:") ->
                        ShellDestination.HomeCategory(HomeCategoryKind.valueOf(destinationValue.substringAfter(':')))
                    destinationValue.startsWith("album:") ->
                        ShellDestination.Album(destinationValue.substringAfter(':').urlDecode())
                    destinationValue.startsWith("artist:") ->
                        ShellDestination.Artist(destinationValue.substringAfter(':').urlDecode())
                    else -> ShellDestination.Home
                }
            val focus =
                parts.getOrNull(1)
                    ?.takeIf(String::isNotBlank)
                    ?.let { encodedFocus ->
                        BrowseFocusTarget(
                            kind = BrowseFocusTargetKind.valueOf(encodedFocus.substringBefore(':')),
                            id = encodedFocus.substringAfter(':').urlDecode(),
                        )
                    }
            return ShellStackEntry(destination, focus)
        }
    }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

private fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.name())
