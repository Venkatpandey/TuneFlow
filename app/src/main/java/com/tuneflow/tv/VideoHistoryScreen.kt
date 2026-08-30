package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.feature.video.VIDEO_HISTORY_LIMIT
import com.tuneflow.feature.video.VideoHistoryEntry

@Composable
internal fun VideoHistoryScreen(
    viewModel: HomeViewModel,
    onPlayVideo: (VideoHistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history = state.videoHistory.take(VIDEO_HISTORY_LIMIT)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Recently played videos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your last ${VIDEO_HISTORY_LIMIT.coerceAtMost(history.size)} played videos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(history, key = VideoHistoryEntry::videoId) { entry ->
            VideoHistoryListCard(entry = entry, onClick = { onPlayVideo(entry) })
        }
    }
}

@Composable
private fun VideoHistoryListCard(
    entry: VideoHistoryEntry,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = TuneFlowShapes.card
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(if (focused) 1.01f else 1f)
                .clip(shape)
                .background(
                    if (focused) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
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
                    shape = shape,
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(256.dp)
                    .height(144.dp)
                    .clip(TuneFlowShapes.artwork)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            TuneFlowArtwork(
                model = entry.thumbnailUrl,
                contentDescription = entry.title,
                width = 256.dp,
                height = 144.dp,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholderText = entry.title,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.publisher,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
