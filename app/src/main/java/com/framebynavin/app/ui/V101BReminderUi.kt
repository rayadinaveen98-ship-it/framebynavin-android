package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.SmartEscalationConfig
import com.framebynavin.app.reminders.SmartEscalationConfigStore
import com.framebynavin.app.reminders.SmartEscalationPolicy
import com.framebynavin.app.reminders.SmartSessionStore
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class PProjectDraft(
    val title: String,
    val platform: String,
    val contentType: String,
    val dueAtMillis: Long,
    val attentionPlan: ProjectAttentionPlan,
    val mode: ReminderMode,
    val reminderAtMillis: Long,
    val deliveryPreference: ReminderDeliveryPreference,
    val priority: TaskPriority,
    val notes: String,
    val alarmSoundUri: String,
    val voicePersona: VoicePersona,
    val voiceRepeatCount: Int,
    val voiceRepeatIntervalSeconds: Int,
    val alarmTimeoutSeconds: Int,
)

@Composable
internal fun PReminderCenter(
    tasks: List<CreatorTask>,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val context = LocalContext.current
    val configStore = remember { SmartEscalationConfigStore(context.applicationContext) }
    val sessionStore = remember { SmartSessionStore(context.applicationContext) }
    val now = System.currentTimeMillis()
    val activeReminders = tasks
        .filter {
            (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) &&
                it.reminderEnabled && it.reminderMode != ReminderMode.NONE
        }
        .sortedBy { it.reminderAtMillis.takeIf { time -> time > 0L } ?: Long.MAX_VALUE }

    val remindingNow = activeReminders.filter { task ->
        val session = sessionStore.current(task.id)
        session != null && session.snoozedStage == null
    }
    val snoozed = activeReminders.filter { task ->
        val session = sessionStore.current(task.id)
        val smartSnoozed = session?.snoozedStage != null && session.snoozedUntilMillis > now
        val regularSnoozed = task.reminderMode != ReminderMode.SMART && task.snoozeCount > 0 && task.reminderAtMillis > now
        (smartSnoozed || regularSnoozed) && task !in remindingNow
    }
    val upcoming = activeReminders.filter {
        it !in remindingNow && it !in snoozed && it.reminderAtMillis >= now && it.reminderAtMillis <= now + 24 * 60 * 60_000L
    }
    val later = activeReminders.filter { it !in remindingNow && it !in snoozed && it !in upcoming && it.reminderAtMillis > now }
    val needsAttention = activeReminders.filter { it !in remindingNow && it.reminderAtMillis in 1 until now }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                    Spacer(Modifier.width(4.dp))
                    Text("REMINDERS", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Surface(onClick = onNew, shape = CircleShape, color = RecRed, modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "New project", tint = ProjectorIvory) }
                    }
                }

                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
                    Spacer(Modifier.height(10.dp))
                    if (activeReminders.isEmpty()) {
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.NotificationsNone, null, tint = MutedGold, modifier = Modifier.size(30.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("No active reminders", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(13.dp))
                                Button(onClick = onNew, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(14.dp)) {
                                    Text("CREATE PROJECT", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    } else {
                        PReminderGroup("REMINDING NOW", remindingNow, RecRed, configStore, onEdit)
                        PReminderGroup("NEEDS ATTENTION", needsAttention, RecRed, configStore, onEdit)
                        PReminderGroup("SNOOZED", snoozed, MutedGold, configStore, onEdit)
                        PReminderGroup("UPCOMING", upcoming, MutedGold, configStore, onEdit)
                        PReminderGroup("LATER", later, MutedText, configStore, onEdit)
                    }
                }
            }
        }
    }
}

