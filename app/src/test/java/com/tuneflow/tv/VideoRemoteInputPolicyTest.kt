package com.tuneflow.tv

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRemoteInputPolicyTest {
    @Test
    fun fullscreenNavigationKeysStayOwnedByYouTubePlayer() {
        val providerKeys =
            listOf(
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_BACK,
            )

        assertTrue(providerKeys.all(::isYouTubeProviderKey))
        assertFalse(isYouTubeProviderKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertFalse(isYouTubeProviderKey(KeyEvent.KEYCODE_MEDIA_STOP))
    }
}
