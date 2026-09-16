package com.tuneflow.feature.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.tuneflow.core.design.TuneFlowFocusableCard
import com.tuneflow.core.design.TuneFlowShapes

@Composable
internal fun FocusScaleCard(
    modifier: Modifier = Modifier,
    shape: Shape = TuneFlowShapes.card,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    TuneFlowFocusableCard(
        modifier = modifier,
        shape = shape,
        onClick = onClick,
    ) {
        content()
    }
}
