package com.tuneflow.feature.playback

import com.tuneflow.core.network.LegacyLyricsDto
import com.tuneflow.core.network.LyricsLineDto
import com.tuneflow.core.network.StructuredLyricsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {
    @Test
    fun selection_prefersMainSynchronizedThenMainUnsynchronized() {
        val translation = structured(kind = "translation", synced = true, text = "Translation", startMs = 10L)
        val mainUnsynced = structured(kind = "main", synced = false, text = "Main plain")
        val mainSynced = structured(kind = "main", synced = true, text = "Main timed", startMs = 20L)

        val result = selectStructuredLyrics(listOf(translation, mainUnsynced, mainSynced))

        assertEquals("Main timed", (result as LyricsLoadResult.Available).lyrics.lines.single().text)
    }

    @Test
    fun selection_usesMainUnsynchronizedBeforeFirstNonMainResult() {
        val result =
            selectStructuredLyrics(
                listOf(
                    structured(kind = "translation", synced = true, text = "Translation", startMs = 10L),
                    structured(kind = null, synced = false, text = "Default main"),
                ),
            )

        assertEquals("Default main", (result as LyricsLoadResult.Available).lyrics.lines.single().text)
        assertEquals(false, result.lyrics.synchronized)
    }

    @Test
    fun synchronizedMalformedLines_haveDistinctParsingResult() {
        val result = selectStructuredLyrics(listOf(structured(kind = "main", synced = true, text = "Missing time")))

        assertTrue(result is LyricsLoadResult.ParsingFailure)
    }

    @Test
    fun emptyResponses_haveDistinctEmptyResult() {
        assertEquals(LyricsLoadResult.Empty, selectStructuredLyrics(emptyList()))
        assertEquals(LyricsLoadResult.Empty, parseLegacyLyrics(LegacyLyricsDto(value = "  \n\n")))
    }

    @Test
    fun legacyLyrics_areUnsynchronizedLines() {
        val result = parseLegacyLyrics(LegacyLyricsDto(value = "First\n\nSecond")) as LyricsLoadResult.Available

        assertEquals(false, result.lyrics.synchronized)
        assertEquals(listOf("First", "Second"), result.lyrics.lines.map { it.text })
    }

    @Test
    fun activeLine_honorsBoundariesOffsetAndBackwardSeek() {
        val lyrics =
            Lyrics(
                synchronized = true,
                offsetMs = 100L,
                lines =
                    listOf(
                        LyricLine("One", 1_000L),
                        LyricLine("Two", 2_000L),
                        LyricLine("Three", 3_000L),
                    ),
            )

        assertNull(resolveActiveLyricLine(lyrics, 899L))
        assertEquals(0, resolveActiveLyricLine(lyrics, 900L))
        assertEquals(2, resolveActiveLyricLine(lyrics, 2_900L))
        assertEquals(0, resolveActiveLyricLine(lyrics, 1_200L))
    }

    @Test
    fun sharedTimedResolver_returnsEquivalentSelectionForBothLyricsViews() {
        val lyrics =
            Lyrics(
                synchronized = true,
                lines =
                    listOf(
                        LyricLine("One", 1_000L),
                        LyricLine("Two", 2_000L),
                        LyricLine("Three", 3_000L),
                    ),
            )
        val positions = listOf(0L, 1_000L, 2_500L, 4_000L, 1_200L)

        val nowPlayingLines = positions.map { resolveActiveLyricLine(lyrics, it) }
        val screensaverLines = positions.map { resolveActiveLyricLine(lyrics, it) }

        assertEquals(nowPlayingLines, screensaverLines)
        assertEquals(listOf(null, 0, 1, 2, 0), nowPlayingLines)
    }

    @Test
    fun unsynchronizedLyrics_neverReceiveFalseHighlighting() {
        val lyrics =
            Lyrics(
                synchronized = false,
                lines = listOf(LyricLine("One"), LyricLine("Two")),
            )

        assertNull(resolveActiveLyricLine(lyrics, 0L))
        assertNull(resolveActiveLyricLine(lyrics, 50_000L))
    }

    private fun structured(
        kind: String?,
        synced: Boolean,
        text: String,
        startMs: Long? = null,
    ) = StructuredLyricsDto(
        kind = kind,
        synced = synced,
        line = listOf(LyricsLineDto(start = startMs, value = text)),
    )
}
