package com.tuneflow.tv

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.feature.browse.AlbumDetailScreen
import com.tuneflow.feature.browse.AlbumsScreen
import com.tuneflow.feature.browse.ArtistDetailScreen
import com.tuneflow.feature.browse.PlaylistsScreen
import com.tuneflow.feature.browse.SearchScreen
import com.tuneflow.feature.playback.NowPlayingScreen

@Composable
internal fun ShellContent(
    currentSection: NavSection,
    selectedAlbumId: String?,
    selectedArtistId: String?,
    showNowPlaying: Boolean,
    preselectedPlaylistId: String?,
    playbackQueue: PlaybackQueue,
    homeViewModel: HomeViewModel,
    albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel,
    albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel,
    artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel,
    playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel,
    searchViewModel: com.tuneflow.feature.browse.SearchViewModel,
    playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel,
    streamModeLabel: String,
    onCycleStreamMode: () -> Unit,
    autoFocusNowPlayingTransport: Boolean,
    onNowPlayingAutoFocusConsumed: () -> Unit,
    onOpenAlbum: (String, NavSection) -> Unit,
    onOpenArtist: (String, NavSection) -> Unit,
    onOpenSection: (NavSection) -> Unit,
    onOpenPlaylist: (String?) -> Unit,
    onPreselectedPlaylistConsumed: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onPlayTracks: (List<com.tuneflow.core.network.TrackSummary>, Int) -> Unit,
    onShuffleTracks: (List<com.tuneflow.core.network.TrackSummary>) -> Unit,
) {
    val screenKey = shellScreenKey(currentSection, selectedAlbumId, selectedArtistId, showNowPlaying)

    Crossfade(targetState = screenKey, label = "shell-content") { targetScreen ->
        when {
            targetScreen == NOW_PLAYING_SCREEN_KEY -> {
                NowPlayingScreen(
                    viewModel = playbackViewModel,
                    streamModeLabel = streamModeLabel,
                    onCycleStreamMode = onCycleStreamMode,
                    autoFocusTransport = autoFocusNowPlayingTransport,
                    onAutoFocusConsumed = onNowPlayingAutoFocusConsumed,
                )
            }
            targetScreen.startsWith("album:") -> {
                AlbumDetailScreen(
                    albumId = targetScreen.removePrefix("album:"),
                    viewModel = albumDetailViewModel,
                    onPlayAlbum = onPlayTracks,
                    onShuffleAlbum = onShuffleTracks,
                )
            }
            targetScreen.startsWith("artist:") -> {
                ArtistDetailScreen(
                    artistId = targetScreen.removePrefix("artist:"),
                    viewModel = artistDetailViewModel,
                    onOpenAlbum = { onOpenAlbum(it, currentSection) },
                )
            }
            targetScreen == NavSection.Home.name -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    playbackQueue = playbackQueue,
                    onOpenAlbum = { onOpenAlbum(it, NavSection.Home) },
                    onOpenArtist = { onOpenArtist(it, NavSection.Home) },
                    onOpenAlbums = { onOpenSection(NavSection.Albums) },
                    onOpenPlaylists = onOpenPlaylist,
                    onOpenSearch = { onOpenSection(NavSection.Search) },
                    onOpenNowPlaying = onOpenNowPlaying,
                    onPlayTracks = onPlayTracks,
                )
            }
            targetScreen == NavSection.Albums.name -> {
                AlbumsScreen(
                    viewModel = albumsViewModel,
                    onAlbumSelected = { onOpenAlbum(it, NavSection.Albums) },
                )
            }
            targetScreen == NavSection.Playlists.name -> {
                PlaylistsScreen(
                    viewModel = playlistsViewModel,
                    preselectedPlaylistId = preselectedPlaylistId,
                    onPreselectedPlaylistConsumed = onPreselectedPlaylistConsumed,
                    onPlayTracks = onPlayTracks,
                    onShuffleTracks = onShuffleTracks,
                )
            }
            targetScreen == NavSection.Search.name -> {
                SearchScreen(
                    viewModel = searchViewModel,
                    onOpenArtist = { onOpenArtist(it, NavSection.Search) },
                    onOpenAlbum = { onOpenAlbum(it, NavSection.Search) },
                    onPlayTracks = onPlayTracks,
                )
            }
        }
    }
}
