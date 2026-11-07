package com.framebynavin.app.personal

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Explicitly cleared Android 15 emulator: never run this against owner data. */
@RunWith(AndroidJUnit4::class)
class B03AdditiveRestoreDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun addsAllDomainsAndVerifiesJpegWithoutOverwritingOriginalReminders() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val journalRoot = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!journalRoot.exists()) { "Must run on an independently cleared test profile" }
        val owner = PersonalReminder(
            id = "b03-existing-owner", title = "Preserve original",
            startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(10, 0),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val incoming = PersonalReminder(
            id = "b03-imported-reminder", title = "Imported synthetic reminder",
            startDate = LocalDate.of(2026, 12, 12), time = LocalTime.of(11, 0),
            createdAtMillis = 150L, updatedAtMillis = 150L,
        )
        PersonalReminderStore(app).create(owner)
        val challenge = V297Challenge(
            id = "b03-synthetic-challenge", title = "Imported 21-day habit",
            startDate = LocalDate.of(2026, 12, 12), createdAtMillis = 200L,
        )
        val group = V298ReminderGroup(
            id = "b03-synthetic-group", name = "Imported synthetic group",
            reminderIds = listOf(incoming.id), createdAtMillis = 200L, updatedAtMillis = 200L,
        )
        val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
        val photoId = "729d2bfa-af37-4836-af9e-c14c3d836421"
        val entryId = "8b51347d-6b4d-4d38-8d11-892cd2673a2f"
        val space = V289PersonalSpacePolicy.newSpace(V289SpaceType.FITNESS, 300L, spaceId)
        val state = V289SpaceState(
            spaces = listOf(space),
            links = listOf(V289SpaceReminderLink(spaceId, incoming.id, 400L)),
            entries = listOf(V289SpaceEntry(
                id = entryId, spaceId = spaceId, kind = V289SpaceEntryKind.NOTE,
                source = V289SpaceEntrySource.MANUAL, occurredAtMillis = 410L,
                createdAtMillis = 410L, note = "Imported from synthetic backup",
            )),
        )
        val photoBytes = ByteArrayOutputStream().also { target ->
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.CYAN)
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, target))
            } finally { bitmap.recycle() }
        }.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(photoBytes)
            .joinToString("") { "%02x".format(it) }
        val photo = V290SpacePhotoEntity(
            id = photoId, spaceId = spaceId, takenAtMillis = 300L,
            addedAtMillis = 400L, fileName = "$photoId.jpg",
            sha256Hex = digest, widthPx = 16, heightPx = 16, note = "Synthetic photograph",
        )
        val portablePersonal = PersonalCompletionBackupSnapshot(listOf(incoming), emptyList())
        val data = V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(
                portablePersonal.reminders, portablePersonal.completions,
            ),
            challenges = V297ChallengeCodec.encode(listOf(challenge)),
            groups = V298ReminderGroupCodec.encode(listOf(group)),
            spaces = V303SpaceReceiptArchiveCodec.encode(state, portablePersonal),
        )
        // A direct caller cannot evade the journal or invoke the Room writer.
        assertThrows(IllegalStateException::class.java) {
            runBlocking { B03RestoreDomainWriter(app).writeSpaces(data) }
        }
        assertFalse(app.getDatabasePath(V289SpacesDatabase.FILE_NAME).exists())
        val passphrase = "Synthetic-B03-Recovery-Only-2026".toCharArray()
        val bytes = ByteArrayOutputStream().also { encrypted ->
            V300EncryptedPersonalArchive.write(
                data,
                listOf(V300PhotoSource(photo, photoBytes.size.toLong()) {
                    ByteArrayInputStream(photoBytes)
                }),
                passphrase, encrypted,
            )
        }.toByteArray()
        val prep = V304ArchivePreparationCoordinator(app).prepareOwnerRequested(
            ByteArrayInputStream(bytes), passphrase, true,
        )
        val escrow = V305RollbackEscrowCoordinator(app).createOwnerApprovedEscrow(
            passphrase, ownerRequested = true,
        )
        val result = B03RestoreTransactionCoordinator(app).writeForOwnerApprovedRecovery(
            incomingArchive = ByteArrayInputStream(bytes),
            incomingPassword = passphrase,
            preparedId = prep.transactionId,
            escrowId = escrow.id,
            escrowPassword = passphrase,
            ownerRequested = true,
            acknowledgedNonPortableState = true,
        )
        assertEquals(StabilizationRestoreWriteStep.VERIFYING, result.stage)
        assertEquals(result.expectedDigest, result.actualDigest)
        assertFalse(result.ownerRestoreFinished)
        assertEquals(
            StabilizationRestoreWriteStep.VERIFYING,
            StabilizationRestoreWriteAheadJournal(journalRoot).inspectAfterRestart()!!.step,
        )
        assertEquals(listOf(owner, incoming), PersonalReminderStore(app).load())
        assertEquals(listOf(challenge), V297ChallengeStore(app).load())
        assertEquals(listOf(group), V298ReminderGroupStore(app).load())
        val db = V289SpacesDatabase.open(app)
        try {
            assertEquals(1, db.spaces().loadSpaces().size)
            assertEquals(1, db.spaces().allLinks().size)
            assertEquals(1, db.spaces().allEntries().size)
            assertEquals(photo, db.spaces().getPhoto(photoId))
        } finally { db.close() }
        val photoFile = File(
            File(File(app.filesDir, "backlot-personal-space-photos"), spaceId), "$photoId.jpg",
        )
        assertArrayEquals(photoBytes, photoFile.readBytes())
        // Emulate power loss after private JPEG bytes are installed but a
        // temporary copy remains: ordinary inventory must fail closed, while
        // owner-authorized forensic inspection still decodes Room metadata.
        val interruptedCopy = File(photoFile.parentFile, ".${photoId}.restore.pending")
        check(interruptedCopy.createNewFile())
        interruptedCopy.writeBytes(byteArrayOf(1, 2, 3, 4))
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) {
            val forensic = V301LocalRecoveryReader(app).readForInterruptedRecovery()
            assertFalse(forensic.localIntegrityVerified)
            assertEquals(listOf(photo), forensic.photos)
            val photoReview = B04PrivatePhotoForensicPolicy.inspect(
                File(app.filesDir, "backlot-personal-space-photos"),
                original = emptyList(),
                target = listOf(photo),
                checkpoint = StabilizationRestoreWriteStep.VERIFYING,
            )
            assertEquals(1, photoReview.interruptedPendingFiles)
            assertTrue(photoReview.demandsOwnerReview)
            assertFalse(photoReview.allTargetBytesPresentAndVerified)
            var normalReadBlocked = false
            try {
                V301LocalRecoveryReader(app).read()
            } catch (_: Exception) {
                normalReadBlocked = true
            }
            assertTrue("Normal backup must reject a torn private JPEG copy", normalReadBlocked)
        }
        assertTrue("Synthetic pending marker remains for owner review", interruptedCopy.exists())
        // Test-only cleanup on a disposable CI emulator, NEVER a production
        // automatic repair or owner's data.
        check(interruptedCopy.delete())
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                PersonalReminderStore(app).delete(incoming.id)
            }
        }
        assertEquals(listOf(owner, incoming), PersonalReminderStore(app).load())
        val interrupted = B04InterruptedRecoveryInspector(app).inspectOwnerRequested(
            escrowId = escrow.id,
            escrowPassphrase = passphrase,
            ownerRequested = true,
        )
        assertEquals(
            B04InterruptedRecoveryDisposition.TARGET_PRESENT_AWAITING_OWNER_FINALIZATION,
            interrupted.disposition,
        )
        assertEquals(result.expectedDigest, interrupted.observedSnapshotDigest)
        assertFalse(interrupted.mayAutoResume)
        assertFalse(interrupted.mayDeleteJournal)
        assertThrows(Exception::class.java) {
            runBlocking {
                B04InterruptedRecoveryInspector(app).inspectOwnerRequested(
                    escrow.id, "bad-password-for-synthetic-test".toCharArray(), true,
                )
            }
        }
        // The owner can record independently authenticated, immutable
        // final-target evidence, but NOT complete or unlock the restore.
        val checkpoint = checkNotNull(
            StabilizationRestoreWriteAheadJournal(journalRoot).inspectAfterRestart()
        )
        val ownerConsent = B04OwnerRecoveryConsent(
            transactionId = checkpoint.transactionId,
            preparedId = checkpoint.preparedId,
            escrowId = checkpoint.escrowId,
            originalDigest = checkpoint.originalDigest,
            targetDigest = checkpoint.targetDigest,
            selectedChoice = B04OwnerRecoveryChoice.FINALIZE_VERIFIED_TARGET,
            acknowledgedDeviceLocalLimitations = true,
            ownerConfirmed = true,
        )
        val evidenceCoordinator = B04OwnerVerifiedTargetEvidenceCoordinator(app)
        assertThrows(Exception::class.java) {
            runBlocking {
                evidenceCoordinator.recordOwnerVerifiedTarget(
                    escrow.id, "incorrect-test-passphrase".toCharArray(), ownerConsent,
                )
            }
        }
        val finalEvidence = evidenceCoordinator.recordOwnerVerifiedTarget(
            escrow.id, passphrase, ownerConsent,
        )
        assertEquals(checkpoint.targetDigest, finalEvidence.targetDigest)
        assertFalse(finalEvidence.permitsJournalRemoval)
        assertFalse(finalEvidence.signifiesCompletedRestore)
        assertEquals(
            finalEvidence,
            evidenceCoordinator.recordOwnerVerifiedTarget(
                escrow.id, passphrase, ownerConsent,
            ),
        )
        assertTrue(journalRoot.exists())
        assertThrows(IllegalStateException::class.java) {
            runBlocking { PersonalReminderStore(app).delete(incoming.id) }
        }
        // The owner separately approves FINALIZATION after a restarted,
        // authenticated target review. The encrypted rollback escrow and the
        // journal remain in private history; ordinary writes unlock only now.
        val retainedJournal = B04OwnerFinalizedRecoveryCoordinator(app)
            .finalizeOwnerApprovedTarget(escrow.id, passphrase, ownerConsent)
        assertFalse(journalRoot.exists())
        assertEquals(
            checkpoint,
            StabilizationRestoreWriteAheadJournal(retainedJournal).inspectAfterRestart(),
        )
        assertTrue(File(app.filesDir,
            "backlot-personal-rollback-escrow-v1/${escrow.id}/archive.zip").isFile)
        assertEquals(
            "Owner reminder remains editable after verified recovery",
            PersonalReminderStore(app).update(owner.id) {
                it.copy(title = "Owner reminder remains editable after verified recovery")
            }.first().title,
        )
        assertEquals(incoming, PersonalReminderStore(app).load()[1])
        // No restore writer or alarm scheduler is exposed by this test.
    }
}
