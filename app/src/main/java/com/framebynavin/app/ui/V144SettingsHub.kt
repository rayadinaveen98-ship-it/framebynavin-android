package com.framebynavin.app.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import java.util.Locale

private enum class V144SettingsSection {
    HOME,
    APPEARANCE,
    GUIDE,
    VOICE,
    REMINDERS,
    PLANNING,
    CONNECTIONS,
    DATA,
    ABOUT,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V144SettingsHub(
    settings: CreatorOsSettings,
    tasks: List<CreatorTask>,
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
    onWeeklyAutoPlan: (Boolean) -> Unit,
    onContextNudges: (Boolean) -> Unit,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onReplayTour: () -> Unit,
    onCloudSync: () -> Unit,
    onYouTube: () -> Unit,
) {
    val context = LocalContext.current
    var sectionName by rememberSaveable { mutableStateOf(V144SettingsSection.HOME.name) }
    val section = V144SettingsSection.entries.firstOrNull { it.name == sectionName } ?: V144SettingsSection.HOME

    BackHandler {
        if (section == V144SettingsSection.HOME) onClose() else sectionName = V144SettingsSection.HOME.name
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        when (section) {
            V144SettingsSection.HOME -> V144SettingsHome(
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

            V144SettingsSection.APPEARANCE -> V144SettingsPage("APPEARANCE", "Your Backlot world", { sectionName = V144SettingsSection.HOME.name }) {
                V127AppearanceSettings(showGuidePicker = false)
            }

            V144SettingsSection.GUIDE -> V144SettingsPage("GUIDE CHARACTER", "Choose who guides your creator journey", { sectionName = V144SettingsSection.HOME.name }) {
                V144GuidePicker()
                Spacer(Modifier.height(12.dp))
                V144SettingsNote("Your guide choice is used across setup, guided tours and helper moments. Frame and Navi keep their vector renderer; Funny and Cute use the premium raster character system.")
            }

            V144SettingsSection.VOICE -> V144SettingsPage("VOICE", "Default voice for new projects", { sectionName = V144SettingsSection.HOME.name }) {
                V140VoiceStudioPicker(
                    selected = settings.defaultVoicePersona,
                    onSelected = onVoice,
                    onPreview = { v144PreviewVoice(context, it) },
                )
                Spacer(Modifier.height(8.dp))
                V144SettingsNote("New projects inherit this voice automatically. Existing projects keep their saved voice unless you edit them.")
            }

            V144SettingsSection.REMINDERS -> V144SettingsPage("NOTIFICATIONS & REMINDERS", "Timing, permissions and defaults", { sectionName = V144SettingsSection.HOME.name }) {
                val setupReady = notificationsReady && preciseTimingReady && fullScreenReady && batteryReady
                V144StatusCard(
                    title = "Project reminders",
                    subtitle = if (setupReady) "Everything is ready." else "One or more Android permissions still need setup.",
                    icon = if (setupReady) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsActive,
                    accent = if (setupReady) SuccessGreen else MutedGold,
                )
                Spacer(Modifier.height(10.dp))
                V144PermissionRow("Notifications", notificationsReady, onNotifications)
                V144PermissionRow("Exact reminder timing", preciseTimingReady, onPreciseTiming)
                V144PermissionRow("Full-screen alerts", fullScreenReady, onFullScreen)
                V144PermissionRow("Allow background reminders", batteryReady, onBattery)

                Spacer(Modifier.height(18.dp))
                V144SectionTitle("REMINDER DEFAULTS", "These choices are reused automatically.")
                Spacer(Modifier.height(10.dp))
                Text("Snooze", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                    listOf(5, 10, 15, 20, 30).forEach { value ->
                        FilterChip(settings.snoozeMinutes == value, { onSnooze(value) }, { Text("${value}m", fontSize = 10.sp) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Alarm auto-stop", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                    listOf(30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m").forEach { (value, label) ->
                        FilterChip(settings.defaultAlarmTimeoutSeconds == value, { onAlarmTimeout(value) }, { Text(label, fontSize = 10.sp) })
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
                                    settings.contextNudgesEnabled -> "On · Backlot can warn you when active work may need attention."
                                    else -> "Off · normal project reminders still work."
                                },
                                color = MutedText,
                                fontSize = 8.8.sp,
                                lineHeight = 12.sp,
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

            V144SettingsSection.PLANNING -> V144SettingsPage("PLANNING & CALENDAR", "Recurring planning and calendar sync", { sectionName = V144SettingsSection.HOME.name }) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(MutedGold.copy(alpha = .11f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Weekly Auto Plan", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(if (weeklyAutoPlanEnabled) "Recurring projects may be added automatically." else "Nothing is added automatically.", color = MutedText, fontSize = 8.8.sp)
                        }
                        Switch(weeklyAutoPlanEnabled, onWeeklyAutoPlan, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
                    }
                }
                Spacer(Modifier.height(18.dp))
                V129GoogleCalendarSettings(tasks)
            }

            V144SettingsSection.CONNECTIONS -> V144SettingsPage("CONNECTIONS", "YouTube and Google services", { sectionName = V144SettingsSection.HOME.name }) {
                V144ActionCard(
                    title = "YouTube Analytics",
                    subtitle = "Real channel performance, matched uploads and creator insights.",
                    icon = Icons.Outlined.SmartDisplay,
                    accent = RecRed,
                    onClick = onYouTube,
                )
                Spacer(Modifier.height(9.dp))
                V144ActionCard(
                    title = "Google account & Cloud Sync",
                    subtitle = "Connected account, cloud backups and restore state.",
                    icon = Icons.Outlined.CloudSync,
                    accent = MutedGold,
                    onClick = onCloudSync,
                )
                Spacer(Modifier.height(12.dp))
                V144SettingsNote("Google Calendar controls live under Planning & Calendar because they affect your publishing workflow rather than account identity.")
            }

            V144SettingsSection.DATA -> V144SettingsPage("DATA, BACKUP & SYNC", "Keep your Backlot data recoverable", { sectionName = V144SettingsSection.HOME.name }) {
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
                    subtitle = "Projects, reminders, ideas, weekly plan and settings.",
                    icon = Icons.Outlined.SaveAlt,
                    accent = MutedGold,
                    onClick = { context.startActivity(Intent(context, BackupActivity::class.java)) },
                )
            }

            V144SettingsSection.ABOUT -> V144SettingsPage("ABOUT BACKLOT", "Version, help and guided setup", { sectionName = V144SettingsSection.HOME.name }) {
                V144ActionCard(
                    title = "Replay guided first run",
                    subtitle = "Today → Ideas → Project → Workspace → Insights → Control",
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
    onOpen: (V144SettingsSection) -> Unit,
) {
    val reminderReady = notificationsReady && preciseTimingReady && fullScreenReady && batteryReady
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 44.dp),
    ) {
        V144SettingsHeader("SETTINGS", "Backlot, your way", onClose)
        Spacer(Modifier.height(10.dp))
        Text(
            "Everything is grouped by purpose now — open only what you want to change.",
            color = MutedText,
            fontSize = 9.5.sp,
            lineHeight = 14.sp,
        )
        Spacer(Modifier.height(18.dp))

        V144CategoryCard(
            title = "Profile & Account",
            subtitle = settings.creatorProfile.safeDisplayName + settings.creatorProfile.category.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
            icon = Icons.Outlined.Person,
            accent = MutedGold,
            onClick = onProfile,
        )
        V144CategoryCard("Appearance", "Theme, app icon and launch sound", Icons.Outlined.Palette, RecRed) { onOpen(V144SettingsSection.APPEARANCE) }
        V144CategoryCard("Guide Character", "Frame, Navi, Funny or Cute", Icons.Outlined.Face, MutedGold) { onOpen(V144SettingsSection.GUIDE) }
        V144CategoryCard("Voice", "Default project voice and preview", Icons.Outlined.RecordVoiceOver, RecRed) { onOpen(V144SettingsSection.VOICE) }
        V144CategoryCard(
            "Notifications & Reminders",
            if (reminderReady) "Ready · timing and reminder defaults" else "Needs setup · permissions and defaults",
            Icons.Outlined.NotificationsActive,
            if (reminderReady) SuccessGreen else MutedGold,
        ) { onOpen(V144SettingsSection.REMINDERS) }
        V144CategoryCard(
            "Planning & Calendar",
            if (weeklyAutoPlanEnabled) "Weekly Auto Plan on · Google Calendar" else "Weekly Auto Plan off · Google Calendar",
            Icons.Outlined.CalendarMonth,
            MutedGold,
        ) { onOpen(V144SettingsSection.PLANNING) }
        V144CategoryCard("Connections", "YouTube Analytics · Google account", Icons.Outlined.Link, RecRed) { onOpen(V144SettingsSection.CONNECTIONS) }
        V144CategoryCard("Data, Backup & Sync", "Cloud sync and local recovery", Icons.Outlined.CloudSync, MutedGold) { onOpen(V144SettingsSection.DATA) }
        V144CategoryCard("About Backlot", "Guided tour and app version", Icons.Outlined.Info, RecRed) { onOpen(V144SettingsSection.ABOUT) }
    }
}

@Composable
private fun V144SettingsPage(kicker: String, title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 44.dp),
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
                Text(title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText, fontSize = 8.8.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                Text(subtitle, color = MutedText, fontSize = 8.8.sp, lineHeight = 12.sp)
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
            Text(label, color = ProjectorIvory, fontSize = 10.5.sp, modifier = Modifier.weight(1f))
            Text(if (ready) "READY" else "SET UP", color = if (ready) SuccessGreen else RecRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V144SectionTitle(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = MutedText, fontSize = 8.8.sp)
}

@Composable
private fun V144SettingsNote(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp, modifier = Modifier.padding(12.dp))
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
