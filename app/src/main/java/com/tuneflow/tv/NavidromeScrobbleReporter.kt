package com.tuneflow.tv

import android.util.Log
import com.tuneflow.core.network.DefaultNavidromeClientProvider
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkFactory
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.player.ScrobbleReporter
import com.tuneflow.core.player.ScrobbleSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class NavidromeScrobbleReporter(
    private val currentSession: () -> SessionData?,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
    private val diagnostic: (String) -> Unit = { message -> Log.w(SCROBBLE_LOG_TAG, message) },
) : ScrobbleReporter {
    constructor(sessionStore: SessionStore) : this(currentSession = { sessionStore.sessionState.value })

    override fun currentAccountKey(): String? = currentSession()?.let { session -> runCatching { session.accountKey() }.getOrNull() }

    override suspend fun scrobble(submission: ScrobbleSubmission) {
        val session = currentSession()
        if (session != null && runCatching { session.accountKey() }.getOrNull() == submission.accountKey) {
            val result =
                try {
                    withContext(Dispatchers.IO) {
                        clientProvider.create(session).scrobble(
                            trackId = submission.trackId,
                            startedAtEpochMs = submission.startedAtEpochMs,
                        )
                    }
                } catch (error: IllegalArgumentException) {
                    diagnostic("Navidrome scrobble failed; playback continues: ${error.message.orEmpty()}")
                    null
                }
            if (result is NetworkResult.Error) {
                diagnostic("Navidrome scrobble failed; playback continues: ${result.message}")
            }
        }
    }
}

private fun SessionData.accountKey(): String = "${NetworkFactory.normalizeBaseUrl(serverUrl)}\u0000$username"

private const val SCROBBLE_LOG_TAG = "TuneFlowScrobble"
