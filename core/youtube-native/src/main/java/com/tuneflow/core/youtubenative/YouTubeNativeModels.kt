package com.tuneflow.core.youtubenative

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.StateFlow

data class YouTubeNativeSearchResult(
    val videoId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val viewCount: Long,
    val isLive: Boolean = false,
    val isShort: Boolean = false,
)

interface YouTubeNativeSearchClient {
    suspend fun search(
        artist: String,
        title: String,
        limit: Int = 25,
    ): List<YouTubeNativeSearchResult>
}

sealed interface YouTubeQuality {
    data object Auto : YouTubeQuality

    data object HighestSupported : YouTubeQuality

    data class Resolution(val height: Int) : YouTubeQuality
}

data class YouTubeVideoFormat(
    val id: String,
    val width: Int,
    val height: Int,
    val fps: Float,
    val bitrate: Int,
    val mimeType: String,
    val codec: String,
    val hardwareSupported: Boolean,
) {
    val displayLabel: String
        get() = "${height}p ${codec.uppercase()}"
}

data class YouTubeCaption(
    val id: String,
    val label: String,
    val language: String?,
)

enum class YouTubeSourceKind {
    Dash,
    Sabr,
    Hls,
    Direct,
}

enum class YouTubeNativeError {
    Unplayable,
    RegionRestricted,
    AgeRestricted,
    Resolver,
    Decoder,
    Network,
    Initialization,
}

sealed interface YouTubeNativePlayerState {
    data object Idle : YouTubeNativePlayerState

    data class Resolving(val videoId: String) : YouTubeNativePlayerState

    data class Preparing(
        val videoId: String,
        val source: YouTubeSourceKind,
    ) : YouTubeNativePlayerState

    data class Ready(
        val videoId: String,
        val durationMs: Long,
    ) : YouTubeNativePlayerState

    data class Playing(
        val videoId: String,
        val positionMs: Long,
        val durationMs: Long,
    ) : YouTubeNativePlayerState

    data class Paused(
        val videoId: String,
        val positionMs: Long,
        val durationMs: Long,
    ) : YouTubeNativePlayerState

    data class Buffering(
        val videoId: String,
        val positionMs: Long,
        val durationMs: Long,
    ) : YouTubeNativePlayerState

    data class Ended(
        val videoId: String,
        val positionMs: Long,
        val durationMs: Long,
    ) : YouTubeNativePlayerState

    data class Error(
        val videoId: String,
        val kind: YouTubeNativeError,
        val message: String,
    ) : YouTubeNativePlayerState
}

interface YouTubeNativePlayer {
    val state: StateFlow<YouTubeNativePlayerState>
    val availableVideoFormats: StateFlow<List<YouTubeVideoFormat>>
    val activeVideoFormat: StateFlow<YouTubeVideoFormat?>
    val selectedQuality: StateFlow<YouTubeQuality>
    val availableCaptions: StateFlow<List<YouTubeCaption>>
    val selectedCaptionId: StateFlow<String?>

    fun createView(context: Context): View

    fun prepare(videoId: String)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun selectQuality(quality: YouTubeQuality)

    fun selectCaption(captionId: String?)

    fun release()
}
