package com.tuneflow.core.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.accessibilityPreferencesDataStore by preferencesDataStore(name = "tuneflow_accessibility_preferences")

enum class MotionPreference(val rawValue: String) {
    System("system"),
    Always("always"),
    Never("never"),
    ;

    companion object {
        fun fromRaw(value: String?): MotionPreference = entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: System
    }
}

class AccessibilityPreferencesStore(private val context: Context) {
    private object Keys {
        val motionPreference = stringPreferencesKey("motion_preference")
    }

    private val preferencesFlow =
        context.accessibilityPreferencesDataStore.data.catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val motionPreferenceFlow: Flow<MotionPreference> =
        preferencesFlow.map { preferences ->
            MotionPreference.fromRaw(preferences[Keys.motionPreference])
        }

    suspend fun setMotionPreference(preference: MotionPreference) {
        context.accessibilityPreferencesDataStore.edit { preferences ->
            preferences[Keys.motionPreference] = preference.rawValue
        }
    }

    companion object {
        val DEFAULT_MOTION_PREFERENCE = MotionPreference.System
    }
}
