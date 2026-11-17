package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.text.DateFormat
import java.util.Date
import java.util.UUID

/**
 * UX05.1D: creator-entered post-publication observations for a single
 * canonical deliverable. No live analytics fetch, revenue calculation,
 * platform connection, publication change or workflow stage advancement.
 */
@Composable
internal fun CW52OutputLearningDesk(
    task: CreatorTask,
    onBack: () -> Unit,
    onOpenPublish: () -> Unit,
    onOpenPlanOutputs: () -> Unit,
    onSave: (Long, CreatorOutputLearning) -> Unit,
    onSelectedOutput: (String) -> Unit = {},
    onDiscardDraft: (String, Long) -> Boolean = { _, _ -> true },
    saving: Boolean = false,
) {
    val context = LocalContext.current
    val journal = remember(task.id) {
        CreatorOutputLearningDraftStore(context.applicationContext)
    }
    val workspace = task.workspace
    val outputs = workspace.deliverables
    val revision = workspace.revision
    var selectedId by rememberSaveable(task.id) {
        mutableStateOf(outputs.firstOrNull()?.id.orEmpty())
    }
    val selected = outputs.firstOrNull { it.id == selectedId } ?: outputs.firstOrNull()
    val activeId = selected?.id.orEmpty()
    LaunchedEffect(task.id, activeId) { onSelectedOutput(activeId) }
    val recovered = remember(task.id, activeId, revision) {
        if (activeId.isBlank()) null else journal.load(task.id, activeId, revision)
    }
    var draftWarning by remember(task.id, activeId, revision) { mutableStateOf(false) }
    var showOutputMenu by remember { mutableStateOf(false) }
    var requestedSwitch by remember { mutableStateOf<String?>(null) }
    var pendingPublishNavigation by remember { mutableStateOf(false) }

    var expectation by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.expectation.orEmpty())
    }
    var observed by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.observedOutcome.orEmpty())
    }
    var worked by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.whatWorked.orEmpty())
    }
    var improve by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.improveNext.orEmpty())
    }
    var nextAction by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.nextAction.orEmpty())
    }
    var metricLabel by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.metricLabel.orEmpty())
    }
    var metricValue by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.metricValue.orEmpty())
    }
    var metricSource by rememberSaveable(task.id, activeId, revision) {
        mutableStateOf(recovered?.metricSource.orEmpty())
    }

    val currentDraft = CreatorOutputLearningDraft(
        expectation = expectation, observedOutcome = observed,
        whatWorked = worked, improveNext = improve, nextAction = nextAction,
        metricLabel = metricLabel, metricValue = metricValue, metricSource = metricSource,
    )
    val hasDraft = currentDraft.hasContent()
    // Journaling uses the current revision and restore epoch; it never
    // writes the canonical learning history. A stale draft stays quarantined.
    LaunchedEffect(task.id, activeId, revision, currentDraft) {
        if (activeId.isBlank()) return@LaunchedEffect
        delay(350)
        val failure = withContext(Dispatchers.IO) {
            runCatching {
                if (currentDraft.hasContent()) {
                    journal.save(task.id, activeId, revision, currentDraft)
                } else if (journal.load(task.id, activeId, revision) != null) {
                    journal.clear(task.id, activeId, revision)
                }
            }.exceptionOrNull()
        }
        if (failure != null) draftWarning = true
    }
    val hasReflection = listOf(expectation, observed, worked, improve, nextAction)
        .any { it.isNotBlank() }
    val metricFieldsConsistent =
        metricLabel.isBlank() == metricValue.isBlank() &&
        (metricSource.isBlank() || metricValue.isNotBlank())
    val allowedToWrite = selected?.let { output ->
        output.status == CreatorDeliverableStatus.PUBLISHED && output.publishedAtMillis > 0L
    } == true && task.archivedAtMillis == 0L && task.status != TaskStatus.SKIPPED

    val availableOutputIds = outputs.map { it.id }.toSet()
    val unavailableOutputHistory = workspace.outputLearnings.filter {
        it.deliverableId !in availableOutputIds
    }.sortedByDescending { it.recordedAtMillis }
    val history = workspace.outputLearnings.filter { it.deliverableId == activeId }
        .sortedWith(
            compareByDescending<CreatorOutputLearning> { it.recordedAtMillis }
                .thenByDescending { it.id }
        )

    fun changeOutput(nextId: String) {
        if (saving || nextId == activeId) return
        if (hasDraft) requestedSwitch = nextId
        else if (discardCurrentDraft()) selectedId = nextId
    }
    fun discardCurrentDraft(): Boolean {
        // One Activity-owned discard path also handles system Back, so every
        // explicit discard follows the same generation/revision safety check.
        val result = runCatching { onDiscardDraft(activeId, revision) }
        if (result.isFailure || result.getOrDefault(false) != true) {
            draftWarning = true
            return false
        }
        return true
    }

    WorkspaceAdaptiveFrame {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, enabled = !saving, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to workspace",
                        tint = ProjectorIvory)
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text("OUTPUT LEARNING", color = MutedGold,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(task.title, color = ProjectorIvory,
                        fontSize = 19.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (draftWarning) {
                Text(
                    "Could not securely retain or clear the unfinished reflection. " +
                        "Keep this editor open and retry before discarding.",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp),
                    color = RecRed, fontSize = 12.sp,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("ux051d-output-learning"),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                item {
                    Text(
                        "Reflect on real published work. These notes are manual observations, " +
                            "not verified social-platform analytics.",
                        color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                }
                if (outputs.isEmpty()) {
                    item {
                        Text("This project has no planned outputs yet.",
                            color = ProjectorIvory, fontSize = 14.sp)
                        if (task.archivedAtMillis == 0L &&
                            task.status != TaskStatus.DONE &&
                            task.status != TaskStatus.SKIPPED) {
                            TextButton(onClick = onOpenPlanOutputs) {
                                Text("PLAN FIRST OUTPUT", color = RecRed)
                            }
                        }
                    }
                } else {
                    item {
                        Text("SELECT DELIVERABLE", color = MutedGold, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            OutlinedButton(
                                onClick = { showOutputMenu = true },
                                enabled = !saving,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                    .testTag("ux051d-select-output"),
                            ) {
                                Text(
                                    selected?.let { output ->
                                        "${output.platform} · " +
                                            output.title.ifBlank { output.format.ifBlank { "Unnamed output" } }
                                    } ?: "Choose output",
                                    modifier = Modifier.weight(1f), maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showOutputMenu,
                                onDismissRequest = { showOutputMenu = false },
                            ) {
                                outputs.forEach { output ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${output.platform} · " +
                                                    output.title.ifBlank { output.format.ifBlank { "Unnamed output" } },
                                                maxLines = 2,
                                            )
                                        },
                                        onClick = {
                                            showOutputMenu = false
                                            changeOutput(output.id)
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            when {
                                selected?.let { output ->
                                    output.status == CreatorDeliverableStatus.PUBLISHED &&
                                        output.publishedAtMillis > 0L
                                } == true ->
                                    "Publication confirmed by creator · ready for manual reflection"
                                else -> "This output has no current confirmed live record."
                            },
                            color = MutedGold, fontSize = 12.sp,
                        )
                    }
                    if (allowedToWrite) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(17.dp),
                                color = CinemaSurface,
                                border = BorderStroke(1.dp, CinemaLine),
                            ) {
                                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                    Text("NEW REFLECTION", color = ProjectorIvory,
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Keep expectations, actual results and decisions distinct. " +
                                            "The entry is saved as a new snapshot, not an overwrite.",
                                        color = MutedText, fontSize = 12.sp,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    CW52Field("EXPECTED OUTCOME", expectation, { expectation = it },
                                        "What were you hoping to achieve?")
                                    CW52Field("WHAT HAPPENED", observed, { observed = it },
                                        "What did you actually observe?")
                                    CW52Field("WHAT WORKED", worked, { worked = it },
                                        "Strong moments and useful audience signals")
                                    CW52Field("WHAT TO IMPROVE", improve, { improve = it },
                                        "What should change next time?")
                                    CW52Field("NEXT CREATIVE ACTION", nextAction, { nextAction = it },
                                        "One concrete change for your next project")
                                    Text("OPTIONAL MANUALLY OBSERVED METRIC",
                                        color = MutedGold, fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(7.dp))
                                    CW52Field("METRIC LABEL", metricLabel, { metricLabel = it },
                                        "e.g. average view duration", minLines = 1)
                                    CW52Field("VALUE", metricValue, { metricValue = it },
                                        "e.g. 2m 12s", minLines = 1)
                                    CW52Field("SOURCE / WHEN CHECKED", metricSource,
                                        { metricSource = it },
                                        "e.g. creator-entered Studio figures from day 7",
                                        minLines = 1)
                                    if (hasDraft && !metricFieldsConsistent) {
                                        Text("Enter both the metric label and value, or clear both.",
                                            color = RecRed, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val now = System.currentTimeMillis()
                                            onSave(
                                                revision,
                                                CreatorOutputLearning(
                                                    id = UUID.randomUUID().toString(),
                                                    deliverableId = activeId, recordedAtMillis = now,
                                                    expectation = expectation,
                                                    observedOutcome = observed,
                                                    whatWorked = worked,
                                                    improveNext = improve,
                                                    nextAction = nextAction,
                                                    metricLabel = metricLabel,
                                                    metricValue = metricValue,
                                                    metricSource = metricSource,
                                                ),
                                            )
                                        },
                                        enabled = !saving && hasReflection && metricFieldsConsistent &&
                                            listOf(expectation, observed, worked, improve, nextAction)
                                                .all { it.length <= 2500 } &&
                                            metricLabel.length <= 120 &&
                                            metricValue.length <= 120 &&
                                            metricSource.length <= 300,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            .testTag("ux051d-save-learning"),
                                    ) {
                                        Text(if (saving) "SAVING…" else "RECORD LEARNING")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                when {
                                    task.archivedAtMillis != 0L || task.status == TaskStatus.SKIPPED ->
                                        "This project is archived or skipped. Existing learning remains readable."
                                    else ->
                                        "Record actual publication in Publish Studio before adding learning.",
                                },
                                color = MutedText, fontSize = 12.sp,
                            )
                            if (task.archivedAtMillis == 0L &&
                                task.status != TaskStatus.SKIPPED) {
                                TextButton(onClick = {
                                    if (hasDraft) pendingPublishNavigation = true else onOpenPublish()
                                }) {
                                    Text("OPEN PUBLISH STUDIO", color = RecRed)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("RECORDED LEARNING", color = ProjectorIvory,
                                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            Text("${history.size}", color = MutedGold, fontSize = 12.sp)
                        }
                    }
                    if (history.isEmpty()) item {
                        Text("No learning snapshots for this output yet.",
                            color = MutedText, fontSize = 12.sp)
                    }
                    items(history, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine.copy(alpha = .7f)),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                Text(
                                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                        .format(Date(item.recordedAtMillis)),
                                    color = MutedGold, fontSize = 11.sp,
                                )
                                CW52SavedField("EXPECTED", item.expectation)
                                CW52SavedField("OBSERVED", item.observedOutcome)
                                CW52SavedField("WORKED", item.whatWorked)
                                CW52SavedField("IMPROVE", item.improveNext)
                                CW52SavedField("NEXT ACTION", item.nextAction)
                                if (item.metricLabel.isNotBlank()) {
                                    CW52SavedField("MANUAL METRIC",
                                        "${item.metricLabel}: ${item.metricValue}")
                                }
                                CW52SavedField("SOURCE", item.metricSource)
                                Text(
                                    "Historical entry · original snapshot retained",
                                    color = MutedText, fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
                if (unavailableOutputHistory.isNotEmpty()) {
                    item {
                        Text("HISTORICAL LEARNING · OUTPUT UNAVAILABLE",
                            color = MutedGold, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                        Text(
                            "Older learning is retained even when its original deliverable " +
                                "is absent. No record was deleted or reassigned.",
                            color = MutedText, fontSize = 12.sp,
                        )
                    }
                    items(unavailableOutputHistory, key = { "missing:" + it.id }) { entry ->
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                Text("Original output ID: ${entry.deliverableId}",
                                    color = MutedGold, fontSize = 11.sp)
                                Text(
                                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                        .format(Date(entry.recordedAtMillis)),
                                    color = MutedText, fontSize = 11.sp,
                                )
                                CW52SavedField("OBSERVED", entry.observedOutcome)
                                CW52SavedField("WORKED", entry.whatWorked)
                                CW52SavedField("IMPROVE", entry.improveNext)
                                CW52SavedField("NEXT ACTION", entry.nextAction)
                            }
                        }
                    }
                }
            }
        }
    }
    if (pendingPublishNavigation) {
        AlertDialog(
            onDismissRequest = { pendingPublishNavigation = false },
            title = { Text("Leave this unsaved reflection?") },
            text = {
                Text("Opening Publish Studio will discard unsaved reflection text. " +
                    "Previously recorded learning stays safe.")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (discardCurrentDraft()) {
                        pendingPublishNavigation = false
                        onOpenPublish()
                    }
                }, enabled = !saving) { Text("DISCARD & OPEN") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPublishNavigation = false }) {
                    Text("KEEP EDITING")
                }
            },
        )
    }
    requestedSwitch?.let { destination ->
        AlertDialog(
            onDismissRequest = { requestedSwitch = null },
            title = { Text("Discard this unsaved reflection?") },
            text = {
                Text(
                    "Your current text has not been recorded. Switching outputs will discard it. " +
                        "Earlier saved learning entries remain intact.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (discardCurrentDraft()) {
                        selectedId = destination
                        requestedSwitch = null
                    }
                }, enabled = !saving) { Text("DISCARD & SWITCH") }
            },
            dismissButton = {
                TextButton(onClick = { requestedSwitch = null }) {
                    Text("KEEP EDITING")
                }
            },
        )
    }
}

@Composable
private fun CW52Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    minLines: Int = 2,
) {
    Text(label, color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(hint) }, minLines = minLines,
        shape = RoundedCornerShape(13.dp),
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun CW52SavedField(label: String, value: String) {
    if (value.isBlank()) return
    Spacer(Modifier.height(9.dp))
    Text(label, color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(value, color = ProjectorIvory, fontSize = 13.sp, lineHeight = 18.sp)
}
