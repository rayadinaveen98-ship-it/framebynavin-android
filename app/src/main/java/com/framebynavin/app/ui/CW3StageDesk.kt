package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * CW3 changes the working context, not the meaning of stage completion.
 * These labels are presentation-only. Each stage still uses its real workflow
 * objective, and unsupported video/audio editing is never claimed.
 */
internal data class CW3StageExperience(
    val heading: String,
    val notePrompt: String,
    val evidencePrompt: String,
    val suggestedTasks: List<String>,
)

internal object CW3StageExperiencePolicy {
    fun forStage(stage: WorkflowStage): CW3StageExperience = when (stage.id) {
        "watch_notes" -> CW3StageExperience(
            "Viewing journal", "What stood out during the watch? Record reactions and observations.",
            "Scene observation or filmmaking choice",
            listOf("Capture important moments", "Record scene timecodes"),
        )
        "verdict", "angle" -> CW3StageExperience(
            "Verdict and angle", "What is the review's central point of view and audience promise?",
            "Specific example supporting your verdict",
            listOf("Decide the central argument", "Document strengths and limitations"),
        )
        "evidence", "scene_evidence", "proof", "test" -> CW3StageExperience(
            "Evidence desk", "Which claims are verified, and what actual scenes or tests support them?",
            "Evidence, scene claim or observed test result",
            listOf("Verify each key claim", "Cross-check original sources"),
        )
        "spoiler_plan" -> CW3StageExperience(
            "Spoiler map", "Separate spoiler-safe observations from material requiring a warning.",
            "Moment and whether it contains spoilers",
            listOf("Mark spoiler boundaries", "Plan a clear warning"),
        )
        "edit", "process_edit", "sound_grade", "mix", "visuals" -> CW3StageExperience(
            "Production and cut log", "Track versions, edit decisions and unresolved production issues. This is not a video editor.",
            "Edit review note with version or timecode",
            listOf("Review rough cut", "Check sound and picture", "Verify final export"),
        )
        "voice", "record", "capture" -> CW3StageExperience(
            "Recording log", "Plan or document takes, recording conditions and material still needed.",
            "Take, shot, or recording observation",
            listOf("Check recording setup", "Confirm usable takes"),
        )
        "package", "packaging", "thumbnail", "metadata", "cover", "artwork" -> CW3StageExperience(
            "Packaging checklist", "Record packaging decisions here; final titles and metadata belong in Publish Studio.",
            "Cover, title, or metadata review observation",
            listOf("Review cover and title fit", "Proofread metadata"),
        )
        "published", "send" -> CW3StageExperience(
            "Release verification", "Keep release notes here. Only Publish Studio can confirm an output is live.",
            "Release verification observation",
            listOf("Verify live page", "Check description and link"),
        )
        "promote", "clips", "follow_up" -> CW3StageExperience(
            "Distribution log", "Record platform plans and follow-ups, linked to the project outputs.",
            "Promotion or follow-up observation",
            listOf("Draft platform-specific copy", "Check live links"),
        )
        "script", "narration", "outline", "draft", "commentary" -> CW3StageExperience(
            "Narrative notes", "Keep stage decisions here; the canonical narration remains in Script Studio.",
            "Script evidence or narration note",
            listOf("Review structure", "Verify supporting evidence"),
        )
        else -> CW3StageExperience(
            stage.label + " desk",
            stage.action.ifBlank { "Capture the work and decisions for this stage." },
            "Observation, reference or supporting evidence",
            emptyList(),
        )
    }
}

/**
 * One reusable stage-owned working desk for all workflow templates.
 * Everything is scoped by current project ID + stable template/stage IDs.
 * Drafts are locally journaled; only explicit Save touches TaskStore.
 */
