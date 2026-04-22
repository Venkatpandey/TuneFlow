package com.tuneflow.feature.playback

import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.QueueItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun queueCurrentItem_reflectsIndex() {
        val queue =
            PlaybackQueue(
                items =
                    listOf(
                        QueueItem("1", "A", "A", "A", streamUrl = "s1"),
                        QueueItem("2", "B", "A", "A", streamUrl = "s2"),
                    ),
                currentIndex = 1,
            )

        assertEquals("2", queue.currentItem?.id)
    }
}
