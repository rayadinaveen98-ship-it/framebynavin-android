package com.framebynavin.app.cloud

import java.io.InputStream

/**
 * Offline transaction-state contract for a future authenticated cloud
 * adapter. This object performs NO network, SQL, Storage, upload or restore.
 *
 * Sequence is mandatory:
 * NEW UNIQUE KEY -> acknowledged immutable upload -> ACTUAL download proof
 * -> atomic CAS accepted -> fetch same verified head -> confirmed.
 *
 * Never erases prior remote heads and never grants universal-source coverage.
 */
internal class TrustT1C4TwoPhaseState private constructor(
    private val previous: TrustCloudRevisionHead?,
    val proposed: TrustCloudCiphertextDescriptor,
    val stage: TrustCloudPublishStage,
    private val readbackProof: TrustCloudVerifiedReadback? = null,
) {
    companion object {
        fun begin(
            before: TrustCloudRevisionHead?,
            proposed: TrustCloudCiphertextDescriptor,
        ): TrustT1C4TwoPhaseState {
            TrustT1C4CloudRevisionPolicy.validate(
                proposed, proposed.ownerUserId,
            )
            TrustT1C4CloudRevisionPolicy.validateRemoteHead(
                before, proposed.ownerUserId,
            )
            require(before?.objectPath != proposed.objectPath) {
                "A new immutable cloud object must not overwrite the previous good copy"
            }
            return TrustT1C4TwoPhaseState(
                before, proposed, TrustCloudPublishStage.LOCAL_ONLY,
            )
        }
    }

    fun immutableUploadAcknowledged(): TrustT1C4TwoPhaseState {
        check(stage == TrustCloudPublishStage.LOCAL_ONLY) {
            "Cloud object upload acknowledged out of sequence"
        }
        return TrustT1C4TwoPhaseState(
            previous, proposed, TrustCloudPublishStage.IMMUTABLE_UPLOAD_ACKNOWLEDGED,
        )
    }

    fun verifyActualDownload(download: InputStream): TrustT1C4TwoPhaseState {
        check(stage == TrustCloudPublishStage.IMMUTABLE_UPLOAD_ACKNOWLEDGED) {
            "Cannot verify remote bytes before an immutable upload"
        }
        val receipt = TrustCloudVerifiedReadback.verify(download, proposed)
        return TrustT1C4TwoPhaseState(
            previous, proposed, TrustCloudPublishStage.DOWNLOADED_AND_VERIFIED,
            receipt,
        )
    }

    fun serverCompareAndSwapAccepted(): TrustT1C4TwoPhaseState {
        check(stage == TrustCloudPublishStage.DOWNLOADED_AND_VERIFIED &&
            readbackProof != null) {
            "Server CAS cannot advance before complete authenticated readback"
        }
        return TrustT1C4TwoPhaseState(
            previous, proposed,
            TrustCloudPublishStage.HEAD_COMPARE_AND_SWAP_ACCEPTED,
            readbackProof,
        )
    }

    fun confirmServerHead(reFetchedHead: TrustCloudRevisionHead): TrustCloudRevisionProgress {
        check(stage == TrustCloudPublishStage.HEAD_COMPARE_AND_SWAP_ACCEPTED) {
            "Server head was not atomically claimed"
        }
        return TrustT1C4CloudRevisionPolicy.acknowledgeNewHead(
            previous, reFetchedHead, proposed,
            checkNotNull(readbackProof) { "Missing actual remote byte proof" },
        )
    }

    /** CAS conflict is terminal. Retain both original and orphan new object. */
    fun requireNoConcurrentAdvance(currentHead: TrustCloudRevisionHead?) {
        TrustT1C4CloudRevisionPolicy.requireUnchanged(
            previous, currentHead, proposed.ownerUserId,
        )
    }
}
