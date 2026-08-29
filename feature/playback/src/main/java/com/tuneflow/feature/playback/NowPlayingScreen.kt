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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.feature.video.VideoCandidatePicker
import com.tuneflow.feature.video.VideoDisclosureOverlay
import com.tuneflow.feature.video.VideoUiState
import com.tuneflow.feature.video.VideoViewModel
import com.tuneflow.feature.video.hasVisiblePlayer
import kotlinx.coroutines.delay
import android.view.KeyEvent as AndroidKeyEvent

@Composable
@Suppress("CyclomaticComplexMethod")
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    videoViewModel: VideoViewModel,
    streamModeLabel: String,
    onCycleStreamMode: () -> Unit,
    autoFocusTransport: Boolean,
    onAutoFocusConsumed: () -> Unit,
    onVideoViewportBoundsChanged: (IntRect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val videoState by videoViewModel.uiState.collectAsStateWithLifecycle()
    val item = state.queue.currentItem
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
    var focusedQueueIndex by rememberSaveable { mutableIntStateOf(0) }
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
        if (videoState is VideoUiState.Candidates) {
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
        TuneFlowArtwork(
            model = item?.artUrl,
            contentDescription = null,
            width = 1280.dp,
            height = 720.dp,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.18f,
            placeholderText = item?.title,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.54f)),
        )

        if (!autoFocusTransport) {
            ScreenInitialFocusAnchor()
        }

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
                videoState = videoState,
                artSize = artSize,
                artFrameHeight = artFrameHeight,
                streamModeLabel = streamModeLabel,
                activePanel = activePanel,
                hasLyrics = availableLyrics != null,
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
                    if (videoState is VideoUiState.Candidates) {
                        activePanel = NowPlayingPanel.VideoCandidates
                    } else {
                        videoViewModel.onVideoAction()
                    }
                    clearRequestedFocus()
                },
                onChooseAnotherVideo = {
                    clearRequestedFocus()
                    activePanel = NowPlayingPanel.VideoCandidates
                    videoViewModel.chooseAnother()
                },
                onStopVideo = videoViewModel::stopVideo,
                onVideoViewportBoundsChanged = onVideoViewportBoundsChanged,
                onCyclePlaybackMode = viewModel::cyclePlaybackMode,
                onRetry = viewModel::retry,
                onPrevious = {
                    if (videoState.hasVisiblePlayer) videoViewModel.closeForQueueChange()
                    viewModel.previous()
                },
                onTogglePlayPause = {
                    if (!videoViewModel.togglePlayPause()) viewModel.togglePlayPause()
                },
                onNext = {
                    if (videoState.hasVisiblePlayer) videoViewModel.closeForQueueChange()
                    viewModel.next()
                },
                compactTransport = panelVisible,
                autoFocusTransport = autoFocusTransport || requestTransportFocus,
                autoFocusStreamMode = requestStreamFocus,
                autoFocusQueue = requestQueueFocus,
                autoFocusLyrics = requestLyricsFocus,
                autoFocusVideo = requestVideoFocus,
                onAutoFocusConsumed = onAutoFocusConsumed,
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
                            state = state,
                            onSelectTrack = viewModel::playFromIndex,
                            onQueueExit = ::closeQueue,
                            onFocusedIndexChanged = { focusedQueueIndex = it },
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
                                positionMs = state.positionMs,
                                durationMs = state.durationMs,
                                onExit = ::closePanelToButton,
                            )
                        }
                    NowPlayingPanel.VideoCandidates ->
                        VideoCandidatePicker(
                            candidates = (videoState as? VideoUiState.Candidates)?.candidates.orEmpty(),
                            onSelect = videoViewModel::selectCandidate,
                        )
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
                videoViewModel.closeForQueueChange()
                viewModel.next()
                true
            }
            AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                videoViewModel.closeForQueueChange()
                viewModel.previous()
                true
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
    state: NowPlayingUiState,
    onSelectTrack: (Int) -> Unit,
    onQueueExit: (QueueExitTarget) -> Unit,
    onFocusedIndexChanged: (Int) -> Unit,
    preferredExitTarget: QueueExitTarget,
) {
    val currentFocusRequester = remember { FocusRequester() }
    val queueListState = rememberLazyListState()
    val currentIndex = state.queue.currentIndex
    val hasCurrentQueueItem = currentIndex in state.queue.items.indices

    LaunchedEffect(hasCurrentQueueItem, currentIndex) {
        if (hasCurrentQueueItem) {
            // Show the user where we are by animating the list to current track first.
            queueListState.animateScrollToItem(currentIndex)
            delay(500)
            runCatching { currentFocusRequester.requestFocus() }
            onFocusedIndexChanged(currentIndex)
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
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                        -> {
                            onQueueExit(preferredExitTarget)
                            true
                        }
                        else -> false
                    }
                }
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LazyColumn(
            state = queueListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.queue.items, key = { _, track -> track.id }) { index, track ->
                QueueRow(
                    title = track.title,
                    subtitle = track.artist,
                    isCurrent = index == currentIndex,
                    onClick = { onSelectTrack(index) },
                    onFocused = { onFocusedIndexChanged(index) },
                    modifier =
                        Modifier
                            .boundaryLockedVerticalItem(
                                index = index,
                                lastIndex = state.queue.items.lastIndex,
                            )
                            .then(
                                if (index == currentIndex) {
                                    Modifier.focusRequester(currentFocusRequester)
                                } else {
                                    Modifier
                                },
                            ),
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    title: String,
    subtitle: String,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .scale(if (focused) 1.01f else 1f)
                .clip(TuneFlowShapes.row)
                .background(
                    when {
                        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                    },
                )
                .border(
                    width = if (focused || isCurrent) 2.dp else 1.dp,
                    color =
                        when {
                            focused || isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        },
                    shape = TuneFlowShapes.row,
                )
                .onFocusChanged {
                    focused = it.hasFocus
                    if (it.hasFocus) onFocused()
                }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
    }
}

internal enum class QueueExitTarget {
    StreamControls,
    TransportControls,
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
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onRequestedFocusApplied()
        }
    }

    Box(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .scale(if (focused) 1.01f else 1f)
                .clip(TuneFlowShapes.button)
                .background(
                    if (accent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f)
                    },
                )
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = TuneFlowShapes.button,
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        Row(
            modifier =
                Modifier.padding(
                    horizontal = if (compact) 10.dp else 18.dp,
                    vertical = if (compact) 8.dp else 15.dp,
                ),
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

@Composable
private fun ScreenInitialFocusAnchor() {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier =
            Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .focusable(),
    )
}
