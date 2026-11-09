package com.framebynavin.app.cloud

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deliberately invoked, owner-authenticated encrypted Backlot backup.
 *
 * This is a NEW backup source, not automatic "Sync Now". Source bytes are
 * frozen under the existing Creator -> Personal owner gates and copied into
 * disposable private staging. Only encrypted verified ciphertext survives.
 * The existing RLS/CAS transport publishes an immutable revision after
 * independently re-downloading the complete remote object.
 *
 * Until live backend and whole-app crash/restore acceptance are complete,
 * UI must not expose an auto-backup/complete-backup claim. This source is
 * prepared for that activation, not a successful real-world upload.
 */
internal class TrustOwnerBackupCoordinator(
    context: Context,
    private val accounts: CloudSyncManager = CloudSyncManager(context),
    private val remote: TrustUniversalCloudTransport = TrustUniversalCloudTransport(),
) {
    private val app = context.applicationContext
    private val root = File(app.noBackupFilesDir, "trust-owner-verified-backups-v1")
    private val maxCloudBytes = 50L * 1024 * 1024

    internal data class VerifiedEncryptedOwnerSource(
        val ownerId: String,
        val file: File,
        val expectedSha256: String,
        val bytes: Long,
        val parts: Int,
        val domainCounts: Map<String, Int>,
    ) {
        val isPortableSourceVerified: Boolean get() = parts > 0
        // Publication and restore remain separate owner-confirmed operations.
        val mayReplaceOtherDeviceData: Boolean get() = false
    }

    internal data class PublishResult(
        val ownerId: String,
        val verifiedRevision: Long,
        val immutablePath: String,
        val encryptedBytes: Long,
        val sourceParts: Int,
        val verifiedRemoteBytes: Boolean,
    )

    suspend fun prepareOwnerSource(
        ownerPassphrase: CharArray,
        explicitlyRequestedByOwner: Boolean,
    ): VerifiedEncryptedOwnerSource {
        require(explicitlyRequestedByOwner) {
            "Backlot never creates a universal owner backup without an owner request"
        }
        require(ownerPassphrase.size in 12..512) {
            "A private encryption passphrase of at least 12 characters is required"
        }
        return accounts.withFreshSession { session ->
            TrustT1C4CloudRevisionPolicy.validateOwner(session.userId)
            val prepared = TrustT1C3OperationGate.withExclusive(app) {
                withContext(Dispatchers.IO) {
                    ensureRoot()
                    check(root.listFiles().orEmpty().count {
                        it.isFile && it.name.startsWith("verified-") &&
                            it.name.endsWith(".enc")
                    } < 3) {
                        "Three verified encrypted backups are already on this device; " +
                            "review them before creating another"
                    }
                    val id = UUID.randomUUID().toString()
                    val pending = File(root, "pending-$id.enc")
                    val verified = File(root, "verified-$id.enc")
                    check(pending.createNewFile()) {
                        "Private owner encrypted backup path is already occupied"
                    }
                    var promoted = false
                    try {
                        TrustT1C2OwnerSourceCapture(app).freezePartialReadOnly().use {
                            frozen ->
                            // A sealed immutable source is encrypted after
                            // capture; plaintext is not left in cloud storage.
                            FileOutputStream(pending).use { disk ->
                                TrustEncryptedStagingCodec.write(
                                    session.userId,
                                    ownerPassphrase,
                                    frozen.sources(),
                                    disk,
                                )
                                disk.fd.sync()
                            }
                            check(pending.length() in 1..maxCloudBytes) {
                                "Encrypted owner source exceeds 50 MB free-tier " +
                                    "single-object limit; no cloud charge authorized"
                            }
                            val inspected = pending.inputStream().buffered().use { stream ->
                                TrustEncryptedStagingCodec.stageAndVerify(
                                    session.userId, ownerPassphrase, stream, app.cacheDir,
                                ).use { restored ->
                                    require(restored.parts.size == frozen.parts.size) {
                                        "Encrypted source omitted owner data"
                                    }
                                    val proof = frozen.parts.associateBy { it.id }
                                    restored.parts.forEach { source ->
                                        val original = checkNotNull(proof[source.id])
                                        require(source.records == original.records &&
                                            source.sha256 == original.sha256 &&
                                            source.sizeBytes == original.bytes) {
                                            "Encrypted part changed from frozen original"
                                        }
                                    }
                                    TrustRestorableSourceReview.inspect(app, restored)
                                }
                            }
                            check(inspected.missingRequiredSources.isEmpty() &&
                                inspected.uncoveredPreferenceKeys.isEmpty()) {
                                "Some owner settings or source domains cannot yet be restored"
                            }
                            val sha = sha256(pending)
                            val bytes = pending.length()
                            check(!verified.exists())
                            Files.move(
                                pending.toPath(), verified.toPath(),
                                StandardCopyOption.ATOMIC_MOVE,
                            )
                            promoted = true
                            check(verified.isFile && verified.length() == bytes &&
                                sha256(verified) == sha) {
                                "Private encrypted backup changed after atomic promotion"
                            }
                            VerifiedEncryptedOwnerSource(
                                ownerId = session.userId,
                                file = verified,
                                expectedSha256 = sha,
                                bytes = bytes,
                                parts = inspected.sourcePartsValidated,
                                domainCounts = inspected.counts,
                            )
                        }
                    } catch (failure: Throwable) {
                        if (!promoted && pending.exists() && !pending.delete()) {
                            failure.addSuppressed(IllegalStateException(
                                "Failed to erase unverified encrypted owner backup",
                            ))
                        }
                        throw failure
                    }
                }
            }
            // The connected account generation is still gated by
            // withFreshSession; do not serialize an access token anywhere.
            check(prepared.ownerId == session.userId)
            prepared
        }
    }

    suspend fun publishExplicitlyApproved(
        prepared: VerifiedEncryptedOwnerSource,
        ownerApprovedUpload: Boolean,
    ): PublishResult {
        require(ownerApprovedUpload) { "Owner must approve cloud upload" }
        require(prepared.parts > 0 && prepared.bytes in 1..maxCloudBytes)
        return accounts.withFreshSession { session ->
            check(session.userId == prepared.ownerId) {
                "The selected encrypted backup belongs to another account"
            }
            check(prepared.file.parentFile?.canonicalFile ==
                root.canonicalFile &&
                prepared.file.isFile &&
                !Files.isSymbolicLink(prepared.file.toPath()) &&
                prepared.file.name.matches(Regex(
                    "verified-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-" +
                        "[0-9a-f]{4}-[0-9a-f]{12}[.]enc",
                ))) {
                "Only a previously verified local encrypted Backlot source may be uploaded"
            }
            check(prepared.file.length() == prepared.bytes &&
                sha256(prepared.file) == prepared.expectedSha256) {
                "Local encrypted source changed before upload"
            }
            remote.beginUpload(session)
            val previous = remote.head(session)
            val descriptor = TrustCloudCiphertextDescriptor(
                ownerUserId = session.userId,
                objectPath = TrustT1C4CloudRevisionPolicy.newImmutablePath(
                    session.userId, UUID.randomUUID(),
                ),
                encryptedSha256 = prepared.expectedSha256,
                encryptedBytes = prepared.bytes,
            )
            remote.uploadNewImmutable(session, descriptor, prepared.file)
            val proof = remote.verifiedDownload(session, descriptor)
            val commit = remote.claimRevision(session, previous, descriptor, proof)
            check(commit.canClaimDurableRemoteCopy &&
                commit.remoteRevision != null) {
                "Immutable encrypted cloud revision was not confirmed"
            }
            PublishResult(
                ownerId = session.userId,
                verifiedRevision = checkNotNull(commit.remoteRevision),
                immutablePath = descriptor.objectPath,
                encryptedBytes = prepared.bytes,
                sourceParts = prepared.parts,
                verifiedRemoteBytes = true,
            )
        }
    }

    private fun ensureRoot() {
        check(root.isDirectory || root.mkdirs()) {
            "Could not prepare owner encrypted backup directory"
        }
        check(!Files.isSymbolicLink(root.toPath())) {
            "Refusing private backup directory symlink"
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val bytes = input.read(buffer)
                if (bytes < 0) break
                if (bytes > 0) digest.update(buffer, 0, bytes)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
