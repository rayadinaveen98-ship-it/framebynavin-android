package com.framebynavin.app.personal.spaces

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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** All sample images are synthetic and remain in test app-private storage. */
@RunWith(AndroidJUnit4::class)
class V290InterruptedPhotoRecoveryDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun syntheticPhoto(): V290PrivatePhotoFiles.PreparedPhoto {
        val image = Bitmap.createBitmap(40, 42, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        try {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out))
        } finally {
            image.recycle()
        }
        return V290PrivatePhotoFiles.sanitizeImage(
            out.toByteArray(), UUID.randomUUID().toString()
        )
    }

    private fun newSpace() = V289PersonalSpacePolicy.newSpace(
        V289SpaceType.SKIN_CARE,
        System.currentTimeMillis(),
        UUID.randomUUID().toString(),
    )

    @Test fun interruptedPrivateJpegCanBeRecoveredOnlyByExplicitOwnerAction() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val space = newSpace()
        val prepared = syntheticPhoto()
        val local = V290PrivatePhotoFiles.storePrepared(app, space.id, prepared)
        assertTrue(local.setLastModified(System.currentTimeMillis() - 120_000L))
        try {
            runBlocking {
                V289SpacesRepository(app, db.spaces()).createSpace(space)
                val recovery = V290InterruptedPhotoRecovery(app, db.spaces())
                val detected = recovery.find(space.id).single()
                assertEquals(prepared.id, detected.photoId)
                assertTrue(local.isFile)
                assertTrue(db.spaces().photosForSpace(space.id).isEmpty())
                // Finding an interrupted save has no side effects.
                val restored = recovery.recover(detected)
                assertEquals("Recovered from interrupted photo save", restored.note)
                assertTrue(V290PrivatePhotoFiles.verifyPrivateBytes(app, restored))
                assertEquals(1, db.spaces().photosForSpace(space.id).size)
                assertTrue(recovery.find(space.id).isEmpty())
                assertTrue(runCatching { recovery.discard(detected) }.isFailure)
            }
        } finally {
            local.delete()
            db.close()
        }
    }

    @Test fun scannerNeverTreatsTrackedOrTooRecentFilesAsRecoverable() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val space = newSpace()
        val tracked = syntheticPhoto()
        val fresh = syntheticPhoto()
        val first = V290PrivatePhotoFiles.storePrepared(app, space.id, tracked)
        val second = V290PrivatePhotoFiles.storePrepared(app, space.id, fresh)
        val unknown = File(first.parentFile, "not-our-format.txt").apply {
            writeText("owner-private-unrecognized-file")
        }
        first.setLastModified(System.currentTimeMillis() - 120_000L)
        try {
            runBlocking {
                V289SpacesRepository(app, db.spaces()).createSpace(space)
                db.spaces().insertPhoto(V290SpacePhotoEntity(
                    id = tracked.id, spaceId = space.id, takenAtMillis = 1_792_050_000_000,
                    addedAtMillis = 1_792_050_000_000,
                    fileName = tracked.fileName, sha256Hex = tracked.sha256Hex,
                    widthPx = tracked.widthPx, heightPx = tracked.heightPx, note = "",
                ))
                val recovery = V290InterruptedPhotoRecovery(app, db.spaces())
                assertTrue(recovery.find(space.id).isEmpty())
                assertTrue(first.isFile)
                assertTrue(second.isFile)
                assertTrue(unknown.isFile)
            }
        } finally {
            first.delete()
            second.delete()
            unknown.delete()
            db.close()
        }
    }

    @Test fun deliberateDiscardDeletesOnlyUntrackedPrivateCopy() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val space = newSpace()
        val prepared = syntheticPhoto()
        val privateFile = V290PrivatePhotoFiles.storePrepared(app, space.id, prepared)
        privateFile.setLastModified(System.currentTimeMillis() - 180_000)
        try {
            runBlocking {
                V289SpacesRepository(app, db.spaces()).createSpace(space)
                val recovery = V290InterruptedPhotoRecovery(app, db.spaces())
                val item = recovery.find(space.id).single()
                assertTrue(recovery.discard(item))
                assertFalse(privateFile.exists())
                assertTrue(db.spaces().photosForSpace(space.id).isEmpty())
            }
        } finally {
            privateFile.delete()
            db.close()
        }
    }
    @Test fun explicitSpaceDeleteCleansOnlyOurPrivateIncompleteFiles() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val space = newSpace()
        val folder = File(
            File(app.filesDir, "backlot-personal-space-photos"), space.id
        )
        assertTrue(folder.isDirectory || folder.mkdirs())
        val photoId = UUID.randomUUID().toString()
        val incomplete = File(folder, ".$photoId.partial")
        val unrelated = File(folder, ".user-owned-other-file.partial")
        incomplete.writeText("interrupted private photo bytes")
        unrelated.writeText("not a Backlot temporary photo")
        try {
            runBlocking {
                V289SpacesRepository(app, db.spaces()).createSpace(space)
                V290InterruptedPhotoRecovery(app, db.spaces())
                    .removeTemporaryCopiesOnOwnerSpaceDelete(space.id)
            }
            assertFalse(incomplete.exists())
            assertTrue(unrelated.isFile)
        } finally {
            incomplete.delete()
            unrelated.delete()
            db.close()
        }
    }

}
