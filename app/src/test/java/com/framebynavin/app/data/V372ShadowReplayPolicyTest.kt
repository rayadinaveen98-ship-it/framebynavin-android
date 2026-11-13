package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure no-I/O policy regression; no real Creator, Personal or media writes. */
class V372ShadowReplayPolicyTest {
    private val original = V365VoiceCopyAudit(
        transactionId = "00000000-0000-0000-0000-000000000005",
        planned = 3,
        matchingCopies = 1,
        missingCopies = 0,
        alteredOrPartialCopies = 2,
        unsafeCopies = 0,
    )

    @Test fun supportsPartialsOnlyWhenAllOtherOriginalAllocatedFilesMatch() {
        assertTrue(V372ShadowReplayPolicy.eligibleOriginalAllocation(
            original, confirmedOriginalState = true,
        ))
        assertFalse(V372ShadowReplayPolicy.eligibleOriginalAllocation(
            original, confirmedOriginalState = false,
        ))
        assertFalse(V372ShadowReplayPolicy.eligibleOriginalAllocation(
            original.copy(alteredOrPartialCopies = 0, matchingCopies = 3),
            confirmedOriginalState = true,
        ))
        assertFalse(V372ShadowReplayPolicy.eligibleOriginalAllocation(
            original.copy(missingCopies = 1, matchingCopies = 0),
            confirmedOriginalState = true,
        ))
        assertFalse(V372ShadowReplayPolicy.eligibleOriginalAllocation(
            original.copy(unsafeCopies = 1, matchingCopies = 0),
            confirmedOriginalState = true,
        ))
    }

    @Test fun noShadowOverwriteWhenExistingFileIsPartialOrUnsafe() {
        assertTrue(V372ShadowReplayPolicy.eligiblePreCopy(
            V371VoiceShadowAudit(2, matching = 0, missing = 2,
                alteredOrPartial = 0, unsafe = 0),
        ))
        assertTrue(V372ShadowReplayPolicy.eligiblePreCopy(
            V371VoiceShadowAudit(2, matching = 2, missing = 0,
                alteredOrPartial = 0, unsafe = 0),
        ))
        assertFalse(V372ShadowReplayPolicy.eligiblePreCopy(
            V371VoiceShadowAudit(2, matching = 1, missing = 0,
                alteredOrPartial = 1, unsafe = 0),
        ))
        assertFalse(V372ShadowReplayPolicy.eligiblePreCopy(
            V371VoiceShadowAudit(2, matching = 1, missing = 0,
                alteredOrPartial = 0, unsafe = 1),
        ))
    }

    @Test fun successfulShadowReceiptDoesNotPromoteCreatorOrCombinedRestore() {
        val receipt = V372ShadowReplayReceipt(
            transactionId = original.transactionId,
            damagedOriginalsRetained = 2,
            shadowsVerified = 2,
            newShadowsCopied = 1,
        )
        assertEquals(1, receipt.newShadowsCopied)
        assertTrue(receipt.combinedRecoveryPending)
        assertFalse(receipt.mayImportCreator)
        assertFalse(receipt.mayDeletePartialAudio)
        assertFalse(receipt.mayAdvanceH4)
        assertFalse(receipt.mayRearmAlarms)
        assertFalse(receipt.mayReleaseOriginalEscrow)
    }
}
