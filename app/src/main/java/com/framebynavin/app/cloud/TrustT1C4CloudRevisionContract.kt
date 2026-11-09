package com.framebynavin.app.cloud

import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * Trust T1C4A: provider-independent encrypted-object revision contract.
 *
 * This code is NOT wired to CloudApiClient, the UI, existing Personal v2,
 * Creator sync, or any remote bucket. It does not authorize an upload.
 * Real T1C4B needs owner-authenticated transport + verified RLS/CAS backend.
 */
internal data class TrustCloudRevisionHead(
    val revision: Long,
    val objectPath: String,
    val encryptedSha256: String,
    val encryptedBytes: Long,
)

internal data class TrustCloudCiphertextDescriptor(
    val ownerUserId: String,
    val objectPath: String,
    val encryptedSha256: String,
    val encryptedBytes: Long,
) {
    val canClaimUniversalBackup: Boolean get() = false
    val canRestoreFromThisDescriptor: Boolean get() = false
}

internal enum class TrustCloudPublishStage {
    LOCAL_ONLY,
    IMMUTABLE_UPLOAD_ACKNOWLEDGED,
    DOWNLOADED_AND_VERIFIED,
    HEAD_COMPARE_AND_SWAP_ACCEPTED,
    HEAD_READBACK_CONFIRMED,
}

internal data class TrustCloudRevisionProgress(
    val stage: TrustCloudPublishStage,
    val remoteRevision: Long?,
    val readbackSha256: String?,
) {
    /** A network acknowledgment alone must NEVER be treated as durable. */
    val canClaimDurableRemoteCopy: Boolean
        get() = stage == TrustCloudPublishStage.HEAD_READBACK_CONFIRMED
    /** Even full transport verification does not prove universal source coverage. */
    val canClaimCompleteBacklotBackup: Boolean get() = false
    val canOfferRestore: Boolean get() = false
}

/**
 * This proof exists only after reading all remote ciphertext bytes locally.
 * The private constructor prevents a plain HTTP/upload status from becoming
 * a simulated readback-success record.
 */
internal class TrustCloudVerifiedReadback private constructor(
    val descriptor: TrustCloudCiphertextDescriptor,
) {
    companion object {
        fun verify(
            stream: InputStream,
            descriptor: TrustCloudCiphertextDescriptor,
        ): TrustCloudVerifiedReadback {
            TrustT1C4CloudRevisionPolicy.verifyRemoteCiphertext(stream, descriptor)
            return TrustCloudVerifiedReadback(descriptor)
        }
    }
}

internal object TrustT1C4CloudRevisionPolicy {
    const val REVIEW_ONLY_BUCKET = "backlot-trust-universal-v1"
    const val MAX_ENCRYPTED_BYTES = 1024L * 1024 * 1024 + 50L * 1024 * 1024
    private val shaPattern = Regex("[0-9a-f]{64}")
    private val uuidPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    fun validateOwner(userId: String) {
        require(uuidPattern.matches(userId) &&
            UUID.fromString(userId).toString() == userId) {
            "Expected a signed-in owner UUID, not a name or email"
        }
    }

    fun newImmutablePath(ownerUserId: String, randomId: UUID): String {
        validateOwner(ownerUserId)
        return "$ownerUserId/revisions/v1-${randomId}.enc"
    }

    fun validate(
        descriptor: TrustCloudCiphertextDescriptor,
        expectedOwnerUserId: String,
    ) {
        validateOwner(expectedOwnerUserId)
        require(descriptor.ownerUserId == expectedOwnerUserId) {
            "Encrypted cloud object belongs to a different owner"
        }
        require(descriptor.encryptedSha256.matches(shaPattern) &&
            descriptor.encryptedBytes in 1..MAX_ENCRYPTED_BYTES) {
            "Invalid remote ciphertext digest or size"
        }
        val name = descriptor.objectPath.removePrefix(
            "$expectedOwnerUserId/revisions/",
        )
        require(descriptor.objectPath == "$expectedOwnerUserId/revisions/$name" &&
            name.startsWith("v1-") && name.endsWith(".enc")) {
            "Remote backup path must be immutable and owner-scoped"
        }
        val uniqueId = name.removePrefix("v1-").removeSuffix(".enc")
        require(uuidPattern.matches(uniqueId) &&
            UUID.fromString(uniqueId).toString() == uniqueId) {
            "Remote backup identity is invalid"
        }
    }

