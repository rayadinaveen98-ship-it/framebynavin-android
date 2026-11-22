package com.framebynavin.app.reminders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** RX02: app-private gallery media, never synced or used as reminder receipts. */
internal enum class RX02MediaKind { IMAGE, ANIMATED, VIDEO }
internal data class RX02Media(val kind: RX02MediaKind, val file: File, val poster: File?)

internal object RX02EntranceMediaStore {
    const val MAX_VIDEO_MILLIS = 15_000L
    const val MAX_BYTES = 40L * 1024 * 1024
    private const val PREFS = "backlot_rx02_entrance_media_v1"
    private val types = mapOf(
        "image/jpeg" to ("jpg" to RX02MediaKind.IMAGE),
        "image/png" to ("png" to RX02MediaKind.IMAGE),
        "image/webp" to ("webp" to RX02MediaKind.ANIMATED),
        "image/gif" to ("gif" to RX02MediaKind.ANIMATED),
        "image/heic" to ("heic" to RX02MediaKind.IMAGE),
        "image/heif" to ("heif" to RX02MediaKind.IMAGE),
        "video/mp4" to ("mp4" to RX02MediaKind.VIDEO),
        "video/webm" to ("webm" to RX02MediaKind.VIDEO),
        "video/3gpp" to ("3gp" to RX02MediaKind.VIDEO),
    )
    private fun folder(context: Context): File =
        File(context.applicationContext.filesDir, "rx02_entrance_media").apply { mkdirs() }
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun key(domain: RX01ReminderDomain) = domain.name + ".media"
    private fun validated(context: Context, domain: RX01ReminderDomain, name: String?): File? {
        if (name.isNullOrEmpty() ||
            !Regex("^" + domain.name + "_[a-f0-9-]{36}\\.[a-z0-9]{3,5}$").matches(name)) return null
        return File(folder(context), name).takeIf { it.isFile && it.length() in 1..MAX_BYTES }
    }
    private fun poster(file: File) = File(file.parentFile, file.nameWithoutExtension + "_poster.jpg")

    @Synchronized
    fun selected(context: Context, domain: RX01ReminderDomain): RX02Media? {
        // A wrong-type/corrupt stored preference must NEVER crash a live
        // reminder's double-tap screen. Fall back to stock RX01 artwork.
        val savedName = runCatching {
            prefs(context).getString(key(domain), null)
        }.getOrNull()
        val file = validated(context, domain, savedName) ?: return null
        val kind = when (file.extension.lowercase()) {
            "jpg", "png", "heic", "heif" -> RX02MediaKind.IMAGE
            "gif", "webp" -> RX02MediaKind.ANIMATED
            "mp4", "webm", "3gp" -> RX02MediaKind.VIDEO
            else -> return null
        }
        return RX02Media(kind, file, poster(file).takeIf { kind == RX02MediaKind.VIDEO && it.isFile })
    }

    /** Call only on Dispatchers.IO. Failed imports leave the previous choice untouched. */
    @Synchronized
    fun import(context: Context, domain: RX01ReminderDomain, uri: Uri): Result<RX02Media> =
        runCatching {
            val app = context.applicationContext
            val mime = app.contentResolver.getType(uri)?.lowercase().orEmpty()
            val (extension, kind) = types[mime]
                ?: throw IllegalArgumentException("Select a JPG, PNG, GIF, WebP, HEIF, or compatible video.")
            if ((extension == "heic" || extension == "heif") && Build.VERSION.SDK_INT < 28)
                throw IllegalArgumentException("HEIF needs Android 9 or newer.")
            val file = File(folder(app), domain.name + "_" + UUID.randomUUID() + "." + extension)
            val poster = poster(file)
            val old = selected(app, domain)
            try {
                app.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var total = 0L
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > MAX_BYTES)
                                throw IllegalArgumentException("Maximum file size is 40 MB.")
                            output.write(buffer, 0, n)
                        }
                        if (total == 0L) throw IllegalArgumentException("The file is empty.")
                        output.fd.sync()
                    }
                } ?: throw IllegalArgumentException("This gallery file is no longer available.")
                if (kind == RX02MediaKind.VIDEO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val duration = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull() ?: 0L
                        if (duration !in 1..MAX_VIDEO_MILLIS)
                            throw IllegalArgumentException(
                                "Choose a video up to 15 seconds. Trim longer clips in Gallery first."
                            )
                        val frame = retriever.getFrameAtTime(0)
                        if (frame != null) {
                            val factor = minOf(1f, 720f / maxOf(frame.width, frame.height))
                            val thumb = if (factor < 1f) Bitmap.createScaledBitmap(
                                frame, (frame.width * factor).toInt().coerceAtLeast(1),
                                (frame.height * factor).toInt().coerceAtLeast(1), true
                            ) else frame
                            FileOutputStream(poster).use {
                                thumb.compress(Bitmap.CompressFormat.JPEG, 78, it)
                            }
                            if (thumb !== frame) thumb.recycle()
                            frame.recycle()
                        }
                    } finally { retriever.release() }
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                        bounds.outWidth > 12_000 || bounds.outHeight > 12_000)
                        throw IllegalArgumentException("This image cannot be safely displayed.")
                }
                check(prefs(app).edit().putString(key(domain), file.name).commit()) {
                    "Could not save this entrance background."
                }
                old?.file?.takeIf { it != file }?.delete()
                old?.poster?.takeIf { it != poster }?.delete()
                RX02Media(kind, file, poster.takeIf { it.isFile })
            } catch (failure: Exception) {
                file.delete()
                poster.delete()
                throw failure
            }
        }

    @Synchronized
    fun clear(context: Context, domain: RX01ReminderDomain): Boolean {
        val old = selected(context, domain)
        val saved = prefs(context).edit().remove(key(domain)).commit()
        if (saved) { old?.file?.delete(); old?.poster?.delete() }
        return saved
    }
}
