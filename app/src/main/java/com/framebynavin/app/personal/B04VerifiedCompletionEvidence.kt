package com.framebynavin.app.personal

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/**
 * Durable evidence for a fully written target after a cold restart.
 *
 * This does NOT complete the restore, remove the active journal, rearm any
 * alarms, delete an escrow or permit any user-facing action. The future B4
 * completion coordinator must independently reauthenticate the escrow and
 * live photo bytes under an exclusive gate before invoking this isolated
 * file-only primitive. Unverified/partial snapshots cannot be recorded.
 */
internal data class B04VerifiedCompletionEvidence(
    val transactionId: String,
    val escrowId: String,
    val originalDigest: String,
    val targetDigest: String,
    val journalUpdatedAtMillis: Long,
    val confirmedAtMillis: Long,
) {
    val permitsJournalRemoval: Boolean get() = false
    val authorizesLiveMutation: Boolean get() = false
    val signifiesCompletedRestore: Boolean get() = false
}

/** Bound to the exact durable VERIFYING journal, escrow and target snapshot. */
internal object B04VerifiedCompletionEvidencePolicy {
    fun validate(
        journal: StabilizationRestoreJournalRecord,
        escrow: V305EscrowReceipt,
        current: V301LocalRecoverySnapshot,
        ownerConfirmed: Boolean,
        nowMillis: Long,
    ): B04VerifiedCompletionEvidence {
        require(ownerConfirmed) { "Owner must explicitly confirm verified target" }
        require(journal.step == StabilizationRestoreWriteStep.VERIFYING) {
            "Only the final VERIFYING checkpoint permits a target receipt"
        }
        require(current.localIntegrityVerified) {
            "Photo or Personal record integrity could not be confirmed"
        }
        require(escrow.id == journal.escrowId &&
            escrow.baselineDigest == journal.originalDigest &&
            escrow.archiveBytes > 0L &&
            escrow.archiveSha256.matches(Regex("[0-9a-f]{64}"))) {
            "Independent encrypted rollback copy is not bound to this recovery"
        }
        require(nowMillis >= journal.updatedAtMillis && nowMillis > 0L) {
            "Invalid target verification timestamp"
        }
        require(V304RestorePlanPolicy.fingerprint(current.components, current.photos) ==
            journal.targetDigest) {
            "Reopened phone data differs from verified recovery target"
        }
        return B04VerifiedCompletionEvidence(
            transactionId = journal.transactionId,
            escrowId = journal.escrowId,
            originalDigest = journal.originalDigest,
            targetDigest = journal.targetDigest,
            journalUpdatedAtMillis = journal.updatedAtMillis,
            confirmedAtMillis = nowMillis,
        )
    }
}

/**
 * Small private, atomic, fsynced evidence ledger. Owner DATA is never touched.
 * The one transaction ID is the only allowed basename. Existing evidence
 * cannot be overwritten, even to update its timestamp. A crash leaves an
 * *.pending file and forces explicit forensic review.
 */
internal class B04VerifiedCompletionEvidenceStore(private val root: File) {
    private val uuid = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val hex = Regex("[0-9a-f]{64}")
    private val header = "backlot.personal.verified-target-evidence.v1"
    private val maxBytes = 1024

    @Synchronized
    fun record(evidence: B04VerifiedCompletionEvidence): B04VerifiedCompletionEvidence {
        validate(evidence)
        require(!Files.isSymbolicLink(root.toPath())) { "Unsafe target-evidence root" }
        check(root.isDirectory || root.mkdirs()) { "Cannot reserve target evidence directory" }
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()))
        val file = File(root, "${evidence.transactionId}.receipt")
        val pending = File(root, "${evidence.transactionId}.pending")
        check(!Files.exists(pending.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Interrupted target evidence requires owner review"
        }
        check(!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "This recovery already has immutable completion evidence"
        }
        check(pending.createNewFile()) { "Target-evidence reservation failed" }
        FileOutputStream(pending).use {
            it.write(encode(evidence))
            it.fd.sync()
        }
        // Use a no-overwrite move: an existing receipt must not be replaced.
        try {
            Files.move(pending.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
            FileChannel.open(root.toPath(), StandardOpenOption.READ).use { it.force(true) }
        } catch (error: Throwable) {
            // A torn or unfinished file must remain available for review.
            throw error
        }
        check(inspect(evidence.transactionId) == evidence) {
            "Persisted target evidence differs from expected record"
        }
        return evidence
    }

    @Synchronized
    fun inspect(transactionId: String): B04VerifiedCompletionEvidence? {
        require(uuid.matches(transactionId)) { "Invalid transaction identity" }
        if (!root.exists()) return null
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()))
        val pending = File(root, "$transactionId.pending")
        check(!Files.exists(pending.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Interrupted target evidence needs owner review"
        }
        val file = File(root, "$transactionId.receipt")
        if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return null
        check(Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            file.length() in 1L..maxBytes.toLong()) { "Invalid receipt file" }
        return decode(file.readBytes()).also {
            check(it.transactionId == transactionId) { "Mismatched completion evidence identity" }
        }
    }

    private fun validate(e: B04VerifiedCompletionEvidence) {
        require(uuid.matches(e.transactionId) && uuid.matches(e.escrowId)) {
            "Invalid recovery identity"
        }
        require(hex.matches(e.originalDigest) && hex.matches(e.targetDigest)) {
            "Invalid completion fingerprints"
        }
        require(e.journalUpdatedAtMillis > 0L &&
            e.confirmedAtMillis >= e.journalUpdatedAtMillis) {
            "Invalid completion evidence timestamp"
        }
    }

    private fun encode(e: B04VerifiedCompletionEvidence): ByteArray {
        validate(e)
        val payload = listOf(
            header, e.transactionId, e.escrowId, e.originalDigest, e.targetDigest,
            e.journalUpdatedAtMillis, e.confirmedAtMillis,
        ).joinToString("\n", postfix = "\n")
        return (payload + sha(payload.toByteArray(Charsets.UTF_8)) + "\n")
            .toByteArray(Charsets.UTF_8).also { require(it.size in 1..maxBytes) }
    }

    private fun decode(data: ByteArray): B04VerifiedCompletionEvidence {
        require(data.size in 1..maxBytes) { "Oversized target evidence" }
        val lines = data.toString(Charsets.UTF_8).split('\n')
        require(lines.size == 9 && lines[0] == header && lines.last().isEmpty()) {
            "Damaged or unsupported verified-target evidence"
        }
        val payload = lines.take(7).joinToString("\n", postfix = "\n")
        require(lines[7] == sha(payload.toByteArray(Charsets.UTF_8))) {
            "Verified-target evidence checksum mismatch"
        }
        return B04VerifiedCompletionEvidence(
            transactionId = lines[1], escrowId = lines[2],
            originalDigest = lines[3], targetDigest = lines[4],
            journalUpdatedAtMillis = lines[5].toLong(),
            confirmedAtMillis = lines[6].toLong(),
        ).also(::validate)
    }

    private fun sha(raw: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(raw)
            .joinToString("") { "%02x".format(it) }
}
