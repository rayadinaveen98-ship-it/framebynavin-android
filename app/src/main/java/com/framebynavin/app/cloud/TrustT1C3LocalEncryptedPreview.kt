package com.framebynavin.app.cloud

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * T1C3: INTERNAL, explicitly invoked encryption/readback proof for the
 * PARTIAL sources supplied by T1C2. This is NOT a universal backup or
 * cloud-upload/restore entry point. No app UI or startup job invokes it.
 *
 * Security boundaries:
 * - never overwrite a previously verified encrypted partial preview;
 * - first create a new AtomicFile in an untrusted/pending directory;
 * - independently decrypt and verify every part before atomic promotion;
 * - delete abandoned plaintext cache *only within these known prefixes*;
 * - never store the passphrase or a plaintext backup in account storage;
 * - require the supplied identity and password to open a preview again.
 *
 * Crash-safe atomic file promotion is not proof that all Backlot producers
 * participated in the cross-store lock; that release gate remains open.
 */
internal data class TrustLocalPartialPreviewReceipt(
    val previewId: String,
    val encryptedBytes: Long,
    val encryptedSha256: String,
    val verifiedParts: Int,
    val verifiedAtMillis: Long,
) {
    val isUniversalBackup: Boolean get() = false
    val canUploadToCloud: Boolean get() = false
    val canRestoreOwnerData: Boolean get() = false
}

