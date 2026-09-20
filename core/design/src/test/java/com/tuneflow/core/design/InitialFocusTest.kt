package com.tuneflow.core.design

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialFocusTest {
    @Test
    fun waitsUntilVisibleTargetIsAvailable() {
        assertFalse(shouldRequestInitialFocus(targetAvailable = false, initialFocusAlreadyHandled = false))
        assertTrue(shouldRequestInitialFocus(targetAvailable = true, initialFocusAlreadyHandled = false))
    }

    @Test
    fun refreshDoesNotStealFocusAfterInitialRequest() {
        assertFalse(shouldRequestInitialFocus(targetAvailable = true, initialFocusAlreadyHandled = true))
    }

    @Test
    fun pendingRestorationWinsOverDefaultTarget() {
        assertFalse(
            shouldRequestInitialFocus(
                targetAvailable = true,
                initialFocusAlreadyHandled = false,
                restorationPending = true,
            ),
        )
    }
}
