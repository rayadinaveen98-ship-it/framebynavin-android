package com.framebynavin.app.cloud

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.security.MessageDigest

/**
 * T1C2 reusable byte-freeze primitive: copies source bytes into new app-private
 * temporary files while the caller holds the relevant owner mutation locks.
 * No cloud interaction, restore, content mutation, scheduling or owner UI.
 *
 * These source files are PLAINTEXT: caller must encrypt them with the separate
 * T1C1 staging codec, then close this snapshot in a finally/use block.
 */
internal data class TrustFrozenPart(
    val id: String,
    val records: Int,
    val bytes: Long,
    val sha256: String,
    val file: File,
) {
    fun source(): TrustStagingSource = TrustStagingSource(
        id, records, bytes, sha256,
    ) { file.inputStream() }
}

internal class TrustFrozenSourceFiles internal constructor(
    val parts: List<TrustFrozenPart>,
    private val tempDirectory: File,
) : AutoCloseable {
    val canClaimCompleteBackup: Boolean get() = false
    val canUploadAsUniversalBackup: Boolean get() = false

    fun sources(): List<TrustStagingSource> = parts.map { it.source() }

    override fun close() {
        try {
            check(tempDirectory.deleteRecursively() || !tempDirectory.exists()) {
                "Temporary plaintext owner snapshot could not be removed"
            }
        } finally {
            TrustTemporaryStageRegistry.deactivate(tempDirectory)
        }
    }
}

internal class TrustSourceFreezer(
    private val sourceRoot: File,
    private val tempRoot: File,
) : AutoCloseable {
    companion object {
        private const val MAX_SOURCES = 800
        private const val MAX_SINGLE = 64L * 1024 * 1024
        private const val MAX_TOTAL = 1024L * 1024 * 1024
        private val SAFE_ID = Regex("[A-Za-z0-9._/-]{1,128}")
        private val SHA256 = Regex("[0-9a-f]{64}")
    }

    private val dir = Files.createTempDirectory(
        tempRoot.canonicalFile.toPath(), "trust-t1c2-owner-bytes-",
    ).toFile()
    private val frozen = ArrayList<TrustFrozenPart>()
    private val ids = HashSet<String>()
    private var bytesWritten = 0L
    private var finished = false

    init {
        require(sourceRoot.canonicalFile.isDirectory && tempRoot.canonicalFile.isDirectory) {
            "Trusted owner source and temporary directories must exist"
        }
        TrustTemporaryStageRegistry.activate(dir)
    }

    fun addJson(id: String, raw: String, records: Int) =
        addStream(id, records, ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8)))

    /**
     * File must be an app-owned original, under the private sourceRoot. This
     * disallows SAF/external documents, traversal and symlink substitution.
     */
    fun addOwnedFile(
        id: String, file: File, records: Int, expectedSha256: String? = null,
    ) {
        check(!finished)
        val root = sourceRoot.canonicalFile
        val candidate = file.absoluteFile.normalize()
        require(candidate.toPath().startsWith(root.toPath())) {
            "Backup original lies outside private Backlot storage"
        }
        var segment = root.toPath()
        val relative = root.toPath().relativize(candidate.toPath())
        for (part in relative) {
            segment = segment.resolve(part)
            require(!Files.isSymbolicLink(segment)) {
                "Backup cannot follow a private-file symlink"
            }
        }
        val original = candidate.canonicalFile
        require(original.isFile && original.canRead() &&
            original.toPath().startsWith(root.toPath())) {
            "Original private media is not readable"
        }
        val beforeSize = original.length()
        val beforeModified = original.lastModified()
        require(beforeSize in 1..MAX_SINGLE) { "Invalid original media size" }
        addStream(id, records, original.inputStream(), expectedSha256)
        check(original.length() == beforeSize &&
            original.lastModified() == beforeModified) {
            "Owner media changed while being frozen; source must be inspected again"
        }
    }

    fun addStream(
        id: String, records: Int, input: InputStream, expectedSha256: String? = null,
    ) {
        check(!finished) { "Frozen snapshot was already finalized" }
        require(SAFE_ID.matches(id) && !id.startsWith('/') &&
            !id.endsWith('/') && !id.contains("..") && records >= 0) {
            "Invalid or unsafe Backlot source ID"
        }
        require(frozen.size < MAX_SOURCES && ids.add(id)) {
            "Duplicate or excessive Backlot source parts"
        }
        if (expectedSha256 != null) require(SHA256.matches(expectedSha256))
        val target = File(dir, "source-" + frozen.size.toString().padStart(4, '0'))
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var length = 0L
            input.use { source ->
                FileOutputStream(target).use { out ->
                    val buffer = ByteArray(32 * 1024)
                    var emptyReads = 0
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (count == 0) {
                            require(++emptyReads <= 16) {
                                "Owner source read stalled; partial snapshot rejected"
                            }
                            continue
                        }
                        emptyReads = 0
                        require(length <= MAX_SINGLE - count &&
                            bytesWritten <= MAX_TOTAL - count) {
                            "Owner snapshot exceeds local freezing limits"
                        }
                        out.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        length += count
                        bytesWritten += count
                    }
                    out.fd.sync()
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            check(expectedSha256 == null || hash == expectedSha256) {
                "Original source checksum changed during freeze"
            }
            check(target.length() == length &&
                hashFile(target) == hash) {
                "Copied owner source failed independent byte verification"
            }
            frozen += TrustFrozenPart(id, records, length, hash, target)
        } catch (error: Throwable) {
            target.delete()
            ids.remove(id)
            throw error
        }
    }

    fun sealPartial(): TrustFrozenSourceFiles {
        check(!finished && frozen.isNotEmpty()) {
            "Empty or already consumed owner snapshot"
        }
        finished = true
        return TrustFrozenSourceFiles(frozen.toList(), dir)
    }

    override fun close() {
        if (!finished) {
            try {
                check(dir.deleteRecursively() || !dir.exists()) {
                    "Failed to remove incomplete private owner source freeze"
                }
                finished = true
            } finally {
                TrustTemporaryStageRegistry.deactivate(dir)
            }
        }
    }

    private fun hashFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
