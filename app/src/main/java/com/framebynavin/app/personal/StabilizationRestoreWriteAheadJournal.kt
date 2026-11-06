package com.framebynavin.app.personal

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID

/** An isolated, durable journal. NO live restore, Room, alarm, cloud or UI writes. */
internal enum class StabilizationRestoreWriteStep {
    PREPARED,
    BEFORE_PERSONAL, PERSONAL_WRITTEN,
    BEFORE_CHALLENGES, CHALLENGES_WRITTEN,
    BEFORE_GROUPS, GROUPS_WRITTEN,
    BEFORE_SPACES, SPACES_WRITTEN,
    BEFORE_PHOTOS, PHOTOS_WRITTEN,
    VERIFYING,
}

internal data class StabilizationRestoreJournalRecord(
    val transactionId: String,
    val preparedId: String,
    val escrowId: String,
    val originalDigest: String,
    val targetDigest: String,
    val step: StabilizationRestoreWriteStep,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val recoveryRequiresOwnerReview: Boolean
        get() = step != StabilizationRestoreWriteStep.PREPARED
    // No final commit is expressible without a future independently verified writer.
    val authorizedToWriteLive: Boolean get() = false
    val completedRestore: Boolean get() = false
}

/** Fixed small schema: only UUIDs, SHA-256, phase and time, never private content. */
internal object StabilizationRestoreJournalCodec {
    private const val FORMAT = "backlot.personal.live-restore-journal.v1"
    private val shaPattern = Regex("[0-9a-f]{64}")
    private val uuidPattern = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    const val MAX_FILE_BYTES = 4096

    fun validate(record: StabilizationRestoreJournalRecord): StabilizationRestoreJournalRecord {
        for (v in listOf(record.transactionId, record.preparedId, record.escrowId)) {
            require(uuidPattern.matches(v) && UUID.fromString(v).toString() == v) {
                "Invalid recovery identity"
            }
        }
        require(shaPattern.matches(record.originalDigest) &&
            shaPattern.matches(record.targetDigest)) { "Invalid recovery fingerprint" }
        require(record.createdAtMillis > 0L && record.updatedAtMillis >= record.createdAtMillis)
        return record
    }

    fun encode(record: StabilizationRestoreJournalRecord): ByteArray {
        val r = validate(record)
        val payload = listOf(FORMAT, r.transactionId, r.preparedId, r.escrowId,
            r.originalDigest, r.targetDigest, r.step.name,
            r.createdAtMillis.toString(), r.updatedAtMillis.toString())
            .joinToString("\n", postfix = "\n")
        val bytes = (payload + sha256(payload.toByteArray(Charsets.UTF_8)) + "\n")
            .toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..MAX_FILE_BYTES)
        return bytes
    }

    fun decode(bytes: ByteArray): StabilizationRestoreJournalRecord {
        require(bytes.size in 1..MAX_FILE_BYTES) { "Invalid recovery journal size" }
        val lines = bytes.toString(Charsets.UTF_8).split('\n')
        require(lines.size == 11 && lines.last().isEmpty() && lines[0] == FORMAT) {
            "Interrupted or unsupported recovery journal"
        }
        val original = lines.take(9).joinToString("\n", postfix = "\n")
        require(lines[9] == sha256(original.toByteArray(Charsets.UTF_8))) {
            "Recovery journal checksum mismatch"
        }
        return validate(StabilizationRestoreJournalRecord(
            transactionId = lines[1], preparedId = lines[2], escrowId = lines[3],
            originalDigest = lines[4], targetDigest = lines[5],
            step = StabilizationRestoreWriteStep.valueOf(lines[6]),
            createdAtMillis = lines[7].toLong(),
            updatedAtMillis = lines[8].toLong(),
        ))
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}

/**
 * Fsync the before/after phase before touching each independent store.
 * No method here writes app data, auto-recovers, marks success or deletes
 * a prior incomplete transaction.
 */
internal class StabilizationRestoreWriteAheadJournal(private val root: File) {
    private val active = File(root, "active.journal")
    private val pending = File(root, "active.pending")

    @Synchronized
    fun prepare(
        barrier: V305CommitBarrierReceipt,
        targetDigest: String,
        ownerApproved: Boolean,
        nowMillis: Long,
    ): StabilizationRestoreJournalRecord {
        require(ownerApproved && nowMillis > 0L) { "Owner approval required" }
        check(!root.exists()) { "An earlier recovery needs review" }
        val record = StabilizationRestoreJournalCodec.validate(
            StabilizationRestoreJournalRecord(
                transactionId = barrier.preparedId, preparedId = barrier.preparedId,
                escrowId = barrier.rollbackEscrowId,
                originalDigest = barrier.localBaselineSha256,
                targetDigest = targetDigest, step = StabilizationRestoreWriteStep.PREPARED,
                createdAtMillis = nowMillis, updatedAtMillis = nowMillis,
            )
        )
        check(root.mkdirs() && !Files.isSymbolicLink(root.toPath())) {
            "Unable to create private recovery journal"
        }
        save(record)
        return record
    }

    /** Cold launch inspects but never silently resumes or discards partial work. */
    @Synchronized
    fun inspectAfterRestart(): StabilizationRestoreJournalRecord? {
        if (!root.exists()) return null
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()))
        check(!pending.exists() && !Files.isSymbolicLink(active.toPath())) {
            "Interrupted checkpoint, owner recovery required"
        }
        check(active.isFile && active.length() in 1..StabilizationRestoreJournalCodec.MAX_FILE_BYTES.toLong()) {
            "Restore journal incomplete, owner recovery required"
        }
        check(root.list()?.toSet() == setOf(active.name)) {
            "Unexpected recovery files, owner review required"
        }
        return StabilizationRestoreJournalCodec.decode(active.readBytes())
    }

    @Synchronized
    fun checkpoint(
        expected: StabilizationRestoreWriteStep,
        next: StabilizationRestoreWriteStep,
        nowMillis: Long,
    ): StabilizationRestoreJournalRecord {
        val current = checkNotNull(inspectAfterRestart()) { "No recovery journal" }
        require(current.step == expected && next.ordinal == expected.ordinal + 1) {
            "Restore cannot skip or repeat mutation checkpoints"
        }
        require(nowMillis >= current.updatedAtMillis)
        return current.copy(step = next, updatedAtMillis = nowMillis).also(::save)
    }

    private fun save(record: StabilizationRestoreJournalRecord) {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()))
        check(!pending.exists() && !Files.isSymbolicLink(active.toPath()))
        check(pending.createNewFile()) { "Could not reserve recovery checkpoint" }
        FileOutputStream(pending).use {
            it.write(StabilizationRestoreJournalCodec.encode(record))
            it.fd.sync()
        }
        Files.move(pending.toPath(), active.toPath(),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        FileChannel.open(root.toPath(), StandardOpenOption.READ).use { it.force(true) }
    }
}
