package com.framebynavin.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** No Android runtime required: pure, fail-closed owner-data coverage policy. */
class TrustBackupInventoryPolicyTest {
    @Test fun verifiedEmptyIsNotInventedAsCompleteBackup() {
        val empty = TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.PERSONAL_SINGLE_ACTIVITY, """{"items":[]}""", 0,
        )
        val preview = TrustBackupInventoryPolicy.assemble(listOf(empty))
        assertEquals(0, preview.inspected.single().records)
        assertEquals(64, preview.inspected.single().sha256.length)
        assertTrue(TrustBackupDomain.CREATOR_PROJECTS in preview.pendingDomains)
        assertTrue(TrustBackupDomain.PERSONAL_PHOTO_BYTES in preview.pendingDomains)
        assertFalse(preview.crossStoreAtomicityVerified)
        assertFalse(preview.canClaimCompleteBackup)
        assertFalse(preview.canUploadAsUniversalBackup)
    }

    @Test fun exactReceiptBytesProduceDistinctFingerprintsWithoutLeakingValues() {
        val first = TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.PERSONAL_SINGLE_ACTIVITY_DONE,
            """{"confirmedAt":"2026-11-09T04:00:00Z"}""", 1,
        )
        val changed = TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.PERSONAL_SINGLE_ACTIVITY_DONE,
            """{"confirmedAt":"2026-11-09T06:00:00Z"}""", 1,
        )
        assertNotEquals(first.sha256, changed.sha256)
        assertFalse(first.toString().contains("confirmedAt"))
        assertFalse(first.toString().contains("2026-11-09"))
    }

    @Test fun unicodeTextIsHashedAsRealUtf8NotCharacterLength() {
        val source = "తెలుగు — camera"
        val proof = TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.CREATOR_IDEAS, source, 1,
        )
        assertEquals(source.toByteArray(Charsets.UTF_8).size, proof.bytes)
        assertTrue(proof.bytes > source.length)
    }

    @Test fun duplicateDomainsCannotMasqueradeAsIndependentCoverage() {
        val x = TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.CREATOR_PROJECTS, "[]", 0,
        )
        assertThrows(IllegalArgumentException::class.java) {
            TrustBackupInventoryPolicy.assemble(listOf(x, x))
        }
    }

    @Test fun tokensAndOsPermissionsAreNeverPortablePayloadDomains() {
        for (domain in TrustBackupDomain.entries.filterNot { it.portable }) {
            assertThrows(IllegalArgumentException::class.java) {
                TrustBackupInventoryPolicy.evidence(domain, "confidential", 1)
            }
        }
        val inventory = TrustBackupInventoryPolicy.assemble(emptyList())
        assertEquals(
            TrustBackupDomain.entries.filterNot { it.portable }, inventory.exclusions,
        )
        assertTrue(inventory.inspected.isEmpty())
        assertFalse(inventory.canClaimCompleteBackup)
    }

    @Test fun evidenceCannotHaveNegativeCountsOrInventedChecksum() {
        assertThrows(IllegalArgumentException::class.java) {
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.CREATOR_FOCUS, "[]", -1,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustDomainEvidence(TrustBackupDomain.CREATOR_FOCUS, 0, 2, "bad")
        }
    }
}
