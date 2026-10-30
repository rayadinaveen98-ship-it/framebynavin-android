package com.framebynavin.app.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.QuietHoursPolicy
import com.framebynavin.app.reminders.V205NudgePausePolicy
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V144SettingsHub(
    settings: CreatorOsSettings,
    tasks: List<CreatorTask>,
    weeklySlots: List<WeeklyScheduleSlot> = emptyList(),
    weeklyAutoPlanEnabled: Boolean,
    notificationsReady: Boolean,
    preciseTimingReady: Boolean,
    fullScreenReady: Boolean,
    batteryReady: Boolean,
    onClose: () -> Unit,
    onProfile: () -> Unit,
    onVoice: (VoicePersona) -> Unit,
    onAlarmTimeout: (Int) -> Unit,
    onSnooze: (Int) -> Unit,
    onUnansweredFollowUps: (Boolean) -> Unit = {},
    onWeeklyAutoPlan: (Boolean) -> Unit,
    onContextNudges: (Boolean) -> Unit,
    onQuietHoursEnabled: (Boolean) -> Unit = {},
    onQuietHoursWindow: (Int, Int) -> Unit = { _, _ -> },
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onReplayTour: () -> Unit,
    onCloudSync: () -> Unit,
    onYouTube: () -> Unit,
    onOpenWeeklyPlan: () -> Unit = {},
    onOpenAutomation: () -> Unit = {},
    onOpenHomeSlideshow: () -> Unit = {},
    companionEnabled: Boolean = true,
    onCompanionEnabledChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var sectionName by rememberSaveable { mutableStateOf(V167SettingsSection.HOME.name) }
    val section = V167SettingsNavigationPolicy.fromSavedName(sectionName)
    fun navigateBack() { sectionName = V167SettingsNavigationPolicy.parent(section).name }
    var confirmAutoPlan by remember { mutableStateOf(false) }
    var showExtraPermissions by rememberSaveable { mutableStateOf(false) }
    val permissionSummary = V169SettingsClarityPolicy.permissions(
        notificationsReady, preciseTimingReady, fullScreenReady, batteryReady,
    )
    val nextProjects = CreatorWeeklyPlanPreviewEngine.build(weeklySlots, tasks)

    BackHandler {
        if (section == V167SettingsSection.HOME) onClose() else navigateBack()
    }

    // V207: the OPAQUE surface must cover the entire shared Activity root.
    // Only content receives system/keyboard insets, or Home can bleed through
    // status/navigation bar strips while a Settings page is open.
    Surface(
        modifier = Modifier.fillMaxSize().testTag("v207-settings-fullscreen"),
        color = CinemaBlack,
    ) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            when (section) {
            V167SettingsSection.HOME -> V144SettingsHome(
                settings = settings,
                weeklyAutoPlanEnabled = weeklyAutoPlanEnabled,
                notificationsReady = notificationsReady,
                preciseTimingReady = preciseTimingReady,
                fullScreenReady = fullScreenReady,
                batteryReady = batteryReady,
                onClose = onClose,
                onProfile = onProfile,
                onOpen = { sectionName = it.name },
            )

            V167SettingsSection.PERSONALIZATION -> V144SettingsPage("PERSONALIZATION", "Make Backlot yours", ::navigateBack) {
                V144CategoryCard("Appearance", "Theme, app icon and launch sound", Icons.Outlined.Palette, RecRed) {
                    sectionName = V167SettingsSection.APPEARANCE.name
                }
                V144CategoryCard("Home photos", "Manage slideshow photos", Icons.Outlined.Collections, MutedGold) {
                    onOpenHomeSlideshow()
                }
                V144CategoryCard(
                    "Home screen widgets",
                    "Add Quick Capture, Today Focus and My Day; safely replace pinned old widgets",
                    Icons.Outlined.Widgets,
                    MutedGold,
                ) {
                    context.startActivity(Intent(context, WidgetMigrationActivity::class.java))
                }
                V144CategoryCard("Companion", "Choose your guide and helpful suggestions", Icons.Outlined.Face, MutedGold) {
                    sectionName = V167SettingsSection.GUIDE.name
                }
                V144CategoryCard("Voice", "Reminder voice and previews", Icons.Outlined.RecordVoiceOver, RecRed) {
                    sectionName = V167SettingsSection.VOICE.name
                }
                V144SettingsNote("These choices change how Backlot looks and sounds. Existing projects stay unchanged.")
            }

            V167SettingsSection.APPEARANCE -> V144SettingsPage("APPEARANCE", "Your Backlot world", ::navigateBack) {
                V127AppearanceSettings(showGuidePicker = false)
            }

            V167SettingsSection.GUIDE -> V144SettingsPage("COMPANION", "Your creative companion", ::navigateBack) {
                V144GuidePicker()
                Spacer(Modifier.height(18.dp))
                V144SectionTitle("HELPFUL SUGGESTIONS", "Optional, private guidance on Home.")
                Spacer(Modifier.height(8.dp))
                V2215CompanionControls(
                    enabled = companionEnabled,
                    onEnabledChange = onCompanionEnabledChange,
                )
                Spacer(Modifier.height(12.dp))
                V144SettingsNote("Choose the companion you want to see in Backlot. Suggestions never create, complete or reschedule reminders or projects. Turning them off does not change your guide.")
            }

            V167SettingsSection.VOICE -> V144SettingsPage("VOICE", "Default voice for new projects", ::navigateBack) {
                V140VoiceStudioPicker(
                    selected = settings.defaultVoicePersona,
                    onSelected = onVoice,
                    onPreview = { v144PreviewVoice(context, it) },
                )
                Spacer(Modifier.height(8.dp))
                V144SettingsNote(
                    "This voice is the default for new project reminders. Existing projects keep their own voice choice. " +
                        "Preview uses your phone's text-to-speech options; results vary by device and installed languages."
                )
            }

            V167SettingsSection.REMINDERS -> V144SettingsPage("NOTIFICATIONS & REMINDERS", "Timing, permissions and defaults", { sectionName = V167SettingsSection.HOME.name }) {
                V144StatusCard(
                    title = permissionSummary.title,
                    subtitle = permissionSummary.explanation,
                    icon = if (notificationsReady) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsActive,
                    accent = if (notificationsReady) SuccessGreen else MutedGold,
                )
                Spacer(Modifier.height(10.dp))
                V144PermissionRow("Allow notifications", notificationsReady, onNotifications)
                TextButton(
                    onClick = { showExtraPermissions = !showExtraPermissions },
                    modifier = Modifier.fillMaxWidth().testTag("v169-advanced-permissions"),
                ) {
                    Text(
                        if (showExtraPermissions) "HIDE ANDROID ALARM OPTIONS"
                        else "MORE ANDROID OPTIONS · ${permissionSummary.advancedMissing} NEED ATTENTION",
                        color = MutedGold, fontSize = 11.sp,
                    )
                }
                if (showExtraPermissions) {
                    V144PermissionRow("Exact alarm timing", preciseTimingReady, onPreciseTiming)
                    V144PermissionRow("Full-screen alarm alerts", fullScreenReady, onFullScreen)
                    V144PermissionRow("Battery access for background reminders", batteryReady, onBattery)
                    Text(
                        "These affect some advanced reminder delivery modes. Android may still delay or restrict alerts.",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))
                V144SectionTitle("REMINDER DEFAULTS", "Used for new reminders. Existing project choices stay saved.")
                Spacer(Modifier.height(10.dp))
                Text("Snooze", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                    V169SettingsClarityPolicy.snoozeChoices(settings.snoozeMinutes).forEach { value ->
                        FilterChip(settings.snoozeMinutes == value, { onSnooze(value) }, { Text("${value}m", fontSize = 10.sp) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Alarm auto-stop", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                    V169SettingsClarityPolicy.alarmTimeoutChoices(settings.defaultAlarmTimeoutSeconds).forEach { value ->
                        val label = if (value < 60) "${value}s" else if (value % 60 == 0) "${value / 60}m" else "${value / 60}m ${value % 60}s"
                        FilterChip(settings.defaultAlarmTimeoutSeconds == value, { onAlarmTimeout(value) }, { Text(label, fontSize = 10.sp) })
                    }
                }

                Spacer(Modifier.height(18.dp))
                V144SectionTitle("IF YOU DON'T RESPOND", "Gentle follow-ups, not extra ringing.")
                Spacer(Modifier.height(9.dp))
                Surface(
                    Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Follow up when unanswered", color = ProjectorIvory,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (settings.unansweredFollowUpsEnabled)
                                    "On · up to 2 quiet notifications. No extra voice or alarm."
                                else "Off · reminders stay unanswered without automatic follow-ups.",
                                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "Follow-up times depend on importance and Android background scheduling. " +
                                    "This never marks your project done.",
                                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                            )
                        }
                        Switch(
                            modifier = Modifier.testTag("v180-unanswered-followups-toggle"),
                            checked = settings.unansweredFollowUpsEnabled,
                            onCheckedChange = onUnansweredFollowUps,
                            colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                        )
                    }
                }

                Spacer(Modifier.height(21.dp))
                V144SectionTitle("QUIET HOURS", "Quiet standard notifications during your rest time.")
                Spacer(Modifier.height(9.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Quiet hours", color = ProjectorIvory,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (settings.quietHoursEnabled)
                                        "On · ${QuietHoursPolicy.label(settings.quietHoursStartMinute)} – ${QuietHoursPolicy.label(settings.quietHoursEndMinute)}"
                                    else "Off · normal notifications at their scheduled times",
                                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                                )
                            }
                            Switch(
                                checked = settings.quietHoursEnabled,
                                onCheckedChange = onQuietHoursEnabled,
                                colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                            )
                        }
                        if (settings.quietHoursEnabled) {
                            Spacer(Modifier.height(12.dp))
                            Text("From", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(20 * 60, 21 * 60, 22 * 60, 23 * 60).forEach { time ->
                                    FilterChip(
                                        selected = settings.quietHoursStartMinute == time,
                                        onClick = { if (time != settings.quietHoursEndMinute)
                                            onQuietHoursWindow(time, settings.quietHoursEndMinute) },
                                        label = { Text(QuietHoursPolicy.label(time), fontSize = 11.sp) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Until", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(6 * 60, 7 * 60, 8 * 60, 9 * 60).forEach { time ->
                                    FilterChip(
                                        selected = settings.quietHoursEndMinute == time,
                                        onClick = { if (time != settings.quietHoursStartMinute)
                                            onQuietHoursWindow(settings.quietHoursStartMinute, time) },
                                        label = { Text(QuietHoursPolicy.label(time), fontSize = 11.sp) },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            V169SettingsClarityPolicy.quietHoursDescription(settings) +
                                " Notifications remain scheduled but Android can delay delivery.",
                            color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Helpful project reminders", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    !notificationsReady -> "Enable notifications first."
                                    settings.contextNudgesEnabled && V205NudgePausePolicy.isPaused(
                                        System.currentTimeMillis(),
                                        context.getSharedPreferences(V205NudgePausePolicy.PREFS, Context.MODE_PRIVATE)
                                            .getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L),
                                    ) -> {
                                        val until = context.getSharedPreferences(
                                            V205NudgePausePolicy.PREFS, Context.MODE_PRIVATE
                                        ).getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L)
                                        "Paused · resumes ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(until))}"
                                    }
                                    settings.contextNudgesEnabled -> "On · optional check-ins when work may need attention."
                                    else -> "Off · your scheduled project reminders still work."
                                },
                                color = MutedText,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            )
                        }
                        Switch(
                            checked = settings.contextNudgesEnabled,
                            onCheckedChange = onContextNudges,
                            enabled = notificationsReady,
                            colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                        )
                    }
                }
            }

            V167SettingsSection.PLANNING -> V144SettingsPage("PLANNING & CALENDAR", "Recurring planning and calendar sync", { sectionName = V167SettingsSection.HOME.name }) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(MutedGold.copy(alpha = .11f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Weekly Auto Plan", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(if (weeklyAutoPlanEnabled) "On · new weekly projects can be added." else "Off · saved templates and projects stay unchanged.", color = MutedText, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                        Switch(
                            modifier = Modifier.testTag("s1-settings-auto-plan"),
                            checked = weeklyAutoPlanEnabled,
                            onCheckedChange = { enable ->
                                if (enable) confirmAutoPlan = true else onWeeklyAutoPlan(false)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                        )
                    }
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    V169SettingsClarityPolicy.autoPlanExplanation(
                        weeklyAutoPlanEnabled, nextProjects.missing.size, weeklySlots
                    ),
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                )
                Spacer(Modifier.height(8.dp))
                V144ActionCard(
                    title = "Edit weekly plan",
                    subtitle = "Change post days, times and templates without editing existing projects.",
                    icon = Icons.Outlined.CalendarMonth,
                    accent = MutedGold,
                    onClick = onOpenWeeklyPlan,
                )
                Spacer(Modifier.height(8.dp))
                V144ActionCard(
                    title = "Automation",
                    subtitle = "See planning checks and optional notification routines.",
                    icon = Icons.Outlined.AutoAwesome,
                    accent = RecRed,
                    onClick = onOpenAutomation,
                )
                Spacer(Modifier.height(16.dp))
                V144SettingsNote(
                    "Auto Plan creates project drafts from your active weekly templates; it never posts videos. Google Calendar below is separate and only exports deadlines after you approve."
                )
                Spacer(Modifier.height(12.dp))
                V129GoogleCalendarSettings(tasks)
            }

            V167SettingsSection.DATA -> V144SettingsPage("DATA, BACKUP & SYNC", "Keep your Backlot data recoverable", { sectionName = V167SettingsSection.HOME.name }) {
                V144ActionCard(
                    title = "Cloud Sync",
                    subtitle = "Google account · cloud backups",
                    icon = Icons.Outlined.CloudSync,
                    accent = RecRed,
                    onClick = onCloudSync,
                )
                Spacer(Modifier.height(9.dp))
                V144ActionCard(
                    title = "Local Data & Backup",
                    subtitle = "Export your work and recordings; see recovery copies.",
                    icon = Icons.Outlined.SaveAlt,
                    accent = MutedGold,
                    onClick = { context.startActivity(Intent(context, BackupActivity::class.java)) },
                )
                Spacer(Modifier.height(12.dp))
                V144SettingsNote(
                    "A portable backup includes projects, ideas, supported Voice Idea recordings, original Best Frames, reminders and planning. " +
                        "Project attachments keep their links only: the referenced files are not included. " +
                        "Companion, theme, app icon, launch sound, Calendar connection and sign-in remain device-specific. " +
                        "Exported files are not encrypted: keep them private."
                )
            }

            V167SettingsSection.ABOUT -> V144SettingsPage("ABOUT BACKLOT", "Version, help and guided setup", { sectionName = V167SettingsSection.HOME.name }) {
                V144ActionCard(
                    title = "Show app tour",
                    subtitle = "Home → Ideas → Projects → Insights → Quick Actions",
                    icon = Icons.Outlined.School,
                    accent = MutedGold,
                    onClick = onReplayTour,
                )
                Spacer(Modifier.height(12.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Version", color = MutedText, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Text(BuildConfig.VERSION_NAME, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
    if (confirmAutoPlan) {
        AlertDialog(
            onDismissRequest = { confirmAutoPlan = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Turn on automatic planning?", color = ProjectorIvory) },
            text = {
                Column {
                    Text(
                        "Backlot may add ${nextProjects.missing.size} missing projects from your saved weekly templates in the next 14 days. Existing projects and edits stay untouched.",
                        color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Auto Plan will not publish videos or delete your work.", color = MutedGold, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirmAutoPlan = false
                    onWeeklyAutoPlan(true)
                }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("TURN ON AUTO PLAN") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAutoPlan = false }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun V144SettingsHome(
    settings: CreatorOsSettings,
    weeklyAutoPlanEnabled: Boolean,
    notificationsReady: Boolean,
    preciseTimingReady: Boolean,
    fullScreenReady: Boolean,
    batteryReady: Boolean,
    onClose: () -> Unit,
    onProfile: () -> Unit,
    onOpen: (V167SettingsSection) -> Unit,
) {
    val reminderReady = notificationsReady && preciseTimingReady && fullScreenReady && batteryReady
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
    ) {
        V144SettingsHeader("SETTINGS", "Backlot, your way", onClose)
        Spacer(Modifier.height(10.dp))
        Text(
            "Choose what you'd like to change.",
            color = MutedText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(18.dp))

        V144CategoryCard(
            title = "My Profile",
            subtitle = settings.creatorProfile.safeDisplayName +
                settings.creatorProfile.category.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
            icon = Icons.Outlined.Person,
            accent = MutedGold,
            onClick = onProfile,
        )
        V144CategoryCard("Personalization", "Appearance, Spark and voice", Icons.Outlined.Palette, RecRed) {
            onOpen(V167SettingsSection.PERSONALIZATION)
        }
        V144CategoryCard(
            "Reminders & Alerts",
            if (reminderReady) "Permissions ready · snooze, alarms and quiet hours"
            else "Check permissions · reminder defaults and quiet hours",
            Icons.Outlined.NotificationsActive,
            if (reminderReady) SuccessGreen else MutedGold,
        ) { onOpen(V167SettingsSection.REMINDERS) }
        V144CategoryCard(
            "Planning & Calendar",
            if (weeklyAutoPlanEnabled) "Auto Plan on · Google Calendar" else "Auto Plan off · Google Calendar",
            Icons.Outlined.CalendarMonth,
            MutedGold,
        ) { onOpen(V167SettingsSection.PLANNING) }
        V144CategoryCard("Backup & Data", "Cloud backup, local export and recovery", Icons.Outlined.CloudSync, MutedGold) {
            onOpen(V167SettingsSection.DATA)
        }
        V144CategoryCard("About & Help", "App tour and version", Icons.Outlined.Info, RecRed) {
            onOpen(V167SettingsSection.ABOUT)
        }
    }
}

@Composable
private fun V144SettingsPage(kicker: String, title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
    ) {
        V144SettingsHeader(kicker, title, onBack)
        Spacer(Modifier.height(18.dp))
        content()
    }
}

@Composable
private fun V144SettingsHeader(kicker: String, title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(kicker, color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(title, color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun V144CategoryCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(accent.copy(alpha = .11f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun V144ActionCard(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    V144CategoryCard(title, subtitle, icon, accent, onClick)
}

@Composable
private fun V144StatusCard(title: String, subtitle: String, icon: ImageVector, accent: Color) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, accent.copy(alpha = .28f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(accent.copy(alpha = .11f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun V144PermissionRow(label: String, ready: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = if (ready) ({}) else onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
        shape = RoundedCornerShape(15.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber, null, tint = if (ready) SuccessGreen else MutedGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(label, color = ProjectorIvory, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(if (ready) "READY" else "SET UP", color = if (ready) SuccessGreen else RecRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V144SectionTitle(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = MutedText, fontSize = 12.sp)
}

@Composable
private fun V144SettingsNote(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(12.dp))
    }
}

private fun v144PreviewVoice(context: android.content.Context, persona: VoicePersona) {
    var tts: TextToSpeech? = null
    tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.let { VoicePersonaEngine.apply(it, persona) }
            tts?.speak(VoicePersonaEngine.previewText(persona), TextToSpeech.QUEUE_FLUSH, null, "v144-${persona.name}")
            Handler(Looper.getMainLooper()).postDelayed({ tts?.shutdown() }, 7_000L)
        } else {
            tts?.shutdown()
        }
    }
}
