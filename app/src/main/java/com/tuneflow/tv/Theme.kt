package com.tuneflow.tv

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TuneFlowDarkScheme =
    darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF4DD0E1),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF001416),
        background = androidx.compose.ui.graphics.Color(0xFF0A0D12),
        onBackground = androidx.compose.ui.graphics.Color(0xFFEAF2FF),
        surface = androidx.compose.ui.graphics.Color(0xFF121821),
        onSurface = androidx.compose.ui.graphics.Color(0xFFEAF2FF),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1D2633),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB8C4D9),
        error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    )

@Composable
fun TuneFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TuneFlowDarkScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
