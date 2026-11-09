package com.framebynavin.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure T1B4 policy tests: no owner stores, Android runtime or cloud. */
class TrustInventoryReviewManifestTest {
    private fun evidence(
        value: String = "{\"title\":\"తెలుగు test owner note\"}",
        count: Int = 1,
    ) = TrustBackupInventoryPolicy.evidence(
        TrustBackupDomain.CREATOR_IDEAS, value, count,
    )

    private fun inventory(vararg items: TrustDomainEvidence) =
        TrustBackupInventoryPolicy.assemble(items.toList())

    private fun agree(
        first: TrustBackupInventory,
        second: TrustBackupInventory = first,
        generationStart: Long = 7L,
        generationEnd: Long = generationStart,
        atMillis: Long = 1_794_206_000_000L,
    ) = TrustInventoryReviewPolicy.reconcile(
        first, second, generationStart, generationEnd, atMillis,
    )

    @Test fun twoMatchingReadsProduceOnlyReadOnlyReviewNotBackupAuthorization() {
        val review = agree(inventory(evidence()))
        assertEquals(1, review.schemaVersion)
        assertTrue(review.stableAcrossTwoReads)
        assertFalse(review.crossStoreAtomicityVerified)
        assertEquals(64, review.sha256.length)
        assertTrue(TrustBackupDomain.PERSONAL_PHOTO_BYTES in review.pendingDomains)
        assertTrue(TrustBackupDomain.EXTERNAL_OAUTH_SECRETS in review.excludedDeviceOnlyDomains)
        assertFalse(review.canClaimCompleteBackup)
        assertFalse(review.canStartCloudUpload)
        assertFalse(review.toString().contains("తెలుగు test owner note"))
        assertFalse(review.toString().contains("\"title\""))
    }

    @Test fun modifiedReminderBytesFailEvenIfRecordCountIsUnchanged() {
        val earlier = inventory(evidence())
        val later = inventory(evidence(value = "{\"title\":\"different note\"}"))
        assertThrows(IllegalArgumentException::class.java) {
            agree(earlier, later)
        }
    }

    @Test fun zeroRecordsVersusMissingDataCannotBeSilentlyEquated() {
        val old = inventory(
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_SINGLE_ACTIVITY_DONE, "[]", 0,
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            agree(old, inventory())
        }
    }

    @Test fun creatorRestoreGenerationChangeIsRejected() {
        val original = inventory(evidence())
        assertThrows(IllegalArgumentException::class.java) {
            agree(original, generationStart = 2L, generationEnd = 3L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            agree(original, generationStart = -1L)
        }
    }

    @Test fun stableManifestFingerprintIsCanonicalAndSensitiveToTimingAndBytes() {
        val one = inventory(
            evidence(), TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_DONE_RECEIPTS, "[]", 0,
            ),
        )
        val two = inventory(
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_DONE_RECEIPTS, "[]", 0,
            ), evidence(),
        )
        val a = agree(one)
        val b = agree(two)
        assertEquals(a.sha256, b.sha256)
        assertNotEquals(a.sha256, agree(one, atMillis = 1_794_206_000_001L).sha256)
        assertNotEquals(a.sha256, agree(one, generationStart = 8L).sha256)
    }

    @Test fun fabricatedCompleteOrAtomicityMetadataCannotPass() {
        val old = inventory(evidence())
        assertThrows(IllegalArgumentException::class.java) {
            agree(old.copy(crossStoreAtomicityVerified = true))
        }
        assertThrows(IllegalArgumentException::class.java) {
            agree(old.copy(pendingDomains = emptyList()), old)
        }
        // Even TWO matching malicious/incomplete inventories cannot fabricate
        // full coverage: recompute the pending list from canonical domains.
        assertThrows(IllegalArgumentException::class.java) {
            val incomplete = old.copy(pendingDomains = emptyList())
            agree(incomplete, incomplete)
        }
        val newByteLength = old.inspected.single().copy(bytes = 999)
        assertThrows(IllegalArgumentException::class.java) {
            agree(old, old.copy(inspected = listOf(newByteLength)))
        }
    }
}
