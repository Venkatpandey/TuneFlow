package com.tuneflow.feature.playback

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import com.tuneflow.core.player.EqualizerState
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.player.PlaybackMode
import com.tuneflow.core.player.QueueItem

@Composable
internal fun NowPlayingPrimaryColumn(
    item: QueueItem?,
    state: NowPlayingUiState,
    artSize: Dp,
    artFrameHeight: Dp,
    streamModeLabel: String,
    showQueue: Boolean,
    equalizerState: EqualizerState,
    onCycleStreamMode: () -> Unit,
    onToggleQueue: () -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onCycleEqualizerPreset: () -> Unit,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    compactTransport: Boolean,
    autoFocusTransport: Boolean,
    autoFocusStreamMode: Boolean,
    onAutoFocusConsumed: () -> Unit,
    onStreamModeFocusConsumed: () -> Unit,
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
            equalizerState = equalizerState,
            onCycleStreamMode = onCycleStreamMode,
            autoFocusStreamMode = autoFocusStreamMode,
            onStreamModeFocusConsumed = onStreamModeFocusConsumed,
            onToggleQueue = onToggleQueue,
            playbackMode = state.playbackMode,
            onCyclePlaybackMode = onCyclePlaybackMode,
            onCycleEqualizerPreset = onCycleEqualizerPreset,
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
            TuneFlowArtwork(
                model = item?.artUrl,
                contentDescription = item?.title,
                width = artSize,
                height = artSize,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholderText = item?.title,
                fallbackPainterResId = R.drawable.ic_tuneflow_brand,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
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
        overflow = TextOverflow.Clip,
        modifier = Modifier.fillMaxWidth().basicMarquee(),
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
internal fun StreamBadge(label: String) {
    StreamMetaBadge(label = label)
}

@Composable
internal fun StreamModeButton(
    label: String,
    onClick: () -> Unit,
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
            Modifier
                .size(44.dp)
                .focusRequester(focusRequester)
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
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun StreamControlRow(
    streamModeLabel: String,
    bitrateLabel: String,
    showQueue: Boolean,
    equalizerState: EqualizerState,
    playbackMode: PlaybackMode,
    onCycleStreamMode: () -> Unit,
    autoFocusStreamMode: Boolean,
    onStreamModeFocusConsumed: () -> Unit,
    onToggleQueue: () -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onCycleEqualizerPreset: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        StreamModeButton(
            label = streamModeLabel,
            onClick = onCycleStreamMode,
            requestFocus = autoFocusStreamMode,
            onRequestedFocusApplied = onStreamModeFocusConsumed,
        )
        StreamBadge(label = bitrateLabel)
        PlaybackModeIconButton(
            playbackMode = playbackMode,
            onClick = onCyclePlaybackMode,
        )
        EqualizerPresetButton(
            equalizerState = equalizerState,
            onClick = onCycleEqualizerPreset,
        )
        EqualizerPresetBadge(label = equalizerState.displayLabel)
        QueueToggleIconButton(
            active = showQueue,
            onClick = onToggleQueue,
        )
    }
}

@Composable
private fun StreamMetaBadge(label: String) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(TuneFlowShapes.button)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = TuneFlowShapes.button,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EqualizerPresetBadge(label: String) {
    Box(
        modifier =
            Modifier
                .width(92.dp)
                .height(44.dp)
                .clip(TuneFlowShapes.button)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = TuneFlowShapes.button,
                )
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaybackModeIconButton(
    playbackMode: PlaybackMode,
    onClick: () -> Unit,
) {
    val (iconRes, active) =
        when (playbackMode) {
            PlaybackMode.Default -> R.drawable.shuffle_disabled to false
            PlaybackMode.Shuffle -> R.drawable.shuffle_enabled to true
            PlaybackMode.Loop -> R.drawable.loop_enabled to true
        }

    PlaybackStateIconButton(
        iconResId = iconRes,
        contentDescription =
            when (playbackMode) {
                PlaybackMode.Default -> "Playback mode default"
                PlaybackMode.Shuffle -> "Playback mode shuffle"
                PlaybackMode.Loop -> "Playback mode loop"
            },
        active = active,
        onClick = onClick,
    )
}

@Composable
private fun QueueToggleIconButton(
    active: Boolean,
    onClick: () -> Unit,
) {
    PlaybackStateIconButton(
        iconResId = if (active) R.drawable.tracklist_enabled else R.drawable.tracklist_disabled,
        contentDescription = if (active) "Hide track list" else "Show track list",
        active = active,
        onClick = onClick,
    )
}

@Composable
private fun EqualizerPresetButton(
    equalizerState: EqualizerState,
    onClick: () -> Unit,
) {
    PlaybackStateIconButton(
        iconResId = R.drawable.eq_preset,
        contentDescription = "Cycle equalizer preset",
        active = equalizerState.isSupported && equalizerState.selectedPreset != com.tuneflow.core.player.EqualizerPreset.Original,
        enabled = equalizerState.isSupported,
        onClick = onClick,
    )
}

@Composable
private fun PlaybackStateIconButton(
    iconResId: Int,
    contentDescription: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .size(44.dp)
                .scale(if (focused) 1.03f else 1f)
                .clip(TuneFlowShapes.button)
                .background(
                    if (!enabled) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                    } else if (focused || active) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                    },
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        if (!enabled) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        } else if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else if (active) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = TuneFlowShapes.button,
                )
                .onFocusChanged { focusState -> focused = focusState.hasFocus }
                .focusable(enabled = enabled)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else 0.45f,
        )
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
                .onFocusChanged { focusState -> focused = focusState.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
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
