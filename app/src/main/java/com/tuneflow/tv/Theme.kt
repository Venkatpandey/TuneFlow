package com.tuneflow.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TuneFlowDarkScheme =
    darkColorScheme(
        primary = Color(0xFF6C63FF),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFFFF6584),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF43E97B),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFF0A0A0F),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF141420),
        onSurface = Color(0xFFF0F0FF),
        surfaceVariant = Color(0xFF1E1E2E),
        onSurfaceVariant = Color(0xFFA0A0C0),
        outline = Color(0x14FFFFFF),
        error = Color(0xFFFF6584),
    )

private val TuneFlowTypography =
    Typography(
        displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
        titleLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    )

@Composable
fun TuneFlowSafeArea(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp),
        content = content,
    )
}

@Composable
fun TuneFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TuneFlowDarkScheme,
        typography = TuneFlowTypography,
        content = content,
    )
}
