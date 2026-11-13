package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2RestoreFingerprint
import com.framebynavin.app.personal.Phase1V2RestoreTargetPolicy
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V300EncryptedPersonalArchive
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.V301LocalRecoverySnapshot
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Narrow OWNER-APPROVED repair of the H4 handoff gap:
 *
 * Personal v2 already completed and archived its independent VERIFYING
 * journal, but the process died before the outer H4 PERSONAL_WRITTEN marker.
 *
 * NO Personal or Creator data write occurs here. The source and original ZIPs
 * are fully reauthenticated (including original+target photo bytes), exact
 * native v2 transaction identity and digest are verified, current live
 * Personal and JPEG bytes must equal the intended merged target, and only
 * then H4 moves BEFORE_PERSONAL -> PERSONAL_WRITTEN.
 *
 * Torn, ACTIVE, unbound, unarchived, partially written or conflicting native
 * v2 journals are NOT recoverable through this method. Owner recovery for
 * those states and whole-domain rollback remain unimplemented and disabled.
 */
internal class V361UnifiedPersonalCompletedHandoff(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val native = V360UnifiedPersonalV2RecoveryInspector(app)

    suspend fun ownerApproveCheckpointOnly(
        passphrase: CharArray,
        ownerConfirmedNoReplay: Boolean,
    ): V361CompletedPersonalHandoffReceipt = withContext(Dispatchers.IO) {
        require(ownerConfirmedNoReplay && passphrase.size >= 12) {
            "Fresh owner approval and the encrypted backup passphrase are required"
        }
        val expected = h4.requireCheckpointReadOnly()
        check(expected.step == V344UnifiedRestoreStep.BEFORE_PERSONAL) {
            "Only a completed Personal v2 import missing its outer checkpoint is eligible"
        }
        val nativeState = native.inspectReadOnly() as?
            V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2CompletedAndArchived
            ?: error("Personal v2 has not proved its independent journal completed")

        val ownerProof = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true,
            passphrase = passphrase,
        )
        check(ownerProof.journal == expected &&
            !ownerProof.creatorStandaloneRecoveryMarker &&
            !ownerProof.personalStandaloneRecoveryMarker) {
            "An unrelated Creator/Personal recovery or stale owner backup blocks finalization"
        }
        val recipe = V356UnifiedPersonalRecoveryRecipeReader(app)
            .inspectVerified(ownerProof, passphrase).personalRecipe
        check(nativeState.originalDigest == recipe.originalDigest &&
            nativeState.mergedDigest == recipe.mergedTargetDigest) {
            "The archived native v2 restore does not match the authenticated Personal target"
        }

        // Verify the native v2 ORIGINAL escrow bytes and SELECTED source
        // independently. Comparing digest IDs alone is insufficient: the
        // archived ZIP files may have been truncated, swapped or overwritten.
        val v2Dir = ownedNativeV2Directory(nativeState.v2TransactionId)
        val archivedOriginal = File(v2Dir, "original-escrow-encrypted.zip")
        val archivedIncoming = File(v2Dir, "incoming-encrypted.zip")
        val original = inspectEncryptedPersonal(archivedOriginal, passphrase)
        check(Phase1V2RestoreFingerprint.calculate(
            original.components, original.photos,
        ) == recipe.originalDigest) {
            "Native v2 original rollback ZIP is not the original owner snapshot"
        }
        val incoming = inspectEncryptedPersonal(archivedIncoming, passphrase)
        val merged = Phase1V2RestoreTargetPolicy.propose(
            V301LocalRecoverySnapshot(
                original.components, original.photos,
                localIntegrityVerified = true,
            ),
            incoming.components, incoming.photos,
        )
        check(merged.preview.conflicts == 0 &&
            merged.baselineDigest == recipe.originalDigest &&
            merged.targetDigest == recipe.mergedTargetDigest) {
            "Native Personal ZIP bytes do not reconstruct the selected merged target"
        }

        // Normal owner editors are frozen by H4. Acquire the Personal lock
        // FIRST and Creator read mutex SECOND, consistent with both original
        // H3 write stages. The verified Personal read checks photo inventory
        // and exact Done receipts; an unexplained orphan JPEG fails closed.
        val localDigest = StabilizationPersonalMutationGate
            .withPendingCombinedReadInspection(app, expected) {
                CreatorDataGate.withPendingCombinedReadInspection(app, expected) {
                    val current = V301LocalRecoveryReader(app).readV2()
                    check(current.localIntegrityVerified) {
                        "Interrupted Personal records or JPEGs cannot be fully verified"
                    }
                    Phase1V2RestoreFingerprint.calculate(
                        current.components, current.photos,
                    )
                }
            }
        check(localDigest == recipe.mergedTargetDigest) {
            "Current Personal records, exact Done instants or private photos " +
                "differ from the fully authenticated v2 merged target"
        }

        // The only eligible native state is still ARCHIVED and exactly
        // bound to this transaction. A newly active, missing or substituted
        // v2 journal cannot be treated as a successfully completed import.
        check(native.inspectReadOnly() == nativeState &&
            h4.requireCheckpointReadOnly() == expected) {
            "Native Personal recovery evidence changed after live verification"
        }

        // Reconfirm immutable native journal history and the owner's
        // recovery direction INSIDE H4's serialized journal advancement.
        val advanced = h4.checkpoint(
            expected, V344UnifiedRestoreStep.PERSONAL_WRITTEN,
            maxOf(System.currentTimeMillis(), expected.updatedAtMillis + 1L),
            otherRecoveryPending = {
                V347OwnerRecoveryDecisionJournal.hasPendingFor(app)
            },
        )
        V361CompletedPersonalHandoffReceipt(
            transactionId = advanced.transactionId,
            verifiedPersonalDigest = localDigest,
            nativeV2TransactionId = nativeState.v2TransactionId,
            stage = advanced.step,
        )
    }

    private data class PersonalContent(
        val components: V300PersonalComponents,
        val photos: List<V290SpacePhotoEntity>,
    )

    private fun ownedNativeV2Directory(id: String): File {
        val root = File(app.filesDir, "backlot-personal-v2-encrypted-transactions")
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted Personal v2 encrypted archive directory"
        }
        val dir = File(root, id)
        check(dir.isDirectory && !Files.isSymbolicLink(dir.toPath()) &&
            dir.canonicalFile.parentFile == root.canonicalFile) {
            "Untrusted Personal v2 recovery transaction"
        }
        return dir
    }

    private fun inspectEncryptedPersonal(
        archive: File,
        passphrase: CharArray,
    ): PersonalContent {
        check(archive.isFile && !Files.isSymbolicLink(archive.toPath()) &&
            archive.length() > 0L &&
            archive.canonicalFile.parentFile == archive.parentFile.canonicalFile) {
            "Untrusted native Personal v2 ZIP copy"
        }
        return archive.inputStream().buffered().use { input ->
            V300EncryptedPersonalArchive.stageAndVerify(
                input, passphrase, app.cacheDir,
            ).use { verified ->
                PersonalContent(
                    components = verified.components,
                    photos = verified.photos.map { it.photo },
                )
            }
        }
    }
}

internal data class V361CompletedPersonalHandoffReceipt(
    val transactionId: String,
    val verifiedPersonalDigest: String,
    val nativeV2TransactionId: String,
    val stage: V344UnifiedRestoreStep,
) {
    init {
        require(stage == V344UnifiedRestoreStep.PERSONAL_WRITTEN &&
            verifiedPersonalDigest.matches(Regex("[0-9a-f]{64}"))) {
            "Completing the Personal stage cannot finalize combined Creator+Personal restore"
        }
    }
    val mayReleaseOriginalEscrow: Boolean get() = false
    val mayStartReminders: Boolean get() = false
    val combinedRestoreComplete: Boolean get() = false
}
