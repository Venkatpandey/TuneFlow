package com.tuneflow.feature.browse

import com.tuneflow.core.network.TrackSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteNavigationTest {
    private val tracks = listOf(track("one"), track("two"), track("three"))

    @Test
    fun removalFocus_prefersNextTrack() {
        assertEquals("three", focusAfterFavoriteRemoval(tracks, "two"))
    }

    @Test
    fun removalFocus_usesPreviousTrackAtEnd() {
        assertEquals("two", focusAfterFavoriteRemoval(tracks, "three"))
    }

    @Test
    fun removalFocus_isEmptyForOnlyTrack() {
        assertNull(focusAfterFavoriteRemoval(listOf(track("only")), "only"))
    }

    private fun track(id: String): TrackSummary =
        TrackSummary(
            id = id,
            title = id,
            artist = "Artist",
            album = "Album",
            durationSec = 180,
            coverArtId = null,
            isFavorite = true,
        )
}
