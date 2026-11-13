package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.B04NoPhotoRollbackDomainWriter
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.Phase1HabitStore
import com.framebynavin.app.personal.Phase1V2OriginalReverseJournal
import com.framebynavin.app.personal.Phase1V2PhotoByteForensics
import com.framebynavin.app.personal.Phase1V2RestoreDomainForensics
import com.framebynavin.app.personal.Phase1V2RestoreFingerprint
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2ReverseRecord
import com.framebynavin.app.personal.Phase1V2ReverseStep
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V297ChallengeStore
import com.framebynavin.app.personal.V298ReminderGroupCodec
import com.framebynavin.app.personal.V298ReminderGroupStore
import com.framebynavin.app.personal.V301LocalRecoveryReader
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 Task 3 internal Personal ORIGINAL rollback for H4 BEFORE_PERSONAL.
 *
 * Reuses the exact authenticated V374 inverse recipe and existing Personal
 * DataStore / Room inverse writer. After an independent, durable reverse
 * journal is prepared, every stage writes ONLY approved imported IDs:
 *
 *   imported private PHOTO ROWS (JPEG bytes RETAINED as recovery evidence),
 *   imported Space entries/links/Spaces, reminder Groups, 21-day Challenges,
 *   legacy Challenges, Personal reminders and exact Done history.
 *
 * The immutable forward v2 journal and combined H4 are NEVER rewound.
 * Every action is preceded by a durable BEFORE_* marker, and can be
 * repeated after a crash only when every observed domain is the whole
 * ORIGINAL or whole TARGET snapshot. No app alarm or cloud sync runs.
 *
 * New/imported JPEGs remain under the original media folder, unreferenced,
 * for Task 4's independent checksum/cleanup decision. All ORIGINAL photos
 * and their metadata remain intact; original encrypted ZIPs retained.
 *
 * This does NOT roll back Creator in the same operation. A separately
 * authenticated Creator original stage must follow before either domain
 * may be declared fully recovered and recovery journal release is considered.
 *
 * No production UI, startup receiver or worker is connected.
 */
internal data class V375PersonalOriginalRecordsReceipt(
    val transactionId: String,
    val originalPersonalDigest: String,
    val reversedReminderCount: Int,
    val reversedHabitCount: Int,
    val reversedSpaceCount: Int,
    val importedPhotoRowsRemoved: Int,
    val originalPhotoRowsRetained: Int,
    val reverseStage: Phase1V2ReverseStep,
) {
    init {
        require(transactionId.isNotBlank() &&
            originalPersonalDigest.matches(Regex("[a-f0-9]{64}")) &&
            listOf(reversedReminderCount, reversedHabitCount,
                reversedSpaceCount, importedPhotoRowsRemoved,
                originalPhotoRowsRetained).all { it >= 0 } &&
            reverseStage == Phase1V2ReverseStep.ORIGINAL_RECORDS_VERIFIED)
    }
    val importedJpegFilesPreserved: Boolean get() = true
    val mayReleaseH4OrEscrow: Boolean get() = false
    val mayDeleteAnyOwnerPhoto: Boolean get() = false
    val mayRearmAlarms: Boolean get() = false
    val creatorAlsoRolledBack: Boolean get() = false
}

