package com.tuneflow.feature.video

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.ByteArrayInputStream

class YouTubeEmbeddedPlayer : EmbeddedVideoPlayer {
    private val _state = MutableStateFlow<EmbeddedVideoPlayerState>(EmbeddedVideoPlayerState.Idle)
    override val state: StateFlow<EmbeddedVideoPlayerState> = _state.asStateFlow()

    private var webView: WebView? = null
    private var session: VideoSessionKey? = null
    private var spec: EmbeddedVideoPlayerSpec? = null
    private var apiReady = false
    private var requestedStartMs = 0L
    private var videoSurfaceLayerConfigured = false

    override fun prepare(
        session: VideoSessionKey,
        spec: EmbeddedVideoPlayerSpec,
    ) {
        require(spec.providerId == VideoProviderId.YouTube)
        this.session = session
        this.spec = spec
        requestedStartMs = 0L
        videoSurfaceLayerConfigured = false
        _state.value = EmbeddedVideoPlayerState.Loading(session)
        loadRequestedVideoIfReady()
    }

    override fun play() {
        evaluateJavascript("playVideo()")
    }

    override fun pause() {
        evaluateJavascript("pauseVideo()")
    }

    override fun seekTo(positionMs: Long) {
        requestedStartMs = positionMs.coerceAtLeast(0L)
        evaluateJavascript("seekVideo(${requestedStartMs / 1000.0})")
    }

    fun focusPlayer() {
        evaluateJavascript("focusPlayer()")
    }

    fun clearPlayerFocus() {
        evaluateJavascript("clearPlayerFocus()")
    }

    fun adjustVolume(delta: Int) {
        evaluateJavascript("adjustVolume($delta)")
    }

