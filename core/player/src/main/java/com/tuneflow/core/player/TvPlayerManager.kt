package com.tuneflow.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class TvPlayerManager(
    context: Context,
    private val queueStore: QueueStore,
) : PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext = context.applicationContext

    private val _queue = MutableStateFlow(PlaybackQueue())
    override val queue: StateFlow<PlaybackQueue> = _queue.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val player: ExoPlayer =
        ExoPlayer.Builder(appContext).build().also { exo ->
            exo.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            exo.playWhenReady = true
            exo.addListener(
                object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        val index = exo.currentMediaItemIndex
                        _queue.update { q ->
                            if (q.items.isEmpty()) q else q.copy(currentIndex = index.coerceIn(0, q.items.lastIndex))
                        }
                        persist()
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: androidx.media3.common.Player.PositionInfo,
                        newPosition: androidx.media3.common.Player.PositionInfo,
                        reason: Int,
                    ) {
                        _queue.update { it.seek(exo.currentPosition) }
                    }
                },
            )
        }

    suspend fun restore() {
        val restored =
            queueStore.queueFlow
                .mapNotNullOnce()
                ?: return

        if (restored.items.isNotEmpty()) {
            _queue.value = restored
            player.setMediaItems(restored.items.map { it.toMediaItem() }, restored.currentIndex, restored.currentPositionMs)
            player.prepare()
        }
    }

    fun playQueue(
        items: List<QueueItem>,
        startIndex: Int = 0,
    ) {
        if (items.isEmpty()) return

        val queue = PlaybackQueue().replace(items, startIndex)
        _queue.value = queue

        val mediaItems = items.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, queue.currentIndex, 0L)
        player.prepare()
        player.play()
        persist()
    }

    override fun play() = player.play()

    override fun pause() {
        player.pause()
        persist()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _queue.update { it.seek(positionMs) }
        persist()
    }

    override fun stopAndClear() {
        player.stop()
        player.clearMediaItems()
        _queue.value = PlaybackQueue()
        _isPlaying.value = false
        scope.launch {
            queueStore.clear()
        }
    }

    override fun next() {
        player.seekToNextMediaItem()
        _queue.update { it.next(player.currentPosition) }
        persist()
    }

    override fun previous() {
        player.seekToPreviousMediaItem()
        _queue.update { it.previous(player.currentPosition) }
        persist()
    }

    override fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)

    override fun durationMs(): Long = player.duration.coerceAtLeast(0L)

    fun release() {
        persist()
        player.release()
    }

    private fun QueueItem.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build(),
            )
            .build()
    }

    private fun persist() {
        scope.launch {
            queueStore.save(
                _queue.value.copy(currentPositionMs = player.currentPosition.coerceAtLeast(0L)),
            )
        }
    }
}

private suspend fun kotlinx.coroutines.flow.Flow<PlaybackQueue?>.mapNotNullOnce(): PlaybackQueue? {
    return first()
}
