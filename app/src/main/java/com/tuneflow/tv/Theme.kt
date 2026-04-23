package com.tuneflow.tv

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val TuneFlowDarkScheme =
    darkColorScheme(
        primary = Color(0xFF34E2D3),
        onPrimary = Color(0xFF041513),
        secondary = Color(0xFF65B5FF),
        onSecondary = Color(0xFF05131E),
        tertiary = Color(0xFF7B6DFF),
        onTertiary = Color(0xFFF5F4FF),
        background = Color(0xFF08111B),
        onBackground = Color(0xFFF1F5FF),
        surface = Color(0xFF0F1B2A),
        onSurface = Color(0xFFF1F5FF),
        surfaceVariant = Color(0xFF152336),
        onSurfaceVariant = Color(0xFFB7C5DA),
        outline = Color(0xFF2F4761),
        error = Color(0xFFFF7D87),
    )

private val TuneFlowTypography =
    Typography(
        displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.SemiBold),
        displayMedium = TextStyle(fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold),
        displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        headlineLarge = TextStyle(fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = TextStyle(fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
        titleLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
        titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold),
    )

@Composable
fun TuneFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TuneFlowDarkScheme,
        typography = TuneFlowTypography,
        content = content,
    )
}
