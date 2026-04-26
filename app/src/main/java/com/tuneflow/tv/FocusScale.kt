package com.tuneflow.tv

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable

@Composable
internal fun animateTvFocusScale(focused: Boolean) =
    animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec =
            tween(
                durationMillis = if (focused) 150 else 100,
                easing = if (focused) FastOutSlowInEasing else LinearOutSlowInEasing,
            ),
        label = "focusScale",
    )
