package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Android private-file preparation, synthetic bytes, zero production imports. */
@RunWith(AndroidJUnit4::class)
class V304RestorePreparationDeviceTest {
    @Test fun photoPreparationIsReversibleWithoutCreatingSpacesDbOrChangingReminders() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val reminderStore = PersonalReminderStore(app)
        val beforeReminders = runBlocking { reminderStore.exportPersonalJson() }
        val spacesDb = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
        val existed = spacesDb.exists()

        val spaceId = "12537c2d-521d-40db-bf25-2d79bf0f1c2a"
        val photoId = "ecb00ac2-134d-4195-b80b-7a41111a3021"
        val bytes = ByteArray(6400) { (it % 223).toByte() }
        val sha = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
        val photo = V290SpacePhotoEntity(
            id = photoId, spaceId = spaceId,
            takenAtMillis = 1100L, addedAtMillis = 1200L,
            fileName = "$photoId.jpg", sha256Hex = sha,
            widthPx = 640, heightPx = 480, note = "",
        )
        val empty = V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(emptyList(), emptyList()),
            challenges = V297ChallengeCodec.encode(emptyList()),
            groups = V298ReminderGroupCodec.encode(emptyList()),
            spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        )
        val proposed = empty.copy(
            spaces = V289SpacesMetadataCodec.encode(V289SpaceState(
                spaces = listOf(V289PersonalSpace(
                    id = spaceId, title = "Synthetic Room Space",
                    type = V289SpaceType.CUSTOM,
                    createdAtMillis = 1000L,
                )),
            )),
        )
        val plan = V304RestorePlanPolicy.propose(
            V301LocalRecoverySnapshot(empty, emptyList(), true),
            proposed, listOf(photo),
        )
        val token = UUID.randomUUID().toString()
        val root = File(app.cacheDir, "v304-instrumented-$token")
        val stage = File(app.cacheDir, "v304-synthetic-photo-$token.jpg")
        stage.writeBytes(bytes)
        try {
            val store = V304RestorePreparationStore(root)
            val receipt = store.prepare(
                plan, listOf(V300VerifiedPhoto(photo, bytes.size.toLong(), stage)),
                requestedByOwner = true,
            )
            val copy = File(root, "${receipt.transactionId}/part-00000.jpg")
            assertTrue(copy.isFile)
            assertArrayEquals(bytes, copy.readBytes())
            assertEquals(receipt, store.inspect(receipt.transactionId))
            assertFalse(receipt.canCommitToLiveStorage)
            store.discard(receipt.transactionId)
            assertFalse(copy.exists())
            assertTrue(stage.isFile)
            assertEquals(beforeReminders, runBlocking { reminderStore.exportPersonalJson() })
            assertEquals(existed, spacesDb.exists())
        } finally {
            stage.delete()
            root.deleteRecursively()
        }
    }
}
