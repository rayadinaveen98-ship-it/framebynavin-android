package com.framebynavin.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `visual experience ships six distinct selectable themes`() {
        assertEquals(6, FrameTheme.entries.size)
        assertEquals(6, FrameTheme.entries.map { it.displayName }.distinct().size)
        assertEquals(6, FrameTheme.entries.map { it.palette.primary }.distinct().size)
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
    fun `ivory studio is the light editorial theme`() {
        assertTrue(FrameTheme.IVORY_STUDIO.palette.isLight)
        assertEquals(FrameSurfacePersonality.EDITORIAL, FrameTheme.IVORY_STUDIO.palette.surfacePersonality)
        FrameTheme.entries.filter { it != FrameTheme.IVORY_STUDIO }.forEach {
            assertFalse(it.palette.isLight)
        }
    }

    @Test
    fun `aurora glass uses a distinct glass personality`() {
        assertEquals(FrameSurfacePersonality.GLASS, FrameTheme.AURORA_GLASS.palette.surfacePersonality)
        assertFalse(FrameTheme.AURORA_GLASS.palette.isLight)
        assertNotEquals(FrameTheme.AURORA_GLASS.palette.primary, FrameTheme.AURORA_GLASS.palette.tertiary)
    }
}
