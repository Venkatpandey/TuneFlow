package com.tuneflow.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

@Composable
internal fun FocusScaleCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .scale(if (focused) 1.04f else 1f)
                .clip(RoundedCornerShape(24.dp))
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
                    shape = RoundedCornerShape(24.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(16.dp),
    ) {
        content()
    }
}
