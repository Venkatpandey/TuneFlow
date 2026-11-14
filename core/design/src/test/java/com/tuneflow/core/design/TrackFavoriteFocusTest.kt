package com.tuneflow.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackFavoriteFocusTest {
    @Test
    fun rightFromRow_movesToFavoriteButton() {
        assertEquals(
            TrackRowFocusTarget.FavoriteButton,
            trackRowFocusDestination(TrackRowFocusTarget.RowBody, HorizontalFocusDirection.Right),
        )
    }

    @Test
    fun leftFromFavoriteButton_movesToRow() {
        assertEquals(
            TrackRowFocusTarget.RowBody,
            trackRowFocusDestination(TrackRowFocusTarget.FavoriteButton, HorizontalFocusDirection.Left),
        )
    }

    @Test
    fun outerDirections_remainAvailableToScreenNavigation() {
        assertNull(trackRowFocusDestination(TrackRowFocusTarget.RowBody, HorizontalFocusDirection.Left))
        assertNull(trackRowFocusDestination(TrackRowFocusTarget.FavoriteButton, HorizontalFocusDirection.Right))
    }
}
