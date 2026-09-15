package com.framebynavin.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `v137 ships six distinct selectable visual worlds`() {
        assertEquals(6, FrameTheme.entries.size)
        assertEquals(6, FrameTheme.entries.map { it.displayName }.distinct().size)
        assertEquals(6, FrameTheme.entries.map { it.palette.primary }.distinct().size)
        assertEquals(6, FrameTheme.entries.map { it.palette.background }.distinct().size)
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
    fun `v137 keeps a dark cinematic default and adds intentional light editorial themes`() {
        assertFalse(FrameTheme.DIRECTORS_CUT.palette.isLight)
        assertFalse(FrameTheme.MIDNIGHT.palette.isLight)
        assertFalse(FrameTheme.VIOLET_NEON.palette.isLight)
        assertFalse(FrameTheme.LUMEN_FLOW.palette.isLight)
        assertTrue(FrameTheme.EMBER.palette.isLight)
        assertTrue(FrameTheme.AURORA_GLASS.palette.isLight)
        assertEquals(FrameSurfacePersonality.EDITORIAL, FrameTheme.EMBER.palette.surfacePersonality)
        assertEquals(FrameSurfacePersonality.EDITORIAL, FrameTheme.AURORA_GLASS.palette.surfacePersonality)
    }
}
