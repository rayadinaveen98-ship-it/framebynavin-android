package com.framebynavin.app.personal

/**
 * Optional planning shortcuts, never healthcare diagnosis/advice. Personal only:
 * no CreatorTask, cloud, YouTube or analytics classification.
 *
 * Persist enum names, not user-facing copy, so future wording can improve
 * without breaking locally backed up reminder records.
 */
internal enum class PersonalPurpose(
    val label: String,
    val supportingText: String,
    val suggestedTitle: String,
) {
    CUSTOM("My reminder", "Keep it personal", ""),
    HYDRATION("Hydration", "A gentle water break", "Have some water"),
    MOVEMENT("Movement", "Stretch or step away", "Take a movement break"),
    REST("Rest", "Give yourself a pause", "Take a short break"),
    MINDFULNESS("Mindfulness", "Make room for quiet", "Take a mindful moment"),
    SLEEP("Sleep routine", "Settle into your evening", "Prepare for rest"),
    CONNECTION("Connection", "Make time for someone", "Check in with someone"),
    PERSONAL_GOAL("Personal goals", "A little progress today", "Work on a personal goal"),
}

internal object PersonalSelfCarePresets {
    fun chooseTitle(currentTitle: String, oldPurpose: PersonalPurpose,
                    newPurpose: PersonalPurpose): String =
        if (currentTitle.isBlank() || currentTitle == oldPurpose.suggestedTitle)
            newPurpose.suggestedTitle else currentTitle

    fun scheduleLabel(repeat: PersonalRepeat, everyHours: Int, everyDays: Int,
                      selectedDays: Set<java.time.DayOfWeek>): String = when (repeat) {
        PersonalRepeat.ONCE -> "One-time reminder"
        PersonalRepeat.DAILY -> "Every day"
        PersonalRepeat.WEEKDAYS -> "Weekdays"
        PersonalRepeat.WEEKENDS -> "Weekends"
        PersonalRepeat.WEEKLY -> "Weekly · ${selectedDays.sortedBy { it.value }.joinToString(", ") { it.name.take(3) }}"
        PersonalRepeat.MONTHLY -> "Each month"
        PersonalRepeat.EVERY_N_DAYS -> "Every ${everyDays} day${if (everyDays == 1) "" else "s"}"
        PersonalRepeat.EVERY_N_HOURS ->
            "Every ${everyHours} hour${if (everyHours == 1) "" else "s"}" +
                if (selectedDays.isEmpty()) " · Daily"
                else " · ${selectedDays.sortedBy { it.value }.joinToString(", ") { it.name.take(3) }}"
    }
}
