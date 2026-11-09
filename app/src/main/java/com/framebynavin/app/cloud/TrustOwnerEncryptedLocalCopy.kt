package com.framebynavin.app.cloud

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owner-invoked, encrypted local COPY of Creator + Personal sources.
 *
 * Works entirely in the app's private no-backup directory. It never changes
 * user records, sends a cloud request, restores data or resets Android alarms.
 * All source bytes are frozen under the existing Creator -> Personal lock
 * order, then encrypted before disposing the temporary plaintext snapshot.
 *
 * A receipt proves authentic, independently readable SOURCE bytes. It does
 * not attest that every saved preference has a safe cross-domain RESTORE
 * adapter; cloud publication requires a separate explicit reviewed gate.
 */
internal class TrustOwnerEncryptedLocalCopy(context: Context) {
    private val app = context.applicationContext
    private val accounts = CloudSyncManager(app)
    private val root = File(app.noBackupFilesDir, "trust-fc13-owner-encrypted-v1")
    private val pending = File(root, "pending")
    private val verified = File(root, "verified")
    private val uuidPattern = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
    )

    internal data class Receipt(
        val ownerId: String,
        val copyId: String,
        val encryptedBytes: Long,
        val encryptedSha256: String,
        val verifiedSources: Int,
        val capturedRecords: Int,
        val createdAtMillis: Long,
        val uncoveredSavedKeys: List<String>,
    ) {
        // Requires a separate, independently verified Creator+Personal
        // restore and remote head CAS. A local receipt never grants those.
        val mayReplaceOwnerData: Boolean get() = false
        val mayPublishCloudHead: Boolean get() = false
    }

    suspend fun createAfterOwnerRequest(
        passphrase: CharArray,
    ): Receipt = accounts.withFreshSession { session ->
        val ownerUserId = session.userId
        TrustT1C3OperationGate.withExclusive(app) {
        TrustT1C4CloudRevisionPolicy.validateOwner(ownerUserId)
        require(passphrase.size in 12..512) {
            "An owner passphrase of at least 12 characters is required"
        }
        prepareFolders()
        check(verified.listFiles().orEmpty().count { it.isFile &&
            it.name.endsWith(".enc") } < 3) {
            "Encrypted owner copies are full; no existing copy was erased"
        }
        val id = UUID.randomUUID().toString()
        val pendingFile = File(pending, "pending-$id.enc")
        val verifiedFile = File(verified, "copy-$id.enc")
        val atomic = AtomicFile(pendingFile)
        var promoted = false
        try {
            val verifiedSource = TrustT1C2OwnerSourceCapture(app)
                .freezePartialReadOnly().use { source ->
                    var fd: FileOutputStream? = null
                    try {
                        val writable = atomic.startWrite()
                        fd = writable
                        TrustEncryptedStagingCodec.write(
                            ownerUserId, passphrase, source.sources(),
                            FlushOnlyOutputStream(writable),
                        )
                        atomic.finishWrite(writable)
                        fd = null
                    } catch (failure: Throwable) {
                        fd?.let { runCatching { atomic.failWrite(it) } }
                        throw failure
                    }
                    // Data was encrypted BEFORE the frozen plaintext sources
                    // leave this use scope; verify while the original capture
                    // is still available for byte-level source comparisons.
                    pendingFile.inputStream().buffered().use { encrypted ->
                        TrustEncryptedStagingCodec.stageAndVerify(
                            ownerUserId, passphrase, encrypted, app.cacheDir,
                        ).use { staged ->
                            val actual = source.parts.associateBy { it.id }
                            check(staged.parts.size == actual.size)
                            staged.parts.forEach { incoming ->
                                val original = requireNotNull(actual[incoming.id]) {
                                    "New encrypted snapshot omitted a captured owner source"
                                }
                                check(incoming.records == original.records &&
                                    incoming.sizeBytes == original.bytes &&
                                    incoming.sha256 == original.sha256) {
                                    "Encrypted owner source changed during staging"
                                }
                            }
                            TrustRestorableSourceReview.inspect(app, staged)
                        }
                    }
                }
            check(verifiedSource.missingRequiredSources.isEmpty()) {
                "Required Creator/Personal owner sources are missing: " +
                    verifiedSource.missingRequiredSources.joinToString()
            }
            val bytes = pendingFile.length()
            check(bytes > 0L && bytes <= 1024L * 1024 * 1024) {
                "The encrypted owner archive exceeds the supported local size"
            }
            val digest = sha256(pendingFile)
            check(!verifiedFile.exists()) { "New encrypted copy ID collided" }
            Files.move(
                pendingFile.toPath(), verifiedFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
            promoted = true
            check(verifiedFile.isFile && verifiedFile.length() == bytes &&
                sha256(verifiedFile) == digest) {
                "Promoted private encrypted owner copy failed final readback"
            }
            val receipt = Receipt(
                ownerUserId, id, bytes, digest,
                verifiedSource.sourcePartsValidated,
                verifiedSource.counts.values.sum(),
                System.currentTimeMillis().coerceAtLeast(1L),
                verifiedSource.uncoveredPreferenceKeys,
            )
            // An encrypted copy must survive app restarts as a discoverable
            // owner artifact. A failed catalog write retains the ciphertext
            // for explicit owner recovery; no older copy is removed.
            TrustOwnerCopyCatalog.persist(root, receipt)
            receipt
        } catch (failure: Throwable) {
            // Only the current pending attempt is removable. A promoted
            // archive must be retained for owner review rather than silently
            // discarded after a post-promotion I/O failure.
            if (!promoted) runCatching { atomic.delete() }
                .onFailure(failure::addSuppressed)
            throw failure
        }
        }
    }

    /**
     * Re-authenticates an existing private archive and independently checks
     * the disk SHA and every payload relationship before any publication.
     * Never returns a raw file path to a UI caller.
     */
    suspend fun inspectExisting(
        receipt: Receipt,
        passphrase: CharArray,
    ): TrustRestorableSourceReview.Result =
        accounts.withFreshSession { session ->
        check(session.userId == receipt.ownerId) {
            "The signed-in owner changed; this private archive cannot be opened"
        }
        TrustT1C3OperationGate.withExclusive(app) {
            TrustT1C4CloudRevisionPolicy.validateOwner(receipt.ownerId)
            require(uuidPattern.matches(receipt.copyId) &&
                passphrase.size in 12..512)
            prepareFolders()
            val persisted = TrustOwnerCopyCatalog.inspect(root, receipt.ownerId)
                .receipts.singleOrNull { it.copyId == receipt.copyId }
            check(persisted == receipt) {
                "This encrypted owner copy receipt was not persisted or was changed"
            }
            val file = File(verified, "copy-${receipt.copyId}.enc")
            check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.length() == receipt.encryptedBytes &&
                sha256(file) == receipt.encryptedSha256) {
                "The selected owner-encrypted copy is missing or changed"
            }
            file.inputStream().buffered().use { encrypted ->
                TrustEncryptedStagingCodec.stageAndVerify(
                    receipt.ownerId, passphrase, encrypted, app.cacheDir,
                ).use { staged ->
                    TrustRestorableSourceReview.inspect(app, staged).also { review ->
                        check(review.missingRequiredSources.isEmpty())
                        check(review.sourcePartsValidated == receipt.verifiedSources)
                    }
                }
            }
        }
        }

    /**
     * Internal owner-bound handoff used ONLY by the universal cloud publisher.
     * Revalidates session, local receipt, ciphertext and decrypted source coverage
     * under the existing operation/file lock. The file cannot escape via a UI
     * path, and no plaintext archive or passphrase is published.
     */
    internal suspend fun <T> withVerifiedCopyForPublish(
        reviewed: Receipt,
        passphrase: CharArray,
        action: suspend (
            session: CloudSession,
            ciphertext: File,
            review: TrustRestorableSourceReview.Result,
        ) -> T,
    ): T = accounts.withFreshSession { session ->
        require(session.userId == reviewed.ownerId) {
            "Signed-in owner changed after selecting the encrypted copy"
        }
        require(passphrase.size in 12..512 && uuidPattern.matches(reviewed.copyId))
        TrustT1C3OperationGate.withExclusive(app) {
            prepareFolders()
            val saved = TrustOwnerCopyCatalog.inspect(root, session.userId)
                .receipts.singleOrNull { it.copyId == reviewed.copyId }
            check(saved == reviewed) { "Encrypted owner receipt changed after review" }
            val file = File(verified, "copy-${reviewed.copyId}.enc")
            check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.length() == reviewed.encryptedBytes &&
                sha256(file) == reviewed.encryptedSha256) {
                "Encrypted owner copy changed; cloud publication refused"
            }
            file.inputStream().buffered().use { encrypted ->
                TrustEncryptedStagingCodec.stageAndVerify(
                    session.userId, passphrase, encrypted, app.cacheDir,
                ).use { parts ->
                    val coverage = TrustRestorableSourceReview.inspect(app, parts)
                    check(coverage.missingRequiredSources.isEmpty() &&
                        coverage.uncoveredPreferenceKeys.isEmpty() &&
                        coverage.sourcePartsValidated == reviewed.verifiedSources &&
                        coverage.counts.values.sum() == reviewed.capturedRecords
                    ) {
                        "This source copy has missing or unrestorable owner data. " +
                            "No cloud backup was written"
                    }
                    action(session, file, coverage)
                }
            }
        }
    }

    /**
     * Reopen existing encrypted copies after Backlot restarts without reading
     * any owner notes, voice recordings, reminders or plaintext media.
     * Every entry still requires a password and disk checksum for inspection.
     */
    suspend fun availableCopiesForSignedInOwner(): TrustOwnerCopyCatalog.Inventory =
        accounts.withFreshSession { session ->
            TrustT1C3OperationGate.withExclusive(app) {
                prepareFolders()
                TrustOwnerCopyCatalog.inspect(root, session.userId)
            }
        }

    private fun prepareFolders() {
        for (directory in listOf(root, pending, verified)) {
            check(directory.isDirectory || directory.mkdirs()) {
                "Unable to prepare private encrypted owner copy directory"
            }
            check(!Files.isSymbolicLink(directory.toPath())) {
                "Encrypted owner copy directory cannot be a symlink"
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { source ->
            val buf = ByteArray(32 * 1024)
            var stalled = 0
            while (true) {
                val count = source.read(buf)
                if (count < 0) break
                if (count == 0) {
                    check(++stalled <= 16) { "Private archive read stalled" }
                    continue
                }
                stalled = 0
                digest.update(buf, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // The staging codec closes its own ZIP output stream. Preserve AtomicFile
    // FD ownership so finishWrite can fsync/close and atomically commit.
    private class FlushOnlyOutputStream(output: OutputStream) :
        FilterOutputStream(output) {
        override fun close() = flush()
    }
}
