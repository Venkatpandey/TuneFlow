package com.tuneflow.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tuneflow.core.design.LocalTuneFlowMotion
import com.tuneflow.core.design.TuneFlowShapes

@Composable
internal fun FavoriteFeedbackBanner(
    message: String?,
    modifier: Modifier = Modifier,
) {
    val motion = LocalTuneFlowMotion.current
    val enterTransition =
        if (motion.enabled) {
            fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
        } else {
            fadeIn(snap())
        }
    val exitTransition =
        if (motion.enabled) {
            fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        } else {
            fadeOut(snap())
        }

    AnimatedVisibility(
        visible = message != null,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(
            modifier =
                Modifier
                    .clip(TuneFlowShapes.panel)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                        shape = TuneFlowShapes.panel,
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
