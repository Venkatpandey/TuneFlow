package com.tuneflow.tv

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAboutInputTest {
    @Test
    fun firstSelectPressArmsLongPress() {
        assertEquals(
            ProfileAboutKeyAction.Reset,
            profileAboutKeyAction(
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                longPressHandled = false,
            ),
        )
    }

    @Test
    fun repeatedSelectPressOpensAboutOnce() {
        assertEquals(
            ProfileAboutKeyAction.OpenAbout,
            profileAboutKeyAction(
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 1,
                longPressHandled = false,
            ),
        )
        assertEquals(
            ProfileAboutKeyAction.Consume,
            profileAboutKeyAction(
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 2,
                longPressHandled = true,
            ),
        )
    }

    @Test
    fun selectReleaseResetsLongPress() {
        assertEquals(
            ProfileAboutKeyAction.Reset,
            profileAboutKeyAction(
                keyCode = KeyEvent.KEYCODE_ENTER,
                action = KeyEvent.ACTION_UP,
                repeatCount = 0,
                longPressHandled = true,
            ),
        )
    }

    @Test
    fun unrelatedKeysRemainAvailableForFocusNavigation() {
        assertEquals(
            ProfileAboutKeyAction.Ignore,
            profileAboutKeyAction(
                keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
                action = KeyEvent.ACTION_DOWN,
                repeatCount = 0,
                longPressHandled = false,
            ),
        )
    }
}
