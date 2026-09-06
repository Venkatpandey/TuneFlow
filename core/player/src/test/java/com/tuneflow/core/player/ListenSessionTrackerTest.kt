package com.tuneflow.core.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListenSessionTrackerTest {
    @Test
    fun `submits once after quarter of a normal track`() =
        runTest {
            val reporter = RecordingScrobbleReporter()
            val tracker = tracker(reporter)

            tracker.onPlaying("track-id", 180_000L)
            advanceTimeBy(44_999L)
            runCurrent()
            assertTrue(reporter.submissions.isEmpty())

            advanceTimeBy(1L)
            runCurrent()
            tracker.onPlaying("track-id", 180_000L)
            advanceTimeBy(45_000L)
            runCurrent()

            assertEquals(
                listOf(ScrobbleSubmission("server\u0000user", "track-id", 1_234_567L)),
                reporter.submissions,
            )
        }

    @Test
    fun `pause and buffering time do not qualify a listen`() =
        runTest {
            val reporter = RecordingScrobbleReporter()
            val tracker = tracker(reporter)

            tracker.onPlaying("track-id", 100_000L)
            advanceTimeBy(12_500L)
            tracker.onPaused("track-id")
            advanceTimeBy(100_000L)
            runCurrent()
            assertTrue(reporter.submissions.isEmpty())

            tracker.onPlaying("track-id", 100_000L)
            advanceTimeBy(12_500L)
            runCurrent()

            assertEquals(1, reporter.submissions.size)
        }

    @Test
    fun `one minute cap applies to long media`() =
        runTest {
            val reporter = RecordingScrobbleReporter()
            val tracker = tracker(reporter)

            tracker.onPlaying("long-track", 900_000L)
            advanceTimeBy(59_999L)
            runCurrent()
            assertTrue(reporter.submissions.isEmpty())

            advanceTimeBy(1L)
            runCurrent()

            assertEquals(1, reporter.submissions.size)
        }

    @Test
    fun `unknown duration qualifies on natural end or one minute`() =
        runTest {
            val reporter = RecordingScrobbleReporter()
            val tracker = tracker(reporter)

            tracker.onPlaying("unknown-track", 0L)
            advanceTimeBy(1_000L)
            tracker.onEnded()
            runCurrent()

            assertEquals(1, reporter.submissions.size)

            tracker.onPlaying("unknown-timer", 0L)
            advanceTimeBy(59_999L)
            runCurrent()
            assertEquals(1, reporter.submissions.size)

            advanceTimeBy(1L)
            runCurrent()
            assertEquals(2, reporter.submissions.size)
        }

    @Test
    fun `early end reset and account change do not submit`() =
        runTest {
            val reporter = RecordingScrobbleReporter()
            val tracker = tracker(reporter)

            tracker.onPlaying("known-track", 180_000L)
            advanceTimeBy(10_000L)
            tracker.onEnded("known-track")
            tracker.onPlaying("skipped-track", 180_000L)
            advanceTimeBy(10_000L)
            tracker.reset()
            tracker.onPlaying("changed-account-track", 2_000L)
            reporter.accountKey = "server\u0000other"
            advanceTimeBy(1_000L)
            runCurrent()

            assertTrue(reporter.submissions.isEmpty())
        }

    @Test
    fun `same media fallback stays in session but repeat starts a new one`() =
        runTest {
            val reporter = RecordingScrobbleReporter()
            val tracker = tracker(reporter)

            tracker.onPlaying("track-id", 100_000L)
            advanceTimeBy(12_500L)
            tracker.onMediaChanged("track-id")
            advanceTimeBy(12_500L)
            runCurrent()
            assertEquals(1, reporter.submissions.size)

            tracker.onMediaChanged("track-id", forceNewSession = true)
            tracker.onPlaying("track-id", 100_000L)
            advanceTimeBy(25_000L)
            runCurrent()

            assertEquals(2, reporter.submissions.size)
        }

    @Test
    fun `report failure is isolated and never retried`() =
        runTest {
            var attempts = 0
            var failures = 0
            val reporter =
                object : ScrobbleReporter {
                    override fun currentAccountKey(): String = "server\u0000user"

                    override suspend fun scrobble(submission: ScrobbleSubmission) {
                        attempts += 1
                        error("offline")
                    }
                }
            val tracker =
                ListenSessionTracker(
                    scope = backgroundScope,
                    reporter = reporter,
                    monotonicClockMs = { testScheduler.currentTime },
                    wallClockMs = { 1_234_567L },
                    onReportFailure = { failures += 1 },
                )

            tracker.onPlaying("track-id", 2_000L)
            advanceTimeBy(1_000L)
            runCurrent()
            tracker.onPlaying("track-id", 2_000L)
            advanceTimeBy(2_000L)
            runCurrent()

            assertEquals(1, attempts)
            assertEquals(1, failures)
        }

    private fun kotlinx.coroutines.test.TestScope.tracker(reporter: RecordingScrobbleReporter) =
        ListenSessionTracker(
            scope = backgroundScope,
            reporter = reporter,
            monotonicClockMs = { testScheduler.currentTime },
            wallClockMs = { 1_234_567L },
        )
}

private class RecordingScrobbleReporter : ScrobbleReporter {
    var accountKey: String? = "server\u0000user"
    val submissions = mutableListOf<ScrobbleSubmission>()

    override fun currentAccountKey(): String? = accountKey

    override suspend fun scrobble(submission: ScrobbleSubmission) {
        submissions += submission
    }
}
