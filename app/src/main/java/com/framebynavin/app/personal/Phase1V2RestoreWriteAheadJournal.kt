package com.framebynavin.app.personal

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/** New checkpoint chronology must never change persisted v1 journals. */
internal object Phase1V2RecoveryRoots {
    const val ACTIVE_NAME = "backlot-personal-active-restore-transaction-v2"
    fun alongsideLegacy(legacyJournalRoot: File): File =
        File(requireNotNull(legacyJournalRoot.parentFile), ACTIVE_NAME)
}

/**
 * Isolated durable v2 write-ahead journal, NOT a live restore coordinator.
 *
 * Future caller MUST hold the global owner lock and authenticate original
 * escrow/target before preparing this marker. This class never mutates owner
 * data, grants a recovery write permit, finalizes or deletes a checkpoint.
 * Any torn/unknown file remains as evidence and freezes normal owner writes.
 */
internal class Phase1V2RestoreWriteAheadJournal(private val root: File) {
    private val active = File(root, "active.journal")
    private val pending = File(root, "active.pending")

    @Synchronized
    fun prepare(
        initial: Phase1V2RestoreCheckpoint,
        ownerRequested: Boolean,
    ): Phase1V2RestoreCheckpoint {
        require(ownerRequested) { "Explicit owner v2 recovery request required" }
        require(root.name == Phase1V2RecoveryRoots.ACTIVE_NAME) {
            "V2 journal cannot reuse v1 recovery directory"
        }
        val checked = Phase1V2RestoreJournalCodec.validate(initial)
        require(checked.step == Phase1V2RestoreStep.PREPARED) {
            "V2 recovery must start at PREPARED"
        }
        check(root.parentFile?.isDirectory == true) { "Missing private v2 journal parent" }
        // The legacy writer cannot safely coexist with a v2 owner transaction.
        // The v1 journal format and directory stay unchanged.
        val oldJournal = File(root.parentFile, "backlot-personal-active-restore-transaction")
        check(!Files.exists(oldJournal.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Unfinished legacy Personal recovery must be resolved first"
        }
        check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Existing v2 recovery evidence requires owner review"
        }
        check(root.mkdir() && !Files.isSymbolicLink(root.toPath())) {
            "Unable to reserve private v2 recovery journal"
        }
        // Crash after mkdir leaves a blocking marker, not a fabricated empty state.
        save(checked)
        return checked
    }

    @Synchronized
    fun inspectAfterRestart(): Phase1V2RestoreCheckpoint? {
        if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) return null
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath())) {
            "Unsafe v2 recovery journal directory"
        }
        check(!Files.exists(pending.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Incomplete v2 checkpoint requires owner recovery"
        }
        check(active.isFile && !Files.isSymbolicLink(active.toPath()) &&
            active.length() in 1L..4096L) {
            "Missing or corrupt v2 recovery checkpoint"
        }
        check(root.list()?.toSet() == setOf(active.name)) {
            "Unexpected files in v2 recovery journal"
        }
        return Phase1V2RestoreJournalCodec.decode(active.readBytes())
    }

    /** Forensic-only view of an interrupted fsync/rename, never auto-replay. */
    @Synchronized
    fun inspectTornCheckpointReadOnly(): Phase1V2TornCheckpointEvidence {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.list()?.toSet() == setOf(active.name, pending.name)) {
            "Unsafe v2 torn-checkpoint file inventory"
        }
        fun strict(file: File): Phase1V2RestoreCheckpoint {
            check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.length() in 1L..4096L) {
                "Incomplete v2 checkpoint evidence"
            }
            return Phase1V2RestoreJournalCodec.decode(file.readBytes())
        }
        val last = strict(active)
        val staged = strict(pending)
        check(staged.transactionId == last.transactionId &&
            staged.preparedId == last.preparedId &&
            staged.escrowId == last.escrowId &&
            staged.originalDigest == last.originalDigest &&
            staged.targetDigest == last.targetDigest &&
            staged.createdAtMillis == last.createdAtMillis &&
            staged.updatedAtMillis >= last.updatedAtMillis &&
            staged.step.ordinal == last.step.ordinal + 1) {
            "V2 torn checkpoint conflicts with durable recovery evidence"
        }
        return Phase1V2TornCheckpointEvidence(last, staged)
    }

    @Synchronized
    fun checkpoint(
        expected: Phase1V2RestoreStep,
        next: Phase1V2RestoreStep,
        atMillis: Long,
    ): Phase1V2RestoreCheckpoint {
        val current = checkNotNull(inspectAfterRestart()) { "No v2 recovery journal" }
        require(current.step == expected) { "Stale v2 recovery checkpoint" }
        val advanced = Phase1V2RestoreJournalCodec.advance(current, next, atMillis)
        save(advanced)
        return advanced
    }

    private fun save(record: Phase1V2RestoreCheckpoint) {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            !Files.exists(pending.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(active.toPath())) {
            "Unsafe v2 journal write state"
        }
        check(pending.createNewFile()) { "Could not reserve v2 checkpoint" }
        FileOutputStream(pending).use { output ->
            output.write(Phase1V2RestoreJournalCodec.encode(record))
            output.fd.sync()
        }
        Files.move(pending.toPath(), active.toPath(),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        FileChannel.open(root.toPath(), StandardOpenOption.READ).use { it.force(true) }
    }
}

