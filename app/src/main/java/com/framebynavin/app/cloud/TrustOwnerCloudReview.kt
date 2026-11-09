package com.framebynavin.app.cloud

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * READ-ONLY owner review of a selected encrypted cloud revision.
 *
 * Opening a remote backup must never replace the phone's Creator projects,
 * Personal reminders, 21-day receipts, Groups, Spaces, photos or alarm leases.
 * The exact remote head is checked before and after copying, the full file is
 * verified twice (stream + private disk), and each encrypted part is decrypted
 * and authenticated in disposable staging before reporting an inventory.
 *
 * This is source coverage inspection, NOT owner-authorized restore. No preview
 * grants mutation rights and no previously approved local review is overwritten.
 */
internal class TrustOwnerCloudReview(
    context: Context,
    private val accounts: CloudSyncManager = CloudSyncManager(context),
    private val transport: TrustUniversalCloudTransport = TrustUniversalCloudTransport(),
) {
    private val app = context.applicationContext
    private val root = File(app.noBackupFilesDir, "trust-owner-cloud-ciphertext-review-v1")
    private val uuid = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
    )

    internal data class OwnerHeadReview(
        val ownerId: String,
        val head: TrustCloudRevisionHead?,
        val storedEncryptedBytes: Long,
    ) {
        val hasEncryptedBackup: Boolean get() = head != null
    }

    internal data class SourceCoverageReview(
        val ownerId: String,
        val remoteRevision: Long,
        val encryptedLocalReviewId: String,
        val encryptedBytes: Long,
        val verifiedParts: Int,
        val counts: Map<String, Int>,
        val missingRequiredParts: Set<String>,
        val knownUncoveredDomains: Set<String>,
    ) {
        // Explicitly protects legacy source previews from becoming a Restore
        // button just because their password and ciphertext are valid.
        val mayReplaceOwnerData: Boolean get() = false
        val mayUploadAsCompleteBackup: Boolean get() = false
    }

    suspend fun inspectCurrentRemote(): OwnerHeadReview =
        accounts.withFreshSession { session ->
            require(transport.phase(session) == "active") {
                "Cloud backup deletion is pending; do not open a stale remote revision"
            }
            val head = transport.head(session)
            OwnerHeadReview(
                ownerId = session.userId,
                head = head,
                storedEncryptedBytes = head?.encryptedBytes ?: 0L,
            )
        }

    suspend fun inspectEncryptedSource(
        ownerReviewed: OwnerHeadReview,
        passphrase: CharArray,
    ): SourceCoverageReview {
        require(passphrase.size in 12..512) {
            "Enter the private backup passphrase to review its contents"
        }
        val selectedHead = checkNotNull(ownerReviewed.head) {
            "No verified encrypted owner revision was selected"
        }
        val reviewId = UUID.randomUUID().toString()
        val pending = File(ensurePrivateRoot(), "pending-$reviewId.enc")
        val verified = File(root, "review-$reviewId.enc")
        try {
            return accounts.withFreshSession { session ->
                check(session.userId == ownerReviewed.ownerId) {
                    "The signed-in Backlot account changed; owner data was not opened"
                }
                check(transport.phase(session) == "active") {
                    "Owner backup deletion is in progress"
                }
                val actualHead = transport.head(session)
                TrustT1C4CloudRevisionPolicy.requireUnchanged(
                    selectedHead, actualHead, session.userId,
                )
                val descriptor = TrustCloudCiphertextDescriptor(
                    session.userId,
                    selectedHead.objectPath,
                    selectedHead.encryptedSha256,
                    selectedHead.encryptedBytes,
                )
                transport.downloadVerifiedToPrivateFile(session, descriptor, pending)
                val verifiedSource = withContext(Dispatchers.IO) {
                    pending.inputStream().buffered().use { input ->
                        TrustEncryptedStagingCodec.stageAndVerify(
                            session.userId, passphrase, input, app.cacheDir,
                        ).use { staged ->
                            TrustRestorableSourceReview.inspect(app, staged)
                        }
                    }
                }
                // If another phone published a newer revision while this
                // password/decryption inspection ran, the old one is not a
                // current-owner restore candidate.
                check(transport.phase(session) == "active")
                TrustT1C4CloudRevisionPolicy.requireUnchanged(
                    selectedHead, transport.head(session), session.userId,
                )
                check(!verified.exists()) { "Private encrypted review ID collided" }
                Files.move(
                    pending.toPath(), verified.toPath(), StandardCopyOption.ATOMIC_MOVE,
                )
                SourceCoverageReview(
                    ownerId = session.userId,
                    remoteRevision = selectedHead.revision,
                    encryptedLocalReviewId = reviewId,
                    encryptedBytes = selectedHead.encryptedBytes,
                    verifiedParts = verifiedSource.sourcePartsValidated,
                    counts = verifiedSource.counts,
                    missingRequiredParts = verifiedSource.missingRequiredSources,
                    knownUncoveredDomains = (
                        verifiedSource.uncoveredPreferenceKeys.map {
                            "Uncaptured saved preference: " + it
                        } + listOf(
                            "Some independent local settings/Creator assets still lack restore adapters",
                            "Cross-store restore rollback, delivery rearming and device permissions",
                        )
                    ).toSet(),
                )
            }
        } catch (failure: Throwable) {
            if (pending.exists() && !pending.delete()) {
                failure.addSuppressed(IllegalStateException(
                    "Could not erase the unverified private ciphertext file",
                ))
            }
            if (verified.exists() && !verified.delete()) {
                failure.addSuppressed(IllegalStateException(
                    "Could not erase the stale owner review after account change",
                ))
            }
            throw failure
        }
    }

    private fun ensurePrivateRoot(): File {
        check(root.isDirectory || root.mkdirs()) {
            "Unable to prepare private owner ciphertext review directory"
        }
        check(!Files.isSymbolicLink(root.toPath())) {
            "Refusing symlinked private owner review directory"
        }
        // Encrypted owner reviews stay on device unless the owner explicitly
        // exports them. Refuse new reviews rather than deleting another one.
        check(root.listFiles().orEmpty().count {
            it.isFile && it.name.matches(Regex("review-$uuid[.]enc"))
        } < 3) {
            "Review storage is full. Resolve an existing encrypted copy first"
        }
        return root
    }
}
