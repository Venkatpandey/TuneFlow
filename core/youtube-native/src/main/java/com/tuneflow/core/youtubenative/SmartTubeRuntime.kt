package com.tuneflow.core.youtubenative

import android.content.Context
import com.liskovsoft.sharedutils.okhttp.OkHttpManager
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData

internal object SmartTubeRuntime {
    private val lock = Any()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            GlobalPreferences.instance(context.applicationContext)
            OkHttpManager.instance(false)
            MediaServiceData.instance().setFormatEnabled(MediaServiceData.FORMATS_ALL, true)
            initialized = true
        }
    }
}
