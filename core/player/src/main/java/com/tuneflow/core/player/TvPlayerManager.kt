package com.tuneflow.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private var lastError: PlaybackException? = null
    private var expectedToPlay = false
    private var fallbackMonitorJob: Job? = null

    private val _queue = MutableStateFlow(PlaybackQueue())
    override val queue: StateFlow<PlaybackQueue> = _queue.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackStatus = MutableStateFlow(PlaybackStatus())
    override val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    val player: ExoPlayer =
        ExoPlayer.Builder(appContext)
            .setHandleAudioBecomingNoisy(true)
            .build().also { exo ->
                exo.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                exo.playWhenReady = true
                exo.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                            if (isPlaying) {
                                lastError = null
                                cancelFallbackMonitor()
                            } else if (expectedToPlay) {
                                scheduleFallbackMonitor()
                            }
                            updatePlaybackStatus()
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    if (expectedToPlay) scheduleFallbackMonitor()
                                }
                                Player.STATE_READY -> {
                                    if (exo.isPlaying) {
                                        lastError = null
                                        cancelFallbackMonitor()
                                    } else if (expectedToPlay) {
                                        scheduleFallbackMonitor()
                                    }
                                }
                                Player.STATE_ENDED -> {
                                    expectedToPlay = false
                                    cancelFallbackMonitor()
                                }
                                Player.STATE_IDLE -> {
                                    if (expectedToPlay) scheduleFallbackMonitor()
                                }
                            }
                            updateQueueIndex(exo.currentMediaItemIndex)
                            updatePlaybackStatus()
                        }

                        override fun onPlayWhenReadyChanged(
                            playWhenReady: Boolean,
                            reason: Int,
                        ) {
                            if (!playWhenReady && !exo.isPlaying) {
                                expectedToPlay = false
                                cancelFallbackMonitor()
                            }
                            updatePlaybackStatus()
                        }

                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int,
                        ) {
                            updateQueueIndex(exo.currentMediaItemIndex)
                            if (expectedToPlay) {
                                scheduleFallbackMonitor()
                            }
                            updatePlaybackStatus()
                            persist()
                        }

                        override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int,
                        ) {
                            _queue.update { it.seek(exo.currentPosition) }
                            updateQueueIndex(exo.currentMediaItemIndex)
                            updatePlaybackStatus()
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            lastError = error
                            if (!tryFallbackForCurrentItem()) {
                                _isPlaying.value = false
                                cancelFallbackMonitor()
                            }
                            updatePlaybackStatus()
                        }
                    },
                )
                updatePlaybackStatus()
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
            updatePlaybackStatus()
        }
    }

    fun playQueue(
        items: List<QueueItem>,
        startIndex: Int = 0,
    ) {
        if (items.isEmpty()) return

        val queue = PlaybackQueue().replace(items, startIndex)
        _queue.value = queue
        lastError = null
        expectedToPlay = true

        val mediaItems = items.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, queue.currentIndex, 0L)
        player.prepare()
        player.play()
        scheduleFallbackMonitor()
        updatePlaybackStatus()
        persist()
    }

    override fun play() {
        if (_queue.value.items.isEmpty()) return
        lastError = null
        expectedToPlay = true
        player.play()
        scheduleFallbackMonitor()
        updatePlaybackStatus()
    }

    override fun pause() {
        expectedToPlay = false
        player.pause()
        cancelFallbackMonitor()
        updatePlaybackStatus()
        persist()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _queue.update { it.seek(positionMs) }
        updatePlaybackStatus()
        persist()
    }

    override fun playFromIndex(index: Int) {
        val queue = _queue.value
        if (queue.items.isEmpty()) return

        val clamped = index.coerceIn(0, queue.items.lastIndex)
        lastError = null
        expectedToPlay = true
        player.seekToDefaultPosition(clamped)
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.play()
        updateQueueIndex(clamped)
        scheduleFallbackMonitor()
        updatePlaybackStatus()
        persist()
    }

    override fun retryCurrent() {
        if (_queue.value.items.isEmpty()) return
        lastError = null
        expectedToPlay = true
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.playWhenReady = true
        player.play()
        scheduleFallbackMonitor()
        updatePlaybackStatus()
    }

    override fun stopAndClear() {
        expectedToPlay = false
        lastError = null
        cancelFallbackMonitor()
        player.stop()
        player.clearMediaItems()
        _queue.value = PlaybackQueue()
        _isPlaying.value = false
        _playbackStatus.value = PlaybackStatus()
        scope.launch {
            queueStore.clear()
        }
    }

    override fun next() {
        lastError = null
        expectedToPlay = player.playWhenReady || _isPlaying.value
        player.seekToNextMediaItem()
        updateQueueIndex(player.currentMediaItemIndex)
        scheduleFallbackMonitor()
        updatePlaybackStatus()
        persist()
    }

    override fun previous() {
        lastError = null
        expectedToPlay = player.playWhenReady || _isPlaying.value
        player.seekToPreviousMediaItem()
        updateQueueIndex(player.currentMediaItemIndex)
        scheduleFallbackMonitor()
        updatePlaybackStatus()
        persist()
    }

    override fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)

    override fun durationMs(): Long = player.duration.coerceAtLeast(0L)

    fun release() {
        cancelFallbackMonitor()
        persist()
        player.release()
    }

    private fun scheduleFallbackMonitor() {
        cancelFallbackMonitor()

        val queueSnapshot = _queue.value
        val trackIndex = queueSnapshot.currentIndex
        val track = queueSnapshot.currentItem ?: return
        if (track.fallbackStreamUrl.isNullOrBlank()) return

        fallbackMonitorJob =
            scope.launch {
                delay(5_000L)
                if (!expectedToPlay) return@launch
                if (_queue.value.currentIndex != trackIndex) return@launch
                if (player.isPlaying) return@launch
                tryFallbackForCurrentItem()
                updatePlaybackStatus()
            }
    }

    private fun cancelFallbackMonitor() {
        fallbackMonitorJob?.cancel()
        fallbackMonitorJob = null
    }

    private fun tryFallbackForCurrentItem(): Boolean {
        val queue = _queue.value
        val currentItem = queue.currentItem ?: return false
        val fallbackUrl = currentItem.fallbackStreamUrl ?: return false
        if (currentItem.streamUrl == fallbackUrl) return false

        val updatedItems =
            queue.items.mapIndexed { index, item ->
                if (index == queue.currentIndex) {
                    item.copy(
                        streamUrl = fallbackUrl,
                        fallbackStreamUrl = null,
                        streamFormatLabel = "MP3",
                        streamBitrateLabel = "Max",
                    )
                } else {
                    item
                }
            }

        val updatedQueue =
            queue.copy(
                items = updatedItems,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
            )

        _queue.value = updatedQueue
        lastError = null
        player.setMediaItems(updatedItems.map { it.toMediaItem() }, updatedQueue.currentIndex, updatedQueue.currentPositionMs)
        player.prepare()
        player.play()
        scheduleFallbackMonitor()
        persist()
        return true
    }

    private fun updateQueueIndex(index: Int) {
        _queue.update { queue ->
            if (queue.items.isEmpty()) {
                queue
            } else {
                queue.copy(
                    currentIndex = index.coerceIn(0, queue.items.lastIndex),
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                )
            }
        }
    }

    private fun updatePlaybackStatus() {
        val suppressionMessage =
            if (player.playWhenReady && player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                "Playback is currently suppressed by the device."
            } else {
                null
            }

        _playbackStatus.value =
            PlaybackStatus(
                phase =
                    when (player.playbackState) {
                        Player.STATE_BUFFERING -> PlaybackPhase.Buffering
                        Player.STATE_READY -> PlaybackPhase.Ready
                        Player.STATE_ENDED -> PlaybackPhase.Ended
                        else -> PlaybackPhase.Idle
                    },
                currentIndex = _queue.value.currentIndex,
                expectedToPlay = expectedToPlay || player.playWhenReady,
                errorCategory = lastError?.errorCodeName,
                errorMessage = lastError?.localizedMessage ?: suppressionMessage,
            )
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
