package com.tuneflow.feature.playback

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import coil.compose.AsyncImage

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

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp)
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (it.key) {
                        Key.DirectionCenter -> {
                            viewModel.togglePlayPause()
                            true
                        }

                        Key.DirectionRight -> {
                            viewModel.next()
                            true
                        }

                        Key.DirectionLeft -> {
                            viewModel.previous()
                            true
                        }

                        else -> false
                    }
                },
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item?.artUrl != null) {
                AsyncImage(
                    model = item.artUrl,
                    contentDescription = item.title,
                    modifier = Modifier.size(320.dp),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(320.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Spacer(Modifier.size(24.dp))
            Column {
                Text(
                    text = item?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = item?.artist ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
                Spacer(Modifier.height(8.dp))
                Text("${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}")
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = viewModel::previous) { Text("Prev") }
            Button(onClick = viewModel::togglePlayPause) { Text(if (state.isPlaying) "Pause" else "Play") }
            Button(onClick = viewModel::next) { Text("Next") }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)
}
