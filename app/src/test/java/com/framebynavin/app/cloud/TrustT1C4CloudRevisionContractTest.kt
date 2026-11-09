package com.framebynavin.app.cloud

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** No live account, network access, cloud storage or user-owned data. */
class TrustT1C4CloudRevisionContractTest {
    private val owner = "00000000-0000-4000-8000-000000000001"
    private val otherOwner = "00000000-0000-4000-8000-000000000002"
    private val objectId = UUID.fromString("00000000-0000-4000-8000-000000000011")
    private val original = "fake encrypted bytes (fixture only)".toByteArray()
    private fun sha(b: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(b)
            .joinToString("") { "%02x".format(it) }
    private fun descriptor(bytes: ByteArray = original) =
        TrustCloudCiphertextDescriptor(
            owner, TrustT1C4CloudRevisionPolicy.newImmutablePath(owner, objectId),
            sha(bytes), bytes.size.toLong(),
        )

    @Test fun privateObjectPathsAreImmutableScopedCanonicalAndNeverPublic() {
        val candidate = descriptor()
        TrustT1C4CloudRevisionPolicy.validate(candidate, owner)
        assertTrue(candidate.objectPath.startsWith("$owner/revisions/v1-"))
        assertTrue(candidate.objectPath.endsWith(".enc"))
        assertFalse(candidate.canClaimUniversalBackup)
        assertFalse(candidate.canRestoreFromThisDescriptor)
        assertNotEquals(candidate.objectPath,
            TrustT1C4CloudRevisionPolicy.newImmutablePath(owner, UUID.randomUUID()))
        for (bad in listOf(
            candidate.copy(objectPath = "$otherOwner/revisions/v1-$objectId.enc"),
            candidate.copy(objectPath = "$owner/../../private/v1-$objectId.enc"),
            candidate.copy(objectPath = "$owner/revisions/v1-$objectId.enc/child"),
            candidate.copy(encryptedSha256 = "not-hash"),
            candidate.copy(encryptedBytes = 0L),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                TrustT1C4CloudRevisionPolicy.validate(bad, owner)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustT1C4CloudRevisionPolicy.validate(candidate, otherOwner)
        }
    }

    @Test fun exactDownloadedEncryptedBytesAreVerifiedIndependentlyOfServerClaim() {
        val candidate = descriptor()
        assertEquals(original.size.toLong(),
            TrustT1C4CloudRevisionPolicy.verifyRemoteCiphertext(
                ByteArrayInputStream(original), candidate))
        assertThrows(IllegalStateException::class.java) {
            TrustT1C4CloudRevisionPolicy.verifyRemoteCiphertext(
                ByteArrayInputStream("changed cloud object".toByteArray()), candidate)
        }
        assertThrows(IllegalStateException::class.java) {
            TrustT1C4CloudRevisionPolicy.verifyRemoteCiphertext(
                ByteArrayInputStream(original.copyOf(original.size - 2)), candidate)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustT1C4CloudRevisionPolicy.verifyRemoteCiphertext(
                ByteArrayInputStream(original + byteArrayOf(3)), candidate)
        }
    }

    @Test fun previousVerifiedHeadMustMatchBeforeAnyLaterRevisionAttempt() {
        val path = TrustT1C4CloudRevisionPolicy.newImmutablePath(
            owner, UUID.fromString("00000000-0000-4000-8000-000000000012"))
        val prior = TrustCloudRevisionHead(5L, path, sha("old".toByteArray()), 3L)
        TrustT1C4CloudRevisionPolicy.requireUnchanged(prior, prior, owner)
        assertThrows(IllegalStateException::class.java) {
            TrustT1C4CloudRevisionPolicy.requireUnchanged(
                prior, prior.copy(revision = 6), owner)
        }
        assertThrows(IllegalStateException::class.java) {
            TrustT1C4CloudRevisionPolicy.requireUnchanged(prior, null, owner)
        }
        assertThrows(IllegalStateException::class.java) {
            TrustT1C4CloudRevisionPolicy.requireUnchanged(null, prior, owner)
        }
    }

    @Test fun acceptedCasMustBeExactNextRevisionAndExactCiphertextObject() {
        val candidate = descriptor()
        val first = TrustCloudRevisionHead(
            revision = 1, objectPath = candidate.objectPath,
            encryptedSha256 = candidate.encryptedSha256,
            encryptedBytes = candidate.encryptedBytes,
        )
        val accepted = TrustT1C4CloudRevisionPolicy.acknowledgeNewHead(
            null, first, candidate,
        )
        assertEquals(TrustCloudPublishStage.HEAD_READBACK_CONFIRMED, accepted.stage)
        assertEquals(1L, accepted.remoteRevision)
        assertTrue(accepted.canClaimDurableRemoteCopy)
        assertFalse(accepted.canClaimCompleteBacklotBackup)
        assertFalse(accepted.canOfferRestore)

        val previous = first.copy(revision = 2,
            objectPath = TrustT1C4CloudRevisionPolicy.newImmutablePath(
                owner, UUID.fromString("00000000-0000-4000-8000-000000000013")))
        val next = first.copy(revision = 3)
        assertEquals(3L,
            TrustT1C4CloudRevisionPolicy.acknowledgeNewHead(previous, next, candidate)
                .remoteRevision)
        for (wrong in listOf(
            next.copy(revision = 4),
            next.copy(objectPath = previous.objectPath),
            next.copy(encryptedSha256 = sha("wrong".toByteArray())),
            next.copy(encryptedBytes = candidate.encryptedBytes + 1),
        )) {
            assertThrows(IllegalStateException::class.java) {
                TrustT1C4CloudRevisionPolicy.acknowledgeNewHead(
                    previous, wrong, candidate)
            }
        }
    }

    @Test fun noNetworkAcknowledgementCanClaimDurabilityOrUniversalBackup() {
        for (stage in TrustCloudPublishStage.entries) {
            val result = TrustCloudRevisionProgress(
                stage, null, null,
            )
            assertFalse(result.canClaimCompleteBacklotBackup)
            assertFalse(result.canOfferRestore)
            if (stage != TrustCloudPublishStage.HEAD_READBACK_CONFIRMED) {
                assertFalse(result.canClaimDurableRemoteCopy)
            }
        }
        val local = TrustLocalPartialPreviewReceipt(
            previewId = UUID.randomUUID().toString(),
            encryptedBytes = original.size.toLong(),
            encryptedSha256 = sha(original),
            verifiedParts = 8,
            verifiedAtMillis = 1_794_206_000_000L,
        )
        assertThrows(IllegalStateException::class.java) {
            TrustT1C4CloudRevisionPolicy.rejectUnqualifiedLocalPreview(local)
        }
    }

    @Test fun invalidAccountOrOldRemoteHeadNeverPassesValidation() {
        assertThrows(IllegalArgumentException::class.java) {
            TrustT1C4CloudRevisionPolicy.newImmutablePath("../owner", objectId)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustT1C4CloudRevisionPolicy.validateRemoteHead(
                TrustCloudRevisionHead(0, descriptor().objectPath,
                    descriptor().encryptedSha256, descriptor().encryptedBytes), owner)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustT1C4CloudRevisionPolicy.validateRemoteHead(
                TrustCloudRevisionHead(1, descriptor().objectPath,
                    descriptor().encryptedSha256, descriptor().encryptedBytes), otherOwner)
        }
    }
}
