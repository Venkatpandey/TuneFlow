package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuneflow.core.design.InitialFocusEffect
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowFocusableCard
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
    val firstVideoFocusRequester = remember { FocusRequester() }

    InitialFocusEffect(
        focusRequester = firstVideoFocusRequester,
        targetAvailable = history.isNotEmpty(),
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 260.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(history, key = { _, entry -> entry.videoHistoryItemKey() }) { index, entry ->
                VideoHistoryTile(
                    entry = entry,
                    onClick = { onPlayVideo(entry) },
                    modifier =
                        Modifier.then(
                            if (index == 0) {
                                Modifier.focusRequester(firstVideoFocusRequester)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

internal fun VideoHistoryEntry.videoHistoryItemKey(): String = "track:$trackId"

@Composable
private fun VideoHistoryTile(
    entry: VideoHistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TuneFlowFocusableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                TuneFlowArtwork(
                    model = entry.thumbnailUrl,
                    contentDescription = entry.title,
                    width = 320.dp,
                    height = 180.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = entry.title,
                )
            }
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