@Composable
private fun PReminderGroup(
    label: String,
    tasks: List<CreatorTask>,
    accent: Color,
    configStore: SmartEscalationConfigStore,
    onEdit: (String) -> Unit,
) {
    if (tasks.isEmpty()) return
    Text("$label · ${tasks.size}", color = accent, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(8.dp))
    tasks.forEach { task ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onEdit(task.id) },
            shape = RoundedCornerShape(18.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, if (label == "REMINDING NOW" || label == "NEEDS ATTENTION") RecRed.copy(alpha = .4f) else CinemaLine),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Icon(pModeIcon(task.reminderMode), null, tint = accent, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text("${pModeLabel(task.reminderMode)} · ${pFormatDateTime(task.reminderAtMillis)}", color = accent, fontSize = 9.sp)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PProjectComposer(
    task: CreatorTask?,
    reminderSetupReady: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onSave: (PProjectDraft) -> Unit,
    onRemoveReminder: () -> Unit,
) {
    val context = LocalContext.current
    val defaults = remember { CreatorOsSettingsStore(context.applicationContext).snapshot() }
    val defaultPlatform = remember(defaults.creatorProfile) { CreatorPlatformRegistry.primaryPlatform(defaults.creatorProfile) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000L)
            now = System.currentTimeMillis()
        }
    }
    val fallbackDue = now + 60 * 60_000L
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var platform by rememberSaveable(task?.id) { mutableStateOf(task?.platform ?: defaultPlatform) }
    var contentType by rememberSaveable(task?.id) { mutableStateOf(task?.contentType ?: CreatorPlatformRegistry.defaultFormat(task?.platform ?: defaultPlatform)) }
    var dueAt by rememberSaveable(task?.id) { mutableLongStateOf(task?.dueAtMillis?.takeIf { it > now } ?: fallbackDue) }
    var attentionPlan by rememberSaveable(task?.id) {
        mutableStateOf(task?.attentionPlan ?: ProjectAttentionPlan.GUIDED)
    }
    var deliveryPreference by rememberSaveable(task?.id) {
        mutableStateOf(task?.deliveryPreference ?: ReminderDeliveryPreference.AUTO)
    }
    var customReminderAt by rememberSaveable(task?.id) {
        mutableLongStateOf(task?.reminderAtMillis?.takeIf { it > now } ?: (dueAt - 30 * 60_000L).coerceAtLeast(now + 5 * 60_000L))
    }
    var priority by rememberSaveable(task?.id) { mutableStateOf(task?.priority ?: TaskPriority.IMPORTANT) }
    var notes by rememberSaveable(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var soundUri by rememberSaveable(task?.id) {
        mutableStateOf(task?.alarmSoundUri?.takeIf { it.isNotBlank() } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString())
    }
    var voice by rememberSaveable(task?.id) { mutableStateOf(task?.voicePersona ?: defaults.defaultVoicePersona) }
    var repeatCount by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatCount ?: 3) }
    var repeatGap by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatIntervalSeconds ?: 10) }
    var alarmTimeout by rememberSaveable(task?.id) { mutableIntStateOf(task?.alarmTimeoutSeconds ?: defaults.defaultAlarmTimeoutSeconds) }

    val platformOptions = remember(defaults.creatorProfile, task?.platform) {
        CreatorPlatformRegistry.orderedSelected(defaults.creatorProfile, include = task?.platform)
    }
    val formats = pFormats(platform)
    LaunchedEffect(platform) { if (contentType !in formats) contentType = formats.first() }

    fun pickDateTime(current: Long, onPicked: (Long) -> Unit) {
        val initial = Calendar.getInstance().apply { timeInMillis = current.takeIf { it > now } ?: fallbackDue }
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, h, min ->
                onPicked(Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }

    val previewTask = remember(title, platform, contentType, dueAt, attentionPlan, deliveryPreference, now) {
        CreatorTask(
            id = "pulse-preview",
            title = title.ifBlank { "Project" },
            platform = platform,
            contentType = contentType,
            dueLabel = "",
            dueAtMillis = dueAt,
            status = TaskStatus.PLANNED,
            workflowStageIndex = 0,
            attentionPlan = attentionPlan,
            deliveryPreference = deliveryPreference,
            pulseManagedReminder = attentionPlan != ProjectAttentionPlan.OFF && attentionPlan != ProjectAttentionPlan.CUSTOM,
        )
    }
    val pulsePreview = remember(previewTask, now) {
        if (attentionPlan == ProjectAttentionPlan.OFF || attentionPlan == ProjectAttentionPlan.CUSTOM) null
        else ProjectPulseEngine.applyAttentionPlan(previewTask, attentionPlan, now).let { ProjectPulseEngine.snapshot(it, now) }
    }
    val customReady = attentionPlan != ProjectAttentionPlan.CUSTOM ||
        (customReminderAt > now && customReminderAt <= dueAt)
    val effectiveReminderMode = when {
        attentionPlan == ProjectAttentionPlan.OFF -> ReminderMode.NONE
        deliveryPreference != ReminderDeliveryPreference.AUTO -> pDeliveryMode(deliveryPreference)
        attentionPlan == ProjectAttentionPlan.CUSTOM -> ReminderMode.SIMPLE
        else -> pulsePreview?.recommendedMode ?: ReminderMode.SIMPLE
    }
    val requiresAdvancedPermissions = effectiveReminderMode in setOf(ReminderMode.VOICE, ReminderMode.ALARM, ReminderMode.SMART)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (task == null) "NEW PROJECT" else "EDIT PROJECT", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Text(if (task == null) "What are you making?" else "Update your project", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                    Spacer(Modifier.height(10.dp))
                    PComposerLabel("PROJECT")
                    OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Project title") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    Spacer(Modifier.height(22.dp))
                    PComposerLabel("PUBLISH ON")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        platformOptions.forEach { value ->
                            FilterChip(selected = platform == value, onClick = { platform = value }, label = { Text(value, fontSize = 9.5.sp) })
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    PComposerLabel("FORMAT")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { value -> FilterChip(selected = contentType == value, onClick = { contentType = value }, label = { Text(value, fontSize = 9.sp) }) }
                    }
                    Spacer(Modifier.height(20.dp))
                    PComposerLabel("PUBLISH BY")
                    PDateTimeButton(pFormatDateTime(dueAt)) {
                        pickDateTime(dueAt) { picked ->
                            dueAt = picked
                            if (customReminderAt >= dueAt) customReminderAt = (dueAt - 30 * 60_000L).coerceAtLeast(now + 60_000L)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("KEEP THIS PROJECT ON TRACK", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    ProjectAttentionPlan.entries.forEach { plan ->
                        PAttentionPlanCard(plan, selected = attentionPlan == plan) { attentionPlan = plan }
                        Spacer(Modifier.height(7.dp))
                    }

                    if (attentionPlan != ProjectAttentionPlan.OFF) {
                        Spacer(Modifier.height(12.dp))
                        PComposerLabel("REMINDER STYLE")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            ReminderDeliveryPreference.entries.forEach { value ->
                                FilterChip(
                                    selected = deliveryPreference == value,
                                    onClick = { deliveryPreference = value },
                                    label = { Text(pDeliveryPreferenceLabel(value), fontSize = 9.sp) },
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (deliveryPreference == ReminderDeliveryPreference.AUTO) "Auto adapts delivery to project urgency. Choose another style to keep it for every stage."
                            else "${pDeliveryPreferenceLabel(deliveryPreference)} will stay selected as this project moves through stages.",
                            color = MutedText,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                        )
                    }

                    pulsePreview?.let { pulse ->
                        Spacer(Modifier.height(8.dp))
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color(0xFF151515), border = BorderStroke(1.dp, CinemaLine)) {
                            Column(Modifier.padding(13.dp)) {
                                Text("NEXT CHECK-IN", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(pFormatDateTime(pulse.nextCheckpointAtMillis), color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${pulse.currentStage.label} · ${pModeLabel(pulse.recommendedMode)}", color = MutedText, fontSize = 9.sp)
                            }
                        }
                    }

                    if (attentionPlan == ProjectAttentionPlan.CUSTOM) {
                        Spacer(Modifier.height(18.dp))
                        PComposerLabel("REMINDER TIME")
                        PDateTimeButton(pFormatDateTime(customReminderAt)) { pickDateTime(customReminderAt) { customReminderAt = it } }
                        Spacer(Modifier.height(14.dp))
                        PComposerLabel("IMPORTANCE")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            TaskPriority.entries.forEach { value ->
                                FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(pPriorityLabel(value), fontSize = 9.sp) })
                            }
                        }
                        if (!customReady) {
                            Spacer(Modifier.height(7.dp))
                            Text("Choose a reminder between now and publish time.", color = RecRed, fontSize = 8.5.sp)
                        }
                    }

                    if (requiresAdvancedPermissions && !reminderSetupReady) {
                        Spacer(Modifier.height(12.dp))
                        Surface(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFF17130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f))) {
                            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Settings, null, tint = MutedGold, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Finish reminder setup", color = ProjectorIvory, fontSize = 9.5.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    PComposerLabel("NOTES · OPTIONAL")
                    OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp), placeholder = { Text("Angle, reference, anything worth remembering…") }, shape = RoundedCornerShape(16.dp))

                    if (task != null && (task.reminderEnabled || task.attentionPlan != ProjectAttentionPlan.OFF)) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onRemoveReminder, modifier = Modifier.fillMaxWidth()) {
                            Text("TURN OFF PROJECT ATTENTION", color = MutedText, fontSize = 8.5.sp)
                        }
                    }
                }

                Surface(color = Color(0xF20B0B0C), tonalElevation = 8.dp) {
                    Button(
                        onClick = {
                            onSave(
                                PProjectDraft(
                                    title = title.trim(),
                                    platform = platform,
                                    contentType = contentType,
                                    dueAtMillis = dueAt,
                                    attentionPlan = attentionPlan,
                                    mode = if (attentionPlan == ProjectAttentionPlan.CUSTOM) pDeliveryMode(deliveryPreference) else ReminderMode.NONE,
                                    reminderAtMillis = if (attentionPlan == ProjectAttentionPlan.CUSTOM) customReminderAt else 0L,
                                    deliveryPreference = deliveryPreference,
                                    priority = priority,
                                    notes = notes.trim(),
                                    alarmSoundUri = soundUri,
                                    voicePersona = voice,
                                    voiceRepeatCount = repeatCount,
                                    voiceRepeatIntervalSeconds = repeatGap,
                                    alarmTimeoutSeconds = alarmTimeout,
                                )
                            )
                        },
                        enabled = title.isNotBlank() && dueAt > now && customReady,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    ) {
                        Text(if (task == null) "CREATE PROJECT" else "SAVE PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun PAttentionPlanCard(plan: ProjectAttentionPlan, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) RecRed.copy(alpha = .10f) else CinemaSurface,
        border = BorderStroke(1.dp, if (selected) RecRed.copy(alpha = .75f) else CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (plan) {
                    ProjectAttentionPlan.OFF -> Icons.Outlined.NotificationsOff
                    ProjectAttentionPlan.LIGHT -> Icons.Outlined.NotificationsNone
                    ProjectAttentionPlan.GUIDED -> Icons.Outlined.NotificationsActive
                    ProjectAttentionPlan.URGENT -> Icons.Outlined.Alarm
                    ProjectAttentionPlan.CUSTOM -> Icons.Outlined.Tune
                },
                null,
                tint = if (selected) RecRed else MutedGold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(ProjectPulseEngine.planLabel(plan), color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text(ProjectPulseEngine.planDescription(plan), color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = RecRed, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PSmartGapPicker(label: String, selectedMinutes: Int, onSelected: (Int) -> Unit) {
    Text(label, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        SmartEscalationPolicy.allowedGapMinutes.forEach { minutes ->
            FilterChip(
                selected = selectedMinutes == minutes,
                onClick = { onSelected(minutes) },
                label = { Text("${minutes}m", fontSize = 8.8.sp) },
            )
        }
    }
}

@Composable
private fun PSmartWindowCard(valid: Boolean, requiredMinutes: Int, availableMinutes: Int, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = if (valid) Color(0xFF101812) else Color(0xFF1A1110),
        border = BorderStroke(1.dp, if (valid) SuccessGreen.copy(alpha = .35f) else RecRed.copy(alpha = .4f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (valid) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber, null, tint = if (valid) SuccessGreen else RecRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(detail, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (requiredMinutes > 0) {
                    Text("Needs ${requiredMinutes}m · ${availableMinutes}m available", color = if (valid) MutedText else RecRed, fontSize = 8.6.sp)
                }
            }
        }
    }
}

@Composable
private fun PModeCard(mode: ReminderMode, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val (icon, description) = when (mode) {
        ReminderMode.NONE -> Icons.Outlined.NotificationsOff to "No alert. Keep it in your plan only."
        ReminderMode.SIMPLE -> Icons.Outlined.Notifications to "One quiet notification."
        ReminderMode.VOICE -> Icons.Outlined.RecordVoiceOver to "A spoken reminder you can repeat or snooze."
        ReminderMode.ALARM -> Icons.Outlined.Alarm to "A strong ringing reminder for deadlines that matter."
        ReminderMode.SMART -> Icons.Outlined.AutoAwesome to "Escalates only while unanswered, using the waits you choose."
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFF19130F) else CinemaSurface,
        border = BorderStroke(1.dp, if (selected) RecRed.copy(alpha = .55f) else CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (!enabled) Color(0xFF56524E) else if (selected) RecRed else MutedGold, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(pModeLabel(mode), color = if (enabled) ProjectorIvory else Color(0xFF6D6964), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text(description, color = if (enabled) MutedText else Color(0xFF56524E), fontSize = 8.7.sp, lineHeight = 12.sp)
            }
            RadioButton(selected = selected, onClick = if (enabled) onClick else null, enabled = enabled, colors = RadioButtonDefaults.colors(selectedColor = RecRed))
        }
    }
}

@Composable
private fun PComposerLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp, modifier = Modifier.padding(bottom = 7.dp))
}

@Composable
private fun PDateTimeButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(14.dp)) {
        Icon(Icons.Outlined.Schedule, null, tint = MutedGold, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = ProjectorIvory, modifier = Modifier.weight(1f))
        Text("CHANGE", color = RecRed, fontSize = 8.sp)
    }
}

internal fun pDeliveryPreferenceLabel(preference: ReminderDeliveryPreference): String = when (preference) {
    ReminderDeliveryPreference.AUTO -> "Auto"
    ReminderDeliveryPreference.NOTIFICATION -> "Notification"
    ReminderDeliveryPreference.VOICE -> "Voice"
    ReminderDeliveryPreference.ALARM -> "Alarm"
    ReminderDeliveryPreference.SMART -> "Smart"
}

internal fun pDeliveryMode(preference: ReminderDeliveryPreference): ReminderMode = when (preference) {
    ReminderDeliveryPreference.AUTO, ReminderDeliveryPreference.NOTIFICATION -> ReminderMode.SIMPLE
    ReminderDeliveryPreference.VOICE -> ReminderMode.VOICE
    ReminderDeliveryPreference.ALARM -> ReminderMode.ALARM
    ReminderDeliveryPreference.SMART -> ReminderMode.SMART
}

internal fun pModeLabel(mode: ReminderMode): String = when (mode) {
    ReminderMode.NONE -> "None"
    ReminderMode.SIMPLE -> "Simple"
    ReminderMode.VOICE -> "Voice"
    ReminderMode.ALARM -> "Alarm"
    ReminderMode.SMART -> "Smart"
}

internal fun pModeIcon(mode: ReminderMode): ImageVector = when (mode) {
    ReminderMode.NONE -> Icons.Outlined.NotificationsOff
    ReminderMode.SIMPLE -> Icons.Outlined.Notifications
    ReminderMode.VOICE -> Icons.Outlined.RecordVoiceOver
    ReminderMode.ALARM -> Icons.Outlined.Alarm
    ReminderMode.SMART -> Icons.Outlined.AutoAwesome
}

internal fun pPriorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.NORMAL -> "Normal"
    TaskPriority.IMPORTANT -> "Important"
    TaskPriority.CRITICAL -> "Critical"
}

private fun pFormats(platform: String): List<String> = CreatorPlatformRegistry.formats(platform)

internal fun pFormatDateTime(millis: Long): String {
    if (millis <= 0L) return "Choose time"
    return SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
}

private fun pComposerPreviewVoice(context: Context, persona: VoicePersona) {
    var tts: TextToSpeech? = null
    tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.let { VoicePersonaEngine.apply(it, persona) }
            tts?.speak("FrameByNavin. This is ${VoicePersonaEngine.label(persona)}.", TextToSpeech.QUEUE_FLUSH, null, "composer-${persona.name}")
            Handler(Looper.getMainLooper()).postDelayed({ tts?.shutdown() }, 7_000L)
        } else tts?.shutdown()
    }
}
