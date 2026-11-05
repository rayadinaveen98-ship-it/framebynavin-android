package com.framebynavin.app.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** No account, storage or cloud is needed for truthful backup coverage copy. */
class V292BackupInventoryCopyTest {
    @Test fun noUnusedSpacesDatabaseDoesNotClaimFullBackup() {
        assertNull(V292BackupInventoryCopy.summary(V292LocalBackupInventory.NoSpacesDatabase))
    }

    @Test fun zeroRecordsAvoidUnnecessaryHomeScreenNoise() {
        val empty = V292LocalBackupInventory.Recorded(0, 0, 0)
        assertFalse(empty.hasUnprotectedRecords)
        assertNull(V292BackupInventoryCopy.summary(empty))
    }

    @Test fun existingEntriesAndMediaAreExplicitlyExcluded() {
        val populated = V292LocalBackupInventory.Recorded(
            spaceCount = 2, journalEntryCount = 14, photoRecordCount = 3
        )
        assertTrue(populated.hasUnprotectedRecords)
        val message = requireNotNull(V292BackupInventoryCopy.summary(populated))
        assertTrue(message.contains("2 Spaces"))
        assertTrue(message.contains("14 journal entries"))
        assertTrue(message.contains("3 private photo records"))
        assertTrue(message.contains("None of these are included in Sync Now"))
        assertTrue(message.contains("Photo files weren't verified"))
        assertFalse(message.contains("fully backed up"))
    }

    @Test fun databaseFailureNeverPretendsThereIsNothingToProtect() {
        val message = requireNotNull(V292BackupInventoryCopy.summary(
            V292LocalBackupInventory.Unavailable
        ))
        assertTrue(message.contains("couldn't be checked"))
        assertTrue(message.contains("unknown"))
    }

    @Test fun invalidCountCannotBeMisreportedAsZero() {
        assertTrue(runCatching {
            V292LocalBackupInventory.Recorded(-1, 0, 0)
        }.isFailure)
        assertTrue(runCatching {
            V292LocalBackupInventory.Recorded(0, 2, -3)
        }.isFailure)
    }
}
