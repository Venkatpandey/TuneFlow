package com.tuneflow.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class TuneFlowThemeId(
    val rawId: String,
    val displayName: String,
) {
    Default(rawId = "default", displayName = "Default"),
    PlainDark(rawId = "plain_dark", displayName = "Plain Dark"),
    BananaBob(rawId = "bananabob", displayName = "bananabob"),
    Matrix(rawId = "matrix", displayName = "Matrix"),
    ;

    companion object {
        fun fromId(id: String?): TuneFlowThemeId {
            if (id.isNullOrBlank()) return Default
            return entries.firstOrNull {
                it.rawId.equals(id, ignoreCase = true) ||
                    it.name.equals(id, ignoreCase = true) ||
                    it.displayName.equals(id, ignoreCase = true)
            } ?: Default
        }
    }
}

enum class ArtworkTreatment {
    Standard,
    HighContrastBorder,
}

data class ThemePreviewColors(
    val background: Color,
    val surface: Color,
    val highlight: Color,
    val text: Color,
)

data class TuneFlowThemeDefinition(
    val id: TuneFlowThemeId,
    val displayName: String,
    val description: String,
    val colorScheme: ColorScheme,
    val previewColors: ThemePreviewColors,
    val artworkTreatment: ArtworkTreatment = ArtworkTreatment.Standard,
)

val TuneFlowDarkScheme: ColorScheme =
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

val PlainDarkScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF282828),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF333333),
        secondary = Color(0xFFE0E0E0),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF202020),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFCCCCCC),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF1C1C1C),
        onTertiaryContainer = Color(0xFFEEEEEE),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF121212),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF242424),
        onSurfaceVariant = Color(0xFFCCCCCC),
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceBright = Color(0xFF303030),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF141414),
        surfaceContainerHigh = Color(0xFF1E1E1E),
        surfaceContainerHighest = Color(0xFF282828),
        outline = Color(0xFF888888),
        outlineVariant = Color(0xFF404040),
        error = Color(0xFFF2AAA0),
        onError = Color(0xFF3B0907),
        errorContainer = Color(0xFF662723),
        onErrorContainer = Color(0xFFFFDAD4),
        scrim = Color.Black,
    )

val BananaBobScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF333333),
        primaryContainer = Color(0xFF333333),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF990000),
        secondary = Color(0xFF333333),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF424242),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFE0E0E0),
        onTertiary = Color(0xFF333333),
        tertiaryContainer = Color(0xFF333333),
        onTertiaryContainer = Color(0xFFFFFFFF),
        background = Color(0xFF990000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF800000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF333333),
        onSurfaceVariant = Color(0xFFE8E8E8),
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFF333333),
        surfaceDim = Color(0xFF6E0000),
        surfaceBright = Color(0xFFA61111),
        surfaceContainerLowest = Color(0xFF590000),
        surfaceContainerLow = Color(0xFF700000),
        surfaceContainer = Color(0xFF800000),
        surfaceContainerHigh = Color(0xFF940000),
        surfaceContainerHighest = Color(0xFFA61111),
        outline = Color(0xFFCCCCCC),
        outlineVariant = Color(0xFF333333),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color.Black,
    )

val MatrixScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF00FF66),
        onPrimary = Color(0xFF00220A),
        primaryContainer = Color(0xFF003814),
        onPrimaryContainer = Color(0xFF66FFA3),
        inversePrimary = Color(0xFF00B344),
        secondary = Color(0xFF33FF77),
        onSecondary = Color(0xFF00220A),
        secondaryContainer = Color(0xFF0D2E14),
        onSecondaryContainer = Color(0xFF80FFAC),
        tertiary = Color(0xFF00E55B),
        onTertiary = Color(0xFF00220A),
        tertiaryContainer = Color(0xFF004D1E),
        onTertiaryContainer = Color(0xFF85FFB2),
        background = Color(0xFF000000),
        onBackground = Color(0xFF00FF66),
        surface = Color(0xFF0B140D),
        onSurface = Color(0xFF00FF66),
        surfaceVariant = Color(0xFF122415),
        onSurfaceVariant = Color(0xFF80EE9E),
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFF00FF66),
        inverseOnSurface = Color(0xFF00220A),
        surfaceDim = Color(0xFF050B06),
        surfaceBright = Color(0xFF1A331E),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF060F07),
        surfaceContainer = Color(0xFF0B170D),
        surfaceContainerHigh = Color(0xFF102012),
        surfaceContainerHighest = Color(0xFF162B1A),
        outline = Color(0xFF00993D),
        outlineVariant = Color(0xFF0D3316),
        error = Color(0xFFFF5555),
        onError = Color(0xFF3B0000),
        errorContainer = Color(0xFF590000),
        onErrorContainer = Color(0xFFFFB3B3),
        scrim = Color.Black,
    )

object TuneFlowThemes {
    val Default =
        TuneFlowThemeDefinition(
            id = TuneFlowThemeId.Default,
            displayName = "Default",
            description = "Champagne accents with deep charcoal surfaces",
            colorScheme = TuneFlowDarkScheme,
            previewColors =
                ThemePreviewColors(
                    background = Color(0xFF101211),
                    surface = Color(0xFF191C1A),
                    highlight = Color(0xFFD8BE91),
                    text = Color(0xFFF2F0EA),
                ),
            artworkTreatment = ArtworkTreatment.Standard,
        )

    val PlainDark =
        TuneFlowThemeDefinition(
            id = TuneFlowThemeId.PlainDark,
            displayName = "Plain Dark",
            description = "High-contrast pure black surfaces with white text",
            colorScheme = PlainDarkScheme,
            previewColors =
                ThemePreviewColors(
                    background = Color(0xFF000000),
                    surface = Color(0xFF121212),
                    highlight = Color(0xFFFFFFFF),
                    text = Color(0xFFFFFFFF),
                ),
            artworkTreatment = ArtworkTreatment.HighContrastBorder,
        )

    val BananaBob =
        TuneFlowThemeDefinition(
            id = TuneFlowThemeId.BananaBob,
            displayName = "bananabob",
            description = "Crimson red background with white text and charcoal highlights",
            colorScheme = BananaBobScheme,
            previewColors =
                ThemePreviewColors(
                    background = Color(0xFF990000),
                    surface = Color(0xFF800000),
                    highlight = Color(0xFF333333),
                    text = Color(0xFFFFFFFF),
                ),
            artworkTreatment = ArtworkTreatment.Standard,
        )

    val Matrix =
        TuneFlowThemeDefinition(
            id = TuneFlowThemeId.Matrix,
            displayName = "Matrix",
            description = "Neon green typography on deep cyber black surfaces",
            colorScheme = MatrixScheme,
            previewColors =
                ThemePreviewColors(
                    background = Color(0xFF000000),
                    surface = Color(0xFF0B140D),
                    highlight = Color(0xFF00FF66),
                    text = Color(0xFF00FF66),
                ),
            artworkTreatment = ArtworkTreatment.HighContrastBorder,
        )

    val all: List<TuneFlowThemeDefinition> = listOf(Default, PlainDark, BananaBob, Matrix)

    fun fromId(id: String?): TuneFlowThemeDefinition {
        val resolvedId = TuneFlowThemeId.fromId(id)
        return all.firstOrNull { it.id == resolvedId } ?: Default
    }
}

fun calculateContrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val l1 = calculateRelativeLuminance(foreground)
    val l2 = calculateRelativeLuminance(background)
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun calculateRelativeLuminance(color: Color): Double {
    fun toLinear(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    val r = toLinear(color.red)
    val g = toLinear(color.green)
    val b = toLinear(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
