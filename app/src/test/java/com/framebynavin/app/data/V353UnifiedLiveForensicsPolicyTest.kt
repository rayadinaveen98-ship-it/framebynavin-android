package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic crash-report policy: no Android storage, Room or media writes. */
class V353UnifiedLiveForensicsPolicyTest {
    private fun report(
        creatorMatches: Boolean = true,
        personalMatches: Boolean = true,
        photos: V354SpacePhotoAuditResult =
            V354SpacePhotoAuditResult(1, 1, 0, 0, 0, 0),
    ) = V353UnifiedLiveForensicReport(
        transactionId = UUID.randomUUID().toString(),
        checkpointStep = V344UnifiedRestoreStep.BEFORE_PERSONAL,
        creatorLiveMatchesOriginalMetadata = creatorMatches,
        personalLiveMatchesOriginalMetadata = personalMatches,
        creatorCurrentMetadataSha256 = "a".repeat(64),
        personalCurrentMetadataSha256 = "b".repeat(64),
        originalCreatorMetadataSha256 = "a".repeat(64),
        originalPersonalMetadataSha256 = "b".repeat(64),
        livePhotoAudit = photos,
        intendedPersonalRecipe = V355UnifiedPersonalRecoveryRecipe(
            originalDigest = "a".repeat(64),
            mergedTargetDigest = "b".repeat(64),
            incomingAdditions = 1,
            originalPhotoCount = 1,
            mergedTargetPhotoCount = 1,
            newPhotoCount = 0,
            allOriginalPhotoReferencesPreserved = true,
        ),
        privatePhotoByteIntegrityProven = false,
        mediaRollbackProven = false,
        mayWriteLiveData = false,
        mayReleaseRecoveryJournals = false,
    )

    @Test fun evenMatchingCreatorPersonalAndCurrentPhotoShaNeverGrantsRollback() {
        val r = report()
        assertTrue(r.creatorLiveMatchesOriginalMetadata)
        assertTrue(r.personalLiveMatchesOriginalMetadata)
        assertTrue(r.livePhotoAudit.currentPhotoFilesMatchTheirLiveRows)
        assertFalse(r.livePhotoAudit.authorizesCombinedMediaRollback)
        assertFalse(V353UnifiedLiveForensicsPolicy.allowsRecoveryWrites(r))
        assertFalse(V353UnifiedLiveForensicsPolicy.allowsJournalRelease(r))
    }

    @Test fun orphanFilesAndChangedPersonalDataAlsoKeepRecoveryLocked() {
        val r = report(personalMatches = false,
            photos = V354SpacePhotoAuditResult(1, 1, 0, 0, 1, 0))
        assertFalse(r.personalLiveMatchesOriginalMetadata)
        assertFalse(r.livePhotoAudit.currentPhotoFilesMatchTheirLiveRows)
        assertFalse(V353UnifiedLiveForensicsPolicy.allowsRecoveryWrites(r))
        assertFalse(V353UnifiedLiveForensicsPolicy.allowsJournalRelease(r))
    }

    @Test fun reportRejectsAccidentalPromotionToLiveWriteAuthority() {
        assertThrows(IllegalArgumentException::class.java) {
            report().copy(mayWriteLiveData = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            report().copy(mediaRollbackProven = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            report().copy(privatePhotoByteIntegrityProven = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            report().copy(mayReleaseRecoveryJournals = true)
        }
    }
}
