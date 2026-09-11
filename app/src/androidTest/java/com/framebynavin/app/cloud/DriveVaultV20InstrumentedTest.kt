package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DriveVaultV20InstrumentedTest {
    @Test fun meaningfulSnapshotWinsAndEmptyOverwriteIsBlocked() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val taskId = "drive-vault-test-${UUID.randomUUID()}"
        try {
            TaskStore(context).mutate {
                it + CreatorTask(
                    id = taskId,
                    title = "Drive vault safety",
                    platform = "YouTube",
                    contentType = "Long-form",
                    dueLabel = "Today",
                )
            }
            val localBackup = manager.createBackup()
            val preview = manager.validate(localBackup)
            assertTrue(preview.projectCount > 0)

            val meaningful = DriveVaultRestorePoint(
                fileId = "meaningful",
                name = "meaningful.json",
                capturedAtMillis = 1000L,
                appVersion = "2.0.0-alpha1.2b-drive-vault",
                projectCount = preview.projectCount,
                ideaCount = preview.ideaCount,
                weeklySlotCount = preview.weeklySlotCount,
                activeReminderCount = preview.activeReminderCount,
                sha256 = "a".repeat(64),
                sizeBytes = localBackup.toByteArray().size.toLong(),
            )
            val newerEmpty = meaningful.copy(
                fileId = "empty",
                name = "empty.json",
                capturedAtMillis = 2000L,
                projectCount = 0,
                ideaCount = 0,
            )

            assertEquals("meaningful", DriveVaultPolicy.recommended(listOf(newerEmpty, meaningful))?.fileId)
            assertTrue(DriveVaultPolicy.shouldBlockEmptySnapshot(0, 0, listOf(meaningful)))
            assertFalse(DriveVaultPolicy.shouldBlockEmptySnapshot(preview.projectCount, preview.ideaCount, listOf(meaningful)))
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }
}
