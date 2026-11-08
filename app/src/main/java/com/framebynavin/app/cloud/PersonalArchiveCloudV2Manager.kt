package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.personal.Phase1V2ArchiveDryRun
import com.framebynavin.app.personal.Phase1V2OwnerRestoreCoordinator
import com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport
import com.framebynavin.app.personal.V300PersonalArchiveSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opt-in complete encrypted cloud ARCHIVE. All nine domains, including
 * independent JPEG bytes and exact 21-day confirmedAt instants, are included
 * by the same already-tested V300PersonalArchiveSource as manual ZIP export.
 *
 * This does not replace the old Personal JSON Sync Now API or restore into
 * live stores. Password is never saved or transmitted. No cloud overwrite.
 */
internal class PersonalArchiveCloudV2Manager(context: Context) {
    private val app = context.applicationContext
    private val account = CloudSyncManager(app)
    private val api = PersonalArchiveCloudV2Api()
    private val source = V300PersonalArchiveSource(app)
    private val cursor = PersonalCloudCursor(app)

    internal data class CloudPreview(
        val revision: Long,
        val verifiedEncryptedBytes: Int,
        val report: Phase1V2ArchiveDryRunReport,
    )

    /**
     * Creates the first complete encrypted account backup only when there is
     * NO existing v2 cloud head. Existing cloud history is never overwritten.
     * An explicitly approved, conflict-aware replacement flow is separately
     * required for subsequent snapshots and multi-device sync.
     */
    suspend fun createFirstCloudCopy(password: CharArray): Long {
        require(password.size >= 12) { "Personal archive passphrase must have at least 12 characters" }
        return account.withFreshSession { session ->
            val priorOwner = cursor.ownerUserId()
            check(priorOwner == null || priorOwner == session.userId) {
                "Another signed-in account owns this device's Personal cloud cursor"
            }
            check(api.head(session) == null) {
                "A cloud Personal archive already exists. Review that copy before replacing it."
            }
            val encrypted = withContext(Dispatchers.IO) {
                val buffer = ByteArrayOutputStream()
                source.export(password, buffer)
                val bytes = buffer.toByteArray()
                require(bytes.size in 1..PersonalArchiveCloudV2Rules.MAX_BYTES) {
                    "Encrypted Personal archive exceeds the supported cloud file limit"
                }
                source.verify(password, ByteArrayInputStream(bytes))
                bytes
            }
            val objectPath = session.userId + "/v2-" + UUID.randomUUID() + ".zip"
            val checksum = sha(encrypted)
            api.upload(session, objectPath, encrypted)
            // Network success is not proof the provider persisted all bytes.
            val readBack = api.download(session, objectPath)
            check(sha(readBack) == checksum) {
                "The saved encrypted cloud ZIP does not match the local archive"
            }
            source.verify(password, ByteArrayInputStream(readBack))
            check(api.compareAndSwap(session, null, objectPath, checksum)) {
                "A different phone created a Personal cloud snapshot first. Nothing local changed."
            }
            val confirmed = checkNotNull(api.head(session)) {
                "Cloud did not return the new Personal archive head"
            }
            check(confirmed.revision == 1L &&
                confirmed.objectPath == objectPath && confirmed.sha256 == checksum) {
                "Cloud archive metadata was not verified after upload"
            }
            confirmed.revision
        }
    }

    /** Download, authenticate, preview conflicts: NEVER restore live data. */
    suspend fun inspectCloudCopy(password: CharArray): CloudPreview {
        require(password.size >= 12) { "Personal archive passphrase must have at least 12 characters" }
        return account.withFreshSession { session ->
            val cloud = checkNotNull(api.head(session)) {
                "There is no encrypted Personal cloud archive under this Google account"
            }
            val zipped = api.download(session, cloud.objectPath)
            check(sha(zipped) == cloud.sha256) {
                "Encrypted cloud archive checksum mismatch; nothing was imported"
            }
            // ZIP decryption and all private JPEG byte checks happen before
            // comparing current phone's Personal/Spaces/Done receipts.
            source.verify(password, ByteArrayInputStream(zipped))
            val report = Phase1V2ArchiveDryRun(app).check(
                ByteArrayInputStream(zipped), password,
            )
            CloudPreview(cloud.revision, zipped.size, report)
        }
    }


