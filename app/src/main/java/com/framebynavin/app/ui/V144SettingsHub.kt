package com.framebynavin.app.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.framebynavin.app.ui.theme.*
import java.text.DateFormat
import java.util.Date

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
    var searchedDirectlyToDetail by rememberSaveable { mutableStateOf(false) }
    val section = V167SettingsNavigationPolicy.fromSavedName(sectionName)
    fun navigateBack() {
        sectionName = if (searchedDirectlyToDetail &&
            section in listOf(
                V167SettingsSection.APPEARANCE,
                V167SettingsSection.REMINDER_SCENES,
                V167SettingsSection.GUIDE,
                V167SettingsSection.VOICE,
            )
        ) {
            searchedDirectlyToDetail = false
            V167SettingsSection.HOME.name
        } else V167SettingsNavigationPolicy.parent(section).name
    }
    fun openSearchDestination(destination: V404SettingsDestination) {
        searchedDirectlyToDetail = destination in setOf(
            V404SettingsDestination.APPEARANCE,
            V404SettingsDestination.REMINDER_SCENES,
            V404SettingsDestination.COMPANION,
            V404SettingsDestination.VOICE,
        )
        when (destination) {
            V404SettingsDestination.ACCOUNT -> onProfile()
            V404SettingsDestination.APPEARANCE -> sectionName = V167SettingsSection.APPEARANCE.name
            V404SettingsDestination.REMINDER_SCENES -> sectionName = V167SettingsSection.REMINDER_SCENES.name
            V404SettingsDestination.COMPANION -> sectionName = V167SettingsSection.GUIDE.name
            V404SettingsDestination.VOICE -> sectionName = V167SettingsSection.VOICE.name
            V404SettingsDestination.HOME_PHOTOS -> onOpenHomeSlideshow()
            V404SettingsDestination.REMINDERS -> sectionName = V167SettingsSection.REMINDERS.name
            V404SettingsDestination.PLANNING -> sectionName = V167SettingsSection.PLANNING.name
            V404SettingsDestination.YOUTUBE -> onYouTube()
            V404SettingsDestination.PRIVACY -> sectionName = V167SettingsSection.PRIVACY.name
            V404SettingsDestination.ADVANCED -> sectionName = V167SettingsSection.ADVANCED.name
            V404SettingsDestination.ABOUT -> sectionName = V167SettingsSection.ABOUT.name
        }
    }
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
                weeklyAutoPlanEnabled = weeklyAutoPlanEnabled,
                notificationsReady = notificationsReady,
                preciseTimingReady = preciseTimingReady,
                fullScreenReady = fullScreenReady,
                batteryReady = batteryReady,
                onClose = onClose,
                onProfile = onProfile,
                onOpen = {
                    searchedDirectlyToDetail = false
                    sectionName = it.name
                },
                onSearchDestination = ::openSearchDestination,
            )

            V167SettingsSection.PERSONALIZATION -> V144SettingsPage("PERSONALIZATION", "Your Backlot world", ::navigateBack) {
                V404SettingsGroupLabel("LOOK & FEEL")
                V294QuietSettingRow(
                    title = "Theme, icon & welcome",
                    icon = Icons.Outlined.Palette,
                    support = "${VisualExperiencePrefs.currentTheme.displayName} · ${VisualExperiencePrefs.welcomeStyle.displayName}",
                ) { sectionName = V167SettingsSection.APPEARANCE.name }
                V294QuietSettingsDivider()
                V294QuietSettingRow(
                    title = "Home photos",
                    icon = Icons.Outlined.Collections,
                    support = "Manage the existing Home slideshow",
                    onClick = onOpenHomeSlideshow,
                )
                V294QuietSettingsDivider()
                V294QuietSettingRow(
                    title = "Reminder appearance",
                    icon = Icons.Outlined.MovieFilter,
                    support = "Entrance gallery, originals, Classic and Premiere",
                ) { sectionName = V167SettingsSection.REMINDER_SCENES.name }

                Spacer(Modifier.height(18.dp))
                V404SettingsGroupLabel("COMPANION & SOUND")
                V294QuietSettingRow(
                    title = "Companion",
                    icon = Icons.Outlined.Face,
                    support = "${V144GuidePrefs.currentGuide.displayName} · choose your character and suggestions",
                ) { sectionName = V167SettingsSection.GUIDE.name }
                V294QuietSettingsDivider()
                V294QuietSettingRow(
                    title = "Reminder voice",
                    icon = Icons.Outlined.RecordVoiceOver,
                    support = "Existing voice styles and default background music",
                ) { sectionName = V167SettingsSection.VOICE.name }
                Spacer(Modifier.height(17.dp))
                Text(
                    "Your saved choices stay in place. Appearance changes do not edit " +
                        "projects, reminders, recordings or account data.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 9.dp),
                )
            }

            V167SettingsSection.APPEARANCE -> V144SettingsPage("APPEARANCE", "Your Backlot world", ::navigateBack) {
                V127AppearanceSettings(showGuidePicker = false)
            }

            V167SettingsSection.REMINDER_SCENES -> V144SettingsPage(
                "REMINDER APPEARANCE", "Entrance backgrounds and preserved classic styles",
                ::navigateBack,
            ) {
                // RX02: one place for gallery/default entrance art and all
                // preserved Classic/Premiere choices. Never mutate delivery
                // behavior or legacy preferences merely by opening Settings.
                com.framebynavin.app.reminders.RX02ReminderAppearanceSettings()
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

            V167SettingsSection.VOICE -> V144SettingsPage("VOICE", "Default voice and background music", ::navigateBack) {
                val musicStore = remember(context) {
                    com.framebynavin.app.reminders.CreatorMusicLocalStore(context)
                }
                val savedMusic = remember(musicStore) { musicStore.defaultChoice() }
                var defaultMusicMode by remember { mutableStateOf(savedMusic.mode) }
                var defaultMusicUri by remember { mutableStateOf(savedMusic.uri) }
                V140VoiceStudioPicker(
                    selected = settings.defaultVoicePersona,
                    onSelected = onVoice,
                    onPreview = { V403VoicePreviewPlayer.play(context, it) },
                )
                Spacer(Modifier.height(18.dp))
                RX03BReminderVoicePacksSettings()
                Spacer(Modifier.height(18.dp))
                RX03LocalVoiceIdentityStudio()
                Spacer(Modifier.height(16.dp))
                V273ReminderMusicPicker(
                    mode = defaultMusicMode,
                    uri = defaultMusicUri,
                    onChanged = { mode, uri -> defaultMusicMode = mode; defaultMusicUri = uri },
                    modifier = Modifier.fillMaxWidth().testTag("v276-global-music-settings"),
                    allowSetCreatorDefault = true,
                )
                Spacer(Modifier.height(8.dp))
                V144SettingsNote(
                    "New projects use the selected Backlot voice style. Existing projects keep their chosen style; " +
                        "if you assign a different installed speaker to a style, that speaker also applies to existing projects using that style. " +
                        "Offline identity choices depend on the device's installed TTS engine and language."
                )
            }

            V167SettingsSection.REMINDERS -> V144SettingsPage("NOTIFICATIONS & REMINDERS", "Timing, permissions and defaults", { sectionName = V167SettingsSection.HOME.name }) {
                V144StatusCard(
                    title = permissionSummary.title,
                    subtitle = permissionSummary.explanation,
                    icon = if (notificationsReady) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsActive,
                    accent = when {
                        !notificationsReady -> RecRed
                        permissionSummary.advancedMissing > 0 -> MutedGold
                        else -> SuccessGreen
                    },
                )
                Spacer(Modifier.height(10.dp))
                V144PermissionRow("Allow notifications", notificationsReady, onNotifications)
                TextButton(
                    onClick = { showExtraPermissions = !showExtraPermissions },
                    modifier = Modifier.fillMaxWidth().testTag("v169-advanced-permissions"),
                ) {
                    Text(
                        when {
                            showExtraPermissions -> "HIDE ANDROID ALARM OPTIONS"
                            permissionSummary.advancedMissing > 0 ->
                                "MORE ANDROID OPTIONS · ${permissionSummary.advancedMissing} NEED ATTENTION"
                            else -> "SHOW ANDROID ALARM OPTIONS"
                        },
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
                    Spacer(Modifier.height(12.dp))
                    // RX02: permission/OS entitlement belongs to delivery
                    // settings, never to the media/appearance chooser.
                    com.framebynavin.app.reminders.RX01ChallengeFullScreenSettings()
                }

                Spacer(Modifier.height(18.dp))
                V144SectionTitle(
                    "CREATOR REMINDER DEFAULTS",
                    "Used for newly created projects. Personal reminder choices stay separate.",
                )
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
                    border = BacklotUX10Tokens.quietBorder(CinemaLine),
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
                    border = BacklotUX10Tokens.quietBorder(CinemaLine)) {
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
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BacklotUX10Tokens.quietBorder(CinemaLine)) {
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
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BacklotUX10Tokens.quietBorder(CinemaLine)) {
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

            V167SettingsSection.DATA -> Unit

            V167SettingsSection.PRIVACY -> V144SettingsPage("PRIVACY & PERMISSIONS", "Privacy and device access", ::navigateBack) {
                V144ActionCard(
                    title = "Data & privacy",
                    subtitle = "Review stored information and privacy controls",
                    icon = Icons.Outlined.Shield,
                    accent = MutedGold,
                    onClick = { context.startActivity(Intent(context, V269DataPrivacyActivity::class.java)) },
                )
                V144ActionCard(
                    title = "Notification permissions",
                    subtitle = "Check alarm and notification access",
                    icon = Icons.Outlined.NotificationsActive,
                    accent = MutedGold,
                    onClick = { sectionName = V167SettingsSection.REMINDERS.name },
                )
                Spacer(Modifier.height(12.dp))
                V144SettingsNote(
                    "This page explains permissions and stored-data controls. " +
                        "Cloud synchronization, backup recovery and account deletion are separate " +
                        "systems; a full deletion or backup workflow is not offered here yet."
                )
            }

            V167SettingsSection.ADVANCED -> V144SettingsPage("ADVANCED", "Device data controls", ::navigateBack) {
                V144ActionCard(
                    title = "Start fresh on this phone",
                    subtitle = "Review a confirmed device-only reset. This does not delete cloud data.",
                    icon = Icons.Outlined.DeleteForever,
                    accent = RecRed,
                    onClick = { context.startActivity(Intent(context, FreshStartActivity::class.java)) },
                )
            }

            V167SettingsSection.ABOUT -> V144SettingsPage("ABOUT BACKLOT", "Version, help and guided setup", { sectionName = V167SettingsSection.HOME.name }) {
                V144ActionCard(
                    title = "Show app tour",
                    subtitle = "Home → Ideas → Projects → Reminder Hub → Insights → Quick Actions",
                    icon = Icons.Outlined.School,
                    accent = MutedGold,
                    onClick = onReplayTour,
                )
                Spacer(Modifier.height(12.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BacklotUX10Tokens.quietBorder(CinemaLine)) {
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
    weeklyAutoPlanEnabled: Boolean,
    notificationsReady: Boolean,
    preciseTimingReady: Boolean,
    fullScreenReady: Boolean,
    batteryReady: Boolean,
    onClose: () -> Unit,
    onProfile: () -> Unit,
    onOpen: (V167SettingsSection) -> Unit,
    onSearchDestination: (V404SettingsDestination) -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val matches = remember(searchText) { V404SettingsIndex.search(searchText) }
    val reminderSupport = when {
        !notificationsReady -> "Notifications are off"
        !preciseTimingReady || !fullScreenReady || !batteryReady ->
            "Some advanced Android alert options need attention"
        else -> null
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = BacklotUX10Tokens.pageGutter).padding(bottom = 28.dp),
    ) {
        V144SettingsHeader("BACKLOT", "Settings", onClose)
        Spacer(Modifier.height(10.dp))
        UX12D6StudioProfileCard(onOpenPersonalization = {
            onOpen(V167SettingsSection.PERSONALIZATION)
        })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it.take(80) },
            modifier = Modifier.fillMaxWidth().testTag("v404-settings-search"),
            label = { Text("Find a setting") },
            placeholder = { Text("Voice, theme, reminders…") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedGold)
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear Settings search",
                            tint = MutedText)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.height(14.dp))

        if (searchText.isNotBlank()) {
            Text(
                if (matches.isEmpty()) "NO MATCHES" else "SETTINGS MATCHES",
                color = MutedGold, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 9.dp, bottom = 8.dp),
            )
            if (matches.isEmpty()) {
                Text(
                    "No settings match this search. Try 'voice', 'permissions', 'theme' or 'account'.",
                    color = MutedText, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)
                        .testTag("v404-settings-no-results"),
                )
            } else {
                Column(Modifier.fillMaxWidth().testTag("v404-settings-results")) {
                    matches.forEachIndexed { index, entry ->
                        V294QuietSettingRow(
                            title = entry.title,
                            icon = Icons.Outlined.ChevronRight,
                            support = entry.description,
                            onClick = { onSearchDestination(entry.destination) },
                        )
                        if (index != matches.lastIndex) V294QuietSettingsDivider()
                    }
                }
            }
        } else {
            V404SettingsGroupLabel("YOU & YOUR STUDIO")
            V294QuietSettingRow(
                title = "Account & profile",
                icon = Icons.Outlined.Person,
                support = "Creator ID, Google sign-in and personal details",
                onClick = onProfile,
            )
            V294QuietSettingsDivider()
            V294QuietSettingRow(
                title = "Appearance & sound",
                icon = Icons.Outlined.Palette,
                support = "${VisualExperiencePrefs.currentTheme.displayName} · ${VisualExperiencePrefs.welcomeStyle.displayName}",
            ) { onOpen(V167SettingsSection.PERSONALIZATION) }

            Spacer(Modifier.height(13.dp))
            V404SettingsGroupLabel("REMINDERS & WORKFLOW")
            V294QuietSettingRow(
                title = "Notifications & reminders",
                icon = Icons.Outlined.NotificationsActive,
                support = reminderSupport,
                isWarning = !notificationsReady,
            ) { onOpen(V167SettingsSection.REMINDERS) }
            V294QuietSettingsDivider()
            V294QuietSettingRow(
                title = "Planning & integrations",
                icon = Icons.Outlined.CalendarMonth,
                support = if (weeklyAutoPlanEnabled) "Weekly Auto Plan is enabled" else "Weekly plan, automation and calendar",
            ) { onOpen(V167SettingsSection.PLANNING) }

            Spacer(Modifier.height(13.dp))
            V404SettingsGroupLabel("PRIVACY & SUPPORT")
            V294QuietSettingRow(
                title = "Privacy & data",
                icon = Icons.Outlined.Shield,
                support = "Permissions and stored-data information",
            ) { onOpen(V167SettingsSection.PRIVACY) }
            V294QuietSettingsDivider()
            V294QuietSettingRow(
                title = "Help & about",
                icon = Icons.Outlined.Info,
                support = "Guided tour and app version",
            ) { onOpen(V167SettingsSection.ABOUT) }
            V294QuietSettingsDivider()
            V294QuietSettingRow(
                title = "Advanced",
                icon = Icons.Outlined.Tune,
                support = "Review device-only data erasure",
            ) { onOpen(V167SettingsSection.ADVANCED) }

            Spacer(Modifier.height(16.dp))
            Text(
                "Your projects and reminders are not changed by opening Settings. " +
                    "Device-level alarm permissions may affect delivery.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 9.dp),
            )
        }
    }
}

