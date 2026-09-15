package com.framebynavin.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `v129 ships six distinct selectable themes without ivory`() {
        assertEquals(6, FrameTheme.entries.size)
        assertEquals(6, FrameTheme.entries.map { it.displayName }.distinct().size)
        assertEquals(6, FrameTheme.entries.map { it.palette.primary }.distinct().size)
        assertTrue(FrameTheme.entries.none { it.name == "IVORY_STUDIO" })
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
    fun `v129 theme system is dark first and lumen is structurally distinct`() {
        FrameTheme.entries.forEach { assertFalse(it.palette.isLight) }
        assertEquals(FrameSurfacePersonality.LUMEN, FrameTheme.LUMEN_FLOW.palette.surfacePersonality)
        assertEquals(FrameSurfacePersonality.GLASS, FrameTheme.AURORA_GLASS.palette.surfacePersonality)
    }
}
