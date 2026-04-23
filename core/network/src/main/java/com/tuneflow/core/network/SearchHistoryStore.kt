package com.tuneflow.core.network

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.searchHistoryDataStore by preferencesDataStore(name = "tuneflow_search_history")

class SearchHistoryStore(private val context: Context) {
    private object Keys {
        val queries = stringPreferencesKey("queries")
    }

    val recentQueriesFlow: Flow<List<String>> =
        context.searchHistoryDataStore.data
            .catch { ex ->
                if (ex is IOException) emit(emptyPreferences()) else throw ex
            }
            .map { prefs -> prefs.toRecentQueries() }

    suspend fun record(
        query: String,
        limit: Int = 8,
    ) {
        val normalized = query.trim()
        if (normalized.isBlank()) return

        context.searchHistoryDataStore.edit { prefs ->
            val updated =
                listOf(normalized) +
                    prefs.toRecentQueries().filterNot {
                        it.equals(normalized, ignoreCase = true)
                    }

            prefs[Keys.queries] =
                updated.take(limit).joinToString(separator = "\n")
        }
    }

    private fun Preferences.toRecentQueries(): List<String> {
        val raw = this[Keys.queries] ?: return emptyList()
        return raw
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
