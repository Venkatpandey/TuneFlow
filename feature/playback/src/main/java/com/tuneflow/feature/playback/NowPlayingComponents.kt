package com.tuneflow.feature.playback

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.player.QueueItem

@Composable
internal fun NowPlayingPrimaryColumn(
    item: QueueItem?,
    state: NowPlayingUiState,
    artSize: Dp,
    artFrameHeight: Dp,
    streamModeLabel: String,
    showQueue: Boolean,
    onCycleStreamMode: () -> Unit,
    onToggleQueue: () -> Unit,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    compactTransport: Boolean,
    autoFocusTransport: Boolean,
    onAutoFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ArtworkCard(
            item = item,
            artSize = artSize,
            artFrameHeight = artFrameHeight,
        )
        TrackMetadata(item = item)
        StreamControlRow(
            streamModeLabel = streamModeLabel,
            bitrateLabel = item?.streamBitrateLabel ?: "--",
            showQueue = showQueue,
            onCycleStreamMode = onCycleStreamMode,
            onToggleQueue = onToggleQueue,
        )

        state.statusMessage?.let {
            PlaybackStatusCard(
                message = it,
                onRetry = onRetry,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        PlaybackProgress(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
        )
        TransportControls(
            isPlaying = state.isPlaying,
            onPrevious = onPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
            compact = compactTransport,
            autoFocusTransport = autoFocusTransport,
            onAutoFocusConsumed = onAutoFocusConsumed,
        )
    }
}

@Composable
internal fun ArtworkCard(
    item: QueueItem?,
    artSize: Dp,
    artFrameHeight: Dp,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(artFrameHeight)
                .clip(TuneFlowShapes.albumArt)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .animateContentSize()
                .padding(10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .size(artSize)
                    .clip(TuneFlowShapes.albumArt)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            if (item?.artUrl != null) {
                AsyncImage(
                    model = item.artUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "TuneFlow",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
internal fun TrackMetadata(item: QueueItem?) {
    Text(
        text = item?.title ?: "Nothing playing",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = item?.artist ?: "",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = item?.album ?: "",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun StreamControlRow(
    streamModeLabel: String,
    bitrateLabel: String,
    showQueue: Boolean,
    onCycleStreamMode: () -> Unit,
    onToggleQueue: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StreamModeButton(
            label = streamModeLabel,
            onClick = onCycleStreamMode,
        )
        StreamModeButton(
            label = if (showQueue) "Hide List" else "Track List",
            onClick = onToggleQueue,
        )
        StreamBadge(label = bitrateLabel)
    }
}

@Composable
internal fun PlaybackProgress(
    positionMs: Long,
    durationMs: Long,
) {
    val progress =
        if (durationMs > 0) {
            positionMs.toFloat() / durationMs.toFloat()
        } else {
            0f
        }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(positionMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun TransportControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    compact: Boolean,
    autoFocusTransport: Boolean,
    onAutoFocusConsumed: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = if (compact) 10.dp else 16.dp
        val desiredSideSize = if (compact) 74.dp else 82.dp
        val desiredCenterSize = if (compact) 88.dp else 98.dp
        val desiredTotal = (desiredSideSize * 2) + desiredCenterSize
        val availableButtonWidth = (maxWidth - (gap * 2)).coerceAtLeast(0.dp)
        val fitScale =
            if (desiredTotal > 0.dp) {
                minOf(1f, availableButtonWidth / desiredTotal)
            } else {
                1f
            }
        val focusReserve = 1.08f
        val sideButtonSize = desiredSideSize * fitScale
        val centerButtonSize = desiredCenterSize * fitScale
        val sideSlotWidth = sideButtonSize * focusReserve
        val centerSlotWidth = centerButtonSize * focusReserve

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(sideSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                PlaybackIconButton(
                    iconResId = R.drawable.playback_control_prev,
                    contentDescription = "Previous",
                    onClick = onPrevious,
                    buttonSize = sideButtonSize,
                )
            }
            Box(
                modifier = Modifier.width(centerSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                PlaybackIconButton(
                    iconResId = if (isPlaying) R.drawable.playback_control_pause else R.drawable.playback_control_play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = onTogglePlayPause,
                    buttonSize = centerButtonSize,
                    requestFocus = autoFocusTransport,
                    onRequestedFocusApplied = onAutoFocusConsumed,
                )
            }
            Box(
                modifier = Modifier.width(sideSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                PlaybackIconButton(
                    iconResId = R.drawable.playback_control_next,
                    contentDescription = "Next",
                    onClick = onNext,
                    buttonSize = sideButtonSize,
                )
            }
        }
    }
}

@Composable
internal fun PlaybackIconButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    onRequestedFocusApplied: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec =
            tween(
                durationMillis = if (focused) 150 else 100,
                easing = if (focused) FastOutSlowInEasing else LinearOutSlowInEasing,
            ),
        label = "playbackIconScale",
    )

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onRequestedFocusApplied()
        }
    }

    Box(
        modifier =
            modifier
                .size(buttonSize)
                .focusRequester(focusRequester)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (focused) 0.22f else 0.08f))
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                        },
                    shape = CircleShape,
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            contentScale = ContentScale.Fit,
        )
    }
}

internal fun formatTime(ms: Long): String {
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
