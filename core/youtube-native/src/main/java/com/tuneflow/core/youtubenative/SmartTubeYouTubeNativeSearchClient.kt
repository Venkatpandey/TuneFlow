package com.tuneflow.core.youtubenative

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem
import com.liskovsoft.mediaserviceinterfaces.data.SearchOptions
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

class SmartTubeYouTubeNativeSearchClient(
    context: Context,
) : YouTubeNativeSearchClient {
    init {
        SmartTubeRuntime.initialize(context)
    }

    override suspend fun search(
        artist: String,
        title: String,
        limit: Int,
    ): List<YouTubeNativeSearchResult> =
        withContext(Dispatchers.IO) {
            val query = listOf(artist.trim(), title.trim()).filter(String::isNotBlank).joinToString(" ")
            require(query.isNotBlank()) { "Artist and title cannot both be blank." }
            val options = SearchOptions.TYPE_VIDEO or SearchOptions.SORT_BY_RELEVANCE
            val groups = YouTubeServiceManager.instance().contentService.getSearch(query, options).orEmpty()
            mapSmartTubeSearchItems(
                groups.flatMap { it.mediaItems.orEmpty() },
                limit.coerceIn(1, MAX_RESULTS),
            )
        }
}

internal fun mapSmartTubeSearchItems(
    items: List<MediaItem?>,
    limit: Int,
): List<YouTubeNativeSearchResult> =
    items
        .asSequence()
        .filterNotNull()
        .filter { isPlayableSmartTubeSearchResult(it.type, it.videoId) }
        .filterNot { it.isLive || it.isUpcoming || it.isShorts }
        .mapNotNull(::mapSmartTubeItem)
        .distinctBy(YouTubeNativeSearchResult::videoId)
        .take(limit)
        .toList()

internal fun isPlayableSmartTubeSearchResult(
    type: Int,
    videoId: String?,
): Boolean =
    when (type) {
        MediaItem.TYPE_VIDEO,
        MediaItem.TYPE_MUSIC,
        MediaItem.TYPE_UNDEFINED,
        -> videoId?.matches(YOUTUBE_VIDEO_ID) == true
        else -> false
    }

private fun mapSmartTubeItem(item: MediaItem): YouTubeNativeSearchResult? {
    return mapSmartTubeFields(
        videoId = item.videoId,
        title = item.title,
        author = item.author,
        secondTitle = item.secondTitle?.toString(),
        thumbnailUrl = item.cardImageUrl,
        durationMs = item.durationMs,
        isLive = item.isLive,
        isShort = item.isShorts,
    )
}

@Suppress("ReturnCount")
internal fun mapSmartTubeFields(
    videoId: String?,
    title: String?,
    author: String?,
    secondTitle: String?,
    thumbnailUrl: String?,
    durationMs: Long,
    isLive: Boolean,
    isShort: Boolean,
): YouTubeNativeSearchResult? {
    val mappedVideoId = videoId?.takeIf(String::isNotBlank) ?: return null
    val mappedTitle = title?.takeIf(String::isNotBlank) ?: return null
    val details = secondTitle.orEmpty().split(DETAIL_SEPARATOR).map(String::trim).filter(String::isNotBlank)
    val channel = details.firstOrNull().orEmpty().ifBlank { author.orEmpty() }
    val viewCount = details.firstNotNullOfOrNull(::parseYouTubeViewCount) ?: 0L
    return YouTubeNativeSearchResult(
        videoId = mappedVideoId,
        title = mappedTitle,
        channel = channel,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs.coerceAtLeast(0L),
        viewCount = viewCount,
        isLive = isLive,
        isShort = isShort,
    )
}

@Suppress("ReturnCount")
internal fun parseYouTubeViewCount(text: String): Long? {
    val normalized =
        Normalizer.normalize(text, Normalizer.Form.NFKD)
            .lowercase(Locale.ROOT)
            .replace(Regex("\\p{M}+"), "")
    if (!VIEW_MARKERS.any(normalized::contains)) return null
    val numberToken = Regex("\\d[\\d\\s.,]*").find(normalized)?.value?.trim() ?: return null
    val multiplier =
        when {
            BILLION_MARKERS.any(normalized::contains) -> 1_000_000_000L
            MILLION_MARKERS.any(normalized::contains) -> 1_000_000L
            THOUSAND_MARKERS.any(normalized::contains) -> 1_000L
            else -> 1L
        }
    val number =
        if (multiplier == 1L) {
            numberToken.filter(Char::isDigit).toDoubleOrNull()
        } else {
            parseAbbreviatedNumber(numberToken)
        } ?: return null
    return (number * multiplier).toLong().coerceAtLeast(0L)
}

private fun parseAbbreviatedNumber(token: String): Double? {
    val compact = token.filterNot(Char::isWhitespace)
    val separatorIndex = maxOf(compact.lastIndexOf(','), compact.lastIndexOf('.'))
    if (separatorIndex < 0) return compact.toDoubleOrNull()
    val whole = compact.take(separatorIndex).filter(Char::isDigit)
    val fraction = compact.drop(separatorIndex + 1).filter(Char::isDigit)
    return "$whole.$fraction".toDoubleOrNull()
}

private const val MAX_RESULTS = 25
private val YOUTUBE_VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
private val DETAIL_SEPARATOR = Regex("[•·]")
private val VIEW_MARKERS = listOf("view", "aufruf", "vue", "visualiz", "watched")
private val THOUSAND_MARKERS = listOf("k view", "k aufruf", "tsd")
private val MILLION_MARKERS = listOf("m view", "m aufruf", "mio", "million")
private val BILLION_MARKERS = listOf("b view", "b aufruf", "mrd", "billion", "milliard")
