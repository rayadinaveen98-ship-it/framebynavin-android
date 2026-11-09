package com.framebynavin.app.cloud

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Same-account offline Restore PREVIEW for the encrypted copies created in
 * Backlot or imported through Android's document picker.
 *
 * This path needs NO deployed Trust database protocol, Storage bucket or
 * network ciphertext revision. It authenticates the existing private file and
 * verifies all original Creator and Personal parts before comparing them to
 * the current phone with the native, read-only recovery policies.
 *
 * The review never grants mutation rights. A later owner-confirmed local
 * restore must retain an independent recovery escrow and native journals.
 */
internal class TrustOwnerLocalRecoveryReview(context: Context) {
    private val app = context.applicationContext
    private val local = TrustOwnerEncryptedLocalCopy(app)

    internal data class Result(
        val ownerId: String,
        val encryptedCopyId: String,
        val encryptedBytes: Long,
        val creator: TrustCreatorRestoreBridge.Review,
        val personal: com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport,
        val coveredParts: Int,
        val uncoveredPreferenceKeys: List<String>,
    ) {
        val hasExistingCreatorWork: Boolean get() = creator.hasExistingOwnerWork
        val hasPersonalConflicts: Boolean get() = personal.conflicts > 0
        val safeToPrepareFreshDevice: Boolean get() =
            uncoveredPreferenceKeys.isEmpty() &&
                !hasExistingCreatorWork &&
                creator.missingCreatorSources.isEmpty() &&
                creator.projectIdConflicts == 0 &&
                creator.ideaIdConflicts == 0 &&
                !hasPersonalConflicts

        // A preview is never a recovery-completion receipt.
        val performedRestore: Boolean get() = false
    }

    suspend fun preview(
        selected: TrustOwnerEncryptedLocalCopy.Receipt,
        passphrase: CharArray,
    ): Result = local.withVerifiedCopyForPublish(
        selected, passphrase,
    ) { session, ciphertext, coverage ->
        check(session.userId == selected.ownerId &&
            coverage.missingRequiredSources.isEmpty() &&
            coverage.uncoveredPreferenceKeys.isEmpty()) {
            "The encrypted offline copy is partial or belongs to another owner"
        }
        withContext(Dispatchers.IO) {
            ciphertext.inputStream().buffered().use { source ->
                TrustEncryptedStagingCodec.stageAndVerify(
                    session.userId, passphrase, source, app.cacheDir,
                ).use { verified ->
                    val authenticated = TrustRestorableSourceReview.inspect(app, verified)
                    check(authenticated.missingRequiredSources.isEmpty() &&
                        authenticated.uncoveredPreferenceKeys.isEmpty()) {
                        "Offline copy contains incomplete saved owner records"
                    }
                    val creator = TrustCreatorRestoreBridge(app).preview(verified)
                    val personal = TrustPersonalRestoreBridge(app).previewAdditions(verified)
                    Result(
                        ownerId = session.userId,
                        encryptedCopyId = selected.copyId,
                        encryptedBytes = selected.encryptedBytes,
                        creator = creator,
                        personal = personal,
                        coveredParts = authenticated.sourcePartsValidated,
                        uncoveredPreferenceKeys = authenticated.uncoveredPreferenceKeys,
                    )
                }
            }
        }
    }
}
