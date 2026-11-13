package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
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
                    shadowFile = reserveUnusedShadowName(directory),
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

    /**
     * Reserve a pathname that is provably absent, including symlinks and
     * dangling symlinks. The later CREATE_NEW write is still mandatory:
     * an absence check alone is never an overwrite-safety guarantee.
     */
    private fun reserveUnusedShadowName(folder: File): String {
        repeat(40) {
            val candidate = "voice_idea_" + UUID.randomUUID().toString() + ".m4a"
            if (!Files.exists(
                    File(folder, candidate).toPath(), LinkOption.NOFOLLOW_LINKS,
                )) return candidate
        }
        error("Unable to reserve a fresh Voice Idea shadow destination")
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
        // An intact replacement is NOT a trustworthy recovery source if
        // the original damaged bytes were modified after reservation.
        plan.entries.forEach { entry ->
            val damaged = File(directory, entry.damagedFile)
            check(Files.isRegularFile(damaged.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(damaged.toPath()) &&
                damaged.canonicalFile.parentFile == directory.canonicalFile &&
                damaged.length() == entry.damagedBytes &&
                sha256(damaged) == entry.damagedSha256) {
                "Original damaged recording changed; shadow recovery needs owner review"
            }
        }
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

    /**
     * H3 Task 2: install only MISSING immutable shadow copies. This method
     * MUST run inside the owner's Personal missing-Voice fence AND the exact
     * H4 Creator write permit. It cannot create a recovery journal, choose a
     * recovery direction, replace original/partial bytes, or touch Creator
     * records/Best Frames/alarms. Caller authenticates BOTH sealed archives,
     * verifies live ORIGINAL Creator+Personal data and stages TARGET files.
     *
     * A crash mid-copy leaves a partial shadow as blocking evidence; never
     * unlink/retry the same path automatically. Existing MATCHING shadows
     * are verified but not rewritten (idempotent completed retry).
     */
    internal suspend fun installMissingShadowsForCurrentOwnerWriter(
        plan: V371VoiceShadowPlan,
        selectedTargetMedia: Map<String, File>,
    ): V371VoiceShadowAudit {
        check(CreatorDataGate.isJournalBoundCreatorWrite(app)) {
            "Shadow Voice installation requires H4 Creator owner write authority"
        }
        val checkpoint = h4.requireCheckpointReadOnly()
        check(checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR &&
            checkpoint.transactionId == plan.transactionId &&
            checkpoint.sourceSha256 == plan.sourceSha256 &&
            checkpoint.escrowSha256 == plan.escrowSha256 &&
            com.framebynavin.app.personal.StabilizationPersonalMutationGate
                .isWithinCombinedCreatorMissingVoiceFence(app, checkpoint)) {
            "Shadow Voice installation requires BOTH exclusive owner recovery fences"
        }
        val choice = (V347OwnerRecoveryDecisionJournal(app).readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("Owner recovery direction is absent or torn")
        check(choice.choice == V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(choice, checkpoint)) {
            "Shadow Voice installation needs the exact durable FORWARD_TARGET choice"
        }
        val original = allocations.inspectReadOnly()
            ?: error("Immutable original Voice Idea allocation is missing")
        V371VoiceShadowPolicy.validate(plan, original)
        check(readStrict(original) == plan &&
            selectedTargetMedia.keys == original.entries.map { it.ideaId }.toSet()) {
            "Authenticated target audio or shadow journal is stale"
        }
        val folder = voiceRoot
        check(folder.isDirectory &&
            !Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Unsafe Voice Idea output folder"
        }
        // Verify ALL sources and ALL original damaged bytes BEFORE the first
        // CREATE_NEW. Never produce a partial success for a known-bad source.
        plan.entries.forEach { entry ->
            val source = requireNotNull(selectedTargetMedia[entry.ideaId])
            check(Files.isRegularFile(source.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(source.toPath()) &&
                source.length() == entry.expectedBytes &&
                sha256(source) == entry.expectedSha256) {
                "Authenticated target recording changed before shadow copy"
            }
        }
        val before = auditShadowReadOnly(plan)
        check(before.unsafe == 0 && before.alteredOrPartial == 0 &&
            before.matching + before.missing == before.planned) {
            "A partial, altered or unsafe shadow cannot be overwritten or retried"
        }
        plan.entries.forEach { entry ->
            val destination = File(folder, entry.shadowFile)
            val source = requireNotNull(selectedTargetMedia[entry.ideaId])
            // The previous audit established that any existing copy matches;
            // never reopen an existing file in write mode, even after a race.
            if (Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                check(Files.isRegularFile(destination.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                    !Files.isSymbolicLink(destination.toPath()) &&
                    destination.canonicalFile.parentFile == folder.canonicalFile &&
                    destination.length() == entry.expectedBytes &&
                    sha256(destination) == entry.expectedSha256) {
                    "Previously allocated shadow changed: preserve it for owner review"
                }
            } else {
                check(destination.canonicalFile.parentFile == folder.canonicalFile) {
                    "Shadow file would escape app-private media folder"
                }
                // Atomic CREATE_NEW: another owner file is never truncated.
                // fsync precedes exposing any result, checksum verified after.
                FileChannel.open(
                    destination.toPath(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                ).use { channel ->
                    source.inputStream().buffered().use { input ->
                        input.copyTo(Channels.newOutputStream(channel), 64 * 1024)
                    }
                    channel.force(true)
                }
                check(destination.length() == entry.expectedBytes &&
                    sha256(destination) == entry.expectedSha256) {
                    "Incomplete shadow copy preserved; owner recovery blocked"
                }
            }
        }
        val after = auditShadowReadOnly(plan)
        check(after.allVerified &&
            h4.requireCheckpointReadOnly() == checkpoint &&
            readStrict(original) == plan &&
            V347OwnerRecoveryDecisionJournal(app).readOnlyStatus() ==
                V347OwnerRecoveryDecisionJournal.ReadState.Recorded(choice)) {
            "Shadow completion must not modify owner recovery evidence"
        }
        return after
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
