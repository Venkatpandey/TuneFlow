package com.tuneflow.feature.video

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class VideoHistoryEntry(
    val trackId: String,
    val provider: String,
    val videoId: String,
    val title: String,
    val publisher: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val viewCount: Long,
    val mappingUpdatedAt: String,
    val lastPlayedAt: String,
)

sealed interface PreferredVideoLookupResult {
    data class Found(val video: VideoHistoryEntry) : PreferredVideoLookupResult

    data object Missing : PreferredVideoLookupResult

    data object BackendUnavailable : PreferredVideoLookupResult
}

interface PreferredVideoStore {
    val history: StateFlow<List<VideoHistoryEntry>>

    suspend fun lookup(trackId: String): PreferredVideoLookupResult

    suspend fun savePreferredVideo(
        trackId: String,
        candidate: VideoCandidate,
    ): Boolean

    suspend fun markPlayed(trackId: String): Boolean

    suspend fun deletePreferredVideo(trackId: String): Boolean

    suspend fun refreshHistory(limit: Int = VIDEO_HISTORY_LIMIT): Boolean
}

class RemotePreferredVideoStore(
    baseUrl: String,
    private val client: OkHttpClient = defaultPreferredVideoClient(),
) : PreferredVideoStore {
    @Volatile
    private var serviceUrl: HttpUrl? = preferredVideoServiceHttpUrl(baseUrl)
    private val json = Json { ignoreUnknownKeys = true }
    private val _history = MutableStateFlow<List<VideoHistoryEntry>>(emptyList())
    override val history: StateFlow<List<VideoHistoryEntry>> = _history.asStateFlow()

    fun updateServiceUrl(baseUrl: String) {
        serviceUrl = preferredVideoServiceHttpUrl(baseUrl)
        _history.value = emptyList()
    }

    override suspend fun lookup(trackId: String): PreferredVideoLookupResult {
        val request =
            requestBuilder("v1", "tracks", trackId, "preferred-video")?.get()?.build()
                ?: return PreferredVideoLookupResult.BackendUnavailable
        return executeSafely(request) { response ->
            when (response.code) {
                200 ->
                    PreferredVideoLookupResult.Found(
                        decodeVideoResponse(response),
                    )
                404 -> PreferredVideoLookupResult.Missing
                else -> PreferredVideoLookupResult.BackendUnavailable
            }
        } ?: PreferredVideoLookupResult.BackendUnavailable
    }

    override suspend fun savePreferredVideo(
        trackId: String,
        candidate: VideoCandidate,
    ): Boolean {
        val payload =
            PreferredVideoWrite(
                provider = YOUTUBE_PROVIDER,
                videoId = candidate.videoId,
                title = candidate.title,
                publisher = candidate.publisher,
                thumbnailUrl = candidate.thumbnailUrl,
                durationMs = candidate.durationMs,
                viewCount = candidate.viewCount,
            )
        val request =
            requestBuilder("v1", "tracks", trackId, "preferred-video")
                ?.put(json.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE))
                ?.build()
                ?: return false
        return executeVideoWrite(request)
    }

    override suspend fun markPlayed(trackId: String): Boolean {
        val request =
            requestBuilder("v1", "tracks", trackId, "preferred-video", "played")
                ?.post(EMPTY_JSON_BODY)
                ?.build()
                ?: return false
        return executeVideoWrite(request)
    }

    override suspend fun deletePreferredVideo(trackId: String): Boolean {
        val request =
            requestBuilder("v1", "tracks", trackId, "preferred-video")?.delete()?.build()
                ?: return false
        return executeSafely(request) { response -> response.code == 204 || response.code == 404 } ?: false
    }

    override suspend fun refreshHistory(limit: Int): Boolean {
        val builder = requestBuilder("v1", "videos", "recent")
        if (builder == null) {
            _history.value = emptyList()
            return false
        }
        val url =
            builder.build().url.newBuilder()
                .addQueryParameter("limit", limit.coerceIn(1, VIDEO_HISTORY_LIMIT).toString())
                .build()
        val request = builder.url(url).get().build()
        val result =
            executeSafely(request) { response ->
                if (response.code != 200) return@executeSafely null
                val envelope = json.decodeFromString<RecentVideosResponse>(response.requireBody())
                require(envelope.apiVersion == API_VERSION) { "Unsupported API version." }
                require(envelope.videos.all(::isValidVideoResponse)) { "Invalid video response." }
                envelope.videos.take(VIDEO_HISTORY_LIMIT)
            }
        _history.value = result.orEmpty()
        return result != null
    }

    private suspend fun executeVideoWrite(request: Request): Boolean =
        executeSafely(request) { response ->
            if (response.code != 200) return@executeSafely false
            val video = decodeVideoResponse(response)
            _history.value = updatedRemoteHistory(_history.value, video)
            true
        } ?: false

    private fun requestBuilder(vararg pathSegments: String): Request.Builder? {
        val base = serviceUrl ?: return null
        val url =
            base.newBuilder().apply {
                pathSegments.forEach(::addPathSegment)
            }.build()
        return Request.Builder()
            .url(url)
            .header("Accept", "application/json")
    }

    private suspend fun <T> executeSafely(
        request: Request,
        transform: (Response) -> T,
    ): T? =
        try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use(transform)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            null
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        }

    private fun decodeVideoResponse(response: Response): VideoHistoryEntry {
        val envelope = json.decodeFromString<VideoResponse>(response.requireBody())
        require(envelope.apiVersion == API_VERSION) { "Unsupported API version." }
        require(isValidVideoResponse(envelope.preferredVideo)) { "Invalid video response." }
        return envelope.preferredVideo
    }
}

