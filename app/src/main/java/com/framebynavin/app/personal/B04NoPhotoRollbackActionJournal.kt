package com.framebynavin.app.personal

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID

/**
 * Dedicated durable journal for owner-approved no-photo B4 ROLLBACK.
 * Never edits the forward journal or any application data.
 *
 * PREPARED -> reverse stores (Spaces, Groups, Challenges, Personal) ->
 * VERIFYING -> VERIFIED. An incomplete journal is never silently discarded,
 * even if all DataStore and Room writes appear to have succeeded.
 */
internal enum class B04RollbackWriteStep {
    PREPARED,
    BEFORE_SPACES, SPACES_REVERTED,
    BEFORE_GROUPS, GROUPS_REVERTED,
    BEFORE_CHALLENGES, CHALLENGES_REVERTED,
    BEFORE_PERSONAL, PERSONAL_REVERTED,
    VERIFYING, VERIFIED,
}

internal data class B04RollbackJournalRecord(
    val transactionId: String,
    val escrowId: String,
    val forwardStepAtApproval: StabilizationRestoreWriteStep,
    val originalDigest: String,
    val targetDigest: String,
    val step: B04RollbackWriteStep,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val completedRollback: Boolean get() = false
    val permitsNormalWrites: Boolean get() = false
    val permitsDiscardingEscrow: Boolean get() = false
}

internal object B04RollbackJournalCodec {
    private const val FORMAT = "backlot.personal.no-photo-rollback.v1"
    private val HEX = Regex("[0-9a-f]{64}")
    private val UUID_REGEX = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    const val MAX_BYTES = 3072

    fun validate(input: B04RollbackJournalRecord): B04RollbackJournalRecord {
        for (value in listOf(input.transactionId, input.escrowId)) {
            require(UUID_REGEX.matches(value) &&
                UUID.fromString(value).toString() == value) {
                "Invalid no-photo rollback identity"
            }
        }
        require(HEX.matches(input.originalDigest) && HEX.matches(input.targetDigest)) {
            "Invalid rollback fingerprints"
        }
        require(input.createdAtMillis > 0L &&
            input.updatedAtMillis >= input.createdAtMillis) {
            "Invalid rollback checkpoint timestamp"
        }
        return input
    }

    fun encode(input: B04RollbackJournalRecord): ByteArray {
        val record = validate(input)
        val content = listOf(
            FORMAT, record.transactionId, record.escrowId,
            record.forwardStepAtApproval.name, record.originalDigest,
            record.targetDigest, record.step.name,
            record.createdAtMillis.toString(), record.updatedAtMillis.toString(),
        ).joinToString("\n", postfix = "\n")
        return (content + sha(content.toByteArray(Charsets.UTF_8)) + "\n")
            .toByteArray(Charsets.UTF_8).also {
                require(it.size in 1..MAX_BYTES)
            }
    }

    fun decode(data: ByteArray): B04RollbackJournalRecord {
        require(data.size in 1..MAX_BYTES) { "Invalid no-photo rollback journal size" }
        val lines = data.toString(Charsets.UTF_8).split('\n')
        require(lines.size == 11 && lines[0] == FORMAT && lines.last().isEmpty()) {
            "Interrupted or unsupported rollback journal"
        }
        val content = lines.take(9).joinToString("\n", postfix = "\n")
        require(lines[9] == sha(content.toByteArray(Charsets.UTF_8))) {
            "Rollback checkpoint checksum mismatch"
        }
        return validate(B04RollbackJournalRecord(
            transactionId = lines[1], escrowId = lines[2],
            forwardStepAtApproval = StabilizationRestoreWriteStep.valueOf(lines[3]),
            originalDigest = lines[4], targetDigest = lines[5],
            step = B04RollbackWriteStep.valueOf(lines[6]),
            createdAtMillis = lines[7].toLong(),
            updatedAtMillis = lines[8].toLong(),
        ))
    }

    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

internal class B04NoPhotoRollbackActionJournal(private val root: File) {
    private val active = File(root, "rollback.journal")
    private val partial = File(root, "rollback.pending")

    @Synchronized
    fun prepare(
        forward: StabilizationRestoreJournalRecord,
        ownerApproved: Boolean,
        nowMillis: Long,
    ): B04RollbackJournalRecord {
        require(ownerApproved && nowMillis > 0L) { "Owner rollback authorization required" }
        check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "An earlier rollback attempt needs review or resumption"
        }
        val record = B04RollbackJournalCodec.validate(B04RollbackJournalRecord(
            transactionId = forward.transactionId, escrowId = forward.escrowId,
            forwardStepAtApproval = forward.step,
            originalDigest = forward.originalDigest,
            targetDigest = forward.targetDigest,
            step = B04RollbackWriteStep.PREPARED,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis,
        ))
        check(root.mkdirs() && root.isDirectory &&
            !Files.isSymbolicLink(root.toPath())) {
            "Cannot reserve private rollback journal"
        }
        save(record)
        return record
    }

    @Synchronized
    fun inspect(): B04RollbackJournalRecord? {
        if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) return null
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath())) {
            "Rollback journal root is unsafe"
        }
        check(!Files.exists(partial.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(active.toPath()) &&
            root.list()?.toSet() == setOf(active.name)) {
            "Rollback checkpoint interrupted or unrecognized; owner review required"
        }
        check(active.isFile && active.length() in 1L..B04RollbackJournalCodec.MAX_BYTES.toLong()) {
            "Rollback checkpoint incomplete"
        }
        return B04RollbackJournalCodec.decode(active.readBytes())
    }

    @Synchronized
    fun checkpoint(
        expected: B04RollbackWriteStep,
        next: B04RollbackWriteStep,
        nowMillis: Long,
    ): B04RollbackJournalRecord {
        val current = checkNotNull(inspect()) { "No prepared rollback checkpoint" }
        require(current.step == expected && next.ordinal == expected.ordinal + 1) {
            "Rollback cannot skip or replay durable checkpoints"
        }
        require(nowMillis >= current.updatedAtMillis)
        return current.copy(step = next, updatedAtMillis = nowMillis).also(::save)
    }

    private fun save(record: B04RollbackJournalRecord) {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()))
        check(!Files.exists(partial.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(active.toPath()))
        check(partial.createNewFile()) { "Rollback pending reservation failed" }
        FileOutputStream(partial).use { file ->
            file.write(B04RollbackJournalCodec.encode(record))
            file.fd.sync()
        }
        Files.move(partial.toPath(), active.toPath(),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        FileChannel.open(root.toPath(), StandardOpenOption.READ).use { it.force(true) }
    }
}
