package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V303SpaceReceiptArchiveCodecTest {
    private val space = V289PersonalSpace(
        id = "166d863c-4b49-4d58-b977-fb8ae1ba8829",
        title = "My habit",
        type = V289SpaceType.CUSTOM,
        createdAtMillis = 1000L,
    )
    private val reminder = PersonalReminder(
        id = "owner-reminder",
        title = "Apply sunscreen",
        startDate = LocalDate.of(2026, 11, 12),
        time = LocalTime.of(9, 0),
        createdAtMillis = 1000L,
        updatedAtMillis = 1000L,
    )
    private val receipt = PersonalCompletionEvent(
        reminderId = reminder.id,
        occurrenceMillis = 2_000L,
        doneAtMillis = 2_500L,
    )
    private val evidence = PersonalCompletionBackupSnapshot(
        listOf(reminder), listOf(receipt),
    )
    private val manual = V289SpaceEntry(
        id = "45d7c36f-5ff6-4e3a-9708-547b45b1a008",
        spaceId = space.id,
        kind = V289SpaceEntryKind.NOTE,
        source = V289SpaceEntrySource.MANUAL,
        occurredAtMillis = 1_800L,
        createdAtMillis = 1_900L,
        note = "Private daily note",
    )
    private val done = V289SpaceEntry(
        id = "422607ae-2d96-443c-bdd5-2cd820e36b2a",
        spaceId = space.id,
        kind = V289SpaceEntryKind.CHECK_IN,
        source = V289SpaceEntrySource.CONFIRMED_REMINDER_DONE,
        occurredAtMillis = receipt.doneAtMillis,
        createdAtMillis = receipt.doneAtMillis + 100L,
        reminderId = reminder.id,
        originalOccurrenceMillis = receipt.occurrenceMillis,
    )
    private val state = V289SpaceState(
        spaces = listOf(space), entries = listOf(manual, done),
    )

    @Test fun oldManualOnlyArchiveKeepsOriginalMetadataFormat() {
        val legacy = V289SpacesMetadataCodec.encode(
            V289SpaceState(spaces = listOf(space), entries = listOf(manual)),
        )
        val actual = V303SpaceReceiptArchiveCodec.encode(
            state.copy(entries = listOf(manual)), evidence,
        )
        assertEquals(legacy, actual)
        assertEquals(state.copy(entries = listOf(manual)),
            V303SpaceReceiptArchiveCodec.decode(legacy, evidence))
    }

    @Test fun exactConfirmedDoneProofRoundTripsWithOwnerNotes() {
        val raw = V303SpaceReceiptArchiveCodec.encode(state, evidence)
        assertEquals("backlot.personal.spaces.attested-done-entries",
            JSONObject(raw).getString("format"))
        val returned = V303SpaceReceiptArchiveCodec.decode(raw, evidence)
        assertEquals(state, returned)
        assertTrue(raw.contains("originalOccurrenceMillis"))
    }

    @Test fun noActualPersonalDoneReceiptBlocksConfirmedEntry() {
        val noDone = evidence.copy(completions = emptyList())
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.encode(state, noDone)
        }
        val fromValid = V303SpaceReceiptArchiveCodec.encode(state, evidence)
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.decode(fromValid, noDone)
        }
    }

    @Test fun mismatchedOccurrenceOrDoneTimeCannotForgeActivity() {
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.encode(
                state.copy(entries = listOf(done.copy(originalOccurrenceMillis = 2001L))),
                evidence,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.encode(
                state.copy(entries = listOf(done.copy(occurredAtMillis = 2600L))),
                evidence,
            )
        }
    }

    @Test fun repeatedDoneForSameSpaceAndOccurrenceIsNotDuplicated() {
        val other = done.copy(id = "9e61aaf3-75d7-4df1-a564-cb92d610313f")
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.encode(
                state.copy(entries = listOf(done, other)), evidence,
            )
        }
    }

    @Test fun mismatchedOrMissingReminderCannotClaimDone() {
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.encode(
                state.copy(entries = listOf(done.copy(reminderId = "missing"))),
                evidence,
            )
        }
    }

    @Test fun modifiedPayloadOrVersionIsRejected() {
        val raw = V303SpaceReceiptArchiveCodec.encode(state, evidence)
        val changed = JSONObject(raw).put("payload", "{}").toString()
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.decode(changed, evidence)
        }
        val unsupported = JSONObject(raw).put("schemaVersion", 2).toString()
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.decode(unsupported, evidence)
        }
    }

    @Test fun receiptForUnknownSpaceFailsBeforeExport() {
        val orphan = state.copy(entries = listOf(done.copy(
            spaceId = "ab9ab1e0-908b-431b-a95f-8c48a6399a7f",
        )))
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.encode(orphan, evidence)
        }
    }

    @Test fun duplicateJournalIdentityAcrossManualAndDoneIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            V303SpaceReceiptArchiveCodec.encode(
                state.copy(entries = listOf(manual.copy(id = done.id), done)), evidence,
            )
        }
    }
}
