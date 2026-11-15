package com.tuneflow.tv

import com.tuneflow.core.network.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Test

class AppAboutDialogTest {
    @Test
    fun stableBuildReportsAvailableUpdate() {
        assertEquals(
            "Update 1.3.0 is available.",
            aboutUpdateStatus(
                state = AppUpdateCheckUiState.Complete(RELEASE, updateAvailable = true),
                updatesEnabled = true,
            ),
        )
    }

    @Test
    fun betaBuildExplainsManualUpdates() {
        assertEquals(
            "Latest stable release: 1.3.0. Beta builds update manually.",
            aboutUpdateStatus(
                state = AppUpdateCheckUiState.Complete(RELEASE, updateAvailable = true),
                updatesEnabled = false,
            ),
        )
    }

    @Test
    fun logoutConfirmationMessageIncludesUsernameWhenAvailable() {
        assertEquals(
            "Are you sure you want to log out of demo_user? Playback will stop and you will return to the sign-in screen.",
            aboutLogoutConfirmationMessage("demo_user"),
        )
    }

    @Test
    fun logoutConfirmationMessageHandlesBlankOrNullUsername() {
        assertEquals(
            "Are you sure you want to log out? Playback will stop and you will return to the sign-in screen.",
            aboutLogoutConfirmationMessage(null),
        )
        assertEquals(
            "Are you sure you want to log out? Playback will stop and you will return to the sign-in screen.",
            aboutLogoutConfirmationMessage("   "),
        )
    }
}

private val RELEASE =
    AppRelease(
        version = "1.3.0",
        apkUrl = "https://example.com/tuneflow-tv.apk",
        apkSizeBytes = 100L,
        sha256 = null,
    )
