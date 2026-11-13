package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure policy tests: never read/write owner audio, records or Best Frames. */
class V373RecoveredCreatorMediaPolicyTest {
    private val base = V365VoiceCopyPlan(
        transactionId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowSha256 = "b".repeat(64),
        entries = listOf(
            V365VoiceCopyPlanEntry(
                "damaged-idea", "voice_idea_" + UUID.randomUUID() + ".m4a",
                100L, "c".repeat(64),
            ),
            V365VoiceCopyPlanEntry(
                "good-idea", "voice_idea_" + UUID.randomUUID() + ".m4a",
                200L, "d".repeat(64),
            ),
        ),
    )
    private val originalAudit = V365VoiceCopyAudit(
        transactionId = base.transactionId, planned = 2,
        matchingCopies = 1, missingCopies = 0,
        alteredOrPartialCopies = 1, unsafeCopies = 0,
    )
    private val shadow = V371VoiceShadowPlan(
        transactionId = base.transactionId,
        sourceSha256 = base.sourceSha256,
        escrowSha256 = base.escrowSha256,
        allocationSha256 = V371VoiceShadowPolicy.allocationDigest(base),
        entries = listOf(V371VoiceShadowEntry(
            ideaId = "damaged-idea",
            damagedFile = base.entries[0].fileName,
            damagedBytes = 23L,
            damagedSha256 = "e".repeat(64),
            shadowFile = "voice_idea_" + UUID.randomUUID() + ".m4a",
            expectedBytes = 100L,
            expectedSha256 = "c".repeat(64),
        )),
    )
    private val shadowAudit = V371VoiceShadowAudit(
        planned = 1, matching = 1, missing = 0, alteredOrPartial = 0, unsafe = 0,
    )
    private fun refuses(
        sourceAudit: V365VoiceCopyAudit = originalAudit,
        shadows: V371VoiceShadowPlan? = shadow,
        verifiedShadows: V371VoiceShadowAudit? = shadowAudit,
    ) = runCatching {
        V373RecoveredCreatorMediaPolicy.selectNames(
            base, sourceAudit, shadows, verifiedShadows,
        )
    }.isFailure

    @Test fun verifiedShadowReplacesOnlyDamagedIdAndRetainsGoodOriginalName() {
        val selected = V373RecoveredCreatorMediaPolicy.selectNames(
            base, originalAudit, shadow, shadowAudit,
        )
        assertEquals(2, selected.size)
        assertEquals(shadow.entries.single().shadowFile, selected["damaged-idea"])
        assertEquals(base.entries[1].fileName, selected["good-idea"])
        assertFalse(selected.containsValue(base.entries[0].fileName))
    }

    @Test fun allOriginalsIntactMayBeReusedWithNoShadow() {
        val clean = originalAudit.copy(
            matchingCopies = 2, alteredOrPartialCopies = 0,
        )
        assertEquals(
            base.entries.associate { it.ideaId to it.fileName },
            V373RecoveredCreatorMediaPolicy.selectNames(
                base, clean, shadow = null, shadowAudit = null,
            ),
        )
        assertTrue(refuses(sourceAudit = originalAudit, shadows = null,
            verifiedShadows = null))
    }

    @Test fun missingOrUnsafeOriginalAllocationOrUnverifiedShadowNeverPromotes() {
        assertTrue(refuses(sourceAudit = originalAudit.copy(
            missingCopies = 1, matchingCopies = 0,
        )))
        assertTrue(refuses(sourceAudit = originalAudit.copy(
            unsafeCopies = 1, matchingCopies = 0,
        )))
        assertTrue(refuses(verifiedShadows = shadowAudit.copy(
            matching = 0, missing = 1,
        )))
        assertTrue(refuses(verifiedShadows = shadowAudit.copy(
            matching = 0, alteredOrPartial = 1,
        )))
        assertTrue(refuses(verifiedShadows = shadowAudit.copy(
            matching = 0, unsafe = 1,
        )))
        assertTrue(refuses(verifiedShadows = null))
        assertTrue(refuses(shadows = shadow.copy(entries = listOf(
            shadow.entries.single().copy(
                shadowFile = base.entries[1].fileName,
            ),
        ))))
    }

    @Test fun mismatchedCountsOrOwnerIdentityAlwaysBlock() {
        assertTrue(refuses(sourceAudit = originalAudit.copy(
            planned = 3, matchingCopies = 2,
        )))
        assertTrue(refuses(sourceAudit = originalAudit.copy(
            transactionId = UUID.randomUUID().toString(),
        )))
        assertTrue(refuses(shadows = shadow.copy(
            transactionId = UUID.randomUUID().toString(),
        )))
    }

    @Test fun payloadProofNeverGrantsImportOrReleasePermission() {
        val payload = V373RecoveredCreatorPayload(
            transactionId = base.transactionId,
            rewrittenCreatorJson = """{"synthetic":true}""",
            restoredVoiceIdeaCount = 2,
            shadowVoiceIdeaCount = 1,
            verifiedPortableFrameCount = null,
        )
        assertFalse(payload.mayImportCreator)
        assertFalse(payload.mayAdvanceCombinedCheckpoint)
        assertFalse(payload.mayDeleteOriginalAudio)
        assertFalse(payload.mayReleaseRecovery)
    }
}
