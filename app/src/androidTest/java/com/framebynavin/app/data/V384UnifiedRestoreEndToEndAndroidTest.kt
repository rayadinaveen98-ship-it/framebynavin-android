package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable CI emulator only. Exercises the real unified owner transaction:
 * export -> authenticate -> prepare -> protect original -> reserve H4 ->
 * Creator -> Personal v2 -> both-domain final verification -> journal release.
 *
 * Restoring an identical encrypted snapshot is the initial no-conflict smoke
 * case, NOT proof for nonempty conflicts, interruptions, arbitrary hardware,
 * cloud deletion, or a user-facing Restore All release.
 *
 * Never calls ActivityManager.clearApplicationUserData() or deletes any
 * pre-existing owner media / recovery evidence. Uses a fresh CI emulator.
 */
@RunWith(AndroidJUnit4::class)
class V384UnifiedRestoreEndToEndAndroidTest {
    @Test fun identicalOwnerSnapshotCompletesRealJournaledForwardRestore() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        assertFalse("Disposable emulator must start without H4 recovery evidence",
            V344UnifiedRestoreJournal.hasPendingFor(app))
        val passphrase = "emulator-only-safe-test-" + UUID.randomUUID().toString()
        val secret = passphrase.toCharArray()
        val zip = File(app.cacheDir, "v384-ci-owner-" + UUID.randomUUID() + ".zip")
        try {
            withTimeout(240_000L) {
                val manager = V341UnifiedBackupManager(app)
                zip.outputStream().buffered().use { manager.export(secret, it) }
                assertTrue(zip.length() > 0L)
                zip.inputStream().buffered().use { manager.verify(secret, it) }
                val review = zip.inputStream().buffered().use {
                    manager.inspectRestore(secret, it)
                }
                assertFalse(review.personalDiff.needsOwnerReview)
                assertTrue(review.personalDiff.conflicts == 0)

                zip.inputStream().buffered().use { chosen ->
                    V342UnifiedRestorePreparationStore(app).prepare(
                        chosen, secret, ownerRequested = true,
                    )
                }
                V343UnifiedRecoveryEscrow(app).retainOriginal(
                    ownerConfirmed = true, passphrase = secret,
                )
                val readOnly = V344UnifiedRestorePrecommit(app).inspectFullOwnerProof(
                    secret, ownerReviewed = true,
                )
                assertTrue(readOnly.preflight.sourceAndEscrowAuthenticated)

                val reserved = V349UnifiedRestoreReservation(app).reserveOnly(
                    passphrase = secret, ownerConfirmedPreparation = true,
                )
                assertTrue(reserved.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES)

                val creator = V357UnifiedCreatorForwardStage(app).executeInternalOnly(
                    ownerConfirmedStart = true, passphrase = secret,
                )
                assertTrue(creator.checkpointReached ==
                    V344UnifiedRestoreStep.CREATOR_WRITTEN)

                val personal = V358UnifiedPersonalForwardStage(app).executeInternalOnly(
                    passphrase = secret,
                    ownerConfirmedPersonalAdditions = true,
                    acknowledgedNonPortableAlarmsAndSongs = true,
                )
                assertTrue(personal.checkpointReached ==
                    V344UnifiedRestoreStep.PERSONAL_WRITTEN)

                V381UnifiedVerifiedForwardCompletion(app)
                    .verifyForwardAndRecordInternalOnly(
                        freshOwnerConsent = true, passphrase = secret,
                    )
                V379UnifiedReleaseCoordinator(app).releaseOwnerVerifiedInternalOnly(
                    freshOwnerConsent = true, passphrase = secret,
                )
                assertFalse("Verified owner release must archive blocking H4 journal",
                    V344UnifiedRestoreJournal.hasPendingFor(app))
            }
        } finally {
            secret.fill('\u0000')
            zip.delete()
        }
    }
}
