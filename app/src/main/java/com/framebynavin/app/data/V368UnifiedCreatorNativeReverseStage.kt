package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.LinkOption
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 Task 2 INTERNAL, owner-approved native Creator rollback for ONE narrow
 * interrupted case: H4 BEFORE_CREATOR, a fully intact and authenticated native
 * Creator pending original, authentic target/original encrypted archives,
 * original Voice Idea files byte-identical to the original escrow, and a
 * DURABLE explicit REVERSE_ORIGINAL owner decision.
 *
 * This intentionally reuses CreatorBackupManager's existing native durable
 * rollback rather than inventing a second record/Best Frame importer. It
 * writes Creator records and restores Best Frame order ONLY while Personal's
 * process lock and the exact H4-scoped Creator writer are both held.
 *
 * ABSOLUTELY NO standalone replay, forward-target Creator import, Voice Idea
 * orphan cleanup, Personal mutation, cross-domain checkpoint advancement,
 * alarm re-arm or H4/escrow/decision journal release is permitted here.
 *
 * A crash or failure retains the H4 and V347 owner-direction journals,
 * original/target sealed ZIPs, V365 Voice allocation and the native recovery
 * evidence when present. Even an intact Creator rollback is NOT a combined
 * restore and must wait for Task 3/4 reconciliation and owner approval.
 *
 * NOT CONNECTED TO UI, NOT CALLED BY STARTUP/WORKERS.
 */
internal data class V368CreatorOriginalReverseReceipt(
    val transactionId: String,
    val verifiedCreatorOriginalSha256: String,
    val verifiedVoiceIdeaCount: Int,
    val verifiedOriginalBestFrames: Int,
    val restoredNativeOriginal: Boolean,
    val combinedRecoveryStillPending: Boolean = true,
) {
    init {
        require(verifiedCreatorOriginalSha256.matches(Regex("[a-f0-9]{64}")) &&
            verifiedVoiceIdeaCount >= 0 && verifiedOriginalBestFrames >= 0 &&
            restoredNativeOriginal && combinedRecoveryStillPending)
    }
    val mayReleaseJournal: Boolean get() = false
    val mayDeleteAudio: Boolean get() = false
    val mayScheduleAlarms: Boolean get() = false
    val mayClaimEntireAppRestored: Boolean get() = false
}

internal object V368CreatorOriginalReversePolicy {
    fun mayBegin(
        checkpoint: V344UnifiedRestoreCheckpoint,
        ownerDecision: V347OwnerRecoveryDecision,
        source: V346InterruptedRecoveryProof,
        native: V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal,
        voice: V366CreatorVoiceRecoveryProof,
        original: V367OriginalVoiceInventory,
        ownerConfirmed: Boolean,
    ): Boolean {
        val pair = source.sealedArchives
        return ownerConfirmed &&
            checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(ownerDecision, checkpoint) &&
            ownerDecision.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V346InterruptedRecoveryProofPolicy.matches(source, checkpoint) &&
            source.creatorStandaloneRecoveryMarker &&
            !source.personalStandaloneRecoveryMarker &&
            ownerDecision.targetCreatorSha256 == pair.targetCreatorSha256 &&
            ownerDecision.targetPersonalSha256 == pair.targetPersonalSha256 &&
            ownerDecision.originalCreatorSha256 == pair.originalCreatorSha256 &&
            ownerDecision.originalPersonalSha256 == pair.originalPersonalSha256 &&
            native.combinedTransactionId == checkpoint.transactionId &&
            native.originalCreatorFingerprint == checkpoint.creatorOriginalSha256 &&
            native.voice == voice.installedCopies &&
            voice.transactionId == checkpoint.transactionId &&
            voice.originalCreatorSha256 == pair.originalCreatorSha256 &&
            voice.targetCreatorSha256 == pair.targetCreatorSha256 &&
            voice.noMutationAuthority &&
            voice.originalMediaAuthenticated &&
            voice.targetAllocationAuthenticated &&
            original.verifiedOriginalVoiceCount == voice.originalMediaCount &&
            original.noMutationAuthority &&
            !original.mayRollbackCreator
    }
}

