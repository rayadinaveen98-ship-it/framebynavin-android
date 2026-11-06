package com.framebynavin.app.personal

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/**
 * An independent, encrypted PRE-COMMIT RECOVERY COPY, not a live restore.
 *
 * Rollback requires the owner-provided passphrase again after process death.
 * Nothing in V305 reads or changes active Room, DataStore, alarms or photos.
 * The escrow is kept under the application's persistent PRIVATE filesDir
 * rather than evictable cache.
 */
internal data class V305EscrowReceipt(
    val id: String,
    val archiveSha256: String,
    val archiveBytes: Long,
    val baselineDigest: String,
) {
    /** This metadata NEVER authorizes a cross-store live commit. */
    val canWriteLiveOwnerData: Boolean get() = false
}

internal data class V305EscrowInventory(
    val sealedCandidates: Int,
    val incomplete: Int,
    val unrecognized: Int,
) {
    val needsReview: Boolean get() = incomplete > 0 || unrecognized > 0
}

/**
 * Dedicated escrow directory and a checksum-validated journal. The journal
 * only proves consistent local metadata, not origin authentication; an
 * encrypted archive is verified with its password at creation AND reopening.
 *
 * A crash can leave a partial private folder. Never silently clean such
 * folders on startup; report them for owner review.
 */
internal class V305RollbackEscrowStore(private val privateRoot: File) {
    private val uuid = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val hex64 = Regex("[a-f0-9]{64}")
    private val format = "backlot.personal.rollback-escrow"
    private val maxBytes = 600L * 1024L * 1024L

    /**
     * An isolated, owner-initiated escrow write. The verify callback MUST
     * authenticate the complete V300 ZIP (not just its header) and compare
     * every backed-up domain to the original snapshot. The store calls
     * verifier again on re-open before declaring SEALED.
     */
    suspend fun create(
        ownerRequested: Boolean,
        baselineDigest: String,
        writer: suspend (OutputStream) -> Unit,
        verifier: suspend (InputStream) -> Unit,
    ): V305EscrowReceipt {
        require(ownerRequested) { "Owner did not request a protected recovery copy" }
        require(hex64.matches(baselineDigest)) { "Invalid baseline fingerprint" }
        ensureRoot()
        val id = UUID.randomUUID().toString()
        val folder = File(privateRoot, id)
        check(folder.mkdir()) { "Cannot reserve escrow folder" }
        try {
            val partial = File(folder, "archive.partial")
            check(partial.createNewFile()) { "Escrow file already exists" }
            val outputDigest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            FileOutputStream(partial).use { file ->
                // V300 owns/closes its ZIP stream. This wrapper closes ONLY
                // itself so the underlying file can still be fsynced.
                val sink = object : FilterOutputStream(file) {
                    override fun write(value: Int) {
                        require(size < maxBytes) { "Escrow exceeds safe size" }
                        super.write(value)
                        outputDigest.update(value.toByte())
                        size++
                    }
                    override fun write(bytes: ByteArray, off: Int, len: Int) {
                        require(len >= 0 && size + len <= maxBytes) {
                            "Escrow exceeds safe size"
                        }
                        file.write(bytes, off, len)
                        outputDigest.update(bytes, off, len)
                        size += len
                    }
                    override fun close() { flush() }
                }
                writer(sink)
                sink.flush()
                file.fd.sync()
            }
            check(size in 1L..maxBytes) { "Empty or oversized escrow" }
            val digest = outputDigest.digest().hex()
            require(partial.length() == size && sha256File(partial) == digest) {
                "Persisted escrow bytes differ from encrypted export"
            }
            FileInputStream(partial).use { input -> verifier(input) }
            val saved = File(folder, "archive.zip")
            check(!saved.exists() && partial.renameTo(saved)) {
                "Failed to seal the independent escrow ZIP"
            }
            val receipt = V305EscrowReceipt(id, digest, size, baselineDigest)
            writeJournal(folder, receipt)
            // Independent second read, both local SHA-256 and full
            // encrypted-domain authentication before reporting success.
            verify(id, verifier)
            return receipt
        } catch (failure: Throwable) {
            eraseOwnedFolder(folder)
            throw failure
        }
    }

    suspend fun verify(
        id: String,
        verifier: suspend (InputStream) -> Unit,
    ): V305EscrowReceipt {
        val folder = directory(id)
        val receipt = parseJournal(folder, id)
        val archive = File(folder, "archive.zip")
        require(archive.isFile && archive.length() == receipt.archiveBytes &&
            sha256File(archive) == receipt.archiveSha256) {
            "Independent recovery copy is missing or altered"
        }
        require(folder.listFiles()?.map { it.name }?.toSet() ==
            setOf("archive.zip", "journal.json")) {
            "Escrow contains incomplete or untracked files"
        }
        archive.inputStream().use { verifier(it) }
        return receipt
    }

