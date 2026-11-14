package com.tuneflow.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistUsageStoreTest {
    @Test
    fun withMostRecentPlaylist_movesUsedPlaylistToFront() {
        val updated =
            listOf("playlist-3", "playlist-2", "playlist-1")
                .withMostRecentPlaylist("playlist-2")

        assertEquals(listOf("playlist-2", "playlist-3", "playlist-1"), updated)
    }

    @Test
    fun withMostRecentPlaylist_keepsAllPreviouslyUsedPlaylists() {
        val updated =
            listOf("playlist-2", "playlist-1")
                .withMostRecentPlaylist("playlist-3")

        assertEquals(listOf("playlist-3", "playlist-2", "playlist-1"), updated)
    }
}