internal class V368UnifiedCreatorNativeReverseStage(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val native = V364UnifiedCreatorPendingRecoveryProof(app)
    private val originalVoice = V367UnifiedCreatorOriginalVoiceSafety(app)
    private val decisions = V347OwnerRecoveryDecisionJournal(app)

    /**
     * Task 3, supported whole-account reverse case: H4 BEFORE_CREATOR
     * guarantees Personal's write stage never began. Native Creator may be
     * partly written, or still precisely ORIGINAL with preinstalled target
     * Voice Idea files. Return ORIGINAL + ORIGINAL only after BOTH domains,
     * original recordings, Space JPEGs and encrypted sources match H4.
     *
     * No imported target file is removed; H4/V347/V365, target ZIP and
     * ORIGINAL encrypted rollback escrow remain durable and blocking. Task 4
     * must separately decide verified journal/media release. This does not
     * run if a Personal write or later H4 stage has started.
     */
    internal suspend fun restoreBothOriginalBeforePersonalWritesInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V368CrossDomainOriginalReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Explicit owner rollback consent and archive password are required"
        }
        check(!V371UnifiedCreatorVoiceShadowJournal(app).hasBlockingEvidence()) {
            "Unresolved Voice shadow evidence requires separate owner review"
        }
        val expected = journal.requireCheckpointReadOnly()
        require(expected.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "This whole-domain rollback is only for BEFORE_CREATOR"
        }
        val direction = (decisions.readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("No intact owner reverse-original approval")
        check(direction.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(direction, expected) &&
            !IdeaProjectConversion(app).hasPendingRecovery()) {
            "The original owner reverse decision or conversion evidence is unsafe"
        }
        val sealed = V345UnifiedAuthenticatedRestoreParts(app)
            .authenticateForInterruptedRecovery(
                ownerConfirmedReview = true, passphrase = passphrase,
            )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
            sealed, expected,
        )) {
            "Authenticated owner ORIGINAL or TARGET escrow changed"
        }
        val voice = V366UnifiedCreatorVoiceRecoveryInspector(app)
            .inspectOwnerReviewed(ownerConfirmed = true, passphrase = passphrase)
        check(voice.transactionId == expected.transactionId &&
            voice.originalMediaAuthenticated && voice.targetAllocationAuthenticated) {
            "ORIGINAL Creator Voice Idea recordings were not authenticated"
        }

        // Preflight Personal independently of potentially corrupt Creator
        // Best Frames. All Personal records, exact Done instants and Space
        // photo BYTE hashes must still represent the ORIGINAL owner.
        suspend fun inspectOriginalPersonal(): String =
            StabilizationPersonalMutationGate.withPendingCombinedReadInspection(
                app, expected,
            ) {
                CreatorDataGate.withPendingCombinedReadInspection(app, expected) {
                    val personal = com.framebynavin.app.personal.V301LocalRecoveryReader(app)
                        .readForInterruptedV2Recovery()
                    val hash = V343UnifiedBaselineReader(app)
                        .personalFingerprintForInterrupted(personal)
                    check(hash == expected.personalOriginalSha256 &&
                        V354UnifiedLiveSpacePhotoAudit(app)
                            .inspect(personal.photos)
                            .currentPhotoFilesMatchTheirLiveRows) {
                        "Personal reminders, challenges, Done history or photos are not original"
                    }
                    hash
                }
            }

        val beforePersonal = inspectOriginalPersonal()
        val first = native.inspectReadOnly()
        check(V368CrossDomainOriginalPolicy.canReverseBeforePersonal(
            checkpoint = expected,
            decision = direction,
            native = first,
            voice = voice,
            originalPersonalFingerprint = beforePersonal,
        )) {
            "Creator or Personal original rollback evidence is stale or incomplete"
        }
        val nativeRewound = when (first) {
            is V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal -> {
                restoreOriginalNativeInternalOnly(
                    freshOwnerConsent = true, passphrase = passphrase,
                )
                true
            }
            is V364UnifiedCreatorPendingRecoveryProof.Assessment
                .VoiceAllocatedBeforeNativeCreatorJournal -> false
            else -> error("No authenticated, reversible native Creator crash state")
        }
        // An unrelated or torn native journal must not be reinterpreted as a
        // clean completion. H4 is intentionally not advanced or deleted.
        val afterNative = native.inspectReadOnly() as?
            V364UnifiedCreatorPendingRecoveryProof.Assessment
                .VoiceAllocatedBeforeNativeCreatorJournal
            ?: error("Native Creator rollback left unresolved original recovery evidence")
        check(afterNative.voice.transactionId == expected.transactionId &&
            afterNative.voice == voice.installedCopies &&
            journal.requireCheckpointReadOnly() == expected &&
            decisions.readOnlyStatus() ==
                V347OwnerRecoveryDecisionJournal.ReadState.Recorded(direction)) {
            "Native Creator rollback changed owner decision or Voice allocation"
        }

        val (creatorHash, personalHash, frames) =
            StabilizationPersonalMutationGate.withPendingCombinedReadInspection(
                app, expected,
            ) {
                CreatorDataGate.withPendingCombinedReadInspection(app, expected) {
                    val currentCreator = CreatorBackupManager(app)
                        .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                    val currentPersonal =
                        com.framebynavin.app.personal.V301LocalRecoveryReader(app)
                            .readForInterruptedV2Recovery()
                    val fingerprints = V343UnifiedBaselineReader(app)
                        .fingerprintUnverifiedInterrupted(
                            currentCreator, currentPersonal,
                        )
                    val originalFrames = CreatorHeroArchive.captureOriginalOrder(app)
                    val inventory = originalVoice.inspectOriginalIdeaReferencesReadOnly(
                        ideasJson = JSONObject(currentCreator).getString("ideas"),
                        expectedAudio = voice.originalMediaHashes,
                        voiceDir = File(app.filesDir, "voice_ideas"),
                    )
                    check(fingerprints.creatorSha256 == expected.creatorOriginalSha256 &&
                        fingerprints.personalSha256 == expected.personalOriginalSha256 &&
                        fingerprints.personalSha256 == beforePersonal &&
                        V354UnifiedLiveSpacePhotoAudit(app)
                            .inspect(currentPersonal.photos)
                            .currentPhotoFilesMatchTheirLiveRows &&
                        inventory.verifiedOriginalVoiceCount == voice.originalMediaCount) {
                        "Creator or Personal ORIGINAL was not fully restored"
                    }
                    Triple(fingerprints.creatorSha256,
                        fingerprints.personalSha256, JSONArray(originalFrames).length())
                }
            }
        check(journal.requireCheckpointReadOnly() == expected &&
            decisions.readOnlyStatus() ==
                V347OwnerRecoveryDecisionJournal.ReadState.Recorded(direction) &&
            V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
                V345UnifiedAuthenticatedRestoreParts(app)
                    .authenticateForInterruptedRecovery(
                        ownerConfirmedReview = true, passphrase = passphrase,
                    ),
                expected,
            )) {
            "Owner's rollback target or original encrypted escrow changed after verification"
        }
        V368CrossDomainOriginalReceipt(
            transactionId = expected.transactionId,
            originalCreatorFingerprint = creatorHash,
            originalPersonalFingerprint = personalHash,
            originalVoiceCount = voice.originalMediaCount,
            originalBestFrames = frames,
            nativeCreatorRewound = nativeRewound,
        )
    }

    /**
     * Task 3: return BOTH domains to ORIGINAL after the Creator forward stage
     * reached CREATOR_WRITTEN but BEFORE any Personal write began.
     *
     * Reuse CreatorBackupManager's own native pre-restore journal and the
     * ORIGINAL portable Creator ZIP. Original Voice Idea audio files must
     * already exist with the exact hashes from the encrypted ORIGINAL archive;
     * never create/delete them or import target audio during rollback.
     *
     * The ORIGINAL Best Frame files must still be present under their old,
     * cryptographically verified names; native import may briefly assign
     * fresh frame names but we restore the original verified pointer while
     * the same Creator write lock is held. Old and newly installed media stay
     * on disk: media cleanup/owner release belong to H3 Task 4.
     */
    internal suspend fun restoreBothOriginalFromCommittedCreatorInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V368CrossDomainOriginalReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Original Creator rollback requires fresh owner consent and passphrase"
        }
        val expected = journal.requireCheckpointReadOnly()
        require(expected.step == V344UnifiedRestoreStep.CREATOR_WRITTEN) {
            "This rollback cannot run once the Personal write stage has begun"
        }
        val decision = (decisions.readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("Owner reverse-original direction was not durably recorded")
        check(decision.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(decision, expected) &&
            !IdeaProjectConversion(app).hasPendingRecovery()) {
            "A stale or forward-only choice cannot restore original Creator records"
        }
        val proof = V345UnifiedAuthenticatedRestoreParts(app)
            .authenticateForInterruptedRecovery(
                ownerConfirmedReview = true, passphrase = passphrase,
            )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
            proof, expected,
        )) {
            "ORIGINAL and TARGET owner archives no longer match this transaction"
        }
        fun ownerArchive(folderName: String, fileName: String): File {
            val folder = File(app.filesDir, folderName)
            val file = File(folder, fileName)
            check(folder.isDirectory &&
                !Files.isSymbolicLink(folder.toPath()) &&
                folder.canonicalFile.parentFile == app.filesDir.canonicalFile &&
                file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.canonicalFile.parentFile == folder.canonicalFile) {
                "Original recovery ZIP is missing or points outside private storage"
            }
            return file
        }
        val originalFile = ownerArchive(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
        )
        val selectedFile = ownerArchive(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip",
        )
        fun sha(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count > 0) digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
        }
        check(sha(originalFile) == expected.escrowSha256 &&
            sha(selectedFile) == expected.sourceSha256) {
            "Rollback escrow or selected owner ZIP bytes have changed"
        }

        originalFile.inputStream().buffered().use { encrypted ->
            V341UnifiedOwnerArchive.stageAndVerify(
                encrypted, passphrase, app.cacheDir,
            ).use { authenticated ->
                check(sha(authenticated.creator) == proof.originalCreatorSha256) {
                    "The ORIGINAL Creator portion differs from owner escrow"
                }
                val portable = PortableCreatorBackupManager(app)
                authenticated.creator.inputStream().buffered().use {
                    portable.validate(it)
                }
                val staged = authenticated.creator.inputStream().buffered().use {
                    PortableBackupArchive.stage(it, app.cacheDir)
                }
                try {
                    check(staged.isArchive) {
                        "Exact owner original recovery requires portable media ZIP"
                    }
                    val frameCount = PortableBestFrameMedia.verifyCompactPreview(
                        staged.backupJson, staged.bestFrames, staged.hasFrameManifest,
                    )
                    check(frameCount != null ||
                        (staged.bestFrames.isEmpty() &&
                            JSONObject(staged.backupJson)
                                .getString("personalFrames") == "[]")) {
                        "Original Best Frames cannot be restored from ambiguous legacy media"
                    }
                    val originalFrames = JSONArray()
                    val frameDirectory = File(app.filesDir, "best_frames_original")
                    staged.bestFrames.forEach { image ->
                        val local = File(frameDirectory, image.name)
                        check(frameDirectory.isDirectory &&
                            !Files.isSymbolicLink(frameDirectory.toPath()) &&
                            !Files.isSymbolicLink(local.toPath()) &&
                            local.isFile &&
                            local.canonicalFile.parentFile ==
                                frameDirectory.canonicalFile &&
                            sha(local) == image.sha256) {
                            "Old original Best Frame file must still be present and unchanged"
                        }
                        originalFrames.put(JSONObject().put("name", image.name)
                            .put("sha256", image.sha256))
                    }
                    val originalFrameRecord = originalFrames.toString()
                    check(CreatorHeroArchive.verifyOriginalOrderSourceFiles(
                        app, originalFrameRecord,
                    ).size == staged.bestFrames.size &&
                        V343UnifiedBaselineReader(app)
                            .creatorFingerprintFromNativeOriginal(
                                staged.backupJson, originalFrameRecord,
                            ) == expected.creatorOriginalSha256) {
                        "Authenticated ORIGINAL Creator metadata/Best Frames do not match H4"
                    }
                    val originalHashes = staged.mediaByIdeaId.mapValues { (_, file) ->
                        check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                            file.length() in 1L..(512L * 1024L * 1024L)) {
                            "Unsafe original staged Voice Idea recording"
                        }
                        V366VoiceBytes(file.length(), sha(file))
                    }
                    val originalIdeas = JSONObject(staged.backupJson)
                        .getString("ideas")
                    val inventory = originalVoice.inspectOriginalIdeaReferencesReadOnly(
                        ideasJson = originalIdeas,
                        expectedAudio = originalHashes,
                        voiceDir = File(app.filesDir, "voice_ideas"),
                    )
                    check(inventory.verifiedOriginalVoiceCount ==
                        staged.mediaByIdeaId.size) {
                        "Original local Voice recordings differ from the escrow ZIP"
                    }

                    StabilizationPersonalMutationGate.withPendingCombinedReadInspection(
                        app, expected,
                    ) {
                        val personal = com.framebynavin.app.personal.V301LocalRecoveryReader(
                            app,
                        ).readForInterruptedV2Recovery()
                        check(V343UnifiedBaselineReader(app)
                            .personalFingerprintForInterrupted(personal) ==
                                expected.personalOriginalSha256 &&
                            V354UnifiedLiveSpacePhotoAudit(app)
                                .inspect(personal.photos)
                                .currentPhotoFilesMatchTheirLiveRows) {
                            "Personal data or photos changed before committed Creator rollback"
                        }
                        CreatorDataGate.withJournalBoundCommittedCreatorReverse(
                            app, expected, ownerConfirmed = true,
                        ) {
                            val nativeRoot = File(
                                app.filesDir, "creator_restore_recovery_v181",
                            )
                            check(listOf("pending.json", "pending.json.new",
                                "pending.json.bak").none {
                                    Files.exists(
                                        File(nativeRoot, it).toPath(),
                                        LinkOption.NOFOLLOW_LINKS,
                                    )
                                } &&
                                !IdeaProjectConversion(app).hasPendingRecovery() &&
                                journal.requireCheckpointReadOnly() == expected &&
                                decisions.readOnlyStatus() ==
                                    V347OwnerRecoveryDecisionJournal.ReadState.Recorded(
                                        decision,
                                    ) &&
                                sha(originalFile) == expected.escrowSha256 &&
                                sha(selectedFile) == expected.sourceSha256) {
                                "Competing native Creator recovery, H4 or owner ZIP changed"
                            }
                            val currentCreator = CreatorBackupManager(app)
                                .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                            val currentFrameFingerprint = runCatching {
                                V343UnifiedBaselineReader(app)
                                    .creatorFingerprintFromNativeOriginal(
                                        currentCreator,
                                        CreatorHeroArchive.captureOriginalOrder(app),
                                    )
                            }.getOrNull()
                            val requiredWrite = currentFrameFingerprint !=
                                expected.creatorOriginalSha256
                            if (requiredWrite) {
                                val manager = CreatorBackupManager(app)
                                if (frameCount != null) {
                                    manager.restorePortableCompact(
                                        staged.backupJson, staged.bestFrames,
                                    )
                                } else {
                                    manager.restore(staged.backupJson)
                                }
                                // This changes only the original-frame ORDER
                                // pointer; authenticated original image bytes
                                // already exist under original names.
                                CreatorHeroArchive.restoreOriginalOrder(
                                    app, originalFrameRecord,
                                )
                            }
                            val current = CreatorBackupManager(app)
                                .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                            val livePersonal =
                                com.framebynavin.app.personal.V301LocalRecoveryReader(app)
                                    .readForInterruptedV2Recovery()
                            val originalNow = V343UnifiedBaselineReader(app)
                                .fingerprintUnverifiedInterrupted(
                                    current, livePersonal,
                                )
                            check(originalNow.creatorSha256 ==
                                expected.creatorOriginalSha256 &&
                                originalNow.personalSha256 ==
                                    expected.personalOriginalSha256 &&
                                V354UnifiedLiveSpacePhotoAudit(app)
                                    .inspect(livePersonal.photos)
                                    .currentPhotoFilesMatchTheirLiveRows &&
                                originalVoice.inspectOriginalIdeaReferencesReadOnly(
                                    ideasJson = JSONObject(current).getString("ideas"),
                                    expectedAudio = originalHashes,
                                    voiceDir = File(app.filesDir, "voice_ideas"),
                                ).verifiedOriginalVoiceCount == inventory.verifiedOriginalVoiceCount &&
                                journal.requireCheckpointReadOnly() == expected &&
                                decisions.readOnlyStatus() ==
                                    V347OwnerRecoveryDecisionJournal.ReadState.Recorded(
                                        decision,
                                    ) &&
                                sha(originalFile) == expected.escrowSha256 &&
                                sha(selectedFile) == expected.sourceSha256) {
                                "Committed Creator rollback did not restore both ORIGINAL domains"
                            }
                            V368CrossDomainOriginalReceipt(
                                transactionId = expected.transactionId,
                                originalCreatorFingerprint = originalNow.creatorSha256,
                                originalPersonalFingerprint = originalNow.personalSha256,
                                originalVoiceCount = inventory.verifiedOriginalVoiceCount,
                                originalBestFrames = staged.bestFrames.size,
                                nativeCreatorRewound = requiredWrite,
                            )
                        }
                    }
                } finally {
                    staged.cleanup()
                }
            }
        }
    }

    /**
     * A strictly limited Creator-domain rollback. This is not a substitute
     * for Task 3's complete original Creator+Personal rollback protocol.
     */
    internal suspend fun restoreOriginalNativeInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V368CreatorOriginalReverseReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Fresh owner consent and encrypted recovery passphrase required"
        }
        check(!V371UnifiedCreatorVoiceShadowJournal(app).hasBlockingEvidence()) {
            "Unresolved Voice Idea shadow journal needs separate owner review"
        }
        val checkpoint = journal.requireCheckpointReadOnly()
        require(checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "The Creator domain is not at an interrupted native restore stage"
        }

        // Authenticate both complete encrypted originals and target while
        // not holding the two potentially long-lived owner database mutexes.
        val sealed = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        val voice = V366UnifiedCreatorVoiceRecoveryInspector(app)
            .inspectOwnerReviewed(ownerConfirmed = true, passphrase = passphrase)
        check(!IdeaProjectConversion(app).hasPendingRecovery()) {
            "An independent Creator conversion recovery must be reviewed first"
        }
        val originalNative = native.inspectReadOnly() as?
            V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal
            ?: error("Native Creator original is absent, torn or unbound")
        val verifiedOriginal = originalVoice.inspectNativeOriginalReadOnly(
            originalNative, voice,
        )
        val decision = (decisions.readOnlyStatus() as?
            V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
            ?: error("Owner has not durably selected a valid reverse-original direction")

        check(V368CreatorOriginalReversePolicy.mayBegin(
            checkpoint, decision, sealed, originalNative, voice,
            verifiedOriginal, freshOwnerConsent,
        )) {
            "Creator reverse recovery does not match immutable owner evidence"
        }

        // PERSONAL first, CREATOR second. The H4 checkpoint must remain
        // BEFORE_CREATOR throughout; native recovery may clear only its OWN
        // pending restore marker, never the combined H4 transaction.
        StabilizationPersonalMutationGate.withCombinedCreatorNativeReverseFence(
            app, checkpoint, ownerConfirmed = true,
        ) {
            CreatorDataGate.withJournalBoundCreatorWrite(
                app, checkpoint, ownerConfirmed = true,
            ) {
                check(journal.requireCheckpointReadOnly() == checkpoint &&
                    decisions.readOnlyStatus() ==
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision)) {
                    "Combined journal or owner reverse decision changed"
                }
                check(!V371UnifiedCreatorVoiceShadowJournal(app).hasBlockingEvidence()) {
                    "A shadow recording allocation appeared before Creator rollback"
                }
                check(!IdeaProjectConversion(app).hasPendingRecovery()) {
                    "Competing Creator conversion evidence appeared before native rollback"
                }
                val currentNative = native.inspectReadOnly() as?
                    V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal
                    ?: error("Creator native recovery evidence changed before rollback")
                val currentVoice = V365UnifiedCreatorVoiceInstallJournal(app)
                    .inspectReadOnly()
                    ?: error("Creator Voice allocation is missing")
                check(currentNative == originalNative &&
                    currentVoice.transactionId == checkpoint.transactionId &&
                    V365UnifiedCreatorVoiceInstallJournal(app).auditReadOnly(currentVoice) ==
                        voice.installedCopies) {
                    "Creator original, target allocation or installed media changed"
                }
                val local = originalVoice.inspectNativeOriginalReadOnly(
                    currentNative, voice,
                )
                check(V368CreatorOriginalReversePolicy.mayBegin(
                    checkpoint, decision, sealed, currentNative, voice,
                    local, ownerConfirmed = true,
                )) { "Owner recovery preconditions changed after acquiring domain locks" }

                // Existing native recovery validates its pending JSON and
                // Best Frame rollback source, restores the original records,
                // suppresses Creator alarm scheduling under this H4 scope,
                // then clears only the native Creator pending.json.
                check(CreatorBackupManager(app).recoverPendingRestore()) {
                    "No bound native Creator recovery was performed"
                }

                // This is NOT a combined-restored claim: prove original
                // Creator metadata, original Best Frame bytes, and references
                // to original Voice Idea recordings after native rollback.
                val currentCreator = CreatorBackupManager(app)
                    .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                val currentFrames = CreatorHeroArchive.captureOriginalOrder(app)
                val fingerprint = V343UnifiedBaselineReader(app)
                    .creatorFingerprintFromNativeOriginal(
                        currentCreator, currentFrames,
                    )
                check(fingerprint == checkpoint.creatorOriginalSha256) {
                    "Native Creator rollback did not reproduce the saved original"
                }
                originalVoice.inspectOriginalIdeaReferencesReadOnly(
                    ideasJson = JSONObject(currentCreator).getString("ideas"),
                    expectedAudio = voice.originalMediaHashes,
                    voiceDir = File(app.filesDir, "voice_ideas"),
                )
                check(journal.requireCheckpointReadOnly() == checkpoint &&
                    decisions.readOnlyStatus() ==
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision)) {
                    "Creator rollback evidence changed during original verification"
                }

                V368CreatorOriginalReverseReceipt(
                    transactionId = checkpoint.transactionId,
                    verifiedCreatorOriginalSha256 = fingerprint,
                    verifiedVoiceIdeaCount = local.verifiedOriginalVoiceCount,
                    verifiedOriginalBestFrames = currentNative.verifiedOriginalBestFrameCount,
                    restoredNativeOriginal = true,
                )
            }
        }
    }
}


