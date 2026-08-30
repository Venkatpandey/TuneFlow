package com.tuneflow.core.youtubenative

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import kotlin.math.roundToInt

internal class AspectFitTextureView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : TextureView(context, attrs) {
        private var videoAspectRatio = 0f

        fun setVideoAspectRatio(aspectRatio: Float) {
            if (aspectRatio > 0f && aspectRatio != videoAspectRatio) {
                videoAspectRatio = aspectRatio
                requestLayout()
            }
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
            val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
            val fitted = aspectFitSize(availableWidth, availableHeight, videoAspectRatio)
            setMeasuredDimension(fitted.first, fitted.second)
        }
    }

internal fun aspectFitSize(
    containerWidth: Int,
    containerHeight: Int,
    aspectRatio: Float,
): Pair<Int, Int> {
    if (containerWidth <= 0 || containerHeight <= 0 || aspectRatio <= 0f) return containerWidth to containerHeight
    val containerRatio = containerWidth.toFloat() / containerHeight
    return if (containerRatio > aspectRatio) {
        (containerHeight * aspectRatio).roundToInt() to containerHeight
    } else {
        containerWidth to (containerWidth / aspectRatio).roundToInt()
    }
}
