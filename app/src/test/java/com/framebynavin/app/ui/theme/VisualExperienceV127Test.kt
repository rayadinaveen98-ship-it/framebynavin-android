package com.framebynavin.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `v139 ships ten distinct selectable visual languages`() {
        assertEquals(10, FrameTheme.entries.size)
        assertEquals(10, FrameTheme.entries.map { it.displayName }.distinct().size)
        assertEquals(10, FrameTheme.entries.map { it.palette.primary }.distinct().size)
        assertEquals(10, FrameTheme.entries.map { it.profile.visualLanguage }.distinct().size)
        assertTrue(FrameTheme.entries.any { it.displayName == "Studio Gold" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Ivory Atelier" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Paper Quiet" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Moss Studio" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Terracotta Calm" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Night Bloom" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Blue Hour" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Storyboard" })
    }

    @Test
    fun `classic Directors Cut stays black red and default safe`() {
        val director = FrameTheme.DIRECTORS_CUT
        assertEquals(Color(0xFF070707), director.palette.background)
        assertEquals(Color(0xFFE94A45), director.palette.primary)
        assertEquals(Color(0xFFF4F0E8), director.palette.foreground)
        assertEquals(FrameVisualLanguage.CINEMATIC, director.profile.visualLanguage)
        assertFalse(director.palette.isLight)
    }

    @Test
    fun `yellow v138 direction lives separately as Studio Gold`() {
        val studioGold = FrameTheme.STUDIO_GOLD
        assertEquals(Color(0xFFF4C430), studioGold.palette.primary)
        assertEquals(FrameVisualLanguage.LUXE, studioGold.profile.visualLanguage)
        assertNotEquals(FrameTheme.DIRECTORS_CUT.palette.primary, studioGold.palette.primary)
    }

    @Test
    fun `every palette preserves readable semantic separation`() {
        FrameTheme.entries.forEach { theme ->
            assertNotEquals(theme.palette.background, theme.palette.foreground)
            assertNotEquals(theme.palette.surface, theme.palette.primary)
            assertNotEquals(theme.palette.primary, theme.palette.secondary)
            assertTrue(theme.profile.cardRadius.value >= 0f)
            assertTrue(theme.profile.sectionGap.value > 0f)
        }
    }

    @Test
    fun `v139 spans light dark editorial organic and production moods`() {
        assertTrue(FrameTheme.LUMEN_FLOW.palette.isLight)
        assertTrue(FrameTheme.PAPER_QUIET.palette.isLight)
        assertTrue(FrameTheme.STORYBOARD.palette.isLight)
        assertEquals(FrameSurfacePersonality.EDITORIAL, FrameTheme.PAPER_QUIET.palette.surfacePersonality)
        assertEquals(FrameSurfacePersonality.EDITORIAL, FrameTheme.EMBER.palette.surfacePersonality)
        assertFalse(FrameTheme.MOSS_STUDIO.palette.isLight)
        assertFalse(FrameTheme.VIOLET_NEON.palette.isLight)
        assertEquals(FrameVisualLanguage.STORYBOARD, FrameTheme.STORYBOARD.profile.visualLanguage)
    }
}
