package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
internal fun V19ContentWorkspaceAlpha4Hub(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenScript: () -> Unit,
) {
    val workspace = task.workspace
    val progress = workspace.productionProgressPercent()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Close", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("PROJECT", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        Text(task.title, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text("R${workspace.revision}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
                Text("CREATOR WORKSPACE", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Choose what you want to work on next.", color = MutedText, fontSize = 10.sp)

                Spacer(Modifier.height(20.dp))
                Alpha4HubCard(
                    icon = Icons.Outlined.Dashboard,
                    eyebrow = "PROJECT COMMAND CENTER",
                    title = "Plan & Produce",
                    body = "Brief, research, assets, supporting checklist, templates, deliverables and learnings.",
                    meta = "$progress% checklist · ${workspace.assets.size} assets · ${workspace.deliverables.size} outputs",
                    onClick = onOpenProject,
                )
                Spacer(Modifier.height(12.dp))
                Alpha4HubCard(
                    icon = Icons.Outlined.EditNote,
                    eyebrow = "SINGLE SCRIPT AUTHORITY",
                    title = "Script Studio",
                    body = "Develop hooks and titles, structure beats, write narration, plan visuals and prepare the script for recording.",
                    meta = "Script, narration and visual notes",
                    onClick = onOpenScript,
                )

                Spacer(Modifier.weight(1f))
                Text(
                    "Edit your hook and narration here.",
                    color = MutedText,
                    fontSize = 8.5.sp,
                    modifier = Modifier.padding(bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun Alpha4HubCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    eyebrow: String,
    title: String,
    body: String,
    meta: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(18.dp)) {
            Icon(icon, null, tint = MutedGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(14.dp))
            Text(eyebrow, color = RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            Text(title, color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(body, color = MutedText, fontSize = 10.sp)
            Spacer(Modifier.height(12.dp))
            Text(meta, color = MutedGold, fontSize = 8.7.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun V19ScriptStudioAlpha4Dialog(
    task: CreatorTask,
    studio: CreatorScriptStudio,
    onDismiss: () -> Unit,
    onSave: (Long, Long, CreatorScriptStudio) -> Unit,
    onSaveAndPublish: (Long, Long, CreatorScriptStudio) -> Unit = onSave,
    hasHeldDraft: Boolean = false,
    onReviewHeld: () -> Unit = {},
) {
    val context = LocalContext.current
    val originalStudioRevision = studio.revision
    val originalWorkspaceRevision = task.workspace.revision
    val draftStore = remember { CreatorEditorDraftStore(context.applicationContext) }
    val recovered = remember(task.id, originalWorkspaceRevision, originalStudioRevision) {
        draftStore.loadScript(task.id, originalWorkspaceRevision, originalStudioRevision)
    }
    val initial = recovered ?: studio
    var status by remember(task.id, originalStudioRevision) { mutableStateOf(initial.status) }
    var hooks by remember(task.id, originalStudioRevision) { mutableStateOf(initial.hooks) }
    var titles by remember(task.id, originalStudioRevision) { mutableStateOf(initial.titles) }
    var beats by remember(task.id, originalStudioRevision) { mutableStateOf(initial.beats) }
    var creatorNotes by remember(task.id, originalStudioRevision) { mutableStateOf(initial.creatorNotes) }
    var showPreview by remember { mutableStateOf(false) }
    var showPlanBrief by rememberSaveable(task.id) { mutableStateOf(false) }
    val planBrief = remember(task.id, originalWorkspaceRevision) {
        CreatorPlanScriptBridge.fields(task.workspace)
    }

    fun draft() = CreatorScriptStudio(
        projectId = task.id,
        revision = originalStudioRevision,
        status = status,
        hooks = hooks,
        titles = titles,
        beats = beats,
        creatorNotes = creatorNotes,
    )

    var draftWarningShown by remember(task.id, originalStudioRevision) { mutableStateOf(false) }
    LaunchedEffect(status, hooks, titles, beats, creatorNotes) {
        delay(350)
        val snapshot = draft()
        val saveError = withContext(Dispatchers.IO) {
            runCatching {
            if (snapshot == studio) draftStore.clearScript(task.id)
            else draftStore.saveScript(
                projectId = task.id,
                baseWorkspaceRevision = originalWorkspaceRevision,
                baseStudioRevision = originalStudioRevision,
                draft = snapshot,
            )
            }.exceptionOrNull()
        }
        if (saveError != null && !draftWarningShown) {
            draftWarningShown = true
            android.widget.Toast.makeText(
                context,
                "Could not save this draft. Reopen the project if a backup was restored.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun moveBeat(index: Int, delta: Int) {
        val target = index + delta
        if (index !in beats.indices || target !in beats.indices) return
        beats = beats.toMutableList().also { list ->
            val item = list.removeAt(index)
            list.add(target, item)
        }
    }

    fun selectHook(id: String) {
        hooks = hooks.map { it.copy(selected = it.id == id) }
    }

    fun selectTitle(id: String) {
        titles = titles.map { it.copy(selected = it.id == id) }
    }

    val current = draft()
    WorkspaceAdaptiveFrame {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("SCRIPT", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(task.title, color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (hasHeldDraft) {
                        TextButton(onClick = onReviewHeld) {
                            Text("EARLIER DRAFT", color = MutedGold, fontSize = 10.sp)
                        }
                    }
                }

                if (recovered != null) {
                    Surface(Modifier.fillMaxWidth(), color = MutedGold.copy(alpha = .08f)) {
                        Text("Your unsaved script was recovered", color = MutedGold, fontSize = 8.5.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp))
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp).padding(bottom = 30.dp),
                ) {
                    Alpha4ScriptSummary(current)
                    if (planBrief.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            Modifier.fillMaxWidth(),
                            RoundedCornerShape(17.dp),
                            CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
                                TextButton(
                                    onClick = { showPlanBrief = !showPlanBrief },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                                ) {
                                    Text("PLAN BRIEF", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    Text(if (showPlanBrief) "HIDE" else "VIEW", color = MutedGold, fontSize = 12.sp)
                                    Icon(
                                        if (showPlanBrief) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = null, tint = MutedGold,
                                    )
                                }
                                if (showPlanBrief) {
                                    planBrief.forEach { field ->
                                        Spacer(Modifier.height(9.dp))
                                        Text(field.label.uppercase(), color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(3.dp))
                                        Text(field.value, color = ProjectorIvory, fontSize = 13.sp, lineHeight = 19.sp)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    Alpha4Title("STATUS", "")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CreatorScriptStatus.entries.forEach { item ->
                            FilterChip(
                                selected = status == item,
                                onClick = { status = item },
                                label = { Text(item.name.replace('_', ' '), fontSize = 12.sp) },
                                modifier = Modifier.heightIn(min = 44.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Alpha4Title("HOOKS", "")
                    Button(onClick = { hooks = hooks + CreatorHookIdea(id = UUID.randomUUID().toString()) }) {
                        Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(5.dp)); Text("ADD HOOK")
                    }
                    Spacer(Modifier.height(8.dp))
                    hooks.forEach { hook ->
                        Alpha4IdeaCard(
                            value = hook.text,
                            selected = hook.selected,
                            label = "Hook idea",
                            onValue = { value -> hooks = hooks.map { if (it.id == hook.id) it.copy(text = value) else it } },
                            onSelect = { selectHook(hook.id) },
                            onRemove = { hooks = hooks.filterNot { it.id == hook.id } },
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                    if (hooks.isEmpty()) Alpha4Empty("No opening hook yet. Add one to start.")

                    Spacer(Modifier.height(22.dp))
                    Alpha4Title("TITLES", "")
                    Button(onClick = { titles = titles + CreatorTitleIdea(id = UUID.randomUUID().toString()) }) {
                        Icon(Icons.Outlined.Title, null); Spacer(Modifier.width(5.dp)); Text("ADD TITLE IDEA")
                    }
                    Spacer(Modifier.height(8.dp))
                    titles.forEach { title ->
                        Alpha4IdeaCard(
                            value = title.text,
                            selected = title.selected,
                            label = "Title idea",
                            onValue = { value -> titles = titles.map { if (it.id == title.id) it.copy(text = value) else it } },
                            onSelect = { selectTitle(title.id) },
                            onRemove = { titles = titles.filterNot { it.id == title.id } },
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                    if (titles.isEmpty()) Alpha4Empty("No title ideas yet.")

                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Alpha4Title("BEATS", "", Modifier.weight(1f))
                        TextButton(onClick = {
                            beats = beats + CreatorScriptBeat(id = UUID.randomUUID().toString(), label = "Beat ${beats.size + 1}")
                        }) {
                            Icon(Icons.Outlined.AddCircleOutline, null); Spacer(Modifier.width(4.dp)); Text("ADD BEAT")
                        }
                    }
                    beats.forEachIndexed { index, beat ->
                        Alpha4BeatCard(
                            beat = beat,
                            canUp = index > 0,
                            canDown = index < beats.lastIndex,
                            onUp = { moveBeat(index, -1) },
                            onDown = { moveBeat(index, 1) },
                            onChange = { changed -> beats = beats.map { if (it.id == changed.id) changed else it } },
                            onRemove = { beats = beats.filterNot { it.id == beat.id } },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (beats.isEmpty()) Alpha4Empty("No structured beats yet. Your old script is imported automatically the first time Script Studio opens.")

                    Spacer(Modifier.height(22.dp))
                    Alpha4Title("NOTES", "")
                    OutlinedTextField(
                        value = creatorNotes,
                        onValueChange = { creatorNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        placeholder = { Text("Notes for yourself while writing or recording…", color = MutedText) },
                        shape = RoundedCornerShape(16.dp),
                    )

                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(onClick = { showPreview = !showPreview }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (showPreview) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (showPreview) "HIDE COMPILED PREVIEW" else "PREVIEW COMPILED NARRATION")
                    }
                    if (showPreview) {
                        Spacer(Modifier.height(8.dp))
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(current.selectedHook().ifBlank { "No selected hook" }, color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(current.compiledNarration().ifBlank { "No narration yet." }, color = ProjectorIvory, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
                    WorkspaceAdaptiveSaveActions(
                        primaryLabel = "SAVE & PUBLISH",
                        onPrimary = { onSaveAndPublish(originalStudioRevision, originalWorkspaceRevision, draft()) },
                        onSecondary = { onSave(originalStudioRevision, originalWorkspaceRevision, draft()) },
                    )
                }
    }
}

@Composable
private fun Alpha4ScriptSummary(studio: CreatorScriptStudio) {
    val progress = studio.progressPercent()
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Alpha4Metric("WORDS", studio.wordCount().toString())
                Alpha4Metric("BEATS", studio.beats.size.toString())
                Alpha4Metric("READY", studio.readyBeatCount().toString())
                Alpha4Metric("PROGRESS", "$progress%")
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            val selectedTitle = studio.selectedTitle()
            if (selectedTitle.isNotBlank()) {
                Spacer(Modifier.height(9.dp)); Text("Selected title: $selectedTitle", color = MutedGold, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun Alpha4Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Alpha4IdeaCard(
    value: String,
    selected: Boolean,
    label: String,
    onValue: (String) -> Unit,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val accent = if (selected) MutedGold else CinemaLine
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, accent)) {
        Column(Modifier.padding(11.dp)) {
            OutlinedTextField(value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, minLines = 2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelect) {
                    Icon(if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null)
                    Spacer(Modifier.width(4.dp)); Text(if (selected) "SELECTED" else "SELECT")
                }
                TextButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove", tint = MutedText) }
            }
        }
    }
}

@Composable
private fun Alpha4BeatCard(
    beat: CreatorScriptBeat,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onChange: (CreatorScriptBeat) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by rememberSaveable(beat.id) { mutableStateOf(false) }
    val accent = when (beat.status) {
        CreatorScriptBeatStatus.DRAFT -> MutedText
        CreatorScriptBeatStatus.READY -> MutedGold
        CreatorScriptBeatStatus.LOCKED -> SuccessGreen
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurface, border = BorderStroke(1.dp, accent.copy(alpha = .65f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(beat.status.name, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(beat.label.ifBlank { "Untitled beat" }, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        if (expanded) "Collapse beat" else "Edit beat", tint = MutedGold)
                }
                IconButton(onClick = onUp, enabled = canUp) { Icon(Icons.Outlined.KeyboardArrowUp, "Move up") }
                IconButton(onClick = onDown, enabled = canDown) { Icon(Icons.Outlined.KeyboardArrowDown, "Move down") }
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove", tint = MutedText) }
            }
            if (expanded) {
                OutlinedTextField(value = beat.label, onValueChange = { onChange(beat.copy(label = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Beat label") }, singleLine = true)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value = beat.purpose, onValueChange = { onChange(beat.copy(purpose = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Purpose / emotional job") }, minLines = 2)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value = beat.narration, onValueChange = { onChange(beat.copy(narration = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Narration") }, minLines = 5)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value = beat.visualNotes, onValueChange = { onChange(beat.copy(visualNotes = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Primary visual / shot notes") }, minLines = 2)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value = beat.bRollNotes, onValueChange = { onChange(beat.copy(bRollNotes = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("B-roll / insert ideas") }, minLines = 2)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value = beat.onScreenText, onValueChange = { onChange(beat.copy(onScreenText = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("On-screen text") }, minLines = 2)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CreatorScriptBeatStatus.entries.forEach { item ->
                    FilterChip(
                        selected = beat.status == item,
                        onClick = { onChange(beat.copy(status = item)) },
                        label = { Text(item.name, fontSize = 12.sp) },
                        modifier = Modifier.heightIn(min = 44.dp),
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun Alpha4Title(title: String, @Suppress("UNUSED_PARAMETER") subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(11.dp))
    }
}

@Composable
private fun Alpha4Empty(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(13.dp))
    }
}
