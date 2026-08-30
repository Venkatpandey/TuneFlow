package com.tuneflow.core.youtubenative

internal data class YouTubePlaybackCapabilities(
    val displayWidth: Int,
    val displayHeight: Int,
    val hardwareCodecs: Set<String>,
    val apiLevel: Int,
)

internal object YouTubeQualitySelector {
    fun highestSupported(
        formats: List<YouTubeVideoFormat>,
        capabilities: YouTubePlaybackCapabilities,
    ): YouTubeVideoFormat? =
        formats
            .asSequence()
            .filter { it.width <= capabilities.displayWidth && it.height <= capabilities.displayHeight }
            .filter { format ->
                format.hardwareSupported &&
                    format.codec in capabilities.hardwareCodecs &&
                    !(capabilities.apiLevel <= 25 && format.codec == CODEC_AV1)
            }
            .maxWithOrNull(
                compareBy<YouTubeVideoFormat> { it.height }
                    .thenBy { it.width }
                    .thenBy { codecPreference(it.codec) }
                    .thenBy { it.fps }
                    .thenBy { it.bitrate },
            )
            ?: formats
                .filter { it.codec == CODEC_AVC && it.hardwareSupported }
                .maxWithOrNull(compareBy<YouTubeVideoFormat> { it.height }.thenBy { it.bitrate })

    private fun codecPreference(codec: String): Int =
        when (codec) {
            CODEC_AV1 -> 3
            CODEC_VP9 -> 2
            CODEC_AVC -> 1
            else -> 0
        }
}

internal const val CODEC_AV1 = "av1"
internal const val CODEC_VP9 = "vp9"
internal const val CODEC_AVC = "avc"
