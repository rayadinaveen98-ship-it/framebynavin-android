package com.framebynavin.app.personal.spaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure offline tests: no automatic reminders, health inference or account access. */
class V292SpaceMetricSuggestionsTest {
    @Test fun categoryPresetsHaveClearUnits() {
        val suggestions = V292SpaceMetricSuggestions
        assertEquals("minutes", suggestions.forType(V289SpaceType.FITNESS)?.unit)
        assertEquals("pages", suggestions.forType(V289SpaceType.READING)?.unit)
        assertEquals("Editing practice", suggestions.forType(V289SpaceType.EDITING)?.name)
        assertEquals("Building time", suggestions.forType(V289SpaceType.GAME_DEVELOPMENT)?.name)
        assertEquals("Playing time", suggestions.forType(V289SpaceType.GAMING)?.name)
        assertNull(suggestions.forType(V289SpaceType.SKIN_CARE))
        assertNull(suggestions.forType(V289SpaceType.CUSTOM))
    }

    @Test fun parsedMinutesAreBoundedPositiveWholeNumbers() {
        val preset = requireNotNull(V292SpaceMetricSuggestions.forType(V289SpaceType.FITNESS))
        assertEquals(30.0, preset.parseOrNull("30")!!, 0.0)
        assertEquals(1440.0, preset.parseOrNull("1440")!!, 0.0)
        assertNull(preset.parseOrNull("0"))
        assertNull(preset.parseOrNull("1441"))
        assertNull(preset.parseOrNull("-30"))
        assertNull(preset.parseOrNull("12.5"))
    }

    @Test fun rejectsUnexpectedTextAndEnormousInput() {
        val preset = requireNotNull(V292SpaceMetricSuggestions.forType(V289SpaceType.READING))
        for (input in listOf("", " ", "one", "2e3", "Inf", "NaN", "200000", "1;1", "1,000")) {
            assertNull("Must reject: $input", preset.parseOrNull(input))
        }
        assertEquals(10000.0, preset.parseOrNull("10000")!!, 0.0)
        assertNull(preset.parseOrNull("10001"))
    }

    @Test fun valuesAreOnlyLoggedAfterUserChoosesToRecord() {
        val preset = requireNotNull(V292SpaceMetricSuggestions.forType(V289SpaceType.EDITING))
        val value = preset.parseOrNull("45")!!
        val entry = V289SpaceEntry(
            id = "20000000-0000-4000-8000-000000000081",
            spaceId = "10000000-0000-4000-8000-000000000081",
            kind = V289SpaceEntryKind.METRIC,
            source = V289SpaceEntrySource.MANUAL,
            occurredAtMillis = 1_792_050_000_000L,
            createdAtMillis = 1_792_050_000_000L,
            note = "Practiced cuts",
            metricName = preset.name,
            metricValue = value,
            metricUnit = preset.unit,
        )
        assertEquals(entry, V289PersonalSpacePolicy.validateEntry(entry))
        assertTrue(entry.reminderId == null && entry.originalOccurrenceMillis == null)
    }

    @Test fun recordedMetricSurvivesSpacesMetadataBackupRoundtrip() {
        val space = V289PersonalSpacePolicy.newSpace(
            V289SpaceType.READING,
            1_792_050_000_000L,
            "10000000-0000-4000-8000-000000000081",
        )
        val entry = V289SpaceEntry(
            id = "20000000-0000-4000-8000-000000000081",
            spaceId = space.id,
            kind = V289SpaceEntryKind.METRIC,
            source = V289SpaceEntrySource.MANUAL,
            occurredAtMillis = 1_792_050_100_000L,
            createdAtMillis = 1_792_050_100_000L,
            note = "A chapter completed",
            metricName = "Pages read",
            metricValue = 32.0,
            metricUnit = "pages",
        )
        val raw = V289SpacesMetadataCodec.encode(
            V289SpaceState(spaces = listOf(space), entries = listOf(entry))
        )
        val decoded = V289SpacesMetadataCodec.decode(raw)
        assertEquals(space, decoded.spaces.single())
        assertEquals(entry, decoded.entries.single())
        assertTrue(decoded.links.isEmpty())
    }

    @Test fun historicDecimalMetricsDisplayWithoutRounding() {
        val describe = V292SpaceMetricSuggestions::describe
        assertEquals("Pages read: 23 pages", describe("Pages read", 23.0, "pages"))
        assertEquals("Bodyweight: 72.5 kg", describe("Bodyweight", 72.5, "kg"))
        assertTrue(runCatching { describe("Bad", Double.NaN, "minutes") }.isFailure)
    }
}
