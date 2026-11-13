package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V301LocalRecoveryReader
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * H3 Task 2: owner-approved installation of clean replacement audio beside
 * immutable PARTIAL Voice Idea files. This is NOT a Creator restore.
 *
 * Requires H4 BEFORE_CREATOR, a V347 owner FORWARD_TARGET choice, a verified
 * no-native-journal/original-state report (V369), authenticated complete
 * TARGET+ORIGINAL encrypted archives (V345/V366), and an exact V365 plan.
 * Holds Personal lock FIRST then the scoped Creator writer SECOND and checks
 * current Creator/Personal data, original audio and Space photo bytes against
 * their ORIGINAL escrow fingerprints before V371 can reserve/copy anything.
 *
 * Only the narrow case with already-present (matching or altered) allocated
 * Voice files is supported here. Mixed MISSING+ALTERED reservations remain
 * untouched for future coherent media recovery; V370's missing-only path
 * remains separate. No alternate filename enters live Creator records, and
 * original partial media and both sealed owner ZIPs remain untouched.
 *
 * Safe cold retry: an existing V371 journal is verified and reused; a
 * missing shadow is CREATE_NEW-copied, a matching shadow is left alone, and
 * a changed/partial shadow is preserved and blocks retry. Owner must freshly
 * confirm/passphrase every invocation. No UI/worker/startup callers.
 */
internal data class V372ShadowReplayReceipt(
    val transactionId: String,
    val damagedOriginalsRetained: Int,
    val shadowsVerified: Int,
    val newShadowsCopied: Int,
    val creatorAndPersonalOriginalsUntouched: Boolean = true,
    val combinedRecoveryPending: Boolean = true,
) {
    init {
        require(transactionId.isNotBlank() &&
            damagedOriginalsRetained >= 1 &&
            shadowsVerified == damagedOriginalsRetained &&
            newShadowsCopied in 0..shadowsVerified &&
            creatorAndPersonalOriginalsUntouched &&
            combinedRecoveryPending)
    }
    val mayImportCreator: Boolean get() = false
    val mayDeletePartialAudio: Boolean get() = false
    val mayAdvanceH4: Boolean get() = false
    val mayRearmAlarms: Boolean get() = false
    val mayReleaseOriginalEscrow: Boolean get() = false
}

internal object V372ShadowReplayPolicy {
    fun eligibleOriginalAllocation(
        audit: V365VoiceCopyAudit,
        confirmedOriginalState: Boolean,
    ): Boolean =
        confirmedOriginalState && audit.noWriteAuthority &&
            audit.planned > 0 &&
            audit.alteredOrPartialCopies > 0 &&
            audit.unsafeCopies == 0 &&
            audit.missingCopies == 0 &&
            audit.matchingCopies + audit.alteredOrPartialCopies == audit.planned

    fun eligiblePreCopy(audit: V371VoiceShadowAudit): Boolean =
        audit.planned > 0 && audit.unsafe == 0 &&
            audit.alteredOrPartial == 0 &&
            audit.matching + audit.missing == audit.planned
}

