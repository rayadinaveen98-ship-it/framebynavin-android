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
 * H3 Task 2: write-ahead evidence for copy-on-write recovery of damaged
 * Voice Idea media. Never change an existing V365 reservation or damaged file.
 *
 * Reserves DIFFERENT paths before any future repair writer copies audio. The
 * immutable journal is SHA-bound to H4 and to V365's original allocations.
 * Torn, unexpected, stale, symlinked or untrusted evidence fails CLOSED.
 *
 * This class writes only its private reservation JSON. It does not install
 * media, restore Creator records, modify Best Frames, delete or release any
 * evidence, or schedule reminders. Not connected to UI/startup/workers.
 */
internal data class V371VoiceShadowEntry(
    val ideaId: String,
    val damagedFile: String,
    val damagedBytes: Long,
    val damagedSha256: String,
    val shadowFile: String,
    val expectedBytes: Long,
    val expectedSha256: String,
)

internal data class V371VoiceShadowPlan(
    val transactionId: String,
    val sourceSha256: String,
    val escrowSha256: String,
    val allocationSha256: String,
    val entries: List<V371VoiceShadowEntry>,
) {
    val mayOverwriteDamagedMedia: Boolean get() = false
    val mayImportCreatorRecords: Boolean get() = false
    val mayReleaseEscrow: Boolean get() = false
}

internal object V371VoiceShadowPolicy {
    private val hex = Regex("[a-f0-9]{64}")
    private val file = Regex("voice_idea_[0-9a-f-]{36}\\.m4a")

    fun allocationDigest(original: V365VoiceCopyPlan): String =
        sha256(original.entries.joinToString("\n", postfix = "\n") { e ->
            listOf(e.ideaId, e.fileName, e.sizeBytes, e.sha256).joinToString("\t")
        }.toByteArray(Charsets.UTF_8))

    fun validate(plan: V371VoiceShadowPlan, original: V365VoiceCopyPlan) {
        require(
            runCatching { UUID.fromString(plan.transactionId).toString() ==
                plan.transactionId }.getOrDefault(false) &&
            plan.transactionId == original.transactionId &&
            plan.sourceSha256 == original.sourceSha256 &&
            plan.escrowSha256 == original.escrowSha256 &&
            hex.matches(plan.sourceSha256) && hex.matches(plan.escrowSha256) &&
            plan.allocationSha256 == allocationDigest(original) &&
            plan.entries.isNotEmpty() &&
            plan.entries.size <= original.entries.size &&
            plan.entries.size <= 500
        ) { "Unbound Creator Voice shadow recovery allocation" }
        val originalById = original.entries.associateBy { it.ideaId }
        val ids = mutableSetOf<String>()
        val filenames = original.entries.mapTo(mutableSetOf()) { it.fileName }
        var total = 0L
        for (e in plan.entries) {
            val old = originalById[e.ideaId]
            require(old != null && ids.add(e.ideaId) &&
                e.damagedFile == old.fileName &&
                e.expectedBytes == old.sizeBytes &&
                e.expectedSha256 == old.sha256 &&
                e.damagedBytes in 0L..(512L * 1024L * 1024L) &&
                e.expectedBytes in 1L..(512L * 1024L * 1024L) &&
                total <= 1024L * 1024L * 1024L - e.expectedBytes &&
                hex.matches(e.damagedSha256) &&
                hex.matches(e.expectedSha256) &&
                file.matches(e.damagedFile) &&
                file.matches(e.shadowFile) &&
                filenames.add(e.shadowFile) &&
                (e.damagedBytes != e.expectedBytes ||
                    e.damagedSha256 != e.expectedSha256)
            ) { "Shadow entry is unsafe, duplicate or not actually damaged" }
            total += e.expectedBytes
        }
    }

    private fun sha256(raw: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(raw)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal class V371UnifiedCreatorVoiceShadowJournal(
    context: Context,
    private val root: File = File(
        context.applicationContext.filesDir,
        "backlot-unified-creator-voice-shadow-v1",
    ),
) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val allocations = V365UnifiedCreatorVoiceInstallJournal(app)
    private val active get() = File(root, "shadow.json")
    private val voiceRoot get() = File(app.filesDir, "voice_ideas")

    fun hasBlockingEvidence(): Boolean =
        Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)

    fun inspectReadOnly(): V371VoiceShadowPlan? {
        if (!hasBlockingEvidence()) return null
        val original = allocations.inspectReadOnly()
            ?: error("Shadow evidence has no original Voice Idea journal")
        return readStrict(original)
    }

