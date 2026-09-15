package com.framebynavin.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `v138 ships eight distinct selectable visual worlds`() {
        assertEquals(8, FrameTheme.entries.size)
        assertEquals(8, FrameTheme.entries.map { it.displayName }.distinct().size)
        assertEquals(8, FrameTheme.entries.map { it.palette.primary }.distinct().size)
        assertEquals(8, FrameTheme.entries.map { it.palette.background }.distinct().size)
        assertTrue(FrameTheme.entries.any { it.displayName == "Paper Quiet" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Moss Studio" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Terracotta Calm" })
        assertTrue(FrameTheme.entries.any { it.displayName == "Night Bloom" })
    }

    @Test
    fun `every palette preserves readable semantic separation`() {
        FrameTheme.entries.forEach { theme ->
            assertNotEquals(theme.palette.background, theme.palette.foreground)
            assertNotEquals(theme.palette.surface, theme.palette.primary)
            assertNotEquals(theme.palette.primary, theme.palette.secondary)
        }
    }

    @Test
    fun `v138 spans cinematic glass lumen editorial and natural moods`() {
        assertFalse(FrameTheme.DIRECTORS_CUT.palette.isLight)
        assertTrue(FrameTheme.LUMEN_FLOW.palette.isLight)
        assertTrue(FrameTheme.PAPER_QUIET.palette.isLight)
        assertEquals(FrameSurfacePersonality.GLASS, FrameTheme.AURORA_GLASS.palette.surfacePersonality)
        assertEquals(FrameSurfacePersonality.LUMEN, FrameTheme.LUMEN_FLOW.palette.surfacePersonality)
        assertEquals(FrameSurfacePersonality.EDITORIAL, FrameTheme.PAPER_QUIET.palette.surfacePersonality)
        assertEquals(FrameSurfacePersonality.EDITORIAL, FrameTheme.EMBER.palette.surfacePersonality)
        assertFalse(FrameTheme.MOSS_STUDIO.palette.isLight)
        assertFalse(FrameTheme.VIOLET_NEON.palette.isLight)
    }
}
