package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime

/** Pure, deterministic rule for edits. No store, alarm or UI side effects. */
internal object Phase1HabitManagementPolicy {
    fun reviewedEdit(
        before: Phase1HabitChallenge,
        activity: String,
        startDate: LocalDate,
        alertTime: LocalTime?,
        alertKind: Phase1HabitAlertKind?,
        completionMode: Phase1HabitCompletionMode,
    ): Phase1HabitChallenge {
        before.checked()
        require(!before.archived) { "Restore an archived challenge before editing" }
        require(startDate == before.startDate ||
            (before.confirmations.isEmpty() && before.dayNotes.isEmpty())) {
            "Change the start date only before recording any Done days or notes"
        }
        val scheduleChanged = startDate != before.startDate ||
            alertTime != before.alertTime || alertKind != before.alertKind
        val next = before.copy(
            activity = activity.trim(),
            startDate = startDate,
            alertTime = alertTime,
            alertKind = alertKind,
            completionMode = completionMode,
            snoozedDay = if (scheduleChanged) null else before.snoozedDay,
            snoozedUntil = if (scheduleChanged) null else before.snoozedUntil,
        ).checked()
        check(next.confirmations == before.confirmations &&
            next.dayNotes == before.dayNotes &&
            next.linkedSpaceId == before.linkedSpaceId &&
            next.linkedGroupId == before.linkedGroupId) {
            "Edit may not modify confirmations, notes or links"
        }
        return next
    }
}
