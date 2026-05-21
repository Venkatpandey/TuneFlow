package com.tuneflow.core.player

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.queueDataStore by preferencesDataStore(name = "tuneflow_queue")

class QueueStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val queue = stringPreferencesKey("queue_json")
        val equalizerPreset = stringPreferencesKey("equalizer_preset")
    }

    val queueFlow: Flow<PlaybackQueue?> =
        context.queueDataStore.data
            .catch { ex ->
                if (ex is IOException) emit(emptyPreferences()) else throw ex
            }
            .map { prefs -> prefs.toQueue(json) }

    val equalizerPresetFlow: Flow<EqualizerPreset> =
        context.queueDataStore.data
            .catch { ex ->
                if (ex is IOException) emit(emptyPreferences()) else throw ex
            }
            .map { prefs -> prefs.toEqualizerPreset() }

    suspend fun save(queue: PlaybackQueue) {
        context.queueDataStore.edit { prefs ->
            prefs[Keys.queue] = json.encodeToString(PlaybackQueue.serializer(), queue)
        }
    }

    suspend fun clear() {
        context.queueDataStore.edit { prefs ->
            prefs.remove(Keys.queue)
        }
    }

    suspend fun saveEqualizerPreset(preset: EqualizerPreset) {
        context.queueDataStore.edit { prefs ->
            prefs[Keys.equalizerPreset] = preset.name
        }
    }

    private fun Preferences.toQueue(json: Json): PlaybackQueue? {
        val raw = this[Keys.queue] ?: return null
        return runCatching { json.decodeFromString(PlaybackQueue.serializer(), raw) }.getOrNull()
    }

    private fun Preferences.toEqualizerPreset(): EqualizerPreset {
        val raw = this[Keys.equalizerPreset] ?: return EqualizerPreset.Original
        return runCatching { EqualizerPreset.valueOf(raw) }.getOrDefault(EqualizerPreset.Original)
    }
}
