package com.tuneflow.core.network

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.playlistUsageDataStore by preferencesDataStore(name = "tuneflow_playlist_usage")

class PlaylistUsageStore(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    private val preferencesFlow: Flow<Preferences> =
        context.playlistUsageDataStore.data.catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val recentPlaylistIds: Flow<List<String>> =
        combine(sessionStore.sessionFlow, preferencesFlow) { session, preferences ->
            session?.let { preferences[recentPlaylistIdsKey(it)].toPlaylistIds() }.orEmpty()
        }

    suspend fun record(playlistId: String?) {
        val normalizedId = playlistId?.trim().orEmpty()
        if (normalizedId.isEmpty()) return
        val session = sessionStore.sessionFlow.first() ?: return
        val key = recentPlaylistIdsKey(session)

        context.playlistUsageDataStore.edit { preferences ->
            val updated = preferences[key].toPlaylistIds().withMostRecentPlaylist(normalizedId)
            preferences[key] = updated.joinToString(separator = "\n")
        }
    }
}

internal fun recentPlaylistIdsKey(session: SessionData): Preferences.Key<String> =
    stringPreferencesKey(
        "recent_playlist_ids:${session.serverUrl.trimEnd('/')}:${session.username}",
    )

private fun String?.toPlaylistIds(): List<String> =
    this
        ?.lineSequence()
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.distinct()
        ?.toList()
        .orEmpty()

internal fun List<String>.withMostRecentPlaylist(playlistId: String): List<String> = listOf(playlistId) + filterNot { it == playlistId }