internal class V375UnifiedPersonalOriginalRollbackExecutor(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val decisions = V347OwnerRecoveryDecisionJournal(app)
    private val reverse = Phase1V2OriginalReverseJournal(app)

    internal suspend fun restorePersonalOriginalInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V375PersonalOriginalRecordsReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Fresh explicit ORIGINAL rollback approval and archive passphrase required"
        }
        val expected = h4.requireCheckpointReadOnly()
        check(expected.step == V344UnifiedRestoreStep.BEFORE_PERSONAL) {
            "Only Personal's interrupted before-write stage is eligible for v2 inverse"
        }
        val review = V374UnifiedPersonalOriginalRollbackInspector(app)
            .inspectOwnerApproved(ownerConfirmed = true, passphrase = passphrase)
        check(review.transactionId == expected.transactionId) {
            "Personal inverse is not bound to the current combined owner recovery"
        }
        val decision = (decisions.readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("Original rollback owner direction is missing or torn")
        check(decision.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(decision, expected)) {
            "Personal inverse cannot inherit a stale or forward-recovery approval"
        }
        val initial = Phase1V2ReverseRecord(
            combinedId = expected.transactionId,
            combinedDigest = V359PersonalV2HandoffCodec.sha256(
                V344UnifiedRestoreCheckpointCodec.encode(expected),
            ),
            ownerDecisionDigest = V359PersonalV2HandoffCodec.sha256(
                V347OwnerRecoveryDecisionCodec.encode(decision),
            ),
            nativeV2Id = review.nativeV2TransactionId,
            nativeStep = review.nativeStep,
            originalDigest = review.delta.originalDigest,
            targetDigest = review.delta.mergedTargetDigest,
            step = Phase1V2ReverseStep.PREPARED,
        )
        val originalZip = ownerArchive(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
        )
        val selectedZip = ownerArchive(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip",
        )
        check(sha256(originalZip) == expected.escrowSha256 &&
            sha256(selectedZip) == expected.sourceSha256) {
            "Encrypted ORIGINAL/TARGET sources changed during owner review"
        }

        StabilizationPersonalMutationGate.withPendingCombinedReadInspection(
            app, expected,
        ) {
            CreatorDataGate.withPendingCombinedReadInspection(app, expected) {
                check(h4.requireCheckpointReadOnly() == expected &&
                    decisions.readOnlyStatus() ==
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision) &&
                    sha256(originalZip) == expected.escrowSha256 &&
                    sha256(selectedZip) == expected.sourceSha256) {
                    "Owner escrow or H4 stage changed before original Personal inverse"
                }
                val previous = reverse.inspect()
                check(previous == null || previous.copy(
                    step = Phase1V2ReverseStep.PREPARED,
                ) == initial) {
                    "Existing inverse journal is not the same authenticated owner request"
                }
                val before = V301LocalRecoveryReader(app)
                    .readForInterruptedV2Recovery()
                // A resumed inverse needs reverse-aware known-state proof;
                // otherwise the native forward chronology must be intact.
                if (previous == null) {
                    val latest = V374UnifiedPersonalOriginalRollbackInspector(app)
                        // Already holding Personal/Creator mutexes: DO NOT
                        // reacquire them. Use the previous owner review,
                        // whose full archive evidence is checked again below.
                        .let { review }
                    check(!latest.reverseJournalAlreadyPrepared)
                }
                // Initial inspection already passed native-specific v2
                // chronology; after a crash, only ORIGINAL/TARGET atomic
                // domain states and imported-photo-row subsets are legal.
                // Use the strict reverse policy on every step.
                checkKnownPersonalState(review, before)
                val prepared = previous ?: reverse.prepare(
                    ownerApproved = true, record = initial,
                )

                StabilizationPersonalMutationGate
                    .withOwnerApprovedCombinedPersonalOriginalReverse(
                        app, expected, prepared,
                        ownerConfirmed = true,
                    ) {
                        suspend fun phase(
                            earlier: Phase1V2ReverseStep,
                            beforeStep: Phase1V2ReverseStep,
                            done: Phase1V2ReverseStep,
                            operation: suspend () -> Unit,
                        ) {
                            var persisted = requireNotNull(reverse.inspect())
                            if (persisted.step.ordinal > done.ordinal) return
                            if (persisted.step == earlier) {
                                persisted = reverse.advance(persisted, beforeStep)
                            }
                            check(persisted.step == beforeStep) {
                                "Inverse Personal stage skipped a durable BEFORE marker"
                            }
                            val current = V301LocalRecoveryReader(app)
                                .readForInterruptedV2Recovery()
                            checkKnownPersonalState(review, current)
                            operation()
                            val after = V301LocalRecoveryReader(app)
                                .readForInterruptedV2Recovery()
                            checkKnownPersonalState(review, after)
                            reverse.advance(persisted, done)
                        }
                        val room = B04NoPhotoRollbackDomainWriter(app)
                        phase(
                            Phase1V2ReverseStep.PREPARED,
                            Phase1V2ReverseStep.BEFORE_PHOTO_ROWS,
                            Phase1V2ReverseStep.PHOTO_ROWS_REVERTED,
                        ) {
                            room.removeOnlyImportedPhotoRowsV2(
                                review.originalPhotoRows,
                                review.targetPhotoRows,
                                review.delta,
                            )
                        }
                        phase(
                            Phase1V2ReverseStep.PHOTO_ROWS_REVERTED,
                            Phase1V2ReverseStep.BEFORE_SPACES,
                            Phase1V2ReverseStep.SPACES_REVERTED,
                        ) {
                            room.removeOnlyImportedSpacesForV2(
                                review.targetComponents,
                                expected.transactionId, review.delta,
                            )
                        }
                        phase(
                            Phase1V2ReverseStep.SPACES_REVERTED,
                            Phase1V2ReverseStep.BEFORE_GROUPS,
                            Phase1V2ReverseStep.GROUPS_REVERTED,
                        ) {
                            val baseline = V298ReminderGroupCodec.decode(
                                review.originalComponents.groups,
                            )
                            V298ReminderGroupStore(app).mutate { baseline }
                        }
                        phase(
                            Phase1V2ReverseStep.GROUPS_REVERTED,
                            Phase1V2ReverseStep.BEFORE_SINGLE_ACTIVITY,
                            Phase1V2ReverseStep.SINGLE_ACTIVITY_REVERTED,
                        ) {
                            Phase1HabitStore(app).replaceFromApprovedV2OriginalRollback(
                                requireNotNull(
                                    review.originalComponents.singleActivityChallenges,
                                ),
                            )
                        }
                        phase(
                            Phase1V2ReverseStep.SINGLE_ACTIVITY_REVERTED,
                            Phase1V2ReverseStep.BEFORE_LEGACY_CHALLENGES,
                            Phase1V2ReverseStep.LEGACY_CHALLENGES_REVERTED,
                        ) {
                            V297ChallengeStore(app).replaceFromApprovedImport(
                                review.originalComponents.challenges,
                            )
                        }
                        phase(
                            Phase1V2ReverseStep.LEGACY_CHALLENGES_REVERTED,
                            Phase1V2ReverseStep.BEFORE_PERSONAL,
                            Phase1V2ReverseStep.PERSONAL_REVERTED,
                        ) {
                            PersonalReminderStore(app).replaceFromApprovedImport(
                                review.originalComponents.personal,
                            )
                        }
                        val checkpoint = requireNotNull(reverse.inspect())
                        check(checkpoint.step in setOf(
                            Phase1V2ReverseStep.PERSONAL_REVERTED,
                            Phase1V2ReverseStep.ORIGINAL_RECORDS_VERIFIED,
                        )) {
                            "Personal inverse did not complete all six data phases"
                        }
                        val live = V301LocalRecoveryReader(app)
                            .readForInterruptedV2Recovery()
                        checkKnownPersonalState(review, live)
                        val digest = Phase1V2RestoreFingerprint.calculate(
                            live.components, live.photos,
                        )
                        check(digest == review.delta.originalDigest &&
                            live.photos.sortedBy { it.id } ==
                                review.originalPhotoRows.sortedBy { it.id } &&
                            h4.requireCheckpointReadOnly() == expected &&
                            decisions.readOnlyStatus() ==
                                V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision) &&
                            sha256(originalZip) == expected.escrowSha256 &&
                            sha256(selectedZip) == expected.sourceSha256) {
                            "Personal inverse did not reproduce original owner records/Done receipts"
                        }
                        // Imported JPEG bytes intentionally remain in place
                        // as preserved owner recovery evidence; native and
                        // combined recovery journals remain BLOCKING.
                        val finalStage = if (
                            checkpoint.step == Phase1V2ReverseStep.PERSONAL_REVERTED
                        ) reverse.advance(
                            checkpoint, Phase1V2ReverseStep.ORIGINAL_RECORDS_VERIFIED,
                        ) else checkpoint
                        V375PersonalOriginalRecordsReceipt(
                            transactionId = expected.transactionId,
                            originalPersonalDigest = digest,
                            reversedReminderCount = review.delta.addedReminderIds.size,
                            reversedHabitCount = review.delta.addedHabitIds.size,
                            reversedSpaceCount = review.delta.addedSpaceIds.size,
                            importedPhotoRowsRemoved = review.delta.addedPhotoIds.size,
                            originalPhotoRowsRetained = review.originalPhotoRows.size,
                            reverseStage = finalStage.step,
                        )
                    }
            }
        }
    }

    private fun checkKnownPersonalState(
        reviewed: V374UnifiedPersonalOriginalRollbackReview,
        snapshot: com.framebynavin.app.personal.V301LocalRecoverySnapshot,
    ) {
        val checkpoint = com.framebynavin.app.personal.Phase1V2RestoreCheckpoint(
            transactionId = reviewed.nativeV2TransactionId,
            preparedId = "00000000-0000-0000-0000-000000000000",
            escrowId = "00000000-0000-0000-0000-000000000000",
            originalDigest = reviewed.delta.originalDigest,
            targetDigest = reviewed.delta.mergedTargetDigest,
            step = reviewed.nativeStep,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
        Phase1V2RestoreDomainForensics.inspectWithReverseJournal(
            checkpoint, reviewed.originalComponents,
            reviewed.targetComponents, snapshot.components,
            reviewed.originalPhotoRows, reviewed.targetPhotoRows,
            snapshot.photos,
        )
        // At a reverse checkpoint, target-added photos may be unreferenced
        // but still physically present. Verify each ORIGINAL JPEG and every
        // imported final/pending file using the native byte-forensic rules.
        Phase1V2PhotoByteForensics.inspect(
            Phase1V2RestoreStep.BEFORE_PHOTOS,
            File(app.filesDir, "backlot-personal-space-photos"),
            reviewed.originalPhotoRows, reviewed.targetPhotoRows,
            snapshot.photos,
        )
    }

    private fun ownerArchive(folderName: String, filename: String): File {
        val folder = File(app.filesDir, folderName)
        val file = File(folder, filename)
        check(folder.isDirectory && !Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == folder.canonicalFile) {
            "Owner ZIP is untrusted or points outside private recovery folder"
        }
        return file
    }

    private fun sha256(file: File): String {
        val sha = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val data = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(data)
                if (n < 0) break
                if (n > 0) sha.update(data, 0, n)
            }
        }
        return sha.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
}
