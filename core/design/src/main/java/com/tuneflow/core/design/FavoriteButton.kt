package com.tuneflow.core.design

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val enabled = !isPending

    TuneFlowActionSurface(
        onClick = onClick,
        selected = isFavorite,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(48.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
                    stateDescription =
                        when {
                            isPending -> "Updating"
                            isFavorite -> "Favorited"
                            else -> "Not favorited"
                        }
                },
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
