package com.tuneflow.feature.playback

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.player.PlaybackMode
import com.tuneflow.core.player.QueueItem
import com.tuneflow.feature.video.VideoActionButton
import com.tuneflow.feature.video.VideoControlIconButton
import com.tuneflow.feature.video.VideoErrorCard
import com.tuneflow.feature.video.VideoUiState
import com.tuneflow.feature.video.hasVisiblePlayer
import kotlin.math.roundToInt
import com.tuneflow.feature.video.R as VideoR

@Composable
private fun VideoMiniViewport(onBoundsChanged: (IntRect) -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(270.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    onBoundsChanged(
                        IntRect(
                            left = bounds.left.roundToInt(),
                            top = bounds.top.roundToInt(),
                            right = bounds.right.roundToInt(),
                            bottom = bounds.bottom.roundToInt(),
                        ),
                    )
                }
                .clip(TuneFlowShapes.container)
                .background(androidx.compose.ui.graphics.Color.Black)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
                    shape = TuneFlowShapes.container,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Loading YouTube...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NowPlayingPrimaryColumn(
    item: QueueItem?,
    state: NowPlayingUiState,
    videoState: VideoUiState,
    artSize: Dp,
    artFrameHeight: Dp,
    streamModeLabel: String,
    activePanel: NowPlayingPanel,
    hasLyrics: Boolean,
    onCycleStreamMode: () -> Unit,
    onToggleQueue: () -> Unit,
    onToggleLyrics: () -> Unit,
    onVideoAction: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onStopVideo: () -> Unit,
    onVideoViewportBoundsChanged: (IntRect?) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    compactTransport: Boolean,
    autoFocusTransport: Boolean,
    autoFocusStreamMode: Boolean,
    autoFocusQueue: Boolean,
    autoFocusLyrics: Boolean,
    autoFocusVideo: Boolean,
    onAutoFocusConsumed: () -> Unit,
    onStreamModeFocusConsumed: () -> Unit,
    onQueueFocusConsumed: () -> Unit,
    onLyricsFocusConsumed: () -> Unit,
    onVideoFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(videoState.hasVisiblePlayer) {
        if (!videoState.hasVisiblePlayer) onVideoViewportBoundsChanged(null)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (videoState.hasVisiblePlayer) {
            VideoMiniViewport(onBoundsChanged = onVideoViewportBoundsChanged)
        } else {
            ArtworkCard(
                item = item,
                artSize = artSize,
                artFrameHeight = artFrameHeight,
            )
        }
        TrackMetadata(item = item)
        StreamControlRow(
            streamModeLabel = streamModeLabel,
            bitrateLabel = item?.streamBitrateLabel ?: "--",
            activePanel = activePanel,
            hasLyrics = hasLyrics,
            videoState = videoState,
            videoEnabled = item != null,
            onCycleStreamMode = onCycleStreamMode,
            autoFocusStreamMode = autoFocusStreamMode,
            onStreamModeFocusConsumed = onStreamModeFocusConsumed,
            onToggleQueue = onToggleQueue,
            onToggleLyrics = onToggleLyrics,
            onVideoAction = onVideoAction,
            playbackMode = state.playbackMode,
            onCyclePlaybackMode = onCyclePlaybackMode,
            autoFocusQueue = autoFocusQueue,
            autoFocusLyrics = autoFocusLyrics,
            autoFocusVideo = autoFocusVideo,
            onQueueFocusConsumed = onQueueFocusConsumed,
            onLyricsFocusConsumed = onLyricsFocusConsumed,
            onVideoFocusConsumed = onVideoFocusConsumed,
        )

        (videoState as? VideoUiState.Error)?.let { error ->
            VideoErrorCard(message = error.message, onRetry = onVideoAction)
        }
        (videoState as? VideoUiState.Unavailable)?.let { unavailable ->
            Text(
                text = unavailable.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.statusMessage?.let {
            PlaybackStatusCard(
                message = it,
                onRetry = onRetry,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        PlaybackProgress(
            positionMs = (videoState as? VideoUiState.Playing)?.positionMs ?: state.positionMs,
            durationMs = (videoState as? VideoUiState.Playing)?.durationMs ?: state.durationMs,
        )
        TransportControls(
            isPlaying = (videoState as? VideoUiState.Playing)?.isPlaying ?: state.isPlaying,
            videoActive = videoState.hasVisiblePlayer,
            videoFocusRequestId =
                when (videoState) {
                    is VideoUiState.Loading -> videoState.focusRequestId
                    is VideoUiState.Playing -> videoState.focusRequestId
                    else -> 0L
                },
            onPrevious = onPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
            onEnterFullscreen = onEnterFullscreen,
            onStopVideo = onStopVideo,
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
                .clip(TuneFlowShapes.artwork)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .animateContentSize()
                .padding(10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .size(artSize)
                    .clip(TuneFlowShapes.artwork)
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
    activePanel: NowPlayingPanel,
    hasLyrics: Boolean,
    videoState: VideoUiState,
    videoEnabled: Boolean,
    playbackMode: PlaybackMode,
    onCycleStreamMode: () -> Unit,
    autoFocusStreamMode: Boolean,
    onStreamModeFocusConsumed: () -> Unit,
    onToggleQueue: () -> Unit,
    onToggleLyrics: () -> Unit,
    onVideoAction: () -> Unit,
    onCyclePlaybackMode: () -> Unit,
    autoFocusQueue: Boolean,
    autoFocusLyrics: Boolean,
    autoFocusVideo: Boolean,
    onQueueFocusConsumed: () -> Unit,
    onLyricsFocusConsumed: () -> Unit,
    onVideoFocusConsumed: () -> Unit,
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
        QueueToggleIconButton(
            active = activePanel == NowPlayingPanel.TrackList,
            onClick = onToggleQueue,
            requestFocus = autoFocusQueue,
            onRequestedFocusApplied = onQueueFocusConsumed,
        )
        if (hasLyrics) {
            LyricsToggleButton(
                active = activePanel == NowPlayingPanel.Lyrics,
                onClick = onToggleLyrics,
                requestFocus = autoFocusLyrics,
                onRequestedFocusApplied = onLyricsFocusConsumed,
            )
        }
        VideoActionButton(
            state = videoState,
            enabled =
                videoEnabled &&
                    videoState !is VideoUiState.Unavailable &&
                    videoState !is VideoUiState.Searching &&
                    videoState !is VideoUiState.Loading &&
                    videoState !is VideoUiState.ConsentRequired,
            requestFocus = autoFocusVideo,
            onRequestedFocusApplied = onVideoFocusConsumed,
            onClick = onVideoAction,
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

    VideoControlIconButton(
        iconResId = iconRes,
        contentDescription =
            when (playbackMode) {
                PlaybackMode.Default -> "Playback mode default"
                PlaybackMode.Shuffle -> "Playback mode shuffle"
                PlaybackMode.Loop -> "Playback mode loop"
            },
        accent = active,
        onClick = onClick,
        modifier = Modifier.size(44.dp),
    )
}

@Composable
private fun QueueToggleIconButton(
    active: Boolean,
    onClick: () -> Unit,
    requestFocus: Boolean,
    onRequestedFocusApplied: () -> Unit,
) {
    VideoControlIconButton(
        iconResId = if (active) R.drawable.tracklist_enabled else R.drawable.tracklist_disabled,
        contentDescription = if (active) "Hide track list" else "Show track list",
        accent = active,
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        requestFocus = requestFocus,
        onRequestedFocusApplied = onRequestedFocusApplied,
    )
}

@Composable
private fun LyricsToggleButton(
    active: Boolean,
    onClick: () -> Unit,
    requestFocus: Boolean,
    onRequestedFocusApplied: () -> Unit,
) {
    VideoControlIconButton(
        iconResId = R.drawable.ic_lyrics,
        contentDescription = if (active) "Hide lyrics" else "Show lyrics",
        accent = active,
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        requestFocus = requestFocus,
        onRequestedFocusApplied = onRequestedFocusApplied,
    )
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
    videoActive: Boolean,
    videoFocusRequestId: Long,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onStopVideo: () -> Unit,
    compact: Boolean,
    autoFocusTransport: Boolean,
    onAutoFocusConsumed: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoControlIconButton(
            iconResId = if (videoActive) VideoR.drawable.smarttube_ic_rewind else VideoR.drawable.smarttube_ic_skip_previous,
            contentDescription = if (videoActive) "Rewind 10 seconds" else "Previous",
            onClick = onPrevious,
        )
        VideoControlIconButton(
            iconResId = if (isPlaying) VideoR.drawable.smarttube_ic_pause else VideoR.drawable.smarttube_ic_play,
            contentDescription = if (isPlaying) "Pause" else "Play",
            onClick = onTogglePlayPause,
            accent = true,
            modifier = Modifier.size(width = 64.dp, height = 50.dp),
            requestFocus = autoFocusTransport,
            onRequestedFocusApplied = onAutoFocusConsumed,
        )
        VideoControlIconButton(
            iconResId = if (videoActive) VideoR.drawable.smarttube_ic_fast_forward else VideoR.drawable.smarttube_ic_skip_next,
            contentDescription = if (videoActive) "Fast-forward 10 seconds" else "Next",
            onClick = onNext,
        )
        if (videoActive) {
            VideoControlIconButton(
                iconResId = VideoR.drawable.smarttube_ic_fullscreen,
                contentDescription = "Full screen",
                onClick = onEnterFullscreen,
                requestFocusId = videoFocusRequestId,
            )
            VideoControlIconButton(
                iconResId = VideoR.drawable.smarttube_ic_stop,
                contentDescription = "Stop video",
                onClick = onStopVideo,
            )
        }
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
