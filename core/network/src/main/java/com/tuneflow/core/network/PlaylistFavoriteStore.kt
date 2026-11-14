package com.tuneflow.core.network

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.playlistFavoritesDataStore by preferencesDataStore(name = "tuneflow_playlist_favorites")

class PlaylistFavoriteStore(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    private val preferencesFlow: Flow<Preferences> =
        context.playlistFavoritesDataStore.data.catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val favoritePlaylistIds: Flow<Set<String>> =
        combine(sessionStore.sessionFlow, preferencesFlow) { session, preferences ->
            session?.let { preferences[favoritePlaylistIdsKey(it)].orEmpty().toSet() }.orEmpty()
        }

    suspend fun toggle(playlistId: String) {
        if (playlistId.isBlank()) return
        val session = sessionStore.sessionFlow.first() ?: return
        val key = favoritePlaylistIdsKey(session)

        context.playlistFavoritesDataStore.edit { preferences ->
            val favoriteIds = preferences[key].orEmpty()
            val updated =
                if (playlistId in favoriteIds) {
                    favoriteIds - playlistId
                } else {
                    favoriteIds + playlistId
                }

            if (updated.isEmpty()) preferences.remove(key) else preferences[key] = updated
        }
    }
}

internal fun favoritePlaylistIdsKey(session: SessionData): Preferences.Key<Set<String>> =
    stringSetPreferencesKey(
        "favorite_playlist_ids:${session.serverUrl.trimEnd('/')}:${session.username}",
    )
