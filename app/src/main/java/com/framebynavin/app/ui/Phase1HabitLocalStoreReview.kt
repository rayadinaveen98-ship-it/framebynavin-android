package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitCompletionMode
import com.framebynavin.app.personal.Phase1HabitDayStatus
import com.framebynavin.app.personal.FC14OwnerScheduleCleanup
import com.framebynavin.app.personal.Phase1HabitStore
import com.framebynavin.app.personal.V298ReminderGroup
import com.framebynavin.app.personal.V298ReminderGroupStore
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.Phase1HabitAlarmScheduler
import com.framebynavin.app.personal.Phase1HabitNotification
import com.framebynavin.app.personal.Phase1HabitVoiceService
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owner-data-backed REVIEW integration for the existing one-activity challenge.
 *
 * Reuses precisely the same Compose editor/dashboard as the screenshot-approved
 * debug fixtures, but persists confirmations to Phase1HabitStore (isolated from
 * legacy V297 plans, Personal reminders, Spaces and Creator).
 *
 * Restored in the Personal Settings navigation on owner approval. Reads saved
 * challenges only; the original owner-specific rebuild plan is removed once
 * by an exact-ID migration and no default activity is preloaded.
 *
 * Exact alarm/voice delivery on a physical device still requires acceptance
 * testing; UI must never invent a successful alarm receipt.
 */
