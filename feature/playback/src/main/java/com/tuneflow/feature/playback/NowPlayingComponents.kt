package com.tuneflow.feature.playback

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .animateContentSize()
                .padding(10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(16.dp))
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
    val sideButtonWidth = if (compact) 124.dp else 148.dp
    val centerButtonWidth = if (compact) 144.dp else 172.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp, Alignment.Start),
    ) {
        PlaybackTextButton(
            label = "Previous",
            onClick = onPrevious,
            modifier = Modifier.width(sideButtonWidth),
        )
        PlaybackTextButton(
            label = if (isPlaying) "Pause" else "Play",
            accent = true,
            onClick = onTogglePlayPause,
            modifier = Modifier.width(centerButtonWidth),
            requestFocus = autoFocusTransport,
            onRequestedFocusApplied = onAutoFocusConsumed,
        )
        PlaybackTextButton(
            label = "Next",
            onClick = onNext,
            modifier = Modifier.width(sideButtonWidth),
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
