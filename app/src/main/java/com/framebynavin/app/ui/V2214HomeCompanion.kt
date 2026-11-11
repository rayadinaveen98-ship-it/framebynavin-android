package com.framebynavin.app.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.framebynavin.app.data.*
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.reminders.ReminderAcknowledgementStore
import com.framebynavin.app.reminders.ReminderOccurrenceStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

/**
 * V222.1: the same V221.4 evidence/dismissal/action policy, now hosted inside
 * a 48dp Home-header portrait and a tap-to-open sheet instead of a tall inline
 * card below the creator's actual work. No new evidence, inferred delivery,
 * background service, notification or reminder mutations.
 */
internal object V2214HomeCompanionPolicy {
    fun unorganizedCount(ideas: List<CreatorIdea>): Int = ideas.count {
        !it.isTrashed && it.status == IdeaStatus.INBOX && it.projectTaskId.isBlank()
    }

    /** Never route a stale suggestion to a deleted/finished/archived project. */
    fun openableProjectId(
        decision: V221CompanionDecision,
        tasks: List<CreatorTask>,
    ): String? {
        if (decision.destination != V221CompanionDestination.PROJECT) return null
        return decision.taskId?.takeIf { id ->
            id.isNotBlank() && tasks.any { task ->
                task.id == id && task.archivedAtMillis <= 0L &&
                    (task.status == TaskStatus.PLANNED || task.status == TaskStatus.WORKING)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2214HomeCompanion(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onOpenReminders: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenIdeas: () -> Unit,
    onOpenActivity: () -> Unit = onOpenReminders,
    onOpenPersonal: () -> Unit = onOpenReminders,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val store = remember(context) { V221CompanionDismissalStore(context.applicationContext) }
    val evidence = remember(context) { V221CompanionEvidenceReader(context.applicationContext) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    // Not now must close the card for this Home visit, even if another lower
    // priority candidate exists. Persisted dismissal guards future visits.
    var quietThisVisit by remember { mutableStateOf(false) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var talkingToSpark by rememberSaveable { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = (context as? ComponentActivity)?.lifecycle

    // Recheck when returning from a reminder, another app, or a deep link.
    // No polling or background process when Home is not on screen.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nowMillis = System.currentTimeMillis()
                refreshVersion += 1
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    // Reminder acknowledgement can change without any change to the task list.
    // Refresh only while this Home component is composed, never in the background.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
            refreshVersion += 1
        }
    }

    // Materialize snapshots from actual observable VM data so Home refreshes
    // after saves, changes, deletions, restores and idea status transitions.
    val taskSnapshot = tasks.toList()
    val ideaSnapshot = ideas.toList()
    val decision = remember(taskSnapshot, ideaSnapshot, nowMillis, refreshVersion, enabled) {
        runCatching {
            val contextSnapshot = evidence.snapshot(
                tasks = taskSnapshot,
                nowMillis = nowMillis,
                unorganizedIdeaCount = V2214HomeCompanionPolicy.unorganizedCount(ideaSnapshot),
            )
            V221CompanionBehaviorPolicy.select(
                context = contextSnapshot,
                enabled = enabled && store.enabled(),
                dismissals = store.records(),
            )
        }.getOrNull() // Fail closed when evidence/preferences cannot be read.
    }

    // Spark can be hidden without hiding the independent Notification Center.
    // Disabling companion suggestions must not suppress the ability to open
    // recorded reminder activity or inspect delivery problems.
    val sparkVisible = enabled && store.enabled()
    LaunchedEffect(sparkVisible) {
        if (!sparkVisible) {
            talkingToSpark = false
            sheetOpen = false
        }
    }

    // V308: share the canonical V306 evidence policy; read-only. A companion
    // preview must never create a new reminder or infer a notification was read.
    val creatorJournal = remember(context) {
        ReminderAcknowledgementStore(context.applicationContext)
    }
    val creatorTokens = remember(context) {
        ReminderOccurrenceStore(context.applicationContext)
    }
    val personalStore = remember(context) {
        PersonalReminderStore(context.applicationContext)
    }
    var personalRecords by remember { mutableStateOf(emptyList<PersonalReminder>()) }
    var personalReceipts by remember { mutableStateOf(emptyList<PersonalCompletionEvent>()) }
    var personalReady by remember { mutableStateOf(false) }
    var receiptReady by remember { mutableStateOf(false) }
    LaunchedEffect(personalStore) {
        try {
            personalStore.remindersFlow.collect {
                personalRecords = it
                personalReady = true
            }
        } catch (_: Exception) { personalReady = false }
    }
    LaunchedEffect(personalStore) {
        try {
            personalStore.completionHistoryFlow.collect {
                personalReceipts = it
                receiptReady = true
            }
        } catch (_: Exception) { receiptReady = false }
    }
    val activityFeed = remember(
        taskSnapshot, personalRecords, personalReceipts, personalReady,
        receiptReady, refreshVersion, nowMillis,
    ) {
        runCatching {
            val creator = taskSnapshot.map { task ->
                val history = creatorJournal.recent(task.id)
                V306CreatorEvidence(
                    task = task,
                    history = history,
                    latestTokenCurrent = history.lastOrNull()?.let {
                        creatorTokens.matches(task, it.occurrenceId)
                    } ?: false,
                )
            }
            V306ActivityCenterPolicy.build(
                creators = creator,
                personal = if (personalReady) personalRecords else emptyList(),
                confirmedDone = if (personalReady && receiptReady) personalReceipts else emptyList(),
                nowMillis = nowMillis,
            )
        }.getOrDefault(emptyList())
    }
    val dismissedActivityIds = remember(nowMillis, refreshVersion) {
        store.suppressed(nowMillis)
    }
    val sparkTop = V308SparkActivityPolicy.top(activityFeed, dismissedActivityIds)

    val identity = V144GuidePrefs.currentGuide
    val actionCount = activityFeed.count {
        it.bucket == V306ActivityBucket.NEEDS_ACTION
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        V335HomeNotificationButton(
            needsAction = actionCount > 0,
            onClick = onOpenActivity,
        )
        if (sparkVisible) {
            V2221CompanionPortrait(
                guide = identity,
                // Spark is a separate, optional surface. Only a genuine
                // suggestion affects its gold indicator. No unread semantics.
                hasActionableActivity = false,
                hasSuggestion = decision != null && !quietThisVisit,
                onClick = { talkingToSpark = false; sheetOpen = true },
            )
        }
    }

    if (sparkVisible && sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { talkingToSpark = false; sheetOpen = false },
            containerColor = CinemaSurfaceRaised,
            modifier = Modifier.testTag("v2221-companion-sheet"),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (talkingToSpark) {
                    V2292SparkConversationPanel(
                        tasks = taskSnapshot,
                        ideas = ideaSnapshot,
                        onBack = { talkingToSpark = false },
                        onOpenProject = { id ->
                            // Never route a saved chat reply to a deleted or
                            // completed project after the project list changed.
                            if (tasks.any {
                                it.id == id && it.archivedAtMillis <= 0L &&
                                    (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING)
                            }) {
                                talkingToSpark = false
                                sheetOpen = false
                                onOpenProject(id)
                            }
                        },
                        onOpenIdeas = {
                            talkingToSpark = false
                            sheetOpen = false
                            onOpenIdeas()
                        },
                        onOpenReminders = {
                            talkingToSpark = false
                            sheetOpen = false
                            onOpenReminders()
                        },
                    )
                } else {
                // Suggestion card already shows Spark and the next action; no duplicate header.
                // A suppressed/unavailable decision must never be replaced by
                // fabricated project or reminder evidence.
                val visibleDecision = decision?.takeUnless { quietThisVisit }
                if (sparkTop.isNotEmpty()) {
                    V308SparkActivityDigest(
                        feed = activityFeed,
                        dismissed = dismissedActivityIds,
                        onDismiss = { item ->
                            // Spark-only presentation preference: never acknowledges,
                            // completes, reschedules or hides the original activity.
                            if (store.dismiss(
                                    V308SparkActivityPolicy.dismissalId(item),
                                    System.currentTimeMillis(),
                                )) {
                                nowMillis = System.currentTimeMillis()
                                refreshVersion++
                            }
                        },
                        onOpenActivity = {
                            sheetOpen = false
                            onOpenActivity()
                        },
                        onOpenCreator = { id ->
                            if (tasks.any {
                                    it.id == id && it.archivedAtMillis <= 0L &&
                                    (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING)
                                }) {
                                sheetOpen = false
                                onOpenProject(id)
                            } else {
                                sheetOpen = false
                                onOpenActivity()
                            }
                        },
                        onOpenPersonal = {
                            sheetOpen = false
                            onOpenPersonal()
                        },
                    )
                } else if (visibleDecision != null) {
                    V221CompanionSuggestionCard(
                        decision = visibleDecision,
                        identity = identity,
                        onAction = { current ->
                            sheetOpen = false
                            when (current.destination) {
                                V221CompanionDestination.REMINDER_HUB -> onOpenReminders()
                                V221CompanionDestination.IDEA_VAULT -> onOpenIdeas()
                                V221CompanionDestination.PROJECT ->
                                    V2214HomeCompanionPolicy.openableProjectId(
                                        current, tasks.toList(),
                                    )?.let(onOpenProject)
                                V221CompanionDestination.NONE -> Unit
                            }
                        },
                        onDismiss = { current ->
                            sheetOpen = false
                            quietThisVisit = true
                            if (store.dismiss(current.id, System.currentTimeMillis())) {
                                nowMillis = System.currentTimeMillis()
                                refreshVersion += 1
                            }
                        },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .testTag("v2221-companion-empty")
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        V144GuideCharacter(
                            guide = identity,
                            state = CinePulseState.REST,
                            modifier = Modifier.size(78.dp),
                        )
                        Text("Nothing needs your attention right now.",
                            color = MutedText, modifier = Modifier.weight(1f))
                    }
                }
                TextButton(
                    onClick = { talkingToSpark = true },
                    modifier = Modifier.testTag("v2292-spark-open-chat")
                        .fillMaxWidth(),
                ) {
                    Text("ASK SPARK · OFFLINE", color = MutedGold, fontSize = 12.sp)
                }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** Dedicated outlined Spark companion icon; no longer doubles as the inbox.
 * The icon uses Backlot's shared outlined Material family and 48dp target.
 * The existing Spark bottom sheet, suggestions, privacy and chat stay intact.
 */
@Composable
internal fun V2221CompanionPortrait(
    guide: V144GuideIdentity,
    hasSuggestion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasActionableActivity: Boolean = false,
) {
    val indicator = when {
        hasActionableActivity -> BacklotAccent
        hasSuggestion -> MutedGold
        else -> null
    }
    val state = when {
        hasActionableActivity -> "A reminder needs attention"
        hasSuggestion -> "A Spark suggestion is available"
        else -> "No current attention indicator"
    }
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp)
            .testTag("v2221-companion-avatar")
            .semantics {
                // Keep established accessibility navigation intact; the badge's
                // meaning is conveyed separately without claiming anything was read.
                contentDescription = "Open ${guide.displayName} companion"
                stateDescription = state
            },
        shape = CircleShape,
        color = CinemaSurfaceRaised,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = if (hasActionableActivity) BacklotAccent else ProjectorIvory,
                modifier = Modifier.size(21.dp),
            )
            if (indicator != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 9.dp, end = 9.dp)
                        .size(7.dp)
                        .background(indicator, CircleShape)
                        .testTag(
                            if (hasActionableActivity) "v334-actionable-activity-indicator"
                            else "v334-spark-suggestion-indicator"
                        ),
                )
            }
        }
    }
}


/** Always-available recorded reminder activity, independent of Spark preference.
 * A red dot means an outstanding ACTION (not an unread notification).
 * No status is inferred from posting a notification, and tapping never
 * acknowledges or modifies any reminder.
 */
@Composable
internal fun V335HomeNotificationButton(
    needsAction: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp)
            .testTag("v335-home-notification-entry")
            .semantics {
                contentDescription = "Open Notification Center"
                stateDescription = if (needsAction)
                    "One or more reminders need action"
                else "No recorded action requiring a response"
            },
        shape = CircleShape,
        color = CinemaSurfaceRaised,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (needsAction)
                    Icons.Outlined.NotificationsActive
                else Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = if (needsAction) BacklotAccent else ProjectorIvory,
                modifier = Modifier.size(21.dp),
            )
            if (needsAction) {
                Box(
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = 9.dp, end = 9.dp)
                        .size(7.dp)
                        .background(BacklotAccent, CircleShape)
                        .testTag("v335-action-required-dot"),
                )
            }
        }
    }
}
