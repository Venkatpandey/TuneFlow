package com.tuneflow.feature.video

import com.tuneflow.core.player.QueueItem
import kotlinx.coroutines.flow.StateFlow

enum class VideoProviderId {
    YouTube,
}

data class VideoProviderCapabilities(
    val supportsSeeking: Boolean,
    val usesAdaptiveQuality: Boolean,
)

data class VideoTrackQuery(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val regionCode: String?,
    val languageCode: String?,
)

data class VideoCandidate(
    val providerId: VideoProviderId,
    val videoId: String,
    val title: String,
    val publisher: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val musicCategory: Boolean,
    val viewCount: Long = 0L,
    val score: Double = 0.0,
)

data class EmbeddedVideoPlayerSpec(
    val providerId: VideoProviderId,
    val videoId: String,
)

interface VideoProvider {
    val id: VideoProviderId
    val capabilities: VideoProviderCapabilities
    val configured: Boolean

    suspend fun search(query: VideoTrackQuery): List<VideoCandidate>

    fun createPlayerSpec(candidate: VideoCandidate): EmbeddedVideoPlayerSpec
}

data class VideoSessionKey(
    val trackId: String,
    val generation: Long,
)

sealed interface EmbeddedVideoPlayerState {
    data object Idle : EmbeddedVideoPlayerState

    data class Loading(val session: VideoSessionKey) : EmbeddedVideoPlayerState

    data class Ready(
        val session: VideoSessionKey,
        val durationMs: Long,
    ) : EmbeddedVideoPlayerState

    data class Playing(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : EmbeddedVideoPlayerState

    data class Paused(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : EmbeddedVideoPlayerState

    data class Buffering(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : EmbeddedVideoPlayerState

    data class Ended(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : EmbeddedVideoPlayerState

    data class Error(
        val session: VideoSessionKey,
        val message: String,
    ) : EmbeddedVideoPlayerState
}

interface EmbeddedVideoPlayer {
    val state: StateFlow<EmbeddedVideoPlayerState>

    fun prepare(
        session: VideoSessionKey,
        spec: EmbeddedVideoPlayerSpec,
    )

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun release()
}

enum class VideoPresentationMode {
    Mini,
    Fullscreen,
}

sealed interface VideoUiState {
    val trackId: String?

    data class Unavailable(val message: String) : VideoUiState {
        override val trackId: String? = null
    }

    data object Idle : VideoUiState {
        override val trackId: String? = null
    }

    data class ConsentRequired(
        override val trackId: String,
        val generation: Long,
    ) : VideoUiState

    data class Searching(
        override val trackId: String,
        val generation: Long,
    ) : VideoUiState

    data class Candidates(
        override val trackId: String,
        val generation: Long,
        val candidates: List<VideoCandidate>,
    ) : VideoUiState

    data class Loading(
        override val trackId: String,
        val generation: Long,
        val candidate: VideoCandidate,
        val presentation: VideoPresentationMode,
        val focusRequestId: Long = 0L,
    ) : VideoUiState

    data class Playing(
        override val trackId: String,
        val generation: Long,
        val candidate: VideoCandidate,
        val presentation: VideoPresentationMode,
        val positionMs: Long,
        val durationMs: Long,
        val isPlaying: Boolean,
        val focusRequestId: Long = 0L,
    ) : VideoUiState

    data class Error(
        override val trackId: String,
        val generation: Long,
        val message: String,
    ) : VideoUiState
}

val VideoUiState.hasVisiblePlayer: Boolean
    get() = this is VideoUiState.Loading || this is VideoUiState.Playing

val VideoUiState.isFullscreen: Boolean
    get() =
        when (this) {
            is VideoUiState.Loading -> presentation == VideoPresentationMode.Fullscreen
            is VideoUiState.Playing -> presentation == VideoPresentationMode.Fullscreen
            else -> false
        }

val VideoUiState.isVideoSessionActive: Boolean
    get() = this is VideoUiState.Loading || this is VideoUiState.Playing

fun QueueItem.toVideoTrackQuery(
    regionCode: String?,
    languageCode: String?,
): VideoTrackQuery =
    VideoTrackQuery(
        trackId = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        regionCode = regionCode,
        languageCode = languageCode,
    )
