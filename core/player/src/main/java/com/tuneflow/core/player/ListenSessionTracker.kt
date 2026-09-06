package com.tuneflow.core.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LISTEN_THRESHOLD_CAP_MS = 60 * 1000L

data class ScrobbleSubmission(
    val accountKey: String,
    val trackId: String,
    val startedAtEpochMs: Long,
)

interface ScrobbleReporter {
    fun currentAccountKey(): String?

    suspend fun scrobble(submission: ScrobbleSubmission)
}

object NoOpScrobbleReporter : ScrobbleReporter {
    override fun currentAccountKey(): String? = null

    override suspend fun scrobble(submission: ScrobbleSubmission) = Unit
}

class ListenSessionTracker(
    private val scope: CoroutineScope,
    reporter: ScrobbleReporter = NoOpScrobbleReporter,
    private val monotonicClockMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val wallClockMs: () -> Long = System::currentTimeMillis,
    private val onReportFailure: (Throwable) -> Unit = {},
) {
    private data class Session(
        val sequence: Long,
        val accountKey: String,
        val trackId: String,
        val startedAtEpochMs: Long,
        var durationMs: Long,
        var listenedMs: Long = 0L,
        var playingSinceMs: Long? = null,
        var submitted: Boolean = false,
    )

    private var reporter = reporter
    private var session: Session? = null
    private var thresholdJob: Job? = null
    private var nextSequence = 0L

    fun setReporter(reporter: ScrobbleReporter) {
        this.reporter = reporter
    }

    fun onPlaying(
        trackId: String,
        durationMs: Long,
    ) {
        val accountKey =
            reporter.currentAccountKey() ?: run {
                reset()
                return
            }
        val active =
            session
                ?.takeIf { it.trackId == trackId && it.accountKey == accountKey }
                ?: startSession(accountKey, trackId, durationMs)
        val previousThreshold = active.thresholdMs()
        if (durationMs > 0L) active.durationMs = durationMs
        if (active.submitted) return
        if (active.playingSinceMs == null) active.playingSinceMs = monotonicClockMs()
        if (previousThreshold != active.thresholdMs() || thresholdJob == null) {
            scheduleThreshold(active)
        }
    }

    fun onPaused(trackId: String) {
        val active = session?.takeIf { it.trackId == trackId } ?: return
        stopClock(active)
    }

    fun onEnded(trackId: String? = null) {
        val active = session ?: return
        if (trackId != null && active.trackId != trackId) return
        stopClock(active)
        if (!active.submitted && (active.durationMs <= 0L || active.listenedMs >= active.thresholdMs())) {
            qualify(active)
        }
        session = null
    }

    fun onMediaChanged(
        trackId: String?,
        forceNewSession: Boolean = false,
    ) {
        val active = session ?: return
        if (forceNewSession || active.trackId != trackId) reset()
    }

    fun reset() {
        thresholdJob?.cancel()
        thresholdJob = null
        session = null
    }

    private fun startSession(
        accountKey: String,
        trackId: String,
        durationMs: Long,
    ): Session {
        reset()
        return Session(
            sequence = ++nextSequence,
            accountKey = accountKey,
            trackId = trackId,
            startedAtEpochMs = wallClockMs(),
            durationMs = durationMs.coerceAtLeast(0L),
        ).also { session = it }
    }

    private fun stopClock(active: Session) {
        val playingSinceMs = active.playingSinceMs ?: return
        active.listenedMs += (monotonicClockMs() - playingSinceMs).coerceAtLeast(0L)
        active.playingSinceMs = null
        thresholdJob?.cancel()
        thresholdJob = null
    }

    private fun scheduleThreshold(active: Session) {
        thresholdJob?.cancel()
        val elapsed = active.listenedMs + active.currentPlayingTimeMs()
        val remainingMs = (active.thresholdMs() - elapsed).coerceAtLeast(0L)
        val sequence = active.sequence
        thresholdJob =
            scope.launch {
                delay(remainingMs)
                val current = session?.takeIf { it.sequence == sequence } ?: return@launch
                if (current.playingSinceMs == null) return@launch
                stopClock(current)
                if (current.listenedMs >= current.thresholdMs()) qualify(current)
            }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun qualify(active: Session) {
        if (active.submitted) return
        if (reporter.currentAccountKey() != active.accountKey) {
            reset()
            return
        }
        active.submitted = true
        thresholdJob?.cancel()
        thresholdJob = null
        val submission =
            ScrobbleSubmission(
                accountKey = active.accountKey,
                trackId = active.trackId,
                startedAtEpochMs = active.startedAtEpochMs,
            )
        scope.launch {
            try {
                reporter.scrobble(submission)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onReportFailure(error)
            }
        }
    }

    private fun Session.currentPlayingTimeMs(): Long = playingSinceMs?.let { (monotonicClockMs() - it).coerceAtLeast(0L) } ?: 0L

    private fun Session.thresholdMs(): Long =
        if (durationMs > 0L) {
            (durationMs / 4L).coerceAtLeast(1L).coerceAtMost(LISTEN_THRESHOLD_CAP_MS)
        } else {
            LISTEN_THRESHOLD_CAP_MS
        }
}
