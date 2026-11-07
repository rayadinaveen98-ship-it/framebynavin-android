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

/**
 * First real forward crash resume, with zero private JPEGs. Runs exclusively
 * on a cleared, disposable Android 15 CI emulator app profile.
 */
@RunWith(AndroidJUnit4::class)
class B04NoPhotoForwardResumeDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun interruptedChallengeStageReauthenticatesAndFinishesWithoutDataLoss() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val root = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!root.exists()) { "Synthetic recovery must start on a cleared emulator" }
        val owner = PersonalReminder(
            id = "b04-owner", title = "Keep owner",
            startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(8, 30),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val incoming = PersonalReminder(
            id = "b04-incoming", title = "Recover imported",
            startDate = LocalDate.of(2026, 12, 12), time = LocalTime.of(9, 0),
            createdAtMillis = 200L, updatedAtMillis = 200L,
        )
        PersonalReminderStore(app).create(owner)
        val challenge = V297Challenge(
            id = "b04-plan", title = "Synthetic restored plan",
            startDate = LocalDate.of(2026, 12, 12), createdAtMillis = 250L,
        )
        val group = V298ReminderGroup(
            id = "b04-group", name = "Restored synthetic group",
            reminderIds = listOf(incoming.id),
            createdAtMillis = 251L, updatedAtMillis = 251L,
        )
        val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
        val space = V289PersonalSpacePolicy.newSpace(
            V289SpaceType.FITNESS, 300L, spaceId,
        )
        val portable = PersonalCompletionBackupSnapshot(listOf(incoming), emptyList())
        val import = V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(
                portable.reminders, portable.completions,
            ),
            challenges = V297ChallengeCodec.encode(listOf(challenge)),
            groups = V298ReminderGroupCodec.encode(listOf(group)),
            spaces = V303SpaceReceiptArchiveCodec.encode(
                V289SpaceState(
                    spaces = listOf(space),
                    links = listOf(V289SpaceReminderLink(spaceId, incoming.id, 350L)),
                ),
                portable,
            ),
        )
        val passphrase = "Synthetic-B04-NoPhoto-Crash-2026".toCharArray()
        val zip = ByteArrayOutputStream().also { output ->
            V300EncryptedPersonalArchive.write(import, emptyList(), passphrase, output)
        }.toByteArray()
        val prepared = V304ArchivePreparationCoordinator(app).prepareOwnerRequested(
            ByteArrayInputStream(zip), passphrase, true,
        )
        val escrow = V305RollbackEscrowCoordinator(app).createOwnerApprovedEscrow(
            passphrase, ownerRequested = true,
        )
        val original = V301LocalRecoveryReader(app).read()
        val barrier = V305CommitBarrierPolicy.check(prepared, escrow, original)
        val target = StabilizationRestoreTargetPolicy.calculate(original, import, emptyList())
        StabilizationPersonalMutationGate.withOwnerMutation(app) {
            val journal = StabilizationRestoreWriteAheadJournal(root)
            val first = journal.prepare(
                barrier, target.targetDigest, true, System.currentTimeMillis(),
            )
            StabilizationPersonalMutationGate.withPreparedRecovery(root, first) {
                journal.checkpoint(first.step,
                    StabilizationRestoreWriteStep.BEFORE_PERSONAL, System.currentTimeMillis())
                PersonalReminderStore(app).replaceFromApprovedImport(target.merged.personal)
                journal.checkpoint(StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                    StabilizationRestoreWriteStep.PERSONAL_WRITTEN, System.currentTimeMillis())
                // Simulate process death just after the BEFORE_CHALLENGES
                // fsync, before the Challenge DataStore atomic edit.
                journal.checkpoint(StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_CHALLENGES, System.currentTimeMillis())
            }
        }
        val interrupted = checkNotNull(
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        )
        assertEquals(StabilizationRestoreWriteStep.BEFORE_CHALLENGES, interrupted.step)
        assertEquals(listOf(owner, incoming), PersonalReminderStore(app).load())
        assertEquals(emptyList<V297Challenge>(), V297ChallengeStore(app).load())
        assertThrows(IllegalStateException::class.java) {
            runBlocking { PersonalReminderStore(app).delete(owner.id) }
        }
        val consent = B04OwnerRecoveryConsent(
            transactionId = interrupted.transactionId,
            preparedId = interrupted.preparedId,
            escrowId = interrupted.escrowId,
            originalDigest = interrupted.originalDigest,
            targetDigest = interrupted.targetDigest,
            selectedChoice = B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET,
            acknowledgedDeviceLocalLimitations = true,
            ownerConfirmed = true,
        )
        val coordinator = B04NoPhotoForwardResumeCoordinator(app)
        assertThrows(Exception::class.java) {
            runBlocking {
                coordinator.resumeOwnerApproved(
                    ByteArrayInputStream(zip), passphrase,
                    "incorrect-synthetic-escrow".toCharArray(), consent,
                )
            }
        }
        assertEquals(interrupted, StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart())
        val completed = coordinator.resumeOwnerApproved(
            ByteArrayInputStream(zip), passphrase, passphrase, consent,
        )
        assertEquals(StabilizationRestoreWriteStep.VERIFYING, completed.stage)
        assertEquals(target.targetDigest, completed.actualDigest)
        assertFalse(completed.ownerRestoreFinished)
        assertEquals(listOf(owner, incoming), PersonalReminderStore(app).load())
        assertEquals(listOf(challenge), V297ChallengeStore(app).load())
        assertEquals(listOf(group), V298ReminderGroupStore(app).load())
        val db = V289SpacesDatabase.open(app)
        try {
            assertEquals(1, db.spaces().loadSpaces().size)
            assertEquals(1, db.spaces().allLinks().size)
        } finally { db.close() }
        val finalJournal = checkNotNull(
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        )
        assertEquals(StabilizationRestoreWriteStep.VERIFYING, finalJournal.step)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { V297ChallengeStore(app).upsert(challenge) }
        }
        val finalizeConsent = consent.copy(
            selectedChoice = B04OwnerRecoveryChoice.FINALIZE_VERIFIED_TARGET,
        )
        val saved = B04OwnerVerifiedTargetEvidenceCoordinator(app)
            .recordOwnerVerifiedTarget(escrow.id, passphrase, finalizeConsent)
        assertEquals(finalJournal.targetDigest, saved.targetDigest)
        val archived = B04OwnerFinalizedRecoveryCoordinator(app)
            .finalizeOwnerApprovedTarget(escrow.id, passphrase, finalizeConsent)
        assertFalse(root.exists())
        assertEquals(
            finalJournal,
            StabilizationRestoreWriteAheadJournal(archived).inspectAfterRestart(),
        )
        assertTrue(File(app.filesDir,
            "backlot-personal-rollback-escrow-v1/${escrow.id}/archive.zip").isFile)
        assertEquals(
            "Owner editable after recovery",
            PersonalReminderStore(app).update(owner.id) {
                it.copy(title = "Owner editable after recovery")
            }.first().title,
        )
        assertEquals(incoming, PersonalReminderStore(app).load()[1])
    }
}
