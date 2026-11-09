package com.framebynavin.app.cloud

/**
 * Trust Foundation T1A: existing Sync Now and the explicit Keep This Phone /
 * Use Cloud Copy actions still operate on the LEGACY reminder-only payload.
 *
 * A newer cloud bundle carries single-activity challenge receipts. Allowing
 * legacy push/restore to touch that bundle can destroy information even when
 * this phone currently has zero challenges. Reject it before ANY cloud write,
 * Personal DataStore replacement, or confirmation of "synced".
 *
 * This gate does NOT enable cloud bundle v2, not even when it holds zero habits:
 * the whole-domain, crash-safe restore coordinator must be approved first.
 * JSON and inner checksum validation is performed by the existing v2 codec.
 */
internal class PersonalCloudNewerBackupRequiresReview : IllegalStateException(
    "This cloud copy contains newer Personal backup data. " +
        "The older Sync Now cannot replace or restore it safely. " +
        "No cloud or phone records were changed.",
)

internal object PersonalCloudLegacyRemoteGate {
    fun requireLegacyOnly(raw: String) {
        val snapshot = PersonalCloudPortableSnapshotCodec.decode(raw)
        if (!snapshot.legacyPersonalOnly) {
            throw PersonalCloudNewerBackupRequiresReview()
        }
    }
}
