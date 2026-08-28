package com.tuneflow.core.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object TuneFlowShapes {
    private val tightRectangle = RoundedCornerShape(8.dp)

    val surface = tightRectangle
    val container = tightRectangle
    val card = tightRectangle
    val button = tightRectangle
    val row = tightRectangle
    val field = tightRectangle
    val panel = tightRectangle
    val badge = tightRectangle
    val artwork = tightRectangle

    val avatar = CircleShape
    val iconButton = CircleShape
    val progressTrack = CircleShape

    val material =
        Shapes(
            extraSmall = badge,
            small = button,
            medium = card,
            large = container,
            extraLarge = surface,
        )
}