internal class V372UnifiedCreatorShadowReplayCoordinator(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val originalInstall = V365UnifiedCreatorVoiceInstallJournal(app)
    private val shadowInstall = V371UnifiedCreatorVoiceShadowJournal(app)
    private val ownerDecision = V347OwnerRecoveryDecisionJournal(app)
    private val native = V364UnifiedCreatorPendingRecoveryProof(app)

    internal suspend fun installShadowsInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V372ShadowReplayReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Fresh owner confirmation and 12+ character archive passphrase required"
        }
        val cp = h4.requireCheckpointReadOnly()
        check(cp.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Shadow Voice recovery requires H4 BEFORE_CREATOR"
        }
        // Authenticating these sealed ZIPs must never silently mutate a live
        // Creator/Personal domain. A no-native marker is NOT proof of original.
        val originalReview = V369UnifiedCreatorPreNativeOriginalInspector(app)
            .inspectInternalOwnerReviewed(
                freshOwnerConsent = true, passphrase = passphrase,
                expectedDirection = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
            )
        check(V372ShadowReplayPolicy.eligibleOriginalAllocation(
            originalReview.allocatedTargetCopies,
            confirmedOriginalState = originalReview.transactionId == cp.transactionId,
        )) { "Missing/unsafe Voice files or unverified original data block shadow replay" }

        val audio = V366UnifiedCreatorVoiceRecoveryInspector(app)
            .inspectOwnerReviewed(ownerConfirmed = true, passphrase = passphrase)
        val sealed = V345UnifiedAuthenticatedRestoreParts(app)
            .authenticateForInterruptedRecovery(
                ownerConfirmedReview = true, passphrase = passphrase,
            )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
            sealed, cp,
        ) && audio.transactionId == cp.transactionId &&
            audio.targetCreatorSha256 == sealed.targetCreatorSha256 &&
            audio.originalCreatorSha256 == sealed.originalCreatorSha256 &&
            audio.installedCopies == originalReview.allocatedTargetCopies) {
            "Voice source or original escrow changed during owner review"
        }
        val source = ownerFile(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip",
        )
        val escrow = ownerFile(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
        )
        check(sha256(source) == cp.sourceSha256 &&
            sha256(escrow) == cp.escrowSha256) {
            "Authenticated original/target owner ZIPs changed before recovery"
        }

        source.inputStream().buffered().use { encrypted ->
            V341UnifiedOwnerArchive.stageAndVerify(
                encrypted, passphrase, app.cacheDir,
            ).use { unpacked ->
                check(sha256(unpacked.creator) == sealed.targetCreatorSha256) {
                    "TARGET Creator section is no longer authentic"
                }
                // Verify the full Creator ZIP, including Best Frames, before
                // any allocation or media installation can occur.
                unpacked.creator.inputStream().buffered().use {
                    PortableCreatorBackupManager(app).validate(it)
                }
                val staged = unpacked.creator.inputStream().buffered().use {
                    PortableBackupArchive.stage(it, app.cacheDir)
                }
                try {
                    check(staged.isArchive) {
                        "Owner Creator recovery requires portable media archive"
                    }
                    val allocation = originalInstall.inspectReadOnly()
                        ?: error("Original immutable Voice Idea plan is missing")
                    check(allocation.transactionId == cp.transactionId &&
                        allocation.sourceSha256 == cp.sourceSha256 &&
                        allocation.escrowSha256 == cp.escrowSha256 &&
                        V366CreatorVoiceRecoveryPolicy.matchesTarget(
                            allocation, staged.mediaByIdeaId.mapValues { (_, file) ->
                                check(Files.isRegularFile(
                                    file.toPath(), LinkOption.NOFOLLOW_LINKS,
                                ) && !Files.isSymbolicLink(file.toPath()) &&
                                    file.length() in 1L..(512L * 1024L * 1024L)) {
                                    "Untrusted staged Creator Voice Idea file"
                                }
                                V366VoiceBytes(file.length(), sha256(file))
                            },
                        )) {
                        "Target staged Voice Idea files disagree with V365 allocation"
                    }
                    val decision = (ownerDecision.readOnlyStatus() as?
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
                        ?: error("No valid owner-approved recovery direction")
                    check(decision.choice ==
                        V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
                        V347OwnerRecoveryDecisionPolicy.remainsBound(decision, cp)) {
                        "Shadow installation needs durable FORWARD_TARGET owner approval"
                    }

                    StabilizationPersonalMutationGate.withCombinedCreatorMissingVoiceFence(
                        app, cp, ownerConfirmed = true,
                    ) {
                        CreatorDataGate.withJournalBoundCreatorWrite(
                            app, cp, ownerConfirmed = true,
                        ) {
                            check(!IdeaProjectConversion(app).hasPendingRecovery() &&
                                h4.requireCheckpointReadOnly() == cp &&
                                ownerDecision.readOnlyStatus() ==
                                    V347OwnerRecoveryDecisionJournal.ReadState.Recorded(
                                        decision,
                                    ) &&
                                originalInstall.inspectReadOnly() == allocation &&
                                native.inspectReadOnly() is
                                    V364UnifiedCreatorPendingRecoveryProof.Assessment
                                        .VoiceAllocatedBeforeNativeCreatorJournal) {
                                "Creator recovery evidence changed before shadow repair"
                            }
                            val creatorJson = CreatorBackupManager(app)
                                .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                            val personal = V301LocalRecoveryReader(app)
                                .readForInterruptedV2Recovery()
                            val baseline = V343UnifiedBaselineReader(app)
                                .fingerprintUnverifiedInterrupted(creatorJson, personal)
                            val livePhotos = V354UnifiedLiveSpacePhotoAudit(app)
                                .inspect(personal.photos)
                            val originalVoice = V367UnifiedCreatorOriginalVoiceSafety(app)
                                .inspectOriginalIdeaReferencesReadOnly(
                                    ideasJson = JSONObject(creatorJson).getString("ideas"),
                                    expectedAudio = audio.originalMediaHashes,
                                    voiceDir = File(app.filesDir, "voice_ideas"),
                                )
                            check(baseline.creatorSha256 == cp.creatorOriginalSha256 &&
                                baseline.personalSha256 == cp.personalOriginalSha256 &&
                                originalVoice.ideaCount == originalReview.originalIdeaCount &&
                                originalVoice.verifiedOriginalVoiceCount ==
                                    originalReview.originalVoiceFileCount &&
                                livePhotos.currentPhotoFilesMatchTheirLiveRows &&
                                livePhotos.rows == originalReview.photoRowsVerified &&
                                sha256(source) == cp.sourceSha256 &&
                                sha256(escrow) == cp.escrowSha256) {
                                "Live Creator/Personal original state drifted before repair"
                            }
                            val preMedia = originalInstall.auditReadOnly(allocation)
                            check(V372ShadowReplayPolicy.eligibleOriginalAllocation(
                                preMedia, confirmedOriginalState = true,
                            ) && preMedia == audio.installedCopies) {
                                "Original partial media changed during authenticated review"
                            }
                            val plan = shadowInstall.inspectReadOnly()
                                ?: shadowInstall.reserveForCurrentCreatorWriter(
                                    allocation, staged.mediaByIdeaId,
                                )
                            V371VoiceShadowPolicy.validate(plan, allocation)
                            val preShadow = shadowInstall.auditShadowReadOnly(plan)
                            check(V372ShadowReplayPolicy.eligiblePreCopy(preShadow)) {
                                "Existing partial shadow must be retained for owner review"
                            }
                            val finalAudit =
                                shadowInstall.installMissingShadowsForCurrentOwnerWriter(
                                    plan, staged.mediaByIdeaId,
                                )
                            check(finalAudit.allVerified &&
                                finalAudit.planned == preMedia.alteredOrPartialCopies &&
                                originalInstall.auditReadOnly(allocation) == preMedia &&
                                h4.requireCheckpointReadOnly() == cp &&
                                ownerDecision.readOnlyStatus() ==
                                    V347OwnerRecoveryDecisionJournal.ReadState.Recorded(
                                        decision,
                                    ) &&
                                sha256(source) == cp.sourceSha256 &&
                                sha256(escrow) == cp.escrowSha256) {
                                "Shadow install must preserve damaged originals and owner ZIPs"
                            }
                            // Construct and re-sign the exact portable Creator
                            // target with SHADOW Voice references and verified
                            // Best Frames. This is a read-only preview only:
                            // no Creator DataStore/Hero import happens here.
                            val payload = PortableCreatorBackupManager(app)
                                .previewRecoveredCreatorPortable(
                                    staged, allocation, plan,
                                )
                            check(payload.transactionId == cp.transactionId &&
                                payload.restoredVoiceIdeaCount == allocation.entries.size &&
                                payload.shadowVoiceIdeaCount ==
                                    preMedia.alteredOrPartialCopies &&
                                payload.verifiedPortableFrameCount ==
                                    PortableBestFrameMedia.verifyCompactPreview(
                                        staged.backupJson, staged.bestFrames,
                                        staged.hasFrameManifest,
                                    ) &&
                                !payload.mayImportCreator &&
                                sha256(source) == cp.sourceSha256 &&
                                sha256(escrow) == cp.escrowSha256) {
                                "Verified Creator records, Voice refs or Best Frames changed"
                            }
                            // The voice shadow is NOT referenced by any live
                            // Creator Idea. H4 remains BEFORE_CREATOR, the
                            // encrypted ORIGINAL stays available for Task 3.
                            V372ShadowReplayReceipt(
                                transactionId = cp.transactionId,
                                damagedOriginalsRetained = preMedia.alteredOrPartialCopies,
                                shadowsVerified = finalAudit.matching,
                                newShadowsCopied = preShadow.missing,
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
     * Task 2 integration: finish an interrupted Creator import using the
     * EXISTING native CreatorBackupManager restore and Best Frame journal.
     * Both domains were ORIGINAL at preflight; Voice files must have been
     * completely repaired (V370) or have verified V371 shadows.
     *
     * The current combined H4 remains BEFORE_CREATOR until AFTER native
     * import and all live Creator/Personal/media proofs pass. Then and only
     * then the retained FORWARD_TARGET choice is carried to CREATOR_WRITTEN.
     *
     * A crash at any point still retains H4, the V347 owner choice,
     * encrypted original/target ZIPs, V365 and V371 media evidence.
     * No automatic retry, deletion or alarm scheduling is performed here.
     */
    internal suspend fun resumeVerifiedCreatorImportInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V357CreatorForwardReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Fresh owner confirmation and encrypted passphrase are required"
        }
        val cp = h4.requireCheckpointReadOnly()
        check(cp.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Only the interrupted Creator restore may use this stage"
        }
        val original = V369UnifiedCreatorPreNativeOriginalInspector(app)
            .inspectInternalOwnerReviewed(
                freshOwnerConsent = true, passphrase = passphrase,
                expectedDirection = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
            )
        val voiceProof = V366UnifiedCreatorVoiceRecoveryInspector(app)
            .inspectOwnerReviewed(ownerConfirmed = true, passphrase = passphrase)
        val paired = V345UnifiedAuthenticatedRestoreParts(app)
            .authenticateForInterruptedRecovery(
                ownerConfirmedReview = true, passphrase = passphrase,
            )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(paired, cp) &&
            original.transactionId == cp.transactionId &&
            voiceProof.transactionId == cp.transactionId &&
            original.allocatedTargetCopies == voiceProof.installedCopies) {
            "Creator resume requires unchanged original Creator and Personal data"
        }
        val source = ownerFile(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip",
        )
        val rollback = ownerFile(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
        )
        check(sha256(source) == cp.sourceSha256 &&
            sha256(rollback) == cp.escrowSha256) {
            "Owner original or target escrow changed before Creator recovery"
        }
        source.inputStream().buffered().use { encrypted ->
            V341UnifiedOwnerArchive.stageAndVerify(
                encrypted, passphrase, app.cacheDir,
            ).use { inner ->
                check(sha256(inner.creator) == paired.targetCreatorSha256) {
                    "Creator TARGET differs from independently authenticated owner backup"
                }
                val portable = PortableCreatorBackupManager(app)
                inner.creator.inputStream().buffered().use(portable::validate)
                val staged = inner.creator.inputStream().buffered().use {
                    PortableBackupArchive.stage(it, app.cacheDir)
                }
                try {
                    val allocation = originalInstall.inspectReadOnly()
                        ?: error("Immutable original Creator audio allocation disappeared")
                    val shadow = shadowInstall.inspectReadOnly()
                    val expected = (ownerDecision.readOnlyStatus() as?
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
                        ?: error("Durable owner FORWARD_TARGET choice is absent")
                    check(expected.choice == V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
                        V347OwnerRecoveryDecisionPolicy.remainsBound(expected, cp) &&
                        allocation.transactionId == cp.transactionId &&
                        allocation.sourceSha256 == cp.sourceSha256 &&
                        allocation.escrowSha256 == cp.escrowSha256) {
                        "Creator resume references the wrong owner recovery transaction"
                    }
                    // Source audit may contain damaged V365 files only when
                    // the matching, authenticated V371 shadows are complete.
                    // Refuse unknown legacy embedded image payloads: this
                    // stage can independently verify compact images or an
                    // empty Best Frame collection, never guessed frame bytes.
                    val preview = portable.previewRecoveredCreatorPortable(
                        staged, allocation, shadow,
                    )
                    check(preview.transactionId == cp.transactionId &&
                        preview.restoredVoiceIdeaCount == allocation.entries.size &&
                        (preview.verifiedPortableFrameCount != null ||
                            (staged.bestFrames.isEmpty() &&
                                org.json.JSONObject(staged.backupJson)
                                    .getString("personalFrames") == "[]"))) {
                        "Creator recovery needs verified portable Best Frame bytes"
                    }

                    StabilizationPersonalMutationGate
                        .withCombinedCreatorRecoveryFinishFence(
                            app, cp, ownerConfirmed = true,
                        ) {
                            val completed = CreatorDataGate.withJournalBoundCreatorWrite(
                                app, cp, ownerConfirmed = true,
                            ) {
                                check(h4.requireCheckpointReadOnly() == cp &&
                                    ownerDecision.readOnlyStatus() ==
                                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded(
                                            expected,
                                        ) &&
                                    !IdeaProjectConversion(app).hasPendingRecovery() &&
                                    originalInstall.inspectReadOnly() == allocation &&
                                    shadowInstall.inspectReadOnly() == shadow &&
                                    native.inspectReadOnly() is
                                        V364UnifiedCreatorPendingRecoveryProof.Assessment
                                            .VoiceAllocatedBeforeNativeCreatorJournal) {
                                    "Native Creator recovery or original media evidence changed"
                                }
                                val currentJson = CreatorBackupManager(app)
                                    .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                                val personal = V301LocalRecoveryReader(app)
                                    .readForInterruptedV2Recovery()
                                val before = V343UnifiedBaselineReader(app)
                                    .fingerprintUnverifiedInterrupted(currentJson, personal)
                                val photos = V354UnifiedLiveSpacePhotoAudit(app)
                                    .inspect(personal.photos)
                                val originalVoice =
                                    V367UnifiedCreatorOriginalVoiceSafety(app)
                                        .inspectOriginalIdeaReferencesReadOnly(
                                            ideasJson = org.json.JSONObject(currentJson)
                                                .getString("ideas"),
                                            expectedAudio = voiceProof.originalMediaHashes,
                                            voiceDir = File(app.filesDir, "voice_ideas"),
                                        )
                                check(before.creatorSha256 == cp.creatorOriginalSha256 &&
                                    before.personalSha256 == cp.personalOriginalSha256 &&
                                    photos.currentPhotoFilesMatchTheirLiveRows &&
                                    originalVoice.verifiedOriginalVoiceCount ==
                                        voiceProof.originalMediaCount &&
                                    originalInstall.auditReadOnly(allocation) ==
                                        original.allocatedTargetCopies) {
                                    "Live original Creator/Personal state changed before import"
                                }
                                val checked = portable.previewRecoveredCreatorPortable(
                                    staged, allocation, shadow,
                                )
                                check(checked == preview &&
                                    sha256(source) == cp.sourceSha256 &&
                                    sha256(rollback) == cp.escrowSha256) {
                                    "Authenticated Creator payload changed after acquiring locks"
                                }
                                val restored =
                                    if (preview.verifiedPortableFrameCount != null) {
                                        CreatorBackupManager(app).restorePortableCompact(
                                            preview.rewrittenCreatorJson, staged.bestFrames,
                                        )
                                    } else {
                                        CreatorBackupManager(app).restore(
                                            preview.rewrittenCreatorJson,
                                        )
                                    }

                                verifyInstalledCreatorTargetInsideOwnerGate(
                                    cp, preview, staged, allocation, shadow,
                                    expected, source, rollback,
                                )
                                restored
                            }
                            // No Creator writer remains after this boundary;
                            // Personal mutex is still held, normal writers
                            // remain frozen by the active H4 restore journal.
                            val next = h4.checkpoint(
                                cp, V344UnifiedRestoreStep.CREATOR_WRITTEN,
                                maxOf(System.currentTimeMillis(), cp.updatedAtMillis + 1L),
                                otherRecoveryPending = {
                                    !ownerDecision.matchesUnchangedForwardChoice(cp, expected)
                                },
                            )
                            check(ownerDecision.readOnlyStatus() ==
                                V347OwnerRecoveryDecisionJournal.ReadState
                                    .CreatorWrittenRetained(expected, next)) {
                                "Creator checkpoint lost its durable owner recovery direction"
                            }
                            V357CreatorForwardReceipt(
                                combinedTransactionId = cp.transactionId,
                                sourceSha256 = cp.sourceSha256,
                                checkpointReached = next.step,
                                restoredCreatorProjects = completed.projectCount,
                                personalRestored = false,
                                combinedRestoreComplete = false,
                            )
                        }
                } finally {
                    staged.cleanup()
                }
            }
        }
    }

    /**
     * The single shared post-native and post-crash target verifier.
     * Caller holds PERSONAL read fence then CREATOR owner writer. Returns
     * no new write authority or journal release permission.
     */
    private suspend fun verifyInstalledCreatorTargetInsideOwnerGate(
        cp: V344UnifiedRestoreCheckpoint,
        preview: V373RecoveredCreatorPayload,
        staged: StagedPortableBackup,
        allocation: V365VoiceCopyPlan,
        shadow: V371VoiceShadowPlan?,
        decision: V347OwnerRecoveryDecision,
        source: File,
        rollback: File,
    ) {
        check(CreatorDataGate.isJournalBoundCreatorWrite(app) &&
            StabilizationPersonalMutationGate
                .isWithinCombinedCreatorMissingVoiceFence(app, cp)) {
            "Installed Creator verification requires BOTH recovery domain locks"
        }
        val installedJson = CreatorBackupManager(app)
            .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
        val fingerprint = V343UnifiedBaselineReader(app)
        check(fingerprint.creatorRecordsFingerprintForRecovery(installedJson) ==
            fingerprint.creatorRecordsFingerprintForRecovery(
                preview.rewrittenCreatorJson,
            )) { "Restored Creator records differ from approved TARGET" }
        check(V373RecoveredCreatorMediaPolicy.liveFramesMatchVerifiedTarget(
            CreatorHeroArchive.captureOriginalOrder(app),
            staged.bestFrames, preview.verifiedPortableFrameCount,
        )) { "Restored Best Frame bytes or order differ from TARGET" }
        val installedVoice = V367UnifiedCreatorOriginalVoiceSafety(app)
            .inspectOriginalIdeaReferencesReadOnly(
                ideasJson = org.json.JSONObject(installedJson).getString("ideas"),
                expectedAudio = allocation.entries.associate {
                    it.ideaId to V366VoiceBytes(it.sizeBytes, it.sha256)
                },
                voiceDir = File(app.filesDir, "voice_ideas"),
            )
        check(installedVoice.verifiedOriginalVoiceCount == allocation.entries.size &&
            originalInstall.inspectReadOnly() == allocation &&
            shadowInstall.inspectReadOnly() == shadow) {
            "Restored Creator Voice Idea references are not authentic"
        }
        val personal = V301LocalRecoveryReader(app).readForInterruptedV2Recovery()
        val current = fingerprint.fingerprintUnverifiedInterrupted(
            installedJson, personal,
        )
        check(current.personalSha256 == cp.personalOriginalSha256 &&
            V354UnifiedLiveSpacePhotoAudit(app)
                .inspect(personal.photos).currentPhotoFilesMatchTheirLiveRows &&
            h4.requireCheckpointReadOnly() == cp &&
            ownerDecision.readOnlyStatus() ==
                V347OwnerRecoveryDecisionJournal.ReadState.Recorded(decision) &&
            !IdeaProjectConversion(app).hasPendingRecovery() &&
            sha256(source) == cp.sourceSha256 &&
            sha256(rollback) == cp.escrowSha256) {
            "Installed Creator target, Personal baseline or owner escrow changed"
        }
    }

    private fun ownerFile(folderName: String, filename: String): File {
        val folder = File(app.filesDir, folderName)
        check(folder.isDirectory && !Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted owner backup folder"
        }
        val file = File(folder, filename)
        check(Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == folder.canonicalFile) {
            "Owner recovery ZIP is missing, symlinked or outside private storage"
        }
        return file
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val bytes = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(bytes)
                if (count < 0) break
                if (count > 0) md.update(bytes, 0, count)
            }
        }
        return md.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
}
