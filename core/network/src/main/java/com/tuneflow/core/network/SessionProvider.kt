package com.tuneflow.core.network

import kotlinx.coroutines.flow.first

fun interface SessionProvider {
    suspend fun currentSession(): SessionData?
}

class DataStoreSessionProvider(private val sessionStore: SessionStore) : SessionProvider {
    override suspend fun currentSession(): SessionData? = sessionStore.sessionFlow.first()
}
