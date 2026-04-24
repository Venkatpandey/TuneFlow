package com.tuneflow.core.network

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playbackPreferencesDataStore by preferencesDataStore(name = "tuneflow_playback_preferences")

class PlaybackPreferencesStore(private val context: Context) {
    private object Keys {
        val preferDirectWithFallback = booleanPreferencesKey("prefer_direct_with_fallback")
    }

    val preferDirectWithFallbackFlow: Flow<Boolean> =
        context.playbackPreferencesDataStore.data.map { preferences ->
            preferences[Keys.preferDirectWithFallback] ?: true
        }

    suspend fun setPreferDirectWithFallback(enabled: Boolean) {
        context.playbackPreferencesDataStore.edit { preferences ->
            preferences[Keys.preferDirectWithFallback] = enabled
        }
    }
}
