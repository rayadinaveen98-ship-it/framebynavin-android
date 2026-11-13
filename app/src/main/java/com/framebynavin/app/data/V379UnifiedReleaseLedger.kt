package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import org.json.JSONObject

/**
 * H3.4 owner-confirmed release chronology. BEFORE the H4 journal is moved,
 * every other recovery journal and BOTH encrypted original/selected ZIP roots
 * are retained by atomic rename into durable private history. The active H4
 * is ALWAYS the final blocking journal to be released.
 *
 * A crash between an atomic move and checkpoint write is recoverable: either
 * active or retained location is accepted, never both or neither for
 * mandatory source evidence. No owner data/media is deleted.
 */
internal enum class V379ReleaseStep {
    PREPARED,
    JOURNALS_RETAINED,
    ENCRYPTED_ORIGINAL_AND_TARGET_RETAINED,
    READY_TO_ARCHIVE_H4,
    H4_ARCHIVED,
    ALARMS_RECONCILED,
}

internal data class V379UnifiedReleaseRecord(
    val transactionId: String,
    val checkpointSha256: String,
    val verificationSha256: String,
    val outcome: V377VerifiedOwnerOutcome,
    val step: V379ReleaseStep,
) {
    init {
        require(runCatching {
            java.util.UUID.fromString(transactionId).toString() == transactionId
        }.getOrDefault(false) &&
            listOf(checkpointSha256, verificationSha256).all {
                it.matches(Regex("[0-9a-f]{64}"))
            })
    }
    val mayDeleteOriginalArchive: Boolean get() = false
    val mayDeleteOwnerMedia: Boolean get() = false
    val mayUnfreezeBeforeH4Archived: Boolean get() = false
}

