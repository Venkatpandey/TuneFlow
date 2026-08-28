package com.tuneflow.core.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TuneFlowShapesTest {
    @Test
    fun rectangularTokensUseApprovedTightRadius() {
        val expected = RoundedCornerShape(8.dp)

        listOf(
            TuneFlowShapes.surface,
            TuneFlowShapes.container,
            TuneFlowShapes.card,
            TuneFlowShapes.button,
            TuneFlowShapes.row,
            TuneFlowShapes.field,
            TuneFlowShapes.panel,
            TuneFlowShapes.badge,
            TuneFlowShapes.artwork,
        ).forEach { shape -> assertEquals(expected, shape) }
    }

    @Test
    fun documentedExceptionsRemainCircular() {
        assertEquals(CircleShape, TuneFlowShapes.avatar)
        assertEquals(CircleShape, TuneFlowShapes.iconButton)
        assertEquals(CircleShape, TuneFlowShapes.progressTrack)
    }
}