    /**
     * Explicit cloud-to-phone restoration after the owner viewed the full
     * nine-domain conflict preview. No background automatic import, never
     * overwrite another device's records, and require the same passphrase to
     * encrypt the independent private rollback escrow on the destination.
     */
    suspend fun restoreOwnerApprovedFromCloud(
        password: CharArray,
        ownerApprovedAdditions: Boolean,
    ): Phase1V2OwnerRestoreCoordinator.Finished {
        require(ownerApprovedAdditions && password.size >= 12) {
            "Explicit owner review and private archive passphrase required"
        }
        return account.withFreshSession { session ->
            val head = checkNotNull(api.head(session)) {
                "There is no Personal cloud snapshot to restore"
            }
            require(head.objectPath.startsWith(session.userId + "/")) {
                "Encrypted cloud object does not belong to signed-in account"
            }
            val data = api.download(session, head.objectPath)
            require(data.size in 1..PersonalArchiveCloudV2Rules.MAX_BYTES &&
                sha(data) == head.sha256) {
                "Private cloud archive checksum or size is invalid"
            }
            source.verify(password, ByteArrayInputStream(data))
            val reviewed = Phase1V2ArchiveDryRun(app).check(
                ByteArrayInputStream(data), password,
            )
            require(reviewed.conflicts == 0) {
                "Conflicting Personal cloud records require owner resolution"
            }
            val independentEscrowPassword = password.copyOf()
            try {
                Phase1V2OwnerRestoreCoordinator(app).restoreOwnerApproved(
                    ByteArrayInputStream(data),
                    selectedPassphrase = password,
                    escrowPassphrase = independentEscrowPassword,
                    ownerConfirmedAdditions = true,
                    acknowledgedNonPortableAlarmsAndSongs = true,
                )
            } finally {
                independentEscrowPassword.fill('\u0000')
            }
        }
    }

    /**
     * Append a new complete encrypted revision without silently discarding
     * records from another device. Existing cloud ZIP is downloaded and
     * decrypted FIRST. A server-side revision + checksum CAS prevents writes
     * racing another phone. Uploaded files are immutable, never upserted.
     *
     * Important: a remote-only reminder, Space, private photo, or a changed
     * exact challenge Done instant rejects publishing until the owner has
     * restored/resolved that cloud content locally.
     */
    suspend fun publishReviewedNewCloudRevision(
        password: CharArray,
        ownerApprovedCloudRevision: Boolean,
    ): Long {
        require(ownerApprovedCloudRevision && password.size >= 12) {
            "Explicit owner approval and archive passphrase are required"
        }
        return account.withFreshSession { session ->
            val knownOwner = cursor.ownerUserId()
            check(knownOwner == null || knownOwner == session.userId) {
                "Personal cloud cursor belongs to another signed-in account"
            }
            val existing = checkNotNull(api.head(session)) {
                "No existing private cloud backup. Use Save First Copy instead."
            }
            val oldBytes = api.download(session, existing.objectPath)
            check(sha(oldBytes) == existing.sha256) {
                "Old encrypted Personal cloud backup differs from server head"
            }
            source.verify(password, ByteArrayInputStream(oldBytes))
            val difference = Phase1V2ArchiveDryRun(app).check(
                ByteArrayInputStream(oldBytes), password,
            )
            PersonalArchiveCloudV2Rules.requireVerifiedPhoneSuperset(
                cloudOnly = difference.proposedAdditions,
                conflicts = difference.conflicts,
                localIntegrityVerified = difference.eightDomains.localIntegrityVerified,
            )
            val old = difference.eightDomains
            val localOnly = listOf(
                old.reminders, old.completedActions, old.challenges, old.groups,
                old.spaces, old.spaceLinks, old.journalEntries, old.photos,
            ).sumOf { it.localOnly } + difference.newChallenges.localOnly
            if (localOnly == 0) return@withFreshSession existing.revision

            val encrypted = withContext(Dispatchers.IO) {
                val buffer = ByteArrayOutputStream()
                source.export(password, buffer)
                val bytes = buffer.toByteArray()
                require(bytes.size in 1..PersonalArchiveCloudV2Rules.MAX_BYTES) {
                    "Updated Personal archive exceeds private cloud size limit"
                }
                source.verify(password, ByteArrayInputStream(bytes))
                bytes
            }
            val path = session.userId + "/v2-" + UUID.randomUUID() + ".zip"
            val checksum = sha(encrypted)
            api.upload(session, path, encrypted)
            val received = api.download(session, path)
            check(sha(received) == checksum) {
                "New encrypted cloud revision did not verify after upload"
            }
            source.verify(password, ByteArrayInputStream(received))
            check(api.compareAndSwap(session, existing, path, checksum)) {
                "Another device changed cloud backup during upload. " +
                    "Recheck the latest cloud archive; old revision was not overwritten."
            }
            val updated = checkNotNull(api.head(session))
            check(updated.revision == existing.revision + 1L &&
                updated.objectPath == path && updated.sha256 == checksum) {
                "Cloud revision pointer differs from confirmed encrypted upload"
            }
            updated.revision
        }
    }

    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