@Composable
internal fun Phase1HabitLocalStoreReview(
    onClose: () -> Unit,
    onOpenLinkedSpace: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember(context) { Phase1HabitStore(context) }
    val groupStore = remember(context) { V298ReminderGroupStore(context) }
    val coroutineScope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<Phase1HabitChallenge>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var fatalReadError by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    var chosenId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    val zone = remember { ZoneId.systemDefault() }
    var startDate by remember { mutableStateOf(LocalDate.now(zone)) }
    // Alert delivery is not connected: default Off, refuse an unmet promise.
    var alertKind by remember { mutableStateOf<Phase1HabitAlertKind?>(null) }
    var alertTime by remember { mutableStateOf(LocalTime.of(7, 30)) }
    var completionMode by remember { mutableStateOf(Phase1HabitCompletionMode.STRICT) }
    var detailDay by remember { mutableStateOf<LocalDate?>(null) }
    var detailNote by remember { mutableStateOf("") }
    var linkDialog by remember { mutableStateOf(false) }
    var linkSpaceId by remember { mutableStateOf<String?>(null) }
    var linkGroupId by remember { mutableStateOf<String?>(null) }
    var availableGroups by remember { mutableStateOf<List<V298ReminderGroup>>(emptyList()) }
    var availableSpaces by remember { mutableStateOf<List<V289SpaceEntity>>(emptyList()) }
    var linkCatalogError by remember { mutableStateOf<String?>(null) }
    var linkCatalogReady by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(store) {
        val cleanupError = runCatching {
            FC14OwnerScheduleCleanup.run(context)
        }.exceptionOrNull()
        if (cleanupError != null) {
            message = "Old rebuild schedule cleanup needs attention. " +
                "Your other reminders and challenges were not reset."
        }
        try {
            store.challengesFlow.collect { incoming ->
                records = incoming
                loaded = true
            }
        } catch (_: Exception) {
            fatalReadError = true
            loaded = true
            message = "Saved challenge data could not be read. No records were changed."
        }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = Instant.now()
            delay(30_000L)
        }
    }
    val selected = records.firstOrNull { it.id == chosenId } ?: records.lastOrNull()

    LaunchedEffect(linkDialog) {
        if (!linkDialog) return@LaunchedEffect
        linkCatalogReady = false
        linkCatalogError = null
        try {
            val loadedGroups = groupStore.load()
            val file = context.getDatabasePath(V289SpacesDatabase.FILE_NAME)
            val loadedSpaces = if (!file.exists()) emptyList()
            else {
                require(file.isFile) { "Spaces database is unavailable" }
                val db = V289SpacesDatabase.open(context)
                try {
                    db.spaces().loadSpaces().filter { it.archivedAtMillis == null }
                } finally { db.close() }
            }
            availableGroups = loadedGroups
            availableSpaces = loadedSpaces
            linkCatalogReady = true
        } catch (_: Exception) {
            linkCatalogError = "Could not verify Spaces or Groups. Existing links were not changed."
            linkCatalogReady = true
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BacklotBackground) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("BACKLOT / PERSONAL", color = BacklotPrimaryText,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close", color = BacklotAccent) }
            }
            if (fatalReadError) {
                Text(message.orEmpty(), modifier = Modifier.padding(18.dp),
                    color = BacklotError)
            } else if (!loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BacklotAccent)
                }
            } else {
                if (!creating && selected != null && records.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        records.forEach { item ->
                            OutlinedButton(
                                onClick = { chosenId = item.id; detailDay = null; message = null },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, if (selected.id == item.id) BacklotAccent else BacklotBorder),
                            ) { Text(item.activity, color = BacklotPrimaryText, maxLines = 1) }
                        }
                    }
                }
                if (creating || selected == null) {
                    CreateHabitReview(
                        title = title,
                        onTitle = { title = it.take(200); validationError = false; message = null },
                        date = startDate,
                        onDate = { startDate = it },
                        mode = alertKind,
                        onMode = { alertKind = it; message = null },
                        time = alertTime,
                        onTime = { alertTime = it },
                        error = validationError,
                        completionMode = completionMode,
                        onCompletionMode = { completionMode = it },
                        onStart = {
                            val clean = title.trim()
                            when {
                                saving -> Unit
                                clean.isBlank() -> validationError = true
                                else -> {
                                    val new = Phase1HabitChallenge(
                                        id = UUID.randomUUID().toString(),
                                        activity = clean,
                                        startDate = startDate,
                                        zoneId = zone.id,
                                        alertTime = if (alertKind == null) null else alertTime,
                                        alertKind = alertKind,
                                        completionMode = completionMode,
                                    ).checked()
                                    saving = true
                                    coroutineScope.launch {
                                        try {
                                            records = store.create(new)
                                            chosenId = new.id
                                            creating = false
                                            title = ""
                                            message = runCatching {
                                                Phase1HabitAlarmScheduler(context).reconcile()
                                            }.exceptionOrNull()?.let {
                                                "Challenge saved; check Android alert permissions."
                                            }
                                        } catch (_: Exception) {
                                            message = "Could not save the challenge. Existing progress was not changed."
                                        } finally {
                                            saving = false
                                        }
                                    }
                                }
                            }
                        },
                        previewOnly = false,
                        liveErrorMessage = message,
                    )
                } else {
                    if (message != null) {
                        Text(message.orEmpty(), color = BacklotError,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
                    }
                    FC13ChallengeDashboardPanel(
                        challenge = selected,
                        now = now,
                        linkedSpaceLabel = selected.linkedSpaceId?.let { id ->
                            availableSpaces.firstOrNull { it.id == id }?.title ?: "Linked Space"
                        },
                        linkedGroupLabel = selected.linkedGroupId?.let { id ->
                            availableGroups.firstOrNull { it.id == id }?.name ?: "Linked Group"
                        },
                        busy = saving,
                        store = store,
                        onBusy = { saving = it },
                        onRecords = { records = it },
                        onMessage = { message = it },
                        onDaySelected = { day ->
                            detailDay = day
                            detailNote = selected.dayNotes[day].orEmpty()
                        },
                        onManageLinks = {
                            linkSpaceId = selected.linkedSpaceId
                            linkGroupId = selected.linkedGroupId
                            linkDialog = true
                        },
                        onOpenLinkedSpace = onOpenLinkedSpace,
                    )
                }
            }
        }
    }
    val currentChallenge = records.firstOrNull { it.id == chosenId } ?: records.lastOrNull()
    if (linkDialog && currentChallenge != null) {
        val targetId = currentChallenge.id
        FC13ChallengeLinksDialog(
            saving = saving,
            ready = linkCatalogReady,
            errorMessage = linkCatalogError,
            spaces = availableSpaces,
            groups = availableGroups,
            spaceId = linkSpaceId,
            groupId = linkGroupId,
            onDismiss = { linkDialog = false },
            onSpaceChosen = { linkSpaceId = it },
            onGroupChosen = { linkGroupId = it },
            onSave = {
                val chosenSpace = linkSpaceId
                val chosenGroup = linkGroupId
                saving = true
                coroutineScope.launch {
                    try {
                        records = FC13SaveChallengeLinks(
                            context, store, groupStore, targetId,
                            chosenSpace, chosenGroup,
                        )
                        linkDialog = false
                        message = null
                    } catch (_: Exception) {
                        message = "Links were not saved. Please reopen the picker."
                    } finally {
                        saving = false
                    }
                }
            },
        )
    }
    detailDay?.let { day ->
        currentChallenge?.let { challenge ->
            FC13ChallengeDayDialog(
                challenge = challenge,
                day = day,
                now = now,
                note = detailNote,
                saving = saving,
                onDismiss = { detailDay = null },
                onNoteChanged = { detailNote = it.take(1500) },
                onBackfill = {
                    saving = true
                    coroutineScope.launch {
                        try {
                            records = store.confirmSelectedDay(
                                challenge.id, day, Instant.now(),
                            )
                            detailDay = null
                            message = runCatching {
                                Phase1HabitAlarmScheduler(context).reconcile()
                            }.exceptionOrNull()?.let {
                                "Past day confirmed; Android alert scheduling needs attention."
                            }
                        } catch (_: Exception) {
                            message = "Couldn't confirm this past day. Existing progress was kept."
                        } finally {
                            saving = false
                        }
                    }
                },
                onSaveNote = {
                    saving = true
                    coroutineScope.launch {
                        try {
                            records = store.saveDayNote(challenge.id, day, detailNote)
                            detailDay = null
                            message = null
                        } catch (_: Exception) {
                            message = "Day note could not be saved; no Done receipt changed."
                        } finally {
                            saving = false
                        }
                    }
                },
            )
        }
    }
}
