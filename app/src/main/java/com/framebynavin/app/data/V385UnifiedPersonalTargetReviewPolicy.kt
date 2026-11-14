package com.framebynavin.app.data

import com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport

/**
 * Read-only integrity and conflict gate for preparing an owner-selected
 * unified target. An incoming 21-day challenge is an ADDITION requiring an
 * explicit owner decision, not a conflict. The UI must still show that owner
 * review and the write coordinator must still obtain fresh owner consent.
 *
 * This check does NOT grant a restore write permit or enable Restore All.
 */
internal object V385UnifiedPersonalTargetReviewPolicy {
    fun conflictFreeAndVerified(review: Phase1V2ArchiveDryRunReport): Boolean =
        review.eightDomains.localIntegrityVerified &&
            review.newChallenges.archiveHasChallengeCoverage &&
            review.conflicts == 0 &&
            review.newChallenges.conflictingReceiptDates == 0
}
