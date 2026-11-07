package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class B04NoPhotoRollbackDeltaPolicyTest {
    private val owner = PersonalReminder(
        id = "rollback-owner", title = "Preserved owner",
        startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(8, 30),
        createdAtMillis = 100L, updatedAtMillis = 100L,
    )
    private val incoming = owner.copy(
        id = "rollback-import", title = "Imported reminder",
        createdAtMillis = 200L, updatedAtMillis = 200L,
    )
    private val challenge = V297Challenge(
        id = "rollback-challenge", title = "Synthetic challenge",
        startDate = LocalDate.of(2026, 12, 12), createdAtMillis = 200L,
    )
    private val group = V298ReminderGroup(
        id = "rollback-group", name = "Synthetic group",
        reminderIds = listOf(incoming.id), createdAtMillis = 200L, updatedAtMillis = 200L,
    )
    private val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
    private val space = V289PersonalSpacePolicy.newSpace(V289SpaceType.FITNESS, 300L, spaceId)
    private val oldPersonal = PersonalCompletionBackupSnapshot(listOf(owner), emptyList())
    private val newPersonal = PersonalCompletionBackupSnapshot(
        listOf(owner, incoming), emptyList(),
    )
    private val old = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(
            oldPersonal.reminders, oldPersonal.completions),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V303SpaceReceiptArchiveCodec.encode(V289SpaceState(), oldPersonal),
    )
    private val target = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(
            newPersonal.reminders, newPersonal.completions),
        challenges = V297ChallengeCodec.encode(listOf(challenge)),
        groups = V298ReminderGroupCodec.encode(listOf(group)),
        spaces = V303SpaceReceiptArchiveCodec.encode(
            V289SpaceState(
                spaces = listOf(space),
                links = listOf(V289SpaceReminderLink(spaceId, incoming.id, 400L)),
            ), newPersonal,
        ),
    )
    private val originalDigest = V304RestorePlanPolicy.fingerprint(old, emptyList())
    private val targetDigest = V304RestorePlanPolicy.fingerprint(target, emptyList())
    private fun journal() = StabilizationRestoreJournalRecord(
        transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
        preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
        escrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
        originalDigest = originalDigest, targetDigest = targetDigest,
        step = StabilizationRestoreWriteStep.BEFORE_SPACES,
        createdAtMillis = 1000, updatedAtMillis = 1001,
    )
    private fun plan(
        before: V300PersonalComponents = old,
        after: V300PersonalComponents = target,
        j: StabilizationRestoreJournalRecord = journal(),
    ) = B04NoPhotoRollbackDeltaPolicy.calculate(
        j, before, after, emptyList(), emptyList(),
    )
    @Test fun addedIdentitiesArePreciselyAttributedWithoutOwnerDeletion() {
        val p = plan()
        assertEquals(setOf(incoming.id), p.newlyAddedReminderIds)
        assertTrue(p.newlyAddedCompletionKeys.isEmpty())
        assertEquals(setOf(challenge.id), p.newlyAddedChallengeIds)
        assertEquals(setOf(group.id), p.newlyAddedGroupIds)
        assertEquals(setOf(spaceId), p.newlyAddedSpaceIds)
        assertEquals(setOf(spaceId to incoming.id), p.newlyAddedSpaceLinks)
        assertTrue(p.newlyAddedSpaceEntryIds.isEmpty())
        assertFalse(p.mayRemoveOriginalData)
        assertFalse(p.grantsWriteAuthority)
        assertFalse(p.mayDeletePrivatePhotos)
    }
    @Test fun identicalBaselineAndTargetHaveNoChanges() {
        val j=journal().copy(targetDigest=originalDigest)
        val p=plan(after=old,j=j)
        assertTrue(p.newlyAddedReminderIds.isEmpty())
        assertTrue(p.newlyAddedSpaceIds.isEmpty())
    }
    @Test fun cannotRemoveOriginalReminderFromTarget() {
        val reduced=target.copy(personal=PersonalCompletionBackupCodec.encode(
            listOf(incoming),emptyList()))
        assertThrows(IllegalArgumentException::class.java) {
            plan(after=reduced,j=journal().copy(
                targetDigest=V304RestorePlanPolicy.fingerprint(reduced,emptyList())))
        }
    }
    @Test fun cannotChangeOriginalOwnerReminder() {
        val modified=target.copy(personal=PersonalCompletionBackupCodec.encode(
            listOf(owner.copy(title="altered"),incoming),emptyList()))
        assertThrows(IllegalArgumentException::class.java) {
            plan(after=modified,j=journal().copy(
                targetDigest=V304RestorePlanPolicy.fingerprint(modified,emptyList())))
        }
    }
    @Test fun cannotAlterOriginalOwnerSpaceMetadata() {
        val ownerSpace=V289PersonalSpacePolicy.newSpace(
            V289SpaceType.FITNESS, 100L,
            "192e901a-36b2-4969-bc28-b6316c3505c1")
        val initialPersonal=PersonalCompletionBackupCodec.decode(old.personal)
        val more=old.copy(spaces=V303SpaceReceiptArchiveCodec.encode(
            V289SpaceState(spaces=listOf(ownerSpace)),initialPersonal))
        val later=target.copy(spaces=V303SpaceReceiptArchiveCodec.encode(
            V289SpaceState(spaces=listOf(ownerSpace.copy(title="changed"),space)),
            newPersonal))
        assertThrows(IllegalArgumentException::class.java) {
            plan(before=more,after=later,j=journal().copy(
                originalDigest=V304RestorePlanPolicy.fingerprint(more,emptyList()),
                targetDigest=V304RestorePlanPolicy.fingerprint(later,emptyList())))
        }
    }
    @Test fun refusesWrongBaselineFingerprint() {
        assertThrows(IllegalArgumentException::class.java) {
            plan(j=journal().copy(originalDigest="a".repeat(64)))
        }
    }
    @Test fun refusesWrongTargetFingerprint() {
        assertThrows(IllegalArgumentException::class.java) {
            plan(j=journal().copy(targetDigest="b".repeat(64)))
        }
    }
    @Test fun refusesUnverifiedArchiveWithMissingSpaceLink() {
        assertThrows(IllegalArgumentException::class.java) {
            val spaces=V303SpaceReceiptArchiveCodec.encode(V289SpaceState(
                spaces=listOf(space),
                links=listOf(V289SpaceReminderLink(spaceId,"missing-reminder",400L)),
            ),newPersonal)
            val corrupt=target.copy(spaces=spaces)
            plan(after=corrupt,j=journal().copy(
                targetDigest=V304RestorePlanPolicy.fingerprint(corrupt,emptyList())))
        }
    }
    @Test fun refusesPrivatePhotoRollbackUntilDedicatedRepair() {
        val photo=com.framebynavin.app.personal.spaces.V290SpacePhotoEntity(
            id="729d2bfa-af37-4836-af9e-c14c3d836421",
            spaceId=spaceId,takenAtMillis=100L,addedAtMillis=200L,
            fileName="729d2bfa-af37-4836-af9e-c14c3d836421.jpg",
            sha256Hex="a".repeat(64),widthPx=16,heightPx=16,note="",
        )
        assertThrows(IllegalArgumentException::class.java) {
            B04NoPhotoRollbackDeltaPolicy.calculate(
                journal(),old,target,emptyList(),listOf(photo))
        }
    }
    @Test fun refusesDuplicateReminderIdentity() {
        assertThrows(Exception::class.java) {
            val bad=target.copy(personal=PersonalCompletionBackupCodec.encode(
                listOf(owner,owner,incoming),emptyList()))
            plan(after=bad,j=journal().copy(
                targetDigest=V304RestorePlanPolicy.fingerprint(bad,emptyList())))
        }
    }
    @Test fun deltaKeepsAllOwnerRecordsOutsideRemovalSet() {
        val p=plan()
        assertTrue(owner.id !in p.newlyAddedReminderIds)
        assertTrue(space.id in p.newlyAddedSpaceIds)
        assertTrue(challenge.id in p.newlyAddedChallengeIds)
    }
    @Test fun malformedChallengeInputCannotBeAssumedEmpty() {
        val damaged=target.copy(challenges="{}")
        assertThrows(Exception::class.java) {
            plan(after=damaged,j=journal().copy(
                targetDigest=V304RestorePlanPolicy.fingerprint(damaged,emptyList())))
        }
    }
}
