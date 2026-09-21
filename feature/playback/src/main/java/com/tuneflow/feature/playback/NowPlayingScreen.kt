package com.tuneflow.feature.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuneflow.core.design.HorizontalFocusDirection
import com.tuneflow.core.design.TrackFavoriteButton
import com.tuneflow.core.design.TrackRowFocusTarget
import com.tuneflow.core.design.TuneFlowActionSurface
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.design.TuneFlowTrackRow
import com.tuneflow.core.design.trackRowFocusDestination
import com.tuneflow.core.network.TrackFavoriteState
import com.tuneflow.core.network.TrackFavoriteStore
import com.tuneflow.feature.video.VideoCandidateLoadingPanel
import com.tuneflow.feature.video.VideoCandidatePicker
import com.tuneflow.feature.video.VideoDisclosureOverlay
import com.tuneflow.feature.video.VideoUiState
import com.tuneflow.feature.video.VideoViewModel
import com.tuneflow.feature.video.hasVisiblePlayer
import kotlinx.coroutines.launch
import android.view.KeyEvent as AndroidKeyEvent

@Composable
@Suppress("CyclomaticComplexMethod")
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    videoViewModel: VideoViewModel,
    favoriteStore: TrackFavoriteStore,
    lyricsPositionMs: Long,
    streamModeLabel: String,
    onCycleStreamMode: () -> Unit,
    autoFocusTransport: Boolean,
    onAutoFocusConsumed: () -> Unit,
    onVideoViewportBoundsChanged: (IntRect?) -> Unit,
    cinematicModeEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val videoState by videoViewModel.uiState.collectAsStateWithLifecycle()
    val videoPreferred by videoViewModel.videoPreferred.collectAsStateWithLifecycle()
    val favoriteStates by favoriteStore.states.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val item = state.queue.currentItem
    val currentFavoriteState =
        item?.let { favoriteStates[it.id] ?: TrackFavoriteState(isFavorite = false) }
            ?: TrackFavoriteState(isFavorite = false)
    val availableLyrics =
        (lyricsState as? LyricsUiState.Available)
            ?.takeIf { it.trackId == item?.id }
            ?.lyrics
    var activePanel by rememberSaveable { mutableStateOf(NowPlayingPanel.None) }
    var requestStreamFocus by rememberSaveable { mutableStateOf(false) }
    var requestTransportFocus by rememberSaveable { mutableStateOf(false) }
    var requestQueueFocus by rememberSaveable { mutableStateOf(false) }
    var requestLyricsFocus by rememberSaveable { mutableStateOf(false) }
    var requestVideoFocus by rememberSaveable { mutableStateOf(false) }
    var requestInitialTransportFocus by remember { mutableStateOf(true) }
    var focusedQueueIndex by rememberSaveable { mutableIntStateOf(-1) }
    val panelVisible = activePanel != NowPlayingPanel.None
    val artSize by animateDpAsState(targetValue = if (panelVisible) 152.dp else 180.dp, label = "now-playing-art-size")
    val artFrameHeight by animateDpAsState(targetValue = if (panelVisible) 176.dp else 200.dp, label = "now-playing-art-frame-height")

    DisposableEffect(Unit) {
        viewModel.setActive(true)
        onDispose {
            onVideoViewportBoundsChanged(null)
            viewModel.setActive(false)
        }
    }

    LaunchedEffect(item?.id, availableLyrics) {
        if (activePanel == NowPlayingPanel.Lyrics && availableLyrics == null) {
            activePanel = NowPlayingPanel.None
        }
    }

    LaunchedEffect(videoState) {
        if (videoState.showsVideoCandidatePanel()) {
            activePanel = NowPlayingPanel.VideoCandidates
        } else if (activePanel == NowPlayingPanel.VideoCandidates) {
            activePanel = NowPlayingPanel.None
        }
    }

    fun clearRequestedFocus() {
        requestStreamFocus = false
        requestTransportFocus = false
        requestQueueFocus = false
        requestLyricsFocus = false
        requestVideoFocus = false
    }

    fun closeQueue(target: QueueExitTarget) {
        activePanel = NowPlayingPanel.None
        requestStreamFocus = target == QueueExitTarget.StreamControls
        requestTransportFocus = target == QueueExitTarget.TransportControls
    }

    fun closePanelToButton() {
        val closedPanel = activePanel
        activePanel = NowPlayingPanel.None
        when (resolvePanelFocusTarget(closedPanel, availableLyrics != null)) {
            PanelFocusTarget.QueueButton -> requestQueueFocus = true
            PanelFocusTarget.LyricsButton -> requestLyricsFocus = true
            PanelFocusTarget.VideoButton -> requestVideoFocus = true
            PanelFocusTarget.None -> Unit
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                        event ->
                    handleNowPlayingKeyEvent(
                        event = event,
                        activePanel = activePanel,
                        videoActive = videoState.hasVisiblePlayer,
                        onClosePanel = ::closePanelToButton,
                        viewModel = viewModel,
                        videoViewModel = videoViewModel,
                    )
                },
    ) {
        NowPlayingArtworkBackground(
            item = item,
            cinematic = cinematicModeEnabled,
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            NowPlayingPrimaryColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                item = item,
                state = state,
                playlistName = state.queue.sourcePlaylistName,
                videoState = videoState,
                artSize = artSize,
                artFrameHeight = artFrameHeight,
                streamModeLabel = streamModeLabel,
                activePanel = activePanel,
                hasLyrics = availableLyrics != null,
                videoPreferred = videoPreferred,
                videoPreferenceEnabled = !state.queue.sourcePlaylistName.isNullOrBlank() || state.queue.items.size > 1,
                favoriteState = currentFavoriteState,
                onCycleStreamMode = onCycleStreamMode,
                onToggleQueue = {
                    activePanel = toggleNowPlayingPanel(activePanel, NowPlayingPanel.TrackList)
                    clearRequestedFocus()
                },
                onToggleLyrics = {
                    activePanel = toggleNowPlayingPanel(activePanel, NowPlayingPanel.Lyrics)
                    clearRequestedFocus()
                },
                onVideoAction = {
                    when {
                        videoState is VideoUiState.Searching -> Unit
                        videoState is VideoUiState.Candidates -> activePanel = NowPlayingPanel.VideoCandidates
                        videoState.hasVisiblePlayer -> {
                            activePanel = NowPlayingPanel.VideoCandidates
                            videoViewModel.chooseAnother()
                        }
                        else -> videoViewModel.onVideoAction()
                    }
                    clearRequestedFocus()
                },
                onToggleVideoPreference = videoViewModel::toggleVideoPreferredMode,
                onToggleFavorite = {
                    item?.let { track -> scope.launch { favoriteStore.toggle(track.id) } }
                },
                onEnterFullscreen = videoViewModel::enterFullscreen,
                onStopVideo = videoViewModel::stopVideo,
                onVideoViewportBoundsChanged = onVideoViewportBoundsChanged,
                onCyclePlaybackMode = viewModel::cyclePlaybackMode,
                onRetry = viewModel::retry,
                onPrevious = {
                    if (videoState.hasVisiblePlayer) {
                        videoViewModel.seekBy(-NOW_PLAYING_VIDEO_SEEK_MS)
                    } else {
                        viewModel.previous()
                    }
                },
                onTogglePlayPause = {
                    if (!videoViewModel.togglePlayPause()) viewModel.togglePlayPause()
                },
                onNext = {
                    if (videoState.hasVisiblePlayer) {
                        videoViewModel.seekBy(NOW_PLAYING_VIDEO_SEEK_MS)
                    } else {
                        viewModel.next()
                    }
                },
                compactTransport = panelVisible,
                autoFocusTransport = autoFocusTransport || requestTransportFocus || requestInitialTransportFocus,
                autoFocusStreamMode = requestStreamFocus,
                autoFocusQueue = requestQueueFocus,
                autoFocusLyrics = requestLyricsFocus,
                autoFocusVideo = requestVideoFocus,
                onAutoFocusConsumed = {
                    requestInitialTransportFocus = false
                    requestTransportFocus = false
                    onAutoFocusConsumed()
                },
                onStreamModeFocusConsumed = { requestStreamFocus = false },
                onQueueFocusConsumed = { requestQueueFocus = false },
                onLyricsFocusConsumed = { requestLyricsFocus = false },
                onVideoFocusConsumed = { requestVideoFocus = false },
            )

            AnimatedVisibility(
                visible = panelVisible,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 }),
            ) {
                when (activePanel) {
                    NowPlayingPanel.TrackList ->
                        QueuePanel(
                            title = "Track List",
                            playlistName = state.queue.sourcePlaylistName,
                            state = state,
                            onSelectTrack = { index ->
                                viewModel.playFromIndex(
                                    index = index,
                                    playWhenReady = !videoPreferred && !videoState.hasVisiblePlayer,
                                )
                            },
                            favoriteStates = favoriteStates,
                            onToggleFavorite = { trackId -> scope.launch { favoriteStore.toggle(trackId) } },
                            onQueueExit = ::closeQueue,
                            onFocusedIndexChanged = { focusedQueueIndex = it },
                            initialFocusIndex = focusedQueueIndex,
                            preferredExitTarget =
                                resolveQueueExitTarget(
                                    focusedIndex = focusedQueueIndex,
                                    itemCount = state.queue.items.size,
                                ),
                        )
                    NowPlayingPanel.Lyrics ->
                        availableLyrics?.let { lyrics ->
                            LyricsPanel(
                                lyrics = lyrics,
                                positionMs = lyricsPositionMs,
                                onExit = ::closePanelToButton,
                            )
                        }
                    NowPlayingPanel.VideoCandidates ->
                        when (val currentVideoState = videoState) {
                            is VideoUiState.Searching -> VideoCandidateLoadingPanel()
                            is VideoUiState.Candidates ->
                                VideoCandidatePicker(
                                    candidates = currentVideoState.candidates,
                                    onSelect = videoViewModel::selectCandidate,
                                )
                            else -> Unit
                        }
                    NowPlayingPanel.None -> Unit
                }
            }
        }

        if (videoState is VideoUiState.ConsentRequired) {
            VideoDisclosureOverlay(
                onAccept = videoViewModel::acceptDisclosure,
                onCancel = {
                    videoViewModel.cancelDisclosure()
                    clearRequestedFocus()
                    requestVideoFocus = true
                },
            )
        }
    }
}

