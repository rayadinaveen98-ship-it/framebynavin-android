package com.framebynavin.app.personal.spaces

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Private photos are opt-in only, saved under filesDir, and EXIF/GPS metadata
 * is stripped by decoding and re-encoding as a bounded JPEG. Never save or
 * export the source content URI, and never silently sync these files.
 */
internal object V290PrivatePhotoFiles {
    private const val ROOT = "backlot-personal-space-photos"
    private const val MAX_INPUT_BYTES = 24 * 1024 * 1024
    private const val MAX_OUTPUT_BYTES = 12 * 1024 * 1024
    private const val MAX_SOURCE_EDGE = 40_000
    const val MAX_SAVED_EDGE = 2048

    internal data class PreparedPhoto(
        val id: String,
        val fileName: String,
        val bytes: ByteArray,
        val sha256Hex: String,
        val widthPx: Int,
        val heightPx: Int,
    )

    /**
     * Read at most 24 MB. Does not trust ContentResolver's provider-reported
     * MIME type, file extension, size or path.
     */
    internal fun readBounded(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        while (true) {
            val count = stream.read(chunk)
            if (count == -1) break
            if (count == 0) continue
            require(output.size() + count <= MAX_INPUT_BYTES) {
                "Selected photo is too large"
            }
            output.write(chunk, 0, count)
        }
        require(output.size() > 0) { "Selected photo was empty" }
        return output.toByteArray()
    }

    /** Re-encoding removes location EXIF, original filenames and other metadata. */
    internal fun sanitizeImage(raw: ByteArray, photoId: String): PreparedPhoto {
        require(UUID.fromString(photoId).toString() == photoId) {
            "Photo identifier must be a canonical UUID"
        }
        require(raw.isNotEmpty() && raw.size <= MAX_INPUT_BYTES) {
            "Selected photo is empty or too large"
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        val originalWidth = bounds.outWidth
        val originalHeight = bounds.outHeight
        require(originalWidth in 1..MAX_SOURCE_EDGE &&
            originalHeight in 1..MAX_SOURCE_EDGE) {
            "Selected file is not a supported image"
        }
        var scale = 1
        while (originalWidth / scale > MAX_SAVED_EDGE ||
            originalHeight / scale > MAX_SAVED_EDGE) {
            scale *= 2
        }
        val bitmap = BitmapFactory.decodeByteArray(
            raw, 0, raw.size,
            BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = scale
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: error("Unable to decode selected image")
        val encoded = try {
            val sink = ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, sink)) {
                "Unable to re-encode selected image"
            }
            sink.toByteArray()
        } finally {
            bitmap.recycle()
        }
        require(encoded.isNotEmpty() && encoded.size <= MAX_OUTPUT_BYTES) {
            "Selected image cannot be saved safely"
        }
        val savedBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encoded, 0, encoded.size, savedBounds)
        require(savedBounds.outWidth in 1..MAX_SAVED_EDGE &&
            savedBounds.outHeight in 1..MAX_SAVED_EDGE) {
            "Saved photo dimensions exceed limit"
        }
        return PreparedPhoto(
            id = photoId,
            fileName = "$photoId.jpg",
            bytes = encoded,
            sha256Hex = sha256Hex(encoded),
            widthPx = savedBounds.outWidth,
            heightPx = savedBounds.outHeight,
        )
    }

    private fun photoFolder(context: Context, spaceId: String): File {
        require(runCatching { UUID.fromString(spaceId).toString() == spaceId }
            .getOrDefault(false)) { "Invalid Space id" }
        return File(File(context.filesDir, ROOT), spaceId)
    }

    private fun photoFile(context: Context, item: V290SpacePhotoEntity): File {
        V290PhotoTimelinePolicy.validate(item)
        return File(photoFolder(context, item.spaceId), item.fileName)
    }

    /** Atomic final rename on the same private filesystem; caller must own the record. */
    internal fun storePrepared(
        context: Context, spaceId: String, photo: PreparedPhoto,
    ): File {
        require(photo.fileName == "${photo.id}.jpg" &&
            UUID.fromString(photo.id).toString() == photo.id)
        val dir = photoFolder(context, spaceId)
        require(dir.isDirectory || dir.mkdirs()) { "Photo storage unavailable" }
        val final = File(dir, photo.fileName)
        require(!final.exists()) { "Photo already exists" }
        val pending = File(dir, ".${photo.id}.partial")
        require(pending.createNewFile()) { "Pending photo already exists" }
        try {
            FileOutputStream(pending).use {
                it.write(photo.bytes)
                it.fd.sync()
            }
            check(pending.renameTo(final)) { "Unable to commit private photo" }
        } finally {
            pending.delete()
        }
        return final
    }

    fun verifyPrivateBytes(context: Context, photo: V290SpacePhotoEntity): Boolean {
        val file = photoFile(context, photo)
        if (!file.isFile || file.length() > MAX_OUTPUT_BYTES) return false
        return sha256Hex(file.readBytes()) == photo.sha256Hex
    }

    internal fun deleteFile(context: Context, photo: V290SpacePhotoEntity): Boolean {
        val file = photoFile(context, photo)
        return !file.exists() || file.delete()
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
}

