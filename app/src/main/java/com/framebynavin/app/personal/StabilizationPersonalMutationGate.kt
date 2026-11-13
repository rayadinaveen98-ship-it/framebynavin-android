package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * First production-facing phase of the unfinished Personal recovery lock.
 *
 * Personal/Done, Challenge and Group writers plus V301 read-only recovery
 * snapshots use a single process-wide mutex. A persistent journal, even
 * PREPARED/corrupt, stops all ordinary writes after a process restart.
 *
 * This is NOT a full-app freeze until Room Space and JPEG writers are gated.
 * No code here permits a live restore, deletes an escrow or claims success.
 *
 * A short-lived scoped coroutine permit allows nested same-root Space/photo
 * writes without deadlock; unrelated concurrent mutations still use the lock.
 */
internal object StabilizationPersonalMutationGate {
    private const val ACTIVE = "backlot-personal-active-restore-transaction"
    private val processMutex = Mutex()

    private class Permit(val root: String) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<Permit>
    }

    private class RecoveryPermit(
        val root: String,
        val transactionId: String,
        val originalDigest: String,
        val targetDigest: String,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<RecoveryPermit>
    }

    /**
     * Isolated v2 write authority. This cannot be substituted for the old
     * v1 RecoveryPermit because the durable journal format and target SHA
     * cover a DIFFERENT set of stores (including exact 21-day Done receipts).
     */
    private class V2RecoveryWritePermit(
        val legacyRoot: String,
        val transactionId: String,
        val originalDigest: String,
        val targetDigest: String,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<V2RecoveryWritePermit>
    }

    /**
     * May be entered only after a distinct v2 journal was durably prepared
     * under the existing owner's EXCLUSIVE normal write mutex. Every domain
     * writer must inherit the short-lived matching permit.
     */
    internal suspend fun <T> withPreparedV2OwnerRecovery(
        legacyRoot: File,
        expected: Phase1V2RestoreCheckpoint,
        action: suspend () -> T,
    ): T {
        val path = legacyRoot.absoluteFile.normalize().path
        val owner = currentCoroutineContext()[Permit]
        check(owner != null && owner.active && owner.root == path &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null) {
            "Initial v2 write requires the exclusive owner mutation lock"
        }
        require(expected.step == Phase1V2RestoreStep.PREPARED) {
            "Initial v2 write must begin at PREPARED"
        }
        check(Phase1V2RestoreWriteAheadJournal(
            Phase1V2RecoveryRoots.alongsideLegacy(legacyRoot)
        ).inspectAfterRestart() == expected) {
            "Prepared v2 journal is not the authenticated owner request"
        }
        val permit = V2RecoveryWritePermit(
            path, expected.transactionId, expected.originalDigest,
            expected.targetDigest,
        )
        return try { withContext(permit) { action() } }
        finally { permit.active = false }
    }

    /**
     * Cold-restart continuation requires a fresh, exclusively locked journal
     * inspection AND independent authenticated original/target/byte evidence.
     * Consent is never inherited from the failed transaction or an alarm.
     */
    internal suspend fun <T> withApprovedV2ForwardRecovery(
        legacyRoot: File,
        expected: Phase1V2RestoreCheckpoint,
        verified: Phase1V2InterruptedRecoveryEvidence,
        ownerApproved: Boolean,
        action: suspend () -> T,
    ): T {
        require(ownerApproved) { "Only the owner may resume an interrupted restore" }
        val path = legacyRoot.absoluteFile.normalize().path
        val inspection = currentCoroutineContext()[V2InspectionPermit]
        check(inspection != null && inspection.active && inspection.root == path &&
            inspection.transactionId == expected.transactionId &&
            inspection.originalDigest == expected.originalDigest &&
            inspection.targetDigest == expected.targetDigest &&
            inspection.step == expected.step &&
            inspection.updatedAtMillis == expected.updatedAtMillis &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null) {
            "Stale or nonexclusive v2 owner recovery approval"
        }
        require(verified.checkpoint == expected &&
            verified.domains.checkpoint == expected.step &&
            verified.photos.checkpoint == expected.step &&
            !verified.mayAutoResume && !verified.mayReleaseJournal) {
            "Unauthenticated or mismatched v2 interrupted recovery evidence"
        }
        val journal = Phase1V2RestoreWriteAheadJournal(
            Phase1V2RecoveryRoots.alongsideLegacy(legacyRoot),
        )
        check(journal.inspectAfterRestart() == expected) {
            "Owner v2 checkpoint changed during recovery approval"
        }
        val permit = V2RecoveryWritePermit(
            path, expected.transactionId, expected.originalDigest,
            expected.targetDigest,
        )
        return try { withContext(permit) { action() } }
        finally { permit.active = false }
    }

    /**
     * A recovery writer may ONLY be opened by a coroutine already holding
     * the ordinary mutation lock, AFTER it has durably prepared a matching
     * journal. Other coroutines and all regular UI writers remain frozen.
     * An expired permit cannot escape into child tasks after this scope.
     */
    internal suspend fun <T> withPreparedRecovery(
        journalRoot: File,
        expected: StabilizationRestoreJournalRecord,
        action: suspend () -> T,
    ): T {
        val path = journalRoot.absoluteFile.normalize().path
        val owner = currentCoroutineContext()[Permit]
        check(owner != null && owner.active && owner.root == path) {
            "Recovery requires the exclusive existing owner mutation lock"
        }
        check(currentCoroutineContext()[RecoveryPermit] == null) {
            "Recovery authority cannot be nested or borrowed"
        }
        require(expected.step == StabilizationRestoreWriteStep.PREPARED) {
            "Recovery cannot start from an unreviewed interrupted journal"
        }
        ensureMatchingRecoveryJournal(journalRoot, expected)
        val granted = RecoveryPermit(
            path, expected.transactionId, expected.originalDigest, expected.targetDigest,
        )
        return try { withContext(granted) { action() } }
        finally { granted.active = false }
    }

    /**
     * Upgrade the existing cold-restart inspection lock to one SHORT-LIVED
     * journal-scoped recovery write permit. No second process lock is taken.
     *
     * This must only be called after the coordinator reauthenticates BOTH ZIP
     * archives, matches baseline/target hashes, verifies live per-store
     * chronology and private photo forensics, and obtains owner consent.
     * Recovery writers still require this exact scoped permit.
     */
    internal suspend fun <T> withOwnerApprovedForwardRecovery(
        root: File,
        expected: StabilizationRestoreJournalRecord,
        decision: B04OwnerRecoveryDecision,
        plan: B04ForwardReplayPlan,
        action: suspend () -> T,
    ): T {
        val inspection = checkNotNull(currentCoroutineContext()[InspectionPermit]) {
            "Forward recovery requires the exclusive owner inspection lock"
        }
        val path = root.absoluteFile.normalize().path
        check(inspection.active && inspection.root == path &&
            inspection.transactionId == expected.transactionId &&
            inspection.originalDigest == expected.originalDigest &&
            inspection.targetDigest == expected.targetDigest &&
            inspection.step == expected.step &&
            inspection.updatedAtMillis == expected.updatedAtMillis) {
            "Recovery approval is stale or scoped to another transaction"
        }
        check(StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart() == expected) {
            "Active recovery journal changed after owner consent"
        }
        require(
            decision.selectedChoice == B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET &&
            decision.transactionId == expected.transactionId &&
            decision.escrowId == expected.escrowId &&
            decision.originalDigest == expected.originalDigest &&
            decision.targetDigest == expected.targetDigest &&
            plan.transactionId == expected.transactionId &&
            plan.checkpoint == expected.step &&
            plan.originalDigest == expected.originalDigest &&
            plan.targetDigest == expected.targetDigest &&
            !plan.grantsMutationAuthority && !plan.mayRunWithoutOwnerConsent
        ) { "Unverified or stale forward replay decision" }
        val permit = RecoveryPermit(
            path, expected.transactionId, expected.originalDigest, expected.targetDigest,
        )
        // Kotlin withContext merges with the parent context, so a caller
        // cannot remove the inherited inspection permit by minusKey alone.
        // holdGate explicitly prioritizes THIS validated RecoveryPermit;
        // after the permit expires, its inspection scope becomes read-only.
        return try { withContext(permit) { action() } }
        finally { permit.active = false }
    }

    /**
     * Permit precisely one owner-authenticated, journaled ROLLBACK under the
     * existing read-only inspection mutex. The action journal tracks a reverse
     * write sequence; the forward blocking journal must not be altered here.
     */
    internal suspend fun <T> withOwnerApprovedRollbackRecovery(
        activeRoot: File,
        forward: StabilizationRestoreJournalRecord,
        decision: B04OwnerRecoveryDecision,
        delta: B04NoPhotoRollbackDelta,
        actionJournalRoot: File,
        rollback: B04RollbackJournalRecord,
        action: suspend () -> T,
    ): T {
        val path = activeRoot.absoluteFile.normalize().path
        val inspection = checkNotNull(currentCoroutineContext()[InspectionPermit]) {
            "Rollback requires a separate exclusive owner recovery inspection"
        }
        check(inspection.active && inspection.root == path &&
            inspection.transactionId == forward.transactionId &&
            inspection.originalDigest == forward.originalDigest &&
            inspection.targetDigest == forward.targetDigest &&
            inspection.step == forward.step &&
            inspection.updatedAtMillis == forward.updatedAtMillis) {
            "Owner rollback inspection is stale or bound to a different transaction"
        }
        check(currentCoroutineContext()[RecoveryPermit] == null) {
            "Rollback authority cannot be borrowed from another restore"
        }
        check(StabilizationRestoreWriteAheadJournal(activeRoot).inspectAfterRestart() == forward &&
            B04NoPhotoRollbackActionJournal(actionJournalRoot).inspect() == rollback) {
            "Forward or reverse journal changed since the owner's approval"
        }
        require(decision.selectedChoice ==
            B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW &&
            decision.transactionId == forward.transactionId &&
            decision.escrowId == forward.escrowId &&
            decision.originalDigest == forward.originalDigest &&
            decision.targetDigest == forward.targetDigest &&
            delta.transactionId == forward.transactionId &&
            delta.originalDigest == forward.originalDigest &&
            delta.targetDigest == forward.targetDigest &&
            !delta.grantsWriteAuthority && !delta.mayRemoveOriginalData &&
            !delta.mayDeletePrivatePhotos &&
            rollback.transactionId == forward.transactionId &&
            rollback.escrowId == forward.escrowId &&
            rollback.forwardStepAtApproval == forward.step &&
            rollback.originalDigest == forward.originalDigest &&
            rollback.targetDigest == forward.targetDigest) {
            "Rollback decision or validated imported delta does not match the journal"
        }
        val permit = RecoveryPermit(
            path, forward.transactionId, forward.originalDigest, forward.targetDigest,
        )
        return try { withContext(permit) { action() } }
        finally { permit.active = false }
    }

    /** Reject any direct restore domain call outside the journaled write scope. */
    internal suspend fun requireActiveRecovery(context: Context) {
        val root = journalRootFor(context)
        val path = root.absoluteFile.normalize().path
        val v2 = currentCoroutineContext()[V2RecoveryWritePermit]
        if (v2 != null) {
            check(v2.active && v2.legacyRoot == path) {
                "V2 recovery writer is expired or scoped to another phone"
            }
            ensureMatchingV2RecoveryJournal(root, v2)
            return
        }
        val permit = currentCoroutineContext()[RecoveryPermit]
        check(permit != null && permit.active && permit.root == path) {
            "Only the exclusive authenticated restore coordinator may write recovery data"
        }
        ensureMatchingRecoveryJournal(root, permit)
    }

    /**
     * V2 cold-restart inspection is intentionally READ-ONLY. It never reuses
     * the v1 persisted journal enum or grants a writer permission.
     */
    private class V2InspectionPermit(
        val root: String,
        val transactionId: String,
        val originalDigest: String,
        val targetDigest: String,
        val step: Phase1V2RestoreStep,
        val updatedAtMillis: Long,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<V2InspectionPermit>
    }

    private class InspectionPermit(
        val root: String,
        val transactionId: String,
        val originalDigest: String,
        val targetDigest: String,
        val step: StabilizationRestoreWriteStep,
        val updatedAtMillis: Long,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<InspectionPermit>
    }

    /**
     * Owner-requested COLD RESTART inspection. Reads may run despite the
     * journal, but ordinary mutation calls are unconditionally rejected.
     * Only a fully decoded journal may enter. No rollback/resume authority.
     */
    /**
     * A real v2 interrupted restore can be inspected without mistaking an
     * incomplete new challenge DataStore for empty. The caller is obligated
     * to authenticate the escrow and target before any later repair decision.
     */
    internal suspend fun <T> withPendingV2RecoveryInspection(
        legacyJournalRoot: File,
        action: suspend (Phase1V2RestoreCheckpoint) -> T,
    ): T {
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null) {
            "V2 recovery inspection cannot inherit a write or inspection permit"
        }
        return processMutex.withLock {
            check(!Files.exists(legacyJournalRoot.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "V1 and v2 recovery journals must never coexist"
            }
            val root = Phase1V2RecoveryRoots.alongsideLegacy(legacyJournalRoot)
            val current = checkNotNull(
                Phase1V2RestoreWriteAheadJournal(root).inspectAfterRestart()
            ) { "No pending v2 owner recovery transaction" }
            val permit = V2InspectionPermit(
                legacyJournalRoot.absoluteFile.normalize().path,
                current.transactionId, current.originalDigest,
                current.targetDigest, current.step, current.updatedAtMillis,
            )
            try { withContext(permit) { action(current) } }
            finally { permit.active = false }
        }
    }

    internal suspend fun <T> withPendingV2RecoveryInspection(
        context: Context, action: suspend (Phase1V2RestoreCheckpoint) -> T,
    ): T = withPendingV2RecoveryInspection(journalRootFor(context), action)

    internal suspend fun <T> withPendingRecoveryInspection(
        journalRoot: File,
        action: suspend (StabilizationRestoreJournalRecord) -> T,
    ): T {
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null) {
            "Nested recovery inspection or mutation lock is not permitted"
        }
        return processMutex.withLock {
            ensureNoCompetingV2Journal(journalRoot)
            val record = checkNotNull(
                StabilizationRestoreWriteAheadJournal(journalRoot).inspectAfterRestart()
            ) { "No pending recovery transaction" }
            val permit = InspectionPermit(
                journalRoot.absoluteFile.normalize().path,
                record.transactionId, record.originalDigest, record.targetDigest,
                record.step, record.updatedAtMillis,
            )
            try { withContext(permit) { action(record) } }
            finally { permit.active = false }
        }
    }

    internal suspend fun <T> withPendingRecoveryInspection(
        context: Context,
        action: suspend (StabilizationRestoreJournalRecord) -> T,
    ): T = withPendingRecoveryInspection(journalRootFor(context), action)

    internal fun journalRootFor(context: Context): File =
        File(context.applicationContext.filesDir, ACTIVE)

    suspend fun <T> withOwnerMutation(
        context: Context, action: suspend () -> T,
    ): T = withOwnerMutation(journalRootFor(context), action)

    internal suspend fun <T> withOwnerMutation(
        journalRoot: File, action: suspend () -> T,
    ): T = holdGate(journalRoot, true, action)

    suspend fun <T> withStableSnapshot(
        context: Context, action: suspend () -> T,
    ): T = withStableSnapshot(journalRootFor(context), action)

    internal suspend fun <T> withStableSnapshot(
        journalRoot: File, action: suspend () -> T,
    ): T = holdGate(journalRoot, false, action)

    private suspend fun <T> holdGate(
        root: File, ordinaryMutation: Boolean, action: suspend () -> T,
    ): T {
        val path = root.absoluteFile.normalize().path
        // The combined journal takes precedence over all standalone v1/v2
        // permits: none may change half-restored Creator+Personal owner data.
        requireNoCombinedRestoreJournal(root)
        val v2Recovery = currentCoroutineContext()[V2RecoveryWritePermit]
        if (v2Recovery != null) {
            check(v2Recovery.active && v2Recovery.legacyRoot == path) {
                "Expired or unrelated v2 owner recovery authority"
            }
            ensureMatchingV2RecoveryJournal(root, v2Recovery)
            return action()
        }
        val recovery = currentCoroutineContext()[RecoveryPermit]
        if (recovery != null) {
            check(recovery.active && recovery.root == path) {
                "Expired or unrelated restore authority"
            }
            ensureMatchingRecoveryJournal(root, recovery)
            return action()
        }
        val v2Inspection = currentCoroutineContext()[V2InspectionPermit]
        if (v2Inspection != null) {
            check(!ordinaryMutation && v2Inspection.active &&
                v2Inspection.root == path) {
                "Interrupted v2 inspection cannot mutate owner Personal data"
            }
            check(!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "V1 recovery journal appeared during v2 inspection"
            }
            val staged = checkNotNull(
                Phase1V2RestoreWriteAheadJournal(
                    Phase1V2RecoveryRoots.alongsideLegacy(root)
                ).inspectAfterRestart()
            ) { "V2 recovery evidence disappeared during owner inspection" }
            check(staged.transactionId == v2Inspection.transactionId &&
                staged.originalDigest == v2Inspection.originalDigest &&
                staged.targetDigest == v2Inspection.targetDigest &&
                staged.step == v2Inspection.step &&
                staged.updatedAtMillis == v2Inspection.updatedAtMillis) {
                "V2 recovery journal changed during immutable inspection"
            }
            return action()
        }
        val inspection = currentCoroutineContext()[InspectionPermit]
        if (inspection != null) {
            check(!ordinaryMutation && inspection.active && inspection.root == path) {
                "Recovery inspection is read-only and cannot mutate owner data"
            }
            ensureNoCompetingV2Journal(root)
            val current = checkNotNull(
                StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
            ) { "Pending recovery journal was removed during inspection" }
            check(current.transactionId == inspection.transactionId &&
                current.originalDigest == inspection.originalDigest &&
                current.targetDigest == inspection.targetDigest &&
                current.step == inspection.step &&
                current.updatedAtMillis == inspection.updatedAtMillis) {
                "Recovery journal changed while being inspected"
            }
            return action()
        }
        val inherited = currentCoroutineContext()[Permit]
        if (inherited != null) {
            check(inherited.active && inherited.root == path) {
                "Expired or unrelated recovery mutation permit"
            }
            ensureNoRecoveryJournal(root)
            return action()
        }
        return processMutex.withLock {
            ensureNoRecoveryJournal(root)
            val permit = Permit(path)
            try { withContext(permit) { action() } }
            finally { permit.active = false }
        }
    }

    private fun requireNoCombinedRestoreJournal(personalJournalRoot: File) {
        val appFiles = requireNotNull(personalJournalRoot.absoluteFile.parentFile) {
            "Personal recovery root has no app-private parent"
        }
        val combined = File(
            appFiles,
            com.framebynavin.app.data.V344UnifiedRestoreJournal.ROOT_NAME,
        )
        check(!Files.exists(combined.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Combined Creator and Personal recovery needs owner review. " +
                "Normal Personal writes and snapshots are paused."
        }
    }

    /**
     * Extra fail-closed check for legacy synchronous private-file helpers.
     * This does NOT replace the coroutine mutex: their normal owner-facing
     * callers must already hold withOwnerMutation around the full operation.
     */
    internal fun requireNoPendingRecovery(context: Context) {
        val root = journalRootFor(context)
        // Synchronous JPEG finalize/delete helpers must respect the SAME
        // combined recovery marker as the asynchronous Room/data stores.
        requireNoCombinedRestoreJournal(root)
        ensureNoRecoveryJournal(root)
    }

    private fun ensureMatchingRecoveryJournal(
        root: File, expected: StabilizationRestoreJournalRecord,
    ) {
        ensureNoCompetingV2Journal(root)
        val current = checkNotNull(
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        ) { "Recovery transaction journal is missing" }
        check(
            current.transactionId == expected.transactionId &&
                current.preparedId == expected.preparedId &&
                current.escrowId == expected.escrowId &&
                current.originalDigest == expected.originalDigest &&
                current.targetDigest == expected.targetDigest
        ) { "Recovery journal identity or fingerprints changed" }
    }

    private fun ensureMatchingRecoveryJournal(root: File, permit: RecoveryPermit) {
        ensureNoCompetingV2Journal(root)
        val current = checkNotNull(
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        ) { "Recovery transaction journal disappeared" }
        check(current.transactionId == permit.transactionId &&
            current.originalDigest == permit.originalDigest &&
            current.targetDigest == permit.targetDigest) {
            "Recovery authority does not match current durable transaction"
        }
    }

    private fun ensureMatchingV2RecoveryJournal(
        legacyRoot: File,
        permit: V2RecoveryWritePermit,
    ) {
        check(!Files.exists(legacyRoot.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Legacy recovery started during the separate v2 transaction"
        }
        val current = checkNotNull(Phase1V2RestoreWriteAheadJournal(
            Phase1V2RecoveryRoots.alongsideLegacy(legacyRoot),
        ).inspectAfterRestart()) {
            "V2 recovery journal disappeared while owner data was changing"
        }
        check(current.transactionId == permit.transactionId &&
            current.originalDigest == permit.originalDigest &&
            current.targetDigest == permit.targetDigest) {
            "V2 recovery authority no longer matches durable journal"
        }
    }

    private fun ensureNoCompetingV2Journal(legacyRoot: File) {
        check(!Files.exists(
            Phase1V2RecoveryRoots.alongsideLegacy(legacyRoot).toPath(),
            LinkOption.NOFOLLOW_LINKS,
        )) {
            "A v2 Personal recovery journal exists. Owner writes remain frozen."
        }
    }

    private fun ensureNoRecoveryJournal(journalRoot: File) {
        ensureNoCompetingV2Journal(journalRoot)
        check(!Files.exists(journalRoot.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Personal recovery is pending. Changes are paused until the owner " +
                "reviews the encrypted recovery copy and transaction journal."
        }
    }
}
