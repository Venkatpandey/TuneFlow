package com.tuneflow.feature.playback

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.view.KeyEvent as AndroidKeyEvent

@Composable
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val item = state.queue.currentItem

    val progress =
        if (state.durationMs > 0) {
            state.positionMs.toFloat() / state.durationMs.toFloat()
        } else {
            0f
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event -> handlePlaybackKey(event, viewModel) },
    ) {
        if (item?.artUrl != null) {
            AsyncImage(
                model = item.artUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.22f,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.52f)),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 12.dp)
                        .height(320.dp)
                        .fillMaxWidth(0.34f)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(28.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (item?.artUrl != null) {
                    AsyncImage(
                        model = item.artUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                } else {
                    Text(
                        text = "TuneFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = item?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
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

            Spacer(Modifier.height(22.dp))

            Column(modifier = Modifier.fillMaxWidth(0.54f)) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTime(state.positionMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTime(state.durationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlaybackTextButton(
                    label = "Previous",
                    onClick = viewModel::previous,
                )
                PlaybackTextButton(
                    label = if (state.isPlaying) "Pause" else "Play",
                    accent = true,
                    onClick = viewModel::togglePlayPause,
                )
                PlaybackTextButton(
                    label = "Next",
                    onClick = viewModel::next,
                )
            }
        }
    }
}

private fun handlePlaybackKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    viewModel: PlaybackViewModel,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    return when (event.nativeKeyEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
        AndroidKeyEvent.KEYCODE_ENTER,
        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        -> {
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

        AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
        AndroidKeyEvent.KEYCODE_MEDIA_NEXT,
        -> {
            viewModel.next()
            true
        }

        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
        AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS,
        -> {
            viewModel.previous()
            true
        }

        else -> false
    }
}

@Composable
private fun PlaybackTextButton(
    label: String,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .scale(if (focused) 1.02f else 1f)
                .clip(RoundedCornerShape(24.dp))
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
                    shape = RoundedCornerShape(24.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown &&
                        it.nativeKeyEvent.keyCode in
                        listOf(
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER,
                        )
                    ) {
                        onClick()
                        true
                    } else {
                        false
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = if (accent) 34.dp else 24.dp, vertical = 16.dp),
            color = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatTime(ms: Long): String {
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
