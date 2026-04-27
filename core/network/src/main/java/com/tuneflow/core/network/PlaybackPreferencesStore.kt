package com.tuneflow.core.network

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playbackPreferencesDataStore by preferencesDataStore(name = "tuneflow_playback_preferences")

enum class ScreenScaleOption(val percent: Int, val factor: Float) {
    Full(100, 1f),
    Compact(75, 0.75f),
    Dense(50, 0.5f),
    ;

    companion object {
        fun fromPercent(percent: Int): ScreenScaleOption =
            entries.firstOrNull { it.percent == percent } ?: Compact
    }
}

class PlaybackPreferencesStore(private val context: Context) {
    private object Keys {
        val preferDirectWithFallback = booleanPreferencesKey("prefer_direct_with_fallback")
        val screenScalePercent = intPreferencesKey("screen_scale_percent")
    }

    val preferDirectWithFallbackFlow: Flow<Boolean> =
        context.playbackPreferencesDataStore.data.map { preferences ->
            // Default false = always use MP3 max bitrate.
            // Users can opt into FLAC-first + fallback from the account menu.
            preferences[Keys.preferDirectWithFallback] ?: false
        }

    val screenScaleOptionFlow: Flow<ScreenScaleOption> =
        context.playbackPreferencesDataStore.data.map { preferences ->
            ScreenScaleOption.fromPercent(preferences[Keys.screenScalePercent] ?: ScreenScaleOption.Compact.percent)
        }

    suspend fun setPreferDirectWithFallback(enabled: Boolean) {
        context.playbackPreferencesDataStore.edit { preferences ->
            preferences[Keys.preferDirectWithFallback] = enabled
        }
    }

    suspend fun setScreenScale(option: ScreenScaleOption) {
        context.playbackPreferencesDataStore.edit { preferences ->
            preferences[Keys.screenScalePercent] = option.percent
        }
    }
}
