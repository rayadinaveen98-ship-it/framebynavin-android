package com.framebynavin.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.*
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.ui.theme.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * V223.2 real Personal UI. No fake CreatorTask; no project or YouTube writes.
 * This screen uses existing app-owned full screen (no conflicting nav insets).
 */
@Composable
internal fun V2232PersonalReminderHub(
    onClose: () -> Unit,
    onCreator: () -> Unit,
    onOpenActivity: () -> Unit = {},
    initialFocusReminderId: String? = null,
) {
    val context = LocalContext.current
    val store = remember(context) { PersonalReminderStore(context) }
    val commands = remember(context) { PersonalReminderCommands(context) }
    val personalLinkCreation = remember(context) { V310CreateAndLink(context) }
    val groupCatalog = remember(context) { V298ReminderGroupStore(context) }
    val coordinator = remember(context) { PersonalDeliveryCoordinator(context) }
    val followUpStore = remember(context) { PersonalFollowUpSettingsStore(context) }
    var followUpSettings by remember { mutableStateOf(PersonalFollowUpSettings()) }
    LaunchedEffect(followUpStore) {
        followUpStore.settings.collect { followUpSettings = it }
    }
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf(emptyList<PersonalReminder>()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PersonalReminder?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var availableEditorSpaces by remember { mutableStateOf(emptyList<V289SpaceEntity>()) }
    var availableEditorGroups by remember { mutableStateOf(emptyList<V298ReminderGroup>()) }
    var editorSpacesReady by remember { mutableStateOf(false) }
    var editorGroupsReady by remember { mutableStateOf(false) }
    var editorSpacesError by remember { mutableStateOf<String?>(null) }
    var editorGroupsError by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<PersonalReminder?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    // Recompute local calendar labels while this screen is visible; the next
    // occurrence journal and Android alarms remain authoritative.
    var agendaNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Reduce alarm-list fatigue: show the day first, but never hide action-needed items.
    var agendaScopeName by rememberSaveable { mutableStateOf(V285AgendaScope.TODAY.name) }
    var personalPageName by rememberSaveable { mutableStateOf("TODAY") }
    var activityFocusId by rememberSaveable { mutableStateOf<String?>(null) }
    // Activity Center deep links only focus a real Personal reminder row;
    // they do not mark it viewed, snoozed or completed.
    LaunchedEffect(initialFocusReminderId) {
        if (!initialFocusReminderId.isNullOrBlank()) {
            activityFocusId = initialFocusReminderId
            personalPageName = "TODAY"
            agendaScopeName = V285AgendaScope.ALL.name
        }
    }
    var spacesOpen by rememberSaveable { mutableStateOf(false) }
    var challengesOpen by rememberSaveable { mutableStateOf(false) }
    var reminderGroupsOpen by rememberSaveable { mutableStateOf(false) }
    // Page switches reset their viewport; the two layouts must never share a scroll offset.
    val personalPageScroll = remember(personalPageName) { ScrollState(0) }
    LaunchedEffect(editorOpen, editing?.id) {
        if (editorOpen && editing == null) {
            availableEditorSpaces = emptyList()
            availableEditorGroups = emptyList()
            editorSpacesReady = false
            editorGroupsReady = false
            editorSpacesError = null
            editorGroupsError = null
            val existingSpacesDb = context.getDatabasePath(V289SpacesDatabase.FILE_NAME)
            if (!existingSpacesDb.exists()) {
                // A reminder can be created with no Spaces. Opening the
                // optional chooser must not create a Room DB on first use.
                editorSpacesReady = true
            } else if (!existingSpacesDb.isFile) {
                editorSpacesError = "Spaces couldn't be read. You can still save this reminder."
            } else {
                val db = runCatching { V289SpacesDatabase.open(context) }
                if (db.isFailure) {
                    editorSpacesError = "Spaces couldn't be read. You can still save this reminder."
                } else {
                    val opened = db.getOrThrow()
                    try {
                        availableEditorSpaces = opened.spaces().loadSpaces()
                            .filter { it.archivedAtMillis == null }
                        editorSpacesReady = true
                    } catch (_: Exception) {
                        editorSpacesError = "Spaces couldn't be read. You can still save this reminder."
                    } finally {
                        opened.close()
                    }
                }
            }
            try {
                availableEditorGroups = groupCatalog.load()
                editorGroupsReady = true
            } catch (_: Exception) {
                editorGroupsError = "Groups couldn't be read. You can still save this reminder."
            }
        }
    }
    var completionEvents by remember { mutableStateOf(emptyList<PersonalCompletionEvent>()) }
    var completionHistoryReady by remember { mutableStateOf(false) }
    LaunchedEffect(store) {
        try {
            store.completionHistoryFlow.collect {
                completionEvents = it
                completionHistoryReady = true
            }
        } catch (_: Exception) {
            // Fail closed: never represent a broken history as a verified zero.
            completionHistoryReady = false
        }
    }
    BackHandler(enabled = personalPageName == "SETTINGS") {
        personalPageName = "TODAY"
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            agendaNowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(store) {
        try {
            store.remindersFlow.collect { records ->
                reminders = records.sortedWith(
                    compareBy<PersonalReminder> { it.paused }
                        .thenBy { it.pendingFireAtMillis.takeIf { time -> time > 0L }
                            ?: PersonalRecurrence.nextAfter(it, System.currentTimeMillis())
                            ?: Long.MAX_VALUE }
                )
                loaded = true
                loadError = null
            }
        } catch (error: Exception) {
            loadError = "Personal data couldn't be opened. Nothing was overwritten."
            loaded = true
        }
    }

    fun perform(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()
                infoMessage = null
            } catch (error: Exception) {
                infoMessage = error.message ?: "Could not save. Your existing reminders were kept."
            } finally {
                busy = false
            }
        }
    }

    var permissionRevision by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRevision++
                agendaNowMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionRevision++ }
    val notificationsReady = remember(permissionRevision, context) {
        PersonalReminderNotifications.canPost(context)
    }
    val preciseReady = remember(permissionRevision, context) {
        val am = context.getSystemService(AlarmManager::class.java)
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    CreatorAdaptiveFullScreen(onDismissRequest = onClose) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 11.dp, end = 15.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (personalPageName == "SETTINGS") personalPageName = "TODAY"
                    else onClose()
                },
                modifier = Modifier.testTag("v288-personal-toolbar-back"),
            ) {
                Icon(Icons.Outlined.ArrowBack,
                    contentDescription = if (personalPageName == "SETTINGS")
                        "Back to personal Today" else "Close reminders",
                    tint = ProjectorIvory)
            }
            BacklotReminderHubHeading(
                title = if (personalPageName == "TODAY") "My day" else "Personal settings",
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onOpenActivity,
                modifier = Modifier.testTag("v306-personal-open-activity"),
            ) {
                Icon(Icons.Outlined.NotificationsActive, "Open Activity Center", tint = MutedText)
            }
            IconButton(
                onClick = { personalPageName = if (personalPageName == "TODAY") "SETTINGS" else "TODAY" },
                modifier = Modifier.testTag("v288-personal-toggle-settings"),
            ) {
                Icon(if (personalPageName == "TODAY") Icons.Outlined.Settings
                     else Icons.Outlined.Home,
                     contentDescription = if (personalPageName == "TODAY") "Personal settings"
                         else "Back to personal Today",
                     tint = MutedText)
            }
            IconButton(
                enabled = !busy && loadError == null,
                onClick = { editing = null; editorOpen = true },
                modifier = Modifier.testTag("v2232-personal-add"),
            ) {
                Icon(Icons.Outlined.AddCircleOutline, "Create personal reminder", tint = ProjectorIvory)
            }
        }

        // Same real, shared Director's Cut tab surfaces as Creator. Keep the
        // original workspace and Spaces callbacks/test tags and distinct stores.
        BacklotReminderHubWorkspaceTabs(
            selected = BacklotReminderWorkspace.PERSONAL,
            onOpenPersonal = {},
            onOpenCreator = onCreator,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)
                .testTag("v2232-personal-space-switcher"),
        )
        if (personalPageName == "TODAY") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { spacesOpen = true },
                    enabled = loaded && loadError == null,
                    modifier = Modifier.testTag("v289-open-spaces"),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null,
                        tint = RecRed, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("My Spaces", color = RecRed, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(personalPageScroll)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (personalPageName == "SETTINGS") {
            if (loaded && loadError == null) {
                V295UniversalPersonalRoutines(
                    reminders = reminders,
                    enabled = !busy,
                    onCreateReminder = {
                        editing = null
                        editorOpen = true
                    },
                    onEditReminder = { reminder ->
                        editing = reminder
                        editorOpen = true
                    },
                    onOpenSpaces = { spacesOpen = true },
                    onOpenChallenges = { challengesOpen = true },
                    onOpenGroups = { reminderGroupsOpen = true },
                )
            }
            V288PersonalSettingsHeading(
                "Follow-ups", "Control quiet nudges for unanswered reminders")
            Surface(
                color = CinemaSurface,
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, CinemaLine),
                modifier = Modifier.fillMaxWidth().testTag("v2283-follow-up-settings"),
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("A gentle follow-up", color = ProjectorIvory,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Off by default · once per unanswered reminder · quiet 10 PM–8 AM",
                                color = MutedText, fontSize = 10.sp)
                        }
                        Switch(
                            checked = followUpSettings.enabled,
                            enabled = !busy,
                            onCheckedChange = { enabled ->
                                perform {
                                    followUpStore.setEnabled(enabled)
                                    coordinator.reconcile()
                                }
                            },
                            modifier = Modifier.testTag("v2283-follow-up-toggle"),
                        )
                    }
                    if (followUpSettings.enabled) {
                        Text("FOLLOW UP AFTER", color = MutedText,
                            fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(30, 60, 120).forEach { minutes ->
                                FilterChip(
                                    selected = followUpSettings.delayMinutes == minutes,
                                    enabled = !busy,
                                    onClick = {
                                        perform {
                                            followUpStore.setDelay(minutes)
                                            coordinator.reconcile()
                                        }
                                    },
                                    label = { Text("${minutes} min", fontSize = 10.sp) },
                                )
                            }
                        }
                    }
                }
            }

            if (!notificationsReady) {
                Surface(
                    color = CinemaSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .42f)),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text("Notifications need permission", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                        Text("Personal alerts cannot be shown until notifications are enabled.",
                            color = MutedText, fontSize = 11.sp)
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                            }
                        }) {
                            Text("ENABLE NOTIFICATIONS", color = RecRed)
                        }
                    }
                }
            } else if (!preciseReady) {
                Text("Exact timing is unavailable; Android may deliver personal alerts later.",
                    color = MutedText, fontSize = 11.sp)
            }
            // The standalone Personal media/ringing services are intentionally
            // not enabled: a selected Voice or Alarm is clearly labeled fallback.
            Text("Voice and Alarm attempt short audio playback when Android permits; notifications and response buttons stay available.",
                color = MutedText, fontSize = 10.sp,
                modifier = Modifier.testTag("v2232-personal-fallback-notice"))

            } else {
                if (loaded && loadError == null) {
                    val summary = remember(reminders, completionEvents, agendaNowMillis) {
                        V288PersonalTodayPolicy.snapshot(
                            reminders, completionEvents, agendaNowMillis
                        )
                    }
                    if (reminders.isNotEmpty() || summary.confirmedDoneToday > 0) {
                        V288PersonalTodayCard(
                        snapshot = summary,
                        nowMillis = agendaNowMillis,
                        busy = busy,
                        historyReady = completionHistoryReady,
                        onRespond = { item, action ->
                            perform {
                                val result = coordinator.apply(
                                    id = item.id,
                                    originalOccurrence = item.pendingOccurrenceMillis,
                                    token = item.pendingToken,
                                    action = action,
                                )
                                check(result == PersonalApplyResult.ACCEPTED) {
                                    "This reminder is no longer awaiting a response."
                                }
                            }
                        },
                        )
                    }
                    if (reminders.isNotEmpty() && !completionHistoryReady) {
                        Text("Progress history unavailable; no completions were inferred.",
                            color = MutedText, fontSize = 10.sp)
                    }
                    if (!notificationsReady) {
                        Surface(
                            onClick = { personalPageName = "SETTINGS" },
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, RecRed.copy(alpha = .45f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("v288-permission-warning"),
                        ) {
                            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.WarningAmber, null, tint = RecRed,
                                    modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Notifications blocked · Open Personal settings",
                                    color = ProjectorIvory, fontSize = 12.sp)
                            }
                        }
                    }
                }
            if (loadError != null) {
                Text(loadError.orEmpty(), color = RecRed,
                    modifier = Modifier.testTag("v2232-personal-load-error"))
            } else if (!loaded) {
                CircularProgressIndicator(color = RecRed)
            } else if (reminders.isEmpty()) {
                Surface(
                    color = CinemaSurface, shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                    modifier = Modifier.fillMaxWidth().testTag("v2232-personal-empty"),
                ) {
                    Column(Modifier.padding(23.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.NotificationsNone, null, tint = MutedText,
                            modifier = Modifier.size(30.dp))
                        Text("No reminders yet", color = ProjectorIvory,
                            fontWeight = FontWeight.Bold)
                        Button(onClick = { editing = null; editorOpen = true },
                            modifier = Modifier.testTag("v2232-personal-empty-add"),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                            Text("ADD REMINDER")
                        }
                    }
                }
            } else {
                Text("YOUR REMINDERS · ${reminders.count { !it.paused }} ACTIVE · ${reminders.count { it.paused }} PAUSED",
                    color = MutedText, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.testTag("v2232-personal-count"))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .testTag("v285-agenda-scope-selector"),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    V285AgendaScope.entries.forEach { scope ->
                        FilterChip(
                            selected = agendaScopeName == scope.name,
                            onClick = { agendaScopeName = scope.name },
                            label = { Text(scope.label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("v285-agenda-scope-" + scope.name),
                        )
                    }
                }
                if (activityFocusId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("v335-focused-personal-row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("REMINDER FROM ACTIVITY", color = RecRed,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { activityFocusId = null }) {
                            Text("SHOW ALL", color = ProjectorIvory)
                        }
                    }
                }
                val filteredForActivity = if (activityFocusId == null) reminders
                    else reminders.filter { it.id == activityFocusId }
                val agendaGroups = remember(filteredForActivity, agendaNowMillis, agendaScopeName) {
                    V285PersonalAgendaFilter.groups(
                        filteredForActivity, agendaNowMillis, V285AgendaScope.valueOf(agendaScopeName)
                    )
                }
                val visibleCount = agendaGroups.sumOf { it.items.size }
                Text("$visibleCount shown · choose ALL for every saved reminder",
                    color = MutedText, fontSize = 10.sp,
                    modifier = Modifier.testTag("v285-agenda-count"))
                if (agendaGroups.isEmpty()) {
                    Text("No reminders in this time window. Select ALL to see " +
                        "other upcoming or paused reminders.", color = MutedText, fontSize = 12.sp)
                }
                agendaGroups.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .testTag("v2282-personal-section-${group.section.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(group.section.label, color = if (group.section == PersonalAgendaSection.NEEDS_ATTENTION)
                            RecRed else MutedText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${group.items.size}", color = MutedText, fontSize = 11.sp)
                    }
                    group.items.forEach { item ->
                        V2232PersonalItem(
                            reminder = item,
                            nowMillis = agendaNowMillis,
                            busy = busy,
                            expandedInitially = item.id == activityFocusId,
                            onEdit = { editing = item; editorOpen = true },
                            onPause = { perform { commands.setPaused(item.id, !item.paused) } },
                            onDelete = { deleteTarget = item },
                            onRespond = { action ->
                                perform {
                                    val result = coordinator.apply(
                                        id = item.id, originalOccurrence = item.pendingOccurrenceMillis,
                                        token = item.pendingToken, action = action,
                                    )
                                    check(result == PersonalApplyResult.ACCEPTED) {
                                        "This notification is no longer active. Reopen the reminder."
                                    }
                                }
                            },
                        )
                    }
                }
            }
            }
            if (infoMessage != null) {
                Text(infoMessage.orEmpty(), color = RecRed,
                    modifier = Modifier.testTag("v2232-personal-feedback"))
            }
        }
    }

    if (reminderGroupsOpen) {
        V298ReminderGroupsScreen(
            onClose = { reminderGroupsOpen = false },
            onEditReminder = { id ->
                reminders.firstOrNull { it.id == id }?.let { reminder ->
                    editing = reminder
                    reminderGroupsOpen = false
                    editorOpen = true
                }
            },
        )
    }

    if (challengesOpen) {
        Phase1HabitLocalStoreReview(
            onClose = { challengesOpen = false },
            onOpenLinkedSpace = { _ ->
                challengesOpen = false
                spacesOpen = true
            },
        )
    }

    if (spacesOpen) {
        V289PersonalSpacesScreen(
            onClose = { spacesOpen = false },
            onEditReminder = { id ->
                reminders.firstOrNull { it.id == id }?.let { reminder ->
                    editing = reminder
                    spacesOpen = false
                    editorOpen = true
                }
            },
        )
    }

    if (editorOpen) {
        fun saveDraft(draft: PersonalReminderDraft, targets: V310LinkTargets) {
            perform {
                val oldId = editing?.id
                if (oldId == null) {
                    // Verify only owner-visible Space/Group selections. No
                    // cross-store write occurs before the reminder is saved.
                    val validated = targets.checkedAgainst(
                        validSpaceIds = availableEditorSpaces.map { it.id }.toSet(),
                        validGroupIds = availableEditorGroups.map { it.id }.toSet(),
                    )
                    val result = personalLinkCreation.create(draft, validated)
                    editorOpen = false
                    editing = null
                    if (result.linkProblems.isNotEmpty() || result.deliveryNeedsAttention) {
                        // The ID is committed in Personal DataStore. Never
                        // imply Save failed or invite a duplicate reminder.
                        val problems = buildList {
                            if (result.deliveryNeedsAttention)
                                add("Android alert scheduling or widget refresh needs attention")
                            addAll(result.linkProblems)
                        }
                        val advice = buildList {
                            if (result.deliveryNeedsAttention)
                                add("Check the reminder and Android alert permissions.")
                            if (result.linkProblems.isNotEmpty())
                                add("Open My Spaces or Reminder Groups to finish linking.")
                        }
                        throw IllegalStateException(
                            "Reminder saved, but ${problems.joinToString(" and ")}. " +
                                advice.joinToString(" ")
                        )
                    }
                } else {
                    // Existing reminder links are managed via the original
                    // Space/Group tools and are never silently removed by edit.
                    commands.revise(oldId, draft)
                    editorOpen = false
                    editing = null
                }
            }
        }
        V2232PersonalEditor(
            existing = editing,
            saving = busy,
            availableSpaces = availableEditorSpaces,
            availableGroups = availableEditorGroups,
            spacesReady = editorSpacesReady,
            groupsReady = editorGroupsReady,
            spacesError = editorSpacesError,
            groupsError = editorGroupsError,
            onSaveWithLinks = ::saveDraft,
            onDismiss = { if (!busy) editorOpen = false },
            onSave = { draft -> saveDraft(draft, V310LinkTargets()) },
        )
    }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteTarget = null },
            title = { Text("Delete personal reminder?") },
            text = { Text("This removes only this personal reminder. Creator projects stay unchanged.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    perform {
                        commands.delete(item.id)
                        deleteTarget = null
                    }
                }) { Text("DELETE", color = RecRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun V2232PersonalItem(
    reminder: PersonalReminder,
    nowMillis: Long,
    busy: Boolean,
    expandedInitially: Boolean = false,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onRespond: (String) -> Unit,
) {
    val deliveryHealth = PersonalDeliveryHealthPolicy.status(reminder, nowMillis)
    val due = PersonalAgendaPolicy.dueAtMillis(reminder, nowMillis)
    val whenLabel = due?.let {
        Instant.ofEpochMilli(it).atZone(reminder.zoneId)
            .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault()))
    } ?: "No upcoming occurrence"
    var expanded by rememberSaveable(reminder.id) { mutableStateOf(expandedInitially) }
    LaunchedEffect(expandedInitially) { if (expandedInitially) expanded = true }
    val actionReady = V288PersonalTodayPolicy.canRespond(reminder)
    val needsAttention = PersonalAgendaPolicy.section(reminder, nowMillis) ==
        PersonalAgendaSection.NEEDS_ATTENTION
    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (needsAttention) RecRed.copy(alpha = .55f)
            else CinemaLine),
        modifier = Modifier.fillMaxWidth().testTag("v2232-personal-item"),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V288PersonalReminderIcon(reminder)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(reminder.title, color = ProjectorIvory, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            reminder.paused -> "Paused"
                            needsAttention -> "Needs attention · " + whenLabel
                            else -> whenLabel
                        },
                        color = if (needsAttention) RecRed else MutedText,
                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("v288-personal-expand"),
                ) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess
                        else Icons.Outlined.MoreVert,
                        contentDescription = if (expanded) "Hide reminder details"
                            else "Show reminder options",
                        tint = MutedText)
                }
            }
            if (actionReady) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { onRespond(PersonalDeliveryContract.ACTION_DONE) },
                        modifier = Modifier.testTag("v288-row-done"),
                    ) { Text("DONE", fontSize = 11.sp) }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { onRespond(PersonalDeliveryContract.ACTION_SNOOZE) },
                        modifier = Modifier.testTag("v288-row-snooze"),
                    ) { Text("SNOOZE ${reminder.snoozeMinutes}m", fontSize = 11.sp) }
                }
            }
            if (expanded) {
                if (reminder.notes.isNotBlank()) {
                    Text(reminder.notes, color = ProjectorIvory, fontSize = 12.sp,
                        maxLines = 4, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("v284-plan-meal-details"))
                }
                Text("${reminder.repeat.name.replace('_', ' ')} · ${reminder.alert.name}",
                    color = MutedText, fontSize = 10.sp)
                Text(deliveryHealth.label,
                    color = if (deliveryHealth.needsAttention) RecRed else MutedText,
                    fontSize = 11.sp,
                    modifier = Modifier.testTag("v2282-personal-delivery-health"))
                if (reminder.lastMissedOccurrenceMillis > reminder.lastAcknowledgedOccurrenceMillis) {
                    Text("An earlier alert received no response",
                        color = RecRed, fontSize = 10.sp,
                        modifier = Modifier.testTag("v2234-personal-missed"))
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextButton(enabled = !busy, onClick = onEdit) {
                        Text("EDIT", color = ProjectorIvory)
                    }
                    TextButton(enabled = !busy, onClick = onPause) {
                        Text(if (reminder.paused) "RESUME" else "PAUSE",
                            color = MutedText)
                    }
                    TextButton(enabled = !busy, onClick = onDelete) {
                        Text("DELETE", color = RecRed)
                    }
                }
            }
        }
    }
}

