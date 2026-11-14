package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V167SettingsNavigationPolicyTest {
    @Test fun homeHasSevenUniqueDestinations() {
        assertEquals(7, V167SettingsNavigationPolicy.homeSections.size)
        assertEquals(7, V167SettingsNavigationPolicy.homeSections.toSet().size)
        assertEquals("Account", V167SettingsNavigationPolicy.homeSections.first())
        assertEquals("Advanced", V167SettingsNavigationPolicy.homeSections.last())
        assertEquals(
            listOf("Account", "Appearance & sound", "Notifications & alarms",
                "Planning & integrations", "Privacy & permissions",
                "Help & about", "Advanced"),
            V167SettingsNavigationPolicy.homeSections,
        )
    }

    @Test fun customizationSubpagesReturnToPersonalizationInsteadOfHome() {
        val pages = listOf(
            V167SettingsSection.APPEARANCE,
            V167SettingsSection.GUIDE,
            V167SettingsSection.VOICE,
        )
        assertTrue(pages.all { V167SettingsNavigationPolicy.parent(it) == V167SettingsSection.PERSONALIZATION })
        assertEquals(V167SettingsSection.HOME, V167SettingsNavigationPolicy.parent(V167SettingsSection.PERSONALIZATION))
    }

    @Test fun majorSectionsReturnHomeAndCorruptNavigationStateIsSafe() {
        listOf(
            V167SettingsSection.REMINDERS,
            V167SettingsSection.PLANNING,
            V167SettingsSection.ABOUT,
            V167SettingsSection.PRIVACY,
            V167SettingsSection.ADVANCED,
        ).forEach { assertEquals(V167SettingsSection.HOME, V167SettingsNavigationPolicy.parent(it)) }
        assertEquals(V167SettingsSection.HOME, V167SettingsNavigationPolicy.fromSavedName("DATA"))
        assertEquals(V167SettingsSection.HOME, V167SettingsNavigationPolicy.fromSavedName("CONNECTIONS"))
        assertEquals(V167SettingsSection.HOME, V167SettingsNavigationPolicy.fromSavedName("unknown"))
        assertEquals(V167SettingsSection.VOICE, V167SettingsNavigationPolicy.fromSavedName("VOICE"))
    }
}
