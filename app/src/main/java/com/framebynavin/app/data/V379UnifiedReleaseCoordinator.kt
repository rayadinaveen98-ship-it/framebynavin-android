package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2OriginalReverseJournal
import com.framebynavin.app.personal.Phase1V2RecoveryRoots
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2RestoreWriteAheadJournal
import com.framebynavin.app.personal.Phase1V2ReverseStep
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3.4 internal, fresh owner-approved, NON-DESTRUCTIVE combined release.
 *
 * 1. Reauthenticate BOTH encrypted archives and reproduce the V377 complete
 *    live Creator/Personal/Voice/Best Frame/private JPEG proof.
 * 2. Persist V378 signed-by-digest evidence UNDER Personal+Creator mutexes.
 * 3. Persist V379 release journal.
 * 4. Retain subordinate owner journals in one private transaction history.
 * 5. Retain both encrypted ORIGINAL and selected TARGET ZIP folders.
 * 6. Archive H4 LAST under the same H4 checkpoint mutex used by writers.
 * 7. Reconcile alarms only AFTER H4 is absent, via a durable WorkManager
 *    safety net. Neither source/escrow ZIP nor owner media is ever deleted.
 *
 * A crash between individual ATOMIC_MOVE steps is safe: moveChecked() will
 * accept an exact retained folder when the active one is absent, and
 * refuse the ambiguity when both exist. If the process crashes after the
 * H4 atomic move but before the final ledger update, the WorkManager
 * startup safety net verifies the retained H4 checkpoint and continues.
 *
 * No UI or user-data repair invokes this method. Normal Restore All stays
 * DISABLED pending Package J Android/emulator validation.
 */
