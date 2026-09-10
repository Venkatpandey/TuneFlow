@file:Suppress("MatchingDeclarationName")

package com.tuneflow.feature.video

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

object VideoCandidateRanker {
    const val AUTOPLAY_THRESHOLD = 0.78

    private val unwantedTerms =
        setOf(
            "cover",
            "karaoke",
            "reaction",
            "tutorial",
            "fan edit",
            "lyric video",
            "lyrics video",
            "sped up",
            "slowed",
            "shorts",
        )
    private val variants = setOf("live", "remix", "acoustic", "instrumental")

    fun rank(
        query: VideoTrackQuery,
        candidates: List<VideoCandidate>,
    ): List<VideoCandidate> =
        candidates
            .map { it.copy(score = score(query, it)) }
            .filter { it.score > 0.0 }
            .sortedWith(
                compareByDescending<VideoCandidate>(VideoCandidate::score)
                    .thenByDescending(VideoCandidate::viewCount)
                    .thenBy(VideoCandidate::videoId),
            )

    fun shouldAutoplay(candidates: List<VideoCandidate>): Boolean {
        val top = candidates.firstOrNull() ?: return false
        val runnerUp = candidates.getOrNull(1)
        return top.score >= AUTOPLAY_THRESHOLD &&
            (runnerUp == null || top.score - runnerUp.score >= MIN_AUTOPLAY_MARGIN)
    }

    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    fun score(
        query: VideoTrackQuery,
        candidate: VideoCandidate,
    ): Double {
        val trackTitle = normalizeTrackTitleForMatching(query.title)
        val artistAliases = videoArtistAliases(query.artist)
        val candidateTitle = normalizeVideoText(candidate.title)
        val candidatePublisher = normalizeVideoText(candidate.publisher)
        if (trackTitle.isBlank() || artistAliases.isEmpty()) return 0.0

        val titleSimilarity = tokenSimilarity(trackTitle, candidateTitle)
        if (titleSimilarity < MIN_TITLE_MATCH) return 0.0

        val publisherMatch = publisherMatchesArtist(artistAliases, candidatePublisher)
        val titleArtistSimilarity = artistAliases.maxOf { tokenSimilarity(it, candidateTitle) }
        if (!publisherMatch && titleArtistSimilarity < MIN_ARTIST_MATCH) return 0.0

        var matchScore = 0.0
        matchScore += if (candidateTitle.containsPhrase(trackTitle)) 0.38 else titleSimilarity * 0.38
        matchScore +=
            when {
                publisherMatch -> 0.30
                artistAliases.any(candidateTitle::containsPhrase) -> 0.18
                else -> titleArtistSimilarity * 0.16
            }

        if (query.durationMs > 0L && candidate.durationMs > 0L) {
            val tolerance = max(DURATION_TOLERANCE_MS, (query.durationMs * DURATION_TOLERANCE_RATIO).toLong())
            val difference = abs(query.durationMs - candidate.durationMs)
            matchScore += if (difference <= tolerance) 0.05 else -0.08
        }
        if (candidate.musicCategory) matchScore += 0.03
        if (isOfficialVideoTitle(candidateTitle)) matchScore += 0.14

        unwantedTerms.forEach { term ->
            if (candidateTitle.containsPhrase(term) && !trackTitle.containsPhrase(term)) matchScore -= 0.30
        }
        variants.forEach { variant ->
            if (candidateTitle.containsPhrase(variant) && !trackTitle.containsPhrase(variant)) matchScore -= 0.24
        }

        val popularityScore =
            (log10(candidate.viewCount.coerceAtLeast(0L).toDouble() + 1.0) / MAX_VIEW_COUNT_LOG10)
                .coerceIn(0.0, 1.0)
        return (matchScore + popularityScore * POPULARITY_WEIGHT).coerceIn(0.0, 1.0)
    }

