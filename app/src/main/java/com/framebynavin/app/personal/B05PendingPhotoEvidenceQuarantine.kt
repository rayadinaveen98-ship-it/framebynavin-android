package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID

/**
 * B5 forensic preservation of an interrupted private JPEG copy. NEVER called
 * from startup, sync or a normal screen. A future owner-approved coordinator
 * MUST authenticate the incoming ZIP and original escrow, inventory the
 * entire photo root and hold an exclusive recovery write permit. This primitive
 * establishes no such authority and must never be exposed as a direct action.
 */
internal data class B05QuarantinedPendingPhoto(
    val transactionId: String,
    val photoId: String,
    val bytes: Long,
    val sha256: String,
    val evidencePath: File,
) {
    val authorizesRestore: Boolean get() = false
    val permitsDeletingEvidence: Boolean get() = false
    val permitsNormalWrites: Boolean get() = false
}

internal class B05PendingPhotoEvidenceQuarantine(
    private val liveRoot: File,
    private val historyRoot: File,
) {
    private val noLinks = LinkOption.NOFOLLOW_LINKS
    private val idPattern = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")

    @Synchronized
    fun preserve(
        journal: StabilizationRestoreJournalRecord,
        imported: V290SpacePhotoEntity,
        ownerConfirmed: Boolean,
        isOriginalOwnerPhoto: Boolean,
    ): B05QuarantinedPendingPhoto {
        require(ownerConfirmed) { "Owner must explicitly approve evidence preservation" }
        require(!isOriginalOwnerPhoto) { "Owner's original photo must never be moved" }
        require(journal.step == StabilizationRestoreWriteStep.BEFORE_PHOTOS) {
            "Only an interrupted photo write can be quarantined"
        }
        StabilizationRestoreJournalCodec.validate(journal)
        V290PhotoTimelinePolicy.validate(imported)
        require(canonical(journal.transactionId) &&
            canonical(imported.id) && canonical(imported.spaceId)) {
            "Noncanonical private photo identity"
        }
        val folder = File(liveRoot, imported.spaceId)
        val pending = File(folder, "." + imported.id + ".restore.pending")
        val evidenceFolder = File(historyRoot, journal.transactionId)
        val retained = File(evidenceFolder, imported.id + ".restore.pending")
        for (directory in listOf(liveRoot, folder)) {
            check(Files.isDirectory(directory.toPath(), noLinks) &&
                !Files.isSymbolicLink(directory.toPath())) {
                "Unsafe source photo directory"
            }
        }
        check(Files.isRegularFile(pending.toPath(), noLinks) &&
            !Files.isSymbolicLink(pending.toPath())) {
            "Missing or unsafe pending file"
        }
        val length = pending.length()
        check(length in 1L..V299PhotoIntegrityPolicy.MAX_BYTES) {
            "Unrecognized pending copy size; requires owner review"
        }
        val checksum = sha256(pending)
        check(!Files.isSymbolicLink(historyRoot.toPath())) {
            "Unsafe evidence root"
        }
        check(historyRoot.isDirectory || historyRoot.mkdirs())
        check(Files.isDirectory(historyRoot.toPath(), noLinks))
        check(!Files.isSymbolicLink(evidenceFolder.toPath()) &&
            (evidenceFolder.isDirectory || evidenceFolder.mkdir())) {
            "Unsafe transaction evidence folder"
        }
        check(Files.isDirectory(evidenceFolder.toPath(), noLinks) &&
            !Files.isSymbolicLink(evidenceFolder.toPath()))
        check(!Files.exists(retained.toPath(), noLinks)) {
            "Existing photo evidence must never be overwritten"
        }

        // No REPLACE_EXISTING and no delete. Both file bytes and the final
        // SHA receipt remain verifiable even after an interrupted fsync.
        Files.move(pending.toPath(), retained.toPath(), StandardCopyOption.ATOMIC_MOVE)
        FileChannel.open(folder.toPath(), StandardOpenOption.READ).use { it.force(true) }
        FileChannel.open(evidenceFolder.toPath(), StandardOpenOption.READ).use { it.force(true) }
        FileChannel.open(historyRoot.toPath(), StandardOpenOption.READ).use { it.force(true) }

        check(!Files.exists(pending.toPath(), noLinks) &&
            Files.isRegularFile(retained.toPath(), noLinks) &&
            !Files.isSymbolicLink(retained.toPath()) &&
            retained.length() == length && sha256(retained) == checksum) {
            "Preserved photo evidence failed independent verification"
        }
        return B05QuarantinedPendingPhoto(
            journal.transactionId, imported.id, length, checksum, retained,
        )
    }

    private fun canonical(value: String): Boolean =
        idPattern.matches(value) &&
            runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
