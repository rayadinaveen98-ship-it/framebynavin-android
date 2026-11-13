package com.framebynavin.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.spaces.*
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalRecurrence
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * V289 Personal Spaces — optional, local-only owner UI.
 *
 * This opens its own app-owned full screen, separate from Personal Today.
 * Nothing is created/scheduled on open. No photo access, cloud sync, permissions,
 * Creator writes or automatic Done receipts are possible from this screen.
 */
@Composable
internal fun V289PersonalSpacesScreen(
    onClose: () -> Unit,
    databaseForTesting: V289SpacesDatabase? = null,
    onEditReminder: (String) -> Unit = {},
    initialSelectedSpaceId: String? = null,
) {
    val context = LocalContext.current.applicationContext
    val database = remember(context, databaseForTesting) {
        databaseForTesting ?: V289SpacesDatabase.open(context)
    }
    DisposableEffect(database, databaseForTesting) {
        onDispose { if (databaseForTesting == null) database.close() }
    }
    val dao = remember(database) { database.spaces() }
    val repository = remember(dao) { V289SpacesRepository(context, dao) }
    val personalStore = remember(context) { PersonalReminderStore(context) }
    var personalReminders by remember { mutableStateOf(emptyList<PersonalReminder>()) }
    var doneReceipts by remember { mutableStateOf(emptyList<PersonalCompletionEvent>()) }
    var personalReadError by remember { mutableStateOf(false) }
    LaunchedEffect(personalStore) {
        try { personalStore.remindersFlow.collect { personalReminders = it } }
        catch (_: Exception) { personalReadError = true }
    }
    LaunchedEffect(personalStore) {
        try { personalStore.completionHistoryFlow.collect { doneReceipts = it } }
        catch (_: Exception) { personalReadError = true }
    }
    val photoJournal = remember(context, dao) { V290PhotoJournal(context, dao) }
    val interruptedRecovery = remember(context, dao) {
        V290InterruptedPhotoRecovery(context, dao)
    }
    val coroutineScope = rememberCoroutineScope()
    var spaces by remember { mutableStateOf(emptyList<V289SpaceEntity>()) }
    var spacesLoaded by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(emptyList<V289SpaceEntryEntity>()) }
    var linkedReminders by remember { mutableStateOf(emptyList<V289SpaceReminderLinkEntity>()) }
    var linkRemindersVisible by remember { mutableStateOf(false) }
    var newNoteVisible by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf(emptyList<V290SpacePhotoEntity>()) }
    var interruptedPhotos by remember { mutableStateOf(emptyList<V290InterruptedPhoto>()) }
    var pendingInterruptedAction by remember {
        mutableStateOf<Pair<V290InterruptedPhoto, Boolean>?>(null)
    }
    var pendingPickedPhoto by remember { mutableStateOf<Pair<String, Uri>?>(null) }
    var selectedPhotoForDelete by remember { mutableStateOf<V290SpacePhotoEntity?>(null) }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
    // A Home preview may become stale between the tap and the DB query.
    // Consume focus once; never redirect to an unrelated Space or reopen
    // detail after the owner explicitly goes back to the overview.
    var homeFocusHandled by remember(initialSelectedSpaceId) { mutableStateOf(false) }
    var homeFocusNotice by remember(initialSelectedSpaceId) { mutableStateOf<String?>(null) }
    LaunchedEffect(spacesLoaded, spaces, initialSelectedSpaceId) {
        if (!homeFocusHandled && spacesLoaded && !initialSelectedSpaceId.isNullOrBlank()) {
            val matching = spaces.firstOrNull { it.id == initialSelectedSpaceId }
            if (matching != null) {
                entries = emptyList()
                linkedReminders = emptyList()
                photos = emptyList()
                interruptedPhotos = emptyList()
                selectedSpaceId = matching.id
            } else {
                homeFocusNotice = "This Space is no longer available. Showing your saved Spaces."
            }
            homeFocusHandled = true
        }
    }
    var newNoteText by remember(selectedSpaceId) { mutableStateOf("") }
    var allNotesVisible by remember(selectedSpaceId) { mutableStateOf(false) }
    var spaceActionsOpen by remember(selectedSpaceId) { mutableStateOf(false) }
    var createVisible by remember { mutableStateOf(false) }
    var backupVisible by remember { mutableStateOf(false) }
    var backupNotice by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Pair<String, V289SpaceState>?>(null) }
    var editVisible by remember { mutableStateOf(false) }
    var deleteVisible by remember { mutableStateOf(false) }
    var activityVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dao) {
        try {
            dao.observeSpaces().collect {
                spaces = it
                spacesLoaded = true
            }
        } catch (_: Exception) {
            error = "Spaces could not be read. No data was changed."
        }
    }
    LaunchedEffect(dao, selectedSpaceId) {
        val id = selectedSpaceId
        entries = if (id == null) emptyList() else try {
            dao.entriesForSpace(id)
        } catch (_: Exception) {
            error = "Entries could not be loaded. No data was changed."
            emptyList()
        }
        linkedReminders = if (id == null) emptyList() else try {
            dao.linksForSpace(id)
        } catch (_: Exception) {
            error = "Linked reminders could not be loaded. Nothing was changed."
            emptyList()
        }
        photos = if (id == null) emptyList() else try {
            dao.photosForSpace(id)
        } catch (_: Exception) {
            error = "Photos could not be loaded. No files were changed."
            emptyList()
        }
        interruptedPhotos = if (id == null) emptyList() else try {
            interruptedRecovery.find(id)
        } catch (_: Exception) {
            error = "Unfinished photo saves could not be checked. No files were changed."
            emptyList()
        }
    }
    fun write(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        coroutineScope.launch {
            try {
                StabilizationPersonalMutationGate.withOwnerMutation(context) { block() }
                error = null
            } catch (_: Exception) {
                error = "Action failed. Review the current Space before trying again."
            } finally {
                busy = false
            }
        }
    }

    fun backupAction(
        failureMessage: String = "Backup operation failed. Existing Spaces and reminders were not deleted.",
        block: suspend () -> Unit,
    ) {
        if (busy) return
        busy = true
        coroutineScope.launch {
            try {
                StabilizationPersonalMutationGate.withOwnerMutation(context) { block() }
                error = null
            } catch (_: Exception) {
                error = failureMessage
            } finally {
                busy = false
            }
        }
    }
    val saveBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(V289SpacesOwnerBackupFile.MIME_TYPE)
    ) { uri ->
        if (uri != null) backupAction(
            failureMessage = "Spaces export could not be verified. A partial file may exist in the chosen location; check or delete that file before retrying. Existing Space notes were unchanged.",
        ) {
            backupNotice = null
            val raw = V289SpacesMetadataRecovery(context, database).prepareLocalMetadataExport()
            V289SpacesOwnerBackupFile.writeToSelectedFile(context, uri, raw)
            backupNotice = "Saved and verified Spaces metadata against the chosen file. Keep this plaintext file private."
            backupVisible = false
        }
    }
    val selectBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) backupAction {
            val raw = V289SpacesOwnerBackupFile.readFromSelectedFile(context, uri)
            val snapshot = V289SpacesMetadataCodec.decode(raw)
            // Selection is not consent to import. Require a separate confirmation.
            pendingImport = raw to snapshot
            backupVisible = false
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val spaceId = selectedSpaceId
        if (spaceId != null && uri != null) {
            pendingPickedPhoto = spaceId to uri
        }
    }

    val selected = spaces.firstOrNull { it.id == selectedSpaceId }
    BackHandler {
        if (selectedSpaceId != null) {
            entries = emptyList()
            linkedReminders = emptyList()
            photos = emptyList()
            interruptedPhotos = emptyList()
            selectedSpaceId = null
        } else onClose()
    }
    CreatorAdaptiveFullScreen(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("v289-spaces-root"),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (selectedSpaceId != null) {
            entries = emptyList()
            linkedReminders = emptyList()
            photos = emptyList()
            interruptedPhotos = emptyList()
            selectedSpaceId = null
        } else onClose()
                    },
                    modifier = Modifier.testTag("v289-spaces-back"),
                ) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = BacklotPrimaryText)
                }
                Column(Modifier.weight(1f)) {
                    Text("BACKLOT / PERSONAL", color = BacklotSecondaryAccent, fontSize = 10.sp)
                    Text(selected?.title ?: "My Spaces", color = BacklotPrimaryText,
                        fontSize = 21.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = { backupVisible = true },
                    enabled = spacesLoaded && error == null && !busy,
                    modifier = Modifier.testTag("v289-backup-open")
                ) {
                    Icon(Icons.Outlined.SaveAlt, "Backup or restore Space notes",
                        tint = BacklotSecondaryAccent)
                }
                if (selected == null) {
                    IconButton(
                        onClick = { createVisible = true },
                        enabled = spacesLoaded && error == null && !busy,
                        modifier = Modifier.testTag("v289-create-open"),
                    ) {
                        Icon(Icons.Outlined.AddCircleOutline, "Start a Space",
                            tint = BacklotPrimaryText)
                    }
                }
            }
            if (homeFocusNotice != null) {
                Text(homeFocusNotice.orEmpty(), color = BacklotSecondaryAccent,
                    fontSize = 11.sp, modifier = Modifier.testTag("v340-space-focus-unavailable"))
            }
            if (error != null) {
                Text(error.orEmpty(), color = BacklotAccent,
                    fontSize = 12.sp, modifier = Modifier.testTag("v289-error"))
            }
            if (backupNotice != null) {
                Text(backupNotice.orEmpty(), color = BacklotSecondaryAccent,
                    fontSize = 11.sp, modifier = Modifier.testTag("v289-backup-notice"))
            }
            if (selected == null) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    if (!spacesLoaded) {
                        if (error == null) CircularProgressIndicator(color = BacklotSecondaryAccent)
                        else Text("Spaces are unavailable. No data was reset.",
                            color = BacklotSecondaryText, fontSize = 12.sp)
                    } else if (spaces.isEmpty()) {
                        Surface(
                            color = BacklotSurface,
                            border = BorderStroke(1.dp, BacklotBorder),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().testTag("v289-spaces-empty"),
                        ) {
                            Column(Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.AutoAwesome, null,
                                    tint = BacklotSecondaryAccent)
                                Text("No spaces yet",
                                    color = BacklotPrimaryText, fontWeight = FontWeight.Bold)
                                Text("Create a space to track a goal.",
                                    color = BacklotSecondaryText, fontSize = 12.sp)
                                Button(onClick = { createVisible = true },
                                    modifier = Modifier.testTag("v289-empty-create")) {
                                    Text("CREATE SPACE")
                                }
                            }
                        }
                    } else {
                        spaces.forEach { space ->
                            Surface(
                                onClick = {
                                    // Prevent showing another Space’s journal while loading this one.
                                    entries = emptyList()
                                    linkedReminders = emptyList()
                                    photos = emptyList()
            interruptedPhotos = emptyList()
                                    selectedSpaceId = space.id
                                },
                                color = BacklotSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BacklotBorder),
                                modifier = Modifier.fillMaxWidth()
                                    .testTag("v289-space-" + space.id),
                            ) {
                                Row(Modifier.padding(15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(V289SpaceIcon(space.type), null,
                                        tint = BacklotSecondaryAccent,
                                        modifier = Modifier.size(24.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(space.title, color = BacklotPrimaryText,
                                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text(if (space.archivedAtMillis == null) "Active"
                                            else "Archived", color = BacklotSecondaryText,
                                            fontSize = 11.sp)
                                    }
                                    Icon(Icons.Outlined.ChevronRight, "Open",
                                        tint = BacklotSecondaryText)
                                }
                            }
                        }
                        OutlinedButton(onClick = { createVisible = true },
                            modifier = Modifier.fillMaxWidth()) {
                            Text("NEW SPACE")
                        }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(V289SpaceIcon(selected.type), null,
                            tint = BacklotSecondaryAccent,
                            modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f)) {
                            if (selected.archivedAtMillis != null) {
                                Text("Archived", color = BacklotSecondaryAccent, fontSize = 12.sp)
                            }
                            if (selected.goalText.isNotBlank()) {
                                Text(selected.goalText,
                                    color = BacklotSecondaryText, fontSize = 13.sp)
                            }
                        }
                        if (selected.archivedAtMillis == null) {
                            IconButton(
                                onClick = { editVisible = true },
                                enabled = !busy,
                                modifier = Modifier.testTag("v289-edit-open"),
                            ) {
                                Icon(Icons.Outlined.Edit, "Edit Space details",
                                    tint = BacklotSecondaryAccent)
                            }
                        }
                        androidx.compose.foundation.layout.Box {
                            IconButton(
                                onClick = { spaceActionsOpen = true },
                                enabled = !busy,
                                modifier = Modifier.testTag("v294-space-actions"),
                            ) {
                                Icon(Icons.Outlined.MoreVert, "More Space actions",
                                    tint = BacklotSecondaryText)
                            }
                            DropdownMenu(
                                expanded = spaceActionsOpen,
                                onDismissRequest = { spaceActionsOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete space", color = BacklotError) },
                                    onClick = {
                                        spaceActionsOpen = false
                                        deleteVisible = true
                                    },
                                    modifier = Modifier.testTag("v289-delete-open"),
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Button(
                            onClick = { activityVisible = true },
                            enabled = !busy && selected.archivedAtMillis == null,
                            modifier = Modifier.testTag("v289-log-open"),
                        ) {
                            Icon(Icons.Outlined.EditNote, null,
                                modifier = Modifier.size(18.dp))
                            Text("LOG PROGRESS")
                        }
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                val id = selected.id
                                if (selected.archivedAtMillis == null) write {
                                    val current = checkNotNull(dao.getSpace(id)) { "Space missing" }
                                    val domain = V289SpaceType.valueOf(current.type)
                                    repository.archiveSpace(
                                        V289PersonalSpace(
                                            current.id, current.title, domain, current.iconKey,
                                            current.goalText, current.createdAtMillis,
                                            current.archivedAtMillis, current.displayOrder,
                                        ),
                                        System.currentTimeMillis()
                                    )
                                } else write { repository.unarchiveSpace(id) }
                            },
                            modifier = Modifier.testTag("v289-archive"),
                        ) { Text(if (selected.archivedAtMillis == null) "ARCHIVE" else "RESTORE") }
                    }
                    // A private note is not an activity check-in or fitness
                    // measurement. It must not inflate progress or active days.
                    val progressEntries = entries.filter {
                        it.kind != V289SpaceEntryKind.NOTE.name
                    }
                    val summary = remember(progressEntries) {
                        V289SpaceProgressPolicy.summarize(
                            progressEntries, System.currentTimeMillis()
                        )
                    }
                    Surface(color = BacklotSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        modifier = Modifier.fillMaxWidth().testTag("v289-progress-summary")) {
                        Row(Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${summary.totalRecordedEntries} logs",
                                    color = BacklotPrimaryText,
                                    fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${summary.daysWithActivityLastSeven} active days",
                                    color = BacklotPrimaryText,
                                    fontWeight = FontWeight.SemiBold)
                                Text("Past 7 days", color = BacklotSecondaryText,
                                    fontSize = 10.sp)
                            }
                        }
                    }

                    val linkedIds = linkedReminders.mapTo(HashSet()) { it.reminderId }
                    val confirmedDone = doneReceipts.count { it.reminderId in linkedIds }
                    Surface(color = BacklotSurface, shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        modifier = Modifier.fillMaxWidth().testTag("v295-linked-reminders")) {
                        Column(Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Linked reminders", color = BacklotPrimaryText,
                                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${linkedReminders.size} linked · $confirmedDone confirmed Done",
                                        color = BacklotSecondaryText, fontSize = 12.sp,
                                        modifier = Modifier.testTag("v295-linked-done"))
                                }
                                TextButton(onClick = { linkRemindersVisible = true },
                                    enabled = !busy && !personalReadError &&
                                        selected.archivedAtMillis == null,
                                    modifier = Modifier.testTag("v295-link-open")) { Text("LINK") }
                            }
                            if (personalReadError) {
                                Text("Couldn't load Personal reminders. Links weren't changed.",
                                    color = BacklotError, fontSize = 12.sp)
                            } else if (linkedReminders.isEmpty()) {
                                Text("Link a reminder to see it alongside your progress.",
                                    color = BacklotSecondaryText, fontSize = 12.sp)
                            }
                            linkedReminders.forEach { link ->
                                val reminder = personalReminders.firstOrNull {
                                    it.id == link.reminderId
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(reminder?.title ?: "Reminder unavailable",
                                            color = BacklotPrimaryText, fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium)
                                        val nextAt = reminder?.let {
                                            runCatching {
                                                PersonalRecurrence.nextAfter(
                                                    it, System.currentTimeMillis()
                                                )
                                            }.getOrNull()
                                        }
                                        Text(
                                            if (reminder == null) "Unlink this missing reminder"
                                            else if (reminder.paused) "Paused"
                                            else nextAt?.let {
                                                "Next · " + Instant.ofEpochMilli(it)
                                                    .atZone(reminder.zoneId)
                                                    .format(DateTimeFormatter.ofPattern("EEE h:mm a"))
                                            } ?: "No upcoming occurrence",
                                            color = BacklotSecondaryText, fontSize = 11.sp,
                                        )
                                    }
                                    if (reminder != null) {
                                        TextButton(onClick = { onEditReminder(reminder.id) },
                                            modifier = Modifier.testTag("v295-edit-linked-reminder")) {
                                            Text("EDIT")
                                        }
                                    }
                                    IconButton(enabled = !busy,
                                        onClick = {
                                            val id = selected.id
                                            write {
                                                dao.unlinkReminder(id, link.reminderId)
                                                linkedReminders = dao.linksForSpace(id)
                                            }
                                        },
                                        modifier = Modifier.testTag("v295-unlink-reminder")) {
                                        Icon(Icons.Outlined.LinkOff, "Unlink reminder",
                                            tint = BacklotSecondaryText)
                                    }
                                }
                            }
                        }
                    }
                    Surface(color = BacklotSurface, shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        modifier = Modifier.fillMaxWidth().testTag("v295-space-notes")) {
                        Column(Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Notes", color = BacklotPrimaryText, fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f))
                                TextButton(enabled = !busy && selected.archivedAtMillis == null,
                                    onClick = { newNoteText = ""; newNoteVisible = true },
                                    modifier = Modifier.testTag("v295-note-open")) { Text("ADD NOTE") }
                            }
                            val notes = entries.filter { it.kind == V289SpaceEntryKind.NOTE.name }
                            if (notes.isEmpty()) {
                                Text("No notes yet.", color = BacklotSecondaryText, fontSize = 12.sp)
                            } else {
                                notes.take(if (allNotesVisible) notes.size else 3).forEach { item ->
                                    Text(Instant.ofEpochMilli(item.occurredAtMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("d MMM · h:mm a")),
                                        color = BacklotSecondaryAccent, fontSize = 11.sp)
                                    Text(item.note, color = BacklotPrimaryText,
                                        fontSize = 13.sp,
                                        modifier = Modifier.testTag("v295-saved-note"))
                                }
                                if (notes.size > 3) {
                                    TextButton(onClick = { allNotesVisible = !allNotesVisible },
                                        modifier = Modifier.testTag("v295-notes-expand")) {
                                        Text(if (allNotesVisible) "SHOW LESS"
                                            else "VIEW ALL ${notes.size} NOTES")
                                    }
                                }
                            }
                        }
                    }
                    V290PersonalPhotoSection(
                        photos = photos,
                        interruptedPhotos = interruptedPhotos,
                        journal = photoJournal,
                        enabled = !busy,
                        allowNewPhotos = selected.archivedAtMillis == null,
                        onAddPhoto = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemovePhoto = { photo -> selectedPhotoForDelete = photo },
                        onRecoverPhoto = { item -> pendingInterruptedAction = item to true },
                        onDiscardPhoto = { item -> pendingInterruptedAction = item to false },
                    )
                    Text("Activity", color = BacklotPrimaryText,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Spaces and photos aren't included in cloud backup.",
                        color = BacklotSecondaryText, fontSize = 11.sp,
                        modifier = Modifier.testTag("v289-local-only-notice"))
                    if (progressEntries.isEmpty()) {
                        Text("No progress yet. Log a check-in when you're ready.",
                            color = BacklotSecondaryText, fontSize = 13.sp,
                            modifier = Modifier.testTag("v289-entries-empty"))
                    } else {
                        progressEntries.forEach { item ->
                            Surface(color = BacklotSurface,
                                border = BorderStroke(1.dp, BacklotBorder),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("v289-entry")) {
                                Column(Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        Instant.ofEpochMilli(item.occurredAtMillis)
                                            .atZone(ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("d MMM yyyy · h:mm a", Locale.getDefault())),
                                        color = BacklotSecondaryAccent, fontSize = 11.sp)
                                    Text(item.note.ifBlank {
                                        if (item.kind == V289SpaceEntryKind.METRIC.name)
                                            "Measured progress" else "Checked in"
                                    }, color = BacklotPrimaryText, fontSize = 13.sp)
                                    if (item.kind == V289SpaceEntryKind.METRIC.name &&
                                        item.metricValue != null && item.metricName != null &&
                                        item.metricUnit != null) {
                                        Text(
                                            V292SpaceMetricSuggestions.describe(
                                                item.metricName, item.metricValue, item.metricUnit
                                            ),
                                            color = BacklotSecondaryAccent, fontSize = 12.sp,
                                            modifier = Modifier.testTag("v292-recorded-metric")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingInterruptedAction?.let { (item, shouldRecover) ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingInterruptedAction = null },
            title = {
                Text(if (shouldRecover) "Recover private photo?" else "Discard unfinished photo?")
            },
            text = {
                Text(
                    if (shouldRecover)
                        "This restores the private JPEG to your Space journal using its saved timestamp. Original notes from the interrupted save cannot be recovered."
                    else "This permanently deletes only the unfinished private copy. Your original gallery photo is never touched."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && selectedSpaceId == item.spaceId,
                    modifier = Modifier.testTag("v290-interrupted-action-confirm"),
                    onClick = {
                        write {
                            if (shouldRecover) {
                                interruptedRecovery.recover(item)
                            } else {
                                check(interruptedRecovery.discard(item)) {
                                    "Could not remove unfinished private photo"
                                }
                            }
                            if (selectedSpaceId == item.spaceId) {
                                photos = dao.photosForSpace(item.spaceId)
                                interruptedPhotos = interruptedRecovery.find(item.spaceId)
                            }
                            pendingInterruptedAction = null
                        }
                    },
                ) { Text(if (shouldRecover) "RECOVER" else "DISCARD", color = BacklotAccent) }
            },
            dismissButton = {
                TextButton(onClick = { pendingInterruptedAction = null }, enabled = !busy) {
                    Text("CANCEL")
                }
            },
        )
    }

    pendingPickedPhoto?.let { (spaceId, photoUri) ->
        var photoNote by remember(spaceId, photoUri) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) pendingPickedPhoto = null },
            title = { Text("Save private photo?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "A copy will be saved inside Backlot on this device. Original GPS/EXIF metadata is removed. The source photo stays where it is.",
                        color = BacklotPrimaryText, fontSize = 12.sp,
                    )
                    OutlinedTextField(
                        value = photoNote,
                        onValueChange = { photoNote = it.take(500) },
                        modifier = Modifier.fillMaxWidth().testTag("v290-photo-note"),
                        label = { Text("Add a note (optional)") },
                        minLines = 2,
                        maxLines = 4,
                    )
                    Text(
                        "Photos are NOT included in Spaces JSON backups or cloud restore. Reinstalling or clearing app data may remove them.",
                        color = BacklotAccent, fontSize = 11.sp,
                        modifier = Modifier.testTag("v290-photo-backup-warning"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && selected?.id == spaceId &&
                        selected?.archivedAtMillis == null,
                    modifier = Modifier.testTag("v290-save-picked-photo"),
                    onClick = {
                        val note = photoNote.trim()
                        write {
                            photoJournal.addOwnerPickedPhoto(spaceId, photoUri, note)
                            photos = dao.photosForSpace(spaceId)
                            interruptedPhotos = interruptedRecovery.find(spaceId)
                            pendingPickedPhoto = null
                        }
                    },
                ) { Text("SAVE ON DEVICE") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPickedPhoto = null }, enabled = !busy) {
                    Text("CANCEL")
                }
            },
        )
    }

    selectedPhotoForDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { if (!busy) selectedPhotoForDelete = null },
            title = { Text("Delete private photo?") },
            text = {
                Text("This permanently removes the saved photo from this device's Space journal. It does not delete your original gallery photo.")
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    modifier = Modifier.testTag("v290-delete-photo-confirm"),
                    onClick = {
                        write {
                            check(photoJournal.removePhoto(photo.id)) { "Photo not found" }
                            if (selectedSpaceId == photo.spaceId) {
                                photos = dao.photosForSpace(photo.spaceId)
                                interruptedPhotos = interruptedRecovery.find(photo.spaceId)
                            }
                            selectedPhotoForDelete = null
                        }
                    },
                ) { Text("DELETE PRIVATE COPY", color = BacklotAccent) }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { selectedPhotoForDelete = null },
                ) { Text("CANCEL") }
            },
        )
    }

    if (backupVisible) {
        AlertDialog(
            onDismissRequest = { if (!busy) backupVisible = false },
            title = { Text("Spaces backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Save or restore your Spaces, goals and manual journal notes.",
                        color = BacklotPrimaryText)
                    Text("This JSON file does not include photos, sounds or your actual reminders. Backlot cloud sync is not connected to Spaces yet.",
                        color = BacklotSecondaryText, fontSize = 12.sp)
                    Text("Your notes are NOT encrypted in the exported file. Choose a trusted location. A cloud file provider may upload it.",
                        color = BacklotAccent, fontSize = 12.sp,
                        modifier = Modifier.testTag("v289-backup-plaintext-warning"))
                    Button(
                        onClick = {
                            saveBackupDocument.launch(V289SpacesOwnerBackupFile.DEFAULT_FILE_NAME)
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("v289-backup-export"),
                    ) { Text("SAVE METADATA FILE") }
                    OutlinedButton(
                        onClick = {
                            selectBackupDocument.launch(
                                arrayOf(V289SpacesOwnerBackupFile.MIME_TYPE, "text/plain")
                            )
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag("v289-backup-import"),
                    ) { Text("CHOOSE FILE TO RESTORE") }
                }
            },
            confirmButton = {
                TextButton(onClick = { backupVisible = false }, enabled = !busy) {
                    Text("CLOSE")
                }
            },
        )
    }

    pendingImport?.let { (raw, snapshot) ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingImport = null },
            title = { Text("Review Spaces restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Found " + snapshot.spaces.size + " Spaces, " +
                            snapshot.entries.size + " journal entries and " +
                            snapshot.links.size + " reminder links.",
                        color = BacklotPrimaryText,
                        modifier = Modifier.testTag("v289-backup-import-preview"),
                    )
                    Text("This merges records only. It does not replace existing Spaces or reminders. Conflicts or missing linked reminders cancel the entire restore.",
                        color = BacklotSecondaryText, fontSize = 12.sp)
                    Text("Only metadata and manual entries are supported. Photos are not restored.",
                        color = BacklotSecondaryText, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        backupAction {
                            val existingReminders = PersonalReminderStore(context)
                                .load().mapTo(HashSet<String>()) { it.id }
                            val result = V289SpacesMetadataRecovery(context, database)
                                .mergeVerifiedMetadata(raw, existingReminders)
                            // Refresh currently selected Space history after a merge.
                            entries = selectedSpaceId?.let { dao.entriesForSpace(it) }.orEmpty()
                            backupNotice = "Restored " + result.spacesAdded +
                                " Spaces and " + result.entriesAdded + " journal entries."
                            pendingImport = null
                        }
                    },
                    modifier = Modifier.testTag("v289-backup-import-confirm"),
                ) { Text("MERGE SAFELY") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }, enabled = !busy) {
                    Text("CANCEL")
                }
            },
        )
    }

    if (createVisible) {
        var type by remember { mutableStateOf(V289SpaceType.SKIN_CARE) }
        var name by remember { mutableStateOf(type.defaultTitle) }
        var goal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) createVisible = false },
            title = { Text("Start a Space") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose a starting point", fontSize = 12.sp)
                    V289SpaceType.entries.forEach { suggestion ->
                        FilterChip(
                            selected = type == suggestion,
                            onClick = {
                                val replaceSuggested = name == type.defaultTitle
                                type = suggestion
                                if (replaceSuggested) name = type.defaultTitle
                            },
                            label = { Text(suggestion.defaultTitle) },
                        )
                    }
                    OutlinedTextField(value = name,
                        onValueChange = { name = it.take(80) },
                        label = { Text("Space name") },
                        modifier = Modifier.fillMaxWidth().testTag("v289-new-title"),
                        singleLine = true)
                    OutlinedTextField(value = goal,
                        onValueChange = { goal = it.take(500) },
                        label = { Text("Goal (optional)") },
                        modifier = Modifier.fillMaxWidth())
                    Text("Stored on this device only. Backlot cloud restore does not include Spaces yet. No reminders or photos are added automatically.",
                        color = BacklotSecondaryText, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && name.trim().isNotEmpty(),
                    modifier = Modifier.testTag("v289-create-save"),
                    onClick = {
                        val chosenType = type
                        val chosenName = name.trim()
                        val chosenGoal = goal.trim()
                        write {
                            val record = V289PersonalSpacePolicy.newSpace(
                                chosenType, System.currentTimeMillis()
                            ).copy(title = chosenName, goalText = chosenGoal)
                            repository.createSpace(record)
                            createVisible = false
                            entries = emptyList()
                            photos = emptyList()
            interruptedPhotos = emptyList()
                            selectedSpaceId = record.id
                        }
                    },
                ) { Text("CREATE") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { createVisible = false }) {
                    Text("CANCEL")
                }
            },
        )
    }

    if (editVisible && selected != null && selected.archivedAtMillis == null) {
        // Reinitialize from the selected record when changing Spaces.
        var editName by remember(selected.id) { mutableStateOf(selected.title) }
        var editGoal by remember(selected.id) { mutableStateOf(selected.goalText) }
        AlertDialog(
            onDismissRequest = { if (!busy) editVisible = false },
            title = { Text("Edit Space") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it.take(80) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth().testTag("v289-edit-title"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = editGoal,
                        onValueChange = { editGoal = it.take(500) },
                        label = { Text("Goal (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("v289-edit-goal"),
                        minLines = 2,
                        maxLines = 3,
                    )
                    Text("Changing this Space never edits your linked reminders.",
                        color = BacklotSecondaryText, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && editName.trim().isNotEmpty(),
                    modifier = Modifier.testTag("v289-edit-save"),
                    onClick = {
                        val original = V289PersonalSpace(
                            id = selected.id,
                            title = selected.title,
                            type = V289SpaceType.valueOf(selected.type),
                            iconKey = selected.iconKey,
                            goalText = selected.goalText,
                            createdAtMillis = selected.createdAtMillis,
                            archivedAtMillis = selected.archivedAtMillis,
                            displayOrder = selected.displayOrder,
                        )
                        val title = editName.trim()
                        val goal = editGoal.trim()
                        write {
                            check(repository.editSpace(
                                original, title, goal, original.iconKey
                            )) { "The Space was not found" }
                            editVisible = false
                        }
                    },
                ) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { editVisible = false }) {
                    Text("CANCEL")
                }
            },
        )
    }


    if (linkRemindersVisible && selected != null) {
        val spaceId = selected.id
        val alreadyLinked = linkedReminders.mapTo(HashSet()) { it.reminderId }
        val available = personalReminders.filter { it.id !in alreadyLinked }
        AlertDialog(
            onDismissRequest = { if (!busy) linkRemindersVisible = false },
            title = { Text("Link reminders") },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Linking doesn't change reminder timing, alerts or scheduling.",
                        color = BacklotSecondaryText, fontSize = 12.sp)
                    if (available.isEmpty()) {
                        Text("No other Personal reminders to link.", fontSize = 12.sp)
                    }
                    available.forEach { reminder ->
                        OutlinedButton(
                            enabled = !busy && !personalReadError,
                            onClick = {
                                write {
                                    repository.linkExistingReminder(
                                        V289SpaceReminderLink(
                                            spaceId = spaceId,
                                            reminderId = reminder.id,
                                            linkedAtMillis = System.currentTimeMillis(),
                                        ),
                                        personalReminders.mapTo(HashSet()) { it.id },
                                    )
                                    linkedReminders = dao.linksForSpace(spaceId)
                                    linkRemindersVisible = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("v295-link-available"),
                        ) { Text(reminder.title, maxLines = 1) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { linkRemindersVisible = false }) { Text("DONE") }
            },
        )
    }
    if (newNoteVisible && selected != null) {
        val spaceId = selected.id
        AlertDialog(
            onDismissRequest = { if (!busy) newNoteVisible = false },
            title = { Text("New Space note") },
            text = {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it.take(2000) },
                    label = { Text("Your note") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("v295-note-input"),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && newNoteText.isNotBlank(),
                    onClick = {
                        val content = newNoteText.trim()
                        write {
                            repository.logManualEntry(
                                V289SpaceEntry(
                                    id = V289PersonalSpacePolicy.canonicalUuid(),
                                    spaceId = spaceId,
                                    kind = V289SpaceEntryKind.NOTE,
                                    source = V289SpaceEntrySource.MANUAL,
                                    occurredAtMillis = System.currentTimeMillis(),
                                    createdAtMillis = System.currentTimeMillis(),
                                    note = content,
                                )
                            )
                            entries = dao.entriesForSpace(spaceId)
                            newNoteVisible = false
                        }
                    },
                    modifier = Modifier.testTag("v295-note-save"),
                ) { Text("SAVE NOTE") }
            },
            dismissButton = {
                TextButton(onClick = { newNoteVisible = false }) { Text("CANCEL") }
            },
        )
    }

    if (activityVisible && selected != null) {
        var note by remember(selected.id) { mutableStateOf("") }
        val metricPreset = V292SpaceMetricSuggestions.forType(
            V289SpaceType.valueOf(selected.type)
        )
        var includeMetric by remember(selected.id) { mutableStateOf(false) }
        var metricText by remember(selected.id) { mutableStateOf("") }
        val safeMetric = if (includeMetric) metricPreset?.parseOrNull(metricText) else null
        AlertDialog(
            onDismissRequest = { if (!busy) activityVisible = false },
            title = { Text("Log your progress") },
            text = {
                Column(
                    Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("Log what you did. Optional measures are saved only when you choose.",
                        color = BacklotSecondaryText, fontSize = 12.sp)
                    OutlinedTextField(value = note,
                        onValueChange = { note = it.take(2000) },
                        label = { Text("What did you do today?") },
                        modifier = Modifier.fillMaxWidth().testTag("v289-checkin-note"),
                        minLines = 2, maxLines = 4)
                    if (metricPreset != null) {
                        TextButton(
                            onClick = {
                                includeMetric = !includeMetric
                                if (!includeMetric) metricText = ""
                            },
                            enabled = !busy,
                            modifier = Modifier.testTag("v292-metric-toggle"),
                        ) {
                            Text(if (includeMetric) "REMOVE MEASURE" else "ADD MEASUREMENT")
                        }
                        if (includeMetric) {
                            OutlinedTextField(
                                value = metricText,
                                onValueChange = { raw ->
                                    metricText = raw.take(6)
                                },
                                label = {
                                    Text(metricPreset.name + " (" + metricPreset.unit + ")")
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                isError = metricText.isNotBlank() && safeMetric == null,
                                supportingText = {
                                    Text("Whole number, 1–" + metricPreset.maxValue)
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .testTag("v292-metric-value"),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && (!includeMetric || safeMetric != null),
                    onClick = {
                        val measuredValue = safeMetric
                        val chosenMetric = metricPreset
                        val spaceId = selected.id
                        val text = note.trim()
                        write {
                            repository.logManualEntry(
                                V289SpaceEntry(
                                    id = V289PersonalSpacePolicy.canonicalUuid(),
                                    spaceId = spaceId,
                                    kind = if (measuredValue == null)
                                        V289SpaceEntryKind.CHECK_IN else V289SpaceEntryKind.METRIC,
                                    source = V289SpaceEntrySource.MANUAL,
                                    occurredAtMillis = System.currentTimeMillis(),
                                    createdAtMillis = System.currentTimeMillis(),
                                    note = text,
                                    metricName = if (measuredValue != null) chosenMetric?.name else null,
                                    metricValue = measuredValue,
                                    metricUnit = if (measuredValue != null) chosenMetric?.unit else null,
                                )
                            )
                            entries = dao.entriesForSpace(spaceId)
                            activityVisible = false
                        }
                    },
                    modifier = Modifier.testTag("v289-checkin-save"),
                ) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { activityVisible = false }) {
                    Text("CANCEL")
                }
            },
        )
    }

    if (deleteVisible && selected != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) deleteVisible = false },
            title = { Text("Delete this Space?") },
            text = {
                Text(
                    if (photos.isEmpty() && interruptedPhotos.isEmpty())
                        "This removes local notes, progress and unfinished temporary photo copies. Personal reminders remain untouched."
                    else "Resolve all saved and interrupted private photos before deleting this Space."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && photos.isEmpty() && interruptedPhotos.isEmpty(),
                    onClick = {
                        val id = selected.id
                        write {
                            // Recheck disk right before deletion, including files
                            // younger than the display scanner's grace period.
                            check(
                                interruptedRecovery.find(
                                    id, System.currentTimeMillis() + 120_000L
                                ).isEmpty()
                            ) { "Resolve interrupted private photo copies first" }
                            // Owner explicitly confirmed Space deletion. Clear only
                            // incomplete .UUID.partial copies before removing records.
                            interruptedRecovery.removeTemporaryCopiesOnOwnerSpaceDelete(id)
                            check(repository.deleteSpaceAfterOwnerConfirmation(id)) {
                                "Space no longer exists"
                            }
                            selectedSpaceId = null
                            deleteVisible = false
                        }
                    },
                    modifier = Modifier.testTag("v289-delete-confirm"),
                ) { Text("DELETE", color = BacklotAccent) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { deleteVisible = false }) {
                    Text("CANCEL")
                }
            },
        )
    }
}

internal fun V289SpaceIcon(type: String): ImageVector =
    when (runCatching { V289SpaceType.valueOf(type) }.getOrDefault(V289SpaceType.CUSTOM)) {
        V289SpaceType.SKIN_CARE -> Icons.Outlined.WbSunny
        V289SpaceType.FITNESS -> Icons.Outlined.FitnessCenter
        V289SpaceType.READING -> Icons.Outlined.MenuBook
        V289SpaceType.EDITING -> Icons.Outlined.Movie
        V289SpaceType.GAME_DEVELOPMENT -> Icons.Outlined.Code
        V289SpaceType.GAMING -> Icons.Outlined.SportsEsports
        V289SpaceType.CUSTOM -> Icons.Outlined.AutoAwesome
    }
