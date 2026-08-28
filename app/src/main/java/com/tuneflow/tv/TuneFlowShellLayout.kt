package com.tuneflow.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.network.ScreenScaleOption
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.feature.playback.Lyrics
import com.tuneflow.feature.playback.LyricsRenderer
import com.tuneflow.feature.playback.LyricsUiState

@Composable
internal fun TuneFlowShellLayout(
    currentSection: NavSection,
    currentDestination: ShellDestination,
    showNowPlaying: Boolean,
    username: String,
    currentTimeText: String,
    playbackQueue: PlaybackQueue,
    playbackPositionMs: Long,
    screensaverActive: Boolean,
    lyricsState: LyricsUiState,
    homeViewModel: HomeViewModel,
    albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel,
    homeCategoryViewModel: com.tuneflow.feature.browse.HomeCategoryViewModel,
    albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel,
    artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel,
    playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel,
    searchViewModel: com.tuneflow.feature.browse.SearchViewModel,
    playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel,
    preselectedPlaylistId: String?,
    focusRestoreTarget: com.tuneflow.feature.browse.BrowseFocusTarget?,
    streamModeLabel: String,
    autoFocusNowPlayingTransport: Boolean,
    onSectionSelected: (NavSection) -> Unit,
    onNowPlaying: () -> Unit,
    onCycleStreamMode: () -> Unit,
    onNowPlayingAutoFocusConsumed: () -> Unit,
    onFocusRestoreConsumed: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSection: (NavSection) -> Unit,
    onOpenHomeCategory: (com.tuneflow.feature.browse.HomeCategoryKind) -> Unit,
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
        androidx.compose.foundation.Image(
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
                        .clip(TuneFlowShapes.surface)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            shape = TuneFlowShapes.surface,
                        ),
            ) {
                TuneFlowScaledContent(scaleFactor = ScreenScaleOption.Compact.factor) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                    ) {
                        NavRail(
                            currentSection = currentSection,
                            playbackQueue = playbackQueue,
                            playbackPositionMs = playbackPositionMs,
                            onSectionSelected = onSectionSelected,
                            onNowPlaying = onNowPlaying,
                            isNowPlayingActive = showNowPlaying,
                            username = username,
                            currentTimeText = currentTimeText,
                        )

                        Spacer(Modifier.width(22.dp))

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                        ) {
                            ShellContent(
                                currentDestination = currentDestination,
                                preselectedPlaylistId = preselectedPlaylistId,
                                focusRestoreTarget = focusRestoreTarget,
                                playbackQueue = playbackQueue,
                                homeViewModel = homeViewModel,
                                albumsViewModel = albumsViewModel,
                                homeCategoryViewModel = homeCategoryViewModel,
                                albumDetailViewModel = albumDetailViewModel,
                                artistDetailViewModel = artistDetailViewModel,
                                playlistsViewModel = playlistsViewModel,
                                searchViewModel = searchViewModel,
                                playbackViewModel = playbackViewModel,
                                streamModeLabel = streamModeLabel,
                                onCycleStreamMode = onCycleStreamMode,
                                autoFocusNowPlayingTransport = autoFocusNowPlayingTransport,
                                onNowPlayingAutoFocusConsumed = onNowPlayingAutoFocusConsumed,
                                onFocusRestoreConsumed = onFocusRestoreConsumed,
                                onOpenAlbum = onOpenAlbum,
                                onOpenArtist = onOpenArtist,
                                onOpenSection = onOpenSection,
                                onOpenHomeCategory = onOpenHomeCategory,
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

        if (screensaverActive) {
            PlaybackScreensaverOverlay(
                playbackQueue = playbackQueue,
                playbackPositionMs = playbackPositionMs,
                lyricsState = lyricsState,
            )
        }
    }
}