@Composable
internal fun V2232PersonalEditor(
    existing: PersonalReminder?,
    saving: Boolean,
    onDismiss: () -> Unit,
    availableSpaces: List<V289SpaceEntity> = emptyList(),
    availableGroups: List<V298ReminderGroup> = emptyList(),
    spacesReady: Boolean = true,
    groupsReady: Boolean = true,
    spacesError: String? = null,
    groupsError: String? = null,
    onSaveWithLinks: ((PersonalReminderDraft, V310LinkTargets) -> Unit)? = null,
    onSave: (PersonalReminderDraft) -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val selectedStart = remember(existing?.id) {
        existing?.startDate?.atTime(existing.time)?.atZone(existing.zoneId)?.toInstant()?.toEpochMilli()
            ?: (System.currentTimeMillis() + 60 * 60_000L)
    }
    var selectedTime by rememberSaveable(existing?.id) { mutableLongStateOf(selectedStart) }
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var purpose by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.purpose ?: PersonalPurpose.CUSTOM)
    }
    var repeat by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.repeat?.name ?: PersonalRepeat.ONCE.name)
    }
    var alert by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.alert?.name ?: PersonalAlert.NOTIFICATION.name)
    }
    var snoozeMinutes by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.snoozeMinutes ?: 10)
    }
    var personalVoiceCount by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.voiceRepeatCount ?: 3)
    }
    var personalVoiceGap by rememberSaveable(existing?.id) {
        mutableLongStateOf(existing?.voiceRepeatGapMillis ?: 2_500L)
    }
    var personalAlarmSeconds by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.alarmDurationSeconds ?: 60)
    }
    var automaticRetry by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.autoRetryEnabled ?: false)
    }
    var automaticRetryMinutes by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.autoRetryMinutes ?: 5)
    }
    var automaticRetryLimit by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.autoRetryMaxAttempts ?: 2)
    }
    var voiceMusicMode by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.voiceMusicMode ?: com.framebynavin.app.reminders.ReminderMusicMode.PIANO)
    }
    var voiceMusicUri by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.voiceMusicUri.orEmpty())
    }
    var daySelection by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.weeklyDays?.joinToString(",") { it.name }
            ?: Instant.ofEpochMilli(selectedStart).atZone(zone).dayOfWeek.name)
    }
    var everyNDays by rememberSaveable(existing?.id) { mutableStateOf((existing?.everyNDays ?: 3).toString()) }
    var everyNHours by rememberSaveable(existing?.id) {
        mutableStateOf((existing?.everyNHours ?: 3).toString())
    }
    var hourlyUntil by rememberSaveable(existing?.id) {
        mutableStateOf((existing?.hourlyUntil ?: LocalTime.of(23, 59)).toString())
    }
    // The creation flow progressively discloses details; editing retains the
    // familiar all-fields layout until the guided editor passes owner QA.
    // This step is UI-only and never schedules a notification before Save.
    var composerStep by rememberSaveable(existing?.id) {
        mutableIntStateOf(if (existing == null) 0 else -1)
    }
    var advancedOptionsOpen by rememberSaveable(existing?.id) {
        mutableStateOf(existing != null)
    }
    // Suggestions are optional inspiration, not a required setup screen.
    // Persist their disclosure during rotations, never alter reminder data.
    var suggestionsOpen by rememberSaveable(existing?.id) {
        mutableStateOf(false)
    }
    // Notes are optional; keep the first step compact. Never clear typed notes on collapse.
    var notesOpen by rememberSaveable(existing?.id) {
        mutableStateOf(existing != null)
    }
    val composerScroll = remember(composerStep) { ScrollState(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedSpaceId by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }
    var selectedGroupId by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }
    var fullScreenAllowed by remember { mutableStateOf(
        com.framebynavin.app.reminders.V268FullScreenReminderAccess.allowed(context)
    ) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fullScreenAllowed =
                    com.framebynavin.app.reminders.V268FullScreenReminderAccess.allowed(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val repeatOptions = PersonalRepeat.entries
    val alertOptions = PersonalAlert.entries
    val date = Instant.ofEpochMilli(selectedTime).atZone(zone)
    val weekDays = remember(daySelection) {
        daySelection.split(",").mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
    }

    // V227.3: avoid a second Android Dialog window. On API 35, a separately
    // inset Dialog can shift its Cancel button outside the display despite
    // the Compose root measuring it inside its own bounds. The existing
    // Activity-owned overlay handles safe drawing and IME exactly once.
    CreatorAdaptiveFullScreen(
        onDismissRequest = {
            if (composerStep > 0) { composerStep--; error = null }
            else onDismiss()
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
                .testTag("v2232-personal-editor"),
            color = CinemaSurfaceRaised,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (existing == null) "NEW PERSONAL REMINDER" else "EDIT PERSONAL REMINDER",
                            color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (existing == null) "Make it yours" else "Your personal rhythm",
                            color = ProjectorIvory, fontSize = 19.sp,
                            fontWeight = FontWeight.Bold)
                        if (composerStep >= 0) Text(
                            "STEP ${composerStep + 1} OF 4  ·  " +
                                listOf("Purpose", "Schedule", "Alert", "Review")[composerStep],
                            color = MutedText, fontSize = 12.sp,
                            modifier = Modifier.testTag("v296-composer-progress"),
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close editor", tint = ProjectorIvory)
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(composerScroll),
                    verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    AnimatedVisibility(
                        visible = composerStep == 0 || composerStep < 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    TextButton(
                        onClick = { suggestionsOpen = !suggestionsOpen },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("phase1-personal-toggle-suggestions"),
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null,
                            tint = RecRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (suggestionsOpen) "Hide Suggestions" else "Show Suggestions",
                            color = RecRed, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    if (!suggestionsOpen && purpose != PersonalPurpose.CUSTOM) {
                        Text(
                            "Purpose: " + purpose.label,
                            color = MutedText, fontSize = 11.sp,
                        )
                    }
                    AnimatedVisibility(
                        visible = suggestionsOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        if (existing == null) {
                            Text("START WITH A ROUTINE", color = MutedText,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    .testTag("v288-quick-suggestions"),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                V288ReminderSuggestion.entries.forEach { suggestion ->
                                    FilterChip(
                                        selected = title == suggestion.title,
                                        onClick = {
                                            // Suggestions classify, not schedule. Never replace
                                            // the user's selected time, recurrence or alert type.
                                            if (title.isBlank()) title = suggestion.title
                                            purpose = suggestion.purpose
                                            error = null
                                        },
                                        label = { Text(suggestion.label, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("v288-suggestion-" + suggestion.name),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = BacklotSurface,
                                            labelColor = BacklotSecondaryText,
                                            selectedContainerColor = BacklotSurfaceSelected,
                                            selectedLabelColor = BacklotPrimaryText,
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (title == suggestion.title) BacklotAccent else BacklotBorder,
                                        ),
                                    )
                                }
                            }
                            Text("Set your own title, time and repeat. Nothing is scheduled until Save.",
                                color = MutedText, fontSize = 12.sp)
                        }
                            V274PersonalPurposePicker(
                                selected = purpose,
                                onSelected = { selected ->
                                    title = PersonalSelfCarePresets.chooseTitle(title, purpose, selected)
                                    purpose = selected
                                    error = null
                                },
                            )
                        }
                    }
                    Text("YOUR REMINDER", color = MutedText,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title, onValueChange = { title = it.take(180); error = null },
                        label = { Text("What to remember?") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("v2232-personal-title"),
                    )
                    TextButton(
                        onClick = { notesOpen = !notesOpen },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("fc13-personal-toggle-notes"),
                    ) {
                        Icon(Icons.Outlined.Notes, null, tint = RecRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (notesOpen) "Hide notes" else if (notes.isNotBlank()) "Edit notes"
                            else "Add notes (optional)",
                            color = RecRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    AnimatedVisibility(
                        visible = notesOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        OutlinedTextField(
                            value = notes, onValueChange = { notes = it.take(2000) },
                            label = { Text("Optional notes") }, minLines = 2, maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                                .testTag("fc13-personal-notes-field"),
                        )
                    }
                    }
                    }
                    AnimatedVisibility(
                        visible = composerStep == 1 || composerStep < 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("WHEN SHOULD IT HAPPEN?", color = MutedText,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = {
                        v2232PickDateTime(context, selectedTime) { selectedTime = it; error = null }
                    }, modifier = Modifier.fillMaxWidth().testTag("v2232-personal-date")) {
                        Icon(Icons.Outlined.Event, null)
                        Spacer(Modifier.width(7.dp))
                        Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a")))
                    }
                    Text("REPEAT", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        repeatOptions.forEach { choice ->
                            FilterChip(selected = repeat == choice.name,
                                onClick = {
                                    if (repeat != choice.name && choice == PersonalRepeat.EVERY_N_HOURS) {
                                        daySelection = "" // Empty means all seven days, not today only.
                                        everyNDays = "1"
                                    }
                                    if (repeat != choice.name && choice == PersonalRepeat.WEEKLY &&
                                        daySelection.isEmpty()) daySelection = date.dayOfWeek.name
                                    repeat = choice.name
                                },
                                label = { Text(choice.name.replace('_', ' '), fontSize = 10.sp) })
                        }
                    }
                    if (repeat == PersonalRepeat.WEEKLY.name || repeat == PersonalRepeat.EVERY_N_HOURS.name) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            DayOfWeek.values().forEach { day ->
                                FilterChip(selected = day in weekDays,
                                    onClick = {
                                        val next = if (day in weekDays) weekDays - day else weekDays + day
                                        daySelection = next.sortedBy { it.value }.joinToString(",") { it.name }
                                    },
                                    label = { Text(day.name.take(3), fontSize = 10.sp) })
                            }
                        }
                    }
                    if (repeat == PersonalRepeat.EVERY_N_HOURS.name) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = weekDays.isEmpty(),
                                onClick = { daySelection = "" },
                                label = { Text("Every day", fontSize = 11.sp) },
                            )
                            FilterChip(
                                selected = weekDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                                onClick = { daySelection = "SATURDAY,SUNDAY" },
                                label = { Text("Weekends", fontSize = 11.sp) },
                            )
                            FilterChip(
                                selected = weekDays == setOf(
                                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                                ),
                                onClick = { daySelection = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY" },
                                label = { Text("Weekdays", fontSize = 11.sp) },
                            )
                        }
                        OutlinedTextField(
                            value = everyNHours,
                            onValueChange = { everyNHours = it.filter(Char::isDigit).take(2) },
                            label = { Text("Repeat every N hours (1–24)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val until = runCatching { LocalTime.parse(hourlyUntil) }
                            .getOrDefault(LocalTime.of(23, 59))
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, hour, minute ->
                                    hourlyUntil = LocalTime.of(hour, minute).toString()
                                }, until.hour, until.minute, false).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.AccessTime, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Daily active window ends at " +
                                until.format(DateTimeFormatter.ofPattern("h:mm a")))
                        }
                    }
                    if (repeat == PersonalRepeat.EVERY_N_DAYS.name ||
                        repeat == PersonalRepeat.EVERY_N_HOURS.name) {
                        OutlinedTextField(
                            value = everyNDays,
                            onValueChange = { everyNDays = it.filter(Char::isDigit).take(3) },
                            label = {
                                Text(if (repeat == PersonalRepeat.EVERY_N_HOURS.name)
                                    "Repeat these days every N days (1–365)"
                                else "Every how many days? (1–365)")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (repeat == PersonalRepeat.EVERY_N_HOURS.name) {
                        val previews = remember(selectedTime, daySelection, everyNHours,
                            everyNDays, hourlyUntil) {
                            v2232PreviewUpcomingHourly(
                                selectedTime, zone, title, weekDays,
                                everyNDays, everyNHours, hourlyUntil,
                            )
                        }
                        Text("NEXT FIVE REMINDERS", color = MutedText,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (previews.isEmpty()) Text(
                            "Check hourly interval, days and active window.",
                            color = MutedText, fontSize = 11.sp)
                        else previews.forEach { next ->
                            Text(
                                Instant.ofEpochMilli(next).atZone(zone).format(
                                    DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")),
                                color = ProjectorIvory, fontSize = 12.sp)
                        }
                    }
                    }
                    }
                    AnimatedVisibility(
                        visible = composerStep == 2 || composerStep < 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("HOW SHOULD BACKLOT REACH YOU?", color = MutedText,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        alertOptions.forEach { choice ->
                            FilterChip(selected = alert == choice.name,
                                onClick = { alert = choice.name },
                                label = { Text(choice.name, fontSize = 10.sp) })
                        }
                    }
                    if (composerStep == 2) {
                        Text("Snooze defaults to ${snoozeMinutes} minutes. Voice uses " +
                            "${personalVoiceCount} announcements and your selected music.",
                            color = MutedText, fontSize = 12.sp)
                        TextButton(
                            onClick = { advancedOptionsOpen = !advancedOptionsOpen },
                            modifier = Modifier.testTag("v296-composer-advanced"),
                        ) {
                            Text(if (advancedOptionsOpen) "HIDE SOUND & SNOOZE OPTIONS"
                                else "CUSTOMIZE SOUND & SNOOZE")
                        }
                    }
                    AnimatedVisibility(
                        visible = advancedOptionsOpen || composerStep < 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("SNOOZE DURATION", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(5, 10, 15, 30).forEach { minutes ->
                            FilterChip(selected = snoozeMinutes == minutes,
                                onClick = { snoozeMinutes = minutes },
                                label = { Text("${minutes} min", fontSize = 11.sp) })
                        }
                    }
                    if (alert != PersonalAlert.NOTIFICATION.name) {
                        Text("Voice and Alarm play short audio when Android allows; the notification remains your fallback.",
                            color = MutedText, fontSize = 11.sp)
                    }
                    if (alert == PersonalAlert.ALARM.name) {
                        Text(
                            if (fullScreenAllowed)
                                "When your screen is locked, Personal Alarm can open full screen. " +
                                    "While unlocked, Android usually shows a heads-up notification."
                            else "Full-screen alerts are not permitted. " +
                                    "Your alarm notification and Done/Snooze actions still work.",
                            color = MutedText, fontSize = 11.sp,
                            modifier = Modifier.testTag("v279-personal-fullscreen-explanation"),
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                            !fullScreenAllowed) {
                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                        )
                                    }.onFailure { error = "Open Android settings and enable Full-screen alerts for Backlot." }
                                },
                                modifier = Modifier.testTag("v279-personal-fsi-settings"),
                            ) { Text("Full-screen alert settings") }
                        }
                    }
                    if (alert == PersonalAlert.VOICE.name) {
                        Text("VOICE ANNOUNCEMENTS", color = MutedText,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(1, 2, 3).forEach { times ->
                                FilterChip(selected = personalVoiceCount == times,
                                    onClick = { personalVoiceCount = times },
                                    label = { Text(if (times == 1) "Once" else "$times times", fontSize = 11.sp) })
                            }
                        }
                        Text("PAUSE BETWEEN ANNOUNCEMENTS", color = MutedText,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(2_500L, 5_000L, 10_000L, 20_000L, 30_000L, 60_000L).forEach { millis ->
                                FilterChip(selected = personalVoiceGap == millis,
                                    onClick = { personalVoiceGap = millis },
                                    label = { Text(if (millis == 2_500L) "2.5s" else "${millis / 1_000}s", fontSize = 11.sp) })
                            }
                        }
                        V273ReminderMusicPicker(
                            mode = voiceMusicMode,
                            uri = voiceMusicUri,
                            onChanged = { mode, picked ->
                                voiceMusicMode = mode
                                voiceMusicUri = picked
                            },
                            modifier = Modifier.fillMaxWidth().testTag("v273-personal-music"),
                        )
                    }
                    if (alert == PersonalAlert.ALARM.name) {
                        Text("STOP ALARM SOUND AFTER", color = MutedText,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(30, 45, 60, 120, 180, 300).forEach { seconds ->
                                FilterChip(selected = personalAlarmSeconds == seconds,
                                    onClick = { personalAlarmSeconds = seconds },
                                    label = { Text(if (seconds < 60) "${seconds}s" else "${seconds / 60}m", fontSize = 11.sp) })
                            }
                        }
                    }
                    if (alert == PersonalAlert.VOICE.name || alert == PersonalAlert.ALARM.name) {
                        Row(
                            modifier = Modifier.fillMaxWidth().testTag("v282-personal-auto-retry-toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("REPEAT IF I DON'T RESPOND", color = ProjectorIvory, fontSize = 13.sp)
                                Text("Off by default. Only after sound finishes; Done, Dismiss or Snooze cancels it.",
                                    color = MutedText, fontSize = 11.sp)
                            }
                            Switch(checked = automaticRetry, onCheckedChange = { automaticRetry = it })
                        }
                        if (automaticRetry) {
                            Text("RE-ALERT AFTER", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(5, 10, 15, 30).forEach { minutes ->
                                    FilterChip(selected = automaticRetryMinutes == minutes,
                                        onClick = { automaticRetryMinutes = minutes },
                                        label = { Text("${minutes} min", fontSize = 11.sp) })
                                }
                            }
                            Text("MAXIMUM EXTRA ALERTS", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(1, 2, 3).forEach { count ->
                                    FilterChip(selected = automaticRetryLimit == count,
                                        onClick = { automaticRetryLimit = count },
                                        label = { Text("$count", fontSize = 11.sp) })
                                }
                            }
                            Text("Android may delay background alerts when exact alarm access is unavailable.",
                                color = MutedText, fontSize = 11.sp)
                        }
                    }
                    }
                    }
                    }
                    }
                    AnimatedVisibility(
                        visible = composerStep == 3 || composerStep < 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("v274-personal-review"),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Column(Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("REVIEW BEFORE SAVING", color = MutedText, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                            Text(title.trim().ifBlank { "Unnamed reminder" },
                                color = ProjectorIvory, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold)
                            if (notes.isNotBlank()) Text(notes.trim(),
                                color = MutedText, fontSize = 12.sp)
                            Text(
                                PersonalSelfCarePresets.scheduleLabel(
                                    PersonalRepeat.valueOf(repeat),
                                    everyNHours.toIntOrNull() ?: 3,
                                    everyNDays.toIntOrNull() ?: 1,
                                    weekDays,
                                ),
                                color = ProjectorIvory, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")) +
                                    " · " + PersonalAlert.valueOf(alert).name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                color = MutedText, fontSize = 12.sp,
                            )
                            Text("Snooze: ${snoozeMinutes} minutes" +
                                    if (alert == PersonalAlert.VOICE.name)
                                        " · Voice repeats: ${personalVoiceCount}" else "",
                                color = MutedText, fontSize = 12.sp)
                            Text(
                                "Preview only. Android battery and notification settings may delay alerts.",
                                color = MutedText, fontSize = 11.sp,
                            )
                        }
                    }
                    if (composerStep == 3 && existing == null) {
                        V310ReviewLinks(
                            availableSpaces = availableSpaces,
                            availableGroups = availableGroups,
                            spacesReady = spacesReady,
                            groupsReady = groupsReady,
                            spacesError = spacesError,
                            groupsError = groupsError,
                            value = V310LinkTargets(selectedSpaceId, selectedGroupId),
                            onChange = {
                                selectedSpaceId = it.spaceId
                                selectedGroupId = it.groupId
                            },
                        )
                    }
                    }
                    }
                    error?.let {
                        Text(it, color = RecRed, modifier = Modifier.testTag("v2232-editor-error"))
                    }
                }
                if (composerStep in 0..2) {
                    V296PersonalStepActions(
                        step = composerStep,
                        saving = saving,
                        onBack = {
                            if (composerStep == 0) onDismiss()
                            else { composerStep--; error = null }
                        },
                        onContinue = {
                            if (composerStep == 0 && title.isBlank()) {
                                error = "Give your reminder a name to continue."
                            } else {
                                error = null
                                composerStep++
                            }
                        },
                    )
                } else {
                    if (composerStep == 3) TextButton(
                        onClick = { composerStep = 2; error = null },
                        modifier = Modifier.fillMaxWidth().testTag("v296-composer-review-back"),
                    ) { Text("BACK TO ALERTS") }
                // V227.1: keyboard-safe, adaptive actions; retains all validation.
                V2271PersonalEditorFooter(
                    saving = saving,
                    onCancel = onDismiss,
                        onSave = {
                            try {
                                val chosen = Instant.ofEpochMilli(selectedTime).atZone(zone)
                                val draft = PersonalReminderDraft(
                                    title = title.trim(),
                                    notes = notes.trim(),
                                    startDate = chosen.toLocalDate(),
                                    time = chosen.toLocalTime().withSecond(0).withNano(0),
                                    zoneId = zone,
                                    repeat = PersonalRepeat.valueOf(repeat),
                                    weeklyDays = if (repeat == PersonalRepeat.WEEKLY.name ||
                                        repeat == PersonalRepeat.EVERY_N_HOURS.name)
                                        weekDays else emptySet(),
                                    everyNDays = everyNDays.toIntOrNull() ?: 0,
                                    everyNHours = everyNHours.toIntOrNull() ?: 0,
                                    hourlyUntil = LocalTime.parse(hourlyUntil),
                                    purpose = purpose,
                                    alert = PersonalAlert.valueOf(alert),
                                    voiceMusicMode = voiceMusicMode,
                                    voiceMusicUri = voiceMusicUri,
                                    snoozeMinutes = snoozeMinutes,
                                    voiceRepeatCount = personalVoiceCount,
                                    voiceRepeatGapMillis = personalVoiceGap,
                                    alarmDurationSeconds = personalAlarmSeconds,
                                    autoRetryEnabled = automaticRetry && alert != PersonalAlert.NOTIFICATION.name,
                                    autoRetryMinutes = automaticRetryMinutes,
                                    autoRetryMaxAttempts = automaticRetryLimit,
                                ).checked()
                                val requestedLinks = V310LinkTargets(
                                    spaceId = if (existing == null) selectedSpaceId else null,
                                    groupId = if (existing == null) selectedGroupId else null,
                                )
                                if (onSaveWithLinks != null) onSaveWithLinks(draft, requestedLinks)
                                else onSave(draft)
                            } catch (invalid: Exception) {
                                error = invalid.message ?: "Check the reminder details."
                            }
                },
                )
                }
            }
        }
    }
}

