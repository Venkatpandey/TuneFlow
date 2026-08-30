package com.tuneflow.feature.video

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeVideoBehaviorTest {
    @Test
    fun filtersDuplicatesAndUnwantedVariantsUnlessTrackRequestsThem() {
        val query = VideoTrackQuery("1", "Song", "Artist", "Album", 100L, null, null)
        val candidates =
            listOf(
                candidate("ok", "Artist Song official video"),
                candidate("ok", "duplicate"),
                candidate("discover", "Discover Artist Song"),
                candidate("cover", "Artist Song cover"),
                candidate("live", "Artist Song live"),
                candidate("reaction", "Artist Song reaction"),
            )
        assertEquals(listOf("ok", "discover"), filterUnwantedVideoCandidates(query, candidates).map(VideoCandidate::videoId))

        val liveQuery = query.copy(title = "Song Live")
        assertTrue(filterUnwantedVideoCandidates(liveQuery, listOf(candidates[4])).isNotEmpty())
    }

    @Test
    fun dpadBehaviorUsesTwoStageBackAndHiddenSeeking() {
        assertEquals(NativeControlAction.HideControls, nativeControlAction(KeyEvent.KEYCODE_BACK, true))
        assertEquals(NativeControlAction.ExitFullscreen, nativeControlAction(KeyEvent.KEYCODE_BACK, false))
        assertEquals(NativeControlAction.ShowControls, nativeControlAction(KeyEvent.KEYCODE_DPAD_CENTER, false))
        assertEquals(NativeControlAction.SeekBack, nativeControlAction(KeyEvent.KEYCODE_DPAD_LEFT, false))
        assertEquals(NativeControlAction.SeekForward, nativeControlAction(KeyEvent.KEYCODE_DPAD_RIGHT, false))
        assertEquals(NativeControlAction.None, nativeControlAction(KeyEvent.KEYCODE_DPAD_LEFT, true))
    }

    @Test
    fun autoplayRequiresConfidenceAndMargin() {
        val candidates = listOf(candidate("top", "Artist Song", 0.90), candidate("other", "Artist Song", 0.70))
        assertTrue(VideoCandidateRanker.shouldAutoplay(candidates))
        assertFalse(VideoCandidateRanker.shouldAutoplay(candidates.map { it.copy(score = 0.80) }))
    }

    private fun candidate(
        id: String,
        title: String,
        score: Double = 0.0,
    ) = VideoCandidate(
        providerId = VideoProviderId.YouTube,
        videoId = id,
        title = title,
        publisher = "Artist",
        thumbnailUrl = null,
        durationMs = 100L,
        musicCategory = true,
        score = score,
    )
}