    /**
     * Caller must have independently authenticated BOTH owner archives,
     * read the no-native Creator crash state, and acquired the outer Personal
     * owner fence. This method also requires the EXACT scoped Creator writer.
     * It creates only immutable path reservations, never audio files.
     */
    suspend fun reserveForCurrentCreatorWriter(
        original: V365VoiceCopyPlan,
        selectedTargetMedia: Map<String, File>,
    ): V371VoiceShadowPlan {
        check(CreatorDataGate.isJournalBoundCreatorWrite(app)) {
            "Shadow reservation requires the scoped combined Creator writer"
        }
        val cp = h4.requireCheckpointReadOnly()
        check(cp.step == V344UnifiedRestoreStep.BEFORE_CREATOR &&
            cp.transactionId == original.transactionId &&
            cp.sourceSha256 == original.sourceSha256 &&
            cp.escrowSha256 == original.escrowSha256 &&
            allocations.inspectReadOnly() == original &&
            !hasBlockingEvidence()) {
            "Pre-existing or stale Creator shadow recovery evidence"
        }
        val choice = (V347OwnerRecoveryDecisionJournal(app).readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("No durable owner recovery direction for Creator shadow media")
        check(choice.choice == V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(choice, cp)) {
            "Creator shadow allocation requires the exact FORWARD_TARGET owner choice"
        }
        check(selectedTargetMedia.keys == original.entries.map { it.ideaId }.toSet()) {
            "Authenticated target audio differs from original allocation"
        }
        val directory = voiceRoot
        check(directory.isDirectory &&
            !Files.isSymbolicLink(directory.toPath()) &&
            directory.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Unsafe app-private Voice Idea directory"
        }
        val entries = original.entries.mapNotNull { e ->
            val target = requireNotNull(selectedTargetMedia[e.ideaId])
            check(target.isFile && !Files.isSymbolicLink(target.toPath()) &&
                target.length() == e.sizeBytes && sha256(target) == e.sha256) {
                "Selected target audio no longer matches the owner archive"
            }
            val damaged = File(directory, e.fileName)
            if (!Files.exists(damaged.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                null // Missing files are covered by V370, not by shadow recovery.
            } else {
                check(Files.isRegularFile(damaged.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                    !Files.isSymbolicLink(damaged.toPath()) &&
                    damaged.canonicalFile.parentFile == directory.canonicalFile) {
                    "Unsafe partial Voice Idea recording cannot be modified"
                }
                val size = damaged.length()
                check(size in 0L..(512L * 1024L * 1024L)) {
                    "Unbounded partial Voice Idea recording"
                }
                val digest = sha256(damaged)
                if (size == e.sizeBytes && digest == e.sha256) null
                else V371VoiceShadowEntry(
                    ideaId = e.ideaId,
                    damagedFile = e.fileName,
                    damagedBytes = size,
                    damagedSha256 = digest,
                    shadowFile = "voice_idea_" + UUID.randomUUID().toString() + ".m4a",
                    expectedBytes = e.sizeBytes,
                    expectedSha256 = e.sha256,
                )
            }
        }
        val plan = V371VoiceShadowPlan(
            cp.transactionId, cp.sourceSha256, cp.escrowSha256,
            V371VoiceShadowPolicy.allocationDigest(original), entries,
        )
        V371VoiceShadowPolicy.validate(plan, original)
        check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.mkdir() && !Files.isSymbolicLink(root.toPath())) {
            "Cannot replace existing shadow recovery journal"
        }
        val atomic = AtomicFile(active)
        var writer: FileOutputStream? = null
        try {
            writer = atomic.startWrite()
            writer.write(encode(plan))
            writer.fd.sync()
            atomic.finishWrite(writer)
            writer = null
        } catch (error: Throwable) {
            writer?.let(atomic::failWrite)
            throw error
        }
        check(readStrict(original) == plan &&
            h4.requireCheckpointReadOnly() == cp) {
            "Shadow reservations changed during durable write"
        }
        return plan
    }

    /** Inspect only SHADOW bytes; original damaged files are never touched. */
    fun auditShadowReadOnly(plan: V371VoiceShadowPlan): V371VoiceShadowAudit {
        val original = allocations.inspectReadOnly()
            ?: error("Original Voice Idea journal disappeared")
        V371VoiceShadowPolicy.validate(plan, original)
        check(readStrict(original) == plan) {
            "Shadow journal is stale or altered"
        }
        val directory = voiceRoot
        check(directory.isDirectory && !Files.isSymbolicLink(directory.toPath()) &&
            directory.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted Voice Idea storage"
        }
        var matching = 0
        var missing = 0
        var damaged = 0
        var unsafe = 0
        plan.entries.forEach { e ->
            val shadow = File(directory, e.shadowFile)
            when {
                !Files.exists(shadow.toPath(), LinkOption.NOFOLLOW_LINKS) -> missing++
                !Files.isRegularFile(shadow.toPath(), LinkOption.NOFOLLOW_LINKS) ||
                    Files.isSymbolicLink(shadow.toPath()) ||
                    shadow.canonicalFile.parentFile != directory.canonicalFile -> unsafe++
                shadow.length() != e.expectedBytes ||
                    sha256(shadow) != e.expectedSha256 -> damaged++
                else -> matching++
            }
        }
        return V371VoiceShadowAudit(
            planned = plan.entries.size, matching = matching, missing = missing,
            alteredOrPartial = damaged, unsafe = unsafe,
        )
    }

    private fun encode(p: V371VoiceShadowPlan): ByteArray {
        val items = JSONArray()
        p.entries.forEach { e ->
            items.put(JSONObject().put("ideaId", e.ideaId)
                .put("damagedFile", e.damagedFile)
                .put("damagedBytes", e.damagedBytes)
                .put("damagedSha256", e.damagedSha256)
                .put("shadowFile", e.shadowFile)
                .put("expectedBytes", e.expectedBytes)
                .put("expectedSha256", e.expectedSha256))
        }
        val obj = JSONObject().put("format", "backlot.unified.creator.voice-shadow.v1")
            .put("transactionId", p.transactionId)
            .put("sourceSha256", p.sourceSha256)
            .put("escrowSha256", p.escrowSha256)
            .put("allocationSha256", p.allocationSha256)
            .put("entries", items)
            .put("sha256", sha256(canonical(p)))
        return obj.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size in 1..160_000) { "Unbounded Voice Idea shadow journal" }
        }
    }