private fun v2232PickDateTime(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {
    val initial = Calendar.getInstance().apply { timeInMillis = currentMillis }
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            val chosen = Calendar.getInstance().apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onPicked(chosen)
        }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
    }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
}

/**
 * Extracted from the Personal reminder Compose editor.
 * Keep preview-only recurrence inference out of the editor's already large
 * data-flow graph; this function neither saves nor schedules anything.
 */
private fun v2232PreviewUpcomingHourly(
    selectedTime: Long,
    zone: ZoneId,
    title: String,
    weekDays: Set<DayOfWeek>,
    everyNDays: String,
    everyNHours: String,
    hourlyUntil: String,
): List<Long> {
    return try {
        val chosen = Instant.ofEpochMilli(selectedTime).atZone(zone)
        val example = PersonalReminder(
            title = title.trim().ifBlank { "Personal reminder" },
            startDate = chosen.toLocalDate(),
            time = chosen.toLocalTime().withSecond(0).withNano(0),
            zoneId = zone,
            repeat = PersonalRepeat.EVERY_N_HOURS,
            weeklyDays = weekDays,
            everyNDays = everyNDays.toInt(),
            everyNHours = everyNHours.toInt(),
            hourlyUntil = LocalTime.parse(hourlyUntil),
        ).checked()
        val items = mutableListOf<Long>()
        var after = System.currentTimeMillis()
        repeat(5) {
            val next = PersonalRecurrence.nextAfter(example, after)
                ?: return@repeat
            items.add(next)
            after = next
        }
        items
    } catch (_: Exception) {
        emptyList()
    }
}
