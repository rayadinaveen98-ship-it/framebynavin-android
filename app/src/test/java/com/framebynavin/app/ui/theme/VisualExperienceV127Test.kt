package com.framebynavin.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `v127 ships five distinct selectable themes`() {
        assertEquals(5, FrameTheme.entries.size)
        assertEquals(5, FrameTheme.entries.map { it.displayName }.distinct().size)
        assertEquals(5, FrameTheme.entries.map { it.palette.primary }.distinct().size)
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
    fun `ivory studio is light and cinematic themes remain dark`() {
        assertTrue(FrameTheme.IVORY_STUDIO.palette.isLight)
        FrameTheme.entries.filter { it != FrameTheme.IVORY_STUDIO }.forEach {
            assertFalse(it.palette.isLight)
        }
    }
}
