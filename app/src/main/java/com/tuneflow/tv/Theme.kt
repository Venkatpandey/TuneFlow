package com.tuneflow.tv

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuneflow.core.design.LocalTuneFlowMotion
import com.tuneflow.core.design.TuneFlowMotionProvider
import com.tuneflow.core.design.TuneFlowShapes

private val TuneFlowDarkScheme =
    darkColorScheme(
        primary = Color(0xFFD8BE91),
        onPrimary = Color(0xFF211B12),
        primaryContainer = Color(0xFF3B3224),
        onPrimaryContainer = Color(0xFFF0DBB8),
        inversePrimary = Color(0xFF705B36),
        secondary = Color(0xFFC5BFB3),
        onSecondary = Color(0xFF20201C),
        secondaryContainer = Color(0xFF34332D),
        onSecondaryContainer = Color(0xFFE7E2D7),
        tertiary = Color(0xFFAFBDAA),
        onTertiary = Color(0xFF172015),
        tertiaryContainer = Color(0xFF2D382A),
        onTertiaryContainer = Color(0xFFD8E6D2),
        background = Color(0xFF101211),
        onBackground = Color(0xFFF2F0EA),
        surface = Color(0xFF191C1A),
        onSurface = Color(0xFFF2F0EA),
        surfaceVariant = Color(0xFF303530),
        onSurfaceVariant = Color(0xFFB7BCB5),
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFFE6E8E1),
        inverseOnSurface = Color(0xFF292D29),
        surfaceDim = Color(0xFF101211),
        surfaceBright = Color(0xFF373C37),
        surfaceContainerLowest = Color(0xFF0C0E0D),
        surfaceContainerLow = Color(0xFF151815),
        surfaceContainer = Color(0xFF1D211D),
        surfaceContainerHigh = Color(0xFF272C27),
        surfaceContainerHighest = Color(0xFF323832),
        outline = Color(0xFF858C83),
        outlineVariant = Color(0xFF383E37),
        error = Color(0xFFF2AAA0),
        onError = Color(0xFF3B0907),
        errorContainer = Color(0xFF662723),
        onErrorContainer = Color(0xFFFFDAD4),
        scrim = Color.Black,
    )

private val TuneFlowTypography =
    Typography(
        displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
        titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
        titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
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
fun TuneFlowScaledContent(
    scaleFactor: Float,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaledDensity =
        Density(
            density = density.density * scaleFactor,
            fontScale = density.fontScale * scaleFactor,
        )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        content()
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    if (!LocalTuneFlowMotion.current.enabled) return background(MaterialTheme.colorScheme.surfaceVariant)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmerTranslate",
    )

    return this.background(
        brush =
            Brush.linearGradient(
                colors =
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface,
                    ),
                start = Offset(translateAnim - 500f, 0f),
                end = Offset(translateAnim, 0f),
            ),
    )
}

@Composable
fun TuneFlowTheme(content: @Composable () -> Unit) {
    val motionEnabled =
        remember {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
        }
    TuneFlowMotionProvider(enabled = motionEnabled) {
        MaterialTheme(
            colorScheme = TuneFlowDarkScheme,
            typography = TuneFlowTypography,
            shapes = TuneFlowShapes.material,
            content = content,
        )
    }
}
