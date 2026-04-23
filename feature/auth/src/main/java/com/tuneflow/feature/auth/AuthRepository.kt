package com.tuneflow.feature.auth

import com.tuneflow.core.network.AuthUtils
import com.tuneflow.core.network.DefaultNavidromeClientProvider
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkFactory
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionStore

class AuthRepository(
    private val saveSession: suspend (SessionData) -> Unit,
    private val clearSession: suspend () -> Unit,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
) {
    constructor(
        sessionStore: SessionStore,
        clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
    ) : this(
        saveSession = sessionStore::save,
        clearSession = sessionStore::clear,
        clientProvider = clientProvider,
    )

    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
    ): Result<Unit> {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("All fields are required."))
        }

        val normalizedUrl =
            runCatching { NetworkFactory.normalizeBaseUrl(serverUrl) }.getOrElse {
                return Result.failure(it)
            }

        val salt = AuthUtils.generateSalt()
        val session =
            SessionData(
                serverUrl = normalizedUrl,
                username = username.trim(),
                token = AuthUtils.buildToken(password, salt),
                salt = salt,
            )

        return runCatching {
            val client = clientProvider.create(session)
            when (val result = client.ping()) {
                is NetworkResult.Success -> {
                    saveSession(session)
                    Result.success(Unit)
                }

                is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
            }
        }.getOrElse { Result.failure(it) }
    }

    suspend fun logout() {
        clearSession()
    }
}
