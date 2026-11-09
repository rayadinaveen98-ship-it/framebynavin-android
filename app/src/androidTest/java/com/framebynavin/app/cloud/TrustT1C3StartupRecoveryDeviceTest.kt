package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable Android 15 fixture only. Never resets user data, clears Space
 * photos, deletes a real backup or uses actual accounts.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1C3StartupRecoveryDeviceTest {
    private val app get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext
    private val root get() = File(app.noBackupFilesDir, "trust-t1c3-local-review-v1")

    @Test fun crashDebrisIsRemovedButVerifiedAndUnrelatedFilesArePreserved() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val id = UUID.randomUUID().toString()
        val pending = File(root, "pending").apply { mkdirs() }
        val verified = File(root, "verified").apply { mkdirs() }
        val abandoned = File(pending, "pending-$id.enc.new").apply {
            writeText("crash in encrypted AtomicFile")
        }
        val encryptedGood = File(verified, "verified-$id.enc").apply {
            writeText("existing encrypted owner-review fixture")
        }
        val frozen = File(app.cacheDir, "trust-t1c2-owner-bytes-ci-$id").apply {
            mkdirs()
        }
        val frozenPart = File(frozen, "source-0000").apply {
            writeText("sensitive fixture should not survive crash recovery")
        }
        val decrypted = File(app.cacheDir, "backlot-trust-check-ci-$id").apply {
            mkdirs()
        }
        val decryptedPart = File(decrypted, "part-0001.bin").apply {
            writeText("discard this decrypted fixture")
        }
        val unrelated = File(app.cacheDir, "unrelated-owner-fixture-$id").apply {
            writeText("must never be deleted by backup cleanup")
        }
        try {
            TrustT1C3RecoveryJournal.mark(root, id, "ENCRYPTING")
            val report = TrustT1C3StartupRecovery.recoverOnMainActivityStartup(app)
            assertEquals(2, report.abandonedPlaintextDirectoriesCleaned)
            assertEquals(1, report.pendingEncryptedAttemptsCleaned)
            assertTrue(report.hadInterruptedJournal)
            assertFalse(abandoned.exists())
            assertFalse(frozen.exists())
            assertFalse(frozenPart.exists())
            assertFalse(decrypted.exists())
            assertFalse(decryptedPart.exists())
            assertTrue(encryptedGood.isFile)
            assertEquals("must never be deleted by backup cleanup", unrelated.readText())
            assertEquals(null, TrustT1C3RecoveryJournal.inspect(root))

            val again = TrustT1C3StartupRecovery.recoverOnMainActivityStartup(app)
            assertEquals(0, again.abandonedPlaintextDirectoriesCleaned)
            assertEquals(0, again.pendingEncryptedAttemptsCleaned)
            assertFalse(again.hadInterruptedJournal)
            assertTrue(encryptedGood.isFile)
        } finally {
            TrustT1C3RecoveryJournal.clear(root)
            abandoned.delete()
            encryptedGood.delete()
            frozen.deleteRecursively()
            decrypted.deleteRecursively()
            unrelated.delete()
        }
    }

    @Test fun activeCaptureIsNotRemovedOrOverriddenByStartupSweep() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val stage = File(app.cacheDir,
            "trust-t1c2-owner-bytes-active-" + UUID.randomUUID()).apply { mkdirs() }
        val part = File(stage, "source-0000").apply { writeText("active owner work") }
        TrustTemporaryStageRegistry.activate(stage)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    TrustT1C3StartupRecovery.recoverOnMainActivityStartup(app)
                }
            }
            assertEquals("active owner work", part.readText())
        } finally {
            TrustTemporaryStageRegistry.deactivate(stage)
        }
        val result = TrustT1C3StartupRecovery.recoverOnMainActivityStartup(app)
        assertTrue(result.abandonedPlaintextDirectoriesCleaned >= 1)
        assertFalse(part.exists())
    }

    @Test fun symlinkAndUnexpectedPendingArtifactsFailClosedWithoutDeletingAnything() =
        runBlocking {
            HardeningTestEnvironment.requireCiEmulator()
            val id = UUID.randomUUID().toString()
            val stage = File(app.cacheDir, "trust-t1c2-owner-bytes-ci-$id")
                .apply { mkdirs() }
            val owned = File(stage, "source-0000").apply { writeText("keep on suspicious state") }
            val target = File(app.cacheDir, "ordinary-owner-file-$id")
                .apply { writeText("outside private backup staging") }
            val symlink = File(stage, "unsafe-link")
            val pending = File(root, "pending").apply { mkdirs() }
            val unknown = File(pending, "not-a-backup-file-$id")
                .apply { writeText("unrecognized private file") }
            try {
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        TrustT1C3StartupRecovery.recoverOnMainActivityStartup(app)
                    }
                }
                assertTrue(owned.isFile)
                assertTrue(unknown.isFile)
                unknown.delete()
                Files.createSymbolicLink(symlink.toPath(), target.toPath())
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        TrustT1C3StartupRecovery.recoverOnMainActivityStartup(app)
                    }
                }
                assertTrue(owned.isFile)
                assertEquals("outside private backup staging", target.readText())
            } finally {
                Files.deleteIfExists(symlink.toPath())
                unknown.delete()
                stage.deleteRecursively()
                target.delete()
            }
    }
    @Test fun corruptCrashJournalRefusesUnverifiedCleanupAndNeverDeletesGoodCopy() =
        runBlocking {
            HardeningTestEnvironment.requireCiEmulator()
            val id = UUID.randomUUID().toString()
            val marker = File(root, "incomplete-preview-journal.json")
                .apply { parentFile!!.mkdirs(); writeText("unrecognized-owner-journal") }
            val stage = File(app.cacheDir, "trust-t1c2-owner-bytes-ci-$id")
                .apply { mkdirs() }
            val oldSource = File(stage, "source-0000")
                .apply { writeText("private fixture awaiting manual review") }
            val existing = File(root, "verified/verified-$id.enc").apply {
                parentFile!!.mkdirs()
                writeText("previous verified encrypted fixture")
            }
            try {
                assertThrows(Exception::class.java) {
                    runBlocking {
                        TrustT1C3StartupRecovery.recoverOnMainActivityStartup(app)
                    }
                }
                assertTrue(oldSource.isFile)
                assertTrue(existing.isFile)
                assertTrue(marker.isFile)
            } finally {
                TrustT1C3RecoveryJournal.clear(root)
                stage.deleteRecursively()
                existing.delete()
            }
        }

}
