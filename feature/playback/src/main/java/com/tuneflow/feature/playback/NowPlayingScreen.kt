package com.tuneflow.feature.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuneflow.core.design.TuneFlowShapes
import android.view.KeyEvent as AndroidKeyEvent

@Composable
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    streamModeLabel: String,
    onCycleStreamMode: () -> Unit,
    autoFocusTransport: Boolean,
    onAutoFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val item = state.queue.currentItem
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val artSize by animateDpAsState(targetValue = if (showQueue) 152.dp else 180.dp, label = "now-playing-art-size")
    val artFrameHeight by animateDpAsState(targetValue = if (showQueue) 176.dp else 200.dp, label = "now-playing-art-frame-height")

    DisposableEffect(Unit) {
        viewModel.setActive(true)
        onDispose { viewModel.setActive(false) }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event -> handleNowPlayingKeyEvent(event, showQueue, { showQueue = false }, viewModel) },
    ) {
        if (item?.artUrl != null) {
            AsyncImage(
                model = item.artUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.18f,
            )
        }

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
                artSize = artSize,
                artFrameHeight = artFrameHeight,
                streamModeLabel = streamModeLabel,
                showQueue = showQueue,
                onCycleStreamMode = onCycleStreamMode,
                onToggleQueue = { showQueue = !showQueue },
                onRetry = viewModel::retry,
                onPrevious = viewModel::previous,
                onTogglePlayPause = viewModel::togglePlayPause,
                onNext = viewModel::next,
                compactTransport = showQueue,
                autoFocusTransport = autoFocusTransport,
                onAutoFocusConsumed = onAutoFocusConsumed,
            )

            AnimatedVisibility(
                visible = showQueue,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 }),
            ) {
                QueuePanel(
                    title = "Track List",
                    state = state,
                    onSelectTrack = viewModel::playFromIndex,
                )
            }
        }
    }
}

@Composable
internal fun StreamBadge(label: String) {
    Box(
        modifier =
            Modifier
                .clip(TuneFlowShapes.badge)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = TuneFlowShapes.badge,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun StreamModeButton(
    label: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .scale(if (focused) 1.01f else 1f)
                .clip(TuneFlowShapes.button)
                .background(
                    if (focused) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f)
                    },
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
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
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

private fun handleNowPlayingKeyEvent(
    event: androidx.compose.ui.input.key.KeyEvent,
    showQueue: Boolean,
    onCloseQueue: () -> Unit,
    viewModel: PlaybackViewModel,
): Boolean {
    if (
        showQueue &&
        event.type == KeyEventType.KeyDown &&
        event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BACK
    ) {
        onCloseQueue()
        return true
    }

    return handleTransportMediaKey(event, viewModel)
}

@Composable
private fun QueuePanel(
    title: String,
    state: NowPlayingUiState,
    onSelectTrack: (Int) -> Unit,
) {
    val currentFocusRequester = remember { FocusRequester() }
    val currentIndex = state.queue.currentIndex
    val hasCurrentQueueItem = currentIndex in state.queue.items.indices

    LaunchedEffect(hasCurrentQueueItem, currentIndex) {
        if (hasCurrentQueueItem) {
            runCatching { currentFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier =
            Modifier
                .width(312.dp)
                .fillMaxHeight()
                .clip(TuneFlowShapes.card)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    shape = TuneFlowShapes.card,
                )
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(state.queue.items, key = { _, track -> track.id }) { index, track ->
                QueueRow(
                    title = track.title,
                    subtitle = track.artist,
                    isCurrent = index == currentIndex,
                    onClick = { onSelectTrack(index) },
                    modifier =
                        if (index == currentIndex) {
                            Modifier.focusRequester(currentFocusRequester)
                        } else {
                            Modifier
                        },
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
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
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

@Composable
internal fun PlaybackStatusCard(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TuneFlowShapes.card)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = TuneFlowShapes.card,
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
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            color = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
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
