package com.tuneflow.feature.video

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class PreferredVideoServiceConfigStore(
    context: Context,
    buildDefaultUrl: String,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val normalizedBuildDefaultUrl = normalizePreferredVideoServiceUrl(buildDefaultUrl).orEmpty()

    val serviceUrl: String
        get() =
            if (preferences.contains(KEY_SERVICE_URL)) {
                preferences.getString(KEY_SERVICE_URL, "").orEmpty()
            } else {
                normalizedBuildDefaultUrl
            }

    fun saveServiceUrl(value: String): String? {
        val normalized = normalizePreferredVideoServiceUrl(value) ?: return null
        preferences.edit().putString(KEY_SERVICE_URL, normalized).apply()
        return normalized
    }

    private companion object {
        const val PREFERENCES_NAME = "preferred_video_service"
        const val KEY_SERVICE_URL = "service_url"
    }
}

fun normalizePreferredVideoServiceUrl(value: String): String? =
    value.trim().let { trimmed ->
        if (trimmed.isEmpty()) {
            ""
        } else {
            val candidate = if ("://" in trimmed) trimmed else "http://$trimmed"
            val url = candidate.toHttpUrlOrNull()
            if (
                url == null ||
                url.username.isNotEmpty() ||
                url.password.isNotEmpty() ||
                url.query != null ||
                url.fragment != null
            ) {
                null
            } else {
                url.toString().trimEnd('/')
            }
        }
    }
