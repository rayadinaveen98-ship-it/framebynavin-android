package com.framebynavin.app.cloud

import android.util.AtomicFile
import java.io.File
import java.util.UUID
import org.json.JSONObject

/**
 * Minimal crash breadcrumb; never stores owner ID, passphrase, filenames,
 * plaintext, account tokens or source counts. A corrupt marker requires
 * review, and never authorizes deletion of a verified encrypted copy.
 */
internal object TrustT1C3RecoveryJournal {
    private const val NAME = "incomplete-preview-journal.json"
    private val phases = setOf("CAPTURING", "ENCRYPTING", "VERIFYING", "PROMOTED")

    data class Marker(val attemptId: String, val phase: String)

    fun mark(root: File, attemptId: String, phase: String) {
        require(phase in phases)
        require(UUID.fromString(attemptId).toString() == attemptId) {
            "Invalid internal preview identifier"
        }
        val json = JSONObject().put("schema", 1)
            .put("attemptId", attemptId).put("phase", phase).toString()
        val target = AtomicFile(File(root, NAME))
        var out: java.io.FileOutputStream? = null
        try {
            out = target.startWrite()
            out.write(json.toByteArray(Charsets.UTF_8))
            target.finishWrite(out)
        } catch (error: Throwable) {
            out?.let { runCatching { target.failWrite(it) } }
            throw error
        }
    }

    fun inspect(root: File): Marker? {
        val file = File(root, NAME)
        if (!file.exists() && !File(root, "$NAME.new").exists() &&
            !File(root, "$NAME.bak").exists()) return null
        val input = AtomicFile(file).openRead()
        val bytes = input.use { stream ->
            val data = ByteArray(1025)
            val count = stream.read(data)
            require(count in 1..1024 && stream.read() == -1) {
                "Recovery marker exceeds size limits"
            }
            data.copyOf(count)
        }
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        require(json.keys().asSequence().toSet() ==
            setOf("schema", "attemptId", "phase") &&
            json.getInt("schema") == 1) { "Unsupported recovery marker schema" }
        val id = json.getString("attemptId")
        val phase = json.getString("phase")
        require(UUID.fromString(id).toString() == id && phase in phases) {
            "Invalid owner preview recovery state"
        }
        return Marker(id, phase)
    }

    fun clear(root: File) {
        AtomicFile(File(root, NAME)).delete()
    }
}