private const val NOW_PLAYING_VIDEO_SEEK_MS = 10_000L

private fun handleTransportMediaKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    viewModel: PlaybackViewModel,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    return when (event.nativeKeyEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
            viewModel.togglePlayPause()
            true
        }

        AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
            viewModel.play()
            true
        }

        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
            viewModel.pause()
            true
        }

        AndroidKeyEvent.KEYCODE_MEDIA_NEXT -> {
            viewModel.next()
            true
        }

        AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
            viewModel.previous()
            true
        }

        else -> false
    }
}

@Suppress("ReturnCount")
private fun handleNowPlayingKeyEvent(
    event: androidx.compose.ui.input.key.KeyEvent,
    activePanel: NowPlayingPanel,
    videoActive: Boolean,
    onClosePanel: () -> Unit,
    viewModel: PlaybackViewModel,
    videoViewModel: VideoViewModel,
): Boolean {
    when (
        resolveNowPlayingEscapeAction(
            activePanel = activePanel,
            isKeyDown = event.type == KeyEventType.KeyDown,
            keyCode = event.nativeKeyEvent.keyCode,
        )
    ) {
        NowPlayingEscapeAction.ClosePanel -> {
            onClosePanel()
            return true
        }
        NowPlayingEscapeAction.Propagate -> Unit
    }

    if (event.type == KeyEventType.KeyDown && videoActive) {
        return when (event.nativeKeyEvent.keyCode) {
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> videoViewModel.togglePlayPause()
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> videoViewModel.play()
            AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> videoViewModel.pause()
            AndroidKeyEvent.KEYCODE_MEDIA_STOP -> {
                videoViewModel.stopVideo()
                true
            }
            AndroidKeyEvent.KEYCODE_MEDIA_NEXT -> {
                videoViewModel.nextTrack()
            }
            AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                videoViewModel.previousTrack()
            }
            else -> false
        }
    }

    return handleTransportMediaKey(event, viewModel)
}

