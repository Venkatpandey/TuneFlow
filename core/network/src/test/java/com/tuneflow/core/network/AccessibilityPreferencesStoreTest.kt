package com.tuneflow.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityPreferencesStoreTest {
    @Test
    fun defaultMotionPreferenceIsSystem() {
        assertEquals(MotionPreference.System, AccessibilityPreferencesStore.DEFAULT_MOTION_PREFERENCE)
    }

    @Test
    fun motionPreference_fromRawResolution() {
        assertEquals(MotionPreference.System, MotionPreference.fromRaw("system"))
        assertEquals(MotionPreference.Always, MotionPreference.fromRaw("always"))
        assertEquals(MotionPreference.Never, MotionPreference.fromRaw("never"))
        assertEquals(MotionPreference.Always, MotionPreference.fromRaw("ALWAYS"))
        assertEquals(MotionPreference.System, MotionPreference.fromRaw(null))
        assertEquals(MotionPreference.System, MotionPreference.fromRaw("unknown"))
    }
}