    override fun release() {
        session = null
        spec = null
        apiReady = false
        requestedStartMs = 0L
        videoSurfaceLayerConfigured = false
        _state.value = EmbeddedVideoPlayerState.Idle
        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            destroyWebView(view)
        }
        webView = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(context: Context): WebView {
        webView?.let { return it }
        val appContext = context.applicationContext
        val assetLoader =
            WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
                .build()
        val created =
            WebView(context).apply {
                // Some Fire OS WebView builds render HTML5 video through a decoder surface
                // behind the WebView. Keep this surface transparent and let Compose supply
                // the black backdrop so video frames are not covered while audio plays.
                setBackgroundColor(Color.TRANSPARENT)
                isFocusable = true
                isFocusableInTouchMode = true
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setGeolocationEnabled(false)
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(PlayerBridge(), BRIDGE_NAME)
                webViewClient = HardenedYouTubeWebViewClient(appContext, assetLoader)
                webChromeClient = WebChromeClient()
            }
        configureVideoCodecCompatibility(created)
        webView = created
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(appContext) {}
        }
        created.loadUrl(PLAYER_ASSET_URL)
        return created
    }

    fun disposeWebView(view: WebView) {
        if (view === webView) release()
    }

    private fun destroyWebView(view: WebView) {
        view.stopLoading()
        view.loadUrl("about:blank")
        view.removeJavascriptInterface(BRIDGE_NAME)
        view.clearHistory()
        view.removeAllViews()
        view.destroy()
    }

    private fun loadRequestedVideoIfReady() {
        val requestedSpec = spec ?: return
        if (!apiReady || webView == null) return
        val videoId = JSONObject.quote(requestedSpec.videoId)
        evaluateJavascript("loadVideo($videoId, ${requestedStartMs / 1000.0})")
    }

    private fun evaluateJavascript(script: String) {
        webView?.post { webView?.evaluateJavascript(script, null) }
    }

    private fun configureVideoCodecCompatibility(view: WebView) {
        if (hasHardwareAv1Decoder()) return
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Log.w(LOG_TAG, "Cannot disable software AV1: document-start scripts are unavailable")
            return
        }

        WebViewCompat.addDocumentStartJavaScript(
            view,
            AV1_CODEC_COMPATIBILITY_SCRIPT,
            YOUTUBE_PLAYER_ORIGINS,
        )
        Log.i(LOG_TAG, "Software AV1 disabled; YouTube will select a hardware-decodable codec")
    }

    private fun hasHardwareAv1Decoder(): Boolean =
        runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.any { codec ->
                !codec.isEncoder &&
                    codec.supportedTypes.any { it.equals(AV1_MIME_TYPE, ignoreCase = true) } &&
                    codec.isHardwareAcceleratedCompat()
            }
        }.getOrDefault(false)

    private fun handleEvent(
        name: String,
        payload: String,
    ) {
        webView?.post {
            when (name) {
                "apiReady" -> {
                    apiReady = true
                    loadRequestedVideoIfReady()
                }
                "ready" -> publishReady(payload)
                "state" -> publishState(payload)
                "time" -> publishTime(payload)
                "diagnostic" -> Log.i(LOG_TAG, "YouTube player: $payload")
                "autoplayBlocked" -> publishError("YouTube blocked automatic playback. Press Video to retry.")
                "error" -> publishError(mapYouTubeError(JSONObject(payload).optInt("code")))
            }
        }
    }

    private fun publishReady(payload: String) {
        val activeSession = session ?: return
        val values = JSONObject(payload)
        _state.value =
            EmbeddedVideoPlayerState.Ready(
                session = activeSession,
                durationMs = values.durationMs(),
            )
    }

    private fun publishState(payload: String) {
        val activeSession = session ?: return
        val values = JSONObject(payload)
        val positionMs = values.positionMs()
        val durationMs = values.durationMs()
        _state.value =
            when (values.optInt("code", STATE_UNSTARTED)) {
                STATE_ENDED -> EmbeddedVideoPlayerState.Ended(activeSession, positionMs, durationMs)
                STATE_PLAYING -> {
                    keepHardwareVideoSurfaceBehindControls()
                    EmbeddedVideoPlayerState.Playing(activeSession, positionMs, durationMs)
                }
                STATE_PAUSED -> EmbeddedVideoPlayerState.Paused(activeSession, positionMs, durationMs)
                STATE_BUFFERING -> EmbeddedVideoPlayerState.Buffering(activeSession, positionMs, durationMs)
                else -> return
            }
    }

    private fun keepHardwareVideoSurfaceBehindControls() {
        if (videoSurfaceLayerConfigured) return
        val playerView = webView ?: return
        playerView.post {
            var surfaceCount = 0
            playerView.rootView.forEachSurfaceView { surface ->
                surface.setZOrderOnTop(false)
                surface.setZOrderMediaOverlay(false)
                surfaceCount += 1
            }
            if (surfaceCount > 0) {
                videoSurfaceLayerConfigured = true
                Log.i(LOG_TAG, "Placed $surfaceCount hardware video surface(s) behind YouTube controls")
            }
        }
    }

    private fun publishTime(payload: String) {
        val activeSession = session ?: return
        val values = JSONObject(payload)
        val positionMs = values.positionMs()
        val durationMs = values.durationMs()
        _state.value =
            when (val current = _state.value) {
                is EmbeddedVideoPlayerState.Playing ->
                    EmbeddedVideoPlayerState.Playing(activeSession, positionMs, durationMs)
                is EmbeddedVideoPlayerState.Paused ->
                    EmbeddedVideoPlayerState.Paused(activeSession, positionMs, durationMs)
                is EmbeddedVideoPlayerState.Buffering ->
                    EmbeddedVideoPlayerState.Buffering(activeSession, positionMs, durationMs)
                else -> current
            }
    }

    private fun publishError(message: String) {
        val activeSession = session ?: return
        _state.value = EmbeddedVideoPlayerState.Error(activeSession, message)
    }

    private inner class PlayerBridge {
        @JavascriptInterface
        fun onEvent(
            name: String,
            payload: String,
        ) {
            if (name in APPROVED_BRIDGE_EVENTS && payload.length <= MAX_BRIDGE_PAYLOAD_LENGTH) {
                handleEvent(name, payload)
            }
        }
    }

    private companion object {
        const val PLAYER_ASSET_URL = "https://appassets.androidplatform.net/assets/youtube_player.html"
        const val BRIDGE_NAME = "TuneFlowVideoBridge"
        const val MAX_BRIDGE_PAYLOAD_LENGTH = 1_024
        const val LOG_TAG = "TuneFlowVideo"
        const val AV1_MIME_TYPE = "video/av01"
        const val STATE_UNSTARTED = -1
        const val STATE_ENDED = 0
        const val STATE_PLAYING = 1
        const val STATE_PAUSED = 2
        const val STATE_BUFFERING = 3
        val APPROVED_BRIDGE_EVENTS =
            setOf("apiReady", "ready", "state", "time", "diagnostic", "error", "autoplayBlocked")

        fun mapYouTubeError(code: Int): String =
            when (code) {
                2 -> "YouTube rejected this video ID."
                5 -> "This video cannot play in the YouTube HTML5 player."
                100 -> "This YouTube video is private, deleted, or unavailable."
                101, 150 -> "The publisher disabled embedded playback for this video."
                153 -> "YouTube could not verify this app's player identity."
                else -> "YouTube playback failed ($code)."
            }
    }
}

