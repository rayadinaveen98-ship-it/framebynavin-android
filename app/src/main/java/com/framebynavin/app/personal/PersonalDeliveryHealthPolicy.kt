package com.framebynavin.app.personal

/** Read-only status projection. No claim that Android actually displayed an alert.
 * A posted lease only proves the app recorded an Android notification handoff.
 * No alarm, notification or DataStore writes are performed here.
 */
internal enum class PersonalDeliveryHealthState {
    PAUSED, AWAITING_RESPONSE, EARLIER_MISSED, USER_DEFERRED,
    DUE_UNCONFIRMED, LATE_UNCONFIRMED, UPCOMING, NO_UPCOMING,
}

internal data class PersonalDeliveryHealth(
    val state: PersonalDeliveryHealthState,
    val label: String,
    val needsAttention: Boolean,
)

internal object PersonalDeliveryHealthPolicy {
    private const val LATE_AFTER_MILLIS = 5L * 60_000L

    fun status(reminder: PersonalReminder, nowMillis: Long): PersonalDeliveryHealth {
        fun state(
            kind: PersonalDeliveryHealthState,
            label: String,
            needsAttention: Boolean = false,
        ) = PersonalDeliveryHealth(kind, label, needsAttention)

        if (reminder.paused) return state(
            PersonalDeliveryHealthState.PAUSED, "Paused · no upcoming alarm")
        if (reminder.pendingToken.isNotBlank() &&
            reminder.presentedToken == reminder.pendingToken
        ) return state(PersonalDeliveryHealthState.AWAITING_RESPONSE,
            "Notification posted · awaiting your response", true)

        if (reminder.lastMissedOccurrenceMillis > reminder.lastAcknowledgedOccurrenceMillis) {
            return state(PersonalDeliveryHealthState.EARLIER_MISSED,
                "Earlier reminder unanswered · review it", true)
        }

        val hasLease = reminder.pendingToken.isNotBlank() &&
            reminder.pendingOccurrenceMillis > 0L && reminder.pendingFireAtMillis > 0L
        if (hasLease &&
            reminder.snoozedUntilMillis == reminder.pendingFireAtMillis &&
            reminder.lastResponse in setOf(PersonalResponse.SNOOZED, PersonalResponse.RESCHEDULED) &&
            reminder.pendingFireAtMillis > nowMillis
        ) return state(PersonalDeliveryHealthState.USER_DEFERRED,
            "Deferred by you · awaiting next alert")

        // An unposted lease in the past proves only that the schedule is due;
        // Android may defer an inexact wakeup. Never say delivered or failed.
        if (hasLease && reminder.pendingFireAtMillis <= nowMillis) {
            return if (nowMillis - reminder.pendingFireAtMillis >= LATE_AFTER_MILLIS) {
                state(PersonalDeliveryHealthState.LATE_UNCONFIRMED,
                    "Past due · delivery not confirmed", true)
            } else {
                state(PersonalDeliveryHealthState.DUE_UNCONFIRMED,
                    "Due now · delivery not yet confirmed", true)
            }
        }

        return if (PersonalAgendaPolicy.dueAtMillis(reminder, nowMillis) != null) {
            state(PersonalDeliveryHealthState.UPCOMING, "Next reminder planned")
        } else state(PersonalDeliveryHealthState.NO_UPCOMING, "No upcoming alert")
    }
}
