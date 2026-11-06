package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Complete target planning is pure; no app Context or live restore methods. */
class StabilizationRestoreTargetPolicyTest {
    private val firstSpace = "12537c2d-521d-40db-bf25-2d79bf0f1c2a"
    private val otherSpace = "22537c2d-521d-40db-bf25-2d79bf0f1c2a"
    private val firstPhoto = "ecb00ac2-134d-4195-b80b-7a41111a3021"
    private val otherPhoto = "fcb00ac2-134d-4195-b80b-7a41111a3021"

    private fun reminder(id: String = "keep", title: String = id) = PersonalReminder(
        id = id, title = title,
        startDate = LocalDate.of(2026, 11, 12),
        time = LocalTime.of(9, 30), createdAtMillis = 100L, updatedAtMillis = 100L,
    )
    private fun space(id: String = firstSpace) =
        V289PersonalSpace(id, "Owner Space", V289SpaceType.CUSTOM, createdAtMillis = 100L)
    private fun photo(id: String = firstPhoto, space: String = firstSpace,
                      fileName: String = "$id.jpg") = V290SpacePhotoEntity(
        id = id, spaceId = space, takenAtMillis = 100L,
        addedAtMillis = 120L, fileName = fileName,
        sha256Hex = "a".repeat(64), widthPx = 500,
        heightPx = 700, note = "Synthetic JPEG",
    )
    private fun bundle(
        reminders: List<PersonalReminder> = emptyList(),
        completions: List<PersonalCompletionEvent> = emptyList(),
        groups: List<V298ReminderGroup> = emptyList(),
        challenges: List<V297Challenge> = emptyList(),
        spaces: V289SpaceState = V289SpaceState(),
    ): V300PersonalComponents {
        val personal = PersonalCompletionBackupSnapshot(reminders, completions)
        return V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(reminders, completions),
            groups = V298ReminderGroupCodec.encode(groups),
            challenges = V297ChallengeCodec.encode(challenges),
            spaces = V303SpaceReceiptArchiveCodec.encode(spaces, personal),
        )
    }
    private fun local(data: V300PersonalComponents = bundle(),
                      photos: List<V290SpacePhotoEntity> = emptyList(),
                      verified: Boolean = true) =
        V301LocalRecoverySnapshot(data, photos, verified)

    @Test fun emptyPhoneRetainsAllEightIncomingDomainsWithoutAuthorizingWrites() {
        val r = reminder("new")
        val completed = PersonalCompletionEvent(r.id, 2000L, 2200L)
        val group = V298ReminderGroup(
            id = "group", name = "Custom Morning", reminderIds = listOf(r.id),
            createdAtMillis = 100L, updatedAtMillis = 100L)
        val challenge = V297Challenge(
            id = "challenge", title = "21 day plan",
            startDate = LocalDate.of(2026, 11, 1), createdAtMillis = 100L)
        val note = V289SpaceEntry(
            id = "f1cb520e-1980-4c02-847d-c05f23088201",
            spaceId = firstSpace, kind = V289SpaceEntryKind.NOTE,
            source = V289SpaceEntrySource.MANUAL,
            occurredAtMillis = 3000L, createdAtMillis = 3000L, note = "Private note")
        val incoming = bundle(
            reminders = listOf(r), completions = listOf(completed),
            groups = listOf(group), challenges = listOf(challenge),
            spaces = V289SpaceState(
                spaces = listOf(space()),
                links = listOf(V289SpaceReminderLink(firstSpace, r.id, 100L)),
                entries = listOf(note),
            ),
        )
        val target = StabilizationRestoreTargetPolicy.calculate(
            local(), incoming, listOf(photo()))
        assertEquals(8, target.preview.adds)
        assertFalse(target.authorizedToWriteLive)
        assertNotEquals(target.startingDigest, target.targetDigest)
        val personal = PersonalCompletionBackupCodec.decode(target.merged.personal)
        assertEquals(listOf(r.id), personal.reminders.map { it.id })
        assertEquals(listOf(completed), personal.completions)
        assertEquals(listOf(group), V298ReminderGroupCodec.decode(target.merged.groups))
        assertEquals(listOf(challenge), V297ChallengeCodec.decode(target.merged.challenges))
        val resultingSpaces = V303SpaceReceiptArchiveCodec.decode(
            target.merged.spaces, personal)
        assertEquals(1, resultingSpaces.spaces.size)
        assertEquals(1, resultingSpaces.links.size)
        assertEquals(listOf(note), resultingSpaces.entries)
        assertEquals(listOf(photo()), target.photos)
    }

    @Test fun additiveMergePreservesEveryPhoneOnlyRecordAndItsCurrentAlarmLease() {
        val old = reminder("on-phone")
        val lease = old.copy(
            pendingOccurrenceMillis = 3000L, pendingFireAtMillis = 3500L,
            pendingToken = "bca17856-6da3-4cdf-84d0-e7a94a301ac3",
            presentedToken = "bca17856-6da3-4cdf-84d0-e7a94a301ac3",
            presentedAtMillis = 3600L,
        )
        val remote = reminder("from-archive")
        val target = StabilizationRestoreTargetPolicy.calculate(
            local(bundle(reminders = listOf(lease))),
            bundle(reminders = listOf(remote)), emptyList())
        val saved = PersonalCompletionBackupCodec.decode(target.merged.personal)
        assertEquals(listOf(lease, remote), saved.reminders)
        assertEquals(1, target.preview.reminders.localOnly)
        assertEquals(1, target.preview.reminders.newRecords)
    }

    @Test fun identicalPortableReminderKeepsOriginalOnPhoneLease() {
        val original = reminder()
        val lease = original.copy(
            pendingOccurrenceMillis = 3000L, pendingFireAtMillis = 3200L,
            pendingToken = "bca17856-6da3-4cdf-84d0-e7a94a301ac3")
        val target = StabilizationRestoreTargetPolicy.calculate(
            local(bundle(reminders = listOf(lease))),
            bundle(reminders = listOf(original)), emptyList())
        assertEquals(lease,
            PersonalCompletionBackupCodec.decode(target.merged.personal).reminders.single())
        assertEquals(0, target.preview.adds)
    }

    @Test fun incomingOccurrenceLeaseIsNeverInstalledOnNewPhone() {
        val original = reminder("new").copy(
            pendingOccurrenceMillis = 5000L, pendingFireAtMillis = 5100L,
            pendingToken = "bca17856-6da3-4cdf-84d0-e7a94a301ac3")
        val target = StabilizationRestoreTargetPolicy.calculate(
            local(), bundle(reminders = listOf(original)), emptyList())
        val recovered = PersonalCompletionBackupCodec.decode(
            target.merged.personal).reminders.single()
        assertTrue(recovered.pendingToken.isEmpty())
        assertEquals(0L, recovered.pendingOccurrenceMillis)
    }

    @Test fun conflictNeverReplacesDifferentExistingTitle() {
        val base = bundle(reminders = listOf(reminder(title = "Keep my version")))
        val incoming = bundle(reminders = listOf(reminder(title = "Remote version")))
        assertThrows(IllegalArgumentException::class.java) {
            StabilizationRestoreTargetPolicy.calculate(local(base), incoming, emptyList())
        }
        assertEquals("Keep my version",
            PersonalCompletionBackupCodec.decode(base.personal).reminders.single().title)
    }

    @Test fun unverifiedExistingPhotoPreventsTargetPlanning() {
        assertThrows(IllegalArgumentException::class.java) {
            StabilizationRestoreTargetPolicy.calculate(local(verified = false),
                bundle(), emptyList())
        }
    }

    @Test fun twoDifferentPhotoIdsCannotWriteSamePrivateFilePath() {
        val targetSpaces = V289SpaceState(spaces = listOf(space()))
        val incoming = bundle(spaces = targetSpaces)
        val first = photo()
        val alias = photo(id = otherPhoto, fileName = first.fileName)
        assertThrows(IllegalArgumentException::class.java) {
            StabilizationRestoreTargetPolicy.calculate(
                local(), incoming, listOf(first, alias))
        }
    }

    @Test fun rejectArchivePhotoWhoseSafeUuidFilenameBelongsToAnotherRecord() {
        val incoming = bundle(
            spaces = V289SpaceState(spaces = listOf(space())),
        )
        val spoofed = photo().copy(fileName = "$otherPhoto.jpg")
        assertThrows(IllegalArgumentException::class.java) {
            StabilizationRestoreTargetPolicy.calculate(
                local(), incoming, listOf(spoofed),
            )
        }
    }

    @Test fun stableRepeatedMergeIsIdempotentForExactlyIdenticalRecords() {
        val remote = bundle(reminders = listOf(reminder("saved")))
        val once = StabilizationRestoreTargetPolicy.calculate(local(), remote, emptyList())
        val twice = StabilizationRestoreTargetPolicy.calculate(
            local(once.merged), remote, emptyList())
        assertEquals(0, twice.preview.adds)
        assertEquals(once.targetDigest, twice.targetDigest)
        assertFalse(twice.authorizedToWriteLive)
    }

    @Test fun identicalOtherDomainGroupMustRemainUnchanged() {
        val r = reminder()
        val localGroup = V298ReminderGroup(
            id = "existing", name = "Rhythm",
            reminderIds = listOf(r.id), createdAtMillis = 100L, updatedAtMillis = 100L)
        val onPhone = bundle(reminders = listOf(r), groups = listOf(localGroup))
        val incoming = bundle(reminders = listOf(r), groups = listOf(localGroup))
        val target = StabilizationRestoreTargetPolicy.calculate(
            local(onPhone), incoming, emptyList())
        assertEquals(listOf(localGroup),
            V298ReminderGroupCodec.decode(target.merged.groups))
        assertEquals(0, target.preview.adds)
    }
}
