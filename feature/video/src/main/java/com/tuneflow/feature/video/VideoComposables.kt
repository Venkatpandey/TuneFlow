package com.tuneflow.feature.video

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.NumberFormat

@Composable
fun NativeVideoPlayerSurface(
    player: VideoSurfacePlayer,
    host: FrameLayout,
    bounds: IntRect,
    requestFocus: Boolean,
    onKeyEvent: (KeyEvent) -> Boolean,
    onExitFullscreen: () -> Unit = {},
    onChooseAnother: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val view = remember(player, host) { player.createSurfaceView(host.context) }
    val controlsView =
        remember(player, host, requestFocus) {
            if (player.isNative && requestFocus) {
                ComposeView(host.context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent {
                        MaterialTheme {
                            NativeVideoControlsOverlay(player, onExitFullscreen, onChooseAnother, onStop)
                        }
                    }
                }
            } else {
                null
            }
        }
    val currentOnKeyEvent = rememberUpdatedState(onKeyEvent)
    DisposableEffect(player, host, view, controlsView) {
        (view.parent as? ViewGroup)?.removeView(view)
        host.removeAllViews()
        host.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        controlsView?.let { overlay ->
            host.addView(
                overlay,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        host.visibility = View.VISIBLE
        host.bringToFront()
        view.setOnKeyListener { _, _, event -> currentOnKeyEvent.value(event) }
        controlsView?.setOnKeyListener { _, _, event -> currentOnKeyEvent.value(event) }
        view.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) player.focusPlayer() else player.clearPlayerFocus()
            }
        onDispose {
            view.setOnKeyListener(null)
            controlsView?.setOnKeyListener(null)
            view.onFocusChangeListener = null
            host.removeView(view)
            controlsView?.let(host::removeView)
            host.visibility = View.GONE
            player.disposeSurfaceView(view)
        }
    }

    DisposableEffect(host, bounds) {
        val currentBounds = host.layoutParams as? FrameLayout.LayoutParams
        if (
            currentBounds == null ||
            currentBounds.width != bounds.width ||
            currentBounds.height != bounds.height ||
            currentBounds.leftMargin != bounds.left ||
            currentBounds.topMargin != bounds.top
        ) {
            host.layoutParams =
                FrameLayout.LayoutParams(bounds.width, bounds.height).apply {
                    leftMargin = bounds.left
                    topMargin = bounds.top
                }
        }
        host.bringToFront()
        view.post {
            view.requestLayout()
            view.invalidate()
            player.onSurfaceBoundsChanged(view)
            controlsView?.bringToFront()
        }
        onDispose { }
    }

    DisposableEffect(view, requestFocus) {
        val focusView = controlsView ?: view
        focusView.isFocusable = requestFocus
        focusView.isFocusableInTouchMode = requestFocus
        if (requestFocus) {
            focusView.requestFocus()
            player.focusPlayer()
        } else {
            player.clearPlayerFocus()
            focusView.clearFocus()
        }
        onDispose { }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun NativeVideoControlsOverlay(
    player: VideoSurfacePlayer,
    onExitFullscreen: () -> Unit,
    onChooseAnother: () -> Unit,
    onStop: () -> Unit,
) {
    val playerState by player.state.collectAsState()
    val controls by player.nativeControls.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    var activityToken by remember { mutableStateOf(0L) }
    var qualityMenuVisible by remember { mutableStateOf(false) }
    var captionMenuVisible by remember { mutableStateOf(false) }
    val rootFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val positionMs = playerState.positionMsForControls()
    val durationMs = playerState.durationMsForControls()

    LaunchedEffect(activityToken, controlsVisible) {
        if (controlsVisible) {
            delay(NATIVE_CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
            qualityMenuVisible = false
            captionMenuVisible = false
        }
    }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) playFocusRequester.requestFocus() else rootFocusRequester.requestFocus()
    }
    BackHandler {
        if (controlsVisible) {
            controlsVisible = false
            qualityMenuVisible = false
            captionMenuVisible = false
        } else {
            onExitFullscreen()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(rootFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (nativeControlAction(event.nativeKeyEvent.keyCode, controlsVisible)) {
                        NativeControlAction.ShowControls -> {
                            controlsVisible = true
                            activityToken += 1
                            true
                        }
                        NativeControlAction.HideControls -> {
                            controlsVisible = false
                            qualityMenuVisible = false
                            captionMenuVisible = false
                            true
                        }
                        NativeControlAction.ExitFullscreen -> {
                            onExitFullscreen()
                            true
                        }
                        NativeControlAction.SeekBack -> {
                            player.seekTo((positionMs - NATIVE_SEEK_MS).coerceAtLeast(0L))
                            true
                        }
                        NativeControlAction.SeekForward -> {
                            player.seekTo((positionMs + NATIVE_SEEK_MS).coerceAtMost(durationMs.coerceAtLeast(positionMs)))
                            true
                        }
                        NativeControlAction.None -> {
                            if (controlsVisible) activityToken += 1
                            false
                        }
                    }
                },
    ) {
        if (controlsVisible) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(horizontal = 34.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = { if (durationMs > 0L) positionMs.toFloat() / durationMs else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).progressSemantics(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatVideoTime(positionMs), color = Color.White)
                    Text(formatVideoTime(durationMs), color = Color.White)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VideoTextButton(
                        label = if (playerState is EmbeddedVideoPlayerState.Playing) "Pause" else "Play",
                        enabled = true,
                        accent = true,
                        onClick = {
                            if (playerState is EmbeddedVideoPlayerState.Playing) player.pause() else player.play()
                            activityToken += 1
                        },
                        modifier = Modifier.focusRequester(playFocusRequester).width(108.dp).height(44.dp),
                    )
                    if (controls.qualities.isNotEmpty()) {
                        VideoTextButton(
                            label =
                                "Quality: ${controls.qualities.firstOrNull { it.id == controls.selectedQualityId }?.label ?: "Auto"}",
                            enabled = true,
                            accent = false,
                            onClick = {
                                qualityMenuVisible = !qualityMenuVisible
                                captionMenuVisible = false
                                activityToken += 1
                            },
                            modifier = Modifier.width(220.dp).height(44.dp),
                        )
                    }
                    if (controls.captions.isNotEmpty()) {
                        VideoTextButton(
                            label =
                                controls.captions.firstOrNull { it.id == controls.selectedCaptionId }
                                    ?.let { "CC: ${it.label}" } ?: "Captions: Off",
                            enabled = true,
                            accent = false,
                            onClick = {
                                captionMenuVisible = !captionMenuVisible
                                qualityMenuVisible = false
                                activityToken += 1
                            },
                            modifier = Modifier.width(200.dp).height(44.dp),
                        )
                    }
                    controls.activeFormatLabel?.let { Text(it, color = Color.White) }
                }
                if (qualityMenuVisible) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        controls.qualities.forEach { option ->
                            VideoTextButton(
                                label = option.label,
                                enabled = true,
                                accent = option.id == controls.selectedQualityId,
                                onClick = {
                                    player.selectQuality(option.id)
                                    qualityMenuVisible = false
                                    activityToken += 1
                                },
                                modifier = Modifier.width(150.dp).height(40.dp),
                            )
                        }
                    }
                }
                if (captionMenuVisible) {
                    LazyColumn(
                        modifier = Modifier.width(320.dp).height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            VideoTextButton(
                                label = "Off",
                                enabled = true,
                                accent = controls.selectedCaptionId == null,
                                onClick = {
                                    player.selectCaption(null)
                                    captionMenuVisible = false
                                    activityToken += 1
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                            )
                        }
                        itemsIndexed(
                            controls.captions,
                            key = { index, option -> "${option.id}:$index" },
                        ) { _, option ->
                            VideoTextButton(
                                label = option.label,
                                enabled = true,
                                accent = option.id == controls.selectedCaptionId,
                                onClick = {
                                    player.selectCaption(option.id)
                                    captionMenuVisible = false
                                    activityToken += 1
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                            )
                        }
                    }
                }
                VideoSessionActions(onChooseAnother = onChooseAnother, onStop = onStop)
            }
        }
    }
}

