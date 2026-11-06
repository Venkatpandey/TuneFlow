package com.tuneflow.feature.playback

import android.view.KeyEvent
import com.tuneflow.feature.video.VideoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingNavigationTest {
    @Test
    fun backWithOpenQueue_closesQueue() {
        val action =
            resolveNowPlayingEscapeAction(
                activePanel = NowPlayingPanel.TrackList,
                isKeyDown = true,
                keyCode = KeyEvent.KEYCODE_BACK,
            )

        assertEquals(NowPlayingEscapeAction.ClosePanel, action)
    }

    @Test
    fun backWithClosedQueue_propagatesToShell() {
        val action =
            resolveNowPlayingEscapeAction(
                activePanel = NowPlayingPanel.None,
                isKeyDown = true,
                keyCode = KeyEvent.KEYCODE_BACK,
            )

        assertEquals(NowPlayingEscapeAction.Propagate, action)
    }

    @Test
    fun panels_areMutuallyExclusiveAndActiveButtonClosesPanel() {
        assertEquals(
            NowPlayingPanel.Lyrics,
            toggleNowPlayingPanel(NowPlayingPanel.TrackList, NowPlayingPanel.Lyrics),
        )
        assertEquals(
            NowPlayingPanel.None,
            toggleNowPlayingPanel(NowPlayingPanel.Lyrics, NowPlayingPanel.Lyrics),
        )
    }

    @Test
    fun backRestoresFocusToButtonThatOpenedPanel() {
        assertEquals(
            PanelFocusTarget.QueueButton,
            resolvePanelFocusTarget(NowPlayingPanel.TrackList, lyricsAvailable = true),
        )
        assertEquals(
            PanelFocusTarget.LyricsButton,
            resolvePanelFocusTarget(NowPlayingPanel.Lyrics, lyricsAvailable = true),
        )
        assertEquals(
            PanelFocusTarget.None,
            resolvePanelFocusTarget(NowPlayingPanel.Lyrics, lyricsAvailable = false),
        )
        assertEquals(
            PanelFocusTarget.VideoButton,
            resolvePanelFocusTarget(NowPlayingPanel.VideoCandidates, lyricsAvailable = true),
        )
    }

    @Test
    fun queueExitTarget_matchesFocusedQueueRegion() {
        assertEquals(QueueExitTarget.StreamControls, resolveQueueExitTarget(focusedIndex = 1, itemCount = 6))
        assertEquals(QueueExitTarget.TransportControls, resolveQueueExitTarget(focusedIndex = 5, itemCount = 6))
    }

    @Test
    fun openingQueue_focusesTopTrack() {
        assertEquals(0, resolveQueuePanelFocusIndex(itemCount = 6))
        assertEquals(-1, resolveQueuePanelFocusIndex(itemCount = 0))
    }

    @Test
    fun calculateCenterScrollDelta_centersItemCorrectly() {
        // Item is above center: delta is negative (scroll up)
        assertEquals(-160, calculateCenterScrollDelta(itemOffset = 100, itemSize = 80, viewportHeight = 600))
        // Item is exactly centered: delta is 0
        assertEquals(0, calculateCenterScrollDelta(itemOffset = 260, itemSize = 80, viewportHeight = 600))
        // Item is below center: delta is positive (scroll down)
        assertEquals(160, calculateCenterScrollDelta(itemOffset = 420, itemSize = 80, viewportHeight = 600))
        // Invalid or zero viewport height
        assertEquals(0, calculateCenterScrollDelta(itemOffset = 100, itemSize = 80, viewportHeight = 0))
        assertEquals(0, calculateCenterScrollDelta(itemOffset = 100, itemSize = 80, viewportHeight = -100))
    }

    @Test
    fun playlistContextLabel_onlyShowsNamedPlaylist() {
        assertEquals("Playlist • Evening Mix", playlistContextLabel(" Evening Mix "))
        assertEquals(null, playlistContextLabel("  "))
        assertEquals(null, playlistContextLabel(null))
    }

    @Test
    fun videoCandidatePanel_staysVisibleWhileSearchIsRunning() {
        assertTrue(VideoUiState.Searching("track-1", 1L).showsVideoCandidatePanel())
        assertTrue(VideoUiState.Candidates("track-1", 1L, emptyList()).showsVideoCandidatePanel())
        assertFalse(VideoUiState.Idle.showsVideoCandidatePanel())
    }
}
