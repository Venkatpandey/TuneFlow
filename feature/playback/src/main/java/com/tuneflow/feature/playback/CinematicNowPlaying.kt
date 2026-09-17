package com.tuneflow.feature.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tuneflow.core.design.LocalTuneFlowMotion
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.player.QueueItem

@Composable
internal fun NowPlayingArtworkBackground(
    item: QueueItem?,
    cinematic: Boolean,
    modifier: Modifier = Modifier,
) {
    val motion = LocalTuneFlowMotion.current
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = item,
            transitionSpec = {
                if (motion.enabled) {
                    fadeIn(tween(CINEMATIC_ART_CROSSFADE_MS, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(CINEMATIC_ART_CROSSFADE_MS / 2))
                } else {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                }
            },
            contentKey = { it?.id },
            label = "now-playing-background-art",
        ) { targetItem ->
            TuneFlowArtwork(
                model = targetItem?.artUrl,
                contentDescription = null,
                width = 1280.dp,
                height = 720.dp,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (cinematic) 0.70f else 0.18f,
                placeholderText = targetItem?.title,
                requestSizePx = if (cinematic) CINEMATIC_BACKGROUND_REQUEST_PX else null,
            )
        }

        if (cinematic) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        Color.Transparent,
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
                                colors =
                                    listOf(
                                        Color.Black.copy(alpha = 0.42f),
                                        Color.Black.copy(alpha = 0.68f),
                                        Color.Black.copy(alpha = 0.84f),
                                    ),
                            ),
                        ),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.54f)),
            )
        }
    }
}

private const val CINEMATIC_BACKGROUND_REQUEST_PX = 96
private const val CINEMATIC_ART_CROSSFADE_MS = 550
