package com.tuneflow.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TrackFavoriteButton(
    isFavorite: Boolean,
    isPending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val enabled = !isPending

    Box(
        modifier =
            modifier
                .size(48.dp)
                .scale(if (focused) 1.06f else 1f)
                .alpha(if (enabled) 1f else 0.52f)
                .clip(TuneFlowShapes.button)
                .background(
                    when {
                        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                        isFavorite -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                    },
                )
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                        },
                    shape = TuneFlowShapes.button,
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .semantics {
                    role = Role.Button
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
                    stateDescription =
                        when {
                            isPending -> "Updating"
                            isFavorite -> "Favorited"
                            else -> "Not favorited"
                        }
                }
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFavorite) "♥" else "♡",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color =
                if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}