internal enum class NowPlayingEscapeAction {
    ClosePanel,
    Propagate,
}

internal enum class NowPlayingPanel {
    None,
    TrackList,
    Lyrics,
    VideoCandidates,
}

internal fun toggleNowPlayingPanel(
    current: NowPlayingPanel,
    requested: NowPlayingPanel,
): NowPlayingPanel = if (current == requested) NowPlayingPanel.None else requested

internal fun VideoUiState.showsVideoCandidatePanel(): Boolean = this is VideoUiState.Searching || this is VideoUiState.Candidates

internal enum class PanelFocusTarget {
    None,
    QueueButton,
    LyricsButton,
    VideoButton,
}

internal fun resolvePanelFocusTarget(
    closedPanel: NowPlayingPanel,
    lyricsAvailable: Boolean,
): PanelFocusTarget =
    when (closedPanel) {
        NowPlayingPanel.TrackList -> PanelFocusTarget.QueueButton
        NowPlayingPanel.Lyrics -> if (lyricsAvailable) PanelFocusTarget.LyricsButton else PanelFocusTarget.None
        NowPlayingPanel.VideoCandidates -> PanelFocusTarget.VideoButton
        NowPlayingPanel.None -> PanelFocusTarget.None
    }

internal fun resolveNowPlayingEscapeAction(
    activePanel: NowPlayingPanel,
    isKeyDown: Boolean,
    keyCode: Int,
): NowPlayingEscapeAction =
    if (
        activePanel != NowPlayingPanel.None &&
        isKeyDown &&
        keyCode == AndroidKeyEvent.KEYCODE_BACK
    ) {
        NowPlayingEscapeAction.ClosePanel
    } else {
        NowPlayingEscapeAction.Propagate
    }

