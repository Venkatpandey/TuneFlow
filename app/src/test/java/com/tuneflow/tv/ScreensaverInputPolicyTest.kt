package com.tuneflow.tv

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreensaverInputPolicyTest {
    @Test
    fun firstNavigationSelectBackAndKeyboardWakeEventIsConsumed() {
        listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_A,
        ).forEach { keyCode ->
            assertEquals(
                ScreensaverKeyAction.WakeAndConsume,
                resolveScreensaverKeyAction(screensaverActive = true, keyCode = keyCode),
            )
        }
    }

    @Test
    fun mediaWakeEventStillDispatchesPlaybackAction() {
        assertEquals(
            ScreensaverKeyAction.WakeAndDispatchMedia,
            resolveScreensaverKeyAction(true, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
        )
        assertEquals(MediaPlaybackAction.Toggle, resolveMediaPlaybackAction(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertEquals(MediaPlaybackAction.Play, resolveMediaPlaybackAction(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertEquals(MediaPlaybackAction.Pause, resolveMediaPlaybackAction(KeyEvent.KEYCODE_MEDIA_PAUSE))
        assertEquals(MediaPlaybackAction.Next, resolveMediaPlaybackAction(KeyEvent.KEYCODE_MEDIA_NEXT))
        assertEquals(MediaPlaybackAction.Previous, resolveMediaPlaybackAction(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        assertEquals(MediaPlaybackAction.Stop, resolveMediaPlaybackAction(KeyEvent.KEYCODE_MEDIA_STOP))
        assertEquals(MediaPlaybackAction.DispatchToSystem, resolveMediaPlaybackAction(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
    }

    @Test
    fun inactiveScreensaverRecordsWithoutConsuming() {
        assertEquals(
            ScreensaverKeyAction.RecordAndDispatch,
            resolveScreensaverKeyAction(false, KeyEvent.KEYCODE_DPAD_DOWN),
        )
    }

    @Test
    fun inputCategoriesCoverRemoteMediaKeyboardAndTouch() {
        assertEquals(UserInputCategory.Navigation, classifyUserInput(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(UserInputCategory.Back, classifyUserInput(KeyEvent.KEYCODE_BACK))
        assertEquals(UserInputCategory.Select, classifyUserInput(KeyEvent.KEYCODE_ENTER))
        assertEquals(UserInputCategory.Media, classifyUserInput(KeyEvent.KEYCODE_MEDIA_NEXT))
        assertEquals(UserInputCategory.Keyboard, classifyUserInput(KeyEvent.KEYCODE_A))
        assertEquals(UserInputCategory.Touch, UserInputCategory.Touch)
    }
}
