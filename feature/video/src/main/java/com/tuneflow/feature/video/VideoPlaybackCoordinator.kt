package com.tuneflow.feature.video

import com.tuneflow.core.player.PlaybackController
import com.tuneflow.core.player.PlaybackMode

data class AudioPlaybackSnapshot(
    val trackId: String,
    val positionMs: Long,
    val durationMs: Long,
    val wasPlaying: Boolean,
    val queueIndex: Int,
    val playbackMode: PlaybackMode,
    val streamFormatLabel: String,
    val streamBitrateLabel: String,
)

class VideoPlaybackCoordinator(
    private val audio: PlaybackController,
) {
    private var snapshot: AudioPlaybackSnapshot? = null
    private var restored = false

    @Suppress("ReturnCount")
    fun startVideo(trackId: String): Long? {
        snapshot?.let { return it.positionMs }
        val queue = audio.queue.value
        val current = queue.currentItem?.takeIf { it.id == trackId } ?: return null

        snapshot =
            AudioPlaybackSnapshot(
                trackId = current.id,
                positionMs = audio.currentPositionMs().coerceAtLeast(0L),
                durationMs = (audio.durationMs().takeIf { it > 0L } ?: current.durationMs).coerceAtLeast(0L),
                wasPlaying = audio.isPlaying.value,
                queueIndex = queue.currentIndex,
                playbackMode = audio.playbackMode.value,
                streamFormatLabel = current.streamFormatLabel,
                streamBitrateLabel = current.streamBitrateLabel,
            )
        restored = false
        audio.pause()
        return snapshot?.positionMs
    }

    fun returnToAudio(
        providerPositionMs: Long?,
        resumeAudio: Boolean,
    ) {
        if (snapshot == null) {
            if (!resumeAudio) audio.pause()
            return
        }
        restore(providerPositionMs, resumeAudio)
    }

    fun restoreAfterFailure() {
        val captured = snapshot ?: return
        restore(captured.positionMs, captured.wasPlaying)
    }

    fun discard() {
        snapshot = null
        restored = false
    }

    fun activeSnapshot(): AudioPlaybackSnapshot? = snapshot

    private fun restore(
        requestedPositionMs: Long?,
        resumeAudio: Boolean,
    ) {
        val captured = snapshot ?: return
        if (restored) return
        restored = true

        val queue = audio.queue.value
        val sameTrack = queue.currentItem?.id == captured.trackId && queue.currentIndex == captured.queueIndex
        if (sameTrack) {
            val fallbackPosition = captured.positionMs
            val upperBound = captured.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
            val position = (requestedPositionMs ?: fallbackPosition).coerceIn(0L, upperBound)
            audio.seekTo(position)
            if (resumeAudio) {
                audio.play()
            } else {
                audio.pause()
            }
        }

        snapshot = null
    }
}
