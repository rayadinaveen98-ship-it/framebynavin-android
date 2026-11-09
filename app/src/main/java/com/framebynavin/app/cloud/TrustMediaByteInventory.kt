package com.framebynavin.app.cloud

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/** A reference to an app-owned original file, never an external content URI. */
internal data class TrustMediaSource(
    val relativePath: String,
    val expectedSha256: String,
)

/**
 * Streaming, read-only proof of ORIGINAL private file bytes.
 *
 * A Room photo row or a Best Frames filename alone is NOT evidence that its
 * JPEG exists. Do not send any file bytes or user paths to a server here.
 * This inventory neither acquires cross-store atomicity nor authorizes upload.
 */
internal object TrustMediaByteInventory {
    private val safeSegment = Regex("[a-zA-Z0-9._-]{1,128}")
    private val hashPattern = Regex("[0-9a-f]{64}")
    private val bufferSize = 32 * 1024

    fun verify(
        domain: TrustBackupDomain,
        root: File,
        sources: List<TrustMediaSource>,
        maxEachBytes: Long,
        maxTotalBytes: Long,
        requireExactFileInventory: Boolean,
    ): TrustDomainEvidence {
        require(domain.portable)
        require(maxEachBytes in 1..Int.MAX_VALUE.toLong())
        require(maxTotalBytes in maxEachBytes..Int.MAX_VALUE.toLong())
        require(sources.size <= 500) { "Too many private files in inventory" }

        val rootFile = root.canonicalFile
        require(!Files.isSymbolicLink(root.toPath())) {
            "Private media directory link is not supported"
        }
        require(rootFile.isDirectory || (sources.isEmpty() && !rootFile.exists())) {
            "Private media directory is missing"
        }
        val sorted = sources.sortedBy { it.relativePath }
        require(sorted.map { it.relativePath }.distinct().size == sorted.size) {
            "Duplicate private file path"
        }

        val manifest = MessageDigest.getInstance("SHA-256")
        manifest.update("backlot-trust-original-media-v1\u0000".toByteArray(Charsets.UTF_8))
        var total = 0L
        val expectedPaths = HashSet<String>()
        sorted.forEach { item ->
            require(hashPattern.matches(item.expectedSha256)) {
                "Original file checksum is not valid"
            }
            val parts = item.relativePath.split('/')
            require(parts.isNotEmpty() && parts.size <= 2 &&
                parts.all { safeSegment.matches(it) && it != "." && it != ".." }) {
                "Unsafe original media filename"
            }
            var target = root
            parts.forEach { segment ->
                target = File(target, segment)
                require(!Files.isSymbolicLink(target.toPath())) {
                    "Private media symlinks are not supported"
                }
            }
            val file = target.canonicalFile
            require(file.toPath().startsWith(rootFile.toPath()) &&
                file != rootFile && file.isFile && file.canRead()) {
                "Original media is missing or outside private storage"
            }
            require(expectedPaths.add(file.path)) { "Duplicate canonical media file" }
            val length = file.length()
            require(length in 1..maxEachBytes && total <= maxTotalBytes - length) {
                "Original media exceeds the allowed size"
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var observed = 0L
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(bufferSize)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    observed += read
                    require(observed <= length && observed <= maxEachBytes) {
                        "Original media grew during integrity verification"
                    }
                    digest.update(buffer, 0, read)
                }
            }
            require(observed == length && file.length() == length) {
                "Original media changed during integrity verification"
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            require(actualHash == item.expectedSha256) {
                "Original media checksum mismatch"
            }
            total += length
            // Content digest is a function of the exact ordered byte checksums,
            // byte sizes and relative identities. No raw paths escape into it.
            val descriptor = "${item.relativePath}\u0000$length\u0000$actualHash\u0000"
            manifest.update(descriptor.toByteArray(Charsets.UTF_8))
        }
        if (requireExactFileInventory && rootFile.exists()) {
            val diskFiles = HashSet<String>()
            rootFile.walkTopDown().forEach { node ->
                require(!Files.isSymbolicLink(node.toPath())) {
                    "Untracked private media link"
                }
                if (node.isFile) {
                    require(diskFiles.size < 501) { "Too many private media files" }
                    diskFiles.add(node.canonicalPath)
                }
            }
            require(diskFiles == expectedPaths) {
                "Untracked or missing private media prevents complete inventory"
            }
        }
        val checksum = manifest.digest().joinToString("") { "%02x".format(it) }
        return TrustBackupInventoryPolicy.verifiedByteEvidence(
            domain, sorted.size, total, checksum,
        )
    }
}
