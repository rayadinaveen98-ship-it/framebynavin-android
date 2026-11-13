package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure policy regression cases; no app files, archives or owner data touched. */
class V366CreatorVoiceRecoveryPolicyTest {
    private val one = V365VoiceCopyPlanEntry(
        ideaId = "first",
        fileName = "voice_idea_00000000-0000-0000-0000-000000000001.m4a",
        sizeBytes = 120L,
        sha256 = "a".repeat(64),
    )
    private val two = V365VoiceCopyPlanEntry(
        ideaId = "second",
        fileName = "voice_idea_00000000-0000-0000-0000-000000000002.m4a",
        sizeBytes = 240L,
        sha256 = "b".repeat(64),
    )
    private val plan = V365VoiceCopyPlan(
        transactionId = "00000000-0000-0000-0000-000000000003",
        sourceSha256 = "c".repeat(64),
        escrowSha256 = "d".repeat(64),
        entries = listOf(one, two),
    )
    private val authenticated = mapOf(
        "first" to V366VoiceBytes(120L, "a".repeat(64)),
        "second" to V366VoiceBytes(240L, "b".repeat(64)),
    )

    @Test fun exactSourceIdsBytesAndHashesMatchRegardlessOfStagingOrder() {
        assertTrue(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan, authenticated.toList().reversed().toMap(),
        ))
    }

    @Test fun missingExtraAndChangedMediaFailClosed() {
        assertFalse(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan, authenticated - "second",
        ))
        assertFalse(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan, authenticated + ("third" to V366VoiceBytes(20, "e".repeat(64))),
        ))
        assertFalse(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan, authenticated + ("second" to V366VoiceBytes(241, "b".repeat(64))),
        ))
        assertFalse(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan, authenticated + ("second" to V366VoiceBytes(240, "f".repeat(64))),
        ))
    }

    @Test fun duplicateAllocationAndMalformedChecksumAreNeverAccepted() {
        assertFalse(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan.copy(entries = listOf(one, one)), authenticated,
        ))
        assertFalse(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan.copy(entries = listOf(one.copy(sha256 = "not-a-hash"), two)),
            authenticated,
        ))
    }

    @Test fun zeroVoiceBackupsAreValidButGrantNoRestoreAuthority() {
        assertTrue(V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan.copy(entries = emptyList()), emptyMap(),
        ))
        val audit = V365VoiceCopyAudit(
            transactionId = plan.transactionId,
            planned = 0,
            matchingCopies = 0,
            missingCopies = 0,
            alteredOrPartialCopies = 0,
            unsafeCopies = 0,
        )
        val report = V366CreatorVoiceRecoveryProof(
            transactionId = plan.transactionId,
            targetCreatorSha256 = "e".repeat(64),
            originalCreatorSha256 = "f".repeat(64),
            targetMediaCount = 0,
            originalMediaCount = 0,
            installedCopies = audit,
            originalMediaAuthenticated = true,
            targetAllocationAuthenticated = true,
        )
        assertEquals(0, report.targetMediaCount)
        assertFalse(report.mayReplayCreator)
        assertFalse(report.mayDeleteMedia)
        assertFalse(report.mayReleaseOriginalEscrow)
    }
}
