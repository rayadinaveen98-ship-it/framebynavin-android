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
