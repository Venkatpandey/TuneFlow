package com.tuneflow.core.youtubenative

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class SmartTubeYouTubeNativePlayer(
    context: Context,
) : YouTubeNativePlayer,
    Player.EventListener,
    VideoListener,
    TextOutput {
    private val appContext = context.applicationContext
    private val resolver = SmartTubeFormatResolver(appContext)
    private val mediaSourceFactory = SmartTubeMediaSourceFactory(appContext)
    private var scope = newScope()
    private var trackSelector: DefaultTrackSelector? = null
    private var player: SimpleExoPlayer? = null
    private var textureView: AspectFitTextureView? = null
    private var subtitleView: TextView? = null
    private var videoId: String? = null
    private var initialReadyPublished = false
    private var ticker: Job? = null
    private var requestedQuality: YouTubeQuality = YouTubeQuality.HighestSupported

    private val _state = MutableStateFlow<YouTubeNativePlayerState>(YouTubeNativePlayerState.Idle)
    override val state: StateFlow<YouTubeNativePlayerState> = _state.asStateFlow()
    private val _availableVideoFormats = MutableStateFlow<List<YouTubeVideoFormat>>(emptyList())
    override val availableVideoFormats: StateFlow<List<YouTubeVideoFormat>> = _availableVideoFormats.asStateFlow()
    private val _activeVideoFormat = MutableStateFlow<YouTubeVideoFormat?>(null)
    override val activeVideoFormat: StateFlow<YouTubeVideoFormat?> = _activeVideoFormat.asStateFlow()
    private val _selectedQuality = MutableStateFlow<YouTubeQuality>(requestedQuality)
    override val selectedQuality: StateFlow<YouTubeQuality> = _selectedQuality.asStateFlow()
    private val _availableCaptions = MutableStateFlow<List<YouTubeCaption>>(emptyList())
    override val availableCaptions: StateFlow<List<YouTubeCaption>> = _availableCaptions.asStateFlow()
    private val _selectedCaptionId = MutableStateFlow<String?>(null)
    override val selectedCaptionId: StateFlow<String?> = _selectedCaptionId.asStateFlow()

    override fun createView(context: Context): View {
        val texture = textureView ?: AspectFitTextureView(context).also { textureView = it }
        ensurePlayer().setVideoTextureView(texture)
        val captions =
            subtitleView ?: TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 22f
                gravity = Gravity.CENTER
                setShadowLayer(4f, 0f, 2f, Color.BLACK)
                setPadding(24, 12, 24, 12)
                subtitleView = this
            }
        return FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                texture,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER),
            )
            addView(
                captions,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = 72
                },
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun prepare(videoId: String) {
        this.videoId = videoId
        initialReadyPublished = false
        _state.value = YouTubeNativePlayerState.Resolving(videoId)
        scope.launch {
            try {
                val resolved = resolver.resolve(videoId)
                if (this@SmartTubeYouTubeNativePlayer.videoId != videoId) return@launch
                _availableVideoFormats.value = resolved.formats.sortedByDescending(YouTubeVideoFormat::height)
                _availableCaptions.value = resolved.captions
                applyRequestedQuality()
                _state.value = YouTubeNativePlayerState.Preparing(videoId, resolved.sourceKind)
                ensurePlayer().prepare(mediaSourceFactory.create(resolved))
            } catch (error: NativeResolverException) {
                publishError(videoId, error.kind, error.message)
            } catch (error: Exception) {
                publishError(videoId, YouTubeNativeError.Resolver, error.message)
            }
        }
    }

    override fun play() {
        player?.playWhenReady = true
    }

    override fun pause() {
        player?.playWhenReady = false
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun selectQuality(quality: YouTubeQuality) {
        requestedQuality = quality
        _selectedQuality.value = quality
        applyRequestedQuality()
    }

    @Suppress("ReturnCount")
    override fun selectCaption(captionId: String?) {
        _selectedCaptionId.value = captionId
        val selector = trackSelector ?: return
        val mapped = selector.currentMappedTrackInfo ?: return
        val renderer = (0 until mapped.rendererCount).firstOrNull { mapped.getRendererType(it) == C.TRACK_TYPE_TEXT } ?: return
        val groups = mapped.getTrackGroups(renderer)
        val match =
            captionId?.let { id ->
                (0 until groups.length).firstNotNullOfOrNull { groupIndex ->
                    val group = groups[groupIndex]
                    (0 until group.length).firstOrNull { trackIndex ->
                        val format = group.getFormat(trackIndex)
                        format.id == id || format.language == id || format.label == id
                    }?.let { groupIndex to it }
                }
            }
        val builder = selector.buildUponParameters().setRendererDisabled(renderer, captionId == null)
        if (match != null) {
            builder.setSelectionOverride(renderer, groups, DefaultTrackSelector.SelectionOverride(match.first, match.second))
        } else {
            builder.clearSelectionOverrides(renderer)
        }
        selector.setParameters(builder)
    }

    override fun release() {
        ticker?.cancel()
        ticker = null
        player?.removeListener(this)
        player?.removeVideoListener(this)
        player?.removeTextOutput(this)
        textureView?.let { player?.clearVideoTextureView(it) }
        player?.release()
        player = null
        trackSelector = null
        textureView = null
        subtitleView = null
        videoId = null
        initialReadyPublished = false
        scope.cancel()
        scope = newScope()
        _state.value = YouTubeNativePlayerState.Idle
        _availableVideoFormats.value = emptyList()
        _activeVideoFormat.value = null
        _availableCaptions.value = emptyList()
        _selectedCaptionId.value = null
    }

    override fun onPlayerStateChanged(
        playWhenReady: Boolean,
        playbackState: Int,
    ) {
        val currentPlayer = player ?: return
        val id = videoId ?: return
        val position = currentPlayer.currentPosition.coerceAtLeast(0L)
        val duration = currentPlayer.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
        _state.value =
            if (playbackState == Player.STATE_READY && !initialReadyPublished) {
                initialReadyPublished = true
                YouTubeNativePlayerState.Ready(id, duration)
            } else {
                mapPlayerState(id, playbackState, playWhenReady, position, duration, _state.value)
            }
        if (playbackState == Player.STATE_READY) {
            startTicker()
        } else if (playbackState == Player.STATE_ENDED) {
            ticker?.cancel()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        val kind =
            when {
                error.type == ExoPlaybackException.TYPE_RENDERER -> YouTubeNativeError.Decoder
                error.sourceException is IOException -> YouTubeNativeError.Network
                else -> YouTubeNativeError.Initialization
            }
        publishError(videoId.orEmpty(), kind, error.message)
    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray,
    ) {
        val selected = player?.videoFormat?.toYouTubeVideoFormat() ?: return
        _activeVideoFormat.value = selected
        val desiredId =
            when (val quality = requestedQuality) {
                YouTubeQuality.Auto -> null
                YouTubeQuality.HighestSupported ->
                    YouTubeQualitySelector.highestSupported(
                        _availableVideoFormats.value,
                        deviceCapabilities(appContext),
                    )?.id
                is YouTubeQuality.Resolution ->
                    _availableVideoFormats.value.filter { it.height <= quality.height && it.hardwareSupported }
                        .maxWithOrNull(compareBy<YouTubeVideoFormat> { it.height }.thenBy { it.bitrate })?.id
            }
        if (desiredId != null && selected.id != desiredId) applyRequestedQuality()
    }

    override fun onVideoSizeChanged(
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float,
    ) {
        if (width > 0 && height > 0) textureView?.setVideoAspectRatio(width * pixelWidthHeightRatio / height)
    }

    override fun onRenderedFirstFrame() = Unit

    override fun onSurfaceSizeChanged(
        width: Int,
        height: Int,
    ) = Unit

    override fun onCues(cues: List<Cue>) {
        subtitleView?.text = cues.mapNotNull(Cue::text).joinToString("\n")
    }

    private fun ensurePlayer(): SimpleExoPlayer {
        player?.let { return it }
        val selector = DefaultTrackSelector().also { trackSelector = it }
        selector.setParameters(selector.buildUponParameters().setViewportSizeToPhysicalDisplaySize(appContext, true))
        return ExoPlayerFactory.newSimpleInstance(appContext, selector).also {
            it.addListener(this)
            it.addVideoListener(this)
            it.addTextOutput(this)
            textureView?.let(it::setVideoTextureView)
            player = it
        }
    }

    @Suppress("NestedBlockDepth")
    private fun applyRequestedQuality() {
        val selector = trackSelector ?: return
        val capabilities = deviceCapabilities(appContext)
        val chosen =
            when (val quality = requestedQuality) {
                YouTubeQuality.Auto -> null
                YouTubeQuality.HighestSupported -> YouTubeQualitySelector.highestSupported(_availableVideoFormats.value, capabilities)
                is YouTubeQuality.Resolution ->
                    _availableVideoFormats.value.filter { it.height <= quality.height && it.hardwareSupported }
                        .maxWithOrNull(compareBy<YouTubeVideoFormat> { it.height }.thenBy { it.bitrate })
            }
        val builder = selector.buildUponParameters().setViewportSize(capabilities.displayWidth, capabilities.displayHeight, true)
        if (chosen == null) {
            builder.setForceHighestSupportedBitrate(false).clearVideoSizeConstraints()
            selector.currentMappedTrackInfo?.let { mapped ->
                (0 until mapped.rendererCount)
                    .firstOrNull { mapped.getRendererType(it) == C.TRACK_TYPE_VIDEO }
                    ?.let(builder::clearSelectionOverrides)
            }
        } else {
            builder.setForceHighestSupportedBitrate(true).setMaxVideoSize(chosen.width, chosen.height)
            selector.currentMappedTrackInfo?.let { mapped ->
                val renderer =
                    (0 until mapped.rendererCount).firstOrNull {
                        mapped.getRendererType(it) == C.TRACK_TYPE_VIDEO
                    }
                renderer?.let { rendererIndex ->
                    val groups = mapped.getTrackGroups(rendererIndex)
                    val match =
                        (0 until groups.length).firstNotNullOfOrNull { groupIndex ->
                            val group = groups[groupIndex]
                            (0 until group.length).firstOrNull { group.getFormat(it).id == chosen.id }
                                ?.let { groupIndex to it }
                        }
                    match?.let {
                        builder.setSelectionOverride(
                            rendererIndex,
                            groups,
                            DefaultTrackSelector.SelectionOverride(it.first, it.second),
                        )
                    }
                }
            }
        }
        selector.setParameters(builder)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker =
            scope.launch {
                while (player != null && videoId != null) {
                    delay(POSITION_UPDATE_MS)
                    val currentPlayer = player ?: return@launch
                    val id = videoId ?: return@launch
                    val duration = currentPlayer.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
                    _state.value =
                        mapPlayerState(
                            id,
                            currentPlayer.playbackState,
                            currentPlayer.playWhenReady,
                            currentPlayer.currentPosition.coerceAtLeast(0L),
                            duration,
                            _state.value,
                        )
                }
            }
    }

    private fun publishError(
        id: String,
        kind: YouTubeNativeError,
        detail: String?,
    ) {
        _state.value = YouTubeNativePlayerState.Error(id, kind, nativeErrorMessage(kind, detail))
    }

    private fun Format.toYouTubeVideoFormat(): YouTubeVideoFormat =
        YouTubeVideoFormat(
            id = id ?: "$width-$height-${sampleMimeType.orEmpty()}",
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
            fps = frameRate.coerceAtLeast(0f),
            bitrate = bitrate.coerceAtLeast(0),
            mimeType = sampleMimeType.orEmpty(),
            codec = codecName(listOfNotNull(sampleMimeType, codecs).joinToString(" ")),
            hardwareSupported = true,
        )

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private companion object {
        const val POSITION_UPDATE_MS = 500L
    }
}

internal fun mapPlayerState(
    videoId: String,
    playbackState: Int,
    playWhenReady: Boolean,
    positionMs: Long,
    durationMs: Long,
    previous: YouTubeNativePlayerState,
): YouTubeNativePlayerState =
    when (playbackState) {
        Player.STATE_BUFFERING -> YouTubeNativePlayerState.Buffering(videoId, positionMs, durationMs)
        Player.STATE_READY ->
            if (playWhenReady) {
                YouTubeNativePlayerState.Playing(videoId, positionMs, durationMs)
            } else if (previous is YouTubeNativePlayerState.Preparing || previous is YouTubeNativePlayerState.Resolving) {
                YouTubeNativePlayerState.Ready(videoId, durationMs)
            } else {
                YouTubeNativePlayerState.Paused(videoId, positionMs, durationMs)
            }
        Player.STATE_ENDED -> YouTubeNativePlayerState.Ended(videoId, positionMs, durationMs)
        else -> previous
    }

private fun nativeErrorMessage(
    kind: YouTubeNativeError,
    detail: String?,
): String {
    val prefix =
        when (kind) {
            YouTubeNativeError.Unplayable -> "Video is unplayable."
            YouTubeNativeError.RegionRestricted -> "Video is not available in this region."
            YouTubeNativeError.AgeRestricted -> "Age-restricted video cannot play without sign-in."
            YouTubeNativeError.Resolver -> "Native YouTube resolver failed."
            YouTubeNativeError.Decoder -> "This video format is not supported by the decoder."
            YouTubeNativeError.Network -> "YouTube playback lost its network connection."
            YouTubeNativeError.Initialization -> "Native YouTube player could not start."
        }
    return detail?.takeIf(String::isNotBlank)?.let { "$prefix ${it.take(120)}" } ?: prefix
}