internal class TrustT1C3LocalEncryptedPreview(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.noBackupFilesDir, "trust-t1c3-local-review-v1")
    private val pending = File(root, "pending")
    private val verified = File(root, "verified")
    private val guard = Mutex()

    private val uuidPattern =
        Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    /**
     * Creates an encrypted local PARTIAL review. A returned receipt means
     * crypto readback succeeded, not that all Backlot data was backed up.
     * The owner-specific key still depends on caller's passphrase; no
     * server login or account binding is performed here.
     */
    suspend fun createPartialReview(
        assertedOwnerId: String,
        passphrase: CharArray,
    ): TrustLocalPartialPreviewReceipt = guard.withLock {
        withContext(Dispatchers.IO) {
            preparePrivateDirectories()
            cleanupAbandonedWhileExclusive()
            // Reject excessive retained encrypted previews rather than
            // deleting an owner's previously verified copy without consent.
            check(verified.listFiles().orEmpty().count {
                it.isFile && it.name.endsWith(".enc")
            } < 3) { "Too many existing encrypted review copies; no new backup was started" }

            val id = UUID.randomUUID().toString()
            val pendingFile = File(pending, "pending-$id.enc")
            val verifiedFile = File(verified, "verified-$id.enc")
            val atomic = AtomicFile(pendingFile)
            var promoted = false
            try {
                TrustT1C2OwnerSourceCapture(app).freezePartialReadOnly().use { frozen ->
                    var output: FileOutputStream? = null
                    try {
                        output = atomic.startWrite()
                        // The writer owns and closes its ZIP stream, including
                        // the passed FileOutputStream. AtomicFile.finishWrite
                        // must tolerate the already-closed stream; Android's
                        // AtomicFile uses sync/close and is best-effort on it.
                        TrustEncryptedStagingCodec.write(
                            assertedOwnerId, passphrase, frozen.sources(), output,
                        )
                        atomic.finishWrite(output)
                        output = null
                    } catch (failure: Throwable) {
                        output?.let { runCatching { atomic.failWrite(it) } }
                        throw failure
                    }

                    val checked = verifyEncrypted(
                        pendingFile, assertedOwnerId, passphrase, frozen.parts,
                    )
                    check(checked > 0) {
                        "Encrypted owner preview did not contain any source parts"
                    }
                    check(!verifiedFile.exists()) { "Encrypted preview ID collision" }
                    // Same app-private filesystem; atomic rename never replaces
                    // an older verified backup or a legacy cloud head.
                    Files.move(
                        pendingFile.toPath(), verifiedFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                    promoted = true
                    require(verifiedFile.isFile) { "Encrypted review promotion failed" }
                    TrustLocalPartialPreviewReceipt(
                        previewId = id,
                        encryptedBytes = verifiedFile.length(),
                        encryptedSha256 = sha256(verifiedFile),
                        verifiedParts = checked,
                        verifiedAtMillis = System.currentTimeMillis(),
                    )
                }
            } catch (failure: Throwable) {
                // Do not delete previously committed reviews. This attempt
                // owns only its pending file, never an existing verified copy.
                if (!promoted) {
                    runCatching { AtomicFile(pendingFile).delete() }
                        .onFailure(failure::addSuppressed)
                }
                throw failure
            }
        }
    }

    /**
     * Re-open one previously promoted encrypted preview and validate each
     * authenticated source, without changing an owner record or restoring.
     */
    suspend fun verifyPartialReview(
        previewId: String,
        assertedOwnerId: String,
        passphrase: CharArray,
    ): TrustLocalPartialPreviewReceipt = guard.withLock {
        withContext(Dispatchers.IO) {
            require(uuidPattern.matches(previewId)) { "Invalid private review identifier" }
            preparePrivateDirectories()
            val file = File(verified, "verified-$previewId.enc")
            require(file.isFile && !Files.isSymbolicLink(file.toPath())) {
                "Encrypted review does not exist"
            }
            val parts = file.inputStream().buffered().use { input ->
                TrustEncryptedStagingCodec.stageAndVerify(
                    assertedOwnerId, passphrase, input, app.cacheDir,
                ).use { staged ->
                    require(staged.parts.isNotEmpty())
                    staged.parts.size
                }
            }
            TrustLocalPartialPreviewReceipt(
                previewId, file.length(), sha256(file), parts,
                verifiedAtMillis = System.currentTimeMillis(),
            )
        }
    }

    private fun verifyEncrypted(
        file: File,
        owner: String,
        passphrase: CharArray,
        expected: List<TrustFrozenPart>,
    ): Int {
        require(file.isFile && file.length() > 0L)
        val known = expected.associateBy { it.id }
        file.inputStream().buffered().use { input ->
            TrustEncryptedStagingCodec.stageAndVerify(
                owner, passphrase, input, app.cacheDir,
            ).use { decrypted ->
                require(decrypted.parts.size == expected.size) {
                    "Frozen owner parts were omitted from encrypted staging"
                }
                for (part in decrypted.parts) {
                    val original = known[part.id]
                        ?: error("Unknown source inserted into local encrypted preview")
                    require(
                        part.records == original.records &&
                            part.sizeBytes == original.bytes &&
                            part.sha256 == original.sha256,
                    ) { "Encrypted preview did not match the immutable owner source" }
                }
                return decrypted.parts.size
            }
        }
    }

    private fun preparePrivateDirectories() {
        require(root.isDirectory || root.mkdirs())
        require(pending.isDirectory || pending.mkdirs())
        require(verified.isDirectory || verified.mkdirs())
        for (path in listOf(root, pending, verified)) {
            require(!Files.isSymbolicLink(path.toPath())) {
                "Private preview directory cannot be a symlink"
            }
        }
    }

    /**
     * Must run ONLY with this coordinator's mutex and with no other T1C
     * producer. Does not touch ordinary cache or any unknown user filename.
     * Intended to be called again on next explicit owner preview after crash.
     * NOT an automatic startup recovery guarantee.
     */
    private fun cleanupAbandonedWhileExclusive() {
        val safePending = Regex("pending-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.enc(\\.new|\\.bak)?")
        for (file in pending.listFiles().orEmpty()) {
            require(file.name.matches(safePending)) {
                "Unexpected item in private pending preview directory"
            }
            require(!Files.isSymbolicLink(file.toPath()) && file.isFile) {
                "Unexpected private pending preview file type"
            }
            check(file.delete()) { "Unable to remove abandoned encrypted preview" }
        }
        // The T1C2 source reader writes only to this cache prefix.
        for (file in app.cacheDir.listFiles().orEmpty()) {
            if (!file.name.startsWith("trust-t1c2-owner-bytes-")) continue
            require(
                Regex("trust-t1c2-owner-bytes-[A-Za-z0-9_-]{1,40}")
                    .matches(file.name) &&
                    file.isDirectory &&
                    !Files.isSymbolicLink(file.toPath()),
            ) { "Unrecognized owner staging directory requires manual review" }
            require(file.deleteRecursively() || !file.exists()) {
                "Abandoned plaintext owner snapshot could not be removed"
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