    private fun readStrict(original: V365VoiceCopyPlan): V371VoiceShadowPlan {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.list()?.toSet() == setOf("shadow.json") &&
            active.isFile && !Files.isSymbolicLink(active.toPath()) &&
            active.length() in 1L..160_000L) {
            "Creator Voice shadow journal is absent, torn or modified"
        }
        val bytes = active.readBytes()
        val obj = JSONObject(String(bytes, Charsets.UTF_8))
        check(obj.keys().asSequence().toSet() == setOf(
            "format", "transactionId", "sourceSha256", "escrowSha256",
            "allocationSha256", "entries", "sha256",
        ) && obj.getString("format") ==
            "backlot.unified.creator.voice-shadow.v1") {
            "Unsupported Voice Idea shadow journal version"
        }
        val items = obj.getJSONArray("entries")
        check(items.length() in 1..500) { "Unbounded shadow allocation" }
        val entries = (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            check(item.keys().asSequence().toSet() == setOf(
                "ideaId", "damagedFile", "damagedBytes", "damagedSha256",
                "shadowFile", "expectedBytes", "expectedSha256",
            )) { "Unexpected shadow Voice Idea field" }
            V371VoiceShadowEntry(
                item.getString("ideaId"), item.getString("damagedFile"),
                item.getLong("damagedBytes"), item.getString("damagedSha256"),
                item.getString("shadowFile"), item.getLong("expectedBytes"),
                item.getString("expectedSha256"),
            )
        }
        val plan = V371VoiceShadowPlan(
            obj.getString("transactionId"), obj.getString("sourceSha256"),
            obj.getString("escrowSha256"), obj.getString("allocationSha256"),
            entries,
        )
        V371VoiceShadowPolicy.validate(plan, original)
        check(encode(plan).contentEquals(bytes)) {
            "Tampered or noncanonical Voice Idea shadow journal"
        }
        return plan
    }

    private fun canonical(p: V371VoiceShadowPlan): ByteArray =
        listOf(p.transactionId, p.sourceSha256, p.escrowSha256,
            p.allocationSha256).plus(p.entries.flatMap { e ->
            listOf(e.ideaId, e.damagedFile, e.damagedBytes.toString(),
                e.damagedSha256, e.shadowFile, e.expectedBytes.toString(),
                e.expectedSha256)
        }).joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val bytes = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(bytes)
                if (n < 0) break
                if (n > 0) digest.update(bytes, 0, n)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal data class V371VoiceShadowAudit(
    val planned: Int,
    val matching: Int,
    val missing: Int,
    val alteredOrPartial: Int,
    val unsafe: Int,
) {
    init {
        require(planned >= 0 && matching >= 0 && missing >= 0 &&
            alteredOrPartial >= 0 && unsafe >= 0 &&
            matching + missing + alteredOrPartial + unsafe == planned)
    }
    val allVerified: Boolean get() =
        matching == planned && missing == 0 && alteredOrPartial == 0 && unsafe == 0
    val mayPromoteIntoCreator: Boolean get() = false
}