internal class V379UnifiedReleaseJournal(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.filesDir, "backlot-unified-final-release-v1")
    private val active get() = File(root, "release.json")
    private val keys = setOf("format", "transactionId", "checkpointSha256",
        "verificationSha256", "outcome", "step", "sha256")

    fun hasEvidence(): Boolean =
        Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)

    @Synchronized fun inspect(): V379UnifiedReleaseRecord? {
        if (!hasEvidence()) return null
        check(root.isDirectory &&
            !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.list()?.toSet() == setOf(active.name) &&
            Files.isRegularFile(active.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(active.toPath()) &&
            active.length() in 1L..2048L) {
            "Final owner release journal is torn or contains unknown files"
        }
        val obj = JSONObject(active.readText())
        check(obj.keys().asSequence().toSet() == keys &&
            obj.getString("format") == FORMAT) {
            "Unknown final release journal format"
        }
        val item = V379UnifiedReleaseRecord(
            transactionId = obj.getString("transactionId"),
            checkpointSha256 = obj.getString("checkpointSha256"),
            verificationSha256 = obj.getString("verificationSha256"),
            outcome = V377VerifiedOwnerOutcome.valueOf(obj.getString("outcome")),
            step = V379ReleaseStep.valueOf(obj.getString("step")),
        )
        check(obj.getString("sha256") == sha256(canonical(item))) {
            "Final release journal checksum mismatch"
        }
        return item
    }

    @Synchronized fun prepare(
        record: V379UnifiedReleaseRecord,
        ownerApproved: Boolean,
    ): V379UnifiedReleaseRecord {
        require(ownerApproved && record.step == V379ReleaseStep.PREPARED) {
            "Fresh separate owner approval is needed for final evidence retention"
        }
        check(!hasEvidence() &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.mkdir() && !Files.isSymbolicLink(root.toPath())) {
            "An existing or torn final release journal must never be overwritten"
        }
        write(record)
        check(inspect() == record) { "Release preparation was not fsynced" }
        return record
    }

    @Synchronized fun advance(
        existing: V379UnifiedReleaseRecord,
        next: V379ReleaseStep,
    ): V379UnifiedReleaseRecord {
        check(inspect() == existing &&
            next.ordinal == existing.step.ordinal + 1) {
            "Final release may not jump, revert or replay a durable checkpoint"
        }
        val changed = existing.copy(step = next)
        write(changed)
        check(inspect() == changed) { "Final release journal stage did not persist" }
        return changed
    }

    private fun write(value: V379UnifiedReleaseRecord) {
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.list()?.all { it == active.name } == true &&
            !Files.isSymbolicLink(active.toPath())) {
            "Cannot write into unknown final recovery evidence"
        }
        val obj = JSONObject()
            .put("format", FORMAT)
            .put("transactionId", value.transactionId)
            .put("checkpointSha256", value.checkpointSha256)
            .put("verificationSha256", value.verificationSha256)
            .put("outcome", value.outcome.name)
            .put("step", value.step.name)
            .put("sha256", sha256(canonical(value)))
        val raw = obj.toString().toByteArray(Charsets.UTF_8)
        require(raw.size in 1..2048)
        val atomic = AtomicFile(active)
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(raw)
            stream.fd.sync()
            atomic.finishWrite(stream)
            stream = null
        } catch (t: Throwable) {
            stream?.let(atomic::failWrite)
            throw t
        }
    }

    companion object {
        private const val FORMAT = "backlot.unified.final-release.v1"
        internal fun proofDigest(proof: V377UnifiedFinalReadOnlyProof): String =
            sha256(listOf(
                proof.transactionId, proof.checkpointSha256,
                proof.ownerDirectionSha256, proof.outcome.name,
                proof.expectedCreatorRecordSha256, proof.liveCreatorRecordSha256,
                proof.expectedPersonalSha256, proof.livePersonalSha256,
                proof.voiceIdeaCount.toString(), proof.bestFrameCount.toString(),
                proof.originalPhotoCount.toString(), proof.selectedPhotoCount.toString(),
                proof.preservedImportedJpegFiles.toString(),
                proof.archivedNativePersonalVerified.toString(),
            ).joinToString("\n", postfix = "\n")
                .toByteArray(Charsets.UTF_8))
        private fun canonical(value: V379UnifiedReleaseRecord) = listOf(
            FORMAT, value.transactionId, value.checkpointSha256,
            value.verificationSha256, value.outcome.name, value.step.name,
        ).joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        internal fun sha256(data: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

/** Same-volume atomic retention only. Never copy/delete a private owner ZIP. */
internal object V379AtomicOwnerHistory {
    fun retain(
        app: Context,
        transactionId: String,
        activeName: String,
        required: Boolean,
    ): File? {
        val files = app.applicationContext.filesDir.canonicalFile
        val source = File(files, activeName)
        check(source.parentFile.canonicalFile == files &&
            activeName.matches(Regex("[a-zA-Z0-9-]{1,100}"))) {
            "Unexpected owner journal retention path"
        }
        val history = File(files, "backlot-unified-retained-recovery-history-v1")
        if (!Files.exists(history.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            check(history.mkdir()) { "Cannot create private recovery history directory" }
        }
        check(history.isDirectory &&
            !Files.isSymbolicLink(history.toPath()) &&
            history.canonicalFile.parentFile == files) {
            "Retained owner history root is untrusted"
        }
        val directory = File(history, transactionId)
        if (!Files.exists(directory.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            check(directory.mkdir()) { "Cannot create transaction history directory" }
        }
        check(directory.isDirectory &&
            !Files.isSymbolicLink(directory.toPath()) &&
            directory.canonicalFile.parentFile == history.canonicalFile) {
            "Retained recovery history belongs to another owner"
        }
        val target = File(directory, activeName)
        val current = Files.exists(source.toPath(), LinkOption.NOFOLLOW_LINKS)
        val already = Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS)
        check(!(current && already) && (!required || current || already)) {
            "Missing or duplicated original owner evidence; cannot release H4"
        }
        if (current) {
            check(source.isDirectory &&
                !Files.isSymbolicLink(source.toPath()) &&
                source.canonicalFile.parentFile == files) {
                "Unsafe original owner recovery source directory"
            }
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            forceDirectory(directory)
            forceDirectory(history)
            forceDirectory(files)
        }
        if (current || already) {
            check(target.isDirectory &&
                !Files.isSymbolicLink(target.toPath()) &&
                target.canonicalFile.parentFile == directory.canonicalFile) {
                "Retained owner recovery evidence was not durably archived"
            }
            return target
        }
        return null
    }

    internal fun forceDirectory(dir: File) {
        FileChannel.open(dir.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
    }
}
