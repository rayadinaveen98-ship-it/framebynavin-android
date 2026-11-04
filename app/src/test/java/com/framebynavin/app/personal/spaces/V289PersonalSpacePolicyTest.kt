package com.framebynavin.app.personal.spaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V289 domain tests: no Android storage, notification, cloud or Creator calls. */
class V289PersonalSpacePolicyTest {
    private val policy = V289PersonalSpacePolicy
    private val startedAt = 1_792_050_000_000L
    private val skin = policy.newSpace(
        V289SpaceType.SKIN_CARE, startedAt,
        "10000000-0000-4000-8000-000000000001"
    )
    private val editing = policy.newSpace(
        V289SpaceType.EDITING, startedAt,
        "10000000-0000-4000-8000-000000000002"
    )
    private val reminder = "life21-v1-skin-am"

    private fun entry(
        id: String,
        spaceId: String = skin.id,
        source: V289SpaceEntrySource = V289SpaceEntrySource.MANUAL,
        reminderId: String? = null,
        occurrenceMillis: Long? = null,
    ) = V289SpaceEntry(
        id = id,
        spaceId = spaceId,
        kind = V289SpaceEntryKind.CHECK_IN,
        source = source,
        occurredAtMillis = startedAt + 3000L,
        createdAtMillis = startedAt + 4000L,
        reminderId = reminderId,
        originalOccurrenceMillis = occurrenceMillis,
    )

    @Test fun presetsAreExplicitAndCreateNoReminder() {
        val state = policy.addSpace(V289SpaceState(), skin)
        assertEquals(1, state.spaces.size)
        assertEquals("Skin Care", state.spaces.single().title)
        assertTrue(state.links.isEmpty())
        assertTrue(state.entries.isEmpty())
        assertEquals(7, V289SpaceType.entries.size)
    }

    @Test fun identifiersNamesAndTimestampsAreValidated() {
        assertTrue(runCatching { policy.validateSpace(skin.copy(id = "not-a-uuid")) }.isFailure)
        assertTrue(runCatching { policy.validateSpace(skin.copy(title = "  Skin Care")) }.isFailure)
        assertTrue(runCatching { policy.validateSpace(skin.copy(title = "x".repeat(81))) }.isFailure)
        assertTrue(runCatching { policy.validateSpace(skin.copy(iconKey = "../secret")) }.isFailure)
        assertTrue(runCatching { policy.validateSpace(skin.copy(createdAtMillis = 0)) }.isFailure)
        assertTrue(runCatching { policy.archive(skin, startedAt - 1) }.isFailure)
        assertTrue(runCatching { policy.addSpace(policy.addSpace(V289SpaceState(), skin), skin) }.isFailure)
    }

    @Test fun reminderLinksAreIdempotentAndDoNotTouchExternalReminder() {
        val base = policy.addSpace(policy.addSpace(V289SpaceState(), skin), editing)
        val link = V289SpaceReminderLink(skin.id, reminder, startedAt)
        val once = policy.addLink(base, link)
        val twice = policy.addLink(once, link)
        assertEquals(1, twice.links.size)
        val inSecondSpace = policy.addLink(twice, link.copy(spaceId = editing.id))
        assertEquals(2, inSecondSpace.links.size)
        assertEquals(listOf(reminder, reminder), inSecondSpace.links.map { it.reminderId })
        assertTrue(runCatching {
            policy.addLink(V289SpaceState(), link)
        }.isFailure)
    }

    @Test fun manualJournalIsNotInferredFromLinkedReminders() {
        val linked = policy.addLink(policy.addSpace(V289SpaceState(), skin),
            V289SpaceReminderLink(skin.id, reminder, startedAt))
        assertEquals(0, policy.overviews(linked).single().manualEntries)
        assertEquals(0, policy.overviews(linked).single().confirmedDoneEntries)
        val logged = policy.addEntry(linked, entry("20000000-0000-4000-8000-000000000001"))
        assertEquals(1, policy.overviews(logged).single().manualEntries)
        assertEquals(0, policy.overviews(logged).single().confirmedDoneEntries)
        assertEquals(1, policy.overviews(logged).single().linkedReminderCount)
        assertTrue(runCatching {
            policy.addEntry(linked, entry("20000000-0000-4000-8000-000000000002",
                reminderId = reminder))
        }.isFailure)
    }

    @Test fun confirmedDoneNeedsImmutableReceiptAndIsNotDoubleCounted() {
        val base = policy.addSpace(V289SpaceState(), skin)
        val receipt = entry("20000000-0000-4000-8000-000000000001",
            source = V289SpaceEntrySource.CONFIRMED_REMINDER_DONE,
            reminderId = reminder, occurrenceMillis = startedAt)
        assertTrue(runCatching {
            policy.addEntry(base, receipt.copy(originalOccurrenceMillis = null))
        }.isFailure)
        assertTrue(runCatching {
            policy.addEntry(base, receipt.copy(kind = V289SpaceEntryKind.NOTE))
        }.isFailure)
        val once = policy.addEntry(base, receipt)
        val repeatedReceipt = receipt.copy(id = "20000000-0000-4000-8000-000000000002")
        val twice = policy.addEntry(once, repeatedReceipt)
        assertEquals(1, twice.entries.size)
        assertEquals(1, policy.overviews(twice).single().confirmedDoneEntries)
        assertEquals(0, policy.overviews(twice).single().manualEntries)
    }

    @Test fun metricFieldsRequireValidNumbersAndUnits() {
        val valid = entry("20000000-0000-4000-8000-000000000003").copy(
            kind = V289SpaceEntryKind.METRIC,
            metricName = "Walking", metricValue = 30.0, metricUnit = "minutes"
        )
        assertEquals(valid, policy.validateEntry(valid))
        assertTrue(runCatching {
            policy.validateEntry(valid.copy(metricValue = Double.NaN))
        }.isFailure)
        assertTrue(runCatching {
            policy.validateEntry(valid.copy(metricValue = Double.POSITIVE_INFINITY))
        }.isFailure)
        assertTrue(runCatching {
            policy.validateEntry(valid.copy(metricUnit = null))
        }.isFailure)
        assertTrue(runCatching {
            policy.validateEntry(valid.copy(note = "x".repeat(2001)))
        }.isFailure)
    }

    @Test fun archiveKeepsEntriesAndDeleteOnlyRemovesThatSpaceData() {
        val base = policy.addSpace(policy.addSpace(V289SpaceState(), skin), editing)
        val a = policy.addEntry(base, entry("20000000-0000-4000-8000-000000000001"))
        val b = policy.addEntry(a, entry("20000000-0000-4000-8000-000000000002",
            spaceId = editing.id))
        val linked = policy.addLink(b, V289SpaceReminderLink(skin.id, reminder, startedAt))
        val archived = linked.copy(spaces = linked.spaces.map {
            if (it.id == skin.id) policy.archive(it, startedAt + 5000L) else it
        })
        assertEquals(1, policy.overviews(archived).last().manualEntries)
        assertEquals(archived.spaces.last(), policy.unarchive(archived.spaces.last()))
        val deleted = policy.deleteSpace(archived, skin.id)
        assertEquals(listOf(editing.id), deleted.spaces.map { it.id })
        assertEquals(listOf(editing.id), deleted.entries.map { it.spaceId })
        assertTrue(deleted.links.isEmpty())
        assertFalse(deleted.entries.isEmpty())
        assertTrue(runCatching { policy.deleteSpace(deleted, skin.id) }.isFailure)
        // No call to PersonalReminderStore.delete; the linked reminder still exists externally.
    }
}
