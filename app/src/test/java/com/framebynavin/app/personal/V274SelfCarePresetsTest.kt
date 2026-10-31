package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class V274SelfCarePresetsTest {
    @Test fun choosingPurposeNeverOverwritesActualUserText() {
        assertEquals("My own important reason",
            PersonalSelfCarePresets.chooseTitle("My own important reason",
                PersonalPurpose.HYDRATION, PersonalPurpose.REST))
        assertEquals("Take a short break",
            PersonalSelfCarePresets.chooseTitle("Have some water",
                PersonalPurpose.HYDRATION, PersonalPurpose.REST))
        assertEquals("Have some water",
            PersonalSelfCarePresets.chooseTitle("",
                PersonalPurpose.CUSTOM, PersonalPurpose.HYDRATION))
        assertEquals("", PersonalSelfCarePresets.chooseTitle("Take a short break",
                PersonalPurpose.REST, PersonalPurpose.CUSTOM))
    }

    @Test fun categoriesAreOptionalLocalPlanningPresetsNotMedicalRules() {
        assertEquals(PersonalPurpose.CUSTOM, PersonalReminder(
            title="Custom reminder",
            startDate=java.time.LocalDate.parse("2026-11-05"),
            time=java.time.LocalTime.NOON
        ).purpose)
        assertTrue(PersonalPurpose.entries.size >= 6)
        assertTrue(PersonalPurpose.entries.all { it.label.isNotBlank() &&
            it.supportingText.isNotBlank() })
    }

    @Test fun plannerShowsTruthfulHourlyAndWeekendCopy() {
        assertEquals("Every 3 hours · Daily", PersonalSelfCarePresets.scheduleLabel(
            PersonalRepeat.EVERY_N_HOURS,3,1,emptySet()))
        assertEquals("Weekends", PersonalSelfCarePresets.scheduleLabel(
            PersonalRepeat.WEEKENDS,3,1,emptySet()))
        assertEquals("Every 2 days", PersonalSelfCarePresets.scheduleLabel(
            PersonalRepeat.EVERY_N_DAYS,3,2,emptySet()))
        assertEquals("Every 6 hours · SAT, SUN", PersonalSelfCarePresets.scheduleLabel(
            PersonalRepeat.EVERY_N_HOURS,6,1,setOf(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY)))
    }
}
