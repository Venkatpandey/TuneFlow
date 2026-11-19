package com.tuneflow.tv

import androidx.compose.ui.graphics.Color
import com.tuneflow.core.design.TuneFlowThemeId
import com.tuneflow.core.design.TuneFlowThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppSettingsDialogTest {
    @Test
    fun themeResolution_defaultTheme() {
        val theme = TuneFlowThemes.fromId("default")
        assertEquals(TuneFlowThemeId.Default, theme.id)
        assertEquals("Default", theme.displayName)
    }

    @Test
    fun themeResolution_plainDarkTheme() {
        val theme = TuneFlowThemes.fromId("plain_dark")
        assertEquals(TuneFlowThemeId.PlainDark, theme.id)
        assertEquals("Plain Dark", theme.displayName)
        assertEquals(Color(0xFF000000), theme.colorScheme.background)
        assertEquals(Color(0xFFFFFFFF), theme.colorScheme.onBackground)
    }

    @Test
    fun themeResolution_bananabobTheme() {
        val theme = TuneFlowThemes.fromId("bananabob")
        assertEquals(TuneFlowThemeId.BananaBob, theme.id)
        assertEquals("bananabob", theme.displayName)
        // User requested: #990000 background, white text, and #333333 hi-lights
        assertEquals(Color(0xFF990000), theme.colorScheme.background)
        assertEquals(Color(0xFFFFFFFF), theme.colorScheme.onBackground)
        assertEquals(Color(0xFF333333), theme.colorScheme.surfaceVariant)
        assertEquals(Color(0xFF333333), theme.colorScheme.primaryContainer)
    }

    @Test
    fun themeResolution_matrixTheme() {
        val theme = TuneFlowThemes.fromId("matrix")
        assertEquals(TuneFlowThemeId.Matrix, theme.id)
        assertEquals("Matrix", theme.displayName)
        assertEquals(Color(0xFF000000), theme.colorScheme.background)
        assertEquals(Color(0xFF00FF66), theme.colorScheme.onBackground)
        assertEquals(Color(0xFF00FF66), theme.colorScheme.primary)
    }

    @Test
    fun themeResolution_caseInsensitiveAndFallback() {
        assertEquals(TuneFlowThemeId.Matrix, TuneFlowThemes.fromId("MATRIX").id)
        assertEquals(TuneFlowThemeId.BananaBob, TuneFlowThemes.fromId("BANANABOB").id)
        assertEquals(TuneFlowThemeId.PlainDark, TuneFlowThemes.fromId("PLAIN_DARK").id)
        assertEquals(TuneFlowThemeId.Default, TuneFlowThemes.fromId("unknown_xyz").id)
        assertEquals(TuneFlowThemeId.Default, TuneFlowThemes.fromId(null).id)
    }

    @Test
    fun allAvailableThemesArePresentInCuratedList() {
        val allThemes = TuneFlowThemes.all
        assertEquals(4, allThemes.size)
        assertNotNull(allThemes.find { it.id == TuneFlowThemeId.Default })
        assertNotNull(allThemes.find { it.id == TuneFlowThemeId.PlainDark })
        assertNotNull(allThemes.find { it.id == TuneFlowThemeId.BananaBob })
        assertNotNull(allThemes.find { it.id == TuneFlowThemeId.Matrix })
    }
}
