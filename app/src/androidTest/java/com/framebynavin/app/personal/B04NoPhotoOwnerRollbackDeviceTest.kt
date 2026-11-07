package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic Android 15 profile only. Never run against an installed owner app. */
@RunWith(AndroidJUnit4::class)
class B04NoPhotoOwnerRollbackDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun interruptedSpaceStageRollsBackOnlyImportedDataAndKeepsOwnerLocked() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val forwardRoot = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!forwardRoot.exists()) { "CI must independently clear synthetic Android profile" }
        val originalReminder = PersonalReminder(
            id = "b04-rollback-original", title = "Owner original reminder",
            startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(8, 30),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val importedReminder = PersonalReminder(
            id = "b04-rollback-imported", title = "Imported reminder to undo",
            startDate = LocalDate.of(2026, 12, 12), time = LocalTime.of(10, 0),
            createdAtMillis = 200L, updatedAtMillis = 200L,
        )
        PersonalReminderStore(app).create(originalReminder)
        val original = V301LocalRecoveryReader(app).read()
        val challenge = V297Challenge(
            id = "b04-rollback-challenge", title = "Synthetic extra plan",
            startDate = LocalDate.of(2026, 12, 12), createdAtMillis = 201L,
        )
        val group = V298ReminderGroup(
            id = "b04-rollback-group", name = "Extra reminder group",
            reminderIds = listOf(importedReminder.id),
            createdAtMillis = 202L, updatedAtMillis = 202L,
        )
        val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
        val incomingPersonal = PersonalCompletionBackupSnapshot(
            listOf(importedReminder), emptyList(),
        )
        val incoming = V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(
                incomingPersonal.reminders, incomingPersonal.completions,
            ),
            challenges = V297ChallengeCodec.encode(listOf(challenge)),
            groups = V298ReminderGroupCodec.encode(listOf(group)),
            spaces = V303SpaceReceiptArchiveCodec.encode(
                V289SpaceState(
                    spaces = listOf(V289PersonalSpacePolicy.newSpace(
                        V289SpaceType.FITNESS, 300L, spaceId,
                    )),
                    links = listOf(V289SpaceReminderLink(
                        spaceId, importedReminder.id, 301L,
                    )),
                ), incomingPersonal,
            ),
        )
        val password = "Synthetic-B04-Rollback-Test-2026".toCharArray()
        val zip = ByteArrayOutputStream().also { output ->
            V300EncryptedPersonalArchive.write(incoming, emptyList(), password, output)
        }.toByteArray()
        val preparation = V304ArchivePreparationCoordinator(app)
            .prepareOwnerRequested(ByteArrayInputStream(zip), password, true)
        val escrow = V305RollbackEscrowCoordinator(app)
            .createOwnerApprovedEscrow(password, ownerRequested = true)
        val barrier = V305CommitBarrierPolicy.check(preparation, escrow, original)
        val proposed = StabilizationRestoreTargetPolicy.calculate(
            original, incoming, emptyList(),
        )
        StabilizationPersonalMutationGate.withOwnerMutation(app) {
            val journal = StabilizationRestoreWriteAheadJournal(forwardRoot)
            val prepared = journal.prepare(
                barrier, proposed.targetDigest, true, System.currentTimeMillis(),
            )
            StabilizationPersonalMutationGate.withPreparedRecovery(forwardRoot, prepared) {
                suspend fun checkpoint(
                    from: StabilizationRestoreWriteStep,
                    to: StabilizationRestoreWriteStep,
                ) { journal.checkpoint(from,to,System.currentTimeMillis()) }
                checkpoint(StabilizationRestoreWriteStep.PREPARED,
                    StabilizationRestoreWriteStep.BEFORE_PERSONAL)
                PersonalReminderStore(app).replaceFromApprovedImport(proposed.merged.personal)
                checkpoint(StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                    StabilizationRestoreWriteStep.PERSONAL_WRITTEN)
                checkpoint(StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_CHALLENGES)
                V297ChallengeStore(app).replaceFromApprovedImport(proposed.merged.challenges)
                checkpoint(StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
                    StabilizationRestoreWriteStep.CHALLENGES_WRITTEN)
                checkpoint(StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_GROUPS)
                V298ReminderGroupStore(app).importMissingAfterApproval(proposed.merged.groups)
                checkpoint(StabilizationRestoreWriteStep.BEFORE_GROUPS,
                    StabilizationRestoreWriteStep.GROUPS_WRITTEN)
                checkpoint(StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_SPACES)
                B03RestoreDomainWriter(app).writeSpaces(proposed.merged)
                checkpoint(StabilizationRestoreWriteStep.BEFORE_SPACES,
                    StabilizationRestoreWriteStep.SPACES_WRITTEN)
                // Simulate death before BEFORE_PHOTOS and no final target receipt.
            }
        }
        val interrupted = checkNotNull(
            StabilizationRestoreWriteAheadJournal(forwardRoot).inspectAfterRestart()
        )
        assertEquals(StabilizationRestoreWriteStep.SPACES_WRITTEN, interrupted.step)
        assertEquals(listOf(originalReminder, importedReminder), PersonalReminderStore(app).load())
        assertEquals(listOf(challenge), V297ChallengeStore(app).load())
        val db = V289SpacesDatabase.open(app)
        try {
            assertEquals(1,db.spaces().loadSpaces().size)
            assertEquals(1,db.spaces().allLinks().size)
        } finally { db.close() }
        val consent = B04OwnerRecoveryConsent(
            transactionId = interrupted.transactionId,
            preparedId = interrupted.preparedId,
            escrowId = interrupted.escrowId,
            originalDigest = interrupted.originalDigest,
            targetDigest = interrupted.targetDigest,
            selectedChoice = B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW,
            acknowledgedDeviceLocalLimitations = true,
            ownerConfirmed = true,
        )
        val rollback = B04NoPhotoOwnerRollbackCoordinator(app)
        assertThrows(Exception::class.java) {
            runBlocking {
                rollback.rollbackOwnerApproved(
                    ByteArrayInputStream(zip),password,
                    "incorrect-test-password".toCharArray(),consent,
                )
            }
        }
        assertEquals(interrupted,
            StabilizationRestoreWriteAheadJournal(forwardRoot).inspectAfterRestart())
        val result = rollback.rollbackOwnerApproved(
            ByteArrayInputStream(zip),password,password,consent,
        )
        assertEquals(B04RollbackWriteStep.VERIFIED,result.actionState)
        assertEquals(original.components,
            V301LocalRecoveryReader(app).read().components)
        assertEquals(original.photos,
            V301LocalRecoveryReader(app).read().photos)
        assertEquals(listOf(originalReminder), PersonalReminderStore(app).load())
        assertEquals(emptyList<V297Challenge>(),V297ChallengeStore(app).load())
        assertEquals(emptyList<V298ReminderGroup>(),V298ReminderGroupStore(app).load())
        val restoredDb = V289SpacesDatabase.open(app)
        try {
            assertEquals(0,restoredDb.spaces().loadSpaces().size)
            assertEquals(0,restoredDb.spaces().allLinks().size)
            assertEquals(0,restoredDb.spaces().allEntries().size)
        } finally { restoredDb.close() }
        val actionFile = File(app.filesDir,
            "backlot-personal-no-photo-rollback-actions-v1/${interrupted.transactionId}")
        val action = checkNotNull(B04NoPhotoRollbackActionJournal(actionFile).inspect())
        assertEquals(B04RollbackWriteStep.VERIFIED,action.step)
        assertFalse(result.ownerRollbackFinalized)
        // No implicit unlock even after exact owner-data parity.
        assertTrue(forwardRoot.exists())
        assertThrows(IllegalStateException::class.java) {
            runBlocking { PersonalReminderStore(app).delete(originalReminder.id) }
        }
        assertTrue(File(app.filesDir,
            "backlot-personal-rollback-escrow-v1/${escrow.id}/archive.zip").isFile)
        val durableHistory = B04OwnerNoPhotoRollbackReleaseCoordinator(app)
            .finalizeOwnerConfirmedRollback(
                escrow.id, password, consent,
            )
        assertFalse(forwardRoot.exists())
        assertEquals(interrupted,
            StabilizationRestoreWriteAheadJournal(durableHistory).inspectAfterRestart())
        assertEquals(action,
            B04NoPhotoRollbackActionJournal(actionFile).inspect())
        assertEquals(
            "Owner editable after verified rollback",
            PersonalReminderStore(app).update(originalReminder.id) {
                it.copy(title="Owner editable after verified rollback")
            }.first().title,
        )
        assertEquals(1,PersonalReminderStore(app).load().size)
        assertTrue(File(app.filesDir,
            "backlot-personal-rollback-escrow-v1/${escrow.id}/archive.zip").isFile)
    }
}
