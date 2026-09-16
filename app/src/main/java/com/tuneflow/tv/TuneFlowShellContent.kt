package com.tuneflow.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntRect
import com.tuneflow.core.design.LocalTuneFlowMotion
import com.tuneflow.core.network.PlaylistFavoriteStore
import com.tuneflow.core.network.TrackFavoriteStore
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
    navigationDepth: Int,
    premiumFeaturesEnabled: Boolean,
    preselectedPlaylistId: String?,
    focusRestoreTarget: com.tuneflow.feature.browse.BrowseFocusTarget?,
    playbackQueue: PlaybackQueue,
    playbackPositionMs: Long,
    homeViewModel: HomeViewModel,
    favoriteStore: TrackFavoriteStore,
    playlistFavoriteStore: PlaylistFavoriteStore,
    recentPlaylistIds: List<String>,
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
    onPlayPlaylistTracks: (String, String, List<com.tuneflow.core.network.TrackSummary>, Int) -> Unit,
    onShufflePlaylistTracks: (String, String, List<com.tuneflow.core.network.TrackSummary>) -> Unit,
    preferredVideoServiceUrl: String,
    onPreferredVideoServiceUrlChanged: (String) -> Unit,
) {
    val motion = LocalTuneFlowMotion.current
    val transitionState = ShellContentTransitionState(currentDestination, navigationDepth)
    AnimatedContent(
        targetState = transitionState,
        transitionSpec = {
            shellContentTransform(
                direction = resolveShellMotionDirection(initialState, targetState),
                motionEnabled = motion.enabled,
                forwardDurationMs = motion.screenForwardDurationMs,
                backDurationMs = motion.screenBackDurationMs,
                nowPlayingOpenDurationMs = motion.nowPlayingOpenDurationMs,
                nowPlayingCloseDurationMs = motion.nowPlayingCloseDurationMs,
            )
        },
        contentKey = { it.destination },
        label = "shell-content",
    ) { targetState ->
        when (val targetScreen = targetState.destination) {
            ShellDestination.NowPlaying -> {
                NowPlayingScreen(
                    viewModel = playbackViewModel,
                    videoViewModel = videoViewModel,
                    favoriteStore = favoriteStore,
                    lyricsPositionMs = playbackPositionMs,
                    streamModeLabel = streamModeLabel,
                    onCycleStreamMode = onCycleStreamMode,
                    autoFocusTransport = autoFocusNowPlayingTransport,
                    onAutoFocusConsumed = onNowPlayingAutoFocusConsumed,
                    onVideoViewportBoundsChanged = onVideoViewportBoundsChanged,
                    cinematicModeEnabled = premiumFeaturesEnabled,
                )
            }
            is ShellDestination.Album -> {
                AlbumDetailScreen(
                    albumId = targetScreen.albumId,
                    viewModel = albumDetailViewModel,
                    favoriteStore = favoriteStore,
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
                    favoriteStore = favoriteStore,
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
                    preferredVideoServiceUrl = preferredVideoServiceUrl,
                    onPreferredVideoServiceUrlChanged = onPreferredVideoServiceUrlChanged,
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
                    favoriteStore = favoriteStore,
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
                    favoriteStore = favoriteStore,
                    playlistFavoriteStore = playlistFavoriteStore,
                    recentPlaylistIds = recentPlaylistIds,
                    preselectedPlaylistId = preselectedPlaylistId,
                    onPreselectedPlaylistConsumed = onPreselectedPlaylistConsumed,
                    currentTrackId = playbackQueue.currentItem?.id,
                    currentPlaylistId = playbackQueue.sourcePlaylistId,
                    currentPlaylistName = playbackQueue.sourcePlaylistName,
                    onPlayTracks = onPlayPlaylistTracks,
                    onShuffleTracks = onShufflePlaylistTracks,
                )
            }
            ShellDestination.Search -> {
                SearchScreen(
                    viewModel = searchViewModel,
                    favoriteStore = favoriteStore,
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

internal data class ShellContentTransitionState(
    val destination: ShellDestination,
    val depth: Int,
)

internal enum class ShellMotionDirection {
    Forward,
    Back,
    OpenNowPlaying,
    CloseNowPlaying,
    Fade,
}

internal fun resolveShellMotionDirection(
    initial: ShellContentTransitionState,
    target: ShellContentTransitionState,
): ShellMotionDirection =
    when {
        target.destination == ShellDestination.NowPlaying && initial.destination != ShellDestination.NowPlaying ->
            ShellMotionDirection.OpenNowPlaying
        initial.destination == ShellDestination.NowPlaying && target.destination != ShellDestination.NowPlaying ->
            ShellMotionDirection.CloseNowPlaying
        target.depth > initial.depth -> ShellMotionDirection.Forward
        target.depth < initial.depth -> ShellMotionDirection.Back
        else -> ShellMotionDirection.Fade
    }

private fun shellContentTransform(
    direction: ShellMotionDirection,
    motionEnabled: Boolean,
    forwardDurationMs: Int,
    backDurationMs: Int,
    nowPlayingOpenDurationMs: Int,
    nowPlayingCloseDurationMs: Int,
): ContentTransform {
    if (!motionEnabled) return fadeIn(snap()) togetherWith fadeOut(snap())

    val transform =
        when (direction) {
            ShellMotionDirection.Forward ->
                slideInHorizontally(tween(forwardDurationMs, easing = FastOutSlowInEasing)) { it / 6 } +
                    fadeIn(tween(forwardDurationMs)) togetherWith
                    (
                        slideOutHorizontally(tween(forwardDurationMs, easing = FastOutSlowInEasing)) { -it / 10 } +
                            fadeOut(tween(forwardDurationMs / 2))
                    )
            ShellMotionDirection.Back ->
                slideInHorizontally(tween(backDurationMs, easing = LinearOutSlowInEasing)) { -it / 6 } +
                    fadeIn(tween(backDurationMs)) togetherWith
                    (
                        slideOutHorizontally(tween(backDurationMs, easing = LinearOutSlowInEasing)) { it / 10 } +
                            fadeOut(tween(backDurationMs / 2))
                    )
            ShellMotionDirection.OpenNowPlaying ->
                slideInVertically(tween(nowPlayingOpenDurationMs, easing = FastOutSlowInEasing)) { it / 5 } +
                    fadeIn(tween(nowPlayingOpenDurationMs)) togetherWith
                    fadeOut(tween(nowPlayingOpenDurationMs / 2))
            ShellMotionDirection.CloseNowPlaying ->
                fadeIn(tween(nowPlayingCloseDurationMs)) togetherWith
                    (
                        slideOutVertically(tween(nowPlayingCloseDurationMs, easing = LinearOutSlowInEasing)) { it / 5 } +
                            fadeOut(tween(nowPlayingCloseDurationMs))
                    )
            ShellMotionDirection.Fade ->
                fadeIn(tween(backDurationMs)) togetherWith fadeOut(tween(backDurationMs))
        }
    return transform
}
