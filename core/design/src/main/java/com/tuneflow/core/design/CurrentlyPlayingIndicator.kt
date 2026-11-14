package com.tuneflow.core.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Animated equalizer indicator displayed when a playlist, album, or track is actively playing.
 *
 * Implemented using native Compose canvas drawing to eliminate crashes on devices like Fire TV Stick
 * where animated GIFs in painterResource cause ClassCastException with AnimatedImageDrawable.
 */
@Composable
fun CurrentlyPlayingIndicator(
    modifier: Modifier = Modifier.size(20.dp),
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true,
    barCount: Int = CurrentlyPlayingIndicatorDefaults.DEFAULT_BAR_COUNT,
) {
    val motion = LocalTuneFlowMotion.current
    val transition = rememberInfiniteTransition(label = "CurrentlyPlayingIndicatorTransition")

    val fractions =
        List(barCount) { index ->
            val duration =
                CurrentlyPlayingIndicatorDefaults.BarDurations[
                    index % CurrentlyPlayingIndicatorDefaults.BarDurations.size,
                ]
            val minFraction =
                CurrentlyPlayingIndicatorDefaults.BarMinFractions[
                    index % CurrentlyPlayingIndicatorDefaults.BarMinFractions.size,
                ]
            val maxFraction =
                CurrentlyPlayingIndicatorDefaults.BarMaxFractions[
                    index % CurrentlyPlayingIndicatorDefaults.BarMaxFractions.size,
                ]

            val animatedFraction =
                transition.animateFloat(
                    initialValue = minFraction,
                    targetValue = maxFraction,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "CurrentlyPlayingBar_$index",
                )

            if (motion.enabled && isPlaying) {
                animatedFraction.value
            } else {
                CurrentlyPlayingIndicatorDefaults.RestingFractions[
                    index % CurrentlyPlayingIndicatorDefaults.RestingFractions.size,
                ]
            }
        }

    Canvas(
        modifier =
            modifier.semantics {
                contentDescription = if (isPlaying) "Currently playing" else "Playback paused"
            },
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        if (totalWidth <= 0f || totalHeight <= 0f || barCount <= 0) return@Canvas

        val barWidth = CurrentlyPlayingIndicatorDefaults.calculateBarWidth(totalWidth, barCount)
        val spacing = CurrentlyPlayingIndicatorDefaults.calculateSpacing(barWidth)
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        fractions.forEachIndexed { i, fraction ->
            val barHeight =
                CurrentlyPlayingIndicatorDefaults.calculateBarHeight(
                    totalHeight = totalHeight,
                    fraction = fraction,
                    minHeight = barWidth,
                )
            val left = i * (barWidth + spacing)
            val top = totalHeight - barHeight
            drawRoundRect(
                color = color,
                topLeft = Offset(x = left, y = top),
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = cornerRadius,
            )
        }
    }
}
