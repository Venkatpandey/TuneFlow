package com.tuneflow.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

@Composable
fun InitialFocusEffect(
    focusRequester: FocusRequester,
    targetAvailable: Boolean,
    restorationPending: Boolean = false,
    resetKey: Any? = Unit,
) {
    var initialFocusHandled by remember(resetKey) { mutableStateOf(false) }

    LaunchedEffect(focusRequester, targetAvailable, restorationPending, resetKey) {
        if (restorationPending) {
            initialFocusHandled = true
            return@LaunchedEffect
        }
        if (shouldRequestInitialFocus(targetAvailable, initialFocusHandled, restorationPending)) {
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
            initialFocusHandled = true
        }
    }
}

fun shouldRequestInitialFocus(
    targetAvailable: Boolean,
    initialFocusAlreadyHandled: Boolean,
    restorationPending: Boolean = false,
): Boolean = targetAvailable && !initialFocusAlreadyHandled && !restorationPending
