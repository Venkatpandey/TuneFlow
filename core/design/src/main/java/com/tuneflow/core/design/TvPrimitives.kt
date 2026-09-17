package com.tuneflow.core.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TuneFlowFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = TuneFlowShapes.card,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onFocusedChange: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    TuneFlowFocusableSurface(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        shape = shape,
        focusedScale = LocalTuneFlowMotion.current.cardFocusScale,
        restingColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        contentPadding = contentPadding,
        onFocusedChange = onFocusedChange,
        content = content,
    )
}

@Composable
fun TuneFlowActionSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = TuneFlowShapes.button,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    contentAlignment: Alignment = Alignment.Center,
    onFocusedChange: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    TuneFlowFocusableSurface(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        shape = shape,
        focusedScale = LocalTuneFlowMotion.current.buttonFocusScale,
        restingColor =
            if (accent) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f)
            },
        focusedColor =
            if (accent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            },
        contentPadding = contentPadding,
        contentAlignment = contentAlignment,
        onFocusedChange = onFocusedChange,
        content = content,
    )
}

@Composable
private fun TuneFlowFocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    selected: Boolean,
    enabled: Boolean,
    shape: Shape,
    focusedScale: Float,
    restingColor: Color,
    focusedColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    contentPadding: PaddingValues,
    contentAlignment: Alignment = Alignment.TopStart,
    onFocusedChange: (Boolean) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val motion = LocalTuneFlowMotion.current
    val scale = animateTuneFlowFocusScale(focused, focusedScale, "tv-focus-scale")
    val backgroundColor by
        animateColorAsState(
            targetValue =
                when {
                    focused -> focusedColor
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else -> restingColor
                },
            animationSpec =
                if (motion.enabled) {
                    tween(durationMillis = motion.focusGainDurationMs, easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
            label = "tv-focus-background",
        )
    val borderColor by
        animateColorAsState(
            targetValue =
                when {
                    focused -> MaterialTheme.colorScheme.primary
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                },
            animationSpec =
                if (motion.enabled) {
                    tween(durationMillis = motion.focusGainDurationMs, easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
            label = "tv-focus-border-color",
        )
    val borderWidth by
        animateDpAsState(
            targetValue = if (focused) 2.dp else 1.dp,
            animationSpec =
                if (motion.enabled) {
                    tween(durationMillis = motion.focusGainDurationMs, easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
            label = "tv-focus-border-width",
        )

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .alpha(if (enabled) 1f else 0.48f)
                .clip(shape)
                .background(backgroundColor)
                .border(borderWidth, borderColor, shape)
                .onFocusChanged {
                    focused = it.hasFocus
                    onFocusedChange(it.hasFocus)
                }
                .focusable(enabled)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun TuneFlowTrackRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    height: Dp = 72.dp,
    onFocusedChange: (Boolean) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val motion = LocalTuneFlowMotion.current
    val backgroundColor by
        animateColorAsState(
            targetValue =
                when {
                    focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else -> Color.Transparent
                },
            animationSpec =
                if (motion.enabled) {
                    tween(durationMillis = motion.focusGainDurationMs, easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
            label = "track-row-background",
        )
    val indicatorWidth by
        animateDpAsState(
            targetValue =
                when {
                    focused -> 3.dp
                    selected -> 2.dp
                    else -> 0.dp
                },
            animationSpec =
                if (motion.enabled) {
                    tween(durationMillis = motion.focusGainDurationMs, easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
            label = "track-row-indicator",
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(TuneFlowShapes.row)
                .background(backgroundColor)
                .onFocusChanged {
                    focused = it.hasFocus
                    onFocusedChange(it.hasFocus)
                }
                .focusable(enabled)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        if (indicatorWidth > 0.dp) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(indicatorWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            )
        }
    }
}
