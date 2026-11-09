package com.framebynavin.app.cloud

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owner-initiated offline transfer of the SAME authenticated encrypted source
 * that Backlot uses for cloud backup. Works for archives above the cloud's
 * single-object cap, without adding a server, permission, plaintext export,
 * or second Creator/Personal backup format.
 *
 * Export is never evidence of a cloud upload. Import only adds an immutable
 * encrypted local COPY to the same-account catalog; it does not restore or
 * schedule any Owner data on the destination device.
 */
internal class TrustOwnerDocumentTransfer(context: Context) {
    private val app = context.applicationContext
    private val accounts = CloudSyncManager(app)
    private val local = TrustOwnerEncryptedLocalCopy(app)
    private val root = File(app.noBackupFilesDir, "trust-fc13-owner-encrypted-v1")
    private val pending = File(root, "pending")
    private val verified = File(root, "verified")

    internal data class ExportReceipt(
        val encryptedBytes: Long,
        val encryptedSha256: String,
        val sourceCount: Int,
    )

    /** SAF CreateDocument URI must be returned by an explicit owner picker. */
    suspend fun exportOwnerApproved(
        selected: TrustOwnerEncryptedLocalCopy.Receipt,
        passphrase: CharArray,
        chosenDocument: Uri,
    ): ExportReceipt = local.withVerifiedCopyForPublish(
        selected, passphrase,
    ) { session, encryptedFile, sourceReview ->
        require(session.userId == selected.ownerId)
        require(chosenDocument.scheme == "content") {
            "A system-selected document is required to export an encrypted copy"
        }
        withContext(Dispatchers.IO) {
            val expected = selected.encryptedSha256
            val count = encryptedFile.length()
            require(count == selected.encryptedBytes && count in 1..MAX_BYTES)
            // No decrypted bytes leave the app. Only AES-GCM ciphertext is copied.
            val resolver = app.contentResolver
            val target = requireNotNull(resolver.openOutputStream(chosenDocument, "wt")) {
                "The selected document provider cannot write an encrypted backup"
            }
            val copied = MessageDigest.getInstance("SHA-256")
            var bytes = 0L
            target.use { output ->
                encryptedFile.inputStream().buffered().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val amount = input.read(buffer)
                        if (amount < 0) break
                        if (amount == 0) continue
                        bytes += amount
                        check(bytes <= count) { "Encrypted source grew during export" }
                        output.write(buffer, 0, amount)
                        copied.update(buffer, 0, amount)
                    }
                    output.flush()
                }
            }
            check(bytes == count &&
                copied.digest().toHex() == expected) {
                "Encrypted document export could not verify its source"
            }
            // SAF providers may be remote: success is reported ONLY after a
            // second, independent read of what the provider stored.
            val copiedBack = requireNotNull(
                resolver.openInputStream(chosenDocument),
            ) { "The document provider cannot verify the copied archive" }
            val readback = digest(copiedBack)
            check(readback.first == count && readback.second == expected) {
                "The selected document provider did not preserve the full encrypted copy"
            }
            ExportReceipt(count, expected, sourceReview.sourcePartsValidated)
        }
    }

    /**
     * SAF OpenDocument URI. The authenticated archive MUST identify the active
     * owner and validate every required Creator/Personal source before its
     * ciphertext is promoted. No local Creator/Personal data is touched.
     */
    suspend fun importOwnerApproved(
        passphrase: CharArray,
        chosenDocument: Uri,
    ): TrustOwnerEncryptedLocalCopy.Receipt =
        accounts.withFreshSession { session ->
            TrustT1C3OperationGate.withExclusive(app) {
                require(passphrase.size in 12..512 &&
                    chosenDocument.scheme == "content") {
                    "Sign in, choose a document and enter its encryption password"
                }
                TrustT1C4CloudRevisionPolicy.validateOwner(session.userId)
                prepareFolders()
                check(verified.listFiles().orEmpty().count {
                    it.isFile && it.name.endsWith(".enc")
                } < 3) {
                    "There are already three encrypted owner copies. None were removed"
                }
                val id = UUID.randomUUID().toString()
                val pendingFile = File(pending, "pending-$id.enc")
                val finalFile = File(verified, "copy-$id.enc")
                val atomic = AtomicFile(pendingFile)
                var promoted = false
                try {
                    val byteHash = MessageDigest.getInstance("SHA-256")
                    var copied = 0L
                    var handle: FileOutputStream? = null
                    try {
                        val saved = atomic.startWrite()
                        handle = saved
                        val origin = requireNotNull(
                            app.contentResolver.openInputStream(chosenDocument),
                        ) { "Cannot read the selected encrypted owner archive" }
                        origin.buffered().use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var stalled = 0
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                if (n == 0) {
                                    check(++stalled <= 16) {
                                        "External archive read stalled"
                                    }
                                    continue
                                }
                                stalled = 0
                                check(copied <= MAX_BYTES - n) {
                                    "Encrypted owner file exceeds the safe local limit"
                                }
                                saved.write(buffer, 0, n)
                                byteHash.update(buffer, 0, n)
                                copied += n
                            }
                        }
                        check(copied in 1..MAX_BYTES)
                        atomic.finishWrite(saved)
                        handle = null
                    } catch (failure: Throwable) {
                        handle?.let { runCatching { atomic.failWrite(it) } }
                        throw failure
                    }
                    val expected = byteHash.digest().toHex()
                    check(pendingFile.length() == copied &&
                        digest(pendingFile.inputStream()).let {
                            it.first == copied && it.second == expected
                        }) {
                        "Selected encrypted archive failed private disk readback"
                    }

                    val review = pendingFile.inputStream().buffered().use { encrypted ->
                        TrustEncryptedStagingCodec.stageAndVerify(
                            session.userId, passphrase, encrypted, app.cacheDir,
                        ).use { staged ->
                            TrustRestorableSourceReview.inspect(app, staged)
                        }
                    }
                    check(review.missingRequiredSources.isEmpty()) {
                        "Selected encrypted archive is missing Creator/Personal source records"
                    }
                    check(!finalFile.exists())
                    Files.move(
                        pendingFile.toPath(), finalFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                    promoted = true
                    check(digest(finalFile.inputStream()).let {
                        it.first == copied && it.second == expected
                    }) {
                        "Imported encrypted file failed final disk readback"
                    }
                    val receipt = TrustOwnerEncryptedLocalCopy.Receipt(
                        ownerId = session.userId,
                        copyId = id,
                        encryptedBytes = copied,
                        encryptedSha256 = expected,
                        verifiedSources = review.sourcePartsValidated,
                        capturedRecords = review.counts.values.sum(),
                        createdAtMillis = System.currentTimeMillis().coerceAtLeast(1L),
                        uncoveredSavedKeys = review.uncoveredPreferenceKeys,
                    )
                    TrustOwnerCopyCatalog.persist(root, receipt)
                    receipt
                } catch (failure: Throwable) {
                    if (!promoted) runCatching { atomic.delete() }
                        .onFailure(failure::addSuppressed)
                    // A promoted file with a failed receipt is deliberately
                    // retained for owner recovery, never overwritten/deleted.
                    throw failure
                }
            }
        }

    private fun prepareFolders() {
        for (directory in listOf(root, pending, verified)) {
            check(directory.isDirectory || directory.mkdirs())
            check(!Files.isSymbolicLink(directory.toPath())) {
                "Private encrypted owner archive must not be a symlink"
            }
        }
    }

    private fun digest(source: java.io.InputStream): Pair<Long, String> {
        val hash = MessageDigest.getInstance("SHA-256")
        var total = 0L
        source.buffered().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var stalled = 0
            while (true) {
                val amount = input.read(buffer)
                if (amount < 0) break
                if (amount == 0) {
                    check(++stalled <= 16) { "Encrypted archive read stalled" }
                    continue
                }
                stalled = 0
                check(total <= MAX_BYTES - amount) { "Encrypted archive exceeds local cap" }
                total += amount
                hash.update(buffer, 0, amount)
            }
        }
        return total to hash.digest().toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private companion object {
        const val BUFFER_SIZE = 32 * 1024
        const val MAX_BYTES = 1024L * 1024 * 1024
    }
}
