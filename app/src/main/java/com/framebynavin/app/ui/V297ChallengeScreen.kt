package com.framebynavin.app.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.V297Challenge
import com.framebynavin.app.personal.V297ChallengeStore
import com.framebynavin.app.personal.V297ChallengeMergePolicy
import com.framebynavin.app.ui.theme.*
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owner-authored plans. No pre-installed goals, reminder scheduling, cloud
 * writes, notification permissions or inferred Done activity.
 */
@Composable
internal fun V297ChallengeScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { V297ChallengeStore(context) }
    val scope = rememberCoroutineScope()
    var plans by remember { mutableStateOf<List<V297Challenge>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var dayIndex by rememberSaveable { mutableIntStateOf(0) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var newStart by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var confirmArchive by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<V297Challenge>?>(null) }
    val selected = plans.firstOrNull { it.id == selectedId }
    var dayActivity by rememberSaveable(selectedId, dayIndex) { mutableStateOf("") }
    LaunchedEffect(selected?.id, dayIndex, selected?.days?.getOrNull(dayIndex)?.activity) {
        dayActivity = selected?.days?.getOrNull(dayIndex)?.activity.orEmpty()
    }
    LaunchedEffect(store) {
        try { store.challengesFlow.collect { plans = it; loaded = true } }
        catch (_: Exception) {
            error = "Could not read your saved challenges. No records were changed."
            loaded = true
        }
    }
    fun write(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { action(); error = null }
            catch (e: Exception) { error = e.message ?: "Save failed. Nothing was changed." }
            finally { busy = false }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) write {
            val json = store.exportJson()
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open the selected file.")
            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !busy) {
            scope.launch {
                try {
                    val raw = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("Could not read chosen file")
                        stream.use { input ->
                            val buffer = ByteArray(8192)
                            val output = ByteArrayOutputStream()
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 1_000_000) {
                                    "Challenge backup is too large"
                                }
                                output.write(buffer, 0, count)
                            }
                            output.toString(Charsets.UTF_8.name())
                        }
                    }
                    pendingImport = store.validateImport(raw)
                    error = null
                } catch (e: Exception) {
                    pendingImport = null
                    error = e.message ?: "Import could not be validated. Nothing was changed."
                }
            }
        }
    }
    val goBack: () -> Unit = {
        when {
            pendingImport != null -> pendingImport = null
            confirmArchive -> confirmArchive = false
            createOpen -> createOpen = false
            selectedId != null -> selectedId = null
            else -> onClose()
        }
    }
    BackHandler(onBack = goBack)

    Surface(
        modifier = Modifier.fillMaxSize().testTag("v297-challenges-screen"),
        color = CinemaSurfaceRaised,
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = goBack, modifier = Modifier.testTag("v297-back")) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory)
                }
                Column(Modifier.weight(1f)) {
                    Text("MY PLANS", color = MutedGold, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
                    Text(if (selected == null) "21-day challenges" else selected.title,
                        color = ProjectorIvory, fontSize = 19.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            error?.let { Text(it, color = RecRed,
                modifier = Modifier.testTag("v297-error")) }
            if (!loaded) {
                CircularProgressIndicator()
            } else if (selected == null) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("A fresh start, defined by you. Choose your dates and write your own daily activities.",
                        color = MutedText, fontSize = 13.sp)
                    if (!createOpen) {
                        Button(onClick = { newTitle = ""; newStart = LocalDate.now().toString(); createOpen = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().testTag("v297-create-open")) {
                            Text("CREATE 21-DAY CHALLENGE")
                        }
                    } else {
                        Surface(color = CinemaSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CinemaLine)) {
                            Column(Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Your challenge", color = ProjectorIvory,
                                    fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it.take(100) },
                                    label = { Text("Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("v297-create-title"),
                                )
                                OutlinedButton(onClick = {
                                    val date = LocalDate.parse(newStart)
                                    DatePickerDialog(context, { _, year, month, day ->
                                        newStart = LocalDate.of(year, month + 1, day).toString()
                                    }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                                }, modifier = Modifier.fillMaxWidth().testTag("v297-start-date")) {
                                    Text("Start: " + LocalDate.parse(newStart).format(
                                        DateTimeFormatter.ofPattern("d MMM yyyy")))
                                }
                                Text("All 21 days start blank. You choose the activities; no alerts are created.",
                                    color = MutedText, fontSize = 12.sp)
                                Button(
                                    enabled = !busy && newTitle.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().testTag("v297-create-save"),
                                    onClick = {
                                        val plan = V297Challenge(
                                            title = newTitle.trim(),
                                            startDate = LocalDate.parse(newStart),
                                        ).checked()
                                        write {
                                            store.upsert(plan)
                                            dayIndex = 0
                                            selectedId = plan.id
                                            createOpen = false
                                        }
                                    },
                                ) { Text("CREATE MY PLAN") }
                                TextButton(onClick = { createOpen = false }) { Text("CANCEL") }
                            }
                        }
                    }
                    if (plans.isEmpty()) Text("No challenges yet.",
                        color = MutedText, fontSize = 13.sp,
                        modifier = Modifier.testTag("v297-empty"))
                    plans.sortedByDescending { it.createdAtMillis }.forEach { plan ->
                        Surface(
                            onClick = {
                                selectedId = plan.id
                                dayIndex = ChronoUnit.DAYS.between(
                                    plan.startDate, LocalDate.now()
                                ).toInt().coerceIn(0, 20)
                            },
                            color = CinemaSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CinemaLine),
                            modifier = Modifier.fillMaxWidth().testTag("v297-plan-item"),
                        ) {
                            Row(Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(plan.title, color = ProjectorIvory,
                                        fontWeight = FontWeight.SemiBold)
                                    val complete = plan.days.count { it.completedAtMillis != null }
                                    Text("${complete}/21 completed · " +
                                        plan.startDate.format(DateTimeFormatter.ofPattern("d MMM")) +
                                        if (plan.archivedAtMillis == null) "" else " · Archived",
                                        color = MutedText, fontSize = 12.sp)
                                }
                                Icon(Icons.Outlined.ChevronRight, null, tint = MutedGold)
                            }
                        }
                    }
                    if (plans.isNotEmpty()) {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = { exportLauncher.launch("backlot-personal-21day-challenges.json") },
                            modifier = Modifier.fillMaxWidth().testTag("v297-export"),
                        ) { Text("EXPORT PRIVATE PLAN BACKUP") }
                    }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.fillMaxWidth().testTag("v297-import-open"),
                    ) { Text("IMPORT CHALLENGE BACKUP") }
                    Text("These plans stay on this device and are not included in cloud sync. " +
                        "Export a private backup before changing devices or clearing app data.",
                        color = MutedText, fontSize = 12.sp)
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    val day = selected.days[dayIndex]
                    val archived = selected.archivedAtMillis != null
                    Text("Day ${dayIndex + 1} of 21  ·  " +
                        selected.activityDate(dayIndex).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
                        color = MutedGold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("v297-day-label"))
                    LinearProgressIndicator(
                        progress = { selected.days.count { it.completedAtMillis != null } / 21f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { dayIndex-- }, enabled = dayIndex > 0,
                            modifier = Modifier.testTag("v297-prev-day")) {
                            Icon(Icons.Outlined.ChevronLeft, null)
                            Text("PREVIOUS")
                        }
                        OutlinedButton(onClick = { dayIndex++ }, enabled = dayIndex < 20,
                            modifier = Modifier.testTag("v297-next-day")) {
                            Text("NEXT")
                            Icon(Icons.Outlined.ChevronRight, null)
                        }
                    }
                    OutlinedTextField(
                        value = dayActivity,
                        onValueChange = { dayActivity = it.take(500) },
                        enabled = !archived && !busy,
                        label = { Text("Your activity for this day") },
                        placeholder = { Text("Write whatever matters to you") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("v297-day-activity"),
                    )
                    Button(
                        enabled = !archived && !busy && dayActivity.trim() != day.activity,
                        onClick = {
                            val activity = dayActivity.trim()
                            write { store.update(selected.id) { it.editDay(dayIndex, activity) } }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("v297-save-activity"),
                    ) { Text("SAVE THIS DAY") }
                    if (day.completedAtMillis == null) {
                        OutlinedButton(
                            enabled = !archived && !busy && day.activity.isNotBlank() &&
                                dayActivity.trim() == day.activity,
                            onClick = {
                                write { store.update(selected.id) {
                                    it.markDone(dayIndex, System.currentTimeMillis())
                                } }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("v297-mark-done"),
                        ) { Text("MARK DAY COMPLETE") }
                    } else {
                        Text("Completed by you", color = MutedGold,
                            modifier = Modifier.testTag("v297-confirmed-done"))
                        TextButton(enabled = !archived && !busy, onClick = {
                            write { store.update(selected.id) { it.undoDone(dayIndex) } }
                        }) { Text("UNDO COMPLETION") }
                    }
                    Text("Only your own Done action counts as progress.",
                        color = MutedText, fontSize = 12.sp)
                    HorizontalDivider(color = CinemaLine)
                    OutlinedButton(
                        onClick = { confirmArchive = true },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("v297-archive-open"),
                    ) { Text(if (archived) "RESTORE CHALLENGE" else "ARCHIVE CHALLENGE") }
                    Text("Plan activities do not create reminders automatically. " +
                        "Plans are currently device-only, with a separate manual export.",
                        color = MutedText, fontSize = 12.sp)
                }
            }
        }
    }

    pendingImport?.let { incoming ->
        val existingIds = plans.mapTo(HashSet()) { it.id }
        val newCount = incoming.count { it.id !in existingIds }
        val skipped = incoming.size - newCount
        AlertDialog(
            onDismissRequest = { if (!busy) pendingImport = null },
            title = { Text("Import these challenges?") },
            text = {
                Text("${incoming.size} valid challenges found. " +
                    "${newCount} will be added, ${skipped} with existing IDs skipped. " +
                    "No existing challenge will be overwritten, and no reminders will be scheduled.")
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && newCount > 0,
                    onClick = {
                        write {
                            store.mutate { current ->
                                V297ChallengeMergePolicy.addMissing(current, incoming)
                            }
                            pendingImport = null
                        }
                    },
                    modifier = Modifier.testTag("v297-import-confirm"),
                ) { Text("IMPORT ${newCount}") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }, enabled = !busy) {
                    Text("CANCEL")
                }
            },
        )
    }

    if (confirmArchive && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(if (selected.archivedAtMillis == null) "Archive this challenge?"
                else "Restore this challenge?") },
            text = { Text("Activities and confirmed progress will be retained.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    val id = selected.id
                    val archived = selected.archivedAtMillis != null
                    write {
                        store.update(id) {
                            if (archived) it.restore() else it.archive(System.currentTimeMillis())
                        }
                        confirmArchive = false
                    }
                }) { Text("CONFIRM") }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) { Text("CANCEL") }
            },
        )
    }
}
