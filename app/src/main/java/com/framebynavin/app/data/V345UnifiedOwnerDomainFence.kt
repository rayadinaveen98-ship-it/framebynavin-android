package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.withTimeout

/**
 * A consistent gate-acquisition order for Creator + Personal.
 *
 * Personal FIRST, Creator SECOND. Creator's transactional coroutine owns its
 * own short-lived Job; acquiring Personal inside it would change that Job and
 * invalidate CreatorDataGate's legitimate nested transaction ownership check.
 *
 * An absent cross-domain journal is mandatory on BOTH sides of acquisition.
 * Existing per-domain restore journals are also checked, preventing a
 * combined plan from racing an unrelated Creator/Personal restore.
 *
 * IMPORTANT: this is NOT a combined recovery write permit. It cannot bypass
 * either store's fail-closed recovery journal guard. Once V344 writes any
 * PREPARED marker, ordinary Creator/Personal writes are blocked. An eventual
 * executor must use a separate, journal-bound permit with owner-reviewed
 * resume/rollback semantics; do not remove or weaken those guards here.
 */
internal class V345UnifiedOwnerDomainFence(context: Context) {
    private val app = context.applicationContext
    private val combined = V344UnifiedRestoreJournal(app)

    /**
     * Holds BOTH existing in-process mutexes while performing a non-mutating
     * last-moment check. The supplied action cannot suspend: it must not open
     * child coroutines or accidentally release ownership to a different Job.
     *
     * It does NOT invoke a restore, create a cross-domain marker, or write data.
     * Android file reads are allowed; no database/scheduler writers may be
     * installed in this callback.
     */
    suspend fun <T> withExclusiveReadCheck(readOnlyCheck: () -> T): T =
        withTimeout(5_000L) {
            // Fail closed rather than wait forever if a legacy workflow has
            // the reverse lock order or an editor is still finishing a write.
            StabilizationPersonalMutationGate.withStableSnapshot(app) {
                CreatorDataGate.transaction {
                    requireNoOtherRecovery()
                    val answer = readOnlyCheck()
                    requireNoOtherRecovery()
                    answer
                }
            }
        }

    /**
     * H3 internal ONLY: hold the two independent owner mutexes through the
     * last-moment verified ORIGINAL fingerprint and creation of PREPARED
     * cross-domain journal. The supplied action must write ONLY the durable
     * H4 checkpoint; it may not invoke Creator/Personal restore writers.
     *
     * After the checkpoint appears, ordinary writers are deliberately
     * frozen on both stores. Do NOT expose this as a public "Start Restore"
     * entry before owner-approved forward/rollback is implemented.
     */
    internal suspend fun <T> withExclusiveJournalReservation(
        reserveJournalOnly: suspend () -> T,
    ): T = withTimeout(45_000L) {
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            CreatorDataGate.transaction {
                requireNoOtherRecovery()
                reserveJournalOnly()
            }
        }
    }

    private fun requireNoOtherRecovery() {
        check(!combined.hasBlockingEvidence()) {
            "Cross-domain recovery already exists: owner review required"
        }
        check(!IdeaProjectConversion(app).hasPendingRecovery()) {
            "An interrupted Creator idea-to-project conversion needs its own recovery"
        }
        val pendingCreator = File(
            app.filesDir, "creator_restore_recovery_v181/pending.json"
        )
        check(!Files.exists(pendingCreator.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Independent Creator recovery must finish before a combined transaction"
        }
        val pendingPersonal = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!Files.exists(pendingPersonal.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Independent Personal recovery must finish before a combined transaction"
        }
        // Personal v2 has a DIFFERENT owner journal than the legacy marker.
        val pendingV2 = com.framebynavin.app.personal.Phase1V2RecoveryRoots
            .alongsideLegacy(pendingPersonal)
        check(!Files.exists(pendingV2.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Independent v2 Personal recovery must finish before a combined transaction"
        }
    }
}