/** Both copies are preserved; neither is authorization to recover or delete. */
internal data class Phase1V2TornCheckpointEvidence(
    val durable: Phase1V2RestoreCheckpoint,
    val pending: Phase1V2RestoreCheckpoint,
) {
    val mayRecoverAutomatically: Boolean get() = false
    val mayRemoveEitherFile: Boolean get() = false
}


/**
 * Separate write-ahead chronology for the REVERSE direction. Never rewinds
 * the existing native v2 forward checkpoint or H4; both remain durable.
 * The photo phase removes only target-added Room photo rows, never JPEG files
 * (which remain authentic owner/media evidence for final reconciliation).
 */
internal enum class Phase1V2ReverseStep {
    PREPARED,
    BEFORE_PHOTO_ROWS, PHOTO_ROWS_REVERTED,
    BEFORE_SPACES, SPACES_REVERTED,
    BEFORE_GROUPS, GROUPS_REVERTED,
    BEFORE_SINGLE_ACTIVITY, SINGLE_ACTIVITY_REVERTED,
    BEFORE_LEGACY_CHALLENGES, LEGACY_CHALLENGES_REVERTED,
    BEFORE_PERSONAL, PERSONAL_REVERTED,
    ORIGINAL_RECORDS_VERIFIED,
}

internal data class Phase1V2ReverseRecord(
    val combinedId: String,
    val combinedDigest: String,
    val ownerDecisionDigest: String,
    val nativeV2Id: String,
    val nativeStep: Phase1V2RestoreStep,
    val originalDigest: String,
    val targetDigest: String,
    val step: Phase1V2ReverseStep,
) {
    val mayDeleteOriginalMedia: Boolean get() = false
    val mayReleaseEscrow: Boolean get() = false
    val mayRearmAlarms: Boolean get() = false
}

internal class Phase1V2OriginalReverseJournal(context: android.content.Context) {
    private val app = context.applicationContext
    private val root = File(app.filesDir, "backlot-unified-personal-v2-reverse-actions-v1")
    private val active get() = File(root, "reverse.json")

