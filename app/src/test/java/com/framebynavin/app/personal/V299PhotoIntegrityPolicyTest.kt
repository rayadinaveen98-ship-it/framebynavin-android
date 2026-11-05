package com.framebynavin.app.personal

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V299PhotoIntegrityPolicyTest {
    private fun hash(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { "%02x".format(it) }

    @Test fun validPhotoBytesAreVerifiedByStreaming() {
        val bytes = ByteArray(64_000) { (it % 251).toByte() }
        assertTrue(V299PhotoIntegrityPolicy.matches(ByteArrayInputStream(bytes), hash(bytes)))
    }

    @Test fun modifiedOrMissingBytesNeverCountAsVerified() {
        val bytes = "my private jpeg content".toByteArray()
        val digest = hash(bytes)
        assertFalse(V299PhotoIntegrityPolicy.matches(
            ByteArrayInputStream("changed".toByteArray()), digest,
        ))
        assertFalse(V299PhotoIntegrityPolicy.matches(
            ByteArrayInputStream(byteArrayOf()), digest,
        ))
    }

    @Test fun oversizedInputIsRejectedWithoutLoadingItAll() {
        val bytes = ByteArray((V299PhotoIntegrityPolicy.MAX_BYTES + 1).toInt()) { 4 }
        assertFalse(V299PhotoIntegrityPolicy.matches(ByteArrayInputStream(bytes), hash(bytes)))
    }

    @Test fun invalidChecksumIsAnIntegrityError() {
        assertThrows(IllegalArgumentException::class.java) {
            V299PhotoIntegrityPolicy.matches(ByteArrayInputStream(byteArrayOf(1,2,3)), "bad")
        }
    }
}
