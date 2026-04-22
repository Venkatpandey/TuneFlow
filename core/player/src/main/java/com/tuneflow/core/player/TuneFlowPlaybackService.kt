package com.tuneflow.core.player

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TuneFlowPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var session: MediaSession
    private lateinit var manager: TvPlayerManager

    override fun onCreate() {
        super.onCreate()
        manager = PlayerGraph.get(applicationContext)
        session = MediaSession.Builder(this, manager.player).build()
        serviceScope.launch {
            manager.restore()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onDestroy() {
        session.release()
        PlayerGraph.release()
        super.onDestroy()
    }
}