    fun hasEvidence(): Boolean =
        Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)

    @Synchronized fun inspect(): Phase1V2ReverseRecord? {
        if (!hasEvidence()) return null
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.list()?.toSet() == setOf("reverse.json") &&
            Files.isRegularFile(active.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            active.length() in 1L..4096L) {
            "Torn or unknown original Personal reverse journal; preserve evidence"
        }
        val bytes = active.readBytes()
        val data = org.json.JSONObject(String(bytes, Charsets.UTF_8))
        check(data.keys().asSequence().toSet() == setOf(
            "format", "combinedId", "combinedDigest", "ownerDecisionDigest",
            "nativeV2Id", "nativeStep", "originalDigest", "targetDigest",
            "step", "checksum",
        ) && data.getString("format") ==
            "backlot.unified.personal-v2-original-reverse.v1") {
            "Unknown original Personal inverse journal"
        }
        val record = Phase1V2ReverseRecord(
            combinedId = data.getString("combinedId"),
            combinedDigest = data.getString("combinedDigest"),
            ownerDecisionDigest = data.getString("ownerDecisionDigest"),
            nativeV2Id = data.getString("nativeV2Id"),
            nativeStep = Phase1V2RestoreStep.valueOf(data.getString("nativeStep")),
            originalDigest = data.getString("originalDigest"),
            targetDigest = data.getString("targetDigest"),
            step = Phase1V2ReverseStep.valueOf(data.getString("step")),
        )
        validate(record)
        check(encode(record).contentEquals(bytes)) {
            "Personal inverse checkpoint checksum or formatting has changed"
        }
        return record
    }

    @Synchronized fun prepare(
        ownerApproved: Boolean,
        record: Phase1V2ReverseRecord,
    ): Phase1V2ReverseRecord {
        require(ownerApproved && record.step == Phase1V2ReverseStep.PREPARED) {
            "Original Personal inverse requires fresh owner consent"
        }
        validate(record)
        check(!hasEvidence() &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.mkdir() && !Files.isSymbolicLink(root.toPath())) {
            "Existing inverse journal is blocking evidence, never overwritten"
        }
        durableWrite(record)
        check(inspect() == record) { "Personal reverse journal was not durable" }
        return record
    }

    @Synchronized fun advance(
        old: Phase1V2ReverseRecord,
        next: Phase1V2ReverseStep,
    ): Phase1V2ReverseRecord {
        check(inspect() == old && next.ordinal == old.step.ordinal + 1) {
            "Original rollback cannot skip or replay a persisted reverse checkpoint"
        }
        val new = old.copy(step = next)
        durableWrite(new)
        check(inspect() == new) {
            "Inverse Personal stage could not be durably confirmed"
        }
        return new
    }

    private fun durableWrite(record: Phase1V2ReverseRecord) {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            !Files.isSymbolicLink(active.toPath()) &&
            root.list()?.all { it == active.name } == true) {
            "Cannot write over a torn Personal reverse action journal"
        }
        val file = android.util.AtomicFile(active)
        var out: FileOutputStream? = null
        try {
            out = file.startWrite()
            out.write(encode(record))
            out.fd.sync()
            file.finishWrite(out)
            out = null
        } catch (e: Throwable) {
            out?.let(file::failWrite)
            throw e
        }
    }

    private fun validate(r: Phase1V2ReverseRecord) {
        fun id(s: String) = runCatching {
            java.util.UUID.fromString(s).toString() == s
        }.getOrDefault(false)
        val hex = Regex("[a-f0-9]{64}")
        require(id(r.combinedId) && id(r.nativeV2Id) &&
            listOf(r.combinedDigest, r.ownerDecisionDigest,
                r.originalDigest, r.targetDigest).all(hex::matches)) {
            "Personal rollback action record has invalid owner identity/digests"
        }
    }

    private fun encode(r: Phase1V2ReverseRecord): ByteArray {
        validate(r)
        val canonical = listOf(
            "backlot.unified.personal-v2-original-reverse.v1", r.combinedId,
            r.combinedDigest, r.ownerDecisionDigest, r.nativeV2Id,
            r.nativeStep.name, r.originalDigest, r.targetDigest, r.step.name,
        ).joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        val sha = java.security.MessageDigest.getInstance("SHA-256")
            .digest(canonical).joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
        val data = org.json.JSONObject()
            .put("format", "backlot.unified.personal-v2-original-reverse.v1")
            .put("combinedId", r.combinedId)
            .put("combinedDigest", r.combinedDigest)
            .put("ownerDecisionDigest", r.ownerDecisionDigest)
            .put("nativeV2Id", r.nativeV2Id)
            .put("nativeStep", r.nativeStep.name)
            .put("originalDigest", r.originalDigest)
            .put("targetDigest", r.targetDigest)
            .put("step", r.step.name)
            .put("checksum", sha)
        return data.toString().toByteArray(Charsets.UTF_8).also {
            require(it.size in 1..4096) { "Personal reverse journal exceeds size bound" }
        }
    }
}
