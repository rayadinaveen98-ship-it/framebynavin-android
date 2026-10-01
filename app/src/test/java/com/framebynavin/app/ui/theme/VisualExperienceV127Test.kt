package com.framebynavin.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `v148 exposes only Directors Cut and Light`() {
        assertEquals(
            listOf(FrameTheme.DIRECTORS_CUT, FrameTheme.BACKLOT_LIGHT),
            BacklotSelectableAppearances,
        )
        assertTrue(BacklotSelectableAppearances.all { it.selectable })
        assertEquals(2, BacklotSelectableAppearances.map { it.displayName }.distinct().size)
    }

    @Test
    fun `classic Directors Cut stays black red and default safe`() {
        val director = FrameTheme.DIRECTORS_CUT
        assertEquals(Color(0xFF070707), director.palette.background)
        assertEquals(Color(0xFFE94A45), director.palette.primary)
        assertEquals(Color(0xFFF4F0E8), director.palette.foreground)
        assertEquals(FrameVisualLanguage.CINEMATIC, director.profile.visualLanguage)
        assertFalse(director.palette.isLight)
        assertTrue(director.selectable)
    }

    @Test
    fun `Light is a true light translation of Directors Cut`() {
        val dark = FrameTheme.DIRECTORS_CUT
        val light = FrameTheme.BACKLOT_LIGHT
        assertTrue(light.palette.isLight)
        assertTrue(light.selectable)
        assertNotEquals(dark.palette.background, light.palette.background)
        assertNotEquals(dark.palette.foreground, light.palette.foreground)
        assertEquals(dark.profile, light.profile)
        assertEquals(FrameVisualLanguage.CINEMATIC, light.profile.visualLanguage)
    }

    @Test
    fun `retired theme ids remain migration only and cannot be selected`() {
        val retired = FrameTheme.entries.filterNot { it in BacklotSelectableAppearances }
        assertEquals(9, retired.size)
        assertTrue(retired.all { !it.selectable })
        assertTrue(retired.all { it.palette == FrameTheme.DIRECTORS_CUT.palette })
        assertTrue(retired.all { it.profile == FrameTheme.DIRECTORS_CUT.profile })
    }

    @Test
    fun `both supported palettes preserve readable semantic separation`() {
        BacklotSelectableAppearances.forEach { theme ->
            assertNotEquals(theme.palette.background, theme.palette.foreground)
            assertNotEquals(theme.palette.surface, theme.palette.primary)
            assertNotEquals(theme.palette.primary, theme.palette.secondary)
            assertTrue(theme.profile.cardRadius.value > 0f)
            assertTrue(theme.profile.sectionGap.value > 0f)
        }
    }
}
