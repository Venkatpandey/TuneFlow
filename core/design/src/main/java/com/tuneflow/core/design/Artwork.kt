package com.tuneflow.core.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size
import kotlin.math.roundToInt

@Composable
fun rememberArtworkRequest(
    data: Any?,
    width: Dp,
    height: Dp = width,
): ImageRequest {
    val context = LocalContext.current
    val widthPx = remember(width) { width.roundToPxSize() }
    val heightPx = remember(height) { height.roundToPxSize() }

    return remember(data, context, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .size(Size(widthPx, heightPx))
            .build()
    }
}

@Composable
fun TuneFlowArtwork(
    model: Any?,
    contentDescription: String?,
    width: Dp,
    height: Dp = width,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    placeholderText: String? = null,
    fallbackPainterResId: Int? = null,
) {
    val request = rememberArtworkRequest(data = model, width = width, height = height)

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        loading = {
            ArtworkPlaceholder(
                placeholderText = placeholderText,
                fallbackPainterResId = fallbackPainterResId,
            )
        },
        error = {
            ArtworkPlaceholder(
                placeholderText = placeholderText,
                fallbackPainterResId = fallbackPainterResId,
            )
        },
    )
}

@Composable
fun BoxScope.ArtworkPlaceholder(
    placeholderText: String? = null,
    fallbackPainterResId: Int? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            fallbackPainterResId != null -> {
                Image(
                    painter = painterResource(id = fallbackPainterResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
            }

            !placeholderText.isNullOrBlank() -> {
                Text(
                    text = placeholderText.take(2).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }

            else -> {
                Text(
                    text = "TF",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

private fun Dp.roundToPxSize(): Int = (value * DENSITY_BASELINE).roundToInt().coerceAtLeast(1)

private const val DENSITY_BASELINE = 3f
