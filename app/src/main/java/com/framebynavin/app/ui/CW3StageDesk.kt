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
        "evidence", "scene_evidence", "proof", "test", "benchmarks",
        "test_plan", "verify", "sources", "source", "research", "context",
        "known_unknown", "references" -> CW3StageExperience(
            "Evidence desk", "Which claims are verified, and what actual scenes or tests support them?",
            "Evidence, scene claim or observed test result",
            listOf("Verify each key claim", "Cross-check original sources"),
        )
        "idea", "concept", "promise", "audience", "question", "framework",
        "intent", "learning_goal", "criteria", "brief", "structure" -> CW3StageExperience(
            "Creative brief", "Define the decision, creative promise and evidence you want to build on.",
            "Why this choice supports the audience and the project goal",
            listOf("Write the core promise", "Confirm intended audience"),
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
        "voice", "record", "capture", "shot_plan", "session_plan",
        "visuals_capture", "process_capture" -> CW3StageExperience(
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
        "promote", "clips", "follow_up", "distribution", "review", "outcome" -> CW3StageExperience(
            "Distribution log", "Record platform plans and follow-ups, linked to the project outputs.",
            "Promotion or follow-up observation",
            listOf("Draft platform-specific copy", "Check live links"),
        )
        "route", "locations", "trip_plan", "method", "ingredients", "setup",
        "prerequisites", "safety", "sequence" -> CW3StageExperience(
            "Production plan", "Document dependencies, constraints and the next safe, practical steps.",
            "Decision, location, safety note or procedure observation",
            listOf("Confirm prerequisites", "Record the next actionable step"),
        )
        "script", "narration", "outline", "draft", "commentary", "guide_script" -> CW3StageExperience(
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
    onOpenMainTool: (StageWorkspaceDestination, Long, CreatorStageWork) -> Unit,
    onOpenOutputCraft: (Long, CreatorStageWork) -> Unit,
) {
    val context = LocalContext.current
    val template = CreatorWorkflowEngine.templateFor(task)
    val stageIndex = template.stages.indexOfFirst { it.id == stage.id }
    val revision = task.workspace.revision
    val journal = remember(task.id) { CreatorStageDraftStore(context.applicationContext) }
    val persisted = CreatorStageWorkPolicy.find(task.workspace, template.id, stage.id)
        ?: CreatorStageWork(workflowTemplateId = template.id, stageId = stage.id)
    // Planned projects can prepare stage materials before the first workflow step begins.
    val editable = task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED &&
        task.archivedAtMillis == 0L
    val initial = remember(task.id, template.id, stage.id, revision, editable) {
        // Never show noncanonical unsaved drafts as the official record of a
        // completed or archived project.
        if (editable) journal.load(task.id, revision, template.id, stage.id) ?: persisted
        else persisted
    }
    val experience = CW3StageExperiencePolicy.forStage(stage)
    val contextualModule = CW51StageModulePolicy.forStage(task, stage)
    val canonicalDestination = StageWorkspaceRoutingPolicy.destination(stage)
    val hasOutputs = task.workspace.deliverables.isNotEmpty()
    val isProductionStage = stage.id in setOf(
        "edit", "process_edit", "sound_grade", "mix", "visuals", "record", "capture",
    )
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
    var evidenceSourceId by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var evidenceAssetId by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var evidenceBeatId by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var editingEvidenceId by rememberSaveable(task.id, stage.id, revision) { mutableStateOf("") }
    var beatMenuOpen by remember { mutableStateOf(false) }
    var sourceMenuOpen by remember { mutableStateOf(false) }
    var assetMenuOpen by remember { mutableStateOf(false) }
    var journalWarning by remember(task.id, stage.id, revision) { mutableStateOf(false) }

    fun contentSnapshot(): CreatorStageWork = initial.copy(
        notes = notes, checklist = checklist, evidence = evidence,
        decisions = decisions, linkedReferenceIds = refs,
        linkedAssetIds = assets, linkedDeliverableIds = outputs,
        reviewedAtMillis = 0L,
    )
    // A review is valid only for the exact content explicitly reviewed.
    // Editing any field visibly unchecks Review and the saved snapshot carries no
    // stale reviewed timestamp. Re-checking captures the updated content.
    var reviewedContent by remember(task.id, template.id, stage.id, revision) {
        mutableStateOf(initial.copy(reviewedAtMillis = 0L))
    }
    fun snapshot(): CreatorStageWork {
        val content = contentSnapshot()
        return content.copy(
            reviewedAtMillis = if (reviewedAt > 0L && content == reviewedContent) reviewedAt else 0L,
        )
    }
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
                    Spacer(Modifier.height(12.dp))
                    val routeLabel = when (canonicalDestination) {
                        StageWorkspaceDestination.SCRIPT -> "OPEN SCRIPT STUDIO"
                        StageWorkspaceDestination.PUBLISH -> "OPEN PUBLISH STUDIO"
                        StageWorkspaceDestination.PLAN -> "OPEN SHARED PLAN"
                        StageWorkspaceDestination.HUB -> "OPEN SHARED PLAN"
                    }
                    OutlinedButton(
                        onClick = { onOpenMainTool(canonicalDestination, revision, snapshot()) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("ux05-open-canonical-tool"),
                    ) { Text(routeLabel) }
                    if (isProductionStage && hasOutputs) {
                        TextButton(
                            onClick = { onOpenOutputCraft(revision, snapshot()) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("ux05-open-output-craft"),
                        ) { Text("VIEW OUTPUT CUTS & SCRIPTS", color = MutedGold) }
                    }
                    Text(
                        "Opening another tool requires closing this draft first. " +
                            "Save stage work to preserve changes, or explicitly discard them.",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            contextualModule?.let { module ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine.copy(alpha = .65f)),
                    modifier = Modifier.fillMaxWidth().testTag("ux051c-stage-module"),
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text("STAGE FOCUS · ${module.name.uppercase()}",
                            color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(module.guidance, color = ProjectorIvory, fontSize = 13.sp,
                            lineHeight = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        module.questions.forEach { question ->
                            Text("• $question", color = MutedText, fontSize = 12.sp,
                                lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        val missingSuggestions = module.suggestedTasks.filter { proposed ->
                            checklist.none { it.title.equals(proposed, ignoreCase = true) }
                        }
                        if (editable && missingSuggestions.isNotEmpty()) {
                            Spacer(Modifier.height(9.dp))
                            OutlinedButton(
                                onClick = {
                                    checklist = checklist + missingSuggestions.map { suggestion ->
                                        CreatorStageChecklistItem(
                                            id = UUID.randomUUID().toString(),
                                            title = suggestion,
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .testTag("ux051c-add-context-tasks"),
                            ) {
                                Text("ADD ${missingSuggestions.size} FOCUS TASK(S)", color = MutedGold)
                            }
                        }
                        Text("Suggestions are optional. Save Stage Work to keep added tasks; no stage advances.",
                            color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Text("STAGE NOTES", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (!editable) Text(
                "Read-only · this project has been completed, skipped or archived.",
                color = MutedGold, fontSize = 12.sp,
            )
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
            evidence.forEachIndexed { index, item ->
                Surface(modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                    shape = RoundedCornerShape(12.dp), color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.claim, color = ProjectorIvory, fontSize = 13.sp)
                        if (item.timecode.isNotBlank()) Text("Timecode: ${item.timecode}", color = MutedText, fontSize = 11.sp)
                        if (item.notes.isNotBlank()) Text(item.notes, color = MutedText, fontSize = 11.sp)
                        if (item.referenceId.isNotBlank()) {
                            val source = task.workspace.references.firstOrNull { it.id == item.referenceId }
                            Text(
                                "Source: " + (source?.let { it.label.ifBlank { it.url } }
                                    ?: "Unavailable source (saved link retained)"),
                                color = MutedGold, fontSize = 11.sp,
                            )
                        }
                        if (item.assetId.isNotBlank()) {
                            val asset = task.workspace.assets.firstOrNull { it.id == item.assetId }
                            Text(
                                "Asset: " + (asset?.let { it.label.ifBlank { it.location } }
                                    ?: "Unavailable asset (saved link retained)"),
                                color = MutedGold, fontSize = 11.sp,
                            )
                        }
                        if (item.scriptBeatId.isNotBlank()) {
                            val beat = task.workspace.scriptStudio?.beats?.firstOrNull {
                                it.id == item.scriptBeatId
                            }
                            Text(
                                "Narration beat: " + (beat?.label?.ifBlank { beat.purpose.ifBlank { "Untitled beat" } }
                                    ?: "Unavailable beat (saved link retained)"),
                                color = MutedGold, fontSize = 11.sp,
                            )
                        }
                        if (editable) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = {
                                        evidence = evidence.toMutableList().also { list ->
                                            val moved = list.removeAt(index)
                                            list.add(index - 1, moved)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move evidence earlier",
                                        tint = if (index > 0) MutedGold else MutedText)
                                }
                                IconButton(
                                    enabled = index < evidence.lastIndex,
                                    onClick = {
                                        evidence = evidence.toMutableList().also { list ->
                                            val moved = list.removeAt(index)
                                            list.add(index + 1, moved)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move evidence later",
                                        tint = if (index < evidence.lastIndex) MutedGold else MutedText)
                                }
                                TextButton(onClick = {
                                    editingEvidenceId = item.id
                                    claim = item.claim
                                    timecode = item.timecode
                                    evidenceSourceId = item.referenceId
                                    evidenceAssetId = item.assetId
                                    evidenceBeatId = item.scriptBeatId
                                }) {
                                    Text("EDIT", color = MutedGold, fontSize = 11.sp)
                                }
                                TextButton(onClick = {
                                    evidence = evidence.filterNot { it.id == item.id }
                                    if (editingEvidenceId == item.id) {
                                        editingEvidenceId = ""
                                        claim = ""; timecode = ""
                                        evidenceSourceId = ""; evidenceAssetId = ""; evidenceBeatId = ""
                                    }
                                }) {
                                    Text("REMOVE", color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (editable) {
                if (editingEvidenceId.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("EDITING SAVED EVIDENCE · CHANGES ARE STILL A DRAFT",
                            color = MutedGold, fontSize = 11.sp,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            editingEvidenceId = ""
                            claim = ""; timecode = ""
                            evidenceSourceId = ""; evidenceAssetId = ""; evidenceBeatId = ""
                        }) { Text("CANCEL", color = MutedText) }
                    }
                }
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
                if (task.workspace.references.isNotEmpty() || task.workspace.assets.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Attach supporting project material to this claim (optional).",
                        color = MutedText, fontSize = 11.sp,
                    )
                    if (task.workspace.references.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { sourceMenuOpen = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .testTag("ux051-evidence-source-select"),
                            ) {
                                val selected = task.workspace.references.firstOrNull { it.id == evidenceSourceId }
                                Text("Source: " + (selected?.let { it.label.ifBlank { it.url } }
                                    ?: "None"), maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = sourceMenuOpen,
                                onDismissRequest = { sourceMenuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No source") },
                                    onClick = { evidenceSourceId = ""; sourceMenuOpen = false },
                                )
                                task.workspace.references.forEach { source ->
                                    DropdownMenuItem(
                                        text = { Text(source.label.ifBlank { source.url }, maxLines = 2) },
                                        onClick = { evidenceSourceId = source.id; sourceMenuOpen = false },
                                    )
                                }
                            }
                        }
                    }
                    if (task.workspace.assets.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { assetMenuOpen = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .testTag("ux051-evidence-asset-select"),
                            ) {
                                val selected = task.workspace.assets.firstOrNull { it.id == evidenceAssetId }
                                Text("Asset: " + (selected?.let { it.label.ifBlank { it.location } }
                                    ?: "None"), maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = assetMenuOpen,
                                onDismissRequest = { assetMenuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No asset") },
                                    onClick = { evidenceAssetId = ""; assetMenuOpen = false },
                                )
                                task.workspace.assets.forEach { asset ->
                                    DropdownMenuItem(
                                        text = { Text(asset.label.ifBlank { asset.location }, maxLines = 2) },
                                        onClick = { evidenceAssetId = asset.id; assetMenuOpen = false },
                                    )
                                }
                            }
                        }
                    }
                }
                if (!task.workspace.scriptStudio?.beats.isNullOrEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Box {
                        OutlinedButton(
                            onClick = { beatMenuOpen = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("ux051c-evidence-script-beat"),
                        ) {
                            val selectedBeat = task.workspace.scriptStudio?.beats?.firstOrNull {
                                it.id == evidenceBeatId
                            }
                            Text(
                                "Narration beat: " + (selectedBeat?.let { beat ->
                                    beat.label.ifBlank { beat.purpose.ifBlank { "Untitled beat" } }
                                } ?: "None"),
                                maxLines = 1,
                            )
                        }
                        DropdownMenu(
                            expanded = beatMenuOpen,
                            onDismissRequest = { beatMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("No linked beat") },
                                onClick = { evidenceBeatId = ""; beatMenuOpen = false },
                            )
                            task.workspace.scriptStudio?.beats.orEmpty().forEach { beat ->
                                DropdownMenuItem(
                                    text = { Text(beat.label.ifBlank {
                                        beat.purpose.ifBlank { "Untitled beat" }
                                    }, maxLines = 2) },
                                    onClick = { evidenceBeatId = beat.id; beatMenuOpen = false },
                                )
                            }
                        }
                    }
                    Text(
                        "This links to the saved Script Studio beat; narration is never duplicated.",
                        color = MutedText, fontSize = 11.sp,
                    )
                }
                TextButton(
                    onClick = {
                        if (claim.isNotBlank()) {
                            val old = evidence.firstOrNull { it.id == editingEvidenceId }
                            val referenceId = evidenceSourceId.takeIf { selected ->
                                task.workspace.references.any { it.id == selected } ||
                                    (old != null && old.referenceId == selected)
                            }.orEmpty()
                            val assetId = evidenceAssetId.takeIf { selected ->
                                task.workspace.assets.any { it.id == selected } ||
                                    (old != null && old.assetId == selected)
                            }.orEmpty()
                            val scriptBeatId = evidenceBeatId.takeIf { selected ->
                                task.workspace.scriptStudio?.beats.orEmpty().any { it.id == selected } ||
                                    (old != null && old.scriptBeatId == selected)
                            }.orEmpty()
                            if (old != null) {
                                evidence = evidence.map { record ->
                                    if (record.id == old.id) record.copy(
                                        claim = claim.trim(), timecode = timecode.trim(),
                                        referenceId = referenceId, assetId = assetId,
                                        scriptBeatId = scriptBeatId,
                                    ) else record
                                }
                            } else if (editingEvidenceId.isBlank()) {
                                evidence = evidence + CreatorStageEvidence(
                                    id = UUID.randomUUID().toString(),
                                    claim = claim.trim(), timecode = timecode.trim(),
                                    referenceId = referenceId, assetId = assetId,
                                    scriptBeatId = scriptBeatId,
                                )
                            }
                            editingEvidenceId = ""
                            claim = ""; timecode = ""
                            evidenceSourceId = ""; evidenceAssetId = ""; evidenceBeatId = ""
                        }
                    },
                    enabled = claim.isNotBlank() &&
                        (editingEvidenceId.isBlank() || evidence.any { it.id == editingEvidenceId }),
                ) {
                    Text(if (editingEvidenceId.isNotBlank()) "UPDATE EVIDENCE DRAFT"
                        else "ADD EVIDENCE", color = RecRed)
                }
                if (editingEvidenceId.isNotBlank()) {
                    Text("Save Stage Work below to persist the edited evidence.",
                        color = MutedText, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("LINKED SHARED MATERIALS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "Shared sources and outputs are edited in Plan or Publish. Linking them here " +
                    "does not duplicate media or move the workflow.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
            )
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
            if (decisions.isEmpty()) Text(
                "No decisions recorded for this stage yet.",
                color = MutedText, fontSize = 11.sp,
            )
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
                        checked = draft.reviewedAtMillis > 0L,
                        onCheckedChange = { checked ->
                            if (checked) {
                                reviewedContent = contentSnapshot()
                                reviewedAt = System.currentTimeMillis()
                            } else {
                                reviewedAt = 0L
                            }
                        },
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
    val unavailable = selected.filterNot { id -> available.any { it.first == id } }
    if (unavailable.isNotEmpty()) Text(
        "${unavailable.size} previously linked item(s) are unavailable here. " +
            "Their saved IDs are preserved for review; no automatic deletion.",
        color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp,
        modifier = Modifier.testTag("ux05-unavailable-linked-materials"),
    )
    if (available.isEmpty()) {
        Text("None currently available. Add project materials through Plan or Publish.",
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
