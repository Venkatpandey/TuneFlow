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

    @Suppress("CyclomaticComplexMethod")
    fun score(
        query: VideoTrackQuery,
        candidate: VideoCandidate,
    ): Double {
        val trackTitle = normalizeVideoText(query.title)
        val trackArtist = normalizeVideoText(query.artist)
        val candidateTitle = normalizeVideoText(candidate.title)
        val candidatePublisher = normalizeVideoText(candidate.publisher)
        if (trackTitle.isBlank() || trackArtist.isBlank()) return 0.0

        var matchScore = 0.0
        matchScore += if (candidateTitle.contains(trackTitle)) 0.38 else tokenSimilarity(trackTitle, candidateTitle) * 0.34
        matchScore +=
            when {
                publisherMatchesArtist(trackArtist, candidatePublisher) -> 0.34
                candidateTitle.contains(trackArtist) -> 0.26
                else ->
                    max(
                        tokenSimilarity(trackArtist, candidateTitle),
                        tokenSimilarity(trackArtist, candidatePublisher),
                    ) * 0.22
            }

        if (query.durationMs > 0L && candidate.durationMs > 0L) {
            val tolerance = max(DURATION_TOLERANCE_MS, (query.durationMs * DURATION_TOLERANCE_RATIO).toLong())
            val difference = abs(query.durationMs - candidate.durationMs)
            matchScore += if (difference <= tolerance) 0.18 else -0.16
        }
        if (candidate.musicCategory) matchScore += 0.08
        if (candidateTitle.contains("official music video") || candidateTitle.contains("official video")) {
            matchScore += 0.04
        }

        unwantedTerms.forEach { term ->
            if (candidateTitle.contains(term) && !trackTitle.contains(term)) matchScore -= 0.30
        }
        variants.forEach { variant ->
            if (candidateTitle.contains(variant) && !trackTitle.contains(variant)) matchScore -= 0.24
        }

        val popularityScore =
            (log10(candidate.viewCount.coerceAtLeast(0L).toDouble() + 1.0) / MAX_VIEW_COUNT_LOG10)
                .coerceIn(0.0, 1.0)
        return (
            matchScore.coerceIn(0.0, 1.0) * MATCH_WEIGHT +
                popularityScore * POPULARITY_WEIGHT
        ).coerceIn(0.0, 1.0)
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
        artist: String,
        publisher: String,
    ): Boolean {
        if (publisher.contains(artist)) return true
        val compactArtist = artist.replace(" ", "")
        val compactPublisher = publisher.replace(" ", "")
        return compactArtist.length >= MIN_COMPACT_ARTIST_LENGTH && compactPublisher.startsWith(compactArtist)
    }

    private const val DURATION_TOLERANCE_MS = 20_000L
    private const val DURATION_TOLERANCE_RATIO = 0.10
    private const val MIN_AUTOPLAY_MARGIN = 0.08
    private const val MIN_COMPACT_ARTIST_LENGTH = 4
    private const val MATCH_WEIGHT = 0.94
    private const val POPULARITY_WEIGHT = 0.06
    private const val MAX_VIEW_COUNT_LOG10 = 10.5
}

internal fun normalizeVideoText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
