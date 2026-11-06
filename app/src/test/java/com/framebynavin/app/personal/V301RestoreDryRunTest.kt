package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V301RestoreDryRunTest {
    private val spaceId = "12537c2d-521d-40db-bf25-2d79bf0f1c2a"
    private val photoId = "ecb00ac2-134d-4195-b80b-7a41111a3021"

    private fun reminder(
        id: String = "owner-reminder",
        title: String = "Personal plan",
    ) = PersonalReminder(
        id = id,
        title = title,
        startDate = LocalDate.of(2026, 11, 12),
        time = LocalTime.of(9, 30),
        createdAtMillis = 100L,
        updatedAtMillis = 100L,
    )

    private fun photo(note: String = "") = V290SpacePhotoEntity(
        id = photoId,
        spaceId = spaceId,
        takenAtMillis = 100L,
        addedAtMillis = 120L,
        fileName = "$photoId.jpg",
        sha256Hex = "a".repeat(64),
        widthPx = 500,
        heightPx = 700,
        note = note,
    )

    private fun bundle(
        reminders: List<PersonalReminder> = emptyList(),
        completions: List<PersonalCompletionEvent> = emptyList(),
        groups: List<V298ReminderGroup> = emptyList(),
        spaces: V289SpaceState = V289SpaceState(),
    ) = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(reminders, completions),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(groups),
        spaces = V289SpacesMetadataCodec.encode(spaces),
    )

    private fun local(
        data: V300PersonalComponents = bundle(),
        photos: List<V290SpacePhotoEntity> = emptyList(),
        verified: Boolean = true,
    ) = V301LocalRecoverySnapshot(data, photos, verified)

    @Test fun incomingRecordsAreListedAsAddsOnEmptyPhoneWithoutAuthorization() {
        val input = bundle(reminders = listOf(reminder()))
        val report = V301RestoreDryRun.compare(local(), input, emptyList())
        assertEquals(1, report.reminders.newRecords)
        assertEquals(0, report.conflicts)
        assertEquals(1, report.adds)
        assertFalse(report.requiresConflictReview)
        assertFalse(report.approvedToWrite)
        assertTrue(report.warning.contains("preview only"))
    }

    @Test fun exactSameIdAndValueIsIdempotentNotAConflict() {
        val data = bundle(reminders = listOf(reminder()))
        val report = V301RestoreDryRun.compare(local(data), data, emptyList())
        assertEquals(1, report.reminders.identical)
        assertEquals(0, report.reminders.newRecords)
        assertEquals(0, report.reminders.conflicting)
        assertFalse(report.approvedToWrite)
    }

    @Test fun sameIdButDifferentOwnerTitleIsNeverSilentlyOverwritten() {
        val onPhone = bundle(reminders = listOf(reminder(title = "Keep my phone version")))
        val archived = bundle(reminders = listOf(reminder(title = "Archived version")))
        val report = V301RestoreDryRun.compare(local(onPhone), archived, emptyList())
        assertEquals(1, report.reminders.conflicting)
        assertTrue(report.requiresConflictReview)
        assertTrue(report.warning.contains("No automatic replacement"))
        assertFalse(report.approvedToWrite)
    }

    @Test fun distinctExistingRecordsAreLocalOnlyAndMustBePreserved() {
        val onPhone = bundle(reminders = listOf(reminder(id = "keep-this")))
        val archived = bundle(reminders = listOf(reminder(id = "import-this")))
        val report = V301RestoreDryRun.compare(local(onPhone), archived, emptyList())
        assertEquals(1, report.reminders.localOnly)
        assertEquals(1, report.reminders.newRecords)
        assertEquals(0, report.conflicts)
        assertFalse(report.approvedToWrite)
    }

    @Test fun doneReceiptsAreComparedByExactOccurrenceNotReminderOnly() {
        val r = reminder()
        val onPhone = bundle(listOf(r),
            completions = listOf(PersonalCompletionEvent(r.id, 2000L, 2200L)))
        val archived = bundle(listOf(r),
            completions = listOf(PersonalCompletionEvent(r.id, 2000L, 2300L)))
        val report = V301RestoreDryRun.compare(local(onPhone), archived, emptyList())
        assertEquals(1, report.completedActions.conflicting)
        assertFalse(report.approvedToWrite)
    }

    @Test fun independentGroupIdentityCollisionRequiresReview() {
        val r = reminder()
        val stored = V298ReminderGroup(
            id = "group-id", name = "Daily",
            reminderIds = listOf(r.id), createdAtMillis = 100L,
            updatedAtMillis = 100L,
        )
        val archived = stored.copy(name = "Renamed elsewhere")
        val onPhone = bundle(listOf(r), groups = listOf(stored))
        val source = bundle(listOf(r), groups = listOf(archived))
        val report = V301RestoreDryRun.compare(local(onPhone), source, emptyList())
        assertEquals(1, report.groups.conflicting)
        assertEquals(1, report.reminders.identical)
    }

    @Test fun localPhotoIntegrityUnknownAlwaysPreventsRestoreEvenWithoutCollisions() {
        val space = V289PersonalSpace(spaceId, "My Space", V289SpaceType.CUSTOM,
            createdAtMillis = 100L)
        val archive = bundle(spaces = V289SpaceState(spaces = listOf(space)))
        val report = V301RestoreDryRun.compare(
            local(archive, photos = listOf(photo()), verified = false),
            archive, listOf(photo()),
        )
        assertEquals(1, report.photos.identical)
        assertTrue(report.requiresConflictReview)
        assertTrue(report.warning.contains("not been fully verified"))
    }

    @Test fun samePhotoIdWithDifferentMetadataIsAConflictNotSilentReplacement() {
        val space = V289PersonalSpace(spaceId, "My Space", V289SpaceType.CUSTOM,
            createdAtMillis = 100L)
        val archive = bundle(spaces = V289SpaceState(spaces = listOf(space)))
        val report = V301RestoreDryRun.compare(
            local(archive, photos = listOf(photo(note = "Existing")), verified = true),
            archive, listOf(photo(note = "Archive")),
        )
        assertEquals(1, report.photos.conflicting)
        assertFalse(report.approvedToWrite)
    }

    @Test fun orphanIncomingGroupReminderLinkIsRejectedBeforePreview() {
        val orphan = V298ReminderGroup(
            id = "test", name = "Exercise",
            reminderIds = listOf("missing"), createdAtMillis = 100L,
            updatedAtMillis = 100L,
        )
        assertThrows(IllegalArgumentException::class.java) {
            V301RestoreDryRun.compare(local(), bundle(groups = listOf(orphan)), emptyList())
        }
    }

    @Test fun domainCountsRejectInconsistentTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            V301DomainDiff(proposed = 2, newRecords = 1, identical = 0,
                conflicting = 0, localOnly = 0)
        }
    }
}
