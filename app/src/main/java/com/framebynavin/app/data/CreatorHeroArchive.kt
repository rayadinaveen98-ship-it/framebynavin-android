package com.framebynavin.app.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Portable, path-safe copies of the user's original personal hero frames. */
object CreatorHeroArchive {
    private const val DIRECTORY = "best_frames_original"
    private const val PREFS = "best_frames_original_prefs"
    private const val ORDER = "frame_order"
    private const val MAX_FILE = 20L * 1024 * 1024
    private const val MAX_TOTAL = 100L * 1024 * 1024
    private val namePattern = Regex("[a-zA-Z0-9._-]{1,128}")

    /**
     * Read only the ordered original-source names for portable ZIP packaging.
     * This intentionally avoids parsing the huge Base64 personalFrames JSON
     * a second time just to discover source files.
     */
    internal fun orderedSourceNames(context: Context): List<String> {
        val names = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ORDER, "").orEmpty().split('\n').filter { it.isNotBlank() }
        require(names.size <= 10 && names.distinct().size == names.size) {
            "Invalid or duplicate original Best Frames"
        }
        require(names.all { namePattern.matches(it) && it != "." && it != ".." }) {
            "Invalid original Best Frames source names"
        }
        return names
    }

    /**
     * Durable rollback pointer to pre-restore originals, not their Base64 bytes.
     * import/importVerifiedFiles never deletes old originals; the original
     * names and file hashes can therefore recover from a process-death restore.
     */
    internal fun captureOriginalOrder(context: Context): String {
        val directory = File(context.filesDir, DIRECTORY).canonicalFile
        val items = JSONArray()
        var total = 0L
        orderedSourceNames(context).forEach { name ->
            val source = File(directory, name).canonicalFile
            require(source.parentFile == directory && source.isFile) {
                "A previous Best Frame is missing; restore cancelled"
            }
            val size = source.length()
            require(size in 1..MAX_FILE && total <= MAX_TOTAL - size) {
                "Previous Best Frames exceed recovery safety limits"
            }
            total += size
            checkImageBounds(source)
            items.put(JSONObject().put("name", name).put("sha256", sha256File(source)))
        }
        return items.toString()
    }

    /** Verify all previously saved originals BEFORE making their order visible. */
    internal fun restoreOriginalOrder(context: Context, checkpoint: String) {
        val items = JSONArray(checkpoint)
        require(items.length() <= 10) { "Too many original Best Frames in recovery" }
        val root = File(context.filesDir, DIRECTORY).canonicalFile
        val names = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        var total = 0L
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            require(item.keys().asSequence().toSet() == setOf("name", "sha256")) {
                "Invalid original Best Frames journal"
            }
            val name = item.getString("name")
            val checksum = item.getString("sha256")
            require(namePattern.matches(name) && name != "." && name != ".." &&
                seen.add(name) && checksum.matches(Regex("[a-f0-9]{64}"))) {
                "Invalid original Best Frames recovery reference"
            }
            val file = File(root, name).canonicalFile
            require(file.parentFile == root && file.isFile) {
                "A previous Best Frame cannot be recovered"
            }
            val size = file.length()
            require(size in 1..MAX_FILE && total <= MAX_TOTAL - size &&
                sha256File(file) == checksum) {
                "Previous Best Frames changed during restore"
            }
            total += size
            checkImageBounds(file)
            names += name
        }
        check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ORDER, names.joinToString("\n"))
            .putBoolean("alpha17_empty_hero_reset_done", true)
            .commit()) { "Could not recover original Best Frames order" }
    }

    /**
     * Install verified ZIP images from disk without allocating Base64 strings.
     * Copy into new app-owned names, verify bytes and dimensions before one
     * atomic SharedPreferences order switch. Existing originals are retained
     * for the durable rollback journal.
     */
    internal fun importVerifiedFiles(context: Context, frames: List<PortableBestFrame>) {
        require(frames.size <= 10) { "Too many restored Best Frames" }
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }.canonicalFile
        val staged = mutableListOf<File>()
        val names = mutableListOf<String>()
        var total = 0L
        try {
            frames.forEach { frame ->
                val source = frame.file
                val size = source.length()
                require(source.isFile && size in 1..MAX_FILE && total <= MAX_TOTAL - size) {
                    "Restored Best Frame exceeds media safety limit"
                }
                total += size
                require(sha256File(source) == frame.sha256) {
                    "Restored Best Frame integrity check failed"
                }
                checkImageBounds(source)
                val name = "restore_${UUID.randomUUID()}.img"
                val destination = File(directory, name)
                staged += destination
                val atomic = AtomicFile(destination)
                var output: java.io.FileOutputStream? = null
                try {
                    val destinationStream = atomic.startWrite()
                    output = destinationStream
                    source.inputStream().buffered().use { input ->
                        input.copyTo(destinationStream, DEFAULT_BUFFER_SIZE)
                    }
                    atomic.finishWrite(destinationStream)
                } catch (error: Throwable) {
                    output?.let(atomic::failWrite)
                    throw error
                }
                require(destination.length() == size && sha256File(destination) == frame.sha256) {
                    "Best Frame copy failed integrity validation"
                }
                names += name
            }
            check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(ORDER, names.joinToString("\n"))
                .putBoolean("alpha17_empty_hero_reset_done", true)
                .commit()) { "Could not save restored Best Frames" }
        } catch (error: Throwable) {
            staged.forEach { runCatching { it.delete() } }
            throw error
        }
    }

    /**
     * Append staged, checksum-verified originals without re-copying the
     * creator's existing library. All writes finish before the single order
     * switch; an error deletes only newly copied files.
     *
     * Caller must hold CreatorDataGate.transaction for the whole operation.
     */
    internal fun appendVerifiedFiles(context: Context, frames: List<PortableBestFrame>) {
        if (frames.isEmpty()) return
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }.canonicalFile
        val previous = orderedSourceNames(context)
        require(previous.size + frames.size <= 10) { "Slideshow is full; choose fewer photos" }
        var total = 0L
        previous.forEach { name ->
            val original = File(directory, name).canonicalFile
            require(original.parentFile == directory && original.isFile) {
                "An existing slideshow photo is missing; selection retained"
            }
            val size = original.length()
            require(size in 1..MAX_FILE && total <= MAX_TOTAL - size) {
                "Existing slideshow exceeds backup media limits"
            }
            total += size
            checkImageBounds(original)
        }

        val newlyCopied = mutableListOf<File>()
        val finalNames = previous.toMutableList()
        try {
            frames.forEach { frame ->
                val source = frame.file
                val bytes = source.length()
                require(source.isFile && bytes in 1..MAX_FILE && total <= MAX_TOTAL - bytes) {
                    "Selected photos exceed backup media limits"
                }
                require(sha256File(source) == frame.sha256) {
                    "Selected photo checksum did not match"
                }
                checkImageBounds(source)
                total += bytes

                val destination = File(directory, "append_${UUID.randomUUID()}.img")
                newlyCopied += destination
                val atomic = AtomicFile(destination)
                var output: java.io.FileOutputStream? = null
                try {
                    val destinationStream = atomic.startWrite()
                    output = destinationStream
                    source.inputStream().buffered().use { input ->
                        input.copyTo(destinationStream, DEFAULT_BUFFER_SIZE)
                    }
                    atomic.finishWrite(destinationStream)
                } catch (failure: Throwable) {
                    output?.let(atomic::failWrite)
                    throw failure
                }
                require(destination.length() == bytes && sha256File(destination) == frame.sha256) {
                    "New slideshow photo failed its copy verification"
                }
                finalNames += destination.name
            }
            check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(ORDER, finalNames.joinToString("\n"))
                .putBoolean("alpha17_empty_hero_reset_done", true)
                .commit()) { "Could not save appended photos" }
        } catch (failure: Throwable) {
            newlyCopied.forEach { runCatching { it.delete() } }
            throw failure
        }
    }

    private fun checkImageBounds(file: File) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        require(options.outWidth in 1..20000 && options.outHeight in 1..20000) {
            "Invalid original Best Frame image"
        }
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun export(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val directory = File(context.filesDir, DIRECTORY)
        val names = prefs.getString(ORDER, "").orEmpty().split('\n').filter { it.isNotBlank() }
        require(names.size <= 10) { "Too many personal frames in backup" }
        val frames = JSONArray()
        var total = 0L
        names.forEach { name ->
            require(namePattern.matches(name) && name != "." && name != "..") { "Invalid personal frame name" }
            val file = File(directory, name)
            require(file.isFile && file.length() in 1..MAX_FILE) { "A personal frame is missing or too large. Review Best Frames before exporting." }
            total += file.length()
            require(total <= MAX_TOTAL) { "Personal frames exceed the 100 MB backup limit" }
            val bytes = file.readBytes()
            validateImage(bytes)
            frames.put(JSONObject().put("name", name).put("sha256", sha256(bytes))
                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        }
        return frames.toString()
    }

    fun validate(raw: String): Int {
        val frames = JSONArray(raw)
        require(frames.length() <= 10) { "Too many personal frames" }
        var total = 0L
        val names = mutableSetOf<String>()
        for (i in 0 until frames.length()) {
            val frame = frames.getJSONObject(i)
            val name = frame.getString("name")
            require(namePattern.matches(name) && name != "." && name != ".." && names.add(name)) {
                "Invalid or duplicate personal frame name"
            }
            val bytes = Base64.decode(frame.getString("data"), Base64.DEFAULT)
            require(bytes.size.toLong() in 1..MAX_FILE) { "Invalid personal frame size" }
            total += bytes.size
            require(total <= MAX_TOTAL) { "Personal frames exceed the backup limit" }
            require(sha256(bytes).equals(frame.getString("sha256"), ignoreCase = true)) { "Personal frame integrity check failed" }
            validateImage(bytes)
        }
        return frames.length()
    }

    /** Prepare all files before switching the visible order. Old files stay for recovery. */
    fun import(context: Context, raw: String) {
        validate(raw)
        val frames = JSONArray(raw)
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val names = mutableListOf<String>()
        val staged = mutableListOf<File>()
        try {
            for (i in 0 until frames.length()) {
                val bytes = Base64.decode(frames.getJSONObject(i).getString("data"), Base64.DEFAULT)
                val name = "restore_${UUID.randomUUID()}.img"
                val target = File(directory, name)
                val atomic = AtomicFile(target)
                var output: java.io.FileOutputStream? = null
                try {
                    output = atomic.startWrite()
                    output.write(bytes)
                    atomic.finishWrite(output)
                } catch (error: Throwable) {
                    output?.let(atomic::failWrite)
                    throw error
                }
                staged += target
                names += name
            }
            check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(ORDER, names.joinToString("\n"))
                .putBoolean("alpha17_empty_hero_reset_done", true).commit()) { "Could not save restored personal frames" }
        } catch (error: Throwable) {
            staged.forEach { it.delete() }
            throw error
        }
    }

    private fun validateImage(bytes: ByteArray) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth in 1..20000 && options.outHeight in 1..20000) { "Invalid personal frame image" }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
