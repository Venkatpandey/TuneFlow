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

            assertEquals(listOf("track-id" to 1_234_567L), client.submissions)
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
    val submissions = mutableListOf<Pair<String, Long>>()

    override suspend fun scrobble(
        trackId: String,
        startedAtEpochMs: Long,
    ): NetworkResult<Unit> {
        submissions += trackId to startedAtEpochMs
        return result
    }
}
