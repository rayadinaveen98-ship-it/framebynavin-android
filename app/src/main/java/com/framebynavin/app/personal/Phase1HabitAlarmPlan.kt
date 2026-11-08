package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure, non-Android plan for exactly ONE upcoming 21-day challenge alarm.
 *
 * Does NOT schedule, cancel or dispatch anything. In particular, Android's
 * PendingIntent identity/permissions, exact-alarm fallback, occurrence ledger,
 * and crash-safe commit protocol must be added before a live adapter uses it.
 */
internal data class Phase1HabitAlarmTarget(
    val scheduleKey: String,
    val day: LocalDate,
    val fireAt: Instant,
    val kind: Phase1HabitAlertKind,
)

internal enum class Phase1HabitLeaseStage {
    ARMED,
    PRESENTED,
}

/** A future adapter must persist this lease BEFORE scheduling the alarm. */
internal data class Phase1HabitAlarmLease(
    val scheduleKey: String,
    val day: LocalDate,
    val fireAt: Instant,
    val kind: Phase1HabitAlertKind,
    val token: String,
    val stage: Phase1HabitLeaseStage = Phase1HabitLeaseStage.ARMED,
) {
    fun checkedFor(challenge: Phase1HabitChallenge): Phase1HabitAlarmLease {
        require(scheduleKey == challenge.scheduleKey) {
            "Alarm lease belongs to another challenge; refuse cross-owner action"
        }
        require(!day.isBefore(challenge.startDate) && !day.isAfter(challenge.endDate)) {
            "Alarm occurrence must be within the fixed 21-day window"
        }
        require(ZoneId.of(challenge.zoneId).let {
            LocalDate.ofInstant(fireAt, it) == day
        }) { "Alarm occurrence day does not match the challenge time zone" }
        require(token.matches(Regex("[A-Za-z0-9_-]{8,128}"))) {
            "Invalid lease token"
        }
        return this
    }

    fun sameOccurrence(target: Phase1HabitAlarmTarget): Boolean =
        scheduleKey == target.scheduleKey && day == target.day &&
            fireAt == target.fireAt && kind == target.kind
}

/**
 * No response here is a command to Android. A future coordinator must
 * compare-and-swap the persisted lease and prove the alarm operation worked.
 */
internal sealed interface Phase1HabitAlarmPlan {
    data object Idle : Phase1HabitAlarmPlan
    data class Keep(val lease: Phase1HabitAlarmLease) : Phase1HabitAlarmPlan
    data class Install(
        val target: Phase1HabitAlarmTarget,
        val replacing: Phase1HabitAlarmLease?,
    ) : Phase1HabitAlarmPlan
    data class Cancel(val lease: Phase1HabitAlarmLease) : Phase1HabitAlarmPlan
    data class HoldUnresolved(val lease: Phase1HabitAlarmLease) : Phase1HabitAlarmPlan
}

internal object Phase1HabitAlarmPlanner {
    /**
     * Holds an overdue or already-presented occurrence for explicit delivery
     * recovery. It must NEVER silently discard an unanswered notification or
     * turn it into a Done receipt. Re-running with the same state is idempotent.
     */
    fun plan(
        challenge: Phase1HabitChallenge,
        existing: Phase1HabitAlarmLease?,
        now: Instant,
    ): Phase1HabitAlarmPlan {
        challenge.checked()
        existing?.checkedFor(challenge)

        // Prevent replacing a delivered notification before its separate
        // acknowledgment / expiration journal can be checked.
        if (existing != null &&
            (existing.stage == Phase1HabitLeaseStage.PRESENTED ||
                !existing.fireAt.isAfter(now))
        ) return Phase1HabitAlarmPlan.HoldUnresolved(existing)

        val due = challenge.nextAlertAfter(now)
        if (due == null) {
            return if (existing == null) Phase1HabitAlarmPlan.Idle
                else Phase1HabitAlarmPlan.Cancel(existing)
        }

        val target = Phase1HabitAlarmTarget(
            scheduleKey = challenge.scheduleKey,
            day = due.toLocalDate(),
            fireAt = due.toInstant(),
            kind = requireNotNull(challenge.alertKind),
        )
        return if (existing?.sameOccurrence(target) == true) {
            Phase1HabitAlarmPlan.Keep(existing)
        } else {
            // No token minted here: the future durable coordinator must issue
            // a fresh token only AFTER an atomic owner-data lease update.
            Phase1HabitAlarmPlan.Install(target, existing)
        }
    }
}
