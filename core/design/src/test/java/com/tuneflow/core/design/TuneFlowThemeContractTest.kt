package com.tuneflow.core.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneFlowThemeContractTest {
    @Test
    fun fromIdResolvesRecognizedThemes() {
        assertEquals(TuneFlowThemeId.Default, TuneFlowThemeId.fromId("default"))
        assertEquals(TuneFlowThemeId.Default, TuneFlowThemeId.fromId("DEFAULT"))
        assertEquals(TuneFlowThemeId.PlainDark, TuneFlowThemeId.fromId("plain_dark"))
        assertEquals(TuneFlowThemeId.PlainDark, TuneFlowThemeId.fromId("PlainDark"))
        assertEquals(TuneFlowThemeId.BananaBob, TuneFlowThemeId.fromId("bananabob"))
        assertEquals(TuneFlowThemeId.BananaBob, TuneFlowThemeId.fromId("BananaBob"))
        assertEquals(TuneFlowThemeId.Matrix, TuneFlowThemeId.fromId("matrix"))
        assertEquals(TuneFlowThemeId.Matrix, TuneFlowThemeId.fromId("Matrix"))
    }

    @Test
    fun fromIdFallsBackToDefaultOnInvalidOrNull() {
        assertEquals(TuneFlowThemeId.Default, TuneFlowThemeId.fromId(null))
        assertEquals(TuneFlowThemeId.Default, TuneFlowThemeId.fromId(""))
        assertEquals(TuneFlowThemeId.Default, TuneFlowThemeId.fromId("unknown_theme"))
        assertEquals(TuneFlowThemes.Default, TuneFlowThemes.fromId(null))
        assertEquals(TuneFlowThemes.Default, TuneFlowThemes.fromId("non_existent"))
    }

    @Test
    fun bananaBobThemeConformsToRequestedSpecification() {
        val theme = TuneFlowThemes.BananaBob
        assertEquals("bananabob", theme.displayName)
        // #990000 background
        assertEquals(Color(0xFF990000), theme.colorScheme.background)
        assertEquals(Color(0xFF990000), theme.previewColors.background)
        // White text
        assertEquals(Color(0xFFFFFFFF), theme.colorScheme.onBackground)
        assertEquals(Color(0xFFFFFFFF), theme.colorScheme.onSurface)
        assertEquals(Color(0xFFFFFFFF), theme.previewColors.text)
        // #333333 highlights
        assertEquals(Color(0xFF333333), theme.colorScheme.surfaceVariant)
        assertEquals(Color(0xFF333333), theme.colorScheme.primaryContainer)
        assertEquals(Color(0xFF333333), theme.previewColors.highlight)
    }

    @Test
    fun plainDarkThemeConformsToSpecification() {
        val theme = TuneFlowThemes.PlainDark
        assertEquals("Plain Dark", theme.displayName)
        assertEquals(Color(0xFF000000), theme.colorScheme.background)
        assertEquals(Color(0xFFFFFFFF), theme.colorScheme.onBackground)
        assertEquals(Color(0xFFFFFFFF), theme.colorScheme.onSurface)
    }

    @Test
    fun everyThemeMeetsContrastTargets() {
        // WCAG AA requirement: normal text at least 4.5:1
        TuneFlowThemes.all.forEach { theme ->
            val scheme = theme.colorScheme
            val onBgContrast = calculateContrastRatio(scheme.onBackground, scheme.background)
            assertTrue(
                "Theme ${theme.id.rawId} onBackground contrast ($onBgContrast) should be >= 4.5:1",
                onBgContrast >= 4.5,
            )

            val onSurfaceContrast = calculateContrastRatio(scheme.onSurface, scheme.surface)
            assertTrue(
                "Theme ${theme.id.rawId} onSurface contrast ($onSurfaceContrast) should be >= 4.5:1",
                onSurfaceContrast >= 4.5,
            )

            val onPrimaryContrast = calculateContrastRatio(scheme.onPrimary, scheme.primary)
            assertTrue(
                "Theme ${theme.id.rawId} onPrimary contrast ($onPrimaryContrast) should be >= 4.5:1",
                onPrimaryContrast >= 4.5,
            )

            val onContainerContrast = calculateContrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)
            assertTrue(
                "Theme ${theme.id.rawId} onPrimaryContainer contrast ($onContainerContrast) should be >= 4.5:1",
                onContainerContrast >= 4.5,
            )
        }
    }

    @Test
    fun matrixThemeConformsToSpecification() {
        val theme = TuneFlowThemes.Matrix
        assertEquals("Matrix", theme.displayName)
        assertEquals(Color(0xFF000000), theme.colorScheme.background)
        assertEquals(Color(0xFF00FF66), theme.colorScheme.onBackground)
        assertEquals(Color(0xFF00FF66), theme.colorScheme.onSurface)
        assertEquals(Color(0xFF00FF66), theme.colorScheme.primary)
    }

    @Test
    fun themesListContainsAllCuratedThemes() {
        assertEquals(4, TuneFlowThemes.all.size)
        assertNotNull(TuneFlowThemes.all.find { it.id == TuneFlowThemeId.Default })
        assertNotNull(TuneFlowThemes.all.find { it.id == TuneFlowThemeId.PlainDark })
        assertNotNull(TuneFlowThemes.all.find { it.id == TuneFlowThemeId.BananaBob })
        assertNotNull(TuneFlowThemes.all.find { it.id == TuneFlowThemeId.Matrix })
    }
}
