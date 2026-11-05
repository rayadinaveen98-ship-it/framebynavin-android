package com.framebynavin.app.personal.spaces

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CI emulator synthetic media only. No real camera, photo picker, accounts,
 * owner data, cloud operations or exported backup document.
 */
@RunWith(AndroidJUnit4::class)
class V293PhotoBackupPreflightDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun metadataOnlyPreflightReadsButDoesNotChangeAnyReminders() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val beforePersonal = PersonalReminderStore(app).load()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        try {
            val space = V289PersonalSpacePolicy.newSpace(
                V289SpaceType.READING,
                System.currentTimeMillis(),
                UUID.randomUUID().toString(),
            )
            V289SpacesRepository(db.spaces()).createSpace(space)
            val candidate = V293PhotoBackupPreflight(app, db).inspect()
            assertEquals(0, candidate.photoCount)
            assertEquals(space.id,
                V289SpacesMetadataCodec.decode(candidate.metadataJson).spaces.single().id)
            assertEquals(1, db.spaces().loadSpaces().size)
            assertEquals(beforePersonal, PersonalReminderStore(app).load())
        } finally {
            db.close()
        }
    }

    @Test fun corruptOrMissingPrivateJpegBlocksWholeBackupCandidate() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val beforePersonal = PersonalReminderStore(app).load()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val spaceId = UUID.randomUUID().toString()
        var stored: V290SpacePhotoEntity? = null
        try {
            V289SpacesRepository(db.spaces()).createSpace(
                V289PersonalSpacePolicy.newSpace(
                    V289SpaceType.SKIN_CARE,
                    System.currentTimeMillis(),
                    spaceId,
                )
            )
            val image = Bitmap.createBitmap(48, 72, Bitmap.Config.ARGB_8888)
            val png = ByteArrayOutputStream()
            try {
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, png))
            } finally {
                image.recycle()
            }
            val prepared = V290PrivatePhotoFiles.sanitizeImage(
                png.toByteArray(), UUID.randomUUID().toString()
            )
            val photo = V290SpacePhotoEntity(
                id = prepared.id, spaceId = spaceId,
                takenAtMillis = 1_792_050_000_000L,
                addedAtMillis = 1_792_050_000_001L,
                fileName = prepared.fileName,
                sha256Hex = prepared.sha256Hex,
                widthPx = prepared.widthPx,
                heightPx = prepared.heightPx,
                note = "Synthetic baseline only",
            )
            val file = V290PrivatePhotoFiles.storePrepared(app, spaceId, prepared)
            stored = photo
            db.spaces().insertPhoto(photo)
            val preflight = V293PhotoBackupPreflight(app, db)
            val good = preflight.inspect()
            assertEquals(1, good.photoCount)
            assertEquals(photo.sha256Hex, good.photos.single().sha256Hex)
            assertEquals(1, db.spaces().photosForSpace(spaceId).size)
            assertEquals(beforePersonal, PersonalReminderStore(app).load())

            file.writeBytes("corrupted synthetic JPEG".toByteArray())
            assertTrue("Changed bytes must block a supposedly complete backup",
                runCatching { preflight.inspect() }.isFailure)
            assertEquals(1, db.spaces().photosForSpace(spaceId).size)
            assertTrue(file.delete())
            assertTrue("Missing bytes must block, not silently omit the photo",
                runCatching { preflight.inspect() }.isFailure)
            assertEquals(beforePersonal, PersonalReminderStore(app).load())
        } finally {
            stored?.let { V290PrivatePhotoFiles.deleteFile(app, it) }
            db.close()
        }
    }
}