object UnavailablePreferredVideoStore : PreferredVideoStore {
    override val history: StateFlow<List<VideoHistoryEntry>> = MutableStateFlow(emptyList())

    override suspend fun lookup(trackId: String) = PreferredVideoLookupResult.BackendUnavailable

    override suspend fun savePreferredVideo(
        trackId: String,
        candidate: VideoCandidate,
    ) = false

    override suspend fun markPlayed(trackId: String) = false

    override suspend fun deletePreferredVideo(trackId: String) = false

    override suspend fun refreshHistory(limit: Int) = false
}

fun VideoHistoryEntry.toVideoCandidate(): VideoCandidate =
    VideoCandidate(
        videoId = videoId,
        title = title,
        publisher = publisher,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        musicCategory = true,
        viewCount = viewCount,
    )

internal fun updatedRemoteHistory(
    current: List<VideoHistoryEntry>,
    entry: VideoHistoryEntry,
): List<VideoHistoryEntry> =
    buildList {
        add(entry)
        current.filterTo(this) { it.trackId != entry.trackId }
    }.take(VIDEO_HISTORY_LIMIT)

private fun Response.requireBody(): String {
    val content = body?.string() ?: error("Response body is missing.")
    require(content.length <= MAX_RESPONSE_CHARACTERS) { "Response body is too large." }
    return content
}

private fun isValidVideoResponse(video: VideoHistoryEntry): Boolean =
    video.trackId.isNotBlank() &&
        video.provider == YOUTUBE_PROVIDER &&
        YOUTUBE_VIDEO_ID.matches(video.videoId) &&
        video.title.isNotBlank() &&
        video.publisher.isNotBlank() &&
        video.durationMs >= 0L &&
        video.viewCount >= 0L &&
        video.mappingUpdatedAt.isNotBlank() &&
        video.lastPlayedAt.isNotBlank() &&
        (video.thumbnailUrl == null || video.thumbnailUrl.toHttpUrlOrNull() != null)

private fun preferredVideoServiceHttpUrl(baseUrl: String): HttpUrl? =
    normalizePreferredVideoServiceUrl(baseUrl)
        ?.takeIf(String::isNotEmpty)
        ?.plus("/")
        ?.toHttpUrlOrNull()

private fun defaultPreferredVideoClient(): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(1_500L, TimeUnit.MILLISECONDS)
        .readTimeout(2_500L, TimeUnit.MILLISECONDS)
        .writeTimeout(2_500L, TimeUnit.MILLISECONDS)
        .callTimeout(3_000L, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

@Serializable
private data class VideoResponse(
    val apiVersion: String,
    val preferredVideo: VideoHistoryEntry,
)

@Serializable
private data class RecentVideosResponse(
    val apiVersion: String,
    val videos: List<VideoHistoryEntry>,
)

@Serializable
private data class PreferredVideoWrite(
    val provider: String,
    val videoId: String,
    val title: String,
    val publisher: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val viewCount: Long,
)

const val VIDEO_HISTORY_LIMIT = 100
internal const val YOUTUBE_PROVIDER = "youtube"
private const val API_VERSION = "v1"
private const val MAX_RESPONSE_CHARACTERS = 256 * 1024
private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
private val EMPTY_JSON_BODY = ByteArray(0).toRequestBody(JSON_MEDIA_TYPE)
