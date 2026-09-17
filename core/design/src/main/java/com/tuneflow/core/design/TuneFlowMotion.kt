package com.tuneflow.core.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

data class TuneFlowMotion(
    val enabled: Boolean = true,
    val focusGainDurationMs: Int = 150,
    val focusLossDurationMs: Int = 100,
    val screenForwardDurationMs: Int = 250,
    val screenBackDurationMs: Int = 200,
    val nowPlayingOpenDurationMs: Int = 300,
    val nowPlayingCloseDurationMs: Int = 250,
    val dialogDurationMs: Int = 160,
    val cardFocusScale: Float = 1f,
    val buttonFocusScale: Float = 1.05f,
    val fieldFocusScale: Float = 1.01f,
)

val LocalTuneFlowMotion = staticCompositionLocalOf { TuneFlowMotion() }

@Composable
fun TuneFlowMotionProvider(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTuneFlowMotion provides TuneFlowMotion(enabled = enabled),
        content = content,
    )
}

@Composable
fun animateTuneFlowFocusScale(
    focused: Boolean,
    focusedScale: Float,
    label: String,
): Float {
    val motion = LocalTuneFlowMotion.current
    val value by
        animateFloatAsState(
            targetValue = if (focused && motion.enabled) focusedScale else 1f,
            animationSpec =
                if (motion.enabled) {
                    tween(
                        durationMillis = if (focused) motion.focusGainDurationMs else motion.focusLossDurationMs,
                        easing = if (focused) FastOutSlowInEasing else LinearOutSlowInEasing,
                    )
                } else {
                    snap()
                },
            label = label,
        )
    return value
}
