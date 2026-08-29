package com.tuneflow.feature.video

import android.content.Context

interface VideoConsentStore {
    fun isAccepted(): Boolean

    fun accept()
}

class SharedPreferencesVideoConsentStore(context: Context) : VideoConsentStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun isAccepted(): Boolean = preferences.getBoolean(KEY_YOUTUBE_DISCLOSURE_ACCEPTED, false)

    override fun accept() {
        preferences.edit().putBoolean(KEY_YOUTUBE_DISCLOSURE_ACCEPTED, true).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "video_provider_preferences"
        const val KEY_YOUTUBE_DISCLOSURE_ACCEPTED = "youtube_disclosure_accepted"
    }
}
