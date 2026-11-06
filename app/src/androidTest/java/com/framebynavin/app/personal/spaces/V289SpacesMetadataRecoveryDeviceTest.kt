package com.framebynavin.app.personal.spaces

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

/**
 * Purely isolated in-memory Room tests. No file-picker, cloud upload, photo or
 * Personal reminder restore is invoked. CI emulator only.
 */
@RunWith(AndroidJUnit4::class)
class V289SpacesMetadataRecoveryDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val now = 1_792_050_000_000L
    private val spaceId = "10000000-0000-4000-8000-000000000001"
    private val entryId = "20000000-0000-4000-8000-000000000001"
    private val knownReminder = "owner-reminder-not-created-here"

    private val savedSpace by lazy {
        V289PersonalSpacePolicy.newSpace(V289SpaceType.SKIN_CARE, now, spaceId)
    }
    private val savedEntry by lazy {
        V289SpaceEntry(
            id = entryId, spaceId = spaceId,
            kind = V289SpaceEntryKind.CHECK_IN,
            source = V289SpaceEntrySource.MANUAL,
            occurredAtMillis = now + 1000,
            createdAtMillis = now + 1000,
            note = "Face wash and sunscreen",
        )
    }

    @Test fun metadataExportRestoreIsIdempotentWithoutTouchingPersonalReminders() {
        HardeningTestEnvironment.requireCiEmulator()
        val personalBefore = runBlocking { PersonalReminderStore(app).load() }
        val first = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val second = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        try {
            runBlocking {
                val owner = V289SpacesRepository(first.spaces())
                owner.createSpace(savedSpace)
                owner.logManualEntry(savedEntry)
                owner.linkExistingReminder(
                    V289SpaceReminderLink(spaceId, knownReminder, now),
                    existingReminderIds = setOf(knownReminder)
                )
                val raw = V289SpacesMetadataRecovery(app, first).prepareLocalMetadataExport()
                val recovery = V289SpacesMetadataRecovery(app, second)
                val report = recovery.mergeVerifiedMetadata(raw, setOf(knownReminder))
                assertEquals(1, report.spacesAdded)
                assertEquals(1, report.linksAdded)
                assertEquals(1, report.entriesAdded)
                assertEquals(1, second.spaces().loadSpaces().size)
                assertEquals(1, second.spaces().allEntries().size)
                assertEquals(1, second.spaces().allLinks().size)
                val repeated = recovery.mergeVerifiedMetadata(raw, setOf(knownReminder))
                assertEquals(0, repeated.spacesAdded)
                assertEquals(0, repeated.linksAdded)
                assertEquals(0, repeated.entriesAdded)
                assertEquals(personalBefore, PersonalReminderStore(app).load())
            }
        } finally {
            first.close()
            second.close()
        }
    }

    @Test fun missingReminderReferencesFailWithoutImportingPartialSpaces() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        try {
            runBlocking {
                val incoming = V289SpaceState(
                    spaces = listOf(savedSpace),
                    links = listOf(V289SpaceReminderLink(spaceId, knownReminder, now)),
                    entries = listOf(savedEntry),
                )
                val raw = V289SpacesMetadataCodec.encode(incoming)
                assertTrue(runCatching {
                    V289SpacesMetadataRecovery(app, db).mergeVerifiedMetadata(raw, emptySet())
                }.isFailure)
                assertTrue(db.spaces().loadSpaces().isEmpty())
                assertTrue(db.spaces().allEntries().isEmpty())
                assertTrue(db.spaces().allLinks().isEmpty())
            }
        } finally {
            db.close()
        }
    }

    @Test fun conflictsAndInvalidChecksumNeverOverwriteExistingJourneys() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        try {
            runBlocking {
                val repo = V289SpacesRepository(db.spaces())
                repo.createSpace(savedSpace.copy(title = "Owner's private progress"))
                val freshSpace = V289PersonalSpacePolicy.newSpace(
                    V289SpaceType.READING, now,
                    "10000000-0000-4000-8000-000000000002",
                )
                val incoming = V289SpaceState(
                    spaces = listOf(freshSpace, savedSpace),
                    entries = listOf(savedEntry),
                )
                val raw = V289SpacesMetadataCodec.encode(incoming)
                assertTrue(runCatching {
                    V289SpacesMetadataRecovery(app, db).mergeVerifiedMetadata(raw, emptySet())
                }.isFailure)
                assertEquals(1, db.spaces().loadSpaces().size)
                assertEquals("Owner's private progress",
                    db.spaces().loadSpaces().single().title)
                assertTrue(db.spaces().allEntries().isEmpty())
                val corrupt = JSONObject(raw).put("payloadSha256", "0".repeat(64)).toString()
                assertTrue(runCatching {
                    V289SpacesMetadataRecovery(app, db).mergeVerifiedMetadata(corrupt, emptySet())
                }.isFailure)
                assertEquals(1, db.spaces().loadSpaces().size)
            }
        } finally {
            db.close()
        }
    }
}