/**
 * UX12 D6: Read-only studio identity, backed by the already persisted
 * appearance, opening and companion choices. The whole panel opens the
 * existing Personalization route; it never writes settings itself.
 */
@Composable
private fun UX12D6StudioProfileCard(onOpenPersonalization: () -> Unit) {
    V144GuidePrefs.initialize(LocalContext.current)
    val guide = V144GuidePrefs.currentGuide
    Surface(
        onClick = onOpenPersonalization,
        modifier = Modifier.fillMaxWidth().testTag("ux12-d6-current-look"),
        shape = RoundedCornerShape(17.dp),
        color = CinemaSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "YOUR BACKLOT", color = MutedGold, fontSize = 10.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${VisualExperiencePrefs.currentTheme.displayName} · ${guide.displayName}",
                    color = ProjectorIvory, fontSize = 14.sp, lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Opening: ${VisualExperiencePrefs.welcomeStyle.displayName}",
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text("Personalize", color = RecRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            V144GuideCharacter(
                guide = guide, state = CinePulseState.THINK,
                modifier = Modifier.size(74.dp),
            )
        }
    }
}

@Composable
private fun V404SettingsGroupLabel(title: String) {
    Text(
        title, color = MutedGold,
        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 9.dp, top = 5.dp, bottom = 5.dp),
    )
}

