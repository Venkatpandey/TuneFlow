package com.tuneflow.tv

import android.content.Context
import android.view.View
import com.tuneflow.core.youtubenative.SmartTubeYouTubeNativePlayer
import com.tuneflow.core.youtubenative.SmartTubeYouTubeNativeSearchClient
import com.tuneflow.core.youtubenative.YouTubeNativePlayerState
import com.tuneflow.core.youtubenative.YouTubeQuality
import com.tuneflow.feature.video.EmbeddedVideoPlayerSpec
import com.tuneflow.feature.video.EmbeddedVideoPlayerState
import com.tuneflow.feature.video.ExperimentalNativeVideoBackend
import com.tuneflow.feature.video.NativeVideoControlState
import com.tuneflow.feature.video.VideoCandidate
import com.tuneflow.feature.video.VideoCaptionOption
import com.tuneflow.feature.video.VideoProviderId
import com.tuneflow.feature.video.VideoQualityOption
import com.tuneflow.feature.video.VideoSessionKey
import com.tuneflow.feature.video.VideoSurfacePlayer
import com.tuneflow.feature.video.VideoTrackQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

internal fun createExperimentalNativeVideoBackend(context: Context): ExperimentalNativeVideoBackend? =
    runCatching { SmartTubeNativeVideoBackend(context.applicationContext) }.getOrNull()

private class SmartTubeNativeVideoBackend(context: Context) : ExperimentalNativeVideoBackend {
    private val searchClient = SmartTubeYouTubeNativeSearchClient(context)
    override val player: VideoSurfacePlayer = SmartTubeNativeSurfacePlayer(context)

    override suspend fun search(query: VideoTrackQuery): List<VideoCandidate> =
        searchClient.search(query.artist, query.title).map { result ->
            VideoCandidate(
                providerId = VideoProviderId.YouTube,
                videoId = result.videoId,
                title = result.title,
                publisher = result.channel,
                thumbnailUrl = result.thumbnailUrl,
                durationMs = result.durationMs,
                musicCategory =
                    listOf(result.title, result.channel).joinToString(" ").lowercase(Locale.ROOT)
                        .let { "official" in it || "music" in it || "vevo" in it },
                viewCount = result.viewCount,
            )
        }
}

private class SmartTubeNativeSurfacePlayer(context: Context) : VideoSurfacePlayer {
    override val isNative: Boolean = true
    private val delegate = SmartTubeYouTubeNativePlayer(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<EmbeddedVideoPlayerState>(EmbeddedVideoPlayerState.Idle)
    override val state: StateFlow<EmbeddedVideoPlayerState> = _state.asStateFlow()
    private val _nativeControls = MutableStateFlow(NativeVideoControlState(available = true))
    override val nativeControls: StateFlow<NativeVideoControlState> = _nativeControls.asStateFlow()
    private var session: VideoSessionKey? = null

    init {
        scope.launch {
            delegate.state.collect { nativeState ->
                val activeSession = session
                _state.value = nativeState.toEmbeddedState(activeSession)
            }
        }
        scope.launch {
            combine(
                delegate.availableVideoFormats,
                delegate.activeVideoFormat,
                delegate.selectedQuality,
                delegate.availableCaptions,
                delegate.selectedCaptionId,
            ) { formats, active, quality, captions, captionId ->
                NativeVideoControlState(
                    available = true,
                    qualities =
                        listOf(
                            VideoQualityOption("auto", "Auto"),
                            VideoQualityOption("highest", "Highest supported"),
                        ) + formats.map { VideoQualityOption("${it.height}", "${it.height}p") }.distinctBy(VideoQualityOption::id),
                    selectedQualityId = quality.toControlId(),
                    captions = captions.map { VideoCaptionOption(it.id, it.label) },
                    selectedCaptionId = captionId,
                    activeFormatLabel = active?.displayLabel,
                )
            }.collect(_nativeControls)
        }
    }

    override fun prepare(
        session: VideoSessionKey,
        spec: EmbeddedVideoPlayerSpec,
    ) {
        this.session = session
        _state.value = EmbeddedVideoPlayerState.Loading(session)
        delegate.prepare(spec.videoId)
    }

    override fun play() = delegate.play()

    override fun pause() = delegate.pause()

    override fun seekTo(positionMs: Long) = delegate.seekTo(positionMs)

    override fun release() {
        delegate.release()
        session = null
        _state.value = EmbeddedVideoPlayerState.Idle
    }

    override fun createSurfaceView(context: Context): View = delegate.createView(context)

    override fun disposeSurfaceView(view: View) = Unit

    override fun focusPlayer() = Unit

    override fun clearPlayerFocus() = Unit

    override fun adjustVolume(delta: Int) = Unit

    override fun selectQuality(id: String) {
        delegate.selectQuality(
            when (id) {
                "auto" -> YouTubeQuality.Auto
                "highest" -> YouTubeQuality.HighestSupported
                else -> YouTubeQuality.Resolution(id.toIntOrNull() ?: return)
            },
        )
    }

    override fun selectCaption(id: String?) = delegate.selectCaption(id)
}

private fun YouTubeNativePlayerState.toEmbeddedState(session: VideoSessionKey?): EmbeddedVideoPlayerState {
    if (session == null) return EmbeddedVideoPlayerState.Idle
    return when (this) {
        YouTubeNativePlayerState.Idle -> EmbeddedVideoPlayerState.Idle
        is YouTubeNativePlayerState.Resolving,
        is YouTubeNativePlayerState.Preparing,
        -> EmbeddedVideoPlayerState.Loading(session)
        is YouTubeNativePlayerState.Ready -> EmbeddedVideoPlayerState.Ready(session, durationMs)
        is YouTubeNativePlayerState.Playing -> EmbeddedVideoPlayerState.Playing(session, positionMs, durationMs)
        is YouTubeNativePlayerState.Paused -> EmbeddedVideoPlayerState.Paused(session, positionMs, durationMs)
        is YouTubeNativePlayerState.Buffering -> EmbeddedVideoPlayerState.Buffering(session, positionMs, durationMs)
        is YouTubeNativePlayerState.Ended -> EmbeddedVideoPlayerState.Ended(session, positionMs, durationMs)
        is YouTubeNativePlayerState.Error -> EmbeddedVideoPlayerState.Error(session, message)
    }
}

private fun YouTubeQuality.toControlId(): String =
    when (this) {
        YouTubeQuality.Auto -> "auto"
        YouTubeQuality.HighestSupported -> "highest"
        is YouTubeQuality.Resolution -> height.toString()
    }
