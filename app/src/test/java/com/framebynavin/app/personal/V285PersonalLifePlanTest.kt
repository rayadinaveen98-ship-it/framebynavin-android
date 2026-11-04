package com.framebynavin.app.personal

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure schedule and personal data safeguards; Android delivery needs device QA. */
class V285PersonalLifePlanTest {
    private val plan = V285PersonalLifePlan
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun exactlyTwentyOneConsecutiveDaysAreDefined() {
        assertEquals(LocalDate.of(2026, 11, 5), plan.firstDay)
        assertEquals(LocalDate.of(2026, 11, 25), plan.lastDay)
        assertEquals(21, plan.days.size)
        assertEquals(plan.days.map { it.date }.toSet().size, 21)
        plan.days.forEachIndexed { i, d ->
            assertEquals(plan.firstDay.plusDays(i.toLong()), d.date)
        }
    }

    @Test fun launchAndFridayDatesCannotSlipOrBackdate() {
        assertEquals("NOV 7 · FIRST UPLOAD", plan.focusOn(LocalDate.of(2026, 11, 7))?.publishing)
        assertEquals(null, plan.focusOn(LocalDate.of(2026, 11, 6))?.publishing)
        assertEquals("FRIDAY · REVIEW", plan.focusOn(LocalDate.of(2026, 11, 13))?.publishing)
        assertEquals("FRIDAY · REVIEW", plan.focusOn(LocalDate.of(2026, 11, 20))?.publishing)
        assertEquals("SUNDAY · FLAGSHIP", plan.focusOn(LocalDate.of(2026, 11, 15))?.publishing)
        assertEquals("SUNDAY · FLAGSHIP", plan.focusOn(LocalDate.of(2026, 11, 22))?.publishing)
    }

    @Test fun newRoutineRemindersAreDistinctFromV284AndHaveStableIds() {
        val templates = plan.templates(plan.firstDay, zone)
        assertEquals(10, templates.size)
        assertEquals(10, templates.map { it.id }.toSet().size)
        assertTrue(templates.all { it.id.startsWith(plan.ID_PREFIX) })
        assertTrue(templates.none { it.id.startsWith(PersonalDietFitnessPlanV1.ID_PREFIX) })
        assertTrue(templates.all { it.alert == PersonalAlert.NOTIFICATION })
        assertTrue(templates.all { !it.autoRetryEnabled && it.snoozeMinutes == 10 })
        assertTrue(templates.all { it.voiceRepeatCount == 3 })
    }

    @Test fun selectedCategoriesOnlyInstallTheirOwnRecords() {
        val onlyReading = plan.templates(plan.firstDay, zone,
            setOf(V285PersonalLifePlan.Category.REST_AND_READING))
        assertEquals(2, onlyReading.size)
        assertEquals(setOf("life21-v1-nap", "life21-v1-novel"), onlyReading.map { it.id }.toSet())
        assertTrue(plan.templates(plan.firstDay, zone, emptySet()).isEmpty())
    }

    @Test fun nightReadingBeginsAtTenAndGameWorkOnlyOnChosenWeekdays() {
        val templates = plan.templates(plan.firstDay, zone)
        val reading = templates.single { it.id == "life21-v1-novel" }
        assertEquals(LocalTime.of(22, 0), reading.time)
        assertEquals(PersonalRepeat.DAILY, reading.repeat)
        val game = templates.single { it.id == "life21-v1-game" }
        assertEquals(LocalTime.of(17, 45), game.time)
        assertEquals(PersonalRepeat.WEEKLY, game.repeat)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), game.weeklyDays)
    }

    @Test fun repeatedInstallNeverOverwritesExistingEdits() {
        val current = plan.templates(plan.firstDay, zone)
        val edited = current.first().copy(title = "My own skincare routine", snoozeMinutes = 30)
        val other = PersonalDietFitnessPlanV1.templates(plan.firstDay, zone).first()
        val merged = plan.mergeMissing(listOf(edited, other), current)
        assertEquals(current.size + 1, merged.size)
        assertEquals(edited, merged.first())
        assertEquals(other, merged[1])
        val repeated = plan.mergeMissing(merged, current)
        assertEquals(merged, repeated)
    }

    @Test fun readingConflictRecognizesOnlyUnpausedV284WindDownInsideReadingBlock() {
        val v284 = PersonalDietFitnessPlanV1.templates(plan.firstDay, zone)
            .single { it.id == PersonalDietFitnessPlanV1.ID_PREFIX + "sleep" }
        assertEquals(LocalTime.of(22, 15), v284.time)
        assertTrue(plan.hasReadingConflict(listOf(v284)))
        assertFalse(plan.hasReadingConflict(listOf(v284.copy(time = LocalTime.of(22, 40)))))
        assertFalse(plan.hasReadingConflict(listOf(v284.copy(paused = true))))
    }

    @Test fun recurringRoutinesRemainEditableWithoutCreatorWrites() {
        val templates = plan.templates(LocalDate.of(2026, 11, 12), zone)
        assertTrue(templates.all { it.startDate == LocalDate.of(2026, 11, 12) })
        assertTrue(templates.all { it.weeklyDays.isEmpty() ||
            DayOfWeek.SUNDAY !in it.weeklyDays })
        assertTrue(templates.all { it.notes.isNotBlank() })
    }
}
