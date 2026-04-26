package com.tuneflow.core.network

fun interface SessionProvider {
    suspend fun currentSession(): SessionData?
}

class DataStoreSessionProvider(private val sessionStore: SessionStore) : SessionProvider {
    override suspend fun currentSession(): SessionData? = sessionStore.sessionState.value
}