internal class V379UnifiedReleaseCoordinator(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val receipts = V378UnifiedVerifiedEvidenceJournal(app)
    private val stages = V379UnifiedReleaseJournal(app)

    internal suspend fun releaseOwnerVerifiedInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V379UnifiedReleaseRecord = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "A separate owner finalization approval and passphrase are required"
        }
        val evidence = requireNotNull(receipts.inspect()) {
            "Both-domain outcome is not yet durably verified"
        }
        val record = stages.inspect()
        check(record == null || (
            record.transactionId == evidence.transactionId &&
            record.checkpointSha256 == evidence.checkpointSha256 &&
            record.verificationSha256 ==
                V379UnifiedReleaseJournal.proofDigest(evidence) &&
            record.outcome == evidence.outcome
        )) {
            "Existing release sequence belongs to another verified owner outcome"
        }
        if (record != null && record.step.ordinal >=
            V379ReleaseStep.H4_ARCHIVED.ordinal) {
            check(!V344UnifiedRestoreJournal.hasPendingFor(app))
            V380CombinedAlarmRearmSafetyNet.enqueueIfReleased(app)
            return@withContext record
        }
        val expected = h4.requireCheckpointReadOnly()
        check(expected.transactionId == evidence.transactionId &&
            V379UnifiedReleaseJournal.sha256(
                V344UnifiedRestoreCheckpointCodec.encode(expected),
            ) == evidence.checkpointSha256) {
            "H4 journal changed after the owner's completed live verification"
        }
        // This is read-only and expensive. Do it before acquiring both
        // long-lived owner store mutexes, except on restart after the signed
        // ZIP roots have already been safely moved to retained history.
        if (record == null || record.step.ordinal <
            V379ReleaseStep.ENCRYPTED_ORIGINAL_AND_TARGET_RETAINED.ordinal) {
            val fresh = V377UnifiedFinalVerification(app).inspectOwnerApproved(
                freshOwnerConsent = true, passphrase = passphrase,
            )
            check(fresh == evidence) {
                "Creator, Personal, Voice or Space JPEG data drifted after completion proof"
            }
        }
        val ownerHistory = File(
            File(app.filesDir,
                "backlot-unified-retained-recovery-history-v1"),
            expected.transactionId,
        )
        val activeRecovery = File(app.filesDir, "creator_restore_recovery_v181")
        check(listOf("pending.json", "pending.json.new", "pending.json.bak")
            .none { name -> Files.exists(
                File(activeRecovery, name).toPath(), LinkOption.NOFOLLOW_LINKS,
            ) } &&
            !IdeaProjectConversion(app).hasPendingRecovery()) {
            "Creator import or conversion has a pending native journal"
        }
        val beforeH4 = StabilizationPersonalMutationGate
            .withPendingCombinedReadInspection(app, expected) {
                CreatorDataGate.withPendingCombinedReadInspection(app, expected) {
                    check(h4.requireCheckpointReadOnly() == expected &&
                        receipts.inspect() == evidence) {
                        "Owner's verified checkpoint or completion receipt changed"
                    }
                    val before = record ?: stages.prepare(
                        V379UnifiedReleaseRecord(
                            transactionId = expected.transactionId,
                            checkpointSha256 = evidence.checkpointSha256,
                            verificationSha256 =
                                V379UnifiedReleaseJournal.proofDigest(evidence),
                            outcome = evidence.outcome,
                            step = V379ReleaseStep.PREPARED,
                        ),
                        ownerApproved = true,
                    )
                    var current = before
                    if (current.step == V379ReleaseStep.PREPARED) {
                        verifyOptionalSubjournals(expected, evidence)
                        listOf(
                            "backlot-unified-personal-v2-handoff-v1",
                            "backlot-unified-creator-voice-install-v1",
                            "backlot-unified-creator-voice-shadow-v1",
                            "backlot-unified-creator-original-inverse-v1",
                            "backlot-unified-personal-v2-reverse-actions-v1",
                            Phase1V2RecoveryRoots.ACTIVE_NAME,
                        ).forEach { name ->
                            V379AtomicOwnerHistory.retain(
                                app, expected.transactionId, name,
                                required = false,
                            )
                        }
                        V379AtomicOwnerHistory.retain(
                            app, expected.transactionId,
                            "backlot-unified-owner-recovery-decision-v1",
                            required = true,
                        )
                        current = stages.advance(
                            current, V379ReleaseStep.JOURNALS_RETAINED,
                        )
                    }
                    if (current.step == V379ReleaseStep.JOURNALS_RETAINED) {
                        // Check the bytes at whichever of the two exact
                        // app-private locations currently owns each ZIP root.
                        verifyOwnerArchivesAtActiveOrHistory(
                            expected, ownerHistory, passphrase,
                        )
                        V379AtomicOwnerHistory.retain(
                            app, expected.transactionId,
                            "backlot-unified-owner-recovery-escrow-v1",
                            required = true,
                        )
                        V379AtomicOwnerHistory.retain(
                            app, expected.transactionId,
                            "backlot-unified-owner-restore-preparation-v1",
                            required = true,
                        )
                        current = stages.advance(
                            current,
                            V379ReleaseStep.ENCRYPTED_ORIGINAL_AND_TARGET_RETAINED,
                        )
                    }
                    if (current.step ==
                        V379ReleaseStep.ENCRYPTED_ORIGINAL_AND_TARGET_RETAINED) {
                        verifyOwnerArchivesAtActiveOrHistory(
                            expected, ownerHistory, passphrase,
                        )
                        check(!Files.exists(
                            File(app.filesDir,
                                "backlot-unified-owner-recovery-decision-v1")
                                .toPath(), LinkOption.NOFOLLOW_LINKS,
                        )) {
                            "Owner direction must be retained before H4 release"
                        }
                        current = stages.advance(
                            current, V379ReleaseStep.READY_TO_ARCHIVE_H4,
                        )
                    }
                    check(current.step == V379ReleaseStep.READY_TO_ARCHIVE_H4) {
                        "H4 cannot be archived before all original evidence is preserved"
                    }
                    current
                }
            }
        // Both owner read locks have exited; their invariants required H4
        // to remain exact and active. The next atomic H4 operation shares
        // the checkpoint mutex with every other H4 stage writer.
        val completed = File(ownerHistory, V344UnifiedRestoreJournal.ROOT_NAME)
        check(h4.archiveFullyVerifiedCheckpoint(
            expected = expected,
            proof = evidence,
            release = beforeH4,
            destination = completed,
            ownerConfirmed = true,
        )) {
            "Combined H4 recovery journal was not durably archived"
        }
        val archived = stages.advance(
            beforeH4, V379ReleaseStep.H4_ARCHIVED,
        )
        // Never rearm alarms while H4 is active; if enqueue fails or the
        // process dies immediately after the H4 move, the Application's
        // verified-release safety net recovers independently.
        V380CombinedAlarmRearmSafetyNet.enqueueIfReleased(app)
        archived
    }

    private fun verifyOptionalSubjournals(
        expected: V344UnifiedRestoreCheckpoint,
        proof: V377UnifiedFinalReadOnlyProof,
    ) {
        val shadow = V371UnifiedCreatorVoiceShadowJournal(app)
        if (shadow.hasBlockingEvidence()) check(shadow.inspectReadOnly() != null)
        val voice = V365UnifiedCreatorVoiceInstallJournal(app)
        if (voice.hasBlockingEvidence()) check(voice.inspectReadOnly() != null)
        val inverse = V376CreatorOriginalInverseJournal(app)
        if (inverse.hasEvidence()) check(inverse.inspect() != null)
        val personalInverse = Phase1V2OriginalReverseJournal(app)
        if (personalInverse.hasEvidence()) {
            check(personalInverse.inspect()?.let {
                it.step == Phase1V2ReverseStep.ORIGINAL_RECORDS_VERIFIED &&
                    it.combinedId == expected.transactionId &&
                    proof.outcome == V377VerifiedOwnerOutcome.ORIGINAL_RESTORED
            } == true) {
                "Native Personal v2 inverse isn't verified"
            }
        }
        val native = Phase1V2RecoveryRoots.alongsideLegacy(
            StabilizationPersonalMutationGate.journalRootFor(app),
        )
        if (Files.exists(native.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            val personal = (V359UnifiedPersonalV2HandoffJournal(app)
                .inspectReadOnly() as?
                V359UnifiedPersonalV2HandoffJournal.Inspection.Bound)?.handoff
                ?: error("Native Personal v2 source cannot be released without handoff")
            val checkpoint = requireNotNull(
                Phase1V2RestoreWriteAheadJournal(native).inspectAfterRestart(),
            ) { "Native Personal v2 journal is torn" }
            check(proof.outcome == V377VerifiedOwnerOutcome.ORIGINAL_RESTORED &&
                personalInverse.inspect()?.step ==
                    Phase1V2ReverseStep.ORIGINAL_RECORDS_VERIFIED &&
                V359PersonalV2HandoffCodec.matchesV2(personal, checkpoint)) {
                "Active v2 evidence was not authentically reversed"
            }
        }
    }

    private fun verifyOwnerArchivesAtActiveOrHistory(
        cp: V344UnifiedRestoreCheckpoint,
        history: File,
        passphrase: CharArray,
    ) {
        for ((name, filename, expected) in listOf(
            Triple("backlot-unified-owner-recovery-escrow-v1",
                "original-owner-encrypted-unified.zip", cp.escrowSha256),
            Triple("backlot-unified-owner-restore-preparation-v1",
                "incoming-unified-encrypted.zip", cp.sourceSha256),
        )) {
            val here = File(app.filesDir, name)
            val retained = File(history, name)
            val active = Files.exists(here.toPath(), LinkOption.NOFOLLOW_LINKS)
            val moved = Files.exists(retained.toPath(), LinkOption.NOFOLLOW_LINKS)
            check(active != moved) {
                "Original or selected encrypted archive missing/duplicated in private history"
            }
            val root = if (active) here else retained
            val archive = File(root, filename)
            check(root.isDirectory &&
                !Files.isSymbolicLink(root.toPath()) &&
                Files.isRegularFile(archive.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(archive.toPath()) &&
                archive.canonicalFile.parentFile == root.canonicalFile &&
                sha(archive) == expected) {
                "Retained encrypted owner ZIP differs from the verified checkpoint"
            }
            // Re-authenticate the GCM tags with the owner's supplied
            // passphrase even if the archive was already atomically moved.
            archive.inputStream().buffered().use { input ->
                V341UnifiedOwnerArchive.stageAndVerify(
                    input, passphrase, app.cacheDir,
                ).use { verified ->
                    check(verified.creator.isFile && verified.personal.isFile) {
                        "Retained Creator/Personal archive was not fully authenticated"
                    }
                }
            }
        }
    }

    private fun sha(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
}