@Composable
private fun V294QuietSettingRow(
    title: String,
    icon: ImageVector,
    support: String? = null,
    isWarning: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = BacklotUX10Tokens.rowTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (isWarning) RecRed else MutedGold,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, color = ProjectorIvory, fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            if (support != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    support, color = if (isWarning) RecRed else MutedText,
                    fontSize = 12.sp, lineHeight = 16.sp,
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight, contentDescription = null,
            tint = MutedText, modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun V294QuietSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp, end = 14.dp),
        thickness = 0.75.dp, color = CinemaLine.copy(alpha = 0.55f),
    )
}

@Composable
private fun V144SettingsPage(kicker: String, title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = BacklotUX10Tokens.pageGutter).padding(bottom = 28.dp),
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
            Text(kicker, color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
            Text(title, color = ProjectorIvory, fontSize = BacklotUX10Tokens.pageTitle, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
        border = null,
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
        onClick = onClick,
        enabled = !ready,
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
        shape = RoundedCornerShape(15.dp),
        color = CinemaSurface,
        border = BacklotUX10Tokens.quietBorder(CinemaLine),
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
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurfaceRaised, border = BacklotUX10Tokens.quietBorder(CinemaLine)) {
        Text(text, color = MutedText, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(12.dp))
    }
}
