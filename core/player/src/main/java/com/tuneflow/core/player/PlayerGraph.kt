package com.tuneflow.core.player

import android.content.Context

object PlayerGraph {
    @Volatile
    private var manager: TvPlayerManager? = null

    fun get(context: Context): TvPlayerManager {
        return manager ?: synchronized(this) {
            manager ?: TvPlayerManager(context.applicationContext, QueueStore(context.applicationContext)).also {
                manager = it
            }
        }
    }

    fun release() {
        synchronized(this) {
            manager?.release()
            manager = null
        }
    }
}
