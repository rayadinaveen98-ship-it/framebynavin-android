package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpaceEntryEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android 15 synthetic CI-only coverage tests. No real owner device, Google
 * account, photo bytes, or cloud upload. Never run this fixture on a phone.
 */
@RunWith(AndroidJUnit4::class)
class V292LocalBackupInventoryDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val dbFile get() = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
    private val spaceId = "10000000-0000-4000-8000-000000000099"

    @Before fun resetSyntheticDatabase() {
        HardeningTestEnvironment.requireCiEmulator()
        assertTrue("Spaces DB fixture must be disposable", app.deleteDatabase(V289SpacesDatabase.FILE_NAME))
        // deleteDatabase returns true on some API levels for absent databases.
        // Even if no file existed, nothing in an owner profile is accessible.
    }

    @After fun disposeSyntheticDatabase() {
        HardeningTestEnvironment.requireCiEmulator()
        app.deleteDatabase(V289SpacesDatabase.FILE_NAME)
    }

    @Test fun openingCoverageAuditDoesNotCreateUnusedDatabase() = runBlocking {
        assertFalse(dbFile.exists())
        val result = V292LocalBackupInventoryReader(app).inspect()
        assertEquals(V292LocalBackupInventory.NoSpacesDatabase, result)
        assertFalse("Read-only coverage check should not create an empty database", dbFile.exists())
    }

    @Test fun countsExistingSpaceNotesAndPhotoRecordsWithoutDeletingData() = runBlocking {
        val beforeReminders = PersonalReminderStore(app).load()
        val db = V289SpacesDatabase.open(app)
        try {
            val dao = db.spaces()
            val timestamp = 1_792_050_000_000L
            dao.insertSpace(V289SpaceEntity(
                id = spaceId,
                title = "Synthetic workout",
                type = "FITNESS",
                iconKey = "dumbbell",
                goalText = "Work out regularly",
                createdAtMillis = timestamp,
                archivedAtMillis = null,
                displayOrder = 0,
            ))
            dao.insertEntry(V289SpaceEntryEntity(
                id = "20000000-0000-4000-8000-000000000099",
                spaceId = spaceId,
                kind = "CHECK_IN",
                source = "MANUAL",
                occurredAtMillis = timestamp,
                createdAtMillis = timestamp,
                note = "CI synthetic entry",
                metricName = null,
                metricValue = null,
                metricUnit = null,
                reminderId = null,
                originalOccurrenceMillis = null,
            ))
            dao.insertPhoto(V290SpacePhotoEntity(
                id = "30000000-0000-4000-8000-000000000099",
                spaceId = spaceId,
                takenAtMillis = timestamp,
                addedAtMillis = timestamp,
                fileName = "30000000-0000-4000-8000-000000000099.jpg",
                sha256Hex = "0".repeat(64),
                widthPx = 64,
                heightPx = 64,
                note = "Synthetic media ROW; byte file deliberately not present",
            ))
        } finally {
            db.close()
        }

        val result = V292LocalBackupInventoryReader(app).inspect()
        assertEquals(V292LocalBackupInventory.Recorded(1, 1, 1), result)
        assertTrue(dbFile.exists())
        val reopened = V289SpacesDatabase.open(app)
        try {
            assertEquals("Synthetic workout", reopened.spaces().getSpace(spaceId)?.title)
            assertEquals(1, reopened.spaces().photosForSpace(spaceId).size)
            assertEquals(1, reopened.spaces().entriesForSpace(spaceId).size)
        } finally {
            reopened.close()
        }
        assertEquals(beforeReminders, PersonalReminderStore(app).load())
    }

    @Test fun corruptedExistingDatabaseFailsClosedRatherThanReportingZero() = runBlocking {
        assertTrue(dbFile.parentFile?.mkdirs() == true || dbFile.parentFile?.isDirectory == true)
        dbFile.writeBytes("NOT A SQLITE DB".toByteArray())
        assertTrue(dbFile.exists())
        assertEquals(
            V292LocalBackupInventory.Unavailable,
            V292LocalBackupInventoryReader(app).inspect(),
        )
        // No destructive recovery/auto-rebuild should occur during a read-only audit.
    }
}
