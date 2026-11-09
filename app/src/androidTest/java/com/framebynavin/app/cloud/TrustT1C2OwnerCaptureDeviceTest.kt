package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalBackupTransfer
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.Phase1HabitStore
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

/**
 * Uses the real, persistent owners' storage readers on a disposable CI emulator.
 * No cloud sessions, fixture reset, imports, deletion, alarms or production APK.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1C2OwnerCaptureDeviceTest {
    @Test fun frozenCreatorAndPersonalFilesAreActuallyReadableAndNeverClaimUniversal() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val beforeProjects = TaskStore(app).exportJsonForTrustReadOnly()
        val beforeIdeas = IdeaVaultStore(app).exportJson()
        val beforePersonal = PersonalReminderStore(app).exportPersonalJson()
        val beforePortablePersonal = PersonalBackupTransfer(app).exportForCloud()
        val before21Day = Phase1HabitStore(app).exportJson()

        val frozen = TrustT1C2OwnerSourceCapture(app).freezePartialReadOnly()
        val stagedFile = File.createTempFile(
            "ci-trust-t1c2-encrypted-", ".bundle", app.cacheDir,
        )
        val passphrase = "ci-owner-payload-strong-passphrase".toCharArray()
        try {
            assertFalse(frozen.canClaimCompleteBackup)
            assertFalse(frozen.canUploadAsUniversalBackup)
            val indexed = frozen.parts.associateBy { it.id }
            assertTrue("Projects must have actual frozen bytes", "creator/projects" in indexed)
            assertTrue("Focus must have actual frozen bytes", "creator/focus" in indexed)
            assertTrue("Ideas must have actual frozen bytes", "creator/ideas" in indexed)
            assertTrue("Reminders and Done instants are not optional",
                "personal/reminders-history" in indexed)
            assertTrue("Single activity Done receipts are not optional",
                "personal/single-activity-receipts" in indexed)
            assertArrayEquals(beforeProjects.toByteArray(),
                indexed.getValue("creator/projects").file.readBytes())
            assertArrayEquals(beforeIdeas.toByteArray(),
                indexed.getValue("creator/ideas").file.readBytes())
            assertArrayEquals(beforePortablePersonal.toByteArray(),
                indexed.getValue("personal/reminders-history").file.readBytes())
            assertArrayEquals(before21Day.toByteArray(),
                indexed.getValue("personal/single-activity-receipts").file.readBytes())

            // The follow-up encrypted staging step references ONLY copied
            // immutable files; original Creator/Personal stores aren't read.
            stagedFile.outputStream().use { stream ->
                TrustEncryptedStagingCodec.write(
                    "disposable-owner-on-emulator", passphrase,
                    frozen.sources(), stream,
                )
            }
            assertTrue(stagedFile.isFile && stagedFile.length() > 1000)
            stagedFile.inputStream().use { source ->
                TrustEncryptedStagingCodec.stageAndVerify(
                    "disposable-owner-on-emulator", passphrase,
                    source, app.cacheDir,
                ).use { verified ->
                    val same = verified.parts.associateBy { it.id }
                    assertEquals(frozen.parts.size, verified.parts.size)
                    assertArrayEquals(before21Day.toByteArray(),
                        same.getValue("personal/single-activity-receipts").file.readBytes())
                    assertFalse(verified.canClaimCompleteBackup)
                    assertFalse(verified.canUploadAsUniversalBackup)
                }
            }
            assertEquals(beforeProjects, TaskStore(app).exportJsonForTrustReadOnly())
            assertEquals(beforeIdeas, IdeaVaultStore(app).exportJson())
            assertEquals(beforePersonal, PersonalReminderStore(app).exportPersonalJson())
            assertEquals(before21Day, Phase1HabitStore(app).exportJson())
        } finally {
            passphrase.fill('\u0000')
            frozen.close()
            stagedFile.delete()
        }
    }
}
