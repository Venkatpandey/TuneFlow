package com.tuneflow.feature.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRankingTest {
    private val query =
        VideoTrackQuery(
            trackId = "song-1",
            title = "Enjoy the Silence",
            artist = "Depeche Mode",
            album = "Violator",
            durationMs = 250_000L,
            regionCode = "DE",
            languageCode = "en",
        )

    @Test
    fun exactOfficialMusicVideoRanksAboveCover() {
        val ranked =
            VideoCandidateRanker.rank(
                query,
                listOf(
                    candidate("cover", "Enjoy the Silence cover", "Bedroom Music", 248_000L),
                    candidate("official", "Depeche Mode - Enjoy the Silence (Official Video)", "Depeche Mode", 251_000L),
                ),
            )

        assertEquals("official", ranked.first().videoId)
        assertTrue(ranked.first().score >= VideoCandidateRanker.AUTOPLAY_THRESHOLD)
    }

    @Test
    fun unwantedVariantIsPenalizedUnlessTrackRequestsIt() {
        val live = candidate("live", "Depeche Mode - Enjoy the Silence live", "Concert Archive", 250_000L)
        val studioScore = VideoCandidateRanker.score(query, live)
        val requestedLiveScore = VideoCandidateRanker.score(query.copy(title = "Enjoy the Silence live"), live)

        assertTrue(requestedLiveScore > studioScore)
    }

    @Test
    fun closeScoresRequirePicker() {
        val ranked =
            VideoCandidateRanker.rank(
                query,
                listOf(
                    candidate("a", "Depeche Mode Enjoy the Silence official video", "Label A", 250_000L),
                    candidate("b", "Depeche Mode Enjoy the Silence official video", "Label B", 250_000L),
                ),
            )

        assertFalse(VideoCandidateRanker.shouldAutoplay(ranked))
        assertEquals("a", ranked.first().videoId)
    }

    private fun candidate(
        id: String,
        title: String,
        publisher: String,
        durationMs: Long,
    ) = VideoCandidate(
        providerId = VideoProviderId.YouTube,
        videoId = id,
        title = title,
        publisher = publisher,
        thumbnailUrl = null,
        durationMs = durationMs,
        musicCategory = true,
    )
}
