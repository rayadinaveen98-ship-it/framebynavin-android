package com.framebynavin.app.upgrade

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpaceEntryEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290PrivatePhotoFiles
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic V305 -> V309 in-place upgrade fixture on an isolated Android 15 emulator.
 * No Photo Picker, gallery, camera, real identity, cloud account, or owner data.
 * Designed to prove existing Room data AND on-disk private JPEG bytes survive
 * adb install -r (no uninstall/clear between phases).
 */
@RunWith(AndroidJUnit4::class)
class V309FromV305SpacePhotoUpgradeDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val spaceId = "10000000-0000-4000-8000-000000000041"
    private val journalId = "20000000-0000-4000-8000-000000000041"
    private val photoId = "30000000-0000-4000-8000-000000000041"
    private val now = 1_792_050_000_000L

    private fun requireVersion(expected: Long) {
        assertEquals(
            "Wrong installed signed app version",
            expected,
            app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode
        )
    }

    @Test fun seedOnV305() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        requireVersion(296L)
        val db = V289SpacesDatabase.open(app)
        try {
            val dao = db.spaces()
            assertTrue("Fixture must start empty", dao.loadSpaces().isEmpty())
            dao.insertSpace(
                V289SpaceEntity(
                    id = spaceId,
                    title = "Synthetic skincare journal",
                    type = "SKIN_CARE",
                    iconKey = "sun",
                    goalText = "Record progress safely",
                    createdAtMillis = now,
                    archivedAtMillis = null,
                    displayOrder = 0,
                )
            )
            dao.insertEntry(
                V289SpaceEntryEntity(
                    id = journalId,
                    spaceId = spaceId,
                    kind = "CHECK_IN",
                    source = "MANUAL",
                    occurredAtMillis = now,
                    createdAtMillis = now,
                    note = "Baseline fixture note",
                    metricName = null,
                    metricValue = null,
                    metricUnit = null,
                    reminderId = null,
                    originalOccurrenceMillis = null,
                )
            )

            val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
            val raw = ByteArrayOutputStream()
            try {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, raw))
            } finally {
                bitmap.recycle()
            }
            val prepared = V290PrivatePhotoFiles.sanitizeImage(raw.toByteArray(), photoId)
            val photo = V290SpacePhotoEntity(
                id = photoId,
                spaceId = spaceId,
                takenAtMillis = now,
                addedAtMillis = now,
                fileName = prepared.fileName,
                sha256Hex = prepared.sha256Hex,
                widthPx = prepared.widthPx,
                heightPx = prepared.heightPx,
                note = "Before photo",
            )
            val savedFile = V290PrivatePhotoFiles.storePrepared(app, spaceId, prepared)
            assertTrue(savedFile.isFile)
            dao.insertPhoto(photo)
            assertEquals("Baseline fixture note", dao.entriesForSpace(spaceId).single().note)
            assertTrue(V290PrivatePhotoFiles.verifyPrivateBytes(app, photo))
        } finally {
            db.close()
        }
    }

    @Test fun verifyOnV309() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        requireVersion(309L)
        val db = V289SpacesDatabase.open(app)
        try {
            val dao = db.spaces()
            val space = requireNotNull(dao.getSpace(spaceId)) { "Space lost across upgrade" }
            assertEquals("Synthetic skincare journal", space.title)
            assertEquals("Record progress safely", space.goalText)
            assertEquals("Baseline fixture note", dao.entriesForSpace(spaceId).single().note)
            val photo = requireNotNull(dao.getPhoto(photoId)) {
                "Photo database row lost across upgrade"
            }
            assertEquals("Before photo", photo.note)
            assertEquals(80, photo.widthPx)
            assertEquals(120, photo.heightPx)
            assertTrue(
                "Previously saved private JPEG bytes missing/corrupted after upgrade",
                V290PrivatePhotoFiles.verifyPrivateBytes(app, photo),
            )
            assertEquals(1, dao.photosForSpace(spaceId).size)
        } finally {
            db.close()
        }
    }
}
