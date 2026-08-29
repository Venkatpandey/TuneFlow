package com.tuneflow.tv

import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.network.ScreenScaleOption
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.QueueItem
import com.tuneflow.feature.playback.Lyrics
import com.tuneflow.feature.playback.LyricsRenderer
import com.tuneflow.feature.playback.LyricsUiState
import com.tuneflow.feature.video.VideoUiState
import com.tuneflow.feature.video.VideoViewModel
import com.tuneflow.feature.video.YouTubePlayerSurface
import com.tuneflow.feature.video.hasVisiblePlayer
import com.tuneflow.feature.video.isFullscreen
import kotlin.math.roundToInt
import android.view.KeyEvent as AndroidKeyEvent

internal const val NOW_PLAYING_WIDGET_HEIGHT_DP = 224

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
    videoViewModel: VideoViewModel,
    videoState: VideoUiState,
    videoOverlayHost: FrameLayout,
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
    var videoViewportBounds by remember { mutableStateOf<IntRect?>(null) }
    var videoRailViewportBounds by remember { mutableStateOf<IntRect?>(null) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    BackHandler(enabled = videoState.isFullscreen) {
        videoViewModel.exitFullscreen()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { rootSize = it }
                .onPreviewKeyEvent { event ->
                    if (
                        videoState.hasVisiblePlayer &&
                        event.type == KeyEventType.KeyDown &&
                        event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_MEDIA_STOP
                    ) {
                        videoViewModel.stopVideo()
                        true
                    } else {
                        false
                    }
                }
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
                            playbackPositionMs =
                                (videoState as? VideoUiState.Playing)?.positionMs ?: playbackPositionMs,
                            videoVisible = videoState.hasVisiblePlayer,
                            onVideoViewportBoundsChanged = { videoRailViewportBounds = it },
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
                                videoViewModel = videoViewModel,
                                streamModeLabel = streamModeLabel,
                                onCycleStreamMode = onCycleStreamMode,
                                autoFocusNowPlayingTransport = autoFocusNowPlayingTransport,
                                onNowPlayingAutoFocusConsumed = onNowPlayingAutoFocusConsumed,
                                onVideoViewportBoundsChanged = { videoViewportBounds = it },
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

        val playerContainerBounds =
            if (videoState.isFullscreen) {
                IntRect(0, 0, rootSize.width, rootSize.height)
            } else if (currentDestination == ShellDestination.NowPlaying) {
                videoViewportBounds ?: videoRailViewportBounds
            } else {
                videoRailViewportBounds
            }
        val playerBounds = playerContainerBounds?.let(::fitYouTubePlayerBounds)
        FullscreenVideoBackdrop(visible = videoState.isFullscreen)
        if (videoState.hasVisiblePlayer && playerBounds != null && playerBounds.width > 0 && playerBounds.height > 0) {
            val playerModifier =
                with(density) {
                    Modifier
                        .offset { IntOffset(playerBounds.left, playerBounds.top) }
                        .size(playerBounds.width.toDp(), playerBounds.height.toDp())
                }
            Box(
                modifier =
                    playerModifier
                        .background(Color.Black),
            )
            YouTubePlayerSurface(
                player = videoViewModel.youtubePlayer,
                host = videoOverlayHost,
                bounds = playerBounds,
                requestFocus = videoState.isFullscreen,
                onKeyEvent = { event ->
                    handleVideoOverlayMediaKey(event, videoViewModel, playbackViewModel)
                },
            )
        }
    }
}

@Composable
private fun FullscreenVideoBackdrop(visible: Boolean) {
    if (visible) Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}

