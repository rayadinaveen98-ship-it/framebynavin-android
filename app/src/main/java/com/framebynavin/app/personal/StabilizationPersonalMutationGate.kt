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
        val inverse = currentCoroutineContext()[CombinedPersonalOriginalReversePermit]
        if (inverse != null) {
            check(inverse.active &&
                inverse.expected.step in setOf(
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_PERSONAL,
                    com.framebynavin.app.data.V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                    com.framebynavin.app.data.V344UnifiedRestoreStep.VERIFYING_BOTH,
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BOTH_VERIFIED,
                ) &&
                Phase1V2OriginalReverseJournal(context).inspect()?.let {
                    it.combinedId == inverse.initial.combinedId &&
                    it.combinedDigest == inverse.initial.combinedDigest &&
                    it.ownerDecisionDigest == inverse.initial.ownerDecisionDigest &&
                    it.nativeV2Id == inverse.initial.nativeV2Id &&
                    it.originalDigest == inverse.initial.originalDigest &&
                    it.targetDigest == inverse.initial.targetDigest
                } == true) {
                "Scoped original Personal rollback journal disappeared or changed"
            }
            return
        }
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

    /**
     * H3 combined CRASH FORENSICS, READ ONLY. A valid V344 checkpoint may
     * pause all ordinary Personal writes. Holding the process mutex, this
     * permits ONLY withStableSnapshot (never withOwnerMutation) so we can
     * inspect actual reminder/receipt/Space metadata after process death.
     *
     * No independent v1/v2 recovery permit is borrowed. A torn cross-domain
     * checkpoint refuses entry rather than being treated as empty data.
     */
    private class CombinedReadInspectionPermit(
        val root: String,
        val expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<CombinedReadInspectionPermit>
    }

    internal suspend fun <T> withPendingCombinedReadInspection(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        action: suspend () -> T,
    ): T {
        val root = journalRootFor(context)
        val path = root.absoluteFile.normalize().path
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null) {
            "Combined read-only recovery inspection cannot borrow writer authority"
        }
        return processMutex.withLock {
            val journal = com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
            check(journal.requireCheckpointReadOnly() == expected) {
                "Combined Personal inspection started against stale recovery evidence"
            }
            val permit = CombinedReadInspectionPermit(path, expected)
            try {
                withContext(permit) {
                    val result = action()
                    check(journal.requireCheckpointReadOnly() == expected) {
                        "Cross-domain recovery changed during Personal forensics"
                    }
                    result
                }
            } finally {
                permit.active = false
            }
        }
    }

    /**
     * Outer Personal process mutex for exact H4 final-verification stages.
     * Creator takes its mutex SECOND via CreatorDataGate's final fence.
     * No Personal recovery-write token is installed. A caller may only
     * advance H4 one adjacent VERIFYING/BOTH_VERIFIED stage, not mutate
     * reminders, Space photos, challenges, or alarms.
     */
    internal suspend fun <T> withCombinedFinalCheckpointFence(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        next: com.framebynavin.app.data.V344UnifiedRestoreStep,
        action: suspend () -> T,
    ): T {
        val steps = com.framebynavin.app.data.V344UnifiedRestoreStep
        require(
            (expected.step == steps.PERSONAL_WRITTEN &&
                next == steps.VERIFYING_BOTH) ||
            (expected.step == steps.VERIFYING_BOTH &&
                next == steps.BOTH_VERIFIED)
        ) {
            "Personal checkpoint fence accepts only verification steps"
        }
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null &&
            currentCoroutineContext()[CombinedPersonalOriginalReversePermit] == null) {
            "Final verification cannot inherit Personal write or inspection permit"
        }
        return processMutex.withLock {
            val journal = com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
            check(journal.requireCheckpointReadOnly() == expected) {
                "Personal final checkpoint started from stale owner H4"
            }
            val result = action()
            val actual = journal.requireCheckpointReadOnly()
            check(actual ==
                com.framebynavin.app.data.V344UnifiedRestorePolicy.next(
                    expected, next, actual.updatedAtMillis,
                )) {
                "Personal final checkpoint did not advance exactly one verified stage"
            }
            result
        }
    }

    /**
     * Read-only outer Personal owner fence for a scoped ORIGINAL Creator
     * rollback after H4 CREATOR_WRITTEN. Does not grant Personal writes.
     * The Creator writer must separately verify both sealed archives and
     * the exact reverse-original choice.
     */
    internal suspend fun isWithinPendingCombinedReadInspection(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
    ): Boolean {
        val active = currentCoroutineContext()[CombinedReadInspectionPermit]
            ?: return false
        return active.active &&
            active.expected == expected &&
            active.root == journalRootFor(context).absoluteFile.normalize().path &&
            com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
                .requireCheckpointReadOnly() == expected
    }

    /**
     * Personal v2 ORIGINAL inverse: the owner already holds the Personal
     * process mutex via withPendingCombinedReadInspection, plus the Creator
     * read-only mutex; ONLY the durable reverse action journal grants the
     * nested Personal store mutation. No Creator writes, normal UI writes,
     * media cleanup, reminder scheduling or H4 release are permitted.
     */
    private class CombinedPersonalOriginalReversePermit(
        val root: String,
        val expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        val initial: Phase1V2ReverseRecord,
        val app: Context,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<CombinedPersonalOriginalReversePermit>
    }

    internal suspend fun <T> withOwnerApprovedCombinedPersonalOriginalReverse(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        originalRecord: Phase1V2ReverseRecord,
        ownerConfirmed: Boolean,
        action: suspend () -> T,
    ): T {
        require(ownerConfirmed &&
            expected.step in setOf(
                com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_PERSONAL,
                com.framebynavin.app.data.V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                com.framebynavin.app.data.V344UnifiedRestoreStep.VERIFYING_BOTH,
                com.framebynavin.app.data.V344UnifiedRestoreStep.BOTH_VERIFIED,
            )) {
            "Personal rollback requires owner approval at a verified Personal stage"
        }
        val root = journalRootFor(context).absoluteFile.normalize().path
        val inspection = currentCoroutineContext()[CombinedReadInspectionPermit]
            ?: error("Original Personal rollback requires the outer Personal owner mutex")
        check(inspection.active && inspection.root == root &&
            inspection.expected == expected &&
            currentCoroutineContext()[CombinedPersonalOriginalReversePermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            com.framebynavin.app.data.CreatorDataGate
                .isWithinPendingCombinedReadInspection(context, expected)) {
            "Personal original inverse requires both exact read-locked domains"
        }
        val decision = (com.framebynavin.app.data
            .V347OwnerRecoveryDecisionJournal(context).readOnlyStatus() as?
            com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal.ReadState.Recorded
            )?.choice ?: error("Missing intact original owner rollback direction")
        check(decision.choice ==
            com.framebynavin.app.data.V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            com.framebynavin.app.data.V347OwnerRecoveryDecisionPolicy.remainsBound(
                decision, expected,
            ) &&
            originalRecord.combinedId == expected.transactionId &&
            originalRecord.combinedDigest ==
                com.framebynavin.app.data.V359PersonalV2HandoffCodec.sha256(
                    com.framebynavin.app.data.V344UnifiedRestoreCheckpointCodec.encode(
                        expected,
                    ),
                ) &&
            originalRecord.ownerDecisionDigest ==
                com.framebynavin.app.data.V359PersonalV2HandoffCodec.sha256(
                    com.framebynavin.app.data.V347OwnerRecoveryDecisionCodec.encode(
                        decision,
                    ),
                ) &&
            Phase1V2OriginalReverseJournal(context).inspect() == originalRecord) {
            "Durable inverse action journal differs from original owner consent/escrow"
        }
        val permit = CombinedPersonalOriginalReversePermit(
            root, expected, originalRecord, context.applicationContext,
        )
        return try {
            withContext(permit) {
                val result = action()
                check(com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
                    .requireCheckpointReadOnly() == expected &&
                    com.framebynavin.app.data.CreatorDataGate
                        .isWithinPendingCombinedReadInspection(context, expected) &&
                    com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal(context)
                        .readOnlyStatus() ==
                        com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal.ReadState
                            .Recorded(decision)) {
                    "Personal rollback changed H4 or owner direction"
                }
                result
            }
        } finally {
            permit.active = false
        }
    }

    /**
     * H3 exclusive PERSONAL fence while the authenticated CREATOR stage is
     * committed under a separate, exact BEFORE_CREATOR CreatorDataGate scope.
     *
     * No Personal write permit is installed. Any nested ordinary Personal
     * mutation will still be forbidden by the combined V344 journal. This
     * lock only prevents an unrelated already-running Personal operation
     * from racing a Creator replacement before a combined checkpoint.
     *
     * The cross-domain journal may advance PREPARED -> BEFORE_CREATOR ->
     * CREATOR_WRITTEN while this process mutex remains held. No callbacks
     * from UI, backup export or normal alarm delivery invoke this method.
     */
    internal suspend fun <T> withCombinedCreatorStageFence(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        action: suspend () -> T,
    ): T {
        require(expected.step ==
            com.framebynavin.app.data.V344UnifiedRestoreStep.PREPARED_NO_WRITES) {
            "Creator stage requires a prepared, unwritten combined checkpoint"
        }
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null) {
            "Combined Creator stage may not inherit Personal writer or reader authority"
        }
        return processMutex.withLock {
            val journal = com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
            check(journal.requireCheckpointReadOnly() == expected) {
                "Combined Creator stage no longer matches the owner-approved journal"
            }
            val result = action()
            val after = journal.requireCheckpointReadOnly()
            check(after.transactionId == expected.transactionId &&
                after.sourceId == expected.sourceId &&
                after.sourceSha256 == expected.sourceSha256 &&
                after.escrowId == expected.escrowId &&
                after.escrowSha256 == expected.escrowSha256 &&
                after.step ==
                    com.framebynavin.app.data.V344UnifiedRestoreStep.CREATOR_WRITTEN) {
                "Creator replacement did not reach the durable combined checkpoint"
            }
            result
        }
    }

    /**
     * H3 Task 2: hold the PERSONAL process mutex while the separate, exact
     * CreatorDataGate BEFORE_CREATOR writer restores a verified NATIVE Creator
     * original. This is a dedicated recovery lock boundary, NOT the ordinary
     * combined READ-ONLY forensic inspection permit. No Personal mutation or
     * v1/v2 Personal recovery permit is granted inside this scope.
     *
     * The already-durable V347 owner decision must be REVERSE_ORIGINAL and
     * remain bound to the exact H4 checkpoint before and after the action.
     * Creator native rollback deliberately keeps H4 at BEFORE_CREATOR, all
     * original/target escrows and Voice allocation; Task 3/4 owns release.
     */
    internal suspend fun <T> withCombinedCreatorNativeReverseFence(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        ownerConfirmed: Boolean,
        action: suspend () -> T,
    ): T {
        require(ownerConfirmed &&
            expected.step ==
                com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Creator reverse recovery needs fresh consent at BEFORE_CREATOR"
        }
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null) {
            "Native Creator reverse cannot inherit a Personal writer or reader permit"
        }
        return processMutex.withLock {
            val journal = com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
            val choices = com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal(context)
            check(journal.requireCheckpointReadOnly() == expected) {
                "Creator native recovery journal changed before acquiring Personal lock"
            }
            val direction = choices.readOnlyStatus() as?
                com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal.ReadState.Recorded
                ?: error("Owner has no intact, durable Creator reverse direction")
            check(direction.choice.choice ==
                com.framebynavin.app.data.V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
                com.framebynavin.app.data.V347OwnerRecoveryDecisionPolicy.remainsBound(
                    direction.choice, expected,
                )) {
                "Creator reverse direction is unapproved or belongs to another checkpoint"
            }
            val result = action()
            check(journal.requireCheckpointReadOnly() == expected &&
                choices.readOnlyStatus() == direction) {
                "Creator native rollback cannot advance combined recovery or change owner choice"
            }
            result
        }
    }

    /**
     * Task 2: read-only Personal snapshot while owner-approved Creator recovery
     * fills MISSING, previously allocated TARGET Voice Idea files. This is NOT
     * a Personal writer: all Personal mutations remain blocked by H4.
     */
    private class PreNativeCreatorMediaReadPermit(
        val root: String,
        val expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active: Boolean = true
        companion object Key : CoroutineContext.Key<PreNativeCreatorMediaReadPermit>
    }

    internal suspend fun <T> withCombinedCreatorMissingVoiceFence(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        ownerConfirmed: Boolean,
        action: suspend () -> T,
    ): T {
        require(ownerConfirmed &&
            expected.step ==
                com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Missing Voice repair requires fresh consent at BEFORE_CREATOR"
        }
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null &&
            currentCoroutineContext()[PreNativeCreatorMediaReadPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null) {
            "Creator missing Voice repair cannot inherit a Personal writer/reader"
        }
        val root = journalRootFor(context).absoluteFile.normalize().path
        return processMutex.withLock {
            val checkpoint = com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
            val ownerChoice = com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal(context)
            check(checkpoint.requireCheckpointReadOnly() == expected) {
                "Stale H4 journal for pre-native Creator media repair"
            }
            val direction = ownerChoice.readOnlyStatus() as?
                com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal.ReadState.Recorded
                ?: error("Missing durable owner recovery direction")
            check(direction.choice.choice ==
                com.framebynavin.app.data.V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
                com.framebynavin.app.data.V347OwnerRecoveryDecisionPolicy.remainsBound(
                    direction.choice, expected,
                )) {
                "Missing Voice repair requires the exact FORWARD_TARGET choice"
            }
            val permit = PreNativeCreatorMediaReadPermit(root, expected)
            try {
                withContext(permit) {
                    val result = action()
                    check(checkpoint.requireCheckpointReadOnly() == expected &&
                        ownerChoice.readOnlyStatus() == direction) {
                        "Creator Voice media repair changed H4/owner direction"
                    }
                    result
                }
            } finally {
                permit.active = false
            }
        }
    }

    /**
     * Internal Task 2 safety assertion, usable by the nested scoped Creator
     * shadow-media writer. It does not acquire a second Personal mutex or
     * create a read/write permit; it only verifies the inherited active fence.
     */
    internal suspend fun isWithinCombinedCreatorMissingVoiceFence(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
    ): Boolean {
        val permit = currentCoroutineContext()[PreNativeCreatorMediaReadPermit]
            ?: return false
        return permit.active &&
            permit.expected == expected &&
            permit.root == journalRootFor(context).absoluteFile.normalize().path &&
            com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
                .requireCheckpointReadOnly() == expected
    }

    /**
     * Fixed H3 Task 2 Creator replay completion boundary, not another
     * Personal restore mode. Personal process mutex FIRST; Creator writer
     * SECOND inside action. Read-only Personal preflight remains permitted
     * only while H4 is BEFORE_CREATOR. The action may advance H4 exactly
     * once to CREATOR_WRITTEN after the Creator writer has exited and the
     * authenticated original owner decision has been verified.
     *
     * The retained owner choice is NOT deleted; the next H3 tasks must
     * explicitly review it. Nothing here authorizes Personal writes.
     */
    internal suspend fun <T> withCombinedCreatorRecoveryFinishFence(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        ownerConfirmed: Boolean,
        action: suspend () -> T,
    ): T {
        require(ownerConfirmed &&
            expected.step ==
                com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Creator stage completion requires fresh owner consent at BEFORE_CREATOR"
        }
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null &&
            currentCoroutineContext()[PreNativeCreatorMediaReadPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null) {
            "Creator completion cannot inherit unrelated Personal mutation authority"
        }
        val root = journalRootFor(context).absoluteFile.normalize().path
        return processMutex.withLock {
            val journal = com.framebynavin.app.data.V344UnifiedRestoreJournal(context)
            val decisions = com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal(context)
            check(journal.requireCheckpointReadOnly() == expected) {
                "Combined Creator replay checkpoint drifted before Personal lock"
            }
            val originalChoice = decisions.readOnlyStatus() as?
                com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal.ReadState.Recorded
                ?: error("No valid, durable owner forward-recovery direction")
            check(originalChoice.choice.choice ==
                com.framebynavin.app.data.V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
                com.framebynavin.app.data.V347OwnerRecoveryDecisionPolicy.remainsBound(
                    originalChoice.choice, expected,
                )) {
                "Creator replay cannot use a reversed or stale owner decision"
            }
            val permit = PreNativeCreatorMediaReadPermit(root, expected)
            try {
                withContext(permit) {
                    val result = action()
                    val after = journal.requireCheckpointReadOnly()
                    check(after.step ==
                        com.framebynavin.app.data.V344UnifiedRestoreStep.CREATOR_WRITTEN &&
                        after.transactionId == expected.transactionId &&
                        after.sourceSha256 == expected.sourceSha256 &&
                        after.escrowSha256 == expected.escrowSha256 &&
                        decisions.readOnlyStatus() ==
                            com.framebynavin.app.data.V347OwnerRecoveryDecisionJournal
                                .ReadState.CreatorWrittenRetained(originalChoice.choice, after)) {
                        "Creator replay did not durably complete with its owner direction"
                    }
                    result
                }
            } finally {
                permit.active = false
            }
        }
    }

    /**
     * INTERNAL H3 Personal ADDITIVE forward stage. Requires CREATOR_WRITTEN,
     * owner approval, and the exact combined checkpoint. Holds Personal's
     * process mutex throughout the operation. Callers must also acquire
     * CreatorDataGate.withPendingCombinedReadInspection at BEFORE_PERSONAL.
     *
     * Unlike the ordinary mutation lock, this special permit can read the
     * original Personal baseline at CREATOR_WRITTEN, then enter mutation only
     * AFTER BEFORE_PERSONAL is durably written. The existing v2 journal and
     * photo write scopes are still required; this is not an unjournaled
     * arbitrary Personal-write capability.
     */
    private class CombinedPersonalForwardPermit(
        val root: String,
        val ownerId: String,
        val sourceSha256: String,
        val escrowSha256: String,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<CombinedPersonalForwardPermit>
    }

    internal suspend fun <T> withCombinedPersonalForwardFence(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        ownerConfirmed: Boolean,
        action: suspend () -> T,
    ): T {
        require(ownerConfirmed &&
            expected.step ==
                com.framebynavin.app.data.V344UnifiedRestoreStep.CREATOR_WRITTEN) {
            "Combined Personal stage needs owner consent after a verified Creator write"
        }
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null &&
            currentCoroutineContext()[CombinedPersonalForwardPermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[InspectionPermit] == null) {
            "A Personal combined forward writer cannot inherit another recovery permission"
        }
        val app = context.applicationContext
        val root = journalRootFor(app)
        val path = root.absoluteFile.normalize().path
        return processMutex.withLock {
            val journal = com.framebynavin.app.data.V344UnifiedRestoreJournal(app)
            check(journal.requireCheckpointReadOnly() == expected &&
                !Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.exists(
                    Phase1V2RecoveryRoots.alongsideLegacy(root).toPath(),
                    LinkOption.NOFOLLOW_LINKS,
                )) { "Another recovery journal blocks combined Personal stage" }
            val permit = CombinedPersonalForwardPermit(
                root = path, ownerId = expected.transactionId,
                sourceSha256 = expected.sourceSha256,
                escrowSha256 = expected.escrowSha256,
            )
            try {
                withContext(permit) {
                    val result = action()
                    val current = journal.requireCheckpointReadOnly()
                    check(current.transactionId == expected.transactionId &&
                        current.sourceSha256 == expected.sourceSha256 &&
                        current.escrowSha256 == expected.escrowSha256 &&
                        current.step ==
                            com.framebynavin.app.data.V344UnifiedRestoreStep.PERSONAL_WRITTEN) {
                        "Personal stage ended before both original/target journals were verified"
                    }
                    result
                }
            } finally {
                permit.active = false
            }
        }
    }

    /**
     * H3 Task 1: active native v2 forward recovery while H4 remains
     * BEFORE_PERSONAL. This gate acquires the existing Personal process mutex
     * ONCE through withPendingV2RecoveryInspection; callers may then acquire
     * the Creator read mutex in the established Personal -> Creator order.
     *
     * A torn native journal, missing immutable V359 handoff, competing legacy
     * journal or a different H4 checkpoint is never promoted to permission.
     * The nested native v2 writer still needs withApprovedV2ForwardRecovery,
     * which independently authenticates the original, target and photo bytes.
     */
    private class CombinedPersonalV2ResumePermit(
        val root: String,
        val combined: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        val native: Phase1V2RestoreCheckpoint,
    ) : AbstractCoroutineContextElement(Key) {
        @Volatile var active = true
        companion object Key : CoroutineContext.Key<CombinedPersonalV2ResumePermit>
    }

    internal suspend fun <T> withCombinedActiveV2Resume(
        context: Context,
        expected: com.framebynavin.app.data.V344UnifiedRestoreCheckpoint,
        ownerConfirmedResume: Boolean,
        action: suspend (Phase1V2RestoreCheckpoint) -> T,
    ): T {
        require(ownerConfirmedResume &&
            expected.step ==
                com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_PERSONAL) {
            "Owner must confirm recovery of the exact interrupted Personal stage"
        }
        check(currentCoroutineContext()[Permit] == null &&
            currentCoroutineContext()[RecoveryPermit] == null &&
            currentCoroutineContext()[V2RecoveryWritePermit] == null &&
            currentCoroutineContext()[V2InspectionPermit] == null &&
            currentCoroutineContext()[CombinedPersonalV2ResumePermit] == null &&
            currentCoroutineContext()[CombinedPersonalForwardPermit] == null &&
            currentCoroutineContext()[CombinedReadInspectionPermit] == null) {
            "Combined Personal resume cannot inherit another recovery authority"
        }
        val app = context.applicationContext
        val legacy = journalRootFor(app)
        val journal = com.framebynavin.app.data.V344UnifiedRestoreJournal(app)
        return withPendingV2RecoveryInspection(app) { native ->
            check(journal.requireCheckpointReadOnly() == expected) {
                "Combined owner journal moved while inspecting Personal v2 recovery"
            }
            val bound = (
                com.framebynavin.app.data.V359UnifiedPersonalV2HandoffJournal(app)
                    .inspectReadOnly() as?
                    com.framebynavin.app.data.V359UnifiedPersonalV2HandoffJournal
                        .Inspection.Bound
            )?.handoff ?: error("Missing or corrupt combined Personal v2 handoff")
            check(com.framebynavin.app.data.V359PersonalV2HandoffCodec
                .matchesCombined(bound, expected) &&
                com.framebynavin.app.data.V359PersonalV2HandoffCodec
                    .matchesV2(bound, native)) {
                "Native v2 recovery is not the selected combined Personal transaction"
            }
            val scoped = CombinedPersonalV2ResumePermit(
                legacy.absoluteFile.normalize().path, expected, native,
            )
            try {
                withContext(scoped) {
                    val result = action(native)
                    val final = journal.requireCheckpointReadOnly()
                    check(final.transactionId == expected.transactionId &&
                        final.sourceId == expected.sourceId &&
                        final.sourceSha256 == expected.sourceSha256 &&
                        final.escrowId == expected.escrowId &&
                        final.escrowSha256 == expected.escrowSha256 &&
                        final.creatorOriginalSha256 == expected.creatorOriginalSha256 &&
                        final.personalOriginalSha256 == expected.personalOriginalSha256 &&
                        final.createdAtMillis == expected.createdAtMillis &&
                        final.updatedAtMillis >= expected.updatedAtMillis &&
                        final.step ==
                            com.framebynavin.app.data.V344UnifiedRestoreStep.PERSONAL_WRITTEN) {
                        "Personal recovery cannot return before its verified outer checkpoint"
                    }
                    result
                }
            } finally { scoped.active = false }
        }
    }

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
        val inverse = currentCoroutineContext()[CombinedPersonalOriginalReversePermit]
        if (inverse != null) {
            check(inverse.active && inverse.root == path &&
                inverse.expected.step in setOf(
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_PERSONAL,
                    com.framebynavin.app.data.V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                    com.framebynavin.app.data.V344UnifiedRestoreStep.VERIFYING_BOTH,
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BOTH_VERIFIED,
                ) &&
                com.framebynavin.app.data.V344UnifiedRestoreJournal(
                    inverse.app,
                ).requireCheckpointReadOnly() == inverse.expected &&
                Phase1V2OriginalReverseJournal(inverse.app).inspect()?.let {
                    it.combinedId == inverse.initial.combinedId &&
                    it.combinedDigest == inverse.initial.combinedDigest &&
                    it.ownerDecisionDigest == inverse.initial.ownerDecisionDigest &&
                    it.nativeV2Id == inverse.initial.nativeV2Id &&
                    it.originalDigest == inverse.initial.originalDigest &&
                    it.targetDigest == inverse.initial.targetDigest
                } == true) {
                "Personal reverse permit expired or owner recovery changed"
            }
            return action()
        }
        val combinedResume = currentCoroutineContext()[CombinedPersonalV2ResumePermit]
        if (combinedResume != null) {
            check(combinedResume.active && combinedResume.root == path) {
                "Combined Personal v2 resume permission expired"
            }
            val appFiles = checkNotNull(root.parentFile)
            val combinedFile = File(
                File(appFiles,
                    com.framebynavin.app.data.V344UnifiedRestoreJournal.ROOT_NAME),
                "active.journal",
            )
            check(Files.isRegularFile(
                combinedFile.toPath(), LinkOption.NOFOLLOW_LINKS,
            ) && !Files.isSymbolicLink(combinedFile.toPath()) &&
                com.framebynavin.app.data.V344UnifiedRestoreCheckpointCodec.decode(
                    combinedFile.readBytes(),
                ) == combinedResume.combined) {
                "Combined Personal restore checkpoint is stale or torn"
            }
            val currentNative = Phase1V2RestoreWriteAheadJournal(
                Phase1V2RecoveryRoots.alongsideLegacy(root),
            ).inspectAfterRestart()
            val writer = currentCoroutineContext()[V2RecoveryWritePermit]
            if (ordinaryMutation) {
                check(writer != null && writer.active &&
                    writer.legacyRoot == path &&
                    currentNative != null &&
                    currentNative.transactionId ==
                        combinedResume.native.transactionId &&
                    currentNative.preparedId == combinedResume.native.preparedId &&
                    currentNative.escrowId == combinedResume.native.escrowId &&
                    currentNative.originalDigest ==
                        combinedResume.native.originalDigest &&
                    currentNative.targetDigest ==
                        combinedResume.native.targetDigest) {
                    "Native v2 owner write journal is required for each resumed mutation"
                }
                ensureMatchingV2RecoveryJournal(root, writer)
            } else {
                check(currentCoroutineContext()[V2InspectionPermit] != null ||
                    writer != null) {
                    "Personal interrupted recovery reads need a native inspection permit"
                }
            }
            return action()
        }
        val combinedForward = currentCoroutineContext()[CombinedPersonalForwardPermit]
        if (combinedForward != null) {
            check(combinedForward.active && combinedForward.root == path) {
                "Combined Personal writer is expired or belongs to another recovery root"
            }
            val appFiles = requireNotNull(root.parentFile) {
                "Personal owner recovery path has no parent"
            }
            val combinedRoot = File(
                appFiles, com.framebynavin.app.data.V344UnifiedRestoreJournal.ROOT_NAME,
            )
            check(Files.isRegularFile(
                File(combinedRoot, "active.journal").toPath(),
                LinkOption.NOFOLLOW_LINKS,
            )) { "Combined Personal recovery checkpoint has no valid active journal" }
            val active = com.framebynavin.app.data.V344UnifiedRestoreCheckpointCodec.decode(
                File(combinedRoot, "active.journal").readBytes(),
            )
            check(active.transactionId == combinedForward.ownerId &&
                active.sourceSha256 == combinedForward.sourceSha256 &&
                active.escrowSha256 == combinedForward.escrowSha256) {
                "Combined Personal owner checkpoint no longer matches the write scope"
            }
            if (ordinaryMutation) {
                check(active.step ==
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_PERSONAL) {
                    "No Personal mutation before the durable BEFORE_PERSONAL marker"
                }
            } else {
                check(active.step ==
                    com.framebynavin.app.data.V344UnifiedRestoreStep.CREATOR_WRITTEN ||
                    active.step ==
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_PERSONAL) {
                    "Personal crash-forensic reads require an eligible combined checkpoint"
                }
            }
            // The active v2 recovery journal remains authoritative even
            // when the initial ordinary owner Permit is also inherited.
            // Check it BEFORE accepting the owner permit to prevent an
            // independent/stale Personal v2 import from bypassing its journal.
            val v2 = currentCoroutineContext()[V2RecoveryWritePermit]
            if (v2 != null) {
                check(v2.active && v2.legacyRoot == path) {
                    "Unrelated v2 restore cannot inherit combined Personal authority"
                }
                ensureMatchingV2RecoveryJournal(root, v2)
                return action()
            }
            val existing = currentCoroutineContext()[Permit]
            if (existing != null) {
                check(existing.active && existing.root == path) {
                    "Personal owner's restore scope is stale"
                }
                // The outer owner mutation permits preparing an encrypted
                // Personal v2 escrow and journal, not changing any records.
                // All nested real store/photo mutations must carry the
                // verified V2RecoveryWritePermit checked ABOVE.
                check(!ordinaryMutation) {
                    "Combined Personal data writes require the independent prepared v2 journal"
                }
                return action()
            }
            val owner = Permit(path)
            return try { withContext(owner) { action() } }
            finally { owner.active = false }
        }
        val creatorMedia = currentCoroutineContext()[PreNativeCreatorMediaReadPermit]
        if (creatorMedia != null) {
            check(!ordinaryMutation && creatorMedia.active &&
                creatorMedia.root == path &&
                creatorMedia.expected.step ==
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_CREATOR) {
                "Creator media recovery grants read-only Personal access only"
            }
            val h4File = File(
                File(requireNotNull(root.parentFile),
                    com.framebynavin.app.data.V344UnifiedRestoreJournal.ROOT_NAME),
                "active.journal",
            )
            check(Files.isRegularFile(h4File.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(h4File.toPath()) &&
                com.framebynavin.app.data.V344UnifiedRestoreCheckpointCodec.decode(
                    h4File.readBytes(),
                ) == creatorMedia.expected) {
                "H4 journal changed during read-only Personal media recovery"
            }
            return action()
        }
        val combinedRead = currentCoroutineContext()[CombinedReadInspectionPermit]
        if (combinedRead != null) {
            check(!ordinaryMutation && combinedRead.active &&
                combinedRead.root == path) {
                "Combined recovery inspection cannot mutate Personal records or media"
            }
            return action()
        }
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
