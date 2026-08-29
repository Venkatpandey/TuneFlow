package com.tuneflow.feature.video

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

internal const val YOUTUBE_SEARCH_RESULT_LIMIT = 25

class YouTubeVideoProvider(
    private val apiKey: String,
    private val packageName: String = "",
    private val certificateSha1: String = "",
    private val connectionFactory: (String) -> HttpURLConnection = ::openConnection,
) : VideoProvider {
    override val id: VideoProviderId = VideoProviderId.YouTube
    override val capabilities =
        VideoProviderCapabilities(
            supportsSeeking = true,
            usesAdaptiveQuality = true,
        )
    override val configured: Boolean = apiKey.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: VideoTrackQuery): List<VideoCandidate> {
        check(configured) { "YouTube Data API key is not configured." }

        return withTimeout(SEARCH_TIMEOUT_MS) {
            val searchResponse =
                json.decodeFromString<YouTubeSearchResponse>(
                    executeWithRetry(YouTubeRequests.search(query)),
                )
            val ids = searchResponse.items.mapNotNull { it.id.videoId }.distinct().take(YOUTUBE_SEARCH_RESULT_LIMIT)
            if (ids.isEmpty()) return@withTimeout emptyList()

            val detailsResponse =
                json.decodeFromString<YouTubeVideosResponse>(
                    executeWithRetry(YouTubeRequests.details(ids)),
                )
            val detailById = detailsResponse.items.associateBy(YouTubeVideoItem::id)

            ids.mapNotNull { videoId ->
                detailById[videoId]?.toCandidate(query.regionCode)
            }
        }
    }

    override fun createPlayerSpec(candidate: VideoCandidate): EmbeddedVideoPlayerSpec {
        require(candidate.providerId == id) { "Candidate belongs to another video provider." }
        return EmbeddedVideoPlayerSpec(id, candidate.videoId)
    }

    private suspend fun executeWithRetry(requestUrl: String): String {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return execute(requestUrl)
            } catch (error: YouTubeTransientException) {
                lastError = error
                if (attempt < MAX_ATTEMPTS - 1) {
                    delay(RETRY_BASE_DELAY_MS * (1L shl attempt))
                }
            }
        }
        throw lastError ?: YouTubeTransientException("YouTube request failed.")
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun execute(requestUrl: String): String =
        withContext(Dispatchers.IO) {
            val connection = connectionFactory(requestUrl)
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-goog-api-key", apiKey)
                connection.setRequestProperty("User-Agent", "TuneFlow-Android-TV")
                if (packageName.isNotBlank()) {
                    connection.setRequestProperty("X-Android-Package", packageName)
                }
                if (certificateSha1.isNotBlank()) {
                    connection.setRequestProperty("X-Android-Cert", certificateSha1)
                }

                val status = connection.responseCode
                val responseBody =
                    (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.use { stream ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var total = 0
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= MAX_RESPONSE_BYTES) { "YouTube response is too large." }
                                output.write(buffer, 0, count)
                            }
                            output.toString(StandardCharsets.UTF_8.name())
                        }.orEmpty()

                when {
                    status in 200..299 -> responseBody
                    status == 401 || status == 403 ->
                        throw YouTubeConfigurationException(
                            "YouTube search is unavailable. Check API key restrictions and quota.",
                        )
                    status == 408 || status == 429 || status >= 500 ->
                        throw YouTubeTransientException("YouTube search temporarily failed ($status).")
                    else -> throw YouTubeSearchException("YouTube search failed ($status).")
                }
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        private const val SEARCH_TIMEOUT_MS = 12_000L
        private const val CONNECT_TIMEOUT_MS = 4_000
        private const val READ_TIMEOUT_MS = 6_000
        private const val RETRY_BASE_DELAY_MS = 300L
        private const val MAX_ATTEMPTS = 2
        private const val MAX_RESPONSE_BYTES = 1_048_576

        fun configured(context: Context): YouTubeVideoProvider {
            val identity = AndroidApiClientIdentity.from(context)
            return YouTubeVideoProvider(
                apiKey = BuildConfig.YOUTUBE_API_KEY,
                packageName = identity.packageName,
                certificateSha1 = identity.certificateSha1,
            )
        }

        private fun openConnection(url: String): HttpURLConnection = URI(url).toURL().openConnection() as HttpURLConnection
    }
}

internal object YouTubeRequests {
    private const val API_ROOT = "https://www.googleapis.com/youtube/v3"

    fun search(query: VideoTrackQuery): String {
        val searchText = "${query.artist} ${query.title} official music video".trim()
        val parameters =
            linkedMapOf(
                "part" to "snippet",
                "type" to "video",
                "q" to searchText,
                "maxResults" to YOUTUBE_SEARCH_RESULT_LIMIT.toString(),
                "safeSearch" to "none",
                "videoEmbeddable" to "true",
                "videoSyndicated" to "true",
            ).apply {
                query.regionCode?.takeIf { it.length == 2 }?.let { put("regionCode", it.uppercase(Locale.ROOT)) }
                query.languageCode?.takeIf { it.isNotBlank() }?.let { put("relevanceLanguage", it) }
            }
        return "$API_ROOT/search?${parameters.toQueryString()}"
    }

