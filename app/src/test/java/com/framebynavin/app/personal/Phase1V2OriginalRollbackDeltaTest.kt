package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

/**
 * Synthetic pure delta checks: no Personal, Room, image or H4 writes.
 * Every original owner identity must survive byte-for-byte in target.
 */
class Phase1V2OriginalRollbackDeltaTest {
    private val oldReminder = PersonalReminder(
        id = "owner-reminder", title = "Owner's reminder",
        startDate = LocalDate.of(2026, 11, 13), time = LocalTime.of(8, 30),
        createdAtMillis = 100L, updatedAtMillis = 100L,
    )
    private val importedReminder = oldReminder.copy(
        id = "new-reminder", title = "Imported reminder",
        createdAtMillis = 200L, updatedAtMillis = 200L,
    )
    private val oldHabit = Phase1HabitChallenge(
        id = "owner-21", activity = "Read",
        startDate = LocalDate.of(2026, 11, 7),
        zoneId = "Asia/Kolkata",
    ).confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
    private val incomingHabit = Phase1HabitChallenge(
        id = "imported-21", activity = "Walk",
        startDate = LocalDate.of(2026, 11, 7),
        zoneId = "Asia/Kolkata",
    )
    private val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
    private val space = V289PersonalSpace(
        id = spaceId, title = "New personal Space",
        type = V289SpaceType.CUSTOM, createdAtMillis = 200L,
    )
    private val photo = V290SpacePhotoEntity(
        id = "729d2bfa-af37-4836-af9e-c14c3d836421",
        spaceId = spaceId, takenAtMillis = 100L,
        addedAtMillis = 200L, fileName =
            "729d2bfa-af37-4836-af9e-c14c3d836421.jpg",
        sha256Hex = "f".repeat(64), widthPx = 64, heightPx = 64,
        note = "",
    )
    private val original = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(listOf(oldReminder), emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        singleActivityChallenges = Phase1HabitCodec.encode(listOf(oldHabit)),
    )
    private val merged = original.copy(
        personal = PersonalCompletionBackupCodec.encode(
            listOf(oldReminder, importedReminder), emptyList(),
        ),
        singleActivityChallenges =
            Phase1HabitCodec.encode(listOf(oldHabit, incomingHabit)),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState(spaces = listOf(space))),
    )
    private fun delta(
        prior: V300PersonalComponents = original,
        target: V300PersonalComponents = merged,
        oldPhotos: List<V290SpacePhotoEntity> = emptyList(),
        newPhotos: List<V290SpacePhotoEntity> = listOf(photo),
        originalDigest: String = Phase1V2RestoreFingerprint.calculate(
            prior, oldPhotos,
        ),
        mergedDigest: String = Phase1V2RestoreFingerprint.calculate(
            target, newPhotos,
        ),
    ) = Phase1V2OriginalRollbackDeltaPolicy.calculate(
        prior, target, oldPhotos, newPhotos, originalDigest, mergedDigest,
    )

    @Test fun exactImportedIdentitiesCanBeSeparatedFromOriginalChallengesAndReminders() {
        val plan = delta()
        assertEquals(setOf(importedReminder.id), plan.addedReminderIds)
        assertEquals(setOf(incomingHabit.id), plan.addedHabitIds)
        assertEquals(setOf(spaceId), plan.addedSpaceIds)
        assertEquals(setOf(photo.id), plan.addedPhotoIds)
        assertTrue(plan.addedDoneKeys.isEmpty())
        assertTrue(plan.addedLegacyChallengeIds.isEmpty())
        assertFalse(plan.mayWritePersonalData)
        assertFalse(plan.mayDeleteOriginalPhotoBytes)
        assertFalse(plan.mayReleaseH4)
        assertFalse(plan.mayScheduleAlarms)
        assertFalse(plan.addedHabitIds.contains(oldHabit.id))
    }

    @Test fun modifiedOwnerReminderOrConfirmed21DayReceiptMustBlockRollback() {
        val alteredPersonal = merged.copy(
            personal = PersonalCompletionBackupCodec.encode(
                listOf(oldReminder.copy(title = "changed"), importedReminder),
                emptyList(),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            delta(target = alteredPersonal)
        }
        val changedReceipt = oldHabit.copy(confirmations = mapOf(
            LocalDate.of(2026, 11, 7) to
                Instant.parse("2026-11-07T07:00:00Z"),
        ))
        val changedHabit = merged.copy(singleActivityChallenges =
            Phase1HabitCodec.encode(listOf(changedReceipt, incomingHabit)))
        assertThrows(IllegalArgumentException::class.java) {
            delta(target = changedHabit)
        }
    }

    @Test fun missingOriginalSpacePhotoOrWrongArchiveDigestCannotPass() {
        val originalWithSpace = original.copy(
            spaces = V289SpacesMetadataCodec.encode(V289SpaceState(
                spaces = listOf(space),
            )),
        )
        val originalPhoto = photo.copy(note = "Owner image")
        val modified = originalWithSpace.copy(personal =
            PersonalCompletionBackupCodec.encode(
                listOf(oldReminder, importedReminder), emptyList(),
            ))
        assertThrows(IllegalArgumentException::class.java) {
            delta(prior = originalWithSpace, target = modified,
                oldPhotos = listOf(originalPhoto), newPhotos = emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            delta(originalDigest = "0".repeat(64))
        }
        assertThrows(IllegalArgumentException::class.java) {
            delta(newPhotos = listOf(photo, photo))
        }
    }

    @Test fun noAdditionsAreValidAndNeverGrantLiveWrite() {
        val plan = delta(
            target = original, oldPhotos = emptyList(), newPhotos = emptyList(),
        )
        assertTrue(plan.addedReminderIds.isEmpty())
        assertTrue(plan.addedHabitIds.isEmpty())
        assertTrue(plan.addedSpaceIds.isEmpty())
        assertTrue(plan.addedPhotoIds.isEmpty())
        assertFalse(plan.mayWritePersonalData)
    }
}