private fun View.forEachSurfaceView(action: (SurfaceView) -> Unit) {
    if (this is SurfaceView) action(this)
    if (this is ViewGroup) {
        for (index in 0 until childCount) {
            getChildAt(index).forEachSurfaceView(action)
        }
    }
}

private fun MediaCodecInfo.isHardwareAcceleratedCompat(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isHardwareAccelerated
    } else {
        val normalizedName = name.lowercase()
        SOFTWARE_CODEC_PREFIXES.none(normalizedName::startsWith)
    }

internal val YOUTUBE_PLAYER_ORIGINS =
    setOf(
        "https://www.youtube.com",
        "https://www.youtube-nocookie.com",
    )

private val SOFTWARE_CODEC_PREFIXES =
    listOf(
        "c2.android.",
        "c2.google.",
        "omx.google.",
    )

internal const val AV1_CODEC_COMPATIBILITY_SCRIPT =
    """
    (() => {
        const isAv1 = value => /(?:av01|av1)/i.test(String(value || ''));

        if (window.MediaSource && MediaSource.isTypeSupported) {
            const originalIsTypeSupported = MediaSource.isTypeSupported.bind(MediaSource);
            MediaSource.isTypeSupported = type => isAv1(type) ? false : originalIsTypeSupported(type);
        }

        if (window.HTMLMediaElement && HTMLMediaElement.prototype.canPlayType) {
            const originalCanPlayType = HTMLMediaElement.prototype.canPlayType;
            HTMLMediaElement.prototype.canPlayType = function(type) {
                return isAv1(type) ? '' : originalCanPlayType.call(this, type);
            };
        }

        if (navigator.mediaCapabilities && navigator.mediaCapabilities.decodingInfo) {
            const originalDecodingInfo = navigator.mediaCapabilities.decodingInfo.bind(navigator.mediaCapabilities);
            navigator.mediaCapabilities.decodingInfo = config => {
                const contentType = config && config.video && config.video.contentType;
                return isAv1(contentType)
                    ? Promise.resolve({ supported: false, smooth: false, powerEfficient: false })
                    : originalDecodingInfo(config);
            };
        }
    })();
    """

private class HardenedYouTubeWebViewClient(
    private val context: Context,
    private val assetLoader: WebViewAssetLoader,
) : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val localAsset = assetLoader.shouldInterceptRequest(request.url)
        val uri = request.url
        return when {
            localAsset != null -> localAsset
            uri.scheme == "https" && VideoDomainPolicy.isAllowedResourceHost(uri.host) ->
                super.shouldInterceptRequest(view, request)
            else -> blockedResponse()
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        if (!request.isForMainFrame) return false
        val uri = request.url
        return when {
            uri.toString() == PLAYER_ASSET_URL -> false
            uri.scheme == "https" && VideoDomainPolicy.isApprovedExternalHost(uri.host) -> {
                openExternal(uri)
                true
            }
            else -> true
        }
    }

    @TargetApi(27)
    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse,
    ) {
        callback.backToSafety(true)
    }

    private fun openExternal(uri: Uri) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun blockedResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Blocked",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0)),
        )

    private companion object {
        const val PLAYER_ASSET_URL = "https://appassets.androidplatform.net/assets/youtube_player.html"
    }
}

private fun JSONObject.positionMs(): Long = (optDouble("positionSeconds", 0.0) * 1000.0).toLong().coerceAtLeast(0L)

private fun JSONObject.durationMs(): Long = (optDouble("durationSeconds", 0.0) * 1000.0).toLong().coerceAtLeast(0L)
