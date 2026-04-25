package com.tuneflow.core.network

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private val Context.sessionDataStore by preferencesDataStore(name = "tuneflow_session")

data class SessionData(
    val serverUrl: String,
    val username: String,
    val token: String,
    val salt: String,
)

class SessionStore(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val username = stringPreferencesKey("username")
        val token = stringPreferencesKey("token")
        val salt = stringPreferencesKey("salt")
    }

    val sessionFlow: Flow<SessionData?> =
        context.sessionDataStore.data
            .catch { ex ->
                if (ex is IOException) emit(emptyPreferences()) else throw ex
            }
            .map { prefs -> prefs.toSessionData() }

    val sessionState: StateFlow<SessionData?> =
        sessionFlow.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    suspend fun save(sessionData: SessionData) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.serverUrl] = sessionData.serverUrl.trimEnd('/')
            prefs[Keys.username] = sessionData.username
            prefs[Keys.token] = sessionData.token
            prefs[Keys.salt] = sessionData.salt
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.serverUrl)
            prefs.remove(Keys.username)
            prefs.remove(Keys.token)
            prefs.remove(Keys.salt)
        }
    }

    private fun Preferences.toSessionData(): SessionData? {
        val serverUrl = this[Keys.serverUrl]
        val username = this[Keys.username]
        val token = this[Keys.token]
        val salt = this[Keys.salt]

        if (serverUrl.isNullOrBlank() || username.isNullOrBlank() || token.isNullOrBlank() || salt.isNullOrBlank()) {
            return null
        }

        return SessionData(serverUrl = serverUrl, username = username, token = token, salt = salt)
    }
}
