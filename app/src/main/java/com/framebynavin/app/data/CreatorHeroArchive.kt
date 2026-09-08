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
