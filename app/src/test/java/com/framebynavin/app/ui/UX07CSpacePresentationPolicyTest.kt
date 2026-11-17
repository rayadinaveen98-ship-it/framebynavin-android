package com.framebynavin.app.ui

import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * UX07C regression source only. Execution intentionally deferred to the
 * founder-approved unified UX01–UX10 certification campaign.
 */
class UX07CSpacePresentationPolicyTest {
    @Test
    fun `saved Space types retain their original user-facing names`() {
        V289SpaceType.entries.forEach { type ->
            assertEquals(type.defaultTitle, UX07CSpaceLabels.type(type.name))
        }
        assertEquals("Personal Space", UX07CSpaceLabels.type("UNKNOWN_FUTURE_TYPE"))
    }

    @Test
    fun `manual notes metrics and check ins remain different activities`() {
        val manual = V289SpaceEntrySource.MANUAL.name
        assertEquals("PERSONAL NOTE", UX07CSpaceLabels.entry(V289SpaceEntryKind.NOTE.name, manual))
        assertEquals("MANUAL CHECK-IN", UX07CSpaceLabels.entry(V289SpaceEntryKind.CHECK_IN.name, manual))
        assertEquals("MANUAL MEASUREMENT", UX07CSpaceLabels.entry(V289SpaceEntryKind.METRIC.name, manual))
    }

    @Test
    fun `explicit owner Done provenance is never misrepresented as manual activity`() {
        assertEquals(
            "CONFIRMED REMINDER DONE",
            UX07CSpaceLabels.entry(
                V289SpaceEntryKind.CHECK_IN.name,
                V289SpaceEntrySource.CONFIRMED_REMINDER_DONE.name,
            ),
        )
    }
}
