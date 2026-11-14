package com.tuneflow.core.design

object CurrentlyPlayingIndicatorDefaults {
    const val DEFAULT_BAR_COUNT = 3
    const val SPACING_RATIO = 0.6f

    val BarDurations = listOf(450, 320, 550)
    val BarMinFractions = listOf(0.2f, 0.3f, 0.15f)
    val BarMaxFractions = listOf(0.85f, 1.0f, 0.7f)
    val RestingFractions = listOf(0.35f, 0.8f, 0.5f)

    fun calculateBarWidth(
        totalWidth: Float,
        barCount: Int,
        spacingRatio: Float = SPACING_RATIO,
    ): Float {
        if (barCount <= 0 || totalWidth <= 0f) return 0f
        val totalUnits = barCount + (barCount - 1) * spacingRatio
        return totalWidth / totalUnits
    }

    fun calculateSpacing(
        barWidth: Float,
        spacingRatio: Float = SPACING_RATIO,
    ): Float {
        return barWidth * spacingRatio
    }

    fun calculateBarHeight(
        totalHeight: Float,
        fraction: Float,
        minHeight: Float,
    ): Float {
        if (totalHeight <= 0f) return 0f
        return (totalHeight * fraction).coerceIn(minHeight, totalHeight)
    }
}
