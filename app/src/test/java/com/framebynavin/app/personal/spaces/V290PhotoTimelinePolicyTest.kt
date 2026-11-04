package com.framebynavin.app.personal.spaces

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** No assumptions about appearance, fitness success, treatment or medical results. */
class V290PhotoTimelinePolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val start = LocalDate.of(2026, 11, 4)
    private val space = "10000000-0000-4000-8000-000000000001"
    private fun timestamp(day: LocalDate, h: Int = 9) =
        day.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()
    private fun photo(
        id: Int,
        day: LocalDate,
        spaceId: String = space,
    ): V290SpacePhotoEntity {
        val key = "20000000-0000-4000-8000-" + id.toString().padStart(12, '0')
        return V290SpacePhotoEntity(
            id = key, spaceId = spaceId, takenAtMillis = timestamp(day),
            addedAtMillis = timestamp(day), fileName = "$key.jpg",
            sha256Hex = "a".repeat(64), widthPx = 1600, heightPx = 900,
            note = "",
        )
    }

    @Test fun noPhotoOrSinglePhotoNeverPretendsThereIsAComparison() {
        val now = timestamp(start.plusDays(15))
        assertNull(V290PhotoTimelinePolicy.compare(emptyList(), now, zone))
        assertNull(V290PhotoTimelinePolicy.compare(listOf(photo(1, start)), now, zone))
    }

    @Test fun exactlyFourteenCalendarDaysIsEligibleAndThirteenIsNot() {
        val now = timestamp(start.plusDays(15))
        val first = photo(1, start)
        val early = photo(2, start.plusDays(13))
        val target = photo(3, start.plusDays(14))
        val thirteen = V290PhotoTimelinePolicy.compare(listOf(first, early), now, zone)!!
        assertEquals(13L, thirteen.daysBetween)
        assertFalse(thirteen.meetsFourteenDayGoal)
        val fourteen = V290PhotoTimelinePolicy.compare(listOf(first, target), now, zone)!!
        assertEquals(14L, fourteen.daysBetween)
        assertTrue(fourteen.meetsFourteenDayGoal)
    }

    @Test fun unorderedPhotosUseOldestBaselineAndLatestValidComparison() {
        val now = timestamp(start.plusDays(30))
        val p1 = photo(1, start)
        val p2 = photo(2, start.plusDays(8))
        val p3 = photo(3, start.plusDays(20))
        val actual = V290PhotoTimelinePolicy.compare(listOf(p2, p3, p1), now, zone)!!
        assertEquals(p1.id, actual.baseline.id)
        assertEquals(p3.id, actual.comparison.id)
        assertEquals(20L, actual.daysBetween)
    }

    @Test fun futureDatedPhotosAreNeverShownAsProgress() {
        val now = timestamp(start.plusDays(10))
        assertNull(V290PhotoTimelinePolicy.compare(listOf(
            photo(1, start),
            photo(2, start.plusDays(14)),
        ), now, zone))
    }

    @Test fun refuseCrossSpaceComparisonAndUnsafeFileNames() {
        val now = timestamp(start.plusDays(20))
        assertTrue(runCatching {
            V290PhotoTimelinePolicy.compare(listOf(
                photo(1, start),
                photo(2, start.plusDays(15), "10000000-0000-4000-8000-000000000002"),
            ), now, zone)
        }.isFailure)
        assertTrue(runCatching {
            V290PhotoTimelinePolicy.validate(photo(1, start).copy(fileName = "../other.jpg"))
        }.isFailure)
        assertTrue(runCatching {
            V290PhotoTimelinePolicy.validate(photo(1, start).copy(sha256Hex = "wrong"))
        }.isFailure)
    }

    @Test fun notesAndImageSizesHaveHardSafetyBounds() {
        val first = photo(1, start)
        assertTrue(runCatching {
            V290PhotoTimelinePolicy.validate(first.copy(widthPx = 0))
        }.isFailure)
        assertTrue(runCatching {
            V290PhotoTimelinePolicy.validate(first.copy(heightPx = 40000))
        }.isFailure)
        assertTrue(runCatching {
            V290PhotoTimelinePolicy.validate(first.copy(note = "x".repeat(501)))
        }.isFailure)
    }
}
