package com.framebynavin.app.ui

import com.framebynavin.app.ui.theme.FrameTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P9 source-staged scope tests; do not execute until founder's single
 * consolidated QA gate. Live screens and reminder callbacks require device QA.
 */
class P9AppearanceScopePolicyTest {
    @Test fun appAppearanceOffersOnlyOriginalDarkAndSupportedLight() {
        val themes = V137ThemeOrder
        assertEquals(2, themes.size)
        assertEquals(FrameTheme.DIRECTORS_CUT, themes[0])
        assertEquals(FrameTheme.BACKLOT_LIGHT, themes[1])
        assertFalse(themes.any { theme ->
            theme.displayName.contains("Collection", ignoreCase = true)
        })
    }

    @Test fun existingSettingsNavigationStillSupportsCompanionAndVoice() {
        assertEquals(V167SettingsSection.PERSONALIZATION,
            V167SettingsNavigationPolicy.parent(V167SettingsSection.APPEARANCE))
        assertEquals(V167SettingsSection.PERSONALIZATION,
            V167SettingsNavigationPolicy.parent(V167SettingsSection.GUIDE))
        assertEquals(V167SettingsSection.PERSONALIZATION,
            V167SettingsNavigationPolicy.parent(V167SettingsSection.VOICE))
    }

    @Test fun defaultSettingsNavigationNeverStartsOnAnInvalidPage() {
        assertEquals(V167SettingsSection.HOME,
            V167SettingsNavigationPolicy.fromSavedName("V391_REMINDER_SCENES"))
        assertEquals(V167SettingsSection.HOME,
            V167SettingsNavigationPolicy.fromSavedName("V394_PREMIERE"))
        assertTrue(V137ThemeOrder.contains(FrameTheme.DIRECTORS_CUT))
    }
}
