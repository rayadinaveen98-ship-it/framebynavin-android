package com.framebynavin.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.spaces.*
import com.framebynavin.app.personal.PersonalReminderStore
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
) {
    val context = LocalContext.current.applicationContext
    val database = remember(context, databaseForTesting) {
        databaseForTesting ?: V289SpacesDatabase.open(context)
    }
    DisposableEffect(database, databaseForTesting) {
        onDispose { if (databaseForTesting == null) database.close() }
    }
    val dao = remember(database) { database.spaces() }
    val repository = remember(dao) { V289SpacesRepository(dao) }
    val photoJournal = remember(context, dao) { V290PhotoJournal(context, dao) }
    val coroutineScope = rememberCoroutineScope()
    var spaces by remember { mutableStateOf(emptyList<V289SpaceEntity>()) }
    var spacesLoaded by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(emptyList<V289SpaceEntryEntity>()) }
    var photos by remember { mutableStateOf(emptyList<V290SpacePhotoEntity>()) }
    var pendingPickedPhoto by remember { mutableStateOf<Pair<String, Uri>?>(null) }
    var selectedPhotoForDelete by remember { mutableStateOf<V290SpacePhotoEntity?>(null) }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
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
        photos = if (id == null) emptyList() else try {
            dao.photosForSpace(id)
        } catch (_: Exception) {
            error = "Photos could not be loaded. No files were changed."
            emptyList()
        }
    }
    fun write(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        coroutineScope.launch {
            try {
                block()
                error = null
            } catch (_: Exception) {
                error = "Action failed. Review the current Space before trying again."
            } finally {
                busy = false
            }
        }
    }

    fun backupAction(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        coroutineScope.launch {
            try {
                block()
                error = null
            } catch (_: Exception) {
                error = "Backup operation failed. Existing Spaces and reminders were not deleted."
            } finally {
                busy = false
            }
        }
    }
    val saveBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(V289SpacesOwnerBackupFile.MIME_TYPE)
    ) { uri ->
        if (uri != null) backupAction {
            val raw = V289SpacesMetadataRecovery(database).prepareLocalMetadataExport()
            V289SpacesOwnerBackupFile.writeToSelectedFile(context, uri, raw)
            backupNotice = "Saved Spaces metadata. Keep this plaintext file in a safe place."
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
            photos = emptyList()
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
            photos = emptyList()
            selectedSpaceId = null
        } else onClose()
                    },
                    modifier = Modifier.testTag("v289-spaces-back"),
                ) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = BacklotPrimaryText)
                }
                Column(Modifier.weight(1f)) {
                    Text("BACKLOT / PERSONAL", color = BacklotSecondaryAccent, fontSize = 10.sp)
                    Text(selected?.title ?: "Your spaces", color = BacklotPrimaryText,
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
                    Text("Your habits, skills and little wins — in one place.",
                        color = BacklotSecondaryText, fontSize = 12.sp)
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
                                Text("Make room for your progress",
                                    color = BacklotPrimaryText, fontWeight = FontWeight.Bold)
                                Text("Create one Space for your skincare, workouts, reading or another goal. Nothing is tracked until you log it.",
                                    color = BacklotSecondaryText, fontSize = 12.sp)
                                Button(onClick = { createVisible = true },
                                    modifier = Modifier.testTag("v289-empty-create")) {
                                    Text("START A SPACE")
                                }
                            }
                        }
                    } else {
                        spaces.forEach { space ->
                            Surface(
                                onClick = {
                                    // Prevent showing another Space’s journal while loading this one.
                                    entries = emptyList()
                                    photos = emptyList()
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
                            Text("START ANOTHER SPACE")
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
                            Text(if (selected.archivedAtMillis == null) "YOUR JOURNEY" else "ARCHIVED SPACE",
                                color = BacklotSecondaryAccent, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                            Text(selected.goalText.ifBlank { "One step at a time." },
                                color = BacklotSecondaryText, fontSize = 13.sp)
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
                    val summary = remember(entries) {
                        V289SpaceProgressPolicy.summarize(entries, System.currentTimeMillis())
                    }
                    Surface(color = BacklotSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        modifier = Modifier.fillMaxWidth().testTag("v289-progress-summary")) {
                        Row(Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${summary.manualCheckIns} check-ins",
                                    color = BacklotPrimaryText,
                                    fontWeight = FontWeight.SemiBold)
                                Text("Recorded by you", color = BacklotSecondaryText,
                                    fontSize = 10.sp)
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
                    V290PersonalPhotoSection(
                        photos = photos,
                        journal = photoJournal,
                        enabled = !busy && selected.archivedAtMillis == null,
                        onAddPhoto = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemovePhoto = { photo -> selectedPhotoForDelete = photo },
                    )
                    Text("Activity", color = BacklotPrimaryText,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Only things you explicitly record appear here.",
                        color = BacklotSecondaryText, fontSize = 11.sp)
                    Text("Device-only: current Backlot cloud backup does not include Spaces.",
                        color = BacklotSecondaryText, fontSize = 11.sp,
                        modifier = Modifier.testTag("v289-local-only-notice"))
                    if (entries.isEmpty()) {
                        Text("No entries yet. Add your first note or check-in.",
                            color = BacklotSecondaryText, fontSize = 13.sp,
                            modifier = Modifier.testTag("v289-entries-empty"))
                    } else {
                        entries.forEach { item ->
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
                                    Text(item.note.ifBlank { "Checked in" },
                                        color = BacklotPrimaryText, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { deleteVisible = true },
                        enabled = !busy,
                        modifier = Modifier.testTag("v289-delete-open"),
                    ) { Text("DELETE THIS SPACE", color = BacklotAccent) }
                }
            }
        }
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
                        selected.archivedAtMillis == null,
                    modifier = Modifier.testTag("v290-save-picked-photo"),
                    onClick = {
                        val note = photoNote.trim()
                        write {
                            photoJournal.addOwnerPickedPhoto(spaceId, photoUri, note)
                            photos = dao.photosForSpace(spaceId)
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
                            val result = V289SpacesMetadataRecovery(database)
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

    if (activityVisible && selected != null) {
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) activityVisible = false },
            title = { Text("Log your progress") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Only your own check-ins count toward progress.",
                        color = BacklotSecondaryText, fontSize = 12.sp)
                    OutlinedTextField(value = note,
                        onValueChange = { note = it.take(2000) },
                        label = { Text("What did you do today?") },
                        modifier = Modifier.fillMaxWidth().testTag("v289-checkin-note"),
                        minLines = 3, maxLines = 5)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val spaceId = selected.id
                        val text = note.trim()
                        write {
                            repository.logManualEntry(
                                V289SpaceEntry(
                                    id = V289PersonalSpacePolicy.canonicalUuid(),
                                    spaceId = spaceId,
                                    kind = V289SpaceEntryKind.CHECK_IN,
                                    source = V289SpaceEntrySource.MANUAL,
                                    occurredAtMillis = System.currentTimeMillis(),
                                    createdAtMillis = System.currentTimeMillis(),
                                    note = text,
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
                    if (photos.isEmpty())
                        "This removes local notes and progress. Linked Personal reminders are never deleted."
                    else "Remove every private photo in this Space first. Space deletion is blocked while photos remain."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && photos.isEmpty(),
                    onClick = {
                        val id = selected.id
                        write {
                            repository.deleteSpaceAfterOwnerConfirmation(id)
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

private fun V289SpaceIcon(type: String): ImageVector =
    when (runCatching { V289SpaceType.valueOf(type) }.getOrDefault(V289SpaceType.CUSTOM)) {
        V289SpaceType.SKIN_CARE -> Icons.Outlined.WbSunny
        V289SpaceType.FITNESS -> Icons.Outlined.FitnessCenter
        V289SpaceType.READING -> Icons.Outlined.MenuBook
        V289SpaceType.EDITING -> Icons.Outlined.Movie
        V289SpaceType.GAME_DEVELOPMENT -> Icons.Outlined.Code
        V289SpaceType.GAMING -> Icons.Outlined.SportsEsports
        V289SpaceType.CUSTOM -> Icons.Outlined.AutoAwesome
    }
