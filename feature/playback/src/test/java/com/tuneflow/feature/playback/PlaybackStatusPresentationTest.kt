package com.tuneflow.feature.playback

import com.tuneflow.feature.video.VideoCandidate
import com.tuneflow.feature.video.VideoPresentationMode
import com.tuneflow.feature.video.VideoTrackDetails
import com.tuneflow.feature.video.VideoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStatusPresentationTest {
    private val audioError = NowPlayingUiState(statusMessage = "Source error", canRetry = true)
    private val candidate = VideoCandidate("mappedvid01", "Video", "Artist", null, 180_000L, true)
    private val details = VideoTrackDetails("Track", "Artist", "Album")

    @Test
    fun videoLoadingPlayingAndPausedHideStandbyAudioError() {
        val loading = VideoUiState.Loading("track", 1L, candidate, details, VideoPresentationMode.Mini)
        val playing = VideoUiState.Playing("track", 1L, candidate, details, VideoPresentationMode.Mini, 1000L, 180_000L, true)

        assertNull(audioError.audioStatusMessage(loading))
        assertNull(audioError.audioStatusMessage(playing))
        assertNull(audioError.audioStatusMessage(playing.copy(isPlaying = false)))
    }

    @Test
    fun realAudioFailureRemainsVisibleWhenVideoIsNotActive() {
        assertEquals("Source error", audioError.audioStatusMessage(VideoUiState.Idle))
        assertEquals("Source error", audioError.audioStatusMessage(VideoUiState.Searching("track", 1L)))
    }
}
