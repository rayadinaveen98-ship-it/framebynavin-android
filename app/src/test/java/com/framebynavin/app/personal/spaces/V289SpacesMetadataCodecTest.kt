package com.framebynavin.app.personal.spaces

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/** Pure metadata recovery: no Android Storage Access Framework, cloud or reminder writes. */
class V289SpacesMetadataCodecTest {
    private val space = V289PersonalSpacePolicy.newSpace(
        V289SpaceType.SKIN_CARE,
        1_792_050_000_000,
        "10000000-0000-4000-8000-000000000001",
    )
    private val manual = V289SpaceEntry(
        id = "20000000-0000-4000-8000-000000000001",
        spaceId = space.id,
        kind = V289SpaceEntryKind.CHECK_IN,
        source = V289SpaceEntrySource.MANUAL,
        occurredAtMillis = 1_792_050_000_100,
        createdAtMillis = 1_792_050_000_200,
        note = "Face wash, sunscreen. No photos.",
    )
    private val linked = V289SpaceReminderLink(
        spaceId = space.id, reminderId = "life21-v1-skin-am", linkedAtMillis = 1_792_050_000_300
    )
    private val state = V289SpaceState(listOf(space), listOf(linked), listOf(manual))

    private fun rewritePayload(raw: String, edit: (JSONObject) -> Unit): String {
        val env = JSONObject(raw)
        val payload = JSONObject(env.getString("payload"))
        edit(payload)
        val changed = payload.toString()
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(changed.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return env.put("payload", changed).put("payloadSha256", hash).toString()
    }

    @Test fun emptyRoundTripHasExplicitFormatAndNoFakeHistory() {
        val encoded = V289SpacesMetadataCodec.encode(V289SpaceState())
        assertEquals(V289SpaceState(), V289SpacesMetadataCodec.decode(encoded))
        val root = JSONObject(encoded)
        assertEquals("backlot.personal.spaces.metadata", root.getString("format"))
        assertEquals(1, root.getInt("schemaVersion"))
        assertFalse(root.toString().contains("file://"))
    }

    @Test fun roundTripPreservesSpaceLinkManualNotesAndArchiveDate() {
        val archived = state.copy(spaces = listOf(space.copy(archivedAtMillis = space.createdAtMillis + 1)))
        val raw = V289SpacesMetadataCodec.encode(archived)
        assertEquals(archived, V289SpacesMetadataCodec.decode(raw))
        assertTrue(raw.contains("sunscreen"))
        assertFalse(raw.contains("photoUri"))
        assertFalse(raw.contains("reminderTitle"))
    }

    @Test fun canonicalOrderMakesEqualDataProduceSameBackup() {
        val second = V289PersonalSpacePolicy.newSpace(
            V289SpaceType.READING,
            1_792_050_000_001,
            "10000000-0000-4000-8000-000000000002"
        )
        val first = V289SpaceState(
            spaces = listOf(second, space), links = listOf(linked), entries = listOf(manual)
        )
        val secondOrder = first.copy(spaces = first.spaces.reversed())
        assertEquals(V289SpacesMetadataCodec.encode(first),
            V289SpacesMetadataCodec.encode(secondOrder))
    }

    @Test fun checksumOrVersionTamperingFailsClosed() {
        val raw = V289SpacesMetadataCodec.encode(state)
        val tampered = JSONObject(raw).put("payload", "{}").toString()
        assertTrue(runCatching { V289SpacesMetadataCodec.decode(tampered) }.isFailure)
        val unsupported = JSONObject(raw).put("schemaVersion", 2).toString()
        assertTrue(runCatching { V289SpacesMetadataCodec.decode(unsupported) }.isFailure)
        val wrongFormat = JSONObject(raw).put("format", "backlot.reminders").toString()
        assertTrue(runCatching { V289SpacesMetadataCodec.decode(wrongFormat) }.isFailure)
    }

    @Test fun duplicateAndOrphanEntriesAndLinksNeverDecode() {
        val raw = V289SpacesMetadataCodec.encode(state)
        assertTrue(runCatching {
            V289SpacesMetadataCodec.decode(rewritePayload(raw) {
                it.getJSONArray("spaces").put(JSONObject(it.getJSONArray("spaces").getJSONObject(0).toString()))
            })
        }.isFailure)
        assertTrue(runCatching {
            V289SpacesMetadataCodec.decode(rewritePayload(raw) {
                it.getJSONArray("links").getJSONObject(0).put(
                    "spaceId", "10000000-0000-4000-8000-000000000099"
                )
            })
        }.isFailure)
        assertTrue(runCatching {
            V289SpacesMetadataCodec.decode(rewritePayload(raw) {
                it.getJSONArray("entries").put(JSONObject(it.getJSONArray("entries").getJSONObject(0).toString()))
            })
        }.isFailure)
    }

    @Test fun forgedDoneReceiptIsNotRestorableEvenIfChecksumIsValid() {
        val raw = V289SpacesMetadataCodec.encode(state)
        assertTrue(runCatching {
            V289SpacesMetadataCodec.decode(rewritePayload(raw) {
                it.getJSONArray("entries").getJSONObject(0).put(
                    "source", V289SpaceEntrySource.CONFIRMED_REMINDER_DONE.name
                ).put("reminderId", "some-saved-id")
                    .put("originalOccurrenceMillis", 1_792_050_000_100)
            })
        }.isFailure)
        assertTrue(runCatching {
            V289SpacesMetadataCodec.encode(state.copy(entries = listOf(manual.copy(
                source = V289SpaceEntrySource.CONFIRMED_REMINDER_DONE,
                reminderId = "some-saved-id",
                originalOccurrenceMillis = 1_792_050_000_100,
            ))))
        }.isFailure)
    }

    @Test fun malformedMetricOrOversizedBackupIsRejected() {
        val bad = manual.copy(
            kind = V289SpaceEntryKind.METRIC,
            metricName = "Minutes", metricUnit = "minutes",
            metricValue = Double.NaN,
        )
        assertTrue(runCatching {
            V289SpacesMetadataCodec.encode(state.copy(entries = listOf(bad)))
        }.isFailure)
        val giant = "x".repeat(4_000_001)
        assertTrue(runCatching { V289SpacesMetadataCodec.decode(giant) }.isFailure)
        assertTrue(runCatching {
            V289SpacesMetadataCodec.encode(state.copy(entries = listOf(manual.copy(
                note = "x".repeat(2001)
            ))))
        }.isFailure)
    }
}
