package com.framebynavin.app.cloud

import java.security.MessageDigest

/**
 * Trust T1B: *read-only metadata* about evidence from each real owner data store.
 *
 * No payload, title, photo path or account identifier is retained by this
 * inventory. A checksum is evidence about the bytes INSPECTED, not evidence
 * that those bytes have been encrypted, durably uploaded or made restorable.
 */
internal enum class TrustBackupDomain(val label: String, val portable: Boolean = true) {
    CREATOR_PROJECTS("Creator projects and embedded workspaces"),
    CREATOR_IDEAS("Ideas and text/transcription metadata"),
    CREATOR_FOCUS("Focus sessions"),
    CREATOR_WEEKLY_PLAN("Weekly plan and creator configuration"),
    CREATOR_HISTORY("Creator workflow and publishing history"),
    CREATOR_MEDIA("Creator photos, voice recordings and external assets"),
    PERSONAL_REMINDERS("Personal reminders and recurrence"),
    PERSONAL_DONE_RECEIPTS("Personal reminder completion history"),
    PERSONAL_SINGLE_ACTIVITY("One-activity 21-day challenges"),
    PERSONAL_SINGLE_ACTIVITY_DONE("Exact one-activity Done receipts"),
    PERSONAL_LEGACY_CHALLENGES("Legacy V297 challenge progress"),
    PERSONAL_GROUPS("Reminder groups"),
    PERSONAL_SPACES("Personal Spaces"),
    PERSONAL_JOURNALS("Space notes and check-in history"),
    PERSONAL_SPACE_LINKS("Space and reminder links"),
    PERSONAL_PHOTO_METADATA("Private Space photo metadata"),
    PERSONAL_PHOTO_BYTES("Private Space JPEG bytes"),
    NOTIFICATION_PREFERENCES("Notification, alarm and voice preferences"),
    SPARK_PREFERENCES("Spark and companion preferences"),
    HOME_SLIDESHOW_MEDIA("Home slideshow original images"),
    DEVICE_ALARM_LEASES("Device-specific alarm leases", portable = false),
    EXTERNAL_OAUTH_SECRETS("External account auth tokens", portable = false),
    ANDROID_PERMISSIONS("Android OS notification/alarm permissions", portable = false),
}

internal data class TrustDomainEvidence(
    val domain: TrustBackupDomain,
    val records: Int,
    val bytes: Int,
    val sha256: String,
) {
    init {
        require(domain.portable) { "Device-only data cannot be uploaded as portable backup" }
        require(records >= 0 && bytes >= 0) { "Negative inventory quantity" }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "Invalid evidence checksum" }
    }
}

internal data class TrustBackupInventory(
    val inspected: List<TrustDomainEvidence>,
    val pendingDomains: List<TrustBackupDomain>,
    val exclusions: List<TrustBackupDomain>,
    /**
     * The Creator and Personal stores have DISTINCT snapshot locks.
     * Read-only inspection can never assert that one atomic revision exists.
     */
    val crossStoreAtomicityVerified: Boolean = false,
) {
    // A metadata-only inspection can NEVER authorize a complete-backup claim,
    // even if future callers accidentally mark every domain as observed. T1C
    // must prove source atomicity, byte capture, encryption and durable restore.
    val canClaimCompleteBackup: Boolean
        get() = false
    val canUploadAsUniversalBackup: Boolean
        get() = false // T1C must separately implement encrypted owner-scoped upload.
}

/**
 * Produces only a review inventory, never a backup authorization.
 * In particular, zero records must come from an actual verified empty
 * payload, NOT an inferred absence, exception fallback or placeholder hash.
 */
internal object TrustBackupInventoryPolicy {
    fun evidence(domain: TrustBackupDomain, raw: String, records: Int): TrustDomainEvidence {
        require(domain.portable && records >= 0)
        val bytes = raw.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 24 * 1024 * 1024) { "Inventory evidence exceeds size limit" }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        return TrustDomainEvidence(domain, records, bytes.size, digest)
    }

    fun assemble(evidence: List<TrustDomainEvidence>): TrustBackupInventory {
        require(evidence.map { it.domain }.distinct().size == evidence.size) {
            "Duplicate backup inventory domain"
        }
        val portable = TrustBackupDomain.entries.filter { it.portable }
        val proofs = evidence.sortedBy { it.domain.ordinal }
        return TrustBackupInventory(
            inspected = proofs,
            pendingDomains = portable.filter { expected ->
                proofs.none { it.domain == expected }
            },
            exclusions = TrustBackupDomain.entries.filterNot { it.portable },
        )
    }
}
