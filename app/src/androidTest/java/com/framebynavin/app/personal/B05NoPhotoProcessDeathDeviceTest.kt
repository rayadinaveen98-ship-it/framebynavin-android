package com.framebynavin.app.personal

import android.os.Process
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
 * Real two-instrumentation-process owner recovery fixture. The CI shell
 * force-stops the app between the two methods and starts a new test process.
 * The only persisted import is SYNTHETIC and lives on a disposable emulator.
 */
@RunWith(AndroidJUnit4::class)
class B05NoPhotoProcessDeathDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val password get() = "Synthetic-B05-PowerFailure-2026".toCharArray()
    private val importFile get() = File(app.filesDir, "b05-ci-imported-archive.zip")
    private val escrowFile get() = File(app.filesDir, "b05-ci-rollback-escrow-id.txt")
    private val processFile get() = File(app.filesDir, "b05-ci-first-pid.txt")
    private val forwardRoot get() = StabilizationPersonalMutationGate.journalRootFor(app)

    private fun owner() = PersonalReminder(
        id = "b05-existing-reminder", title = "Preserve after process death",
        startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(8, 30),
        createdAtMillis = 100L, updatedAtMillis = 100L,
    )
    private fun incoming() = PersonalReminder(
        id = "b05-imported-reminder", title = "Synthetic crash import",
        startDate = LocalDate.of(2026, 12, 12), time = LocalTime.of(9, 30),
        createdAtMillis = 200L, updatedAtMillis = 200L,
    )

    @Test fun prepareInterruptedRoomWriteInFirstProcess() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        check(!forwardRoot.exists() && !importFile.exists() &&
            !escrowFile.exists() && !processFile.exists()) {
            "A clean disposable emulator is mandatory for B05"
        }
        PersonalReminderStore(app).create(owner())
        val additional = incoming()
        val incomingPersonal = PersonalCompletionBackupSnapshot(
            listOf(additional), emptyList(),
        )
        val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
        val challenge = V297Challenge(
            id = "b05-imported-challenge", title = "Synthetic crash challenge",
            startDate = LocalDate.of(2026, 12, 12), createdAtMillis = 250L,
        )
        val group = V298ReminderGroup(
            id = "b05-imported-group", name = "Synthetic crash group",
            reminderIds = listOf(additional.id),
            createdAtMillis = 260L, updatedAtMillis = 260L,
        )
        val imported = V300PersonalComponents(
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
                        spaceId, additional.id, 301L,
                    )),
                ), incomingPersonal,
            ),
        )
        val zip = ByteArrayOutputStream().also { sink ->
            V300EncryptedPersonalArchive.write(
                imported, emptyList(), password, sink,
            )
        }.toByteArray()
        val preview = V304ArchivePreparationCoordinator(app)
            .prepareOwnerRequested(ByteArrayInputStream(zip), password, true)
        val escrow = V305RollbackEscrowCoordinator(app)
            .createOwnerApprovedEscrow(password, ownerRequested = true)
        val before = V301LocalRecoveryReader(app).read()
        val barrier = V305CommitBarrierPolicy.check(preview, escrow, before)
        val target = StabilizationRestoreTargetPolicy.calculate(
            before, imported, emptyList(),
        )
        // Persist only synthetic test bytes so a NEW Android process can
        // reauthenticate the same selected encrypted incoming archive.
        importFile.outputStream().use { it.write(zip) }
        escrowFile.writeText(escrow.id)
        processFile.writeText(Process.myPid().toString())
        StabilizationPersonalMutationGate.withOwnerMutation(app) {
            val writeAhead = StabilizationRestoreWriteAheadJournal(forwardRoot)
            val prepared = writeAhead.prepare(
                barrier, target.targetDigest, true, System.currentTimeMillis(),
            )
            StabilizationPersonalMutationGate.withPreparedRecovery(
                forwardRoot, prepared,
            ) {
                suspend fun advance(
                    from: StabilizationRestoreWriteStep,
                    to: StabilizationRestoreWriteStep,
                ) { writeAhead.checkpoint(from, to, System.currentTimeMillis()) }
                advance(StabilizationRestoreWriteStep.PREPARED,
                    StabilizationRestoreWriteStep.BEFORE_PERSONAL)
                PersonalReminderStore(app).replaceFromApprovedImport(target.merged.personal)
                advance(StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                    StabilizationRestoreWriteStep.PERSONAL_WRITTEN)
                advance(StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_CHALLENGES)
                V297ChallengeStore(app).replaceFromApprovedImport(target.merged.challenges)
                advance(StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
                    StabilizationRestoreWriteStep.CHALLENGES_WRITTEN)
                advance(StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_GROUPS)
                V298ReminderGroupStore(app).importMissingAfterApproval(target.merged.groups)
                advance(StabilizationRestoreWriteStep.BEFORE_GROUPS,
                    StabilizationRestoreWriteStep.GROUPS_WRITTEN)
                advance(StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_SPACES)
                // A real Room transaction is durable here, but there is NO
                // corresponding SPACES_WRITTEN checkpoint. This reproduces
                // process death between write and post-write fsync.
                B03RestoreDomainWriter(app).writeSpaces(target.merged)
            }
        }
        assertEquals(
            StabilizationRestoreWriteStep.BEFORE_SPACES,
            StabilizationRestoreWriteAheadJournal(forwardRoot).inspectAfterRestart()!!.step,
        )
        val db = V289SpacesDatabase.open(app)
        try { assertEquals(1, db.spaces().loadSpaces().size) }
        finally { db.close() }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { PersonalReminderStore(app).delete(owner().id) }
        }
        // The harness now ends instrumentation and issues 'am force-stop'.
    }

    @Test fun inspectAndRollBackFromNewAndroidProcess() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertTrue("Synthetic incoming archive must survive process death", importFile.isFile)
        assertTrue("Escrow receipt must survive process death", escrowFile.isFile)
        val previousPid = processFile.readText().trim().toInt()
        assertNotEquals("A distinct Android process is required",
            previousPid, Process.myPid())
        val interrupted = checkNotNull(
            StabilizationRestoreWriteAheadJournal(forwardRoot).inspectAfterRestart()
        )
        assertEquals(StabilizationRestoreWriteStep.BEFORE_SPACES, interrupted.step)
        val selectedEscrow = escrowFile.readText().trim()
        assertEquals(interrupted.escrowId, selectedEscrow)
        val approved = B04OwnerRecoveryConsent(
            transactionId = interrupted.transactionId,
            preparedId = interrupted.preparedId,
            escrowId = interrupted.escrowId,
            originalDigest = interrupted.originalDigest,
            targetDigest = interrupted.targetDigest,
            selectedChoice = B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW,
            acknowledgedDeviceLocalLimitations = true,
            ownerConfirmed = true,
        )
        val result = B04NoPhotoOwnerRollbackCoordinator(app).rollbackOwnerApproved(
            ByteArrayInputStream(importFile.readBytes()),
            password, password, approved,
        )
        assertEquals(B04RollbackWriteStep.VERIFIED, result.actionState)
        assertEquals(result.expectedOriginalDigest, result.actualOriginalDigest)
        assertEquals(listOf(owner()), PersonalReminderStore(app).load())
        assertTrue(V297ChallengeStore(app).load().isEmpty())
        assertTrue(V298ReminderGroupStore(app).load().isEmpty())
        val db = V289SpacesDatabase.open(app)
        try {
            assertTrue(db.spaces().loadSpaces().isEmpty())
            assertTrue(db.spaces().allLinks().isEmpty())
        } finally { db.close() }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { PersonalReminderStore(app).delete(owner().id) }
        }
        val retained = B04OwnerNoPhotoRollbackReleaseCoordinator(app)
            .finalizeOwnerConfirmedRollback(selectedEscrow, password, approved)
        assertEquals(
            interrupted,
            StabilizationRestoreWriteAheadJournal(retained).inspectAfterRestart(),
        )
        assertFalse(forwardRoot.exists())
        assertEquals(
            "Owner writable again",
            PersonalReminderStore(app).update(owner().id) {
                it.copy(title = "Owner writable again")
            }.first().title,
        )
        assertTrue(File(app.filesDir,
            "backlot-personal-rollback-escrow-v1/${selectedEscrow}/archive.zip").isFile)
        // These three files are disposable CI-only session breadcrumbs.
        assertTrue(importFile.delete())
        assertTrue(escrowFile.delete())
        assertTrue(processFile.delete())
    }
}
