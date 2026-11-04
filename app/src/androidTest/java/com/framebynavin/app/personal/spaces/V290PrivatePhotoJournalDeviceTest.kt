package com.framebynavin.app.personal.spaces

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Android 15 CI emulator: no camera, permissions, owner image or external storage. */
@RunWith(AndroidJUnit4::class)
class V290PrivatePhotoJournalDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val now = 1_792_050_000_000L
    private val id = "10000000-0000-4000-8000-000000000001"

    @Test fun jpegIsReencodedIntoPrivateStorageAndVerifiedByChecksum() {
        HardeningTestEnvironment.requireCiEmulator()
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val buffer = ByteArrayOutputStream()
        try {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, buffer))
        } finally {
            image.recycle()
        }
        val photoId = UUID.randomUUID().toString()
        val prepared = V290PrivatePhotoFiles.sanitizeImage(buffer.toByteArray(), photoId)
        assertEquals("$photoId.jpg", prepared.fileName)
        assertEquals(64, prepared.widthPx)
        assertEquals(64, prepared.heightPx)
        val saved = V290SpacePhotoEntity(
            id = photoId, spaceId = id, takenAtMillis = now,
            addedAtMillis = now, fileName = prepared.fileName,
            sha256Hex = prepared.sha256Hex,
            widthPx = prepared.widthPx, heightPx = prepared.heightPx, note = "",
        )
        try {
            val file = V290PrivatePhotoFiles.storePrepared(app, id, prepared)
            assertTrue(file.isFile)
            assertTrue(file.absolutePath.startsWith(app.filesDir.absolutePath))
            assertTrue(V290PrivatePhotoFiles.verifyPrivateBytes(app, saved))
            assertTrue(V290PrivatePhotoFiles.deleteFile(app, saved))
            assertFalse(file.exists())
        } finally {
            V290PrivatePhotoFiles.deleteFile(app, saved)
        }
    }

    @Test fun badImagesAndTraversalNamesAreRejectedBeforeDiskWrite() {
        HardeningTestEnvironment.requireCiEmulator()
        val id1 = UUID.randomUUID().toString()
        assertTrue(runCatching {
            V290PrivatePhotoFiles.sanitizeImage("not-image".toByteArray(), id1)
        }.isFailure)
        assertTrue(runCatching {
            V290PrivatePhotoFiles.sanitizeImage(byteArrayOf(), id1)
        }.isFailure)
        val illegal = V290SpacePhotoEntity(
            id = id1, spaceId = id, takenAtMillis = now, addedAtMillis = now,
            fileName = "../escape.jpg", sha256Hex = "a".repeat(64),
            widthPx = 16, heightPx = 16, note = "",
        )
        assertTrue(runCatching { V290PrivatePhotoFiles.verifyPrivateBytes(app, illegal) }.isFailure)
    }

    @Test fun migratingExistingVersionOnePreservesSpaceAndJournal() {
        HardeningTestEnvironment.requireCiEmulator()
        val dbName = "v290-photo-migration-" + UUID.randomUUID() + ".db"
        val file = app.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        app.deleteDatabase(dbName)
        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            legacy.execSQL("""
                CREATE TABLE IF NOT EXISTS personal_spaces (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL, type TEXT NOT NULL, iconKey TEXT NOT NULL,
                    goalText TEXT NOT NULL, createdAtMillis INTEGER NOT NULL,
                    archivedAtMillis INTEGER, displayOrder INTEGER NOT NULL
                )
            """.trimIndent())
            legacy.execSQL("CREATE INDEX IF NOT EXISTS index_personal_spaces_displayOrder ON personal_spaces(displayOrder)")
            legacy.execSQL("CREATE INDEX IF NOT EXISTS index_personal_spaces_createdAtMillis ON personal_spaces(createdAtMillis)")
            legacy.execSQL("""
                CREATE TABLE IF NOT EXISTS personal_space_reminder_links (
                    spaceId TEXT NOT NULL, reminderId TEXT NOT NULL,
                    linkedAtMillis INTEGER NOT NULL,
                    PRIMARY KEY(spaceId, reminderId),
                    FOREIGN KEY(spaceId) REFERENCES personal_spaces(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            legacy.execSQL("CREATE INDEX IF NOT EXISTS index_personal_space_reminder_links_reminderId ON personal_space_reminder_links(reminderId)")
            legacy.execSQL("""
                CREATE TABLE IF NOT EXISTS personal_space_entries (
                    id TEXT NOT NULL PRIMARY KEY,
                    spaceId TEXT NOT NULL, kind TEXT NOT NULL,
                    source TEXT NOT NULL, occurredAtMillis INTEGER NOT NULL,
                    createdAtMillis INTEGER NOT NULL, note TEXT NOT NULL,
                    metricName TEXT, metricValue REAL, metricUnit TEXT,
                    reminderId TEXT, originalOccurrenceMillis INTEGER,
                    FOREIGN KEY(spaceId) REFERENCES personal_spaces(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            legacy.execSQL("CREATE INDEX IF NOT EXISTS index_personal_space_entries_spaceId_occurredAtMillis ON personal_space_entries(spaceId, occurredAtMillis)")
            legacy.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_personal_space_entries_spaceId_reminderId_originalOccurrenceMillis_source ON personal_space_entries(spaceId, reminderId, originalOccurrenceMillis, source)")
            legacy.execSQL("""
                INSERT INTO personal_spaces
                    (id,title,type,iconKey,goalText,createdAtMillis,archivedAtMillis,displayOrder)
                VALUES ('$id','My skin progress','SKIN_CARE','sun','Gentle routine', $now, NULL,0)
            """.trimIndent())
            legacy.execSQL("""
                INSERT INTO personal_space_entries
                    (id,spaceId,kind,source,occurredAtMillis,createdAtMillis,note,metricName,metricValue,metricUnit,reminderId,originalOccurrenceMillis)
                VALUES ('20000000-0000-4000-8000-000000000001','$id','CHECK_IN','MANUAL',$now,$now,'Face wash',NULL,NULL,NULL,NULL,NULL)
            """.trimIndent())
            legacy.version = 1
        } finally {
            legacy.close()
        }

        val upgraded = Room.databaseBuilder(app, V289SpacesDatabase::class.java, dbName)
            .addMigrations(V290SpacePhotosMigration.MIGRATION_1_2)
            .build()
        try {
            runBlocking {
                val dao = upgraded.spaces()
                assertEquals("My skin progress", dao.loadSpaces().single().title)
                assertEquals("Face wash", dao.entriesForSpace(id).single().note)
                assertTrue(dao.photosForSpace(id).isEmpty())
                val photoId = UUID.randomUUID().toString()
                val record = V290SpacePhotoEntity(
                    id = photoId, spaceId = id, takenAtMillis = now,
                    addedAtMillis = now, fileName = "$photoId.jpg",
                    sha256Hex = "b".repeat(64), widthPx = 100,
                    heightPx = 100, note = "baseline"
                )
                dao.insertPhoto(record)
                assertNotNull(dao.getPhoto(photoId))
                assertTrue(runCatching {
                    V289SpacesRepository(dao).deleteSpaceAfterOwnerConfirmation(id)
                }.isFailure)
                assertEquals(1, dao.loadSpaces().size)
                assertEquals(1, dao.deletePhoto(photoId))
                assertTrue(V289SpacesRepository(dao).deleteSpaceAfterOwnerConfirmation(id))
            }
        } finally {
            upgraded.close()
            app.deleteDatabase(dbName)
        }
    }
}
