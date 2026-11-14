package com.tuneflow.feature.video

import com.tuneflow.core.player.ListenSessionTracker
import com.tuneflow.core.player.ScrobbleReporter
import kotlinx.coroutines.CoroutineScope

internal class VideoListenSession(
    scope: CoroutineScope,
    reporter: ScrobbleReporter,
    diagnostic: (String) -> Unit,
) {
    private val tracker =
        ListenSessionTracker(
            scope = scope,
            reporter = reporter,
            onReportFailure = { diagnostic("Navidrome video scrobble failed; playback continues.") },
        )

    fun onPlayerState(
        state: NativeVideoPlayerState,
        fallbackDurationMs: Long,
    ) {
        when (state) {
            is NativeVideoPlayerState.Playing ->
                tracker.onPlaying(
                    state.session.trackId,
                    state.durationMs.takeIf { it > 0L } ?: fallbackDurationMs,
                )
            is NativeVideoPlayerState.Ready -> tracker.onPaused(state.session.trackId)
            is NativeVideoPlayerState.Paused -> tracker.onPaused(state.session.trackId)
            is NativeVideoPlayerState.Buffering -> tracker.onPaused(state.session.trackId)
            is NativeVideoPlayerState.Loading -> tracker.onPaused(state.session.trackId)
            is NativeVideoPlayerState.Ended -> tracker.onEnded(state.session.trackId)
            is NativeVideoPlayerState.Error -> tracker.reset()
            NativeVideoPlayerState.Idle -> Unit
        }
    }

    fun pause(trackId: String?) {
        trackId?.let(tracker::onPaused)
    }

    fun reset() = tracker.reset()
}
