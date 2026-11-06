package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android 15 test of the real encrypted Personal data source and private
 * escrow file. Synthetic, disposable emulator only; NO import/restore writes.
 */
@RunWith(AndroidJUnit4::class)
class V305RollbackEscrowDeviceTest {
    @Test fun ownerEscrowMatchesCurrentPhoneAndRemainsVerifiableAfterReopen() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val baseline = runBlocking { PersonalReminderStore(app).exportPersonalJson() }
        val spacesDb = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
        val initiallyPresent = spacesDb.exists()
        val passphrase = "synthetic-owner-escrow-passphrase".toCharArray()
        val coordinator = V305RollbackEscrowCoordinator(app)
        val root = File(app.filesDir, "backlot-personal-rollback-escrow-v1")
        try {
            val receipt = runBlocking {
                coordinator.createOwnerApprovedEscrow(passphrase, true)
            }
            assertFalse(receipt.canWriteLiveOwnerData)
            assertTrue(receipt.archiveBytes > 100)
            assertEquals(
                receipt,
                runBlocking { coordinator.verifyOwnerEscrow(receipt.id, passphrase) },
            )
            assertEquals(
                V305EscrowInventory(1, 0, 0),
                runBlocking { coordinator.inspectEscrowCandidates() },
            )
            assertThrows(Exception::class.java) {
                runBlocking {
                    coordinator.verifyOwnerEscrow(
                        receipt.id, "wrong-passphrase-for-test".toCharArray(),
                    )
                }
            }
            assertEquals(baseline, runBlocking {
                PersonalReminderStore(app).exportPersonalJson()
            })
            assertEquals(initiallyPresent, spacesDb.exists())
            runBlocking { coordinator.discardAfterOwnerConfirmation(receipt.id) }
            assertEquals(
                V305EscrowInventory(0, 0, 0),
                runBlocking { coordinator.inspectEscrowCandidates() },
            )
        } finally {
            passphrase.fill('\u0000')
            // The emulator's isolated test escrow is synthetic and disposable.
            root.deleteRecursively()
        }
    }
}