    /** Reports candidate states without pretending a password was checked. */
    fun inventory(): V305EscrowInventory {
        if (!privateRoot.exists()) return V305EscrowInventory(0, 0, 0)
        check(privateRoot.isDirectory && !java.nio.file.Files.isSymbolicLink(privateRoot.toPath())) {
            "Escrow root is unsafe"
        }
        val entries = checkNotNull(privateRoot.listFiles()) {
            "Escrow directory cannot be listed"
        }
        var sealed = 0
        var incomplete = 0
        var unknown = 0
        entries.forEach { child ->
            if (child.isDirectory && !java.nio.file.Files.isSymbolicLink(child.toPath()) &&
                uuid.matches(child.name)) {
                try {
                    val metadata = parseJournal(child, child.name)
                    val file = File(child, "archive.zip")
                    check(file.isFile && file.length() == metadata.archiveBytes &&
                        sha256File(file) == metadata.archiveSha256)
                    check(child.listFiles()?.map { it.name }?.toSet() ==
                        setOf("archive.zip", "journal.json"))
                    sealed++ // Candidate, NOT decrypted verification.
                } catch (_: Exception) {
                    incomplete++
                }
            } else unknown++
        }
        return V305EscrowInventory(sealed, incomplete, unknown)
    }

    /** Explicit owner discard. Escrows are never removed on app startup. */
    fun discard(id: String) {
        val folder = directory(id)
        if (folder.exists()) eraseOwnedFolder(folder)
    }

    private fun writeJournal(folder: File, receipt: V305EscrowReceipt) {
        val body = JSONObject()
            .put("format", format).put("schemaVersion", 1)
            .put("id", receipt.id)
            .put("archiveSha256", receipt.archiveSha256)
            .put("archiveBytes", receipt.archiveBytes)
            .put("baselineDigest", receipt.baselineDigest).toString()
        val record = JSONObject()
            .put("payload", body).put("sha256", sha256(body)).toString()
        val draft = File(folder, "journal.partial")
        FileOutputStream(draft).use {
            it.write(record.toByteArray(Charsets.UTF_8))
            it.fd.sync()
        }
        check(draft.renameTo(File(folder, "journal.json"))) {
            "Could not commit escrow verification record"
        }
    }

    private fun parseJournal(folder: File, id: String): V305EscrowReceipt {
        val file = File(folder, "journal.json")
        require(file.isFile && file.length() in 1L..2048L) {
            "Escrow verification record missing or invalid"
        }
        val record = JSONObject(file.readText(Charsets.UTF_8))
        val payload = record.getString("payload")
        require(record.getString("sha256") == sha256(payload)) {
            "Escrow verification record checksum mismatch"
        }
        val content = JSONObject(payload)
        require(content.getString("format") == format &&
            content.getInt("schemaVersion") == 1 &&
            content.getString("id") == id) {
            "Unrecognized escrow verification record"
        }
        val receipt = V305EscrowReceipt(
            id, content.getString("archiveSha256"),
            content.getLong("archiveBytes"), content.getString("baselineDigest"),
        )
        require(hex64.matches(receipt.archiveSha256) &&
            hex64.matches(receipt.baselineDigest) &&
            receipt.archiveBytes in 1L..maxBytes) {
            "Invalid escrow verification record bounds"
        }
        return receipt
    }

    private fun ensureRoot() {
        check(privateRoot.isDirectory || privateRoot.mkdirs()) {
            "Independent recovery copy cannot be created"
        }
        require(!java.nio.file.Files.isSymbolicLink(privateRoot.toPath())) {
            "Unsafe escrow directory"
        }
    }

    private fun directory(id: String): File {
        require(uuid.matches(id) && UUID.fromString(id).toString() == id) {
            "Invalid independent escrow identifier"
        }
        ensureRoot()
        val folder = File(privateRoot, id)
        require(folder.parentFile.canonicalFile == privateRoot.canonicalFile &&
            !java.nio.file.Files.isSymbolicLink(folder.toPath())) {
            "Unsafe independent escrow path"
        }
        return folder
    }

    private fun eraseOwnedFolder(folder: File) {
        require(folder.parentFile.canonicalFile == privateRoot.canonicalFile) {
            "Refusing to delete outside independent escrow root"
        }
        if (!folder.exists()) return
        java.nio.file.Files.walkFileTree(folder.toPath(),
            object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                override fun visitFile(
                    path: java.nio.file.Path,
                    attrs: java.nio.file.attribute.BasicFileAttributes,
                ): java.nio.file.FileVisitResult {
                    java.nio.file.Files.delete(path)
                    return java.nio.file.FileVisitResult.CONTINUE
                }
                override fun postVisitDirectory(
                    path: java.nio.file.Path,
                    error: java.io.IOException?,
                ): java.nio.file.FileVisitResult {
                    if (error != null) throw error
                    java.nio.file.Files.delete(path)
                    return java.nio.file.FileVisitResult.CONTINUE
                }
            })
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(16 * 1024)
            var count = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                if (n == 0) continue
                count += n
                require(count <= maxBytes) { "Escrow exceeds bound" }
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().hex()
    }

    private fun sha256(content: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8)).hex()

    private fun ByteArray.hex(): String =
        joinToString("") { "%02x".format(it) }
}