    fun details(ids: List<String>): String {
        require(ids.isNotEmpty())
        val parameters =
            linkedMapOf(
                "part" to "snippet,contentDetails,status",
                "id" to ids.joinToString(","),
            )
        return "$API_ROOT/videos?${parameters.toQueryString()}"
    }

    private fun Map<String, String>.toQueryString(): String =
        entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }

    private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}

open class YouTubeSearchException(message: String) : Exception(message)

class YouTubeConfigurationException(message: String) : YouTubeSearchException(message)

class YouTubeTransientException(message: String) : YouTubeSearchException(message)

@Serializable
private data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem> = emptyList(),
)

@Serializable
private data class YouTubeSearchItem(
    val id: YouTubeSearchId = YouTubeSearchId(),
)

@Serializable
private data class YouTubeSearchId(
    val videoId: String? = null,
)

@Serializable
private data class YouTubeVideosResponse(
    val items: List<YouTubeVideoItem> = emptyList(),
)

@Serializable
private data class YouTubeVideoItem(
    val id: String,
    val snippet: YouTubeVideoSnippet = YouTubeVideoSnippet(),
    val contentDetails: YouTubeContentDetails = YouTubeContentDetails(),
    val status: YouTubeVideoStatus = YouTubeVideoStatus(),
) {
    fun toCandidate(regionCode: String?): VideoCandidate? {
        val unplayable =
            !status.embeddable ||
                status.privacyStatus != "public" ||
                snippet.liveBroadcastContent != "none" ||
                !contentDetails.regionRestriction.isPlayableIn(regionCode)
        if (unplayable) return null

        return VideoCandidate(
            providerId = VideoProviderId.YouTube,
            videoId = id,
            title = snippet.title.decodeYouTubeEntities(),
            publisher = snippet.channelTitle.decodeYouTubeEntities(),
            thumbnailUrl = snippet.thumbnails.high?.url ?: snippet.thumbnails.medium?.url ?: snippet.thumbnails.default?.url,
            durationMs = parseIso8601DurationMs(contentDetails.duration),
            musicCategory = snippet.categoryId == "10",
        )
    }
}

@Serializable
private data class YouTubeVideoSnippet(
    val title: String = "",
    val channelTitle: String = "",
    val categoryId: String = "",
    val liveBroadcastContent: String = "none",
    val thumbnails: YouTubeThumbnails = YouTubeThumbnails(),
)

@Serializable
private data class YouTubeThumbnails(
    @SerialName("default") val default: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val high: YouTubeThumbnail? = null,
)

@Serializable
private data class YouTubeThumbnail(val url: String)

@Serializable
private data class YouTubeContentDetails(
    val duration: String = "",
    val contentRating: Map<String, String> = emptyMap(),
    val regionRestriction: YouTubeRegionRestriction = YouTubeRegionRestriction(),
)

@Serializable
private data class YouTubeRegionRestriction(
    val allowed: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
) {
    fun isPlayableIn(regionCode: String?): Boolean {
        val region = regionCode?.uppercase(Locale.ROOT) ?: return allowed.isEmpty()
        return region !in blocked && (allowed.isEmpty() || region in allowed)
    }
}

@Serializable
private data class YouTubeVideoStatus(
    val embeddable: Boolean = false,
    val privacyStatus: String = "",
)

internal fun parseIso8601DurationMs(value: String): Long {
    val match =
        Regex("P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?")
            .matchEntire(value)
            ?: return 0L
    val days = match.groupValues[1].toLongOrNull() ?: 0L
    val hours = match.groupValues[2].toLongOrNull() ?: 0L
    val minutes = match.groupValues[3].toLongOrNull() ?: 0L
    val seconds = match.groupValues[4].toLongOrNull() ?: 0L
    return (((days * 24L + hours) * 60L + minutes) * 60L + seconds) * 1000L
}

private fun String.decodeYouTubeEntities(): String =
    replace("&amp;", "&")
        .replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

private data class AndroidApiClientIdentity(
    val packageName: String,
    val certificateSha1: String,
) {
    companion object {
        @SuppressLint("PackageManagerGetSignatures")
        fun from(context: Context): AndroidApiClientIdentity {
            val packageName = context.packageName
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                }
            val signature =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures?.firstOrNull()
                }
            val fingerprint =
                signature
                    ?.toByteArray()
                    ?.let { MessageDigest.getInstance("SHA-1").digest(it) }
                    ?.joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
                    .orEmpty()
            return AndroidApiClientIdentity(packageName, fingerprint)
        }
    }
}
