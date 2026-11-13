package com.framebynavin.app.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/**
 * H3 Task 2 - ORIGINAL Voice Idea reference integrity before a native Creator
 * rollback. CreatorBackupManager's native rollback contains references to
 * existing app-private audio paths, but does NOT contain their audio bytes.
 *
 * V366 authenticates the separate encrypted ORIGINAL archive audio bytes;
 * this inspector proves the native original JSON refers to EXACTLY those
 * IDs and that the ORIGINAL local audio still matches their bytes. Otherwise
 * a "successful" native Creator JSON rollback could leave broken/changed
 * Voice Idea references. Such a rollback must be blocked.
 *
 * This is READ ONLY: never create, repair or remove a Voice Idea recording.
 */
internal data class V367OriginalVoiceInventory(
    val ideaCount: Int,
    val verifiedOriginalVoiceCount: Int,
    val noMutationAuthority: Boolean = true,
) {
    init {
        require(ideaCount >= 0 && verifiedOriginalVoiceCount in 0..ideaCount &&
            noMutationAuthority)
    }
    val mayRollbackCreator: Boolean get() = false
    val mayDeleteMedia: Boolean get() = false
}

internal object V367OriginalVoiceIntegrityPolicy {
    fun exactlyMatchesOriginalArchive(
        originalArchive: Map<String, V366VoiceBytes>,
        originalLocalFiles: Map<String, V366VoiceBytes>,
    ): Boolean {
        if (originalArchive.keys != originalLocalFiles.keys) return false
        return originalArchive.all { (id, value) ->
            id.isNotBlank() && value.sizeBytes in 1L..(512L * 1024 * 1024) &&
                value.sha256.matches(Regex("[a-f0-9]{64}")) &&
                value == originalLocalFiles[id]
        }
    }
}

internal class V367UnifiedCreatorOriginalVoiceSafety(context: Context) {
    private val app = context.applicationContext

    /**
     * Call only after a fresh, bound V364 native checkpoint and V366 encrypted
     * media proof; a Creator writer MUST recheck this under both domain locks.
     * Does not use AtomicFile.openRead() on a torn journal.
     */
    fun inspectNativeOriginalReadOnly(
        native: V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal,
        authenticated: V366CreatorVoiceRecoveryProof,
    ): V367OriginalVoiceInventory {
        check(native.combinedTransactionId == authenticated.transactionId &&
            native.voice != null &&
            native.voice == authenticated.installedCopies &&
            authenticated.originalMediaAuthenticated &&
            authenticated.targetAllocationAuthenticated &&
            authenticated.noMutationAuthority) {
            "Creator native original and authenticated Voice Idea evidence disagree"
        }
        val recoveryDir = File(app.filesDir, "creator_restore_recovery_v181")
        check(recoveryDir.isDirectory &&
            !Files.isSymbolicLink(recoveryDir.toPath()) &&
            recoveryDir.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Native Creator recovery directory is not trusted"
        }
        val file = File(recoveryDir, native.nativeOriginalFileName)
        check(native.nativeOriginalFileName.matches(
            Regex("pre-restore-[a-zA-Z0-9_-]+\\.fbnbackup")
        ) &&
            file.isFile &&
            !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == recoveryDir.canonicalFile &&
            file.length() in 1L..(128L * 1024L * 1024L) &&
            sha256(file) == native.nativeOriginalSha256) {
            "Native original Creator snapshot is missing or changed"
        }
        val raw = file.readText(Charsets.UTF_8)
        CreatorBackupManager(app).validate(raw)
        val ideas = JSONObject(raw).getString("ideas")
        val result = inspectOriginalIdeaReferencesReadOnly(
            ideasJson = ideas,
            expectedAudio = authenticated.originalMediaHashes,
            voiceDir = File(app.filesDir, "voice_ideas"),
        )
        check(result.verifiedOriginalVoiceCount == authenticated.originalMediaCount) {
            "Native Creator Voice Idea references differ from the original archive"
        }
        return result
    }

    /**
     * Strict local path/byte preflight, also directly testable using isolated
     * synthetic files. A symlink, missing original recording, additional saved
     * audio reference, modified bytes or swapped Idea ID fails closed.
     */
    internal fun inspectOriginalIdeaReferencesReadOnly(
        ideasJson: String,
        expectedAudio: Map<String, V366VoiceBytes>,
        voiceDir: File,
    ): V367OriginalVoiceInventory {
        val ideas = JSONArray(ideasJson)
        require(ideas.length() <= 50_000) { "Unbounded native Creator idea inventory" }
        check(!Files.isSymbolicLink(voiceDir.toPath()) &&
            voiceDir.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            (!Files.exists(voiceDir.toPath(), LinkOption.NOFOLLOW_LINKS) ||
                voiceDir.isDirectory)) {
            "Untrusted original Voice Idea folder"
        }
        val observed = linkedMapOf<String, V366VoiceBytes>()
        val allIds = mutableSetOf<String>()
        for (index in 0 until ideas.length()) {
            val idea = ideas.getJSONObject(index)
            val id = idea.optString("id", "").trim()
            check(id.isNotBlank() && allIds.add(id)) {
                "Missing or duplicated Creator Idea ID in original recovery"
            }
            val path = idea.optString("audioLocalPath", "").trim()
            if (path.isEmpty()) continue
            // Verify the UNRESOLVED filename before canonicalization: a
            // symlink inside voice_ideas could otherwise resolve to another
            // apparently safe owned file and conceal a substituted reference.
            val rawFile = File(path)
            check(rawFile.isAbsolute &&
                Files.isRegularFile(rawFile.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(rawFile.toPath())) {
                "Original Voice Idea reference is a symlink or nonregular file"
            }
            val file = PortableVoiceExportPolicy.requireReadableOwnedRecording(
                voiceDir, path,
            )
            check(!Files.isSymbolicLink(file.toPath()) &&
                file.length() in 1L..(512L * 1024L * 1024L)) {
                "Original Voice Idea file is unsafe or oversized"
            }
            observed[id] = V366VoiceBytes(file.length(), sha256(file))
        }
        check(V367OriginalVoiceIntegrityPolicy.exactlyMatchesOriginalArchive(
            expectedAudio, observed,
        )) {
            "Original recording bytes no longer match the owner's encrypted escrow"
        }
        return V367OriginalVoiceInventory(
            ideaCount = ideas.length(),
            verifiedOriginalVoiceCount = observed.size,
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val bytes = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(bytes)
                if (count < 0) break
                if (count > 0) digest.update(bytes, 0, count)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
}
