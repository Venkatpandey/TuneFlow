package com.tuneflow.feature.video

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class VideoHistoryEntry(
    val videoId: String,
    val title: String,
    val publisher: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val viewCount: Long,
    val playedAtEpochMs: Long,
)

interface VideoHistoryStore {
    val history: StateFlow<List<VideoHistoryEntry>>

    fun record(candidate: VideoCandidate)
}

class SharedPreferencesVideoHistoryStore(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : VideoHistoryStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(VideoHistoryEntry.serializer())
    private val _history = MutableStateFlow(readHistory())
    override val history: StateFlow<List<VideoHistoryEntry>> = _history.asStateFlow()

    override fun record(candidate: VideoCandidate) {
        val updated = updatedVideoHistory(_history.value, candidate.toHistoryEntry(clock()))
        _history.value = updated
        preferences.edit().putString(KEY_HISTORY, json.encodeToString(serializer, updated)).apply()
    }

    private fun readHistory(): List<VideoHistoryEntry> =
        preferences.getString(KEY_HISTORY, null)
            ?.let { encoded -> runCatching { json.decodeFromString(serializer, encoded) }.getOrNull() }
            .orEmpty()
            .take(VIDEO_HISTORY_LIMIT)

    private companion object {
        const val PREFERENCES_NAME = "video_history"
        const val KEY_HISTORY = "recent_videos"
    }
}

object EmptyVideoHistoryStore : VideoHistoryStore {
    override val history: StateFlow<List<VideoHistoryEntry>> = MutableStateFlow(emptyList())

    override fun record(candidate: VideoCandidate) = Unit
}

internal fun updatedVideoHistory(
    current: List<VideoHistoryEntry>,
    entry: VideoHistoryEntry,
): List<VideoHistoryEntry> =
    buildList {
        add(entry)
        current.filterTo(this) { it.videoId != entry.videoId }
    }.take(VIDEO_HISTORY_LIMIT)

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

private fun VideoCandidate.toHistoryEntry(playedAtEpochMs: Long): VideoHistoryEntry =
    VideoHistoryEntry(
        videoId = videoId,
        title = title,
        publisher = publisher,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        viewCount = viewCount,
        playedAtEpochMs = playedAtEpochMs,
    )

const val VIDEO_HISTORY_LIMIT = 20
