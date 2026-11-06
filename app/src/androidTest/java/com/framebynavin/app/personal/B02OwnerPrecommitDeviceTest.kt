package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase B2 TEST-ONLY fixture. This never enters an owner-visible restore.
 * It uses an emulator with cleared synthetic Backlot app data, creates an
 * authenticated PREPARED journal, then verifies ordinary writers are frozen.
 * It never commits Personal/Room/photo data or tells the app to recover.
 */
@RunWith(AndroidJUnit4::class)
class B02OwnerPrecommitDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun authenticatedOwnerPrecommitLocksOrdinaryWritersWithoutRestoring() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val journalRoot = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!journalRoot.exists()) {
            "Fixture requires an independently cleared emulator app profile"
        }
        val passphrase = "Synthetic-Backlot-Phase-B-Only-2026".toCharArray()
        val beforePersonal = PersonalReminderStore(app).exportPersonalJson()
        val incoming = ByteArrayOutputStream().also { output ->
            V300PersonalArchiveSource(app).export(passphrase, output)
        }.toByteArray()
        val prepare = V304ArchivePreparationCoordinator(app)
            .prepareOwnerRequested(
                ByteArrayInputStream(incoming), passphrase, ownerRequested = true,
            )
        val escrow = V305RollbackEscrowCoordinator(app)
            .createOwnerApprovedEscrow(passphrase, ownerRequested = true)

        // Missing owner approval must not allocate an active journal.
        val localBefore = V301LocalRecoveryReader(app).read()
        assertFalse(journalRoot.exists())
        assertTrue(localBefore.localIntegrityVerified)

        val (proof, record) = StabilizationPersonalMutationGate.withOwnerMutation(app) {
            val current = V301LocalRecoveryReader(app).read()
            val latestPrepared = V304RestorePreparationStore(
                File(app.filesDir, "backlot-personal-restore-preparation-v1"),
            ).inspect(prepare.transactionId)
            // Authenticate the escrow's actual encrypted contents, not just
            // its metadata. Require complete portable parity with the phone.
            val verifiedEscrow = V305RollbackEscrowStore(
                File(app.filesDir, "backlot-personal-rollback-escrow-v1"),
            ).verify(escrow.id) { input ->
                V300EncryptedPersonalArchive.stageAndVerify(
                    input, passphrase, app.cacheDir,
                ).use { oldArchive ->
                    V305RollbackMatchPolicy.requireSameSnapshot(
                        current, oldArchive.components, oldArchive.photos.map { it.photo },
                    )
                }
            }
            V300EncryptedPersonalArchive.stageAndVerify(
                ByteArrayInputStream(incoming), passphrase, app.cacheDir,
            ).use { authenticated ->
                val bar = V305CommitBarrierPolicy.check(
                    latestPrepared, verifiedEscrow, current,
                )
                val target = StabilizationRestoreTargetPolicy.calculate(
                    current, authenticated.components, authenticated.photos.map { it.photo },
                )
                val receipt = B01RestorePrecommitPolicy.inspect(
                    B01RestorePrecommitEvidence(
                        prepared = latestPrepared,
                        escrow = verifiedEscrow,
                        barrier = bar,
                        liveLocal = current,
                        authenticatedIncoming = authenticated.components,
                        authenticatedIncomingPhotos = authenticated.photos.map { it.photo },
                        proposedTarget = target,
                        ownerRequested = true,
                        acknowledgedNonPortableState = true,
                    )
                )
                assertFalse(receipt.authorizesLiveWrite)
                val journal = StabilizationRestoreWriteAheadJournal(journalRoot)
                    .prepare(
                        bar, receipt.targetDigest,
                        ownerApproved = true, nowMillis = System.currentTimeMillis(),
                    )
                receipt to journal
            }
        }
        assertEquals(prepare.transactionId, proof.preparedId)
        assertEquals(escrow.id, proof.escrowId)
        assertEquals(StabilizationRestoreWriteStep.PREPARED, record.step)
        assertFalse(record.completedRestore)
        assertFalse(record.authorizedToWriteLive)
        assertEquals(
            record,
            StabilizationRestoreWriteAheadJournal(journalRoot).inspectAfterRestart(),
        )
        var writeAttemptReached = false
        try {
            PersonalReminderStore(app).mutate { existing ->
                writeAttemptReached = true
                existing
            }
            fail("Ordinary Personal mutations must be frozen by prepared journal")
        } catch (_: IllegalStateException) {
            assertFalse(writeAttemptReached)
        }
        assertEquals(beforePersonal, PersonalReminderStore(app).exportPersonalJson())
        assertTrue(File(app.filesDir, "backlot-personal-rollback-escrow-v1").isDirectory)
        // The future owner recovery/rollback UI, live writer and journal
        // finalization are NOT invoked anywhere by this emulator test.
    }
}
