package com.tuneflow.tv

import android.view.KeyEvent

internal enum class UserInputCategory {
    Navigation,
    Back,
    Select,
    Media,
    Keyboard,
    Touch,
}

internal enum class ScreensaverKeyAction {
    RecordAndDispatch,
    WakeAndConsume,
    WakeAndDispatchMedia,
}

internal enum class MediaPlaybackAction {
    Toggle,
    Play,
    Pause,
    Next,
    Previous,
    Stop,
    DispatchToSystem,
}

internal fun resolveScreensaverKeyAction(
    screensaverActive: Boolean,
    keyCode: Int,
): ScreensaverKeyAction =
    when {
        !screensaverActive -> ScreensaverKeyAction.RecordAndDispatch
        isMediaKey(keyCode) -> ScreensaverKeyAction.WakeAndDispatchMedia
        else -> ScreensaverKeyAction.WakeAndConsume
    }

internal fun classifyUserInput(keyCode: Int): UserInputCategory =
    when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        -> UserInputCategory.Navigation
        KeyEvent.KEYCODE_BACK -> UserInputCategory.Back
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        -> UserInputCategory.Select
        else -> if (isMediaKey(keyCode)) UserInputCategory.Media else UserInputCategory.Keyboard
    }

internal fun isMediaKey(keyCode: Int): Boolean =
    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
        keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
        keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
        keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
        keyCode == KeyEvent.KEYCODE_MEDIA_STOP ||
        keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
        keyCode == KeyEvent.KEYCODE_MEDIA_REWIND ||
        keyCode == KeyEvent.KEYCODE_HEADSETHOOK

internal fun resolveMediaPlaybackAction(keyCode: Int): MediaPlaybackAction =
    when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_HEADSETHOOK,
        -> MediaPlaybackAction.Toggle
        KeyEvent.KEYCODE_MEDIA_PLAY -> MediaPlaybackAction.Play
        KeyEvent.KEYCODE_MEDIA_PAUSE -> MediaPlaybackAction.Pause
        KeyEvent.KEYCODE_MEDIA_NEXT -> MediaPlaybackAction.Next
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaPlaybackAction.Previous
        KeyEvent.KEYCODE_MEDIA_STOP -> MediaPlaybackAction.Stop
        else -> MediaPlaybackAction.DispatchToSystem
    }
