package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V298ReminderGroupTest {
    private fun group(id: String = "group-1") = V298ReminderGroup(
        id = id,
        name = "My custom routine",
        createdAtMillis = 100L,
        updatedAtMillis = 100L,
    ).checked()

    @Test fun groupsBeginEmptyAndNeverInstallDefaultSchedules() {
        val created = group()
        assertTrue(created.reminderIds.isEmpty())
        assertEquals("NEUTRAL", created.colorKey)
        assertEquals("CIRCLE", created.iconKey)
    }

    @Test fun linkingRequiresAnExistingReminderIdButDoesNotCopyIt() {
        val original = group()
        assertThrows(IllegalArgumentException::class.java) {
            original.linkExisting("missing", setOf("exists"), 200L)
        }
        val linked = original.linkExisting("exists", setOf("exists"), 200L)
        assertEquals(listOf("exists"), linked.reminderIds)
        assertEquals(original.name, linked.name)
        assertEquals(linked, linked.linkExisting("exists", setOf("exists"), 201L))
    }

    @Test fun linkAndUnlinkAreIndependentAcrossGroups() {
        val a = group("A").linkExisting("reminder", setOf("reminder"), 200L)
        val b = group("B").linkExisting("reminder", setOf("reminder"), 200L)
        val cleared = a.unlink("reminder", 210L)
        assertTrue(cleared.reminderIds.isEmpty())
        assertEquals(listOf("reminder"), b.reminderIds)
        assertEquals(cleared, cleared.unlink("reminder", 230L))
    }

    @Test fun renameOrDecorationNeverChangesGroupMembership() {
        val existing = group().linkExisting("id", setOf("id"), 200L)
        val edited = existing.renamed("  Reading  ", 220L)
            .personalized("BOOK", "BLUE", 221L)
        assertEquals("Reading", edited.name)
        assertEquals("BOOK", edited.iconKey)
        assertEquals("BLUE", edited.colorKey)
        assertEquals(listOf("id"), edited.reminderIds)
    }

    @Test fun codecRoundTripsAndDetectsAnyModifiedPayload() {
        val a = group().linkExisting("id", setOf("id"), 200L)
        val b = group("other").personalized("HEART", "GREEN", 201L)
        val raw = V298ReminderGroupCodec.encode(listOf(a,b))
        assertEquals(listOf(a,b), V298ReminderGroupCodec.decode(raw))
        assertThrows(IllegalArgumentException::class.java) {
            V298ReminderGroupCodec.decode(raw.replace("My custom routine", "Changed content"))
        }
    }

    @Test fun duplicateIdsMembershipAndInvalidAppearanceAreRejected() {
        val first = group()
        assertThrows(IllegalArgumentException::class.java) {
            V298ReminderGroupCodec.encode(listOf(first,first))
        }
        assertThrows(IllegalArgumentException::class.java) {
            first.copy(reminderIds = listOf("id","id")).checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            first.copy(iconKey = "UNKNOWN").checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            first.copy(name = " ").checked()
        }
    }

    @Test fun importAddsOnlyNewGroupIdsAndKeepsExistingGroupData() {
        val owner = group().linkExisting("saved", setOf("saved"), 200L)
        val incoming = listOf(
            owner.copy(name="Should never replace owner"),
            group("new"),
        )
        val merged = V298ReminderGroupMergePolicy.addMissing(listOf(owner),incoming)
        assertEquals(2,merged.size)
        assertEquals(owner,merged[0])
        assertEquals(group("new"),merged[1])
        assertFalse(merged[0].name.contains("replace"))
        assertThrows(IllegalArgumentException::class.java) {
            V298ReminderGroupMergePolicy.addMissing(listOf(owner),listOf(owner,owner))
        }
    }
}
