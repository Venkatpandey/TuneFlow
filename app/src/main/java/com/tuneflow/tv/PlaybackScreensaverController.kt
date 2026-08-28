package com.tuneflow.tv

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val PLAYBACK_SCREENSAVER_TIMEOUT_MS = 60_000L

internal fun interface MonotonicClock {
    fun nowMs(): Long
}

internal object SystemMonotonicClock : MonotonicClock {
    override fun nowMs(): Long = SystemClock.elapsedRealtime()
}

internal data class PlaybackScreensaverState(
    val lastUserActivityMs: Long,
    val playbackEligible: Boolean = false,
    val active: Boolean = false,
)

internal sealed interface PlaybackScreensaverEvent {
    data class UserActivity(val category: UserInputCategory) : PlaybackScreensaverEvent

    data class PlaybackEligibilityChanged(val eligible: Boolean) : PlaybackScreensaverEvent

    data object DeadlineReached : PlaybackScreensaverEvent
}

internal fun reducePlaybackScreensaverState(
    state: PlaybackScreensaverState,
    event: PlaybackScreensaverEvent,
    nowMs: Long,
    timeoutMs: Long = PLAYBACK_SCREENSAVER_TIMEOUT_MS,
): PlaybackScreensaverState =
    when (event) {
        is PlaybackScreensaverEvent.UserActivity ->
            state.copy(lastUserActivityMs = nowMs, active = false)
        is PlaybackScreensaverEvent.PlaybackEligibilityChanged -> {
            when {
                !event.eligible ->
                    state.copy(
                        lastUserActivityMs = nowMs,
                        playbackEligible = false,
                        active = false,
                    )
                !state.playbackEligible ->
                    state.copy(
                        lastUserActivityMs = nowMs,
                        playbackEligible = true,
                        active = false,
                    )
                else -> state
            }
        }
        PlaybackScreensaverEvent.DeadlineReached -> {
            val idleLongEnough = nowMs - state.lastUserActivityMs >= timeoutMs
            state.copy(active = state.playbackEligible && idleLongEnough)
        }
    }

internal class PlaybackScreensaverController(
    private val clock: MonotonicClock = SystemMonotonicClock,
    private val timeoutMs: Long = PLAYBACK_SCREENSAVER_TIMEOUT_MS,
) {
    private val _state = MutableStateFlow(PlaybackScreensaverState(lastUserActivityMs = clock.nowMs()))
    val state: StateFlow<PlaybackScreensaverState> = _state.asStateFlow()

    fun onUserActivity(category: UserInputCategory) {
        update(PlaybackScreensaverEvent.UserActivity(category))
    }

    fun onPlaybackEligibilityChanged(eligible: Boolean) {
        update(PlaybackScreensaverEvent.PlaybackEligibilityChanged(eligible))
    }

    fun onDeadlineReached() {
        update(PlaybackScreensaverEvent.DeadlineReached)
    }

    fun remainingDelayMs(): Long = (timeoutMs - (clock.nowMs() - state.value.lastUserActivityMs)).coerceAtLeast(0L)

    private fun update(event: PlaybackScreensaverEvent) {
        _state.value = reducePlaybackScreensaverState(_state.value, event, clock.nowMs(), timeoutMs)
    }
}
