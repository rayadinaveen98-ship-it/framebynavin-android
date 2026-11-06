package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.File
import java.util.UUID
import java.security.MessageDigest
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
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

    @Test fun verifiedJpegCopiesOnceOnAndroidWithoutOverwritingExistingBytes() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val fixture = File(app.cacheDir, "verified-photo-fixture-" + UUID.randomUUID().toString())
        check(fixture.mkdirs())
        try {
            val space = "81ed09c3-5713-4bca-a40c-fbb909a55f06"
            val id = "2e01c0b5-ec32-4f4c-8ea9-5ca7b4100cd1"
            val synthetic = ByteArray(3072) { ((it * 13 + 7) % 251).toByte() }
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(synthetic).joinToString("") { "%02x".format(it) }
            val photo = V290SpacePhotoEntity(
                id, space, 100L, 110L, "$id.jpg", hash, 24, 24, "",
            )
            val staged = File(fixture, "authenticated-stage.jpg")
            staged.writeBytes(synthetic)
            val targetRoot = File(fixture, "private-photo-root")
            val installer = StabilizationVerifiedPhotoInstaller(targetRoot)
            assertEquals(StabilizationPhotoInstallResult.INSTALLED,
                installer.installVerified(photo, staged, synthetic.size.toLong()))
            val target = File(File(targetRoot, space), "$id.jpg")
            assertArrayEquals(synthetic, target.readBytes())
            assertEquals(StabilizationPhotoInstallResult.ALREADY_VERIFIED,
                StabilizationVerifiedPhotoInstaller(targetRoot)
                    .installVerified(photo, staged, synthetic.size.toLong()))
            assertArrayEquals(synthetic, target.readBytes())
            val partial = File(target.parentFile, ".$id.restore.pending")
            assertFalse(partial.exists())
        } finally { fixture.deleteRecursively() }
    }
}