internal enum class NativeControlAction {
    ShowControls,
    HideControls,
    ExitFullscreen,
    SeekBack,
    SeekForward,
    None,
}

internal fun nativeControlAction(
    keyCode: Int,
    controlsVisible: Boolean,
): NativeControlAction =
    when (keyCode) {
        KeyEvent.KEYCODE_BACK -> if (controlsVisible) NativeControlAction.HideControls else NativeControlAction.ExitFullscreen
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        -> if (controlsVisible) NativeControlAction.None else NativeControlAction.ShowControls
        KeyEvent.KEYCODE_DPAD_LEFT -> if (controlsVisible) NativeControlAction.None else NativeControlAction.SeekBack
        KeyEvent.KEYCODE_DPAD_RIGHT -> if (controlsVisible) NativeControlAction.None else NativeControlAction.SeekForward
        else -> NativeControlAction.None
    }

private fun EmbeddedVideoPlayerState.positionMsForControls(): Long =
    when (this) {
        is EmbeddedVideoPlayerState.Playing -> positionMs
        is EmbeddedVideoPlayerState.Paused -> positionMs
        is EmbeddedVideoPlayerState.Buffering -> positionMs
        is EmbeddedVideoPlayerState.Ended -> positionMs
        else -> 0L
    }

private fun EmbeddedVideoPlayerState.durationMsForControls(): Long =
    when (this) {
        is EmbeddedVideoPlayerState.Ready -> durationMs
        is EmbeddedVideoPlayerState.Playing -> durationMs
        is EmbeddedVideoPlayerState.Paused -> durationMs
        is EmbeddedVideoPlayerState.Buffering -> durationMs
        is EmbeddedVideoPlayerState.Ended -> durationMs
        else -> 0L
    }

private fun formatVideoTime(positionMs: Long): String {
    val seconds = positionMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}

