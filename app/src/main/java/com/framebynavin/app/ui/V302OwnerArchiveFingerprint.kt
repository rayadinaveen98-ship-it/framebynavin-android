package com.framebynavin.app.ui

import java.io.InputStream
import java.security.MessageDigest

/**
 * Reads the ENTIRE document-provider file with constant memory. A valid ZIP
 * preview alone does not establish byte-for-byte equality with the copy we
 * just exported; this independently checks the saved file.
 */
internal object V302OwnerArchiveFingerprint {
    // V300 caps private JPEGs at 500 MiB, plus bounded manifest and ZIP headers.
    const val MAX_SAVED_ARCHIVE_BYTES = 600L * 1024L * 1024L

    fun sha256OfAllBytes(
        input: InputStream,
        maxBytes: Long = MAX_SAVED_ARCHIVE_BYTES,
    ): ByteArray {
        require(maxBytes > 0L)
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            if (n == 0) continue
            total += n
            require(total <= maxBytes) { "Saved archive exceeds permitted size" }
            digest.update(buffer, 0, n)
        }
        require(total > 0L) { "Saved archive is empty" }
        return digest.digest()
    }

    fun matchesExportedBytes(expectedSha256: ByteArray, savedSha256: ByteArray): Boolean {
        require(expectedSha256.size == 32 && savedSha256.size == 32)
        return MessageDigest.isEqual(expectedSha256, savedSha256)
    }
}
