package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable Android emulator, no user data, account or cloud. */
@RunWith(AndroidJUnit4::class)
class StabilizationRestoreWriteAheadJournalDeviceTest {
    @Test fun androidDiskCheckpointSurvivesReopenAndPartialUpdateFailsClosed() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val root = File(app.cacheDir, "recovery-journal-" + UUID.randomUUID().toString())
        val barrier = V305CommitBarrierReceipt(
            preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            rollbackEscrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
            localBaselineSha256 = "a".repeat(64),
            incomingArchiveSha256 = "b".repeat(64),
            rollbackArchiveSha256 = "c".repeat(64),
        )
        try {
            val journal = StabilizationRestoreWriteAheadJournal(root)
            assertFalse(root.exists())
            journal.prepare(barrier, "d".repeat(64), true, 10L)
            journal.checkpoint(StabilizationRestoreWriteStep.PREPARED,
                StabilizationRestoreWriteStep.BEFORE_PERSONAL, 11L)
            val reopened = StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()!!
            assertEquals(StabilizationRestoreWriteStep.BEFORE_PERSONAL, reopened.step)
            assertTrue(reopened.recoveryRequiresOwnerReview)
            assertFalse(reopened.completedRestore); assertFalse(reopened.authorizedToWriteLive)
            File(root, "active.pending").writeBytes(byteArrayOf(1, 2, 3))
            assertThrows(IllegalStateException::class.java) {
                StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
            }
        } finally { root.deleteRecursively() }
    }
}
