package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic file bytes in a UNIQUE private test folder, NEVER live Space
 * photos. No Room, content resolver, restore or owner media writes.
 */
@RunWith(AndroidJUnit4::class)
class V354UnifiedLiveSpacePhotoAuditAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun root() = File(app.filesDir, "v354-test-" + UUID.randomUUID())
    private val photoBytes = "synthetic-jpeg-for-digest-only".toByteArray()
    private fun sha(raw: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(raw).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun photo(spaceId: String, photoId: String) = V290SpacePhotoEntity(
        id = photoId,
        spaceId = spaceId,
        takenAtMillis = 1_900_000_000_000L,
        addedAtMillis = 1_900_000_000_000L,
        fileName = "$photoId.jpg",
        sha256Hex = sha(photoBytes),
        widthPx = 240,
        heightPx = 320,
        note = "synthetic",
    )

    @Test fun matchingLivePrivatePhotoIsEvidenceButNeverRollbackAuthority() {
        val root = root()
        try {
            val space = UUID.randomUUID().toString()
            val id = UUID.randomUUID().toString()
            val folder = File(root, space)
            assertTrue(folder.mkdirs())
            val image = File(folder, "$id.jpg")
            image.writeBytes(photoBytes)
            val audit = V354UnifiedLiveSpacePhotoAudit(app, root)
                .inspect(listOf(photo(space, id)))
            assertEquals(1, audit.matchedBytes)
            assertTrue(audit.currentPhotoFilesMatchTheirLiveRows)
            assertFalse(audit.authorizesCombinedMediaRollback)
            assertTrue(image.readBytes().contentEquals(photoBytes))
        } finally { root.deleteRecursively() }
    }

    @Test fun missingOrChangedPhotoFilesAreNeverMistakenForOriginalMedia() {
        val root = root()
        try {
            val space = UUID.randomUUID().toString()
            val id = UUID.randomUUID().toString()
            val folder = File(root, space)
            assertTrue(folder.mkdirs())
            val photo = photo(space, id)
            val audit = V354UnifiedLiveSpacePhotoAudit(app, root)
            val missing = audit.inspect(listOf(photo))
            assertEquals(1, missing.missing)
            assertFalse(missing.currentPhotoFilesMatchTheirLiveRows)

            val image = File(folder, "$id.jpg")
            image.writeText("changed-after-process-death")
            val modified = audit.inspect(listOf(photo))
            assertEquals(1, modified.modified)
            assertFalse(modified.currentPhotoFilesMatchTheirLiveRows)
            assertEquals("changed-after-process-death", image.readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun orphanAndInterruptedPendingPhotoIsReportedWithoutBeingDeleted() {
        val root = root()
        try {
            val space = UUID.randomUUID().toString()
            val id = UUID.randomUUID().toString()
            val folder = File(root, space)
            assertTrue(folder.mkdirs())
            File(folder, "$id.jpg").writeBytes(photoBytes)
            val unfinished = File(folder, ".other.restore.pending")
            unfinished.writeText("original-uncommitted-evidence")

            val audit = V354UnifiedLiveSpacePhotoAudit(app, root)
                .inspect(listOf(photo(space, id)))
            assertEquals(1, audit.matchedBytes)
            assertEquals(1, audit.unreferenced)
            assertFalse(audit.currentPhotoFilesMatchTheirLiveRows)
            assertTrue(unfinished.isFile)
            assertEquals("original-uncommitted-evidence", unfinished.readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun emptyFolderDoesNotInventPhotoRowsOrRollbackProof() {
        val root = root()
        try {
            val result = V354UnifiedLiveSpacePhotoAudit(app, root).inspect(emptyList())
            assertEquals(0, result.rows)
            assertEquals(0, result.matchedBytes)
            assertFalse(result.authorizesCombinedMediaRollback)
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }
}
