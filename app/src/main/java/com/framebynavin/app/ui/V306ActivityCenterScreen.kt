package com.framebynavin.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.reminders.ReminderAcknowledgementStore
import com.framebynavin.app.reminders.ReminderOccurrenceStore
import com.framebynavin.app.ui.theme.*
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.collect

/**
 * V306 owner-opened, local-first read-only Activity Center.
 * It observes the existing journal and Personal DataStore; it never acknowledges,
 * snoozes, completes, schedules, or changes any reminder on its own.
 */
@Composable
internal fun V306ActivityCenterScreen(
    tasks: List<CreatorTask>,
    nowMillis: Long,
    onClose: () -> Unit,
    onOpenCreator: (String) -> Unit,
    onOpenPersonal: () -> Unit,
    onOpenPersonalItem: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current.applicationContext
    val creatorJournal = remember(context) { ReminderAcknowledgementStore(context) }
    val creatorTokens = remember(context) { ReminderOccurrenceStore(context) }
    val personalStore = remember(context) { PersonalReminderStore(context) }

    var personal by remember { mutableStateOf(emptyList<PersonalReminder>()) }
    var confirmedDone by remember { mutableStateOf(emptyList<PersonalCompletionEvent>()) }
    var personalReady by remember { mutableStateOf(false) }
    var receiptsReady by remember { mutableStateOf(false) }
    var personalFailure by remember { mutableStateOf(false) }
    var receiptFailure by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedName by rememberSaveable { mutableStateOf(V306ActivityFilter.NEEDS_ACTION.name) }

    LaunchedEffect(personalStore) {
        try {
            personalStore.remindersFlow.collect { records ->
                personal = records
                personalReady = true
            }
        } catch (_: Exception) {
            personalFailure = true
        }
    }
    LaunchedEffect(personalStore) {
        try {
            personalStore.completionHistoryFlow.collect { records ->
                confirmedDone = records
                receiptsReady = true
            }
        } catch (_: Exception) {
            receiptFailure = true
        }
    }

    // Refetch the Creator acknowledgement journal on entry, every existing hub
    // refresh tick, and explicit refresh. No stored data is written by this read.
    val creatorEvidence = remember(tasks, nowMillis, refresh) {
        runCatching {
            tasks.map { task ->
                val history = creatorJournal.recent(task.id)
                val latest = history.lastOrNull()
                V306CreatorEvidence(
                    task = task,
                    history = history,
                    latestTokenCurrent = latest != null &&
                        creatorTokens.matches(task, latest.occurrenceId),
                )
            }
        }.getOrNull()
    }
    val feed = remember(creatorEvidence, personal, confirmedDone, personalReady, receiptsReady) {
        V306ActivityCenterPolicy.build(
            creators = creatorEvidence.orEmpty(),
            personal = if (personalReady) personal else emptyList(),
            confirmedDone = if (receiptsReady && personalReady) confirmedDone else emptyList(),
            nowMillis = nowMillis,
        )
    }
    val filter = V306ActivityFilter.entries.firstOrNull { it.name == selectedName }
        ?: V306ActivityFilter.NEEDS_ACTION
    val visible = V306ActivityCenterPolicy.filter(feed, filter)
    val actionCount = feed.count { it.bucket == V306ActivityBucket.NEEDS_ACTION }
    val historyCount = feed.count { it.bucket == V306ActivityBucket.HISTORY }
    val activeDeliveryProblems = feed.count {
        it.deliveryProblem && it.bucket == V306ActivityBucket.NEEDS_ACTION
    }
    // Source-only Spark guidance: never infer that an unavailable store is empty.
    val evidenceComplete = creatorEvidence != null && personalReady && receiptsReady &&
        !personalFailure && !receiptFailure
    val sparkNext = if (evidenceComplete)
        V308SparkActivityPolicy.top(feed)
            .firstOrNull { it.bucket == V306ActivityBucket.NEEDS_ACTION }
    else null
    BackHandler(onBack = onClose)

    CreatorAdaptiveFullScreen(onDismissRequest = onClose) {
        Column(modifier = Modifier.fillMaxSize().testTag("v306-activity-center")) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, "Back to reminders", tint = ProjectorIvory)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("BACKLOT / REMINDERS", color = BacklotAccent,
                        fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Notification Center", color = BacklotPrimaryText,
                        fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = { refresh++ },
                    modifier = Modifier.testTag("v306-activity-refresh")) {
                    Icon(Icons.Outlined.Refresh, "Refresh activity", tint = ProjectorIvory)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .testTag("v306-activity-filters"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                V306ActivityFilter.entries.forEach { option ->
                    val label = when (option) {
                        V306ActivityFilter.NEEDS_ACTION ->
                            if (evidenceComplete) "Needs action $actionCount" else "Needs action"
                        V306ActivityFilter.ALL -> "All"
                        V306ActivityFilter.HISTORY ->
                            if (evidenceComplete) "History $historyCount" else "History"
                    }
                    FilterChip(
                        selected = filter == option,
                        onClick = { selectedName = option.name },
                        label = { Text(label) },
                        modifier = Modifier.testTag("v306-filter-${option.name.lowercase()}"),
                    )
                }
            }
            if (activeDeliveryProblems > 0) {
                Surface(
                    onClick = { selectedName = V306ActivityFilter.NEEDS_ACTION.name },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("v335-delivery-problem-summary"),
                    shape = RoundedCornerShape(14.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, null,
                            tint = RecRed, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$activeDeliveryProblems CREATOR DELIVERY " +
                                if (activeDeliveryProblems == 1) "ISSUE" else "ISSUES",
                                color = ProjectorIvory,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Android did not confirm delivery. Review the original " +
                                "reminder and its permission settings.",
                                color = MutedText, fontSize = 11.sp)
                        }
                        Icon(Icons.Outlined.ChevronRight,
                            contentDescription = "Review failed reminder deliveries",
                            tint = RecRed)
                    }
                }
            }
            if (!personalReady && !personalFailure || !receiptsReady && !receiptFailure) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            if (creatorEvidence == null || personalFailure || receiptFailure) {
                Text(
                    "Some local activity couldn't be read. No missing records were counted as zero; your data wasn't changed.",
                    color = RecRed, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        .testTag("v306-activity-read-error"),
                )
            }
            // Spark links to the original, verified actionable reminder.
            // This does not acknowledge, schedule, dismiss or post an alert.
            if (filter == V306ActivityFilter.NEEDS_ACTION && sparkNext != null) {
                Surface(
                    onClick = {
                        if (sparkNext.domain == V306ActivityDomain.CREATOR)
                            onOpenCreator(sparkNext.targetId)
                        else if (onOpenPersonalItem != null) onOpenPersonalItem(sparkNext.targetId)
                        else onOpenPersonal()
                    },
                    color = BacklotSurfaceSelected,
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, BacklotAccent.copy(alpha = .55f)),
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("fc13-spark-notification-assist"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsActive,
                            contentDescription = "Spark suggestion",
                            tint = BacklotAccent,
                            modifier = Modifier.size(26.dp).testTag("fc13-spark-activity-next"),
                        )
                        Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("SPARK · NEXT ACTION", color = BacklotAccent,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                V308SparkActivityPolicy.displayedTitle(sparkNext),
                                color = BacklotPrimaryText, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                actionCount.toString() + " recorded item" +
                                    (if (actionCount == 1) "" else "s") +
                                    " need a response. Open the original reminder.",
                                color = BacklotSecondaryText, fontSize = 11.sp,
                                maxLines = 2,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight,
                            contentDescription = "Open suggested reminder",
                            tint = BacklotAccent)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (visible.isEmpty()) {
                    item(key = "empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("v306-activity-empty"),
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Outlined.NotificationsActive, null, tint = RecRed)
                                Text(
                                    if (!personalReady || !receiptsReady || creatorEvidence == null)
                                        "Loading available activity…"
                                    else when (filter) {
                                        V306ActivityFilter.NEEDS_ACTION -> "Nothing currently needs a response."
                                        V306ActivityFilter.ALL -> "No recorded reminder activity yet."
                                        V306ActivityFilter.HISTORY -> "No confirmed activity history yet."
                                    },
                                    color = MutedText, fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
                items(visible, key = { it.id }) { activity ->
                    Surface(
                        onClick = {
                            if (activity.domain == V306ActivityDomain.CREATOR)
                                onOpenCreator(activity.targetId)
                            else if (onOpenPersonalItem != null) onOpenPersonalItem(activity.targetId)
                            else onOpenPersonal()
                        },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("v306-item-${activity.domain.name.lowercase()}"),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp,
                            if (activity.deliveryProblem) RecRed.copy(alpha = .5f) else CinemaLine),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    (if (activity.domain == V306ActivityDomain.CREATOR)
                                        "CREATOR" else "PERSONAL") + "  ·  " +
                                        (if (activity.deliveryProblem) "DELIVERY ISSUE"
                                         else if (activity.bucket == V306ActivityBucket.NEEDS_ACTION)
                                             "NEEDS ACTION" else "HISTORY"),
                                    color = BacklotSecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                )
                                Text(activity.title, color = ProjectorIvory, fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(activity.detail, color = MutedText, fontSize = 12.sp,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                                if (activity.atMillis > 0L) {
                                    Text(
                                        DateFormat.getDateTimeInstance(
                                            DateFormat.MEDIUM, DateFormat.SHORT
                                        ).format(Date(activity.atMillis)),
                                        color = MutedText, fontSize = 10.sp,
                                    )
                                }
                            }
                            Icon(Icons.Outlined.ChevronRight, "Open original reminder", tint = RecRed)
                        }
                    }
                }
                item(key = "disclosure") {
                    Text(
                        "Only recorded delivery attempts and explicit responses are shown. " +
                            "Posted notifications are not proof that you read or heard them.",
                        color = MutedText, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