/** Supports only explicit user-picked photos; no camera, background imports or uploads. */
internal class V290PhotoJournal(
    private val context: Context,
    private val dao: V289SpacesDao,
) {
    suspend fun addOwnerPickedPhoto(
        spaceId: String,
        selectedUri: Uri,
        note: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): V290SpacePhotoEntity = withContext(Dispatchers.IO) {
        require(selectedUri.scheme == "content") {
            "Choose a photo using Android's photo picker"
        }
        require(nowMillis > 0L && note.length <= 500) { "Invalid photo details" }
        val space = checkNotNull(dao.getSpace(spaceId)) { "Space not found" }
        require(space.archivedAtMillis == null) { "Archived Spaces cannot add photos" }
        val stream = context.contentResolver.openInputStream(selectedUri)
            ?: error("Selected photo is unavailable")
        val raw = stream.use { V290PrivatePhotoFiles.readBounded(it) }
        val prepared = V290PrivatePhotoFiles.sanitizeImage(
            raw, V289PersonalSpacePolicy.canonicalUuid()
        )
        val row = V290PhotoTimelinePolicy.validate(
            V290SpacePhotoEntity(
                id = prepared.id,
                spaceId = spaceId,
                takenAtMillis = nowMillis,
                addedAtMillis = nowMillis,
                fileName = prepared.fileName,
                sha256Hex = prepared.sha256Hex,
                widthPx = prepared.widthPx,
                heightPx = prepared.heightPx,
                note = note,
            )
        )
        val writtenFile = V290PrivatePhotoFiles.storePrepared(context, spaceId, prepared)
        try {
            dao.insertPhoto(row)
        } catch (failure: Exception) {
            writtenFile.delete()
            throw failure
        }
        row
    }

    suspend fun removePhoto(id: String): Boolean = withContext(Dispatchers.IO) {
        val photo = dao.getPhoto(id) ?: return@withContext false
        // A successful DB removal is never used as a false claim that disk
        // deletion succeeded; a later orphan sweep can clean interrupted work.
        val deleted = dao.deletePhoto(id) == 1
        if (deleted) {
            check(V290PrivatePhotoFiles.deleteFile(context, photo)) {
                "Photo record removed; private file requires cleanup"
            }
        }
        deleted
    }

    suspend fun readVerifiedPhotoBytes(photo: V290SpacePhotoEntity): ByteArray =
        withContext(Dispatchers.IO) {
            check(V290PrivatePhotoFiles.verifyPrivateBytes(context, photo)) {
                "Private photo is missing or altered"
            }
            // Only generated basenames under our app-private directory.
            val location = File(
                File(File(context.filesDir, "backlot-personal-space-photos"), photo.spaceId),
                photo.fileName
            )
            location.readBytes()
        }
}