@Composable
private fun QueuePanel(
    title: String,
    playlistName: String?,
    state: NowPlayingUiState,
    onSelectTrack: (Int) -> Unit,
    favoriteStates: Map<String, TrackFavoriteState>,
    onToggleFavorite: (String) -> Unit,
    onQueueExit: (QueueExitTarget) -> Unit,
    onFocusedIndexChanged: (Int) -> Unit,
    initialFocusIndex: Int,
    preferredExitTarget: QueueExitTarget,
) {
    val initialItemFocusRequester = remember { FocusRequester() }
    val queueListState = rememberLazyListState()
    val currentIndex = state.queue.currentIndex
    val initialItemIndex =
        resolveQueuePanelFocusIndex(
            previousFocusedIndex = initialFocusIndex,
            currentIndex = currentIndex,
            itemCount = state.queue.items.size,
        )

    LaunchedEffect(initialItemIndex, state.queue.items.map { it.id }) {
        if (initialItemIndex >= 0) {
            queueListState.scrollToItem(initialItemIndex)
            withFrameNanos { }
            runCatching { initialItemFocusRequester.requestFocus() }
            onFocusedIndexChanged(initialItemIndex)
        }
    }

    Column(
        modifier =
            Modifier
                .width(312.dp)
                .fillMaxHeight()
                .clip(TuneFlowShapes.panel)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    shape = TuneFlowShapes.panel,
                )
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        playlistName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LazyColumn(
            state = queueListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.queue.items, key = { _, track -> track.id }) { index, track ->
                QueueRow(
                    trackId = track.id,
                    title = track.title,
                    subtitle = track.artist,
                    isCurrent = index == currentIndex,
                    showDivider = index != state.queue.items.lastIndex,
                    favoriteState = favoriteStates[track.id] ?: TrackFavoriteState(isFavorite = false),
                    onClick = { onSelectTrack(index) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onExitLeft = { onQueueExit(preferredExitTarget) },
                    onFocused = { onFocusedIndexChanged(index) },
                    externalRowFocusRequester = initialItemFocusRequester.takeIf { index == initialItemIndex },
                    modifier =
                        Modifier
                            .boundaryLockedVerticalItem(
                                index = index,
                                lastIndex = state.queue.items.lastIndex,
                            ),
                )
            }
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun QueueRow(
    trackId: String,
    title: String,
    subtitle: String,
    isCurrent: Boolean,
    showDivider: Boolean,
    favoriteState: TrackFavoriteState,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onExitLeft: () -> Unit,
    onFocused: () -> Unit,
    externalRowFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val rowFocusRequester = remember(trackId) { FocusRequester() }
    val favoriteFocusRequester = remember(trackId) { FocusRequester() }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TuneFlowTrackRow(
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(rowFocusRequester)
                    .then(externalRowFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.nativeKeyEvent.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                onExitLeft()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (
                                    trackRowFocusDestination(
                                        TrackRowFocusTarget.RowBody,
                                        HorizontalFocusDirection.Right,
                                    ) == TrackRowFocusTarget.FavoriteButton
                                ) {
                                    favoriteFocusRequester.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    },
            selected = isCurrent,
            showDivider = showDivider,
            onFocusedChange = { if (it) onFocused() },
            onClick = onClick,
        ) {
            if (isCurrent) {
                Image(
                    painter = painterResource(id = R.drawable.currently_playing),
                    contentDescription = "Currently playing",
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Spacer(modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TrackFavoriteButton(
            isFavorite = favoriteState.isFavorite,
            isPending = favoriteState.isPending,
            onClick = onToggleFavorite,
            modifier =
                Modifier
                    .focusRequester(favoriteFocusRequester)
                    .onFocusChanged { if (it.hasFocus) onFocused() }
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT &&
                            trackRowFocusDestination(
                                TrackRowFocusTarget.FavoriteButton,
                                HorizontalFocusDirection.Left,
                            ) == TrackRowFocusTarget.RowBody
                        ) {
                            rowFocusRequester.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
        )
    }
}

internal enum class QueueExitTarget {
    StreamControls,
    TransportControls,
}

internal fun resolveQueuePanelFocusIndex(
    previousFocusedIndex: Int,
    currentIndex: Int,
    itemCount: Int,
): Int =
    when {
        previousFocusedIndex in 0 until itemCount -> previousFocusedIndex
        currentIndex in 0 until itemCount -> currentIndex
        itemCount > 0 -> 0
        else -> -1
    }

internal fun resolveQueueExitTarget(
    focusedIndex: Int,
    itemCount: Int,
): QueueExitTarget {
    if (itemCount <= 1) return QueueExitTarget.StreamControls
    val threshold = (itemCount - 1) / 2
    return if (focusedIndex <= threshold) {
        QueueExitTarget.StreamControls
    } else {
        QueueExitTarget.TransportControls
    }
}

private fun Modifier.boundaryLockedVerticalItem(
    index: Int,
    lastIndex: Int,
): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        when {
            event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP && index == 0 -> true
            event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN && index == lastIndex -> true
            else -> false
        }
    }

@Composable
internal fun PlaybackStatusCard(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TuneFlowShapes.panel)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = TuneFlowShapes.panel,
                )
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        PlaybackTextButton(
            label = "Retry",
            accent = true,
            onClick = onRetry,
            modifier = Modifier.width(156.dp),
        )
    }
}

@Composable
internal fun PlaybackTextButton(
    label: String,
    accent: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconResId: Int? = null,
    compact: Boolean = false,
    requestFocus: Boolean = false,
    onRequestedFocusApplied: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onRequestedFocusApplied()
        }
    }

    TuneFlowActionSurface(
        onClick = onClick,
        accent = accent,
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = if (compact) 10.dp else 18.dp,
                vertical = if (compact) 8.dp else 15.dp,
            ),
        modifier =
            modifier
                .focusRequester(focusRequester),
    ) {
        val contentColor = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            iconResId?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                color = contentColor,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleLarge,
                fontWeight = if (compact) FontWeight.SemiBold else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
