package com.tuneflow.feature.video

import android.content.Context
import android.view.View
import com.tuneflow.core.player.QueueItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    val videoId: String,
    val title: String,
    val publisher: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val musicCategory: Boolean,
    val viewCount: Long = 0L,
    val score: Double = 0.0,
)

sealed interface PreferredVideoState {
    val trackId: String?

    data object BackendUnavailable : PreferredVideoState {
        override val trackId: String? = null
    }

    data class Checking(override val trackId: String) : PreferredVideoState

    data class Mapped(
        override val trackId: String,
        val candidate: VideoCandidate,
    ) : PreferredVideoState

    data class Unmapped(override val trackId: String) : PreferredVideoState
}

data class NativeVideoSpec(
    val videoId: String,
)

data class VideoSessionKey(
    val trackId: String,
    val generation: Long,
)

sealed interface NativeVideoPlayerState {
    data object Idle : NativeVideoPlayerState

    data class Loading(val session: VideoSessionKey) : NativeVideoPlayerState

    data class Ready(
        val session: VideoSessionKey,
        val durationMs: Long,
    ) : NativeVideoPlayerState

    data class Playing(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : NativeVideoPlayerState

    data class Paused(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : NativeVideoPlayerState

    data class Buffering(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : NativeVideoPlayerState

    data class Ended(
        val session: VideoSessionKey,
        val positionMs: Long,
        val durationMs: Long,
    ) : NativeVideoPlayerState

    data class Error(
        val session: VideoSessionKey,
        val message: String,
    ) : NativeVideoPlayerState
}

interface NativeVideoPlayer {
    val state: StateFlow<NativeVideoPlayerState>
    val nativeControls: StateFlow<NativeVideoControlState>
        get() = EMPTY_NATIVE_CONTROLS

    fun prepare(
        session: VideoSessionKey,
        spec: NativeVideoSpec,
    )

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun release()

    fun createSurfaceView(context: Context): View

    fun disposeSurfaceView(view: View)

    fun onSurfaceBoundsChanged(view: View) = Unit

    fun focusPlayer()

    fun clearPlayerFocus()

    fun adjustVolume(delta: Int)

    fun selectQuality(id: String) = Unit

    fun selectCaption(id: String?) = Unit
}

data class VideoQualityOption(
    val id: String,
    val label: String,
)

data class VideoCaptionOption(
    val id: String,
    val label: String,
)

data class NativeVideoControlState(
    val available: Boolean = false,
    val qualities: List<VideoQualityOption> = emptyList(),
    val selectedQualityId: String = "highest",
    val captions: List<VideoCaptionOption> = emptyList(),
    val selectedCaptionId: String? = null,
    val activeFormatLabel: String? = null,
)

interface NativeVideoBackend {
    val player: NativeVideoPlayer

    suspend fun search(query: VideoTrackQuery): List<VideoCandidate>
}

private val EMPTY_NATIVE_CONTROLS = MutableStateFlow(NativeVideoControlState())

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
