@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.voice.VoiceIdeaRecording
import com.framebynavin.app.youtube.YouTubeAnalyticsStore
import com.framebynavin.app.youtube.YouTubeOpportunityEngine
import com.framebynavin.app.widget.CreatorWidgetContract
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.platform.testTag

@Composable
internal fun V09IdeaVaultScreen(
    ideas: List<CreatorIdea>,
    deletedIdeas: List<CreatorIdea> = emptyList(),
    onClose: (() -> Unit)?,
    onSave: (CreatorIdea) -> String?,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit = {},
    onArchive: (String) -> Unit,
    onConvert: (String, String, String, Long) -> String?,
    onConvertDurably: (suspend (IdeaProjectConversionRequest) -> String?)? = null,
    externalIdeaId: String = "",
    externalIdeaMode: String = "",
    externalLaunchNonce: Long = 0L,
    bottomNavigationClearance: androidx.compose.ui.unit.Dp = 124.dp,
) {
    val context = LocalContext.current
    val creatorProfile = remember { CreatorOsSettingsStore(context.applicationContext).snapshot().creatorProfile }
    val visibleCategories = remember(creatorProfile) { IdeaVaultLabels.categoriesFor(creatorProfile) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<IdeaStatus?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<IdeaCategory?>(null) }
    var captureFilter by rememberSaveable { mutableStateOf<IdeaCaptureType?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    var deletePending by remember { mutableStateOf<CreatorIdea?>(null) }
    var conversionFailed by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CreatorIdea?>(null) }
    var creating by remember { mutableStateOf(false) }
    var converting by remember { mutableStateOf<CreatorIdea?>(null) }
    var handledExternalNonce by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(externalLaunchNonce, externalIdeaId, ideas.size) {
        if (externalLaunchNonce == 0L || externalLaunchNonce == handledExternalNonce || externalIdeaId.isBlank()) {
            return@LaunchedEffect
        }
        val target = ideas.firstOrNull { it.id == externalIdeaId } ?: return@LaunchedEffect
        handledExternalNonce = externalLaunchNonce
        when (externalIdeaMode) {
            CreatorWidgetContract.IDEA_MODE_CONVERT -> converting = target
            else -> editing = target
        }
    }

    val opportunityReport = YouTubeAnalyticsStore.latest24HourReport
    val opportunityAlerts by remember(opportunityReport) {
        derivedStateOf { YouTubeOpportunityEngine.build(opportunityReport, ideas) }
    }
    val opportunityIdeaIds by remember { derivedStateOf { opportunityAlerts.mapNotNull { it.ideaId }.toSet() } }
    val opportunityMatch by remember { derivedStateOf { opportunityAlerts.firstOrNull { it.ideaId != null } } }
    val readyCount by remember { derivedStateOf { ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE } } }
    val voiceCount by remember { derivedStateOf { ideas.count { it.captureType == IdeaCaptureType.VOICE } } }
    val activeFilterCount by remember {
        derivedStateOf {
            listOfNotNull(
                statusFilter?.takeIf { it !in setOf(IdeaStatus.INBOX, IdeaStatus.READY_TO_PRODUCE, IdeaStatus.CONVERTED) },
                categoryFilter,
                captureFilter,
            ).size
        }
    }

    val filtered by remember {
        derivedStateOf {
            (if (showDeleted) deletedIdeas else ideas).filter { idea ->
                val textMatch = query.isBlank() || listOf(idea.title, idea.topic, idea.notes, idea.transcript)
                    .any { it.contains(query, ignoreCase = true) }
                val statusMatch = statusFilter == null || idea.status == statusFilter
                val categoryMatch = categoryFilter == null || idea.category == categoryFilter
                val captureMatch = captureFilter == null || idea.captureType == captureFilter
                textMatch && (showDeleted || (statusMatch && categoryMatch && captureMatch))
            }.sortedWith(
                compareByDescending<CreatorIdea> { showDeleted && it.isTrashed }
                    .thenByDescending { if (showDeleted) it.trashedAtMillis else 0L }
                    .thenByDescending { !showDeleted && it.id in opportunityIdeaIds }
                    .thenBy { it.status == IdeaStatus.ARCHIVED }
                    .thenByDescending { it.updatedAtMillis }
            )
        }
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onClose != null) IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Column(Modifier.weight(1f)) {
                    Text("IDEAS", color = MutedGold, fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                    Text("Save it now. Make it later.", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                TextButton(onClick = {
                    showDeleted = !showDeleted
                    if (showDeleted) {
                        statusFilter = null
                        categoryFilter = null
                        captureFilter = null
                    }
                }) {
                    Text(if (showDeleted) "ALL IDEAS" else "DELETED (${deletedIdeas.size})",
                        color = MutedGold, fontSize = 10.sp)
                }
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Outlined.Add, "New idea", tint = ProjectorIvory)
                }
            }

            opportunityMatch?.let { match ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MutedGold.copy(alpha = .055f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = .24f)),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("RELATED TO YOUR CHANNEL", color = MutedGold, fontSize = 7.2.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                            Text(match.ideaTitle ?: match.title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("Search your ideas…") },
            )

            Spacer(Modifier.height(9.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = !showDeleted && statusFilter == null, onClick = { showDeleted = false; statusFilter = null }, label = { Text("All", fontSize = 10.sp) })
                FilterChip(selected = !showDeleted && statusFilter == IdeaStatus.INBOX, onClick = { showDeleted = false; statusFilter = IdeaStatus.INBOX }, label = { Text("Inbox", fontSize = 10.sp) })
                FilterChip(selected = !showDeleted && statusFilter == IdeaStatus.READY_TO_PRODUCE, onClick = { showDeleted = false; statusFilter = IdeaStatus.READY_TO_PRODUCE }, label = { Text("Ready", fontSize = 10.sp) })
                FilterChip(selected = !showDeleted && statusFilter == IdeaStatus.CONVERTED, onClick = { showDeleted = false; statusFilter = IdeaStatus.CONVERTED }, label = { Text("Converted", fontSize = 10.sp) })
                FilterChip(selected = showDeleted, onClick = { showDeleted = true; statusFilter = null; categoryFilter = null; captureFilter = null }, label = { Text("Recently Deleted (${deletedIdeas.size})", fontSize = 10.sp) })
                if (!showDeleted) FilterChip(
                    selected = activeFilterCount > 0,
                    onClick = { showFilters = true },
                    label = { Text(if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters", fontSize = 10.sp) },
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${filtered.size} IDEA${if (filtered.size == 1) "" else "S"}", color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                if (!showDeleted) Text("$readyCount READY", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }

            if (filtered.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = MutedGold, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(9.dp))
                    Text(if (showDeleted) "No deleted ideas" else if (ideas.isEmpty()) "Your vault is empty" else "No ideas match this filter", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    Text("Keep ideas here until you're ready to use them.", color = MutedText, fontSize = 10.sp)
                    if (!showDeleted && ideas.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { creating = true }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("SAVE YOUR FIRST IDEA") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomNavigationClearance),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { idea ->
                        if (showDeleted) {
                            V162DeletedIdeaCard(idea) { onRestore(idea.id) }
                        } else V09IdeaCard(
                            idea = idea,
                            isOpportunity = idea.id in opportunityIdeaIds,
                            onEdit = { editing = idea },
                            onConvert = { converting = idea },
                            onArchive = { onArchive(idea.id) },
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        V150IdeaFilterSheet(
            visibleCategories = visibleCategories,
            statusFilter = statusFilter,
            categoryFilter = categoryFilter,
            captureFilter = captureFilter,
            voiceCount = voiceCount,
            onStatus = { statusFilter = it },
            onCategory = { categoryFilter = it },
            onCapture = { captureFilter = it },
            onDismiss = { showFilters = false },
        )
    }

    if (creating) {
        V09IdeaEditor(
            idea = CreatorIdea(id = "", title = ""),
            onDismiss = { creating = false },
            onSave = { draft ->
                val accepted = onSave(draft) != null
                if (accepted) { creating = false; showDeleted = false }
                accepted
            },
            existingIdeas = ideas,
            onDelete = null,
        )
    }

    editing?.let { idea ->
        V09IdeaEditor(
            idea = idea,
            onDismiss = { editing = null },
            onSave = { draft ->
                val accepted = onSave(draft) != null
                if (accepted) editing = null
                accepted
            },
            existingIdeas = ideas,
            onDelete = {
                editing = null
                deletePending = idea
            },
        )
    }

    deletePending?.let { idea ->
        AlertDialog(
            onDismissRequest = { deletePending = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Move to Recently Deleted?", color = ProjectorIvory) },
            text = { Text("You can restore this idea later. Its original recording and linked project are kept.", color = MutedText) },
            confirmButton = {
                Button(
                    onClick = { onDelete(idea.id); deletePending = null },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("MOVE IDEA") }
            },
            dismissButton = { TextButton(onClick = { deletePending = null }) { Text("CANCEL") } },
        )
    }

    converting?.let { idea ->
        V09ConvertIdeaDialog(
            idea = idea,
            onDismiss = { converting = null; conversionFailed = false },
            conversionFailed = conversionFailed,
            onConvert = { platform, format, due -> onConvert(idea.id, platform, format, due) },
            onConvertDurably = onConvertDurably,
            onConverted = { converting = null; conversionFailed = false },
        )
    }
}

@Composable
private fun V162DeletedIdeaCard(idea: CreatorIdea, onRestore: () -> Unit) {
    Surface(
        color = CinemaSurfaceRaised,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(idea.title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(buildString {
                append(IdeaVaultLabels.status(idea.status))
                if (idea.hasOriginalRecording) append(" · Voice saved")
                if (idea.projectTaskId.isNotBlank()) append(" · Project linked")
            }, color = MutedText, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) { Text("RESTORE IDEA") }
        }
    }
}

@Composable
private fun V09IdeaCard(
    idea: CreatorIdea,
    isOpportunity: Boolean,
    onEdit: () -> Unit,
    onConvert: () -> Unit,
    onArchive: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(17.dp),
        color = if (isOpportunity) MutedGold.copy(alpha = .07f) else CinemaSurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOpportunity) MutedGold.copy(alpha = .62f) else CinemaLine),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .08f), border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = .22f))) {
                    Text(IdeaVaultLabels.category(idea.category), color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(IdeaVaultLabels.status(idea.status).uppercase(), color = MutedText, fontSize = 7.8.sp, fontWeight = FontWeight.Bold)
                if (idea.captureType == IdeaCaptureType.VOICE) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(100.dp), color = RecRed.copy(alpha = .12f)) {
                        Text("VOICE", color = RecRed, fontSize = 7.2.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (isOpportunity) {
                    Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .12f)) {
                        Text("⚡ MOMENTUM", color = MutedGold, fontSize = 7.2.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(idea.title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (idea.topic.isNotBlank()) Text(idea.topic, color = MutedGold, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${idea.platformHint} · ${idea.formatHint}", color = MutedText, fontSize = 8.8.sp)
                Spacer(Modifier.weight(1f))
                if (idea.status != IdeaStatus.CONVERTED && idea.status != IdeaStatus.ARCHIVED) {
                    TextButton(onClick = onConvert) {
                        Icon(Icons.Outlined.RocketLaunch, null, tint = RecRed, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("MAKE A PROJECT", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (idea.status != IdeaStatus.ARCHIVED) {
                    IconButton(onClick = onArchive, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Archive, "Archive idea", tint = MutedText, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun V09NewIdeaCleanEditor(
    onDismiss: () -> Unit,
    onSave: (CreatorIdea) -> Boolean,
    existingIdeas: List<CreatorIdea>,
) {
    val context = LocalContext.current
    val creatorProfile = remember { CreatorOsSettingsStore(context.applicationContext).snapshot().creatorProfile }
    val visibleCategories = remember(creatorProfile) { IdeaVaultLabels.categoriesFor(creatorProfile) }
    val defaultPlatform = remember(creatorProfile) { CreatorPlatformRegistry.primaryPlatform(creatorProfile) }

    var title by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(visibleCategories.firstOrNull() ?: IdeaCategory.CINEMATIC_ANALYSIS) }
    var status by rememberSaveable { mutableStateOf(IdeaStatus.INBOX) }
    var potential by rememberSaveable { mutableStateOf(IdeaPotential.MEDIUM) }
    var platform by rememberSaveable { mutableStateOf(defaultPlatform) }
    var format by rememberSaveable { mutableStateOf(CreatorPlatformRegistry.defaultFormat(defaultPlatform)) }
    var notes by rememberSaveable { mutableStateOf("") }
    var reminderAtMillis by rememberSaveable { mutableLongStateOf(0L) }
    var reminderCadence by rememberSaveable { mutableStateOf(IdeaReminderCadence.ONCE) }
    var voiceRecording by remember { mutableStateOf<VoiceIdeaRecording?>(null) }
    var showRecorder by rememberSaveable { mutableStateOf(false) }
    var showDictation by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    var showReminder by rememberSaveable { mutableStateOf(false) }
    var showOrganize by rememberSaveable { mutableStateOf(false) }

    val platformOptions = remember(creatorProfile) {
        CreatorPlatformRegistry.orderedSelected(creatorProfile)
    }
    val formats = v09Formats(platform)
    LaunchedEffect(platform) {
        if (format !in formats) format = formats.firstOrNull().orEmpty()
    }

    fun discardRecording() {
        voiceRecording?.localPath?.takeIf { it.isNotBlank() }?.let { path -> runCatching { File(path).delete() } }
        voiceRecording = null
    }

    fun dismiss() {
        discardRecording()
        onDismiss()
    }

    Dialog(
        onDismissRequest = ::dismiss,
        // New Idea is its own dialog, not the V216 existing-idea editor.
        // Without edge-to-edge window ownership the decor can cover the Save
        // footer on phones with gesture navigation (owner screenshot).
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(Modifier.fillMaxSize().testTag("v2203-new-idea-window"), color = CinemaBlack) {
            Column(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("NEW IDEA", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                        Text("Save your idea", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("Write or record an idea. Add details if you like.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
                    }
                    IconButton(onClick = ::dismiss) {
                        Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory)
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = CinemaSurfaceRaised,
                        border = BorderStroke(1.dp, RecRed.copy(alpha = .45f)),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("YOUR IDEA", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
                            Spacer(Modifier.height(7.dp))
                            OutlinedTextField(
                                title,
                                { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("What's your idea?") },
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(16.dp),
                            )
                            IdeaVaultGuidance.similarTitles(title, existingIdeas).firstOrNull()?.let { duplicate ->
                                Text("Similar saved idea: ${duplicate.title}. You can still save this.", color = MutedGold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { showRecorder = !showRecorder; if (showRecorder) showDictation = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(17.dp),
                            color = if (showRecorder || voiceRecording != null) RecRed.copy(alpha = .13f) else CinemaSurface,
                            border = BorderStroke(1.dp, if (showRecorder || voiceRecording != null) RecRed.copy(alpha = .5f) else CinemaLine),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Mic, null, tint = RecRed, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(7.dp))
                                Column {
                                    Text("RECORD", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                    Text(if (voiceRecording != null) "Recording ready" else "Original audio", color = MutedText, fontSize = 7.8.sp)
                                }
                            }
                        }
                        Surface(
                            onClick = { showDictation = !showDictation; if (showDictation) showRecorder = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(17.dp),
                            color = if (showDictation) MutedGold.copy(alpha = .10f) else CinemaSurface,
                            border = BorderStroke(1.dp, if (showDictation) MutedGold.copy(alpha = .45f) else CinemaLine),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Mic, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(7.dp))
                                Column {
                                    Text("DICTATE", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                    Text("Speak to write an idea", color = MutedText, fontSize = 7.8.sp)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(showRecorder) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            VoiceIdeaRecorderInput(
                                recording = voiceRecording,
                                onRecordingChanged = { voiceRecording = it },
                            )
                        }
                    }

                    AnimatedVisibility(showDictation) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            V117VoiceIdeaInput(
                                onTranscript = { transcript ->
                                    val clean = transcript.trim()
                                    if (clean.isNotBlank()) {
                                        title = if (title.isBlank()) clean else title.trimEnd() + " " + clean
                                    }
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        onClick = { showNotes = !showNotes },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Notes, null, tint = MutedGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("NOTES", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                Text(if (notes.isBlank()) "Optional context, references or details" else "Notes added", color = MutedText, fontSize = 7.8.sp)
                            }
                            Icon(if (showNotes) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MutedText)
                        }
                    }
                    AnimatedVisibility(showNotes) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                notes,
                                { notes = it },
                                Modifier.fillMaxWidth().heightIn(min = 105.dp),
                                label = { Text("Notes") },
                                placeholder = { Text("Add any notes you want to keep…") },
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { showReminder = !showReminder },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Alarm, null, tint = MutedGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("REMINDER", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                Text(if (reminderAtMillis > 0L) "Reminder configured" else "Optional — decide later when to revisit", color = MutedText, fontSize = 7.8.sp)
                            }
                            Icon(if (showReminder) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MutedText)
                        }
                    }
                    AnimatedVisibility(showReminder) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            V144IdeaReminderPicker(
                                reminderAtMillis = reminderAtMillis,
                                cadence = reminderCadence,
                                enabled = status != IdeaStatus.CONVERTED && status != IdeaStatus.ARCHIVED,
                                onChange = { at, cadence ->
                                    reminderAtMillis = at
                                    reminderCadence = cadence
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { showOrganize = !showOrganize },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Tune, null, tint = MutedGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ORGANIZE", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                Text("Topic, status and where to post", color = MutedText, fontSize = 7.8.sp)
                            }
                            Icon(if (showOrganize) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MutedText)
                        }
                    }

                    AnimatedVisibility(showOrganize) {
                        Column(Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                            OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Topic · optional") })
                            Spacer(Modifier.height(10.dp))
                            V09VaultLabel("CATEGORY")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                visibleCategories.forEach { value ->
                                    FilterChip(category == value, { category = value }, { Text(IdeaVaultLabels.category(value), fontSize = 9.sp) })
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            V09VaultLabel("STATUS")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(IdeaStatus.INBOX, IdeaStatus.WORTH_EXPLORING, IdeaStatus.RESEARCHING, IdeaStatus.READY_TO_PRODUCE, IdeaStatus.ARCHIVED).forEach { value ->
                                    FilterChip(status == value, { status = value }, { Text(IdeaVaultLabels.status(value), fontSize = 9.sp) })
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            V09VaultLabel("POTENTIAL")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IdeaPotential.entries.forEach { value ->
                                    FilterChip(potential == value, { potential = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) })
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            V09VaultLabel("LIKELY PLATFORM")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                platformOptions.forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 9.sp) }) }
                            }
                            Spacer(Modifier.height(10.dp))
                            V09VaultLabel("LIKELY FORMAT")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                formats.forEach { value -> FilterChip(format == value, { format = value }, { Text(value, fontSize = 9.sp) }) }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // Always keep the primary action outside the scrolling body.
                // safeDrawing above reserves status/navigation system bars,
                // imePadding reserves the soft keyboard when open.
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("v2203-new-idea-footer"),
                    color = CinemaSurfaceRaised,
                    tonalElevation = 8.dp,
                ) {
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val finalizedRecording = voiceRecording
                            val newVoiceIdea = finalizedRecording != null
                            val accepted = onSave(
                                CreatorIdea(
                                    id = UUID.randomUUID().toString(),
                                    title = title.trim().ifBlank { if (newVoiceIdea) v09DefaultVoiceIdeaTitle() else "" },
                                    topic = topic.trim(),
                                    category = category,
                                    status = status,
                                    potential = potential,
                                    platformHint = platform,
                                    formatHint = format,
                                    notes = notes.trim(),
                                    reminderAtMillis = reminderAtMillis,
                                    reminderCadence = reminderCadence,
                                    captureType = if (newVoiceIdea) IdeaCaptureType.VOICE else IdeaCaptureType.TEXT,
                                    audioLocalPath = finalizedRecording?.localPath.orEmpty(),
                                    audioDurationMillis = finalizedRecording?.durationMillis ?: 0L,
                                    audioMimeType = finalizedRecording?.mimeType.orEmpty(),
                                    audioSyncState = if (newVoiceIdea) IdeaAudioSyncState.LOCAL_ONLY else IdeaAudioSyncState.NONE,
                                    createdAtMillis = now,
                                    updatedAtMillis = now,
                                )
                            )
                            if (accepted) voiceRecording = null
                        },
                        enabled = title.isNotBlank() || voiceRecording != null,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(52.dp)
                            .testTag("v2203-new-idea-save"),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text(if (voiceRecording != null) "SAVE VOICE IDEA" else "SAVE IDEA", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun V09IdeaEditor(
    idea: CreatorIdea,
    onDismiss: () -> Unit,
    onSave: (CreatorIdea) -> Boolean,
    existingIdeas: List<CreatorIdea>,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    if (idea.id.isBlank()) {
        V09NewIdeaCleanEditor(
            onDismiss = onDismiss,
            onSave = onSave,
            existingIdeas = existingIdeas,
        )
        return
    }
    val creatorProfile = remember { CreatorOsSettingsStore(context.applicationContext).snapshot().creatorProfile }
    val visibleCategories = remember(creatorProfile) { IdeaVaultLabels.categoriesFor(creatorProfile) }
    val defaultPlatform = remember(creatorProfile) { CreatorPlatformRegistry.primaryPlatform(creatorProfile) }
    var title by remember(idea.id) { mutableStateOf(idea.title) }
    var topic by remember(idea.id) { mutableStateOf(idea.topic) }
    var category by remember(idea.id, visibleCategories) { mutableStateOf(if (idea.id.isBlank() && idea.category !in visibleCategories) visibleCategories.first() else idea.category) }
    var status by remember(idea.id) { mutableStateOf(idea.status) }
    var potential by remember(idea.id) { mutableStateOf(idea.potential) }
    var platform by remember(idea.id) { mutableStateOf(if (idea.id.isBlank()) defaultPlatform else idea.platformHint.ifBlank { defaultPlatform }) }
    var format by remember(idea.id) { mutableStateOf(if (idea.id.isBlank()) CreatorPlatformRegistry.defaultFormat(defaultPlatform) else idea.formatHint.ifBlank { CreatorPlatformRegistry.defaultFormat(platform) }) }
    val platformOptions = remember(creatorProfile, idea.id, idea.platformHint) {
        CreatorPlatformRegistry.orderedSelected(creatorProfile, include = if (idea.id.isBlank()) null else idea.platformHint)
    }
    var notes by remember(idea.id) { mutableStateOf(idea.notes) }
    var reminderAtMillis by remember(idea.id) { mutableLongStateOf(idea.reminderAtMillis) }
    var reminderCadence by remember(idea.id) { mutableStateOf(idea.reminderCadence) }
    var showOrganize by rememberSaveable(idea.id) { mutableStateOf(false) }
    var voiceRecording by remember(idea.id) { mutableStateOf<VoiceIdeaRecording?>(null) }
    val formats = v09Formats(platform)
    LaunchedEffect(platform) { if (format !in formats) format = formats.first() }

    fun discardNewRecording() {
        voiceRecording?.localPath?.takeIf { it.isNotBlank() }?.let { path -> runCatching { File(path).delete() } }
        voiceRecording = null
    }

    fun dismissEditor() {
        discardNewRecording()
        onDismiss()
    }

    // V216: an AlertDialog footer is not reliable on compact devices when
    // a tall notes/editor body and the on-screen keyboard compete for height.
    // Keep the existing save/voice/reminder model, but make the footer a fixed
    // sibling of a weighted, scrollable editor, inside safe drawing + IME insets.
    Dialog(
        onDismissRequest = ::dismissEditor,
        // A full-window Compose Dialog must opt out of platform decor-fitting
        // so the soft keyboard resizes the dialog on Android 11. Otherwise its
        // footer can be reported inside root bounds yet sit behind the IME.
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(Modifier.fillMaxSize().testTag("v216-edit-editor"), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("EDIT IDEA", modifier = Modifier.weight(1f), color = ProjectorIvory,
                        fontSize = 22.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = ::dismissEditor) {
                        Icon(Icons.Outlined.Close, "Close edit idea", tint = ProjectorIvory)
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .testTag("v216-edit-content"),
                ) {
                OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth().testTag("v216-edit-title"), singleLine = true, label = { Text("Idea title") })
                IdeaVaultGuidance.similarTitles(title, existingIdeas, excludingId = idea.id).firstOrNull()?.let { duplicate ->
                    Text("Similar saved idea: ${duplicate.title}. You can still save this.", color = MutedGold, fontSize = 11.sp)
                }

                if (idea.id.isBlank()) {
                    Spacer(Modifier.height(10.dp))
                    VoiceIdeaRecorderInput(
                        recording = voiceRecording,
                        onRecordingChanged = { voiceRecording = it },
                    )
                } else if (idea.hasOriginalRecording) {
                    Spacer(Modifier.height(10.dp))
                    VoiceIdeaPlaybackControl(idea = idea)
                }

                Spacer(Modifier.height(9.dp))
                OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 95.dp).testTag("v216-edit-notes"), label = { Text("Notes · optional") })
                Spacer(Modifier.height(8.dp))
                V117VoiceIdeaInput(
                    onTranscript = { transcript ->
                        val clean = transcript.trim()
                        if (clean.isNotBlank()) {
                            notes = if (notes.isBlank()) clean else notes.trimEnd() + System.lineSeparator() + clean
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                V144IdeaReminderPicker(
                    reminderAtMillis = reminderAtMillis,
                    cadence = reminderCadence,
                    enabled = status != IdeaStatus.CONVERTED && status != IdeaStatus.ARCHIVED,
                    onChange = { atMillis, cadence ->
                        reminderAtMillis = atMillis
                        reminderCadence = cadence
                    },
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { showOrganize = !showOrganize },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CinemaSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ORGANIZE IDEA · OPTIONAL", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                            Text("Topic, status and publishing hints", color = MutedText, fontSize = 8.5.sp)
                        }
                        Icon(if (showOrganize) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MutedGold)
                    }
                }
                AnimatedVisibility(visible = showOrganize) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(topic, { topic = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Topic · optional") })
                        Spacer(Modifier.height(12.dp)); V09VaultLabel("CATEGORY")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            visibleCategories.forEach { value ->
                                FilterChip(category == value, { category = value }, { Text(IdeaVaultLabels.category(value), fontSize = 10.sp) })
                            }
                        }
                        Spacer(Modifier.height(12.dp)); V09VaultLabel("STATUS")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(IdeaStatus.INBOX, IdeaStatus.WORTH_EXPLORING, IdeaStatus.RESEARCHING, IdeaStatus.READY_TO_PRODUCE, IdeaStatus.ARCHIVED).forEach { value ->
                                FilterChip(status == value, { status = value }, { Text(IdeaVaultLabels.status(value), fontSize = 10.sp) })
                            }
                        }
                        Spacer(Modifier.height(12.dp)); V09VaultLabel("POTENTIAL")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IdeaPotential.entries.forEach { value ->
                                FilterChip(potential == value, { potential = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) })
                            }
                        }
                        Spacer(Modifier.height(12.dp)); V09VaultLabel("LIKELY PLATFORM")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            platformOptions.forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 10.sp) }) }
                        }
                        Spacer(Modifier.height(10.dp)); V09VaultLabel("LIKELY FORMAT")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            formats.forEach { value -> FilterChip(format == value, { format = value }, { Text(value, fontSize = 10.sp) }) }
                        }
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("MOVE TO RECENTLY DELETED", color = RecRed) }
                }
                }
                // Save/Cancel remain visible above system navigation and IME,
                // regardless of optional organizing sections or long Telugu notes.
                Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = ::dismissEditor,
                            modifier = Modifier.weight(1f).height(52.dp).testTag("v216-edit-cancel"),
                        ) { Text("CANCEL", color = ProjectorIvory) }
                        Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val finalizedRecording = voiceRecording
                    val newVoiceIdea = idea.id.isBlank() && finalizedRecording != null
                    val accepted = onSave(
                        idea.copy(
                            id = idea.id.ifBlank { UUID.randomUUID().toString() },
                            title = title.trim().ifBlank { if (newVoiceIdea) v09DefaultVoiceIdeaTitle() else "" },
                            topic = topic.trim(),
                            category = category,
                            status = status,
                            potential = potential,
                            platformHint = platform,
                            formatHint = format,
                            notes = notes.trim(),
                            reminderAtMillis = reminderAtMillis,
                            reminderCadence = reminderCadence,
                            captureType = if (newVoiceIdea) IdeaCaptureType.VOICE else idea.captureType,
                            audioLocalPath = finalizedRecording?.localPath ?: idea.audioLocalPath,
                            audioDurationMillis = finalizedRecording?.durationMillis ?: idea.audioDurationMillis,
                            audioMimeType = finalizedRecording?.mimeType ?: idea.audioMimeType,
                            audioSyncState = if (newVoiceIdea) IdeaAudioSyncState.LOCAL_ONLY else idea.audioSyncState,
                            createdAtMillis = idea.createdAtMillis.takeIf { it > 0L } ?: now,
                            updatedAtMillis = now,
                        )
                    )
                    // If rejected, retain the unsaved original recording and editor draft.
                    if (accepted) voiceRecording = null
                },
                enabled = title.isNotBlank() || voiceRecording != null,
                modifier = Modifier.weight(1.55f).height(52.dp).testTag("v216-edit-save"),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(15.dp),
                        ) {
                            Text(if (voiceRecording != null) "SAVE VOICE IDEA" else "SAVE",
                                fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V09ConvertIdeaDialog(
    idea: CreatorIdea,
    onDismiss: () -> Unit,
    conversionFailed: Boolean,
    onConvert: (String, String, Long) -> String?,
    onConvertDurably: (suspend (IdeaProjectConversionRequest) -> String?)? = null,
    onConverted: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generation = remember { CreatorDataGate.generation(context) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var plan by remember { mutableStateOf(IdeaProjectConversion(context).rememberedPlan()) }
    val creatorProfile = remember { CreatorOsSettingsStore(context.applicationContext).snapshot().creatorProfile }
    val defaultPlatform = remember(creatorProfile) { CreatorPlatformRegistry.primaryPlatform(creatorProfile) }
    var platform by remember { mutableStateOf(idea.platformHint.ifBlank { defaultPlatform }) }
    var format by remember { mutableStateOf(idea.formatHint.ifBlank { CreatorPlatformRegistry.defaultFormat(platform) }) }
    val platformOptions = remember(creatorProfile, idea.platformHint) {
        CreatorPlatformRegistry.orderedSelected(creatorProfile, include = idea.platformHint.takeIf { it.isNotBlank() })
    }
    val formats = v09Formats(platform)
    LaunchedEffect(platform) { if (format !in formats) format = formats.first() }

    val initial = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var year by remember { mutableIntStateOf(initial.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(initial.get(Calendar.MONTH)) }
    var day by remember { mutableIntStateOf(initial.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(initial.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(initial.get(Calendar.MINUTE)) }

    fun pickDate() {
        DatePickerDialog(context, { _, y, m, d -> year = y; month = m; day = d }, year, month, day).show()
    }
    fun pickTime() {
        TimePickerDialog(context, { _, h, min -> hour = h; minute = min }, hour, minute, false).show()
    }

    val due = remember(year, month, day, hour, minute) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val display = remember(due) { SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(due) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = CinemaSurfaceRaised,
        title = { Text("Turn into Project", color = ProjectorIvory, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(idea.title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(13.dp)); V09VaultLabel("PLATFORM")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    platformOptions.forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 12.sp) }, enabled = !saving) }
                }
                Spacer(Modifier.height(10.dp)); V09VaultLabel("FORMAT")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    formats.forEach { value -> FilterChip(format == value, { format = value }, { Text(value, fontSize = 12.sp) }, enabled = !saving) }
                }
                Spacer(Modifier.height(12.dp)); V09VaultLabel("PUBLISH DEADLINE")
                Text(display, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !saving, onClick = { pickDate() }, modifier = Modifier.weight(1f)) { Text("DATE", fontSize = 10.sp) }
                    OutlinedButton(enabled = !saving, onClick = { pickTime() }, modifier = Modifier.weight(1f)) { Text("TIME", fontSize = 10.sp) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Project reminders", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IdeaProjectConversion.supportedPlans.forEach { option ->
                        FilterChip(selected = plan == option, onClick = { plan = option }, enabled = !saving,
                            modifier = Modifier.testTag("conversion-plan-${option.name}"),
                            label = { Text(ProjectPulseEngine.planLabel(option), fontSize = 12.sp) })
                    }
                }
                Text(when (plan) {
                    ProjectAttentionPlan.OFF -> "No automatic reminders. You can add them in the project."
                    ProjectAttentionPlan.LIGHT -> "Quiet notifications with longer gaps between check-ins."
                    ProjectAttentionPlan.GUIDED -> "Stage check-ins; voice or alarms may be used near the deadline."
                    else -> "Closer check-ins with voice or alarms near the deadline."
                }, color = MutedText, fontSize = 13.sp, modifier = Modifier.testTag("conversion-plan-description"))
                Spacer(Modifier.height(8.dp))
                Text("Your idea and original voice note stay in Idea Vault. We'll open the project after it is saved.", color = MutedText, fontSize = 13.sp)
                if (error != null || conversionFailed) {
                    Spacer(Modifier.height(8.dp))
                    Text(error ?: "Could not finish creating the project. Your idea was kept. Try again.",
                        color = RecRed, fontSize = 13.sp, modifier = Modifier.testTag("conversion-error"))
                }
            }
        },
        confirmButton = {
            Button(enabled = !saving, modifier = Modifier.testTag("conversion-create"), onClick = {
                if (!saving) {
                    saving = true; error = null
                    scope.launch {
                        try {
                            val id = if (onConvertDurably != null) onConvertDurably(
                                IdeaProjectConversionRequest(idea.id, platform, format, due, plan, generation))
                            else onConvert(platform, format, due)
                            if (id != null) onConverted()
                            else error = "Could not finish creating the project. Your idea was kept. Try again."
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Throwable) { error = failure.message ?: "Could not save. Your idea was kept. Try again." }
                        finally { saving = false }
                    }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                Icon(Icons.Outlined.RocketLaunch, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp)); Text(if (saving) "SAVING…" else "CREATE PROJECT")
            }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss, modifier = Modifier.testTag("conversion-cancel")) { Text("CANCEL", color = MutedText) } },
    )
}

@Composable
private fun V09VaultLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 8.2.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
}

private fun v09DefaultVoiceIdeaTitle(): String =
    "Voice idea • ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(System.currentTimeMillis())}"

private fun v09Formats(platform: String): List<String> = CreatorPlatformRegistry.formats(platform)