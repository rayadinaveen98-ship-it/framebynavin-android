package com.framebynavin.app.personal.spaces

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V289 owner-selected file IO never auto-uploads or opens a default destination.
 * Test pure bounded-reader behavior without Android file access.
 */
class V289SpacesOwnerBackupFileTest {
    @Test fun readsValidUtf8MetadataWithoutChangingContents() {
        val space = V289PersonalSpacePolicy.newSpace(
            V289SpaceType.READING, 1_792_050_000_000L,
            "10000000-0000-4000-8000-000000000001"
        )
        val raw = V289SpacesMetadataCodec.encode(
            V289SpaceState(spaces = listOf(space.copy(goalText = "పుస్తకాలు చదవాలి")))
        )
        val loaded = V289SpacesOwnerBackupFile.readLimited(
            ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8))
        )
        assertEquals(raw, loaded)
        assertEquals(
            "పుస్తకాలు చదవాలి",
            V289SpacesMetadataCodec.decode(loaded).spaces.single().goalText
        )
    }

    @Test fun emptyFileFailsBeforeImport() {
        assertTrue(runCatching {
            V289SpacesOwnerBackupFile.readLimited(ByteArrayInputStream(byteArrayOf()))
        }.isFailure)
    }

    @Test fun massiveFileIsRejectedBeforeParsing() {
        val beyond = "x".repeat(V289SpacesOwnerBackupFile.MAX_FILE_CHARS + 1)
        assertTrue(runCatching {
            V289SpacesOwnerBackupFile.readLimited(
                ByteArrayInputStream(beyond.toByteArray(Charsets.UTF_8))
            )
        }.isFailure)
    }

    @Test fun ordinaryMalformedJsonIsReadButRejectedByChecksumValidation() {
        val loaded = V289SpacesOwnerBackupFile.readLimited(
            ByteArrayInputStream("{\"spaces\":[]}".toByteArray(Charsets.UTF_8))
        )
        assertTrue(runCatching { V289SpacesMetadataCodec.decode(loaded) }.isFailure)
    }
    @Test fun readBackVerificationAcceptsExactSavedCopyWithUnicodeNotes() {
        val space = V289PersonalSpacePolicy.newSpace(
            V289SpaceType.SKIN_CARE,
            1_792_050_000_000L,
            "10000000-0000-4000-8000-000000000001",
        )
        val expected = V289SpacesMetadataCodec.encode(V289SpaceState(
            spaces = listOf(space.copy(goalText = "వారానికి మూడు రోజులు")),
        ))
        V289SpacesOwnerBackupFile.verifyWrittenCopy(expected, expected)
    }

    @Test fun readBackVerificationRejectsTruncatedOrChangedFile() {
        val raw = V289SpacesMetadataCodec.encode(V289SpaceState())
        assertTrue(runCatching {
            V289SpacesOwnerBackupFile.verifyWrittenCopy(raw, raw.dropLast(1))
        }.isFailure)
        assertTrue(runCatching {
            V289SpacesOwnerBackupFile.verifyWrittenCopy(raw, raw + " ")
        }.isFailure)
    }

    @Test fun readBackVerificationRejectsInvalidChecksumEvenIfCopiesMatch() {
        val malformed = """{"format":"backlot.personal.spaces","spaces":[]}"""
        assertTrue(runCatching {
            V289SpacesOwnerBackupFile.verifyWrittenCopy(malformed, malformed)
        }.isFailure)
    }

}