internal fun fitYouTubePlayerBounds(container: IntRect): IntRect {
    if (container.width <= 0 || container.height <= 0) return container
    val widthAtContainerHeight =
        (container.height.toLong() * YOUTUBE_PLAYER_ASPECT_WIDTH / YOUTUBE_PLAYER_ASPECT_HEIGHT).toInt()
    val fittedWidth: Int
    val fittedHeight: Int
    if (widthAtContainerHeight <= container.width) {
        fittedWidth = widthAtContainerHeight
        fittedHeight = container.height
    } else {
        fittedWidth = container.width
        fittedHeight =
            (container.width.toLong() * YOUTUBE_PLAYER_ASPECT_HEIGHT / YOUTUBE_PLAYER_ASPECT_WIDTH).toInt()
    }
    val left = container.left + (container.width - fittedWidth) / 2
    val top = container.top + (container.height - fittedHeight) / 2
    return IntRect(
        left = left,
        top = top,
        right = left + fittedWidth,
        bottom = top + fittedHeight,
    )
}

private const val YOUTUBE_PLAYER_ASPECT_WIDTH = 16L
private const val YOUTUBE_PLAYER_ASPECT_HEIGHT = 9L

private fun handleVideoOverlayMediaKey(
    event: AndroidKeyEvent,
    videoViewModel: VideoViewModel,
    playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel,
): Boolean {
    if (event.action != AndroidKeyEvent.ACTION_DOWN) return false
    val dpadResult = handleVideoDpadKey(event, videoViewModel)
    return dpadResult
        ?: when (event.keyCode) {
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> videoViewModel.togglePlayPause()
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> videoViewModel.play()
            AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> videoViewModel.pause()
            AndroidKeyEvent.KEYCODE_MEDIA_STOP -> {
                videoViewModel.stopVideo()
                true
            }
            AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> videoViewModel.seekBy(10_000L)
            AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> videoViewModel.seekBy(-10_000L)
            AndroidKeyEvent.KEYCODE_MEDIA_NEXT -> {
                videoViewModel.closeForQueueChange()
                playbackViewModel.next()
                true
            }
            AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                videoViewModel.closeForQueueChange()
                playbackViewModel.previous()
                true
            }
            else -> false
        }
}

private fun handleVideoDpadKey(
    event: AndroidKeyEvent,
    videoViewModel: VideoViewModel,
): Boolean? =
    when (event.keyCode) {
        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
        AndroidKeyEvent.KEYCODE_ENTER,
        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
        -> if (event.repeatCount == 0) videoViewModel.togglePlayPause() else true
        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> videoViewModel.seekBy(-5_000L)
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> videoViewModel.seekBy(5_000L)
        AndroidKeyEvent.KEYCODE_DPAD_UP -> videoViewModel.adjustVolume(5)
        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> videoViewModel.adjustVolume(-5)
        else -> null
    }

@Composable
private fun NavRail(
    currentSection: NavSection,
    playbackQueue: PlaybackQueue,
    playbackPositionMs: Long,
    videoVisible: Boolean,
    onVideoViewportBoundsChanged: (IntRect?) -> Unit,
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
            videoVisible = videoVisible,
            onVideoViewportBoundsChanged = onVideoViewportBoundsChanged,
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
    videoVisible: Boolean = false,
    onVideoViewportBoundsChanged: (IntRect?) -> Unit = {},
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
                .height(NOW_PLAYING_WIDGET_HEIGHT_DP.dp)
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

            RailArtworkViewport(
                currentItem = currentItem,
                videoVisible = videoVisible,
                onVideoViewportBoundsChanged = onVideoViewportBoundsChanged,
            )

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
private fun RailArtworkViewport(
    currentItem: QueueItem?,
    videoVisible: Boolean,
    onVideoViewportBoundsChanged: (IntRect?) -> Unit,
) {
    LaunchedEffect(videoVisible) {
        if (!videoVisible) onVideoViewportBoundsChanged(null)
    }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .onGloballyPositioned { coordinates ->
                    if (videoVisible) {
                        val bounds = coordinates.boundsInRoot()
                        onVideoViewportBoundsChanged(
                            IntRect(
                                left = bounds.left.roundToInt(),
                                top = bounds.top.roundToInt(),
                                right = bounds.right.roundToInt(),
                                bottom = bounds.bottom.roundToInt(),
                            ),
                        )
                    }
                }
                .clip(TuneFlowShapes.artwork)
                .background(
                    if (videoVisible) Color.Black else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (!videoVisible) {
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
