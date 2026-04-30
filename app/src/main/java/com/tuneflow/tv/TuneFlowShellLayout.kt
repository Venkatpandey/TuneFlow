package com.tuneflow.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuneflow.core.network.ScreenScaleOption
import com.tuneflow.core.player.PlaybackQueue

@Composable
internal fun TuneFlowShellLayout(
    currentSection: NavSection,
    showNowPlaying: Boolean,
    username: String,
    playbackQueue: PlaybackQueue,
    homeViewModel: HomeViewModel,
    albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel,
    albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel,
    artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel,
    playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel,
    searchViewModel: com.tuneflow.feature.browse.SearchViewModel,
    playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel,
    selectedAlbumId: String?,
    selectedArtistId: String?,
    preselectedPlaylistId: String?,
    streamModeLabel: String,
    autoFocusNowPlayingTransport: Boolean,
    onSectionSelected: (NavSection) -> Unit,
    onNowPlaying: () -> Unit,
    onExitApp: () -> Unit,
    onCycleStreamMode: () -> Unit,
    onNowPlayingAutoFocusConsumed: () -> Unit,
    onOpenAlbum: (String, NavSection) -> Unit,
    onOpenArtist: (String, NavSection) -> Unit,
    onOpenSection: (NavSection) -> Unit,
    onOpenPlaylist: (String?) -> Unit,
    onPreselectedPlaylistConsumed: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onPlayTracks: (List<com.tuneflow.core.network.TrackSummary>, Int) -> Unit,
    onShuffleTracks: (List<com.tuneflow.core.network.TrackSummary>) -> Unit,
    showExitPrompt: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.16f,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                                ),
                        ),
                    ),
        )

        TuneFlowSafeArea {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(34.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(34.dp),
                        ),
            ) {
                TuneFlowScaledContent(scaleFactor = ScreenScaleOption.Compact.factor) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                    ) {
                        NavRail(
                            currentSection = currentSection,
                            onSectionSelected = onSectionSelected,
                            onNowPlaying = onNowPlaying,
                            isNowPlayingActive = showNowPlaying,
                            username = username,
                            onExitApp = onExitApp,
                        )

                        Spacer(Modifier.width(22.dp))

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                        ) {
                            ShellContent(
                                currentSection = currentSection,
                                selectedAlbumId = selectedAlbumId,
                                selectedArtistId = selectedArtistId,
                                showNowPlaying = showNowPlaying,
                                preselectedPlaylistId = preselectedPlaylistId,
                                playbackQueue = playbackQueue,
                                homeViewModel = homeViewModel,
                                albumsViewModel = albumsViewModel,
                                albumDetailViewModel = albumDetailViewModel,
                                artistDetailViewModel = artistDetailViewModel,
                                playlistsViewModel = playlistsViewModel,
                                searchViewModel = searchViewModel,
                                playbackViewModel = playbackViewModel,
                                streamModeLabel = streamModeLabel,
                                onCycleStreamMode = onCycleStreamMode,
                                autoFocusNowPlayingTransport = autoFocusNowPlayingTransport,
                                onNowPlayingAutoFocusConsumed = onNowPlayingAutoFocusConsumed,
                                onOpenAlbum = onOpenAlbum,
                                onOpenArtist = onOpenArtist,
                                onOpenSection = onOpenSection,
                                onOpenPlaylist = onOpenPlaylist,
                                onPreselectedPlaylistConsumed = onPreselectedPlaylistConsumed,
                                onOpenNowPlaying = onOpenNowPlaying,
                                onPlayTracks = onPlayTracks,
                                onShuffleTracks = onShuffleTracks,
                            )
                        }
                    }
                }
            }
        }

        ExitPromptBanner(
            visible = showExitPrompt,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
        )
    }
}

@Composable
private fun NavRail(
    currentSection: NavSection,
    onSectionSelected: (NavSection) -> Unit,
    onNowPlaying: () -> Unit,
    isNowPlayingActive: Boolean,
    username: String,
    onExitApp: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(148.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(vertical = 20.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = username.ifBlank { "TuneFlow" }.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        NavRailItem(
            label = "Home",
            selected = currentSection == NavSection.Home && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Home) },
        )
        NavRailItem(
            label = "Albums",
            selected = currentSection == NavSection.Albums && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Albums) },
        )
        NavRailItem(
            label = "Playlists",
            selected = currentSection == NavSection.Playlists && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Playlists) },
        )
        NavRailItem(
            label = "Search",
            selected = currentSection == NavSection.Search && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Search) },
        )

        Spacer(modifier = Modifier.weight(1f))

        NavRailItem(
            label = "Now Playing",
            selected = isNowPlayingActive,
            onClick = onNowPlaying,
        )
        NavRailItem(
            label = "Exit",
            selected = false,
            onClick = onExitApp,
        )
    }
}

@Composable
private fun NavRailItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val active = selected || focused

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                    },
                )
                .border(
                    width = if (active) 2.dp else 1.dp,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        },
                    shape = RoundedCornerShape(20.dp),
                )
                .clickable(onClick = onClick)
                .onFocusChanged { focusState -> focused = focusState.isFocused }
                .focusable()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.scale(if (focused) 1.03f else 1f),
        )
    }
}
