package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * H3 TASK 2: durable, read-only-inspectable evidence of the exact Voice Idea
 * destinations selected BEFORE portable Creator restore copies the first byte.
 *
 * PortableCreatorBackupManager installs app-owned Voice Idea files BEFORE its
 * native CreatorBackupManager pending.json exists. A process death in that
 * window must NOT make an orphan recording invisible to combined recovery.
 *
 * One immutable SHA-bound allocation manifest per H4 transaction. It is NEVER
 * automatically removed, replayed or treated as license to delete a recording.
 * New files are named in app-private voice_ideas only. No existing file may be
 * overwritten. Caller MUST hold the narrow CreatorDataGate owner-stage write
 * scope before reserving the manifest. Each staged input is SHA-256 streamed.
 *
 * This journal contains no audio bytes and cannot prove full Creator rollback.
 */
internal data class V365VoiceCopyPlanEntry(
    val ideaId: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
)

internal data class V365VoiceCopyPlan(
    val transactionId: String,
    val sourceSha256: String,
    val escrowSha256: String,
    val entries: List<V365VoiceCopyPlanEntry>,
) {
    val mayDeleteAnyMedia: Boolean get() = false
    val mayReplayCreator: Boolean get() = false
    val mayReleaseEscrow: Boolean get() = false
}

internal data class V365VoiceCopyAudit(
    val transactionId: String,
    val planned: Int,
    val matchingCopies: Int,
    val missingCopies: Int,
    val alteredOrPartialCopies: Int,
    val unsafeCopies: Int,
    val noWriteAuthority: Boolean = true,
) {
    init {
        require(planned >= 0 && matchingCopies >= 0 && missingCopies >= 0 &&
            alteredOrPartialCopies >= 0 && unsafeCopies >= 0 &&
            matchingCopies + missingCopies + alteredOrPartialCopies + unsafeCopies ==
                planned && noWriteAuthority) { "Invalid non-mutating Voice Idea crash inventory" }
    }
    val mayCleanOrReplay: Boolean get() = false
}

