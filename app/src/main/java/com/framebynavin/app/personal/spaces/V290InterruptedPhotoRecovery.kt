package com.framebynavin.app.personal.spaces

import android.content.Context
import android.graphics.BitmapFactory
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recovery of photos saved to app-private disk just before a process crash,
 * before a matching Room photo row could be committed.
 *
 * SAFETY: Discovery never deletes anything. Only UUID-named JPEGs in the
 * selected Space's own private folder can appear. The owner must separately
 * confirm either recovery or discard. Unknown files are left untouched.
 */
internal data class V290InterruptedPhoto(
    val spaceId: String,
    val photoId: String,
    val fileName: String,
    val lastModifiedMillis: Long,
)

internal class V290InterruptedPhotoRecovery(
    private val context: Context,
    private val dao: V289SpacesDao,
) {
    private val photoName = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg"
    )
    private val maxBytes = 12L * 1024L * 1024L
    private val minAgeMillis = 60_000L
    private val root = File(context.filesDir, "backlot-personal-space-photos")

    private fun checkedFolder(spaceId: String): File {
        require(runCatching { UUID.fromString(spaceId).toString() == spaceId }
            .getOrDefault(false)) { "Invalid Space identifier" }
        val folder = File(root, spaceId)
        require(folder.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator)) {
            "Unsafe private photo folder"
        }
        return folder
    }

    private fun checkedFile(spaceId: String, photoId: String): File {
        require(runCatching { UUID.fromString(photoId).toString() == photoId }
            .getOrDefault(false)) { "Invalid photo identifier" }
        val name = "$photoId.jpg"
        require(photoName.matches(name)) { "Invalid private photo name" }
        val folder = checkedFolder(spaceId)
        return File(folder, name).also {
            require(it.canonicalPath == File(folder.canonicalPath, name).path) {
                "Unsafe private photo path"
            }
        }
    }

    suspend fun find(
        spaceId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<V290InterruptedPhoto> = withContext(Dispatchers.IO) {
        require(nowMillis > 0L)
        checkNotNull(dao.getSpace(spaceId)) { "Space does not exist" }
        val usedIds = dao.photosForSpace(spaceId).mapTo(HashSet()) { it.id }
        val folder = checkedFolder(spaceId)
        if (!folder.isDirectory) return@withContext emptyList()
        folder.listFiles().orEmpty().mapNotNull { candidate ->
            val name = candidate.name
            if (!candidate.isFile || !photoName.matches(name)) return@mapNotNull null
            val id = name.removeSuffix(".jpg")
            if (id in usedIds ||
                candidate.lastModified() <= 0L ||
                candidate.lastModified() > nowMillis - minAgeMillis ||
                candidate.length() !in 1..maxBytes
            ) return@mapNotNull null
            val verified = runCatching { checkedFile(spaceId, id) }.getOrNull()
            if (verified == null || verified.canonicalPath != candidate.canonicalPath) {
                return@mapNotNull null
            }
            V290InterruptedPhoto(spaceId, id, name, candidate.lastModified())
        }.sortedWith(compareBy({ it.lastModifiedMillis }, { it.photoId }))
    }

    /** Only called after the owner confirms recovery in the UI. */
    suspend fun recover(item: V290InterruptedPhoto): V290SpacePhotoEntity =
        StabilizationPersonalMutationGate.withOwnerMutation(context) {
        withContext(Dispatchers.IO) {
            checkNotNull(dao.getSpace(item.spaceId)) { "Space no longer exists" }
            require(item.fileName == "${item.photoId}.jpg") { "Unsafe recovery name" }
            require(dao.getPhoto(item.photoId) == null) {
                "Photo was already recovered"
            }
            val file = checkedFile(item.spaceId, item.photoId)
            require(file.isFile && file.length() in 1..maxBytes) {
                "Private photo is missing or too large"
            }
            val bytes = file.readBytes()
            require(bytes.size in 2..maxBytes.toInt() &&
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
                "Interrupted save is not a valid JPEG"
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048) {
                "Interrupted private JPEG dimensions are invalid"
            }
            val occurredAt = file.lastModified()
            require(occurredAt > 0L && occurredAt <= System.currentTimeMillis()) {
                "Invalid interrupted photo timestamp"
            }
            val checksum = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            val photo = V290PhotoTimelinePolicy.validate(
                V290SpacePhotoEntity(
                    id = item.photoId, spaceId = item.spaceId,
                    takenAtMillis = occurredAt,
                    addedAtMillis = System.currentTimeMillis(),
                    fileName = item.fileName, sha256Hex = checksum,
                    widthPx = bounds.outWidth, heightPx = bounds.outHeight,
                    note = "Recovered from interrupted photo save",
                )
            )
            dao.insertPhoto(photo)
            photo
        }
    }

    /**
     * The owner confirmed DELETE SPACE. Remove only the exact private temporary
     * .UUID.partial files that our own atomic JPEG writer can produce. These
     * incomplete bytes are never recoverable as journal entries, and unknown
     * files (including non-matching names) are left untouched.
     */
    suspend fun removeTemporaryCopiesOnOwnerSpaceDelete(spaceId: String) =
        StabilizationPersonalMutationGate.withOwnerMutation(context) {
        withContext(Dispatchers.IO) {
            val folder = checkedFolder(spaceId)
            if (!folder.isDirectory) return@withContext
            val pendingName = Regex(
                "\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.partial"
            )
            folder.listFiles().orEmpty().forEach { file ->
                if (file.isFile && pendingName.matches(file.name) &&
                    file.canonicalPath == File(folder.canonicalPath, file.name).path
                ) {
                    check(file.delete()) {
                        "Unable to clean an incomplete private photo copy"
                    }
                }
            }
        }
    }

    /** Only called after the owner separately confirms permanent discard. */
    suspend fun discard(item: V290InterruptedPhoto): Boolean =
        StabilizationPersonalMutationGate.withOwnerMutation(context) {
        withContext(Dispatchers.IO) {
            require(item.fileName == "${item.photoId}.jpg")
            require(dao.getPhoto(item.photoId) == null) {
                "Tracked photos cannot be discarded as interrupted saves"
            }
            val file = checkedFile(item.spaceId, item.photoId)
            !file.exists() || file.delete()
        }
    }
}
