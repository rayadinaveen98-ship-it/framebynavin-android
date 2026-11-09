package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * T1B4: a deterministic *inspection receipt* from two independently completed
 * reads of the actual Creator and Personal owner stores.
 *
 * This is NOT a byte-preserving backup, a frozen cross-store transaction or an
 * authorization to upload/restore. Writers not covered by both owner gates can
 * still change the source immediately after these checks. T1C must stage the
 * actual immutable bytes under its own proven transaction boundary.
 */
internal data class TrustInventoryReviewManifest(
    val schemaVersion: Int,
    val inspectedAtMillis: Long,
    val creatorGeneration: Long,
    val inspected: List<TrustDomainEvidence>,
    val pendingDomains: List<TrustBackupDomain>,
    val excludedDeviceOnlyDomains: List<TrustBackupDomain>,
    val stableAcrossTwoReads: Boolean,
    val crossStoreAtomicityVerified: Boolean,
    val sha256: String,
) {
    val canClaimCompleteBackup: Boolean get() = false
    val canStartCloudUpload: Boolean get() = false

    init {
        require(schemaVersion == 1 && inspectedAtMillis > 0 && creatorGeneration >= 0)
        require(stableAcrossTwoReads && !crossStoreAtomicityVerified)
        require(sha256.matches(Regex("[0-9a-f]{64}")))
    }
}

internal object TrustInventoryReviewPolicy {
    /**
     * Require the same exact counts, UTF-8 byte lengths and content hashes,
     * identical unknown domains, and no observed Creator restore generation
     * change. A hash disagreement blocks the review, never falls back to zero.
     */
    fun reconcile(
        first: TrustBackupInventory,
        second: TrustBackupInventory,
        creatorGenerationBefore: Long,
        creatorGenerationAfter: Long,
        inspectedAtMillis: Long,
    ): TrustInventoryReviewManifest {
        require(creatorGenerationBefore >= 0 &&
            creatorGenerationBefore == creatorGenerationAfter) {
            "Owner Creator generation changed during backup inventory"
        }
        require(inspectedAtMillis > 0L) { "Inspection timestamp is invalid" }
        require(!first.crossStoreAtomicityVerified &&
            !second.crossStoreAtomicityVerified) {
            "Metadata-only review cannot assert a cross-store transaction"
        }
        require(first.inspected == second.inspected &&
            first.pendingDomains == second.pendingDomains &&
            first.exclusions == second.exclusions) {
            "Local owner data changed during the inventory; nothing was backed up"
        }
        val evidence = first.inspected.sortedBy { it.domain.ordinal }
        require(evidence.map { it.domain }.distinct().size == evidence.size) {
            "Duplicate owner data domain"
        }
        // Do not trust a caller-provided pending/exclusion list, even if both
        // reads return the same fabricated "all covered" list. Re-derive from
        // the schema, including future enum additions, and fail closed.
        val derived = TrustBackupInventoryPolicy.assemble(evidence)
        require(first.pendingDomains == derived.pendingDomains &&
            first.exclusions == derived.exclusions) {
            "Inventory coverage omitted one or more owner data domains"
        }
        val pending = derived.pendingDomains.sortedBy { it.ordinal }
        val excluded = derived.exclusions.sortedBy { it.ordinal }
        require(evidence.none { it.domain in pending || it.domain in excluded }) {
            "Overlapping owner backup domains"
        }
        // Deterministic length-delimited UTF-8, independent of JSONObject
        // key iteration order or locale. No private source bytes are emitted.
        val digest = MessageDigest.getInstance("SHA-256")
        fun put(part: String) {
            val bytes = part.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        put("backlot-trust-local-inventory-v1")
        put(inspectedAtMillis.toString())
        put(creatorGenerationBefore.toString())
        put(evidence.size.toString())
        evidence.forEach { item ->
            put(item.domain.name)
            put(item.records.toString())
            put(item.bytes.toString())
            put(item.sha256)
        }
        put(pending.size.toString())
        pending.forEach { put(it.name) }
        put(excluded.size.toString())
        excluded.forEach { put(it.name) }
        put("stable-across-two-reads-not-atomic")
        val fingerprint = digest.digest().joinToString("") { "%02x".format(it) }
        return TrustInventoryReviewManifest(
            schemaVersion = 1,
            inspectedAtMillis = inspectedAtMillis,
            creatorGeneration = creatorGenerationBefore,
            inspected = evidence,
            pendingDomains = pending,
            excludedDeviceOnlyDomains = excluded,
            stableAcrossTwoReads = true,
            crossStoreAtomicityVerified = false,
            sha256 = fingerprint,
        )
    }
}

/**
 * Explicit, potentially expensive owner-initiated inspection only.
 * No cloud access, app startup hook, prefs seeding, APK upload or restore.
 * Multiple reads catch drift; they do NOT imply cross-store atomicity.
 */
internal class TrustInventoryReviewReader(context: Context) {
    private val app = context.applicationContext

    suspend fun inspectTwiceReadOnly(): TrustInventoryReviewManifest =
        withContext(Dispatchers.IO) {
            val before = CreatorDataGate.generation(app)
            val reader = TrustLocalBackupInventoryReader(app)
            val first = reader.inspectReadOnly()
            val second = reader.inspectReadOnly()
            val after = CreatorDataGate.generation(app)
            TrustInventoryReviewPolicy.reconcile(
                first, second, before, after, System.currentTimeMillis(),
            )
        }
}
