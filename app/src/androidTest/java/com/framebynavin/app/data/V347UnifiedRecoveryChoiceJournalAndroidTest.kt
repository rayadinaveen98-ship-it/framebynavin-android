package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Entirely synthetic: makes its OWN cross-domain checkpoint folder and OWN
 * decision folder inside app-private test files. No real Creator/Personal
 * records, owner archive files, photos, alarms or App recovery gates touched.
 */
@RunWith(AndroidJUnit4::class)
class V347UnifiedRecoveryChoiceJournalAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun checkpoint() = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = V344UnifiedRestoreStep.PREPARED_NO_WRITES,
        createdAtMillis = 1_800_000_000_000L,
        updatedAtMillis = 1_800_000_000_000L,
    )

    private fun proof(j: V344UnifiedRestoreCheckpoint) =
        V346InterruptedRecoveryProof(
            journal = j,
            sealedArchives = V345UnifiedAuthenticatedPairProof(
                sourceId = j.sourceId, sourceSha256 = j.sourceSha256,
                originalEscrowId = j.escrowId, originalEscrowSha256 = j.escrowSha256,
                targetCreatorSha256 = "e".repeat(64),
                targetPersonalSha256 = "f".repeat(64),
                originalCreatorSha256 = "a".repeat(64),
                originalPersonalSha256 = "b".repeat(64),
                targetCreatorProjects = 1,
                originalCreatorProjects = 2,
                targetPersonalSummary = "synthetic", originalPersonalSummary = "synthetic",
            ),
            recoveryKind = V344UnifiedRestorePolicy.classify(j.step),
            creatorStandaloneRecoveryMarker = false,
            personalStandaloneRecoveryMarker = false,
        )

    @Test fun codecRejectsAnyOwnerChoiceEditOrUnknownField() {
        val j = checkpoint()
        val decision = V347OwnerRecoveryDecisionPolicy.propose(
            proof(j), j, V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL,
            ownerConfirmedChoice = true,
            nowMillis = j.updatedAtMillis + 1,
            decisionId = UUID.randomUUID().toString(),
        )
        val encoded = V347OwnerRecoveryDecisionCodec.encode(decision)
        assertEquals(decision, V347OwnerRecoveryDecisionCodec.decode(encoded))
        assertFalse(decision.mayExecute)
        assertFalse(decision.mayAutoResume)
        assertFalse(decision.mayReleaseOriginal)

        val altered = JSONObject(String(encoded)).put(
            "choice", V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET.name)
        assertThrows(Exception::class.java) {
            V347OwnerRecoveryDecisionCodec.decode(altered.toString().toByteArray())
        }
        val injected = JSONObject(String(encoded)).put("approveWrites", true)
        assertThrows(Exception::class.java) {
            V347OwnerRecoveryDecisionCodec.decode(injected.toString().toByteArray())
        }
    }

    @Test fun staleStepOrMissingOwnerConsentCannotProduceARecoveryChoice() {
        val j = checkpoint()
        val p = proof(j)
        assertThrows(IllegalArgumentException::class.java) {
            V347OwnerRecoveryDecisionPolicy.propose(
                p, j, V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
                ownerConfirmedChoice = false,
                nowMillis = j.updatedAtMillis + 1,
                decisionId = UUID.randomUUID().toString(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            V347OwnerRecoveryDecisionPolicy.propose(
                p, j.copy(step = V344UnifiedRestoreStep.BEFORE_CREATOR),
                V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
                ownerConfirmedChoice = true,
                nowMillis = j.updatedAtMillis + 1,
                decisionId = UUID.randomUUID().toString(),
            )
        }
    }

    @Test fun durableChoiceSurvivesRestartButCannotBeReplacedOrReplayed() = runBlocking {
        val token = UUID.randomUUID().toString()
        val journalRoot = File(app.filesDir, "v347-journal-$token")
        val decisionRoot = File(app.filesDir, "v347-choice-$token")
        try {
            val journal = V344UnifiedRestoreJournal(app, journalRoot)
            val committed = journal.prepare(checkpoint(), ownerApproved = true)
            val store = V347OwnerRecoveryDecisionJournal(app, decisionRoot, journal)
            assertEquals(V347OwnerRecoveryDecisionJournal.ReadState.None,
                store.readOnlyStatus())
            val consent = store.recordChoice(
                proof(committed),
                V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
                ownerConfirmedChoice = true,
            )
            assertFalse(consent.mayExecute)
            val reopened = V347OwnerRecoveryDecisionJournal(
                app, decisionRoot, V344UnifiedRestoreJournal(app, journalRoot))
            assertEquals(V347OwnerRecoveryDecisionJournal.ReadState.Recorded(consent),
                reopened.readOnlyStatus())
            assertThrows(Exception::class.java) {
                runBlocking {
                    reopened.recordChoice(
                        proof(committed),
                        V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL,
                        ownerConfirmedChoice = true,
                    )
                }
            }
            assertEquals(V347OwnerRecoveryDecisionJournal.ReadState.Recorded(consent),
                reopened.readOnlyStatus())
            // Advance synthetic forward journal: the previous consent is
            // now explicitly stale; it cannot be reused for the next stage.
            journal.checkpoint(committed, V344UnifiedRestoreStep.BEFORE_CREATOR,
                committed.updatedAtMillis + 1)
            assertEquals(V347OwnerRecoveryDecisionJournal.ReadState.NeedsOwnerReview,
                reopened.readOnlyStatus())
        } finally {
            journalRoot.deleteRecursively()
            decisionRoot.deleteRecursively()
        }
    }

    @Test fun tornAtomicDecisionIsPreservedRatherThanAutoRecovered() = runBlocking {
        val token = UUID.randomUUID().toString()
        val root = File(app.filesDir, "v347-choice-$token")
        try {
            assertTrue(root.mkdir())
            File(root, "owner-choice.json.bak").writeText("partial")
            val store = V347OwnerRecoveryDecisionJournal(app, root,
                V344UnifiedRestoreJournal(app,
                    File(app.filesDir, "v347-journal-$token")))
            assertEquals(V347OwnerRecoveryDecisionJournal.ReadState.NeedsOwnerReview,
                store.readOnlyStatus())
            assertTrue(File(root, "owner-choice.json.bak").isFile)
        } finally { root.deleteRecursively() }
    }
}
