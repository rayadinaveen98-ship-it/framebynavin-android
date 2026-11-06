package com.framebynavin.app.personal.spaces

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** CI-emulator only, never runs against owner data or a real-device database. */
@RunWith(AndroidJUnit4::class)
class V289SpacesDatabaseTest {
    private val app: Context get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val now = 1_792_050_000_000L
    private val spaceId = "30000000-0000-4000-8000-000000000001"
    private val entryId = "40000000-0000-4000-8000-000000000001"

    @Test fun explicitCreationPersistsAcrossCloseReopenWithoutTouchingPersonalDataStore() {
        HardeningTestEnvironment.requireCiEmulator()
        val before = runBlocking { PersonalReminderStore(app).load() }
        val file = "v289-test-" + UUID.randomUUID() + ".db"
        val record = V289PersonalSpacePolicy.newSpace(V289SpaceType.SKIN_CARE, now, spaceId)
        try {
            val first = Room.databaseBuilder(app, V289SpacesDatabase::class.java, file).build()
            try {
                runBlocking {
                    V289SpacesRepository(app, first.spaces()).createSpace(record)
                    assertEquals(1, first.spaces().loadSpaces().size)
                }
            } finally {
                first.close()
            }
            val reopened = Room.databaseBuilder(app, V289SpacesDatabase::class.java, file).build()
            try {
                runBlocking {
                    val value = reopened.spaces().getSpace(spaceId)
                    assertNotNull(value)
                    assertEquals("Skin Care", value?.title)
                    assertTrue(reopened.spaces().entriesForSpace(spaceId).isEmpty())
                }
            } finally {
                reopened.close()
            }
            assertEquals(before, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            app.deleteDatabase(file)
        }
    }

    @Test fun deletingSpaceCascadesOnlyItsOwnLinksAndEntries() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        try {
            runBlocking {
                val dao = db.spaces()
                val repository = V289SpacesRepository(app, dao)
                val first = V289PersonalSpacePolicy.newSpace(V289SpaceType.FITNESS, now, spaceId)
                val second = V289PersonalSpacePolicy.newSpace(
                    V289SpaceType.READING, now, "30000000-0000-4000-8000-000000000002"
                )
                repository.createSpace(first)
                repository.createSpace(second)
                val link = V289SpaceReminderLink(first.id, "owner-reminder-stable-id", now)
                assertTrue(runCatching {
                    repository.linkExistingReminder(link, emptySet())
                }.isFailure)
                assertTrue(repository.linkExistingReminder(
                    link, setOf("owner-reminder-stable-id")
                ))
                assertTrue(repository.logManualEntry(
                    V289SpaceEntry(
                        id = entryId, spaceId = first.id,
                        kind = V289SpaceEntryKind.CHECK_IN,
                        source = V289SpaceEntrySource.MANUAL,
                        occurredAtMillis = now, createdAtMillis = now,
                    )
                ))
                assertTrue(repository.deleteSpaceAfterOwnerConfirmation(first.id))
                assertTrue(dao.linksForSpace(first.id).isEmpty())
                assertTrue(dao.entriesForSpace(first.id).isEmpty())
                assertEquals(second.id, dao.loadSpaces().single().id)
                // Personal reminder records are in a separate DataStore and untouched.
            }
        } finally {
            db.close()
        }
    }

    @Test fun repositoryDoesNotAcceptForgedDoneOrInvalidMetrics() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        try {
            runBlocking {
                val repository = V289SpacesRepository(app, db.spaces())
                repository.createSpace(
                    V289PersonalSpacePolicy.newSpace(V289SpaceType.EDITING, now, spaceId)
                )
                val forged = V289SpaceEntry(
                    id = entryId, spaceId = spaceId,
                    kind = V289SpaceEntryKind.CHECK_IN,
                    source = V289SpaceEntrySource.CONFIRMED_REMINDER_DONE,
                    occurredAtMillis = now, createdAtMillis = now,
                    reminderId = "a-reminder", originalOccurrenceMillis = now,
                )
                assertTrue(runCatching { repository.logManualEntry(forged) }.isFailure)
                assertTrue(runCatching {
                    repository.logManualEntry(forged.copy(
                        source = V289SpaceEntrySource.MANUAL,
                        reminderId = null,
                        originalOccurrenceMillis = null,
                        kind = V289SpaceEntryKind.METRIC,
                        metricName = "Minutes", metricValue = Double.NaN,
                        metricUnit = "min",
                    ))
                }.isFailure)
                assertTrue(db.spaces().entriesForSpace(spaceId).isEmpty())
            }
        } finally {
            db.close()
        }
    }

    @Test fun sqliteDeduplicatesAcceptedReceiptButAllowsIndependentManualNotes() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        try {
            runBlocking {
                val dao = db.spaces()
                V289SpacesRepository(app, dao).createSpace(
                    V289PersonalSpacePolicy.newSpace(V289SpaceType.CUSTOM, now, spaceId)
                )
                val receipt = V289SpaceEntryEntity(
                    id = entryId, spaceId = spaceId, kind = "CHECK_IN",
                    source = "CONFIRMED_REMINDER_DONE", occurredAtMillis = now,
                    createdAtMillis = now, note = "",
                    metricName = null, metricValue = null, metricUnit = null,
                    reminderId = "some-reminder", originalOccurrenceMillis = now,
                )
                assertTrue(dao.insertEntry(receipt) > 0L)
                assertEquals(-1L, dao.insertEntry(receipt.copy(
                    id = "40000000-0000-4000-8000-000000000002"
                )))
                val note = receipt.copy(
                    id = "40000000-0000-4000-8000-000000000003",
                    kind = "NOTE", source = "MANUAL",
                    reminderId = null, originalOccurrenceMillis = null,
                )
                assertTrue(dao.insertEntry(note) > 0L)
                assertTrue(dao.insertEntry(note.copy(
                    id = "40000000-0000-4000-8000-000000000004"
                )) > 0L)
                assertEquals(3, dao.entriesForSpace(spaceId).size)
                assertFalse(dao.loadSpaces().isEmpty())
            }
        } finally {
            db.close()
        }
    }
}
