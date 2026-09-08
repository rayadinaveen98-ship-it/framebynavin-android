package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.outlined.Lightbulb
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
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.YouTubeAnalyticsStore
import com.framebynavin.app.youtube.YouTubeOpportunityEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
internal fun V09IdeaVaultScreen(
    ideas: List<CreatorIdea>,
    onClose: (() -> Unit)?,
    onSave: (CreatorIdea) -> String?,
    onDelete: (String) -> Unit,
    onArchive: (String) -> Unit,
    onConvert: (String, String, String, Long) -> String?,
) {
    val context = LocalContext.current
    val creatorProfile = remember { CreatorOsSettingsStore(context.applicationContext).snapshot().creatorProfile }
    val visibleCategories = remember(creatorProfile) { IdeaVaultLabels.categoriesFor(creatorProfile) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<IdeaStatus?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<IdeaCategory?>(null) }
    var editing by remember { mutableStateOf<CreatorIdea?>(null) }
    var creating by remember { mutableStateOf(false) }
    var converting by remember { mutableStateOf<CreatorIdea?>(null) }

    val opportunityReport = YouTubeAnalyticsStore.latest24HourReport
    val opportunityAlerts by remember(opportunityReport) {
        derivedStateOf { YouTubeOpportunityEngine.build(opportunityReport, ideas) }
    }
    val opportunityIdeaIds by remember { derivedStateOf { opportunityAlerts.mapNotNull { it.ideaId }.toSet() } }
    val opportunityMatch by remember { derivedStateOf { opportunityAlerts.firstOrNull { it.ideaId != null } } }
    val readyCount by remember { derivedStateOf { ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE } } }

    val filtered by remember {
        derivedStateOf {
            ideas.filter { idea ->
                val textMatch = query.isBlank() || listOf(idea.title, idea.topic, idea.notes)
                    .any { it.contains(query, ignoreCase = true) }
                val statusMatch = statusFilter == null || idea.status == statusFilter
                val categoryMatch = categoryFilter == null || idea.category == categoryFilter
                textMatch && statusMatch && categoryMatch
            }.sortedWith(
                compareByDescending<CreatorIdea> { it.id in opportunityIdeaIds }
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
                    Text("IDEA VAULT", color = MutedGold, fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                    Text("Capture now. Produce later.", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Outlined.Add, "New idea", tint = ProjectorIvory)
                }
            }

            opportunityMatch?.let { match ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = Color(0xFF1A1710),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = .45f)),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Text("MATCHED IDEA", color = MutedGold, fontSize = 7.8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(3.dp))
                        Text(match.ideaTitle ?: match.title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("This idea matches a topic gaining attention on your channel, so I moved it to the top.", color = MutedText, fontSize = 8.8.sp, lineHeight = 12.5.sp)
                    }
                }
                Spacer(Modifier.height(5.dp))
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("Search ideas, topics or notes…") },
            )

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All", fontSize = 10.sp) })
                listOf(IdeaStatus.INBOX, IdeaStatus.WORTH_EXPLORING, IdeaStatus.RESEARCHING, IdeaStatus.READY_TO_PRODUCE, IdeaStatus.CONVERTED).forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { Text(IdeaVaultLabels.status(status), fontSize = 10.sp) },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("All topics", fontSize = 10.sp) })
                visibleCategories.forEach { category ->
                    FilterChip(
                        selected = categoryFilter == category,
                        onClick = { categoryFilter = category },
                        label = { Text(IdeaVaultLabels.category(category), fontSize = 10.sp) },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${filtered.size} IDEA${if (filtered.size == 1) "" else "S"}", color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("$readyCount READY", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }

            if (filtered.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = MutedGold, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(9.dp))
                    Text(if (ideas.isEmpty()) "Your vault is empty" else "No ideas match this filter", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    Text("Save rough thoughts without turning everything into a deadline.", color = MutedText, fontSize = 10.sp)
                    if (ideas.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { creating = true }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("ADD FIRST IDEA") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 124.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { idea ->
                        V09IdeaCard(
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

    if (creating) {
        V09IdeaEditor(
            idea = CreatorIdea(id = "", title = ""),
            onDismiss = { creating = false },
            onSave = { onSave(it); creating = false },
            onDelete = null,
        )
    }

    editing?.let { idea ->
        V09IdeaEditor(
            idea = idea,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(idea.id); editing = null },
        )
    }

    converting?.let { idea ->
        V09ConvertIdeaDialog(
            idea = idea,
            onDismiss = { converting = null },
            onConvert = { platform, format, due ->
                onConvert(idea.id, platform, format, due)
                converting = null
            },
        )
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
        color = if (isOpportunity) Color(0xFF1A1712) else CinemaSurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOpportunity) MutedGold.copy(alpha = .62f) else CinemaLine),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF14110D), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF342C21))) {
                    Text(IdeaVaultLabels.category(idea.category), color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(IdeaVaultLabels.status(idea.status).uppercase(), color = MutedText, fontSize = 7.8.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (isOpportunity) {
                    Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .12f)) {
                        Text("MATCHED", color = MutedGold, fontSize = 7.2.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(idea.potential.name, color = if (idea.potential == IdeaPotential.HIGH) RecRed else MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(idea.title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (idea.topic.isNotBlank()) Text(idea.topic, color = MutedGold, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (idea.notes.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(idea.notes, color = MutedText, fontSize = 9.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${idea.platformHint} · ${idea.formatHint}", color = MutedText, fontSize = 8.8.sp)
                Spacer(Modifier.weight(1f))
                if (idea.status != IdeaStatus.CONVERTED && idea.status != IdeaStatus.ARCHIVED) {
                    TextButton(onClick = onConvert) {
                        Icon(Icons.Outlined.RocketLaunch, null, tint = RecRed, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("TURN INTO PROJECT", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (idea.status != IdeaStatus.ARCHIVED) {
                    IconButton(onClick = onArchive, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Archive, "Archive idea", tint = MutedText, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V09IdeaEditor(
    idea: CreatorIdea,
    onDismiss: () -> Unit,
    onSave: (CreatorIdea) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
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
    val formats = v09Formats(platform)
    LaunchedEffect(platform) { if (format !in formats) format = formats.first() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text(if (idea.id.isBlank()) "New Idea" else "Edit Idea", color = ProjectorIvory, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Idea title") })
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(topic, { topic = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Topic · optional") })
                Spacer(Modifier.height(12.dp)); V09VaultLabel("TOPIC")
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
                    IdeaPotential.entries.forEach { value -> FilterChip(potential == value, { potential = value }, { Text(value.name, fontSize = 10.sp) }) }
                }
                Spacer(Modifier.height(12.dp)); V09VaultLabel("LIKELY PLATFORM")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    platformOptions.forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 10.sp) }) }
                }
                Spacer(Modifier.height(10.dp)); V09VaultLabel("LIKELY FORMAT")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    formats.forEach { value -> FilterChip(format == value, { format = value }, { Text(value, fontSize = 10.sp) }) }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 95.dp), label = { Text("Notes") })
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("DELETE IDEA", color = RecRed) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        idea.copy(
                            id = idea.id.ifBlank { UUID.randomUUID().toString() },
                            title = title.trim(),
                            topic = topic.trim(),
                            category = category,
                            status = status,
                            potential = potential,
                            platformHint = platform,
                            formatHint = format,
                            notes = notes.trim(),
                            createdAtMillis = idea.createdAtMillis.takeIf { it > 0L } ?: now,
                            updatedAtMillis = now,
                        )
                    )
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V09ConvertIdeaDialog(
    idea: CreatorIdea,
    onDismiss: () -> Unit,
    onConvert: (String, String, Long) -> Unit,
) {
    val context = LocalContext.current
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
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text("Turn into Project", color = ProjectorIvory, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(idea.title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(13.dp)); V09VaultLabel("PLATFORM")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    platformOptions.forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 10.sp) }) }
                }
                Spacer(Modifier.height(10.dp)); V09VaultLabel("FORMAT")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    formats.forEach { value -> FilterChip(format == value, { format = value }, { Text(value, fontSize = 10.sp) }) }
                }
                Spacer(Modifier.height(12.dp)); V09VaultLabel("PUBLISH DEADLINE")
                Text(display, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickDate() }, modifier = Modifier.weight(1f)) { Text("DATE", fontSize = 10.sp) }
                    OutlinedButton(onClick = { pickTime() }, modifier = Modifier.weight(1f)) { Text("TIME", fontSize = 10.sp) }
                }
                Spacer(Modifier.height(8.dp))
                Text("The project will open in Create with the right workflow and reminder timing for its format.", color = MutedText, fontSize = 9.3.sp)
            }
        },
        confirmButton = {
            Button(onClick = { onConvert(platform, format, due) }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                Icon(Icons.Outlined.RocketLaunch, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("CREATE PROJECT")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) } },
    )
}

@Composable
private fun V09VaultLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 8.2.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
}

private fun v09Formats(platform: String): List<String> = CreatorPlatformRegistry.formats(platform)
