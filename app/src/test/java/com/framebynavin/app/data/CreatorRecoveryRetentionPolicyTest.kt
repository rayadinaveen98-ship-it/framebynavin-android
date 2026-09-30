package com.framebynavin.app.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorRecoveryRetentionPolicyTest {
    @Test
    fun `keeps newest five recovery copies`() {
        val dir = Files.createTempDirectory("backlot-recovery-retention").toFile()
        try {
            val copies = (1..7).map { index ->
                File(dir, "pre-restore-$index.fbnbackup").apply {
                    writeText("backup-$index")
                    assertTrue(setLastModified(index * 1_000L))
                }
            }

            val toDelete = CreatorRecoveryRetentionPolicy.filesToDelete(copies)

            assertEquals(
                setOf("pre-restore-1.fbnbackup", "pre-restore-2.fbnbackup"),
                toDelete.map { it.name }.toSet(),
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `does not delete anything while history is within limit`() {
        val dir = Files.createTempDirectory("backlot-recovery-retention-small").toFile()
        try {
            val copies = (1..CreatorRecoveryRetentionPolicy.MAX_RECOVERY_COPIES).map { index ->
                File(dir, "pre-restore-$index.fbnbackup").apply {
                    writeText("backup-$index")
                    assertTrue(setLastModified(index * 1_000L))
                }
            }

            assertTrue(CreatorRecoveryRetentionPolicy.filesToDelete(copies).isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requires at least one retained recovery copy`() {
        CreatorRecoveryRetentionPolicy.filesToDelete(emptyList(), maxCopies = 0)
    }
}