@Composable
private fun NavRail(
    currentSection: NavSection,
    playbackQueue: PlaybackQueue,
    playbackPositionMs: Long,
    onSectionSelected: (NavSection) -> Unit,
    onNowPlaying: () -> Unit,
    isNowPlayingActive: Boolean,
    username: String,
    currentTimeText: String,
) {
    Column(
        modifier =
            Modifier
                .width(148.dp)
                .fillMaxHeight()
                .clip(TuneFlowShapes.container)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    shape = TuneFlowShapes.container,
                )
                .padding(vertical = 20.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(TuneFlowShapes.avatar)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            TuneFlowShapes.avatar,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = username.ifBlank { "TuneFlow" }.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = currentTimeText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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

        NowPlayingRailWidget(
            playbackQueue = playbackQueue,
            playbackPositionMs = playbackPositionMs,
            selected = isNowPlayingActive,
            onClick = onNowPlaying,
            modifier = Modifier.fillMaxWidth(),
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
                .onFocusChanged { focusState -> focused = focusState.isFocused }
                .focusable()
                .clip(TuneFlowShapes.row)
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                    },
                )
                .border(
                    width = if (active) 3.dp else 1.dp,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        },
                    shape = TuneFlowShapes.row,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.scale(if (focused) 1.05f else 1f),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun NowPlayingRailWidget(
    playbackQueue: PlaybackQueue,
    playbackPositionMs: Long,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val active = interactive && (selected || focused)
    val currentItem = playbackQueue.currentItem
    val durationMs = currentItem?.durationMs ?: 0L
    val positionMs = playbackPositionMs.coerceAtLeast(0L)
    val progress =
        if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    Box(
        modifier =
            modifier
                .height(196.dp)
                .then(
                    if (interactive) {
                        Modifier
                            .onFocusChanged { focusState -> focused = focusState.isFocused }
                            .focusable()
                    } else {
                        Modifier
                    },
                )
                .clip(TuneFlowShapes.card)
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                    },
                )
                .border(
                    width = if (active) 3.dp else 1.dp,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = TuneFlowShapes.card,
                )
                .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                TuneFlowArtwork(
                    model = currentItem?.artUrl,
                    contentDescription = currentItem?.title,
                    width = 128.dp,
                    height = 92.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = currentItem?.title ?: "Nothing playing",
                    fallbackPainterResId = R.drawable.ic_tuneflow_brand,
                )
            }

            RailMarqueeText(
                text = currentItem?.title ?: "Nothing playing",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            RailMarqueeText(
                text =
                    listOfNotNull(
                        currentItem?.artist?.takeIf { it.isNotBlank() },
                        currentItem?.album?.takeIf { it.isNotBlank() },
                    ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.weight(1f))

            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(TuneFlowShapes.progressTrack),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
            )
            Text(
                text = "${railFormatTime(positionMs)} / ${railFormatTime(durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaybackScreensaverOverlay(
    playbackQueue: PlaybackQueue,
    playbackPositionMs: Long,
    lyricsState: LyricsUiState,
) {
    val currentItem = playbackQueue.currentItem ?: return
    val lyrics = resolveScreensaverLyrics(lyricsState, currentItem.id)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        TuneFlowArtwork(
            model = currentItem.artUrl,
            contentDescription = null,
            width = 1280.dp,
            height = 720.dp,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.42f,
            placeholderText = currentItem.title,
            fallbackPainterResId = R.drawable.ic_tuneflow_brand,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f)),
        )

        NowPlayingRailWidget(
            playbackQueue = playbackQueue,
            playbackPositionMs = playbackPositionMs,
            selected = false,
            onClick = {},
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 44.dp, bottom = 40.dp)
                    .width(190.dp),
            interactive = false,
        )

        if (lyrics != null) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 52.dp, horizontal = 64.dp)
                        .width(430.dp)
                        .fillMaxHeight()
                        .clip(TuneFlowShapes.panel)
                        .background(Color.Black.copy(alpha = 0.34f))
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LyricsRenderer(
                    lyrics = lyrics,
                    positionMs = playbackPositionMs,
                    durationMs = currentItem.durationMs,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    autoFollow = true,
                    interactive = false,
                    estimateUnsynchronized = true,
                )
            }
        }
    }
}

internal fun resolveScreensaverLyrics(
    lyricsState: LyricsUiState,
    currentTrackId: String,
): Lyrics? =
    (lyricsState as? LyricsUiState.Available)
        ?.takeIf { it.trackId == currentTrackId }
        ?.lyrics

private fun railFormatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RailMarqueeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
) {
    if (text.isBlank()) return

    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            modifier = Modifier.basicMarquee().fillMaxWidth(),
        )
    }
}
