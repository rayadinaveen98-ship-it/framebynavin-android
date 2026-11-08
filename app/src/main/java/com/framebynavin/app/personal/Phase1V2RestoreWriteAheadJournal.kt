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
