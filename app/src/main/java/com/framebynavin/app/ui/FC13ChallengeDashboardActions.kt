package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.framebynavin.app.personal.Phase1HabitAlarmScheduler
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitNotification
import com.framebynavin.app.personal.Phase1HabitStore
import com.framebynavin.app.personal.Phase1HabitVoiceService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * Keep challenge DataStore mutations out of the large Compose parent:
 * saves are owner initiated, durable, and reconciled after commit.
 */
internal enum class FC13ChallengeOperation { DONE, UNDO, SNOOZE, PAUSE }

internal data class FC13ChallengeOperationOutcome(
    val records: List<Phase1HabitChallenge>,
    val deliveryMessage: String?,
)

internal suspend fun FC13ApplyChallengeOperation(
    context: Context,
    store: Phase1HabitStore,
    challenge: Phase1HabitChallenge,
    operation: FC13ChallengeOperation,
): FC13ChallengeOperationOutcome {
    val scheduler = Phase1HabitAlarmScheduler(context)
    val updated: List<Phase1HabitChallenge>
    val message: String
    when (operation) {
        FC13ChallengeOperation.DONE -> {
            updated = store.confirmToday(challenge.id, Instant.now())
            message = "Progress saved; Android alert scheduling needs attention."
        }
        FC13ChallengeOperation.UNDO -> {
            val day = LocalDate.ofInstant(Instant.now(), ZoneId.of(challenge.zoneId))
            updated = store.undoDay(challenge.id, day)
            message = "Progress saved; Android alert scheduling needs attention."
        }
        FC13ChallengeOperation.PAUSE -> {
            updated = store.setPaused(challenge.id, !challenge.paused)
            message = "Progress saved; Android alert scheduling needs attention."
        }
        FC13ChallengeOperation.SNOOZE -> {
            val requestedAt = Instant.now()
            val zone = ZoneId.of(challenge.zoneId)
            val localDay = LocalDate.ofInstant(requestedAt, zone)
            val previousDue = challenge.snoozedUntil
                ?.takeIf { !it.isAfter(requestedAt) }
                ?: localDay.atTime(requireNotNull(challenge.alertTime)).atZone(zone).toInstant()
            updated = store.snoozeToday(challenge.id, requestedAt, 10)
            Phase1HabitVoiceService.stopMatching(challenge.id, previousDue.toEpochMilli())
            val deliveryFailed = try {
                scheduler.reconcile()
                false
            } catch (_: Exception) {
                true
            }
            if (!deliveryFailed) {
                Phase1HabitNotification.cancelPosted(
                    context, challenge, previousDue.toEpochMilli(),
                )
            }
            return FC13ChallengeOperationOutcome(
                updated,
                if (deliveryFailed) "10-minute snooze saved. Android delivery needs attention."
                else null,
            )
        }
    }
    val deliveryFailed = try {
        scheduler.reconcile()
        false
    } catch (_: Exception) {
        true
    }
    return FC13ChallengeOperationOutcome(updated, if (deliveryFailed) message else null)
}

@Composable
internal fun FC13ChallengeDashboardPanel(
    challenge: Phase1HabitChallenge,
    now: Instant,
    linkedSpaceLabel: String?,
    linkedGroupLabel: String?,
    busy: Boolean,
    store: Phase1HabitStore,
    onBusy: (Boolean) -> Unit,
    onRecords: (List<Phase1HabitChallenge>) -> Unit,
    onMessage: (String?) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onManageLinks: () -> Unit,
    onOpenLinkedSpace: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun respond(action: FC13ChallengeOperation) {
        if (busy) return
        if (action == FC13ChallengeOperation.SNOOZE &&
            !challenge.canSnooze(Instant.now())) return
        onBusy(true)
        scope.launch {
            try {
                val outcome = FC13ApplyChallengeOperation(context, store, challenge, action)
                onRecords(outcome.records)
                onMessage(outcome.deliveryMessage)
            } catch (_: Exception) {
                val failure = when (action) {
                    FC13ChallengeOperation.DONE ->
                        "Could not confirm today. No Done receipt was added."
                    FC13ChallengeOperation.UNDO ->
                        "Could not undo today. Existing progress was kept."
                    FC13ChallengeOperation.PAUSE ->
                        "Could not update pause state. No records were changed."
                    FC13ChallengeOperation.SNOOZE ->
                        "Snooze could not be confirmed. Existing Done receipts were retained."
                }
                onMessage(failure)
            } finally {
                onBusy(false)
            }
        }
    }
    DashboardHabitReview(
        habit = challenge,
        now = now,
        onDaySelected = onDaySelected,
        linkedSpaceLabel = linkedSpaceLabel,
        linkedGroupLabel = linkedGroupLabel,
        onManageLinks = onManageLinks,
        onOpenLinkedSpace = challenge.linkedSpaceId?.let { id ->
            { onOpenLinkedSpace(id) }
        },
        onDone = { respond(FC13ChallengeOperation.DONE) },
        onUndo = { respond(FC13ChallengeOperation.UNDO) },
        onSnooze = { respond(FC13ChallengeOperation.SNOOZE) },
        onPause = { respond(FC13ChallengeOperation.PAUSE) },
        previewOnly = false,
    )
}
