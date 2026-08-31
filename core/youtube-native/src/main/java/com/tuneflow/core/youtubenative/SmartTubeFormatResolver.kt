package com.tuneflow.core.youtubenative

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.WindowManager
import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

internal data class ResolvedYouTubeVideo(
    val formatInfo: MediaItemFormatInfo,
    val sourceKind: YouTubeSourceKind,
    val formats: List<YouTubeVideoFormat>,
    val captions: List<YouTubeCaption>,
)

internal class SmartTubeFormatResolver(
    private val context: Context,
) {
    init {
        SmartTubeRuntime.initialize(context)
    }

    suspend fun resolve(videoId: String): ResolvedYouTubeVideo =
        withContext(Dispatchers.IO) {
            val info = YouTubeServiceManager.instance().mediaItemService.getFormatInfo(videoId)
            if (info.isUnplayable) {
                throw NativeResolverException(classifyPlayabilityError(info.playabilityReason), info.playabilityReason)
            }
            val sourceKind = selectSourceKind(info) ?: throw NativeResolverException(YouTubeNativeError.Resolver, null)
            val capabilities = deviceCapabilities(context)
            ResolvedYouTubeVideo(
                formatInfo = info,
                sourceKind = sourceKind,
                formats = info.adaptiveFormats.orEmpty().mapNotNull { it.toVideoFormat(capabilities.hardwareCodecs) }.distinctBy { it.id },
                captions =
                    info.subtitles.orEmpty().mapNotNull { subtitle ->
                        val id = subtitle.vssId?.takeIf(String::isNotBlank) ?: subtitle.baseUrl?.takeIf(String::isNotBlank)
                        id?.let {
                            YouTubeCaption(
                                id = it,
                                label = subtitle.name?.takeIf(String::isNotBlank) ?: subtitle.languageCode.orEmpty(),
                                language = subtitle.languageCode,
                            )
                        }
                    },
            )
        }
}

internal class NativeResolverException(
    val kind: YouTubeNativeError,
    detail: String?,
) : IllegalStateException(detail ?: "YouTube returned no playable native format.")

internal fun selectSourceKind(info: MediaItemFormatInfo): YouTubeSourceKind? =
    selectSourceKind(
        hasDashFormats = info.containsDashFormats(),
        hasSabrFormats = info.containsSabrFormats() && !info.isLive,
        hasHls = info.containsHlsUrl(),
        hasDirect = info.containsUrlFormats(),
        hasDashManifest = info.containsDashUrl(),
    )

internal fun selectSourceKind(
    hasDashFormats: Boolean,
    hasSabrFormats: Boolean,
    hasHls: Boolean,
    hasDirect: Boolean,
    hasDashManifest: Boolean = false,
): YouTubeSourceKind? =
    when {
        hasDashFormats || hasDashManifest -> YouTubeSourceKind.Dash
        hasSabrFormats -> YouTubeSourceKind.Sabr
        hasHls -> YouTubeSourceKind.Hls
        hasDirect -> YouTubeSourceKind.Direct
        else -> null
    }

private fun classifyPlayabilityError(reason: String?): YouTubeNativeError {
    val normalized = reason.orEmpty().lowercase(Locale.ROOT)
    return when {
        "region" in normalized || "country" in normalized -> YouTubeNativeError.RegionRestricted
        "age" in normalized || "sign in" in normalized -> YouTubeNativeError.AgeRestricted
        else -> YouTubeNativeError.Unplayable
    }
}

private fun MediaFormat.toVideoFormat(hardwareCodecs: Set<String>): YouTubeVideoFormat? {
    if (width <= 0 || height <= 0) return null
    val codec = codecName(mimeType)
    return YouTubeVideoFormat(
        id = iTag ?: "$width-$height-$mimeType",
        width = width,
        height = height,
        fps = fps?.toFloatOrNull() ?: 0f,
        bitrate = bitrate?.toIntOrNull() ?: 0,
        mimeType = mimeType.orEmpty(),
        codec = codec,
        hardwareSupported = codec in hardwareCodecs,
    )
}

internal fun codecName(mimeType: String?): String {
    val value = mimeType.orEmpty().lowercase(Locale.ROOT)
    return when {
        "av01" in value || "video/av01" in value -> CODEC_AV1
        "vp9" in value || "vp09" in value || "video/x-vnd.on2.vp9" in value -> CODEC_VP9
        "avc" in value || "h264" in value || "video/avc" in value -> CODEC_AVC
        else -> value.substringAfter("codecs=\"").substringBefore('"').substringBefore('.').ifBlank { "unknown" }
    }
}

internal fun deviceCapabilities(context: Context): YouTubePlaybackCapabilities {
    val display = context.getSystemService(WindowManager::class.java)?.defaultDisplay
    val size = android.graphics.Point()
    @Suppress("DEPRECATION")
    display?.getRealSize(size)
    val codecs =
        runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                .asSequence()
                .filterNot(MediaCodecInfo::isEncoder)
                .filter { it.isHardwareAcceleratedCompat() }
                .flatMap { it.supportedTypes.asSequence() }
                .map(::codecName)
                .toSet()
        }.getOrDefault(emptySet())
    return YouTubePlaybackCapabilities(
        displayWidth = size.x.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels,
        displayHeight = size.y.takeIf { it > 0 } ?: context.resources.displayMetrics.heightPixels,
        hardwareCodecs = codecs,
        apiLevel = Build.VERSION.SDK_INT,
    )
}

private fun MediaCodecInfo.isHardwareAcceleratedCompat(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isHardwareAccelerated
    } else {
        val name = name.lowercase(Locale.ROOT)
        !name.startsWith("omx.google.") && !name.startsWith("c2.android.") && !name.contains("software") && !name.contains("sw.")
    }
