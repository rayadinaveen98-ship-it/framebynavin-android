package com.framebynavin.app.personal

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure regression checks; timing/audio delivery also requires Android device QA. */
class V284DietFitnessPlanTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val installDate = LocalDate.of(2026, 11, 4) // Wednesday
    private fun plan() = PersonalDietFitnessPlanV1.templates(installDate, zone)
    private fun item(suffix: String) =
        plan().single { it.id == PersonalDietFitnessPlanV1.ID_PREFIX + suffix }

    @Test fun fixedTemplatesAreValidatedAndStable() {
        val first = plan()
        val second = plan()
        assertEquals(38, first.size)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(38, first.map { it.id }.distinct().size)
        assertTrue(first.all { it.id.startsWith(PersonalDietFitnessPlanV1.ID_PREFIX) })
        assertTrue(first.all { it.startDate == installDate && it.zoneId == zone })
        assertTrue(first.all { it.checked() === it })
    }

    @Test fun eachWeekdayHasExactlyFourSpecificMealsPlusOneDailyFruit() {
        val meals = plan().filter { it.id.contains("-meal-") }
        assertEquals(28, meals.size)
        DayOfWeek.entries.forEach { day ->
            val today = meals.filter { it.weeklyDays == setOf(day) }
            assertEquals("Missing food items for $day", 4, today.size)
            assertEquals(setOf(LocalTime.of(8, 15), LocalTime.of(13, 30),
                LocalTime.of(16, 30), LocalTime.of(19, 30)),
                today.map { it.time }.toSet())
            assertTrue(today.all { it.repeat == PersonalRepeat.WEEKLY })
            assertTrue(today.all { it.alert == PersonalAlert.NOTIFICATION && it.snoozeMinutes == 15 })
            assertTrue(today.all { it.notes.isNotBlank() })
        }
        assertEquals(LocalTime.of(11, 0), item("fruit").time)
        assertEquals(PersonalRepeat.DAILY, item("fruit").repeat)
        assertEquals(15, item("fruit").snoozeMinutes)
        assertEquals(29, plan().count { it.id.contains("-meal-") || it.id.endsWith("-fruit") })
    }

    @Test fun noChapatiAndWednesdaySundayMealsAreAccurate() {
        assertFalse(plan().any { it.notes.lowercase().contains("chapathi") ||
            it.notes.lowercase().contains("chapati") })
        assertTrue(item("meal-wednesday-breakfast").notes.contains("2 medium dosas"))
        assertTrue(item("meal-wednesday-dinner").notes.contains("whole-wheat rotis"))
        assertTrue(item("meal-sunday-lunch").notes.contains("chicken biryani"))
        assertTrue(item("meal-tuesday-breakfast").notes.contains("pesarattu"))
    }

    @Test fun strengthMobilityWalkingAndWeightDaysAreDistinct() {
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            item("workout").weeklyDays)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            item("mobility").weeklyDays)
        assertEquals(setOf(DayOfWeek.SATURDAY), item("walk-40").weeklyDays)
        assertEquals(3, plan().count { it.id.contains("walk-") })
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), item("weigh").weeklyDays)
        assertEquals(LocalTime.of(7, 5), item("weigh").time)
        assertEquals(LocalTime.of(18, 30), item("weekly-review").time)
    }

    @Test fun alertAndVoicePoliciesMatchReviewedSetup() {
        assertEquals(PersonalAlert.ALARM, item("wake").alert)
        assertEquals(LocalTime.of(7, 0), item("wake").time)
        assertEquals(PersonalAlert.VOICE, item("workout").alert)
        assertEquals(PersonalAlert.VOICE, item("walk-30").alert)
        assertEquals(PersonalAlert.VOICE, item("sleep").alert)
        assertEquals(LocalTime.of(22, 15), item("sleep").time)
        assertTrue(plan().all { it.voiceRepeatCount == 3 && !it.autoRetryEnabled })
        assertTrue(plan().filter { it.alert == PersonalAlert.VOICE }
            .all { it.snoozeMinutes == 10 })
    }

    @Test fun recurrenceSkipsNonSelectedDaysAndDoesNotDriftToDaily() {
        val wednesdayNoon = installDate.atTime(12, 0).atZone(zone)
            .toInstant().toEpochMilli()
        val thuBreakfast = PersonalRecurrence.nextAfter(
            item("meal-thursday-breakfast"), wednesdayNoon)!!
        val expected = installDate.plusDays(1).atTime(8, 15).atZone(zone)
            .toInstant().toEpochMilli()
        assertEquals(expected, thuBreakfast)
        val nextWedBreakfast = PersonalRecurrence.nextAfter(
            item("meal-wednesday-breakfast"), wednesdayNoon)!!
        assertEquals(installDate.plusWeeks(1).atTime(8, 15).atZone(zone)
            .toInstant().toEpochMilli(), nextWedBreakfast)
        assertNotEquals(thuBreakfast, nextWedBreakfast)
    }

    @Test fun repeatInstallNeverOverwritesEditsOrOccurrenceReceipts() {
        val original = plan()
        val customized = original.first().copy(
            title = "My own wake alarm",
            snoozeMinutes = 30,
            pendingOccurrenceMillis = 0,
            pendingFireAtMillis = 0,
        )
        val already = listOf(customized,
            PersonalReminder(id = "custom-keep", title = "Keep my reminder",
                startDate = installDate, time = LocalTime.NOON))
        val merged = PersonalDietFitnessPlanV1.mergeMissing(already, original)
        assertEquals(original.size + 1, merged.size)
        assertEquals(customized, merged.first())
        assertEquals("Keep my reminder", merged[1].title)
        assertEquals(merged, PersonalDietFitnessPlanV1.mergeMissing(merged, original))
        assertEquals(merged.map { it.id }.distinct().size, merged.size)
    }
}
