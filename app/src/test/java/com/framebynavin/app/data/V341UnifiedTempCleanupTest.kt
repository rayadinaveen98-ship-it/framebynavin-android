package com.framebynavin.app.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V341UnifiedTempCleanupTest {
    @Test fun onlyOldV341PrivateStagingIsRemoved() {
        val sandbox = Files.createTempDirectory("backlot-v341-policy-test-").toFile()
        val now = 1_900_000_000_000L
        try {
            val oldExport = File(sandbox, "backlot-unified-export-old").apply {
                mkdirs()
                File(this, "private-owner.tmp").writeText("synthetic")
                setLastModified(now - 30L * 3_600_000L)
            }
            val oldStage = File(sandbox, "backlot-unified-stage-old").apply {
                mkdirs()
                File(this, "staged.tmp").writeText("synthetic")
                setLastModified(now - 30L * 3_600_000L)
            }
            val current = File(sandbox, "backlot-unified-export-current").apply {
                mkdirs()
                setLastModified(now - 2L * 3_600_000L)
            }
            val unrelatedRecovery = File(sandbox, "personal-owner-rollback").apply {
                mkdirs()
                setLastModified(now - 120L * 3_600_000L)
            }
            assertEquals(2, V341UnifiedTempCleanup.prune(sandbox, now))
            assertTrue(!oldExport.exists())
            assertTrue(!oldStage.exists())
            assertTrue(current.exists())
            assertTrue(unrelatedRecovery.exists())
        } finally {
            sandbox.deleteRecursively()
        }
    }

    @Test fun missingCacheIsNotTreatedAsOwnerConsentForDeletion() {
        val root = Files.createTempDirectory("backlot-v341-empty-").toFile()
        try {
            assertEquals(0, V341UnifiedTempCleanup.prune(File(root, "missing"), 1000L))
        } finally {
            root.deleteRecursively()
        }
    }
}
