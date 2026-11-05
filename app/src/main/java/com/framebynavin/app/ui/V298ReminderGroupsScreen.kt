package com.framebynavin.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.V298ReminderGroup
import com.framebynavin.app.personal.V298ReminderGroupOptions
import com.framebynavin.app.personal.V298ReminderGroupCodec
import com.framebynavin.app.personal.V298ReminderGroupStore
import com.framebynavin.app.personal.V298ReminderGroupMergePolicy
import com.framebynavin.app.ui.theme.*
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun v298Accent(key: String): Color = when (key) {
    "GOLD" -> Color(0xFFC9A76F)
    "BLUE" -> Color(0xFF8FB9D9)
    "GREEN" -> Color(0xFF9EBDA0)
    "ROSE" -> Color(0xFFD3A7B9)
    else -> MutedText
}

private fun v298Symbol(key: String): String = when (key) {
    "HEART" -> "♡"
    "SUN" -> "☀"
    "BOOK" -> "▣"
    "STAR" -> "☆"
    "CHECK" -> "✓"
    else -> "○"
}

/**
 * Local-first collection of LINKS to existing Personal reminders.
 * No group action may install, reschedule or delete a Personal reminder.
 */
@Composable
internal fun V298ReminderGroupsScreen(
    onClose: () -> Unit,
    onEditReminder: (String) -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val groupsStore = remember(context) { V298ReminderGroupStore(context) }
    val reminderStore = remember(context) { PersonalReminderStore(context) }
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<V298ReminderGroup>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<PersonalReminder>>(emptyList()) }
    var groupsLoaded by remember { mutableStateOf(false) }
    var remindersLoaded by remember { mutableStateOf(false) }
    var readError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editVisible by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var iconKey by rememberSaveable { mutableStateOf("CIRCLE") }
    var colorKey by rememberSaveable { mutableStateOf("NEUTRAL") }
    var linkVisible by remember { mutableStateOf(false) }
    var deleteVisible by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<V298ReminderGroup>?>(null) }
    val selected = groups.firstOrNull { it.id == selectedId }
    val indexedReminders = reminders.associateBy { it.id }

    LaunchedEffect(groupsStore) {
        try {
            groupsStore.groupsFlow.collect {
                groups = it
                groupsLoaded = true
            }
        } catch (_: Exception) {
            groupsLoaded = true
            readError = "Saved groups could not be read; no data was changed."
        }
    }
    LaunchedEffect(reminderStore) {
        try {
            reminderStore.remindersFlow.collect {
                reminders = it
                remindersLoaded = true
            }
        } catch (_: Exception) {
            remindersLoaded = true
            readError = "Personal reminders could not be read; no links were changed."
        }
    }
    fun write(action: suspend () -> Unit) {
        if (busy || readError != null) return
        busy = true
        scope.launch {
            try { action(); actionError = null }
            catch (e: Exception) {
                actionError = e.message ?: "Couldn't save. Your saved reminders were not changed."
            } finally { busy = false }
        }
    }
    val exportFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) write {
            val raw = groupsStore.exportJson()
            withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Could not write the chosen file")
                stream.use { it.write(raw.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    val importFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && !busy && readError == null) {
            scope.launch {
                try {
                    val raw = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("Could not open chosen file")
                        input.use {
                            val bytes = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = it.read(buffer)
                                if (count < 0) break
                                require(bytes.size() + count <= 2_000_000) {
                                    "Reminder-group backup too large"
                                }
                                bytes.write(buffer, 0, count)
                            }
                            bytes.toString(Charsets.UTF_8.name())
                        }
                    }
                    pendingImport = V298ReminderGroupCodec.decode(raw)
                    actionError = null
                } catch (e: Exception) {
                    pendingImport = null
                    actionError = e.message ?: "Backup validation failed; nothing changed"
                }
            }
        }
    }

    fun openEditor(current: V298ReminderGroup?) {
        editId = current?.id
        editName = current?.name.orEmpty()
        iconKey = current?.iconKey ?: "CIRCLE"
        colorKey = current?.colorKey ?: "NEUTRAL"
        editVisible = true
    }
    val goBack: () -> Unit = {
        when {
            pendingImport != null -> pendingImport = null
            deleteVisible -> deleteVisible = false
            linkVisible -> linkVisible = false
            editVisible -> editVisible = false
            selectedId != null -> selectedId = null
            else -> onClose()
        }
    }
    BackHandler(onBack = goBack)
    Surface(color = CinemaSurfaceRaised, modifier = Modifier.fillMaxSize()
        .testTag("v298-groups-screen")) {
        Column(Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = goBack, modifier = Modifier.testTag("v298-back")) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory)
                }
                Column(Modifier.weight(1f)) {
                    Text("PERSONAL", color = MutedGold, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold)
                    Text(selected?.name ?: "Reminder Groups", color = ProjectorIvory,
                        fontSize = 19.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            (readError ?: actionError)?.let {
                Text(it, color = RecRed, fontSize = 12.sp,
                    modifier = Modifier.testTag("v298-group-error"))
            }
            if (!groupsLoaded || !remindersLoaded) {
                CircularProgressIndicator()
            } else if (selected == null) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("One reminder can live in more than one group.",
                        color = MutedText, fontSize = 13.sp)
                    Button(
                        onClick = { openEditor(null) },
                        enabled = !busy && readError == null,
                        modifier = Modifier.fillMaxWidth().testTag("v298-create-open"),
                    ) { Text("NEW REMINDER GROUP") }
                    if (groups.isEmpty()) {
                        Text("No groups yet.",
                            color = MutedText, fontSize = 13.sp,
                            modifier = Modifier.testTag("v298-empty"))
                    }
                    groups.forEach { group ->
                        Surface(
                            onClick = { selectedId = group.id },
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("v298-group-card"),
                        ) {
                            Row(Modifier.padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(v298Symbol(group.iconKey), fontSize = 23.sp,
                                    color = v298Accent(group.colorKey))
                                Column(Modifier.weight(1f)) {
                                    Text(group.name, color = ProjectorIvory,
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${group.reminderIds.size} linked reminders",
                                        color = MutedText, fontSize = 12.sp)
                                }
                                Icon(Icons.Outlined.ChevronRight, "Open group",
                                    tint = MutedGold)
                            }
                        }
                    }
                    if (groups.isNotEmpty()) {
                        OutlinedButton(
                            enabled = !busy && readError == null,
                            onClick = { exportFile.launch("backlot-reminder-groups.json") },
                            modifier = Modifier.fillMaxWidth().testTag("v298-export"),
                        ) { Text("EXPORT MY GROUPS") }
                    }
                    OutlinedButton(
                        enabled = !busy && readError == null,
                        onClick = { importFile.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.fillMaxWidth().testTag("v298-import-open"),
                    ) { Text("IMPORT GROUP BACKUP") }
                    Text("Device-only groups · Not included in cloud sync. " +
                        "Exports contain links, not reminder records.",
                        color = MutedText, fontSize = 12.sp)
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(v298Symbol(selected.iconKey), fontSize = 25.sp,
                            color = v298Accent(selected.colorKey))
                        Column(Modifier.weight(1f)) {
                            Text("${selected.reminderIds.size} reminders", color = ProjectorIvory,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Linked to existing Personal reminders",
                                color = MutedText, fontSize = 11.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedButton(
                            onClick = { openEditor(selected) }, enabled = !busy && readError == null,
                            modifier = Modifier.weight(1f).testTag("v298-edit-group"),
                        ) { Text("EDIT GROUP") }
                        OutlinedButton(
                            onClick = { deleteVisible = true },
                            enabled = !busy && readError == null,
                            modifier = Modifier.weight(1f).testTag("v298-delete-open"),
                        ) { Text("DELETE GROUP") }
                    }
                    Button(
                        onClick = { linkVisible = true },
                        enabled = !busy && readError == null,
                        modifier = Modifier.fillMaxWidth().testTag("v298-link-open"),
                    ) { Text("LINK AN EXISTING REMINDER") }
                    if (selected.reminderIds.isEmpty()) {
                        Text("Nothing linked yet. Link any saved Personal reminder.",
                            color = MutedText, fontSize = 13.sp,
                            modifier = Modifier.testTag("v298-no-links"))
                    }
                    selected.reminderIds.forEach { id ->
                        val reminder = indexedReminders[id]
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("v298-linked-row"),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                            shape = RoundedCornerShape(13.dp),
                        ) {
                            Column(Modifier.padding(11.dp)) {
                                Text(reminder?.title ?: "Reminder no longer available",
                                    color = ProjectorIvory, fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium)
                                if (reminder != null) Text(
                                    if (reminder.paused) "Paused"
                                    else "Saved · ${reminder.repeat.name.replace('_', ' ')}",
                                    color = MutedText, fontSize = 11.sp,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (reminder != null) TextButton(onClick = {
                                        onEditReminder(id)
                                    }) { Text("EDIT REMINDER") }
                                    TextButton(
                                        enabled = !busy && readError == null,
                                        onClick = {
                                            val groupId = selected.id
                                            write {
                                                groupsStore.update(groupId) {
                                                    it.unlink(id, System.currentTimeMillis())
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("v298-unlink"),
                                    ) { Text("UNLINK") }
                                }
                            }
                        }
                    }
                    Text("Group changes never alter reminder schedules.",
                        color = MutedText, fontSize = 12.sp)
                }
            }
        }
    }

    if (editVisible) AlertDialog(
        onDismissRequest = { if (!busy) editVisible = false },
        title = { Text(if (editId == null) "New reminder group" else "Edit group") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it.take(80) },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("v298-group-name"),
                )
                Text("Icon", color = MutedText, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V298ReminderGroupOptions.iconKeys.forEach { key ->
                        FilterChip(selected = iconKey == key, onClick = { iconKey = key },
                            label = { Text(v298Symbol(key) + " " + key.lowercase(), fontSize = 11.sp) })
                    }
                }
                Text("Color", color = MutedText, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V298ReminderGroupOptions.colorKeys.forEach { key ->
                        FilterChip(selected = colorKey == key, onClick = { colorKey = key },
                            label = { Text(key.lowercase().replaceFirstChar { it.uppercase() },
                                color = v298Accent(key), fontSize = 11.sp) })
                    }
                }
                Text("Schedules are managed in Personal reminders.",
                    color = MutedText, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && readError == null && editName.isNotBlank(),
                onClick = {
                    val title = editName.trim()
                    val icon = iconKey
                    val color = colorKey
                    val id = editId
                    write {
                        if (id == null) {
                            val group = V298ReminderGroup(
                                name = title, iconKey = icon, colorKey = color,
                            ).checked()
                            groupsStore.create(group)
                            selectedId = group.id
                        } else {
                            groupsStore.update(id) { group ->
                                group.renamed(title, System.currentTimeMillis())
                                    .personalized(icon, color, System.currentTimeMillis())
                            }
                        }
                        editVisible = false
                    }
                },
                modifier = Modifier.testTag("v298-group-save"),
            ) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = { editVisible = false }, enabled = !busy) {
                Text("CANCEL")
            }
        },
    )

    if (linkVisible && selected != null) AlertDialog(
        onDismissRequest = { if (!busy) linkVisible = false },
        title = { Text("Link saved reminders") },
        text = {
            val candidates = reminders.filterNot { it.id in selected.reminderIds }
            Column(Modifier.heightIn(max = 370.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Choose an existing Personal reminder.",
                    color = MutedText, fontSize = 12.sp)
                if (candidates.isEmpty()) Text("No more saved reminders to link.",
                    color = MutedText, fontSize = 12.sp)
                candidates.forEach { reminder ->
                    TextButton(
                        enabled = !busy && readError == null,
                        onClick = {
                            val id = selected.id
                            val reminderId = reminder.id
                            write {
                                groupsStore.update(id) { group ->
                                    group.linkExisting(reminderId,
                                        reminders.mapTo(HashSet()) { it.id },
                                        System.currentTimeMillis())
                                }
                                linkVisible = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("v298-link-choice"),
                    ) { Text(reminder.title, maxLines = 2) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { linkVisible = false }, enabled = !busy) { Text("CLOSE") }
        },
    )

    if (deleteVisible && selected != null) AlertDialog(
        onDismissRequest = { if (!busy) deleteVisible = false },
        title = { Text("Delete this group?") },
        text = { Text("Only the group and its links are removed. Every Personal reminder, " +
            "schedule and Done history will remain untouched.") },
        confirmButton = {
            TextButton(
                enabled = !busy && readError == null,
                onClick = {
                    val id = selected.id
                    write {
                        groupsStore.deleteGroup(id)
                        selectedId = null
                        deleteVisible = false
                    }
                },
                modifier = Modifier.testTag("v298-delete-confirm"),
            ) { Text("DELETE GROUP") }
        },
        dismissButton = {
            TextButton(onClick = { deleteVisible = false }, enabled = !busy) {
                Text("CANCEL")
            }
        },
    )

    pendingImport?.let { incoming ->
        val known = groups.mapTo(HashSet()) { it.id }
        val newCount = incoming.count { it.id !in known }
        AlertDialog(
            onDismissRequest = { if (!busy) pendingImport = null },
            title = { Text("Import reminder groups?") },
            text = { Text("${incoming.size} verified groups found. Add ${newCount} new groups; " +
                "skip ${incoming.size - newCount} with existing IDs. " +
                "Existing groups and all reminder schedules are unchanged. " +
                "A group backup never contains the reminders themselves.") },
            confirmButton = {
                TextButton(
                    enabled = !busy && readError == null && newCount > 0,
                    onClick = {
                        write {
                            groupsStore.mutate { current ->
                                V298ReminderGroupMergePolicy.addMissing(current, incoming)
                            }
                            pendingImport = null
                        }
                    },
                    modifier = Modifier.testTag("v298-import-confirm"),
                ) { Text("IMPORT ${newCount}") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }, enabled = !busy) {
                    Text("CANCEL")
                }
            },
        )
    }
}
