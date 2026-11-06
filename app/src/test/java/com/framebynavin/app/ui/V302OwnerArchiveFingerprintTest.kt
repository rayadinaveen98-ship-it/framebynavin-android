package com.framebynavin.app.ui

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V302OwnerArchiveFingerprintTest {
    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    @Test fun exactSavedBytesMatchExportedDigest() {
        val bytes = ByteArray(70_000) { (it % 251).toByte() }
        assertTrue(V302OwnerArchiveFingerprint.matchesExportedBytes(
            sha(bytes), V302OwnerArchiveFingerprint.sha256OfAllBytes(
                ByteArrayInputStream(bytes),
            ),
        ))
    }

    @Test fun modifiedOrTruncatedProviderCopyCannotBeCalledVerified() {
        val original = ByteArray(2_000) { (it % 251).toByte() }
        val changed = original.copyOf().apply { this[800] = 15 }
        assertFalse(V302OwnerArchiveFingerprint.matchesExportedBytes(
            sha(original), V302OwnerArchiveFingerprint.sha256OfAllBytes(
                ByteArrayInputStream(changed),
            ),
        ))
        assertFalse(V302OwnerArchiveFingerprint.matchesExportedBytes(
            sha(original), V302OwnerArchiveFingerprint.sha256OfAllBytes(
                ByteArrayInputStream(original.copyOf(500)),
            ),
        ))
    }

    @Test fun emptyOrOversizedSavedCopyFailsBeforeVerification() {
        assertThrows(IllegalArgumentException::class.java) {
            V302OwnerArchiveFingerprint.sha256OfAllBytes(
                ByteArrayInputStream(byteArrayOf()),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            V302OwnerArchiveFingerprint.sha256OfAllBytes(
                ByteArrayInputStream(ByteArray(100)), maxBytes = 50,
            )
        }
    }
}
