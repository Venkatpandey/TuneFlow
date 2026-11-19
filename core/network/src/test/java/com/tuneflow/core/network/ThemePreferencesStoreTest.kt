package com.tuneflow.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesStoreTest {
    @Test
    fun themeKey_scopedToProfileServerAndUsername() {
        val session1 =
            SessionData(
                serverUrl = "https://music.example.com/",
                username = "alice",
                token = "token1",
                salt = "salt1",
            )
        val session2 =
            SessionData(
                serverUrl = "https://music.example.com",
                username = "bob",
                token = "token2",
                salt = "salt2",
            )

        val key1 = ThemePreferencesStore.themeKey(session1)
        val key2 = ThemePreferencesStore.themeKey(session2)

        assertEquals("theme_id:https://music.example.com:alice", key1.name)
        assertEquals("theme_id:https://music.example.com:bob", key2.name)
    }

    @Test
    fun defaultThemeConstantIsDefault() {
        assertEquals("default", ThemePreferencesStore.DEFAULT_THEME_ID)
    }
}
