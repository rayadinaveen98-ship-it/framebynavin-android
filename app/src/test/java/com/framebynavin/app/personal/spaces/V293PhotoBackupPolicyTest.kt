package com.framebynavin.app.personal.spaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure preflight policy tests. Never access real files or cloud accounts. */
class V293PhotoBackupPolicyTest {
    private val spaceId = "10000000-0000-4000-8000-0000000000a1"
    private val photoId = "30000000-0000-4000-8000-0000000000a1"
    private val space = V289PersonalSpacePolicy.newSpace(
        V289SpaceType.SKIN_CARE, 1_792_050_000_000L, spaceId
    )
    private val json = V289SpacesMetadataCodec.encode(
        V289SpaceState(spaces = listOf(space))
    )
    private val photo = V290SpacePhotoEntity(
        id = photoId, spaceId = spaceId,
        takenAtMillis = 1_792_050_000_000L,
        addedAtMillis = 1_792_050_000_100L,
        fileName = "$photoId.jpg",
        sha256Hex = "f".repeat(64),
        widthPx = 120,
        heightPx = 180,
        note = "synthetic baseline",
    )

    @Test fun emptyPhotoListCanProceedAsMetadataOnlyCandidate() {
        val candidate = V293PhotoBackupPolicy.validate(json, emptyList()) {
            error("Verifier cannot be called without photo rows")
        }
        assertEquals(0, candidate.photoCount)
        assertEquals(json, candidate.metadataJson)
    }

    @Test fun verifiedPrivatePhotoIsIncludedWithoutReadingOrUploadingNotes() {
        var checked = 0
        val candidate = V293PhotoBackupPolicy.validate(json, listOf(photo)) {
            checked++
            it.sha256Hex == photo.sha256Hex
        }
        assertEquals(1, checked)
        assertEquals(1, candidate.photoCount)
        assertEquals(photo, candidate.photos.single())
    }

    @Test fun aMissingOrChangedPhotoBlocksTheEntireCandidate() {
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(json, listOf(photo)) { false }
        }.isFailure)
    }

    @Test fun foreignSpaceAndDuplicateIdsCannotBeBackedUp() {
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(
                json, listOf(photo.copy(spaceId = "10000000-0000-4000-8000-0000000000a2"))
            ) { true }
        }.isFailure)
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(
                json, listOf(photo, photo.copy(note = "duplicate"))
            ) { true }
        }.isFailure)
    }

    @Test fun unsafePhotoFilenameOrUnsupportedMetadataIsRejected() {
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(
                json, listOf(photo.copy(fileName = "../escape.jpg"))
            ) { true }
        }.isFailure)
        val differentUuid = "30000000-0000-4000-8000-0000000000a2.jpg"
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(
                json, listOf(photo.copy(fileName = differentUuid))
            ) { true }
        }.isFailure)
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(
                json, listOf(photo.copy(sha256Hex = "bad"))
            ) { true }
        }.isFailure)
    }

    @Test fun tooManyRecordsNeverReachImageVerifier() {
        var verifierCalls = 0
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(
                json, List(V293PhotoBackupPolicy.MAX_PHOTOS + 1) { photo }
            ) {
                verifierCalls++
                true
            }
        }.isFailure)
        assertEquals(0, verifierCalls)
    }

    @Test fun checksumTamperingInSpacesMetadataBlocksPhotos() {
        assertTrue(runCatching {
            V293PhotoBackupPolicy.validate(json.replace("SKIN_CARE", "EDITING"),
                listOf(photo)) { true }
        }.isFailure)
    }
}
