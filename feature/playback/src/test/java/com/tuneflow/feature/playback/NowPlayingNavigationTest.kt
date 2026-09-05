package com.tuneflow.feature.playback

import android.view.KeyEvent
import org.junit.Assert.assertEquals
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
    fun playlistContextLabel_onlyShowsNamedPlaylist() {
        assertEquals("Playlist • Evening Mix", playlistContextLabel(" Evening Mix "))
        assertEquals(null, playlistContextLabel("  "))
        assertEquals(null, playlistContextLabel(null))
    }
}