    private fun tokenSimilarity(
        expected: String,
        actual: String,
    ): Double {
        val expectedTokens = expected.split(' ').filter(String::isNotBlank).toSet()
        val actualTokens = actual.split(' ').filter(String::isNotBlank).toSet()
        if (expectedTokens.isEmpty() || actualTokens.isEmpty()) return 0.0
        return expectedTokens.intersect(actualTokens).size.toDouble() / expectedTokens.size.toDouble()
    }

    private fun publisherMatchesArtist(
        artistAliases: List<String>,
        publisher: String,
    ): Boolean {
        val compactPublisher =
            publisher
                .replace(" ", "")
                .replace("official", "")
                .replace("vevo", "")
                .replace("topic", "")
                .replace("music", "")
                .replace("channel", "")
        return artistAliases.any { artist ->
            val compactArtist = artist.replace(" ", "")
            compactArtist.length >= MIN_COMPACT_ARTIST_LENGTH &&
                (
                    compactPublisher == compactArtist ||
                        compactArtist.length >= MIN_PARTIAL_ARTIST_LENGTH && compactPublisher.contains(compactArtist)
                )
        }
    }

    private fun isOfficialVideoTitle(title: String): Boolean {
        val tokens = title.split(' ').toSet()
        return "official" in tokens && ("video" in tokens || "mv" in tokens)
    }

    private const val DURATION_TOLERANCE_MS = 20_000L
    private const val DURATION_TOLERANCE_RATIO = 0.10
    private const val MIN_AUTOPLAY_MARGIN = 0.08
    private const val MIN_COMPACT_ARTIST_LENGTH = 3
    private const val MIN_PARTIAL_ARTIST_LENGTH = 5
    private const val MIN_TITLE_MATCH = 0.60
    private const val MIN_ARTIST_MATCH = 0.60
    private const val POPULARITY_WEIGHT = 0.10
    private const val MAX_VIEW_COUNT_LOG10 = 10.5
}

internal fun normalizeVideoText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

internal fun normalizeTrackTitleForMatching(value: String): String {
    val withoutFeatureCredit = value.replace(FEATURE_CREDIT_SUFFIX, "")
    val withoutBracketedMetadata = withoutFeatureCredit.replace(BRACKETED_AUDIO_METADATA, "")
    return normalizeVideoText(withoutBracketedMetadata.replace(TRAILING_AUDIO_METADATA, ""))
        .ifBlank { normalizeVideoText(value) }
}

internal fun videoArtistAliases(value: String): List<String> {
    val fullCredit = normalizeVideoText(value)
    val individualArtists = value.split(ARTIST_CREDIT_SEPARATOR).map(::normalizeVideoText)
    return (listOf(fullCredit) + individualArtists)
        .filter { it.length >= MIN_ARTIST_ALIAS_LENGTH }
        .distinct()
}

internal fun filterUnwantedVideoCandidates(
    query: VideoTrackQuery,
    candidates: List<VideoCandidate>,
): List<VideoCandidate> {
    val requestedTitle = normalizeVideoText(query.title)
    return candidates.distinctBy(VideoCandidate::videoId).filter { candidate ->
        val candidateTitle = normalizeVideoText(candidate.title)
        HARD_EXCLUDED_TERMS.none { term -> candidateTitle.containsPhrase(term) && !requestedTitle.containsPhrase(term) } &&
            CONDITIONAL_VARIANTS.none { term ->
                candidateTitle.containsPhrase(term) && !requestedTitle.containsPhrase(term)
            }
    }
}

private fun String.containsPhrase(phrase: String): Boolean = " $this ".contains(" $phrase ")

private val HARD_EXCLUDED_TERMS = setOf("karaoke", "reaction", "cover", "shorts")
private val CONDITIONAL_VARIANTS = setOf("remix", "live", "acoustic")
private val ARTIST_CREDIT_SEPARATOR =
    Regex(
        """\s*(?:,|&|/|;|\bfeat(?:uring)?\.?\b|\bft\.?\b|\bx\b|\band\b)\s*""",
        RegexOption.IGNORE_CASE,
    )
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
private const val MIN_ARTIST_ALIAS_LENGTH = 3
