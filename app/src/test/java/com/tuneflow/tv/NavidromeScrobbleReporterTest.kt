package com.tuneflow.tv

import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.player.ScrobbleSubmission
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavidromeScrobbleReporterTest {
    @Test
    fun `matching account reports now playing without a submission`() =
        runTest {
            val session = session("user")
            val client = RecordingNavidromeClient(session)
            val reporter =
                NavidromeScrobbleReporter(
                    currentSession = { session },
                    clientProvider = NavidromeClientProvider { client },
                    diagnostic = {},
                )

            reporter.nowPlaying(
                ScrobbleSubmission(
                    accountKey = requireNotNull(reporter.currentAccountKey()),
                    trackId = "track-id",
                    startedAtEpochMs = 1_234_567L,
                ),
            )

            assertEquals(listOf(Triple("track-id", 1_234_567L, false)), client.submissions)
        }

    @Test
    fun `matching account submits original track and timestamp`() =
        runTest {
            val session = session("user")
            val client = RecordingNavidromeClient(session)
            val reporter =
                NavidromeScrobbleReporter(
                    currentSession = { session },
                    clientProvider = NavidromeClientProvider { client },
                    diagnostic = {},
                )
            val accountKey = requireNotNull(reporter.currentAccountKey())

            reporter.scrobble(ScrobbleSubmission(accountKey, "track-id", 1_234_567L))

            assertEquals(listOf(Triple("track-id", 1_234_567L, true)), client.submissions)
        }

    @Test
    fun `changed account discards submission`() =
        runTest {
            var activeSession = session("first")
            val client = RecordingNavidromeClient(activeSession)
            val reporter =
                NavidromeScrobbleReporter(
                    currentSession = { activeSession },
                    clientProvider = NavidromeClientProvider { client },
                    diagnostic = {},
                )
            val originalAccountKey = requireNotNull(reporter.currentAccountKey())
            activeSession = session("second")

            reporter.scrobble(ScrobbleSubmission(originalAccountKey, "track-id", 1_234_567L))

            assertTrue(client.submissions.isEmpty())
        }

    @Test
    fun `server failure is diagnostic only`() =
        runTest {
            val session = session("user")
            val diagnostics = mutableListOf<String>()
            val client = RecordingNavidromeClient(session, NetworkResult.Error("offline"))
            val reporter =
                NavidromeScrobbleReporter(
                    currentSession = { session },
                    clientProvider = NavidromeClientProvider { client },
                    diagnostic = diagnostics::add,
                )

            reporter.scrobble(
                ScrobbleSubmission(
                    accountKey = requireNotNull(reporter.currentAccountKey()),
                    trackId = "track-id",
                    startedAtEpochMs = 1_234_567L,
                ),
            )

            assertEquals(1, diagnostics.size)
            assertTrue(diagnostics.single().contains("playback continues"))
        }

    private fun session(username: String) =
        SessionData(
            serverUrl = "https://music.example.com",
            username = username,
            token = "token",
            salt = "salt",
        )
}

private class RecordingNavidromeClient(
    session: SessionData,
    private val result: NetworkResult<Unit> = NetworkResult.Success(Unit),
) : NavidromeClient(session) {
    val submissions = mutableListOf<Triple<String, Long, Boolean>>()

    override suspend fun scrobble(
        trackId: String,
        startedAtEpochMs: Long,
        submission: Boolean,
    ): NetworkResult<Unit> {
        submissions += Triple(trackId, startedAtEpochMs, submission)
        return result
    }
}