@Composable
internal fun CW3StageDesk(
    task: CreatorTask,
    stage: WorkflowStage,
    onSave: (Long, CreatorStageWork) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val template = CreatorWorkflowEngine.templateFor(task)
    val stageIndex = template.stages.indexOfFirst { it.id == stage.id }
    val revision = task.workspace.revision
    val journal = remember(task.id) { CreatorStageDraftStore(context.applicationContext) }
    val persisted = CreatorStageWorkPolicy.find(task.workspace, template.id, stage.id)
        ?: CreatorStageWork(workflowTemplateId = template.id, stageId = stage.id)
    val initial = remember(task.id, template.id, stage.id, revision) {
        journal.load(task.id, revision, template.id, stage.id) ?: persisted
    }
    // Planned projects can prepare stage materials before the first workflow step begins.
    val editable = task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED &&
        task.archivedAtMillis == 0L
    val experience = CW3StageExperiencePolicy.forStage(stage)
    var notes by rememberSaveable(task.id, stage.id, revision) { mutableStateOf(initial.notes) }
    var checklist by remember(task.id, stage.id, revision) { mutableStateOf(initial.checklist) }
    var evidence by remember(task.id, stage.id, revision) { mutableStateOf(initial.evidence) }
    var decisions by remember(task.id, stage.id, revision) { mutableStateOf(initial.decisions) }
    var refs by remember(task.id, stage.id, revision) { mutableStateOf(initial.linkedReferenceIds) }
    var assets by remember(task.id, stage.id, revision) { mutableStateOf(initial.linkedAssetIds) }
    var outputs by remember(task.id, stage.id, revision) { mutableStateOf(initial.linkedDeliverableIds) }
    var reviewedAt by remember(task.id, stage.id, revision) { mutableLongStateOf(initial.reviewedAtMillis) }
    var newTask by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var claim by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var timecode by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var decisionText by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var journalWarning by remember(task.id, stage.id, revision) { mutableStateOf(false) }

    fun snapshot(): CreatorStageWork = initial.copy(
        notes = notes, checklist = checklist, evidence = evidence,
        decisions = decisions, linkedReferenceIds = refs,
        linkedAssetIds = assets, linkedDeliverableIds = outputs,
        reviewedAtMillis = reviewedAt,
    )
    val draft = snapshot()
    val unsaved = draft != persisted

    // The draft journal is not a project write. It can be recovered only while
    // the exact project revision and restore generation still match.
    LaunchedEffect(task.id, revision, stage.id, draft, editable) {
        if (!editable) return@LaunchedEffect
        delay(400)
        val failure = withContext(Dispatchers.IO) {
            runCatching {
                if (draft != persisted) journal.save(task.id, revision, draft)
                else journal.clear(task.id, template.id, stage.id)
            }.exceptionOrNull()
        }
        if (failure != null) journalWarning = true
    }

    WorkspaceAdaptiveFrame {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).testTag("cw3-stage-desk"),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to project", tint = ProjectorIvory)
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (CreatorWorkflowEngine.currentStage(task).id == stage.id)
                            "CURRENT PROJECT STAGE" else "EARLIER / OTHER PROJECT STAGE",
                        color = MutedText, fontSize = 11.sp,
                    )
                    Text(stage.label, color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (stageIndex >= 0) "${stageIndex + 1}/${template.stages.size}" else "STAGE",
                    color = RecRed, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = CinemaSurfaceRaised,
                border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(experience.heading, color = ProjectorIvory, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(stage.action, color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("One shared project library · This stage has its own notes and evidence",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("STAGE NOTES", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().testTag("cw3-stage-notes"),
                label = { Text("Notes for ${stage.label}") },
                supportingText = { Text(experience.notePrompt) },
                minLines = 3, maxLines = 8, enabled = editable,
            )
            Spacer(Modifier.height(20.dp))
            Text("STAGE TASKS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("These checklists never advance your project's workflow automatically.",
                color = MutedText, fontSize = 12.sp)
            checklist.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = item.status == CreatorChecklistStatus.DONE,
                        onCheckedChange = { yes ->
                            if (editable) checklist = checklist.map {
                                if (it.id == item.id) it.copy(status = if (yes) CreatorChecklistStatus.DONE else CreatorChecklistStatus.TODO)
                                else it
                            }
                        }, enabled = editable,
                    )
                    Text(item.title, color = ProjectorIvory, modifier = Modifier.weight(1f),
                        fontSize = 13.sp)
                    if (editable) TextButton(onClick = {
                        checklist = checklist.filterNot { it.id == item.id }
                    }) { Text("REMOVE", color = MutedText, fontSize = 10.sp) }
                }
            }
            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTask, onValueChange = { newTask = it },
                        modifier = Modifier.weight(1f).testTag("cw3-new-task"),
                        label = { Text("Add stage task") }, singleLine = true,
                    )
                    Button(onClick = {
                        if (newTask.isNotBlank()) {
                            checklist = checklist + CreatorStageChecklistItem(
                                id = UUID.randomUUID().toString(), title = newTask.trim(),
                            )
                            newTask = ""
                        }
                    }, enabled = newTask.isNotBlank()) { Text("ADD") }
                }
                experience.suggestedTasks.filter { suggestion ->
                    checklist.none { it.title.equals(suggestion, ignoreCase = true) }
                }.forEach { suggestion ->
                    TextButton(onClick = {
                        checklist = checklist + CreatorStageChecklistItem(
                            id = UUID.randomUUID().toString(), title = suggestion,
                        )
                    }) { Text("+ $suggestion", color = RecRed, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("EVIDENCE & TIMECODES", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(experience.evidencePrompt, color = MutedText, fontSize = 12.sp)
            evidence.forEach { item ->
                Surface(modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                    shape = RoundedCornerShape(12.dp), color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.claim, color = ProjectorIvory, fontSize = 13.sp)
                        if (item.timecode.isNotBlank()) Text("Timecode: ${item.timecode}", color = MutedText, fontSize = 11.sp)
                        if (item.notes.isNotBlank()) Text(item.notes, color = MutedText, fontSize = 11.sp)
                        if (editable) TextButton(onClick = { evidence = evidence.filterNot { it.id == item.id } }) {
                            Text("REMOVE EVIDENCE", color = MutedText, fontSize = 10.sp)
                        }
                    }
                }
            }
            if (editable) {
                OutlinedTextField(
                    value = claim, onValueChange = { claim = it },
                    modifier = Modifier.fillMaxWidth().testTag("cw3-evidence-claim"),
                    label = { Text("Scene, claim, take or observation") }, minLines = 2,
                )
                Spacer(Modifier.height(5.dp))
                OutlinedTextField(
                    value = timecode, onValueChange = { timecode = it },
                    modifier = Modifier.fillMaxWidth().testTag("cw3-evidence-timecode"),
                    label = { Text("Timecode / version (optional)") }, singleLine = true,
                )
                TextButton(
                    onClick = {
                        if (claim.isNotBlank()) {
                            evidence = evidence + CreatorStageEvidence(
                                id = UUID.randomUUID().toString(),
                                claim = claim.trim(), timecode = timecode.trim(),
                            )
                            claim = ""; timecode = ""
                        }
                    },
                    enabled = claim.isNotBlank(),
                ) { Text("ADD EVIDENCE", color = RecRed) }
            }
            Spacer(Modifier.height(20.dp))

            Text("LINKED SHARED MATERIALS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Links point to this project's materials, not copied media or guaranteed restored files.",
                color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
            CW3MaterialLinks("Sources", task.workspace.references.map { it.id to it.label.ifBlank { it.url } },
                refs, enabled = editable, onChange = { refs = it })
            CW3MaterialLinks("Assets", task.workspace.assets.map { it.id to it.label.ifBlank { it.location } },
                assets, enabled = editable, onChange = { assets = it })
            CW3MaterialLinks("Outputs", task.workspace.deliverables.map { it.id to
                (it.title.ifBlank { it.format } + " · " + it.platform) },
                outputs, enabled = editable, onChange = { outputs = it })
            Spacer(Modifier.height(20.dp))

            Text("CREATIVE DECISIONS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            decisions.forEach { item ->
                Text("• ${item.text}", color = ProjectorIvory, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    fontSize = 13.sp)
            }
            if (editable) {
                OutlinedTextField(
                    value = decisionText, onValueChange = { decisionText = it },
                    modifier = Modifier.fillMaxWidth().testTag("cw3-new-decision"),
                    label = { Text("Record a decision") }, minLines = 2,
                )
                TextButton(onClick = {
                    if (decisionText.isNotBlank()) {
                        decisions = decisions + CreatorStageDecision(
                            id = UUID.randomUUID().toString(), text = decisionText.trim(),
                            recordedAtMillis = System.currentTimeMillis(),
                        )
                        decisionText = ""
                    }
                }, enabled = decisionText.isNotBlank()) { Text("ADD DECISION", color = RecRed) }
            }
            Spacer(Modifier.height(22.dp))

            if (journalWarning) Text("Local draft recovery is unavailable. Use SAVE STAGE WORK before leaving.",
                color = RecRed, fontSize = 12.sp)
            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = reviewedAt > 0L,
                        onCheckedChange = { reviewedAt = if (it) System.currentTimeMillis() else 0L },
                    )
                    Text("Mark stage notes reviewed (does not complete the workflow)",
                        color = MutedText, modifier = Modifier.weight(1f), fontSize = 12.sp)
                }
                Button(onClick = { onSave(revision, snapshot()) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("cw3-save-stage"),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed), enabled = unsaved) {
                    Text("SAVE STAGE WORK", fontWeight = FontWeight.Bold)
                }
                Text(if (unsaved) "Draft only · Save to update this project" else "All stage changes saved",
                    color = MutedText, modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                    fontSize = 11.sp)
            } else {
                Text("Read-only stage · Completed or archived project data is preserved.",
                    color = MutedText, fontSize = 12.sp)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CW3MaterialLinks(
    label: String,
    available: List<Pair<String, String>>,
    selected: List<String>,
    enabled: Boolean,
    onChange: (List<String>) -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Text(label, color = ProjectorIvory, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    if (available.isEmpty()) {
        Text("None yet. Add items through Plan or Publish from the project hub.",
            color = MutedText, fontSize = 11.sp)
        return
    }
    available.forEach { (id, name) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = id in selected, enabled = enabled,
                onCheckedChange = { checked ->
                    onChange(if (checked) selected + id else selected.filterNot { it == id })
                }
            )
            Text(name, modifier = Modifier.weight(1f), color = ProjectorIvory, fontSize = 12.sp,
                maxLines = 2)
        }
    }
}
