package com.tuneflow.tv

import com.tuneflow.feature.playback.LyricLine
import com.tuneflow.feature.playback.Lyrics
import com.tuneflow.feature.playback.LyricsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackScreensaverPresentationTest {
    @Test
    fun lyricsOnlyResolveForCurrentTrack() {
        val lyrics = Lyrics(synchronized = false, lines = listOf(LyricLine("Line")))
        val state = LyricsUiState.Available(trackId = "one", lyrics = lyrics)

        assertEquals(lyrics, resolveScreensaverLyrics(state, "one"))
        assertNull(resolveScreensaverLyrics(state, "two"))
        assertNull(resolveScreensaverLyrics(LyricsUiState.Empty("one"), "one"))
    }
}
