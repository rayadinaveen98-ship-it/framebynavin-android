package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitCompletionMode
import com.framebynavin.app.personal.Phase1HabitDayStatus
import com.framebynavin.app.personal.Phase1HabitStore
import com.framebynavin.app.personal.Phase1HabitAlarmScheduler
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owner-data-backed REVIEW integration for the existing one-activity challenge.
 *
 * Reuses precisely the same Compose editor/dashboard as the screenshot-approved
 * debug fixtures, but persists confirmations to Phase1HabitStore (isolated from
 * legacy V297 plans, Personal reminders, Spaces and Creator).
 *
 * Not linked from production navigation while encrypted ZIP/cloud data coverage
 * (#327), alarm dispatch and owner screenshot acceptance remain open. It MUST
 * NOT claim that voice/alarm/quiet notifications work yet.
 */
@Composable
internal fun Phase1HabitLocalStoreReview(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { Phase1HabitStore(context) }
    val coroutineScope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<Phase1HabitChallenge>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var fatalReadError by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    var chosenId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    val zone = remember { ZoneId.systemDefault() }
    var startDate by remember { mutableStateOf(LocalDate.now(zone)) }
    // Alert delivery is not connected: default Off, refuse an unmet promise.
    var alertKind by remember { mutableStateOf<Phase1HabitAlertKind?>(null) }
    var alertTime by remember { mutableStateOf(LocalTime.of(7, 30)) }
    var completionMode by remember { mutableStateOf(Phase1HabitCompletionMode.STRICT) }
    var detailDay by remember { mutableStateOf<LocalDate?>(null) }
    var detailNote by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(store) {
        try {
            store.challengesFlow.collect { incoming ->
                records = incoming
                loaded = true
            }
        } catch (_: Exception) {
            fatalReadError = true
            loaded = true
            message = "Saved challenge data could not be read. No records were changed."
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = Instant.now()
            delay(30_000L)
        }
    }
    val selected = records.firstOrNull { it.id == chosenId } ?: records.lastOrNull()

    Surface(modifier = Modifier.fillMaxSize(), color = BacklotBackground) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("BACKLOT / PERSONAL", color = BacklotPrimaryText,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close", color = BacklotAccent) }
            }
            if (fatalReadError) {
                Text(message.orEmpty(), modifier = Modifier.padding(18.dp),
                    color = BacklotError)
            } else if (!loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BacklotAccent)
                }
            } else {
                if (!creating && selected != null && records.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        records.forEach { item ->
                            OutlinedButton(
                                onClick = { chosenId = item.id; message = null },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, if (selected.id == item.id) BacklotAccent else BacklotBorder),
                            ) { Text(item.activity, color = BacklotPrimaryText, maxLines = 1) }
                        }
                    }
                }
                if (creating || selected == null) {
                    CreateHabitReview(
                        title = title,
                        onTitle = { title = it.take(200); validationError = false; message = null },
                        date = startDate,
                        onDate = { startDate = it },
                        mode = alertKind,
                        onMode = { alertKind = it; message = null },
                        time = alertTime,
                        onTime = { alertTime = it },
                        error = validationError,
                        completionMode = completionMode,
                        onCompletionMode = { completionMode = it },
                        onStart = {
                            val clean = title.trim()
                            when {
                                saving -> Unit
                                clean.isBlank() -> validationError = true
                                else -> {
                                    val new = Phase1HabitChallenge(
                                        id = UUID.randomUUID().toString(),
                                        activity = clean,
                                        startDate = startDate,
                                        zoneId = zone.id,
                                        alertTime = if (alertKind == null) null else alertTime,
                                        alertKind = alertKind,
                                        completionMode = completionMode,
                                    ).checked()
                                    saving = true
                                    coroutineScope.launch {
                                        try {
                                            records = store.create(new)
                                            chosenId = new.id
                                            creating = false
                                            title = ""
                                            message = runCatching {
                                                Phase1HabitAlarmScheduler(context).reconcile()
                                            }.exceptionOrNull()?.let {
                                                "Challenge saved; check Android alert permissions."
                                            }
                                        } catch (_: Exception) {
                                            message = "Could not save the challenge. Existing progress was not changed."
                                        } finally {
                                            saving = false
                                        }
                                    }
                                }
                            }
                        },
                        previewOnly = false,
                        liveErrorMessage = message,
                    )
                } else {
                    if (message != null) {
                        Text(message.orEmpty(), color = BacklotError,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
                    }
                    DashboardHabitReview(
                        habit = selected,
                        now = now,
                        onDaySelected = { day ->
                            detailDay = day
                            detailNote = selected.dayNotes[day].orEmpty()
                        },
                        onDone = {
                            if (!saving) {
                                saving = true
                                coroutineScope.launch {
                                    try {
                                        records = store.confirmToday(selected.id, Instant.now())
                                        message = runCatching {
                                            Phase1HabitAlarmScheduler(context).reconcile()
                                        }.exceptionOrNull()?.let {
                                            "Progress saved; Android alert scheduling needs attention."
                                        }
                                    } catch (_: Exception) {
                                        message = "Could not confirm today. No Done receipt was added."
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        },
                        onUndo = {
                            if (!saving) {
                                saving = true
                                coroutineScope.launch {
                                    try {
                                        val day = LocalDate.ofInstant(Instant.now(), ZoneId.of(selected.zoneId))
                                        records = store.undoDay(selected.id, day)
                                        message = runCatching {
                                            Phase1HabitAlarmScheduler(context).reconcile()
                                        }.exceptionOrNull()?.let {
                                            "Progress saved; Android alert scheduling needs attention."
                                        }
                                    } catch (_: Exception) {
                                        message = "Could not undo today. Existing progress was kept."
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        },
                        onPause = {
                            if (!saving) {
                                saving = true
                                coroutineScope.launch {
                                    try {
                                        records = store.setPaused(selected.id, !selected.paused)
                                        message = runCatching {
                                            Phase1HabitAlarmScheduler(context).reconcile()
                                        }.exceptionOrNull()?.let {
                                            "Progress saved; Android alert scheduling needs attention."
                                        }
                                    } catch (_: Exception) {
                                        message = "Could not update pause state. No records were changed."
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        },
                        previewOnly = false,
                    )
                }
            }
        }
    }
}