    fun validateRemoteHead(head: TrustCloudRevisionHead?, ownerUserId: String) {
        if (head == null) return
        require(head.revision > 0L)
        validate(
            TrustCloudCiphertextDescriptor(
                ownerUserId, head.objectPath, head.encryptedSha256,
                head.encryptedBytes,
            ),
            ownerUserId,
        )
    }

    /**
     * An old head is a precondition, never a deletion target. A concurrent
     * device advance must return conflict rather than silently overwrite it.
     */
    fun requireUnchanged(
        expected: TrustCloudRevisionHead?,
        actual: TrustCloudRevisionHead?,
        ownerUserId: String,
    ) {
        validateRemoteHead(expected, ownerUserId)
        validateRemoteHead(actual, ownerUserId)
        check(expected == actual) {
            "Cloud backup was updated from another device; review before retrying"
        }
    }

    fun acknowledgeNewHead(
        previous: TrustCloudRevisionHead?,
        claimed: TrustCloudRevisionHead,
        descriptor: TrustCloudCiphertextDescriptor,
        proof: TrustCloudVerifiedReadback,
    ): TrustCloudRevisionProgress {
        validate(descriptor, descriptor.ownerUserId)
        check(proof.descriptor == descriptor) {
            "Remote ciphertext readback does not match the committed object"
        }
        validateRemoteHead(previous, descriptor.ownerUserId)
        validateRemoteHead(claimed, descriptor.ownerUserId)
        val next = if (previous == null) 1L else {
            require(previous.revision < Long.MAX_VALUE)
            previous.revision + 1L
        }
        check(claimed.revision == next &&
            claimed.objectPath == descriptor.objectPath &&
            claimed.encryptedSha256 == descriptor.encryptedSha256 &&
            claimed.encryptedBytes == descriptor.encryptedBytes) {
            "Remote CAS response did not confirm exact uploaded encrypted bytes"
        }
        return TrustCloudRevisionProgress(
            TrustCloudPublishStage.HEAD_READBACK_CONFIRMED,
            claimed.revision, descriptor.encryptedSha256,
        )
    }

    /** Verify the ACTUAL download stream, not a provider's claimed checksum. */
    fun verifyRemoteCiphertext(
        stream: InputStream,
        descriptor: TrustCloudCiphertextDescriptor,
    ): Long {
        validate(descriptor, descriptor.ownerUserId)
        val hash = MessageDigest.getInstance("SHA-256")
        var readTotal = 0L
        var emptyReads = 0
        stream.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n == 0) {
                    require(++emptyReads <= 16) {
                        "Cloud download stalled during readback"
                    }
                    continue
                }
                emptyReads = 0
                require(readTotal <= descriptor.encryptedBytes - n) {
                    "Cloud object has unexpected or extra bytes"
                }
                hash.update(buffer, 0, n)
                readTotal += n
            }
        }
        val observed = hash.digest().joinToString("") { "%02x".format(it) }
        check(readTotal == descriptor.encryptedBytes &&
            observed == descriptor.encryptedSha256) {
            "Cloud ciphertext readback was truncated or did not match local SHA-256"
        }
        return readTotal
    }

    /**
     * The existing T1C3 preview explicitly describes itself as PARTIAL.
     * Guard the production upload entrypoint, even if somebody adds a button.
     */
    fun rejectUnqualifiedLocalPreview(preview: TrustLocalPartialPreviewReceipt): Nothing {
        check(!preview.isUniversalBackup && !preview.canUploadToCloud) {
            "Unexpected preview privilege escalation; release requires investigation"
        }
        error("Partial local review is not a verified complete owner backup")
    }
}
