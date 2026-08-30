package com.tuneflow.tv

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntRect
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.feature.browse.AlbumDetailScreen
import com.tuneflow.feature.browse.AlbumsScreen
import com.tuneflow.feature.browse.ArtistDetailScreen
import com.tuneflow.feature.browse.HomeCategoryScreen
import com.tuneflow.feature.browse.PlaylistsScreen
import com.tuneflow.feature.browse.SearchScreen
import com.tuneflow.feature.playback.NowPlayingScreen

@Composable
internal fun ShellContent(
    currentDestination: ShellDestination,
    preselectedPlaylistId: String?,
    focusRestoreTarget: com.tuneflow.feature.browse.BrowseFocusTarget?,
    playbackQueue: PlaybackQueue,
    homeViewModel: HomeViewModel,
    albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel,
    homeCategoryViewModel: com.tuneflow.feature.browse.HomeCategoryViewModel,
    albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel,
    artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel,
    playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel,
    searchViewModel: com.tuneflow.feature.browse.SearchViewModel,
    playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel,
    videoViewModel: com.tuneflow.feature.video.VideoViewModel,
    streamModeLabel: String,
    onCycleStreamMode: () -> Unit,
    autoFocusNowPlayingTransport: Boolean,
    onNowPlayingAutoFocusConsumed: () -> Unit,
    onVideoViewportBoundsChanged: (IntRect?) -> Unit,
    onFocusRestoreConsumed: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSection: (NavSection) -> Unit,
    onOpenHomeCategory: (com.tuneflow.feature.browse.HomeCategoryKind) -> Unit,
    onOpenPlaylist: (String?) -> Unit,
    onPreselectedPlaylistConsumed: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenVideoHistory: () -> Unit,
    onPlayVideo: (com.tuneflow.feature.video.VideoHistoryEntry) -> Unit,
    onPlayTracks: (List<com.tuneflow.core.network.TrackSummary>, Int) -> Unit,
    onShuffleTracks: (List<com.tuneflow.core.network.TrackSummary>) -> Unit,
) {
    Crossfade(targetState = currentDestination, label = "shell-content") { targetScreen ->
        when (targetScreen) {
            ShellDestination.NowPlaying -> {
                NowPlayingScreen(
                    viewModel = playbackViewModel,
                    videoViewModel = videoViewModel,
                    streamModeLabel = streamModeLabel,
                    onCycleStreamMode = onCycleStreamMode,
                    autoFocusTransport = autoFocusNowPlayingTransport,
                    onAutoFocusConsumed = onNowPlayingAutoFocusConsumed,
                    onVideoViewportBoundsChanged = onVideoViewportBoundsChanged,
                )
            }
            is ShellDestination.Album -> {
                AlbumDetailScreen(
                    albumId = targetScreen.albumId,
                    viewModel = albumDetailViewModel,
                    onPlayAlbum = onPlayTracks,
                    onShuffleAlbum = onShuffleTracks,
                )
            }
            is ShellDestination.Artist -> {
                ArtistDetailScreen(
                    artistId = targetScreen.artistId,
                    viewModel = artistDetailViewModel,
                    focusRestoreTarget = focusRestoreTarget,
                    onFocusRestoreConsumed = onFocusRestoreConsumed,
                    onOpenAlbum = onOpenAlbum,
                )
            }
            ShellDestination.Home -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    playbackQueue = playbackQueue,
                    focusRestoreTarget = focusRestoreTarget,
                    onFocusRestoreConsumed = onFocusRestoreConsumed,
                    onOpenAlbum = onOpenAlbum,
                    onOpenArtist = onOpenArtist,
                    onOpenHomeCategory = onOpenHomeCategory,
                    onOpenAlbums = { onOpenSection(NavSection.Albums) },
                    onOpenPlaylists = onOpenPlaylist,
                    onOpenSearch = { onOpenSection(NavSection.Search) },
                    onOpenNowPlaying = onOpenNowPlaying,
                    onOpenVideoHistory = onOpenVideoHistory,
                    onPlayVideo = onPlayVideo,
                    onPlayTracks = onPlayTracks,
                )
            }
            ShellDestination.VideoHistory -> {
                VideoHistoryScreen(
                    viewModel = homeViewModel,
                    onPlayVideo = onPlayVideo,
                )
            }
            is ShellDestination.HomeCategory -> {
                HomeCategoryScreen(
                    category = targetScreen.category,
                    viewModel = homeCategoryViewModel,
                    focusRestoreTarget = focusRestoreTarget,
                    onFocusRestoreConsumed = onFocusRestoreConsumed,
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenPlaylist = onOpenPlaylist,
                    onPlayTracks = onPlayTracks,
                )
            }
            ShellDestination.Albums -> {
                AlbumsScreen(
                    viewModel = albumsViewModel,
                    focusRestoreTarget = focusRestoreTarget,
                    onFocusRestoreConsumed = onFocusRestoreConsumed,
                    onAlbumSelected = onOpenAlbum,
                )
            }
            ShellDestination.Playlists -> {
                PlaylistsScreen(
                    viewModel = playlistsViewModel,
                    preselectedPlaylistId = preselectedPlaylistId,
                    onPreselectedPlaylistConsumed = onPreselectedPlaylistConsumed,
                    currentTrackId = playbackQueue.currentItem?.id,
                    onPlayTracks = onPlayTracks,
                    onShuffleTracks = onShuffleTracks,
                )
            }
            ShellDestination.Search -> {
                SearchScreen(
                    viewModel = searchViewModel,
                    focusRestoreTarget = focusRestoreTarget,
                    onFocusRestoreConsumed = onFocusRestoreConsumed,
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onPlayTracks = onPlayTracks,
                )
            }
        }
    }
}