internal class V365UnifiedCreatorVoiceInstallJournal(
    context: Context,
    private val root: File = File(
        context.applicationContext.filesDir,
        "backlot-unified-creator-voice-install-v1",
    ),
) {
    private val app = context.applicationContext
    private val combined = V344UnifiedRestoreJournal(app)
    private val file get() = File(root, "allocation.json")
    private val shaPattern = Regex("[0-9a-f]{64}")
    private val namePattern = Regex("voice_idea_[0-9a-f-]{36}\\.m4a")

    private fun verifyPlan(plan: V365VoiceCopyPlan) {
        require(UUID.fromString(plan.transactionId).toString() == plan.transactionId &&
            shaPattern.matches(plan.sourceSha256) &&
            shaPattern.matches(plan.escrowSha256) &&
            plan.entries.size <= 500) { "Invalid combined Creator voice allocation identity" }
        val ids = mutableSetOf<String>()
        val names = mutableSetOf<String>()
        var total = 0L
        plan.entries.forEach {
            require(it.ideaId.isNotBlank() && it.ideaId.length <= 256 &&
                ids.add(it.ideaId) && names.add(it.fileName) &&
                namePattern.matches(it.fileName) &&
                shaPattern.matches(it.sha256) &&
                it.sizeBytes in 1L..(512L * 1024L * 1024L) &&
                total <= (1024L * 1024L * 1024L) - it.sizeBytes) {
                "Invalid Creator Voice Idea file allocation"
            }
            total += it.sizeBytes
        }
    }

    /** Persist the entire allocation, including planned destinations, before copying audio. */
    suspend fun reserveForCurrentCreatorWriter(
        staged: Map<String, File>,
    ): V365VoiceCopyPlan {
        check(CreatorDataGate.isJournalBoundCreatorWrite(app)) {
            "Combined Voice Idea allocations require the exact Creator recovery writer"
        }
        val cp = combined.requireCheckpointReadOnly()
        check(cp.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Voice files may only be reserved at BEFORE_CREATOR"
        }
        check(staged.size <= 500) { "Too many Voice Idea files" }
        val entries = staged.toSortedMap().map { (id, source) ->
            check(source.isFile && !Files.isSymbolicLink(source.toPath())) {
                "Source Voice Idea file was replaced or is unsafe"
            }
            V365VoiceCopyPlanEntry(
                ideaId = id,
                fileName = "voice_idea_${UUID.randomUUID()}.m4a",
                sizeBytes = source.length(),
                sha256 = digest(source),
            )
        }
        val plan = V365VoiceCopyPlan(
            transactionId = cp.transactionId,
            sourceSha256 = cp.sourceSha256,
            escrowSha256 = cp.escrowSha256,
            entries = entries,
        )
        verifyPlan(plan)
        check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "An earlier Voice Idea install journal must be reviewed before any copy"
        }
        check(root.mkdir() && !Files.isSymbolicLink(root.toPath())) {
            "Could not reserve app-private Creator Voice Idea journal"
        }
        val raw = encode(plan)
        val atomic = AtomicFile(file)
        var out: FileOutputStream? = null
        try {
            out = atomic.startWrite()
            out.write(raw)
            out.fd.sync()
            atomic.finishWrite(out)
            out = null
        } catch (failure: Throwable) {
            out?.let(atomic::failWrite)
            throw failure
        }
        check(readStrict() == plan && combined.requireCheckpointReadOnly() == cp) {
            "Voice Idea journal was not durably bound to the current Creator checkpoint"
        }
        return plan
    }

    fun inspectReadOnly(): V365VoiceCopyPlan? {
        if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) return null
        return readStrict()
    }

    fun hasBlockingEvidence(): Boolean =
        Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)

    /** Read-only file inventory. Checksum mismatch NEVER deletes the audio. */
    fun auditReadOnly(plan: V365VoiceCopyPlan): V365VoiceCopyAudit {
        verifyPlan(plan)
        check(readStrict() == plan) { "Voice allocation journal changed" }
        val voiceDir = File(app.filesDir, "voice_ideas")
        check(voiceDir.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            (!Files.exists(voiceDir.toPath(), LinkOption.NOFOLLOW_LINKS) ||
                (voiceDir.isDirectory && !Files.isSymbolicLink(voiceDir.toPath())))) {
            "Unsafe Voice Idea directory"
        }
        var matched = 0
        var missing = 0
        var damaged = 0
        var unsafe = 0
        plan.entries.forEach { entry ->
            val copy = File(voiceDir, entry.fileName)
            when {
                !Files.exists(copy.toPath(), LinkOption.NOFOLLOW_LINKS) -> missing++
                Files.isSymbolicLink(copy.toPath()) ||
                    !copy.isFile || copy.canonicalFile.parentFile != voiceDir.canonicalFile ->
                    unsafe++
                copy.length() != entry.sizeBytes ||
                    digest(copy) != entry.sha256 -> damaged++
                else -> matched++
            }
        }
        return V365VoiceCopyAudit(
            transactionId = plan.transactionId,
            planned = plan.entries.size,
            matchingCopies = matched,
            missingCopies = missing,
            alteredOrPartialCopies = damaged,
            unsafeCopies = unsafe,
        )
    }

    private fun encode(plan: V365VoiceCopyPlan): ByteArray {
        verifyPlan(plan)
        val entries = JSONArray()
        plan.entries.forEach {
            entries.put(JSONObject()
                .put("ideaId", it.ideaId)
                .put("fileName", it.fileName)
                .put("sizeBytes", it.sizeBytes)
                .put("sha256", it.sha256))
        }
        val body = JSONObject()
            .put("format", "backlot.unified.creator.voice-allocation.v1")
            .put("transactionId", plan.transactionId)
            .put("sourceSha256", plan.sourceSha256)
            .put("escrowSha256", plan.escrowSha256)
            .put("entries", entries)
        val canonical = listOf(
            plan.transactionId, plan.sourceSha256, plan.escrowSha256,
        ).plus(plan.entries.flatMap {
            listOf(it.ideaId, it.fileName, it.sizeBytes.toString(), it.sha256)
        }).joinToString("\n", postfix = "\n")
        body.put("metadataSha256", digest(canonical.toByteArray(Charsets.UTF_8)))
        return body.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size in 1..150_000) { "Voice allocation journal exceeds bounded size" }
        }
    }

    private fun readStrict(): V365VoiceCopyPlan {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.list()?.toSet() == setOf("allocation.json") &&
            file.isFile && !Files.isSymbolicLink(file.toPath()) &&
            file.length() in 1L..150_000L) {
            "Missing, torn or modified Voice Idea allocation journal"
        }
        val bytes = file.readBytes()
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        check(json.keys().asSequence().toSet() == setOf(
            "format", "transactionId", "sourceSha256", "escrowSha256",
            "entries", "metadataSha256",
        ) && json.getString("format") == "backlot.unified.creator.voice-allocation.v1") {
            "Unknown Voice allocation format or extra fields"
        }
        val arr = json.getJSONArray("entries")
        check(arr.length() <= 500) { "Unbounded Voice Idea journal" }
        val entries = (0 until arr.length()).map { index ->
            val row = arr.getJSONObject(index)
            check(row.keys().asSequence().toSet() == setOf(
                "ideaId", "fileName", "sizeBytes", "sha256",
            )) { "Unknown Voice Idea manifest fields" }
            V365VoiceCopyPlanEntry(
                ideaId = row.getString("ideaId"),
                fileName = row.getString("fileName"),
                sizeBytes = row.getLong("sizeBytes"),
                sha256 = row.getString("sha256"),
            )
        }
        val plan = V365VoiceCopyPlan(
            json.getString("transactionId"),
            json.getString("sourceSha256"),
            json.getString("escrowSha256"),
            entries,
        )
        verifyPlan(plan)
        check(encode(plan).contentEquals(bytes)) {
            "Voice allocation manifest changed or is non-canonical"
        }
        return plan
    }

    private fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    private fun digest(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
