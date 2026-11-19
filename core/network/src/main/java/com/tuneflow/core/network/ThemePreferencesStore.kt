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

private val Context.themePreferencesDataStore by preferencesDataStore(name = "tuneflow_theme_preferences")

class ThemePreferencesStore(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    private val preferencesFlow: Flow<Preferences> =
        context.themePreferencesDataStore.data.catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val currentThemeIdFlow: Flow<String> =
        combine(sessionStore.sessionFlow, preferencesFlow) { session, preferences ->
            val profileKey = session?.let(::themeKey)
            val profileTheme = profileKey?.let { preferences[it] }
            profileTheme ?: preferences[GlobalThemeKey] ?: DEFAULT_THEME_ID
        }

    suspend fun setThemeId(themeId: String) {
        val session = sessionStore.sessionFlow.first()
        val profileKey = session?.let(::themeKey)
        context.themePreferencesDataStore.edit { preferences ->
            if (profileKey != null) {
                preferences[profileKey] = themeId
            }
            preferences[GlobalThemeKey] = themeId
        }
    }

    companion object {
        const val DEFAULT_THEME_ID = "default"
        internal val GlobalThemeKey = stringPreferencesKey("theme_id:global")

        internal fun themeKey(session: SessionData): Preferences.Key<String> =
            stringPreferencesKey(
                "theme_id:${session.serverUrl.trimEnd('/')}:${session.username}",
            )
    }
}
