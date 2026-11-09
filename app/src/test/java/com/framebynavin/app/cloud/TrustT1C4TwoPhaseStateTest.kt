package com.framebynavin.app.cloud

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure fake-transport ordering contract. No live cloud bucket or records. */
class TrustT1C4TwoPhaseStateTest {
    private val owner = "00000000-0000-4000-8000-000000000001"
    private val oldId = UUID.fromString("00000000-0000-4000-8000-000000000021")
    private val newId = UUID.fromString("00000000-0000-4000-8000-000000000022")
    private val ciphertext = ByteArray(141_217) { (it % 239).toByte() }

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun candidate() = TrustCloudCiphertextDescriptor(
        owner,
        TrustT1C4CloudRevisionPolicy.newImmutablePath(owner, newId),
        hash(ciphertext),
        ciphertext.size.toLong(),
    )

    private fun prior() = TrustCloudRevisionHead(
        5L,
        TrustT1C4CloudRevisionPolicy.newImmutablePath(owner, oldId),
        hash("old good copy".toByteArray()), 13L,
    )

    @Test fun successfulProtocolRequiresAllFiveOrderedProofsAndNeverClaimsUniversal() {
        val target = candidate()
        val stage = TrustT1C4TwoPhaseState.begin(prior(), target)
        assertEquals(TrustCloudPublishStage.LOCAL_ONLY, stage.stage)
        val uploaded = stage.immutableUploadAcknowledged()
        val downloaded = uploaded.verifyActualDownload(ByteArrayInputStream(ciphertext))
        assertEquals(TrustCloudPublishStage.DOWNLOADED_AND_VERIFIED, downloaded.stage)
        downloaded.requireNoConcurrentAdvance(prior())
        val claimed = downloaded.serverCompareAndSwapAccepted()
        val result = claimed.confirmServerHead(TrustCloudRevisionHead(
            6L, target.objectPath, target.encryptedSha256, target.encryptedBytes,
        ))
        assertEquals(TrustCloudPublishStage.HEAD_READBACK_CONFIRMED, result.stage)
        assertEquals(6L, result.remoteRevision)
        assertTrue(result.canClaimDurableRemoteCopy)
        assertFalse(result.canClaimCompleteBacklotBackup)
        assertFalse(result.canOfferRestore)
    }

    @Test fun cannotSkipImmutableUploadProofOrRemoteDownloadProof() {
        val beginning = TrustT1C4TwoPhaseState.begin(null, candidate())
        assertThrows(IllegalStateException::class.java) {
            beginning.serverCompareAndSwapAccepted()
        }
        assertThrows(IllegalStateException::class.java) {
            beginning.verifyActualDownload(ByteArrayInputStream(ciphertext))
        }
        val uploaded = beginning.immutableUploadAcknowledged()
        assertThrows(IllegalStateException::class.java) {
            uploaded.serverCompareAndSwapAccepted()
        }
        assertThrows(IllegalStateException::class.java) {
            uploaded.immutableUploadAcknowledged()
        }
    }

    @Test fun corruptReadbackOrConcurrentDeviceAdvanceCannotReachCas() {
        val uploaded = TrustT1C4TwoPhaseState.begin(prior(), candidate())
            .immutableUploadAcknowledged()
        assertThrows(IllegalStateException::class.java) {
            uploaded.verifyActualDownload(
                ByteArrayInputStream(ciphertext.copyOf(ciphertext.size - 1)),
            )
        }
        val valid = uploaded.verifyActualDownload(ByteArrayInputStream(ciphertext))
        assertThrows(IllegalStateException::class.java) {
            valid.requireNoConcurrentAdvance(prior().copy(revision = 6))
        }
        val advanced = valid.serverCompareAndSwapAccepted()
        assertThrows(IllegalStateException::class.java) {
            advanced.confirmServerHead(
                prior().copy(revision = 6, objectPath = candidate().objectPath),
            )
        }
    }

    @Test fun neverReusePriorRemoteImmutablePathEvenWhenEncryptedShaMatches() {
        val original = prior()
        val attempted = candidate().copy(
            objectPath = original.objectPath,
            encryptedSha256 = original.encryptedSha256,
            encryptedBytes = original.encryptedBytes,
        )
        assertThrows(IllegalArgumentException::class.java) {
            TrustT1C4TwoPhaseState.begin(original, attempted)
        }
    }
}
