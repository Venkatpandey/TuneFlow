package com.tuneflow.core.youtubenative

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
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
    ): List<YouTubeNativeSearchResult> =
        withContext(Dispatchers.IO) {
            val query = buildSmartTubeVideoSearchQuery(artist, title)
            val options = SearchOptions.TYPE_VIDEO or SearchOptions.SORT_BY_RELEVANCE
            val contentService = YouTubeServiceManager.instance().contentService
            val groups =
                collectSmartTubeSearchGroups(
                    initialGroups = contentService.getSearch(query, options).orEmpty(),
                    maximumPagesPerGroup = MAX_SEARCH_PAGES,
                    continueGroup = contentService::continueGroup,
                )
            mapSmartTubeSearchItems(
                groups.flatMap { it.mediaItems.orEmpty() },
            )
        }
}

internal fun collectSmartTubeSearchGroups(
    initialGroups: List<MediaGroup>,
    maximumPagesPerGroup: Int,
    continueGroup: (MediaGroup) -> MediaGroup?,
): List<MediaGroup> {
    require(maximumPagesPerGroup > 0) { "Search page limit must be positive." }
    return buildList {
        initialGroups.forEach { initialGroup ->
            val seenPageKeys = mutableSetOf<String>()
            var currentGroup: MediaGroup? = initialGroup
            var collectedPageCount = 0
            while (currentGroup != null && collectedPageCount < maximumPagesPerGroup) {
                val group = currentGroup
                add(group)
                collectedPageCount += 1
                val nextPageKey = group.nextPageKey?.takeIf(String::isNotBlank)
                currentGroup =
                    if (
                        collectedPageCount < maximumPagesPerGroup &&
                        nextPageKey != null &&
                        seenPageKeys.add(nextPageKey)
                    ) {
                        continueGroup(group)
                    } else {
                        null
                    }
            }
        }
    }
}

internal fun buildSmartTubeVideoSearchQuery(
    artist: String,
    title: String,
): String {
    val cleanedArtist = artist.trim()
    val cleanedTitle = cleanTrackTitleForVideoSearch(title)
    require(cleanedArtist.isNotBlank() || cleanedTitle.isNotBlank()) {
        "Artist and title cannot both be blank."
    }
    return listOf(cleanedArtist, cleanedTitle, OFFICIAL_VIDEO_QUERY)
        .filter(String::isNotBlank)
        .joinToString(" ")
}

private fun cleanTrackTitleForVideoSearch(title: String): String {
    val withoutFeatureCredit = title.replace(FEATURE_CREDIT_SUFFIX, "")
    val withoutBracketedMetadata = withoutFeatureCredit.replace(BRACKETED_AUDIO_METADATA, "")
    return withoutBracketedMetadata
        .replace(TRAILING_AUDIO_METADATA, "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { title.trim() }
}

internal fun mapSmartTubeSearchItems(items: List<MediaItem?>): List<YouTubeNativeSearchResult> =
    items
        .asSequence()
        .filterNotNull()
        .filter { isPlayableSmartTubeSearchResult(it.type, it.videoId) }
        .filterNot { it.isLive || it.isUpcoming || it.isShorts }
        .mapNotNull(::mapSmartTubeItem)
        .distinctBy(YouTubeNativeSearchResult::videoId)
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
    val viewCount = details.firstNotNullOfOrNull(::parseYouTubeViewCount) ?: 0L
    val candidateChannel =
        details.firstOrNull { detail ->
            parseYouTubeViewCount(detail) == null && !isDateOrTimeAgo(detail)
        }
    val channel =
        candidateChannel?.takeIf(String::isNotBlank)
            ?: author?.takeIf(String::isNotBlank).orEmpty()
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

private val YOUTUBE_VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
private val DETAIL_SEPARATOR = Regex("[•·]")
private val FEATURE_CREDIT_SUFFIX =
    Regex("""\s*(?:[\[(]\s*)?(?:feat(?:uring)?|ft)\.?\s+.*$""", RegexOption.IGNORE_CASE)
private val BRACKETED_AUDIO_METADATA =
    Regex(
        """\s*[\[(][^)\]]*(?:remaster(?:ed)?|album version|single version|radio edit|explicit|clean|mono|stereo|bonus track|original mix)[^)\]]*[)\]]""",
        RegexOption.IGNORE_CASE,
    )
private val TRAILING_AUDIO_METADATA =
    Regex(
        """\s*[-–—]\s*(?:\d{4}\s*)?(?:remaster(?:ed)?|album version|single version|radio edit|explicit|clean|mono|stereo|bonus track|original mix).*$""",
        RegexOption.IGNORE_CASE,
    )

private fun isDateOrTimeAgo(text: String): Boolean {
    val lower = text.lowercase(Locale.ROOT)
    return DATE_TIME_MARKERS.any(lower::contains) || Regex("""\b(19|20)\d{2}\b""").containsMatchIn(lower)
}

private val DATE_TIME_MARKERS =
    listOf(
        "ago",
        "vor",
        "year",
        "month",
        "week",
        "day",
        "hour",
        "minute",
        "sec",
        "yr",
        "mo",
        "wk",
        "hr",
        "min",
        "il y a",
        "hace",
    )
private val VIEW_MARKERS = listOf("view", "aufruf", "vue", "visualiz", "watched")
private val THOUSAND_MARKERS = listOf("k view", "k aufruf", "tsd")
private val MILLION_MARKERS = listOf("m view", "m aufruf", "mio", "million")
private val BILLION_MARKERS = listOf("b view", "b aufruf", "mrd", "billion", "milliard")
private const val MAX_SEARCH_PAGES = 3
private const val OFFICIAL_VIDEO_QUERY = "official music video"
