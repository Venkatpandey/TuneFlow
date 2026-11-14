package com.tuneflow.tv

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuneflow.core.design.LocalTuneFlowMotion
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.feature.playback.LyricsRenderer
import com.tuneflow.feature.playback.LyricsUiState

@Composable
internal fun CinematicPlaybackScreensaverOverlay(
    playbackQueue: PlaybackQueue,
    playbackPositionMs: Long,
    lyricsState: LyricsUiState,
    currentTimeText: String,
) {
    val currentItem = playbackQueue.currentItem ?: return
    val lyrics = resolveScreensaverLyrics(lyricsState, currentItem.id)
    val motion = LocalTuneFlowMotion.current
    val offset = ambientBurnInOffset(playbackPositionMs)
    val offsetX by
        animateDpAsState(
            targetValue = offset.xDp.dp,
            animationSpec =
                if (motion.enabled) {
                    tween(1_200, easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
            label = "ambient-offset-x",
        )
    val offsetY by
        animateDpAsState(
            targetValue = offset.yDp.dp,
            animationSpec =
                if (motion.enabled) {
                    tween(1_200, easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
            label = "ambient-offset-y",
        )
    val durationMs = currentItem.durationMs.coerceAtLeast(0L)
    val progress =
        if (durationMs > 0L) {
            (playbackPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        TuneFlowArtwork(
            model = currentItem.artUrl,
            contentDescription = null,
            width = 1280.dp,
            height = 720.dp,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.68f,
            placeholderText = currentItem.title,
            fallbackPainterResId = R.drawable.ic_tuneflow_brand,
            requestSizePx = AMBIENT_BACKGROUND_REQUEST_PX,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.90f),
                                    Color.Black.copy(alpha = 0.62f),
                                    Color.Black.copy(alpha = 0.82f),
                                ),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.70f)),
                        ),
                    ),
        )

        Text(
            text = currentTimeText,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 56.dp),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset(x = offsetX, y = offsetY)
                    .padding(horizontal = 64.dp, vertical = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                TuneFlowArtwork(
                    model = currentItem.artUrl,
                    contentDescription = currentItem.title,
                    width = 286.dp,
                    height = 286.dp,
                    modifier = Modifier.size(286.dp).clip(TuneFlowShapes.artwork),
                    contentScale = ContentScale.Crop,
                    placeholderText = currentItem.title,
                    fallbackPainterResId = R.drawable.ic_tuneflow_brand,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = currentItem.title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentItem.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                currentItem.album.takeIf(String::isNotBlank)?.let { album ->
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(TuneFlowShapes.progressTrack),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = ambientFormatTime(playbackPositionMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ambientFormatTime(durationMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (lyrics != null) {
                Column(
                    modifier =
                        Modifier
                            .width(430.dp)
                            .fillMaxHeight(0.84f)
                            .clip(TuneFlowShapes.panel)
                            .background(Color.Black.copy(alpha = 0.30f))
                            .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LyricsRenderer(
                        lyrics = lyrics,
                        positionMs = playbackPositionMs,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        autoFollow = true,
                        interactive = false,
                    )
                }
            }
        }
    }
}

internal data class AmbientBurnInOffset(
    val xDp: Int,
    val yDp: Int,
)

internal fun ambientBurnInOffset(positionMs: Long): AmbientBurnInOffset =
    when ((positionMs.coerceAtLeast(0L) / AMBIENT_SHIFT_INTERVAL_MS) % 4L) {
        0L -> AmbientBurnInOffset(-8, -4)
        1L -> AmbientBurnInOffset(8, -4)
        2L -> AmbientBurnInOffset(8, 4)
        else -> AmbientBurnInOffset(-8, 4)
    }

private fun ambientFormatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private const val AMBIENT_BACKGROUND_REQUEST_PX = 96
private const val AMBIENT_SHIFT_INTERVAL_MS = 60_000L