private const val NATIVE_CONTROLS_HIDE_DELAY_MS = 3_000L
private const val NATIVE_SEEK_MS = 10_000L

@Composable
fun VideoActionButton(
    state: VideoUiState,
    enabled: Boolean,
    requestFocusId: Long,
    requestFocus: Boolean,
    onRequestedFocusApplied: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        when (state) {
            is VideoUiState.Searching -> "Searching..."
            is VideoUiState.Loading -> "Loading..."
            is VideoUiState.Playing -> if (state.presentation == VideoPresentationMode.Mini) "Full screen" else "Video"
            is VideoUiState.Candidates -> "Choose video"
            else -> "Video"
        }
    VideoTextButton(
        label = label,
        enabled = enabled,
        accent = state is VideoUiState.Playing || state is VideoUiState.Candidates,
        requestFocusId = requestFocusId,
        requestFocus = requestFocus,
        onRequestedFocusApplied = onRequestedFocusApplied,
        onClick = onClick,
        modifier = modifier.width(118.dp).height(44.dp),
    )
}

@Composable
fun VideoSessionActions(
    onChooseAnother: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VideoTextButton(
            label = "Choose another video",
            enabled = true,
            accent = false,
            onClick = onChooseAnother,
            modifier = Modifier.width(210.dp).height(44.dp),
        )
        VideoTextButton(
            label = "Stop video",
            enabled = true,
            accent = false,
            onClick = onStop,
            modifier = Modifier.width(140.dp).height(44.dp),
        )
    }
}

@Composable
fun VideoCandidatePicker(
    candidates: List<VideoCandidate>,
    onSelect: (VideoCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstCandidateFocusRequester = remember { FocusRequester() }
    LaunchedEffect(candidates.firstOrNull()?.videoId) {
        if (candidates.isNotEmpty()) firstCandidateFocusRequester.requestFocus()
    }
    Column(
        modifier =
            modifier
                .width(360.dp)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), MaterialTheme.shapes.large)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Choose a YouTube video",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Text(
            text = "${candidates.size} matches. Select one to play full screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(candidates, key = { _, candidate -> candidate.videoId }) { index, candidate ->
                VideoCandidateRow(
                    candidate = candidate,
                    onClick = { onSelect(candidate) },
                    modifier =
                        if (index == 0) {
                            Modifier.focusRequester(firstCandidateFocusRequester)
                        } else {
                            Modifier
                        },
                )
            }
        }
    }
}

@Composable
fun VideoDisclosureOverlay(
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).testTag("video-disclosure"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .width(620.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                    .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Use YouTube video?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    "TuneFlow sends this song's artist, title, album, duration, and your device/network " +
                        "information to YouTube for search and playback. Navidrome credentials, server URL, " +
                        "queue, and listening history are not sent. YouTube controls, branding, links, and ads remain visible.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                VideoTextButton(
                    label = "Continue",
                    enabled = true,
                    accent = true,
                    requestFocusId = 1L,
                    onClick = onAccept,
                    modifier = Modifier.width(150.dp).height(48.dp),
                )
                VideoTextButton(
                    label = "Cancel",
                    enabled = true,
                    accent = false,
                    onClick = onCancel,
                    modifier = Modifier.width(120.dp).height(48.dp),
                )
            }
        }
    }
}

@Composable
fun VideoErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.84f))
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        VideoTextButton(
            label = "Retry",
            enabled = true,
            accent = false,
            requestFocusId = 1L,
            onClick = onRetry,
            modifier = Modifier.width(100.dp).height(40.dp),
        )
    }
}

@Composable
private fun VideoCandidateRow(
    candidate: VideoCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.Transparent,
                )
                .border(
                    if (focused) 2.dp else 1.dp,
                    if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                    MaterialTheme.shapes.medium,
                )
                .clickable(onClick = onClick)
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = candidate.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 112.dp, height = 63.dp).clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = candidate.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    "${formatVideoViewCount(candidate.viewCount)} • ${candidate.publisher} • " +
                        formatVideoDuration(candidate.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VideoTextButton(
    label: String,
    enabled: Boolean,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    requestFocusId: Long = 0L,
    requestFocus: Boolean = false,
    onRequestedFocusApplied: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    LaunchedEffect(requestFocusId, requestFocus) {
        if (requestFocusId > 0L || requestFocus) {
            runCatching { requester.requestFocus() }
            if (requestFocus) onRequestedFocusApplied()
        }
    }
    Box(
        modifier =
            modifier
                .focusRequester(requester)
                .scale(if (focused) 1.03f else 1f)
                .alpha(if (enabled) 1f else 0.45f)
                .onFocusChanged { focused = it.hasFocus }
                .focusable(enabled)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (focused || accent) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    },
                )
                .border(
                    if (focused) 2.dp else 1.dp,
                    if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                    MaterialTheme.shapes.medium,
                )
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatVideoDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}

private fun formatVideoViewCount(viewCount: Long): String = "${NumberFormat.getIntegerInstance().format(viewCount.coerceAtLeast(0L))} views"
