package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class V310LinkTargetsTest {
    private val spaces = setOf("space-one", "space-two")
    private val groups = setOf("group-one", "group-two")

    @Test fun emptySelectionIsValidAndDoesNotClaimAnyLinks() {
        val choice = V310LinkTargets().checkedAgainst(emptySet(), emptySet())
        assertFalse(choice.hasLinks)
        assertEquals(null, choice.spaceId)
        assertEquals(null, choice.groupId)
    }

    @Test fun oneSpaceAndOneGroupAreOptionalIndependently() {
        assertTrue(V310LinkTargets(spaceId = "space-one").checkedAgainst(spaces, groups).hasLinks)
        assertTrue(V310LinkTargets(groupId = "group-one").checkedAgainst(spaces, groups).hasLinks)
    }

    @Test fun bothSelectionsCanBeValidatedBeforeReminderWrite() {
        val expected = V310LinkTargets("space-two", "group-one")
        assertEquals(expected, expected.checkedAgainst(spaces, groups))
    }

    @Test fun deletedSpaceMustFailClosedBeforeSave() {
        try {
            V310LinkTargets(spaceId = "stale").checkedAgainst(spaces, groups)
            fail("Stale Space was accepted")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Space"))
        }
    }

    @Test fun deletedGroupMustFailClosedBeforeSave() {
        try {
            V310LinkTargets(groupId = "stale").checkedAgainst(spaces, groups)
            fail("Stale Group was accepted")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Group"))
        }
    }

    @Test fun zeroGroupsDoesNotBlockValidSpaceSelection() {
        assertEquals("space-one",
            V310LinkTargets(spaceId = "space-one")
                .checkedAgainst(spaces, emptySet()).spaceId)
    }

    @Test fun immutableSelectionDoesNotModifyOriginal() {
        val initial = V310LinkTargets(spaceId = "space-one")
        val another = initial.copy(groupId = "group-two")
        assertEquals(null, initial.groupId)
        assertEquals("group-two", another.groupId)
    }
}