/** A verified original state is NOT authority to erase recovery evidence. */
internal data class V368CrossDomainOriginalReceipt(
    val transactionId: String,
    val originalCreatorFingerprint: String,
    val originalPersonalFingerprint: String,
    val originalVoiceCount: Int,
    val originalBestFrames: Int,
    val nativeCreatorRewound: Boolean,
) {
    init {
        require(transactionId.isNotBlank() &&
            originalCreatorFingerprint.matches(Regex("[a-f0-9]{64}")) &&
            originalPersonalFingerprint.matches(Regex("[a-f0-9]{64}")) &&
            originalVoiceCount >= 0 && originalBestFrames in 0..10)
    }
    val bothOriginalsVerified: Boolean get() = true
    val mayDeleteMedia: Boolean get() = false
    val mayReleaseH4OrEscrow: Boolean get() = false
    val mayEnableAlarms: Boolean get() = false
    val mayClaimRestoredToOwner: Boolean get() = false
}


/**
 * Task 3: this is the ONLY H4 stage where Personal can be proven unchanged
 * without running an inverse Personal writer. An interrupted Creator restore
 * must have a bound original native marker OR an immutable pre-native Voice
 * allocation; an invalid or unrelated journal is never considered original.
 */
internal object V368CrossDomainOriginalPolicy {
    fun canReverseBeforePersonal(
        checkpoint: V344UnifiedRestoreCheckpoint,
        decision: V347OwnerRecoveryDecision,
        native: V364UnifiedCreatorPendingRecoveryProof.Assessment,
        voice: V366CreatorVoiceRecoveryProof,
        originalPersonalFingerprint: String,
    ): Boolean =
        checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR &&
            decision.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL &&
            V347OwnerRecoveryDecisionPolicy.remainsBound(decision, checkpoint) &&
            originalPersonalFingerprint == checkpoint.personalOriginalSha256 &&
            voice.transactionId == checkpoint.transactionId &&
            voice.originalMediaAuthenticated && voice.targetAllocationAuthenticated &&
            voice.noMutationAuthority &&
            when (native) {
                is V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal ->
                    native.combinedTransactionId == checkpoint.transactionId &&
                        native.originalCreatorFingerprint ==
                            checkpoint.creatorOriginalSha256 &&
                        native.voice == voice.installedCopies
                is V364UnifiedCreatorPendingRecoveryProof.Assessment
                    .VoiceAllocatedBeforeNativeCreatorJournal ->
                    native.voice == voice.installedCopies
                else -> false
            }
}
