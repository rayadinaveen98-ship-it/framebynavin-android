from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def save(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_once_or_done(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one old marker, found {count}")
    return text.replace(old, new, 1)


# ---------------------------------------------------------------------------
# Version: Beta 2 is a stabilization milestone, not a feature expansion.
# ---------------------------------------------------------------------------
path = "app/build.gradle.kts"
text = load(path)
text = replace_once_or_done(text, "versionCode = 117", "versionCode = 118", "version code")
text = replace_once_or_done(
    text,
    'versionName = "2.0.0-beta1.1-voice-quick-idea"',
    'versionName = "2.0.0-beta2-ux-reliability-audit"',
    "version name",
)
save(path, text)


# ---------------------------------------------------------------------------
# Navigation/state reliability + simpler primary surfaces.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
text = load(path)

text = replace_once_or_done(
    text,
    '''    fun routeJourney(destination: V18JourneyDestination?) {
        when (destination) {
            V18JourneyDestination.IDEAS -> tab = PTab.IDEAS
            V18JourneyDestination.CREATE -> tab = PTab.CREATE
            V18JourneyDestination.INSIGHTS -> tab = PTab.INSIGHTS
            null -> Unit
        }
        if (destination != null) overlay = POverlay.NONE
    }
''',
    '''    fun routeJourney(destination: V18JourneyDestination?) {
        when (destination) {
            V18JourneyDestination.IDEAS -> tab = PTab.IDEAS
            V18JourneyDestination.CREATE -> tab = PTab.CREATE
            V18JourneyDestination.INSIGHTS -> tab = PTab.INSIGHTS
            null -> Unit
        }
        if (destination != V18JourneyDestination.CREATE) externalStudioId = null
        if (destination != null) overlay = POverlay.NONE
    }
''',
    "journey stale studio state",
)

text = replace_once_or_done(
    text,
    '''            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                onCapture = { openComposer() },
''',
    '''            PBottomNav(
                selected = tab,
                onSelect = { destination ->
                    externalStudioId = null
                    tab = destination
                },
                onCapture = { openComposer() },
''',
    "manual navigation clears stale expansion",
)

text = replace_once_or_done(
    text,
    'contentDescription = "Capture idea",',
    'contentDescription = "Create project",',
    "bottom plus accessibility",
)

# Back from a tab must not leave an old externally-expanded project waiting behind Today.
text = replace_once_or_done(
    text,
    '''            overlay != POverlay.NONE -> overlay = POverlay.NONE
            else -> tab = PTab.TODAY
''',
    '''            overlay != POverlay.NONE -> overlay = POverlay.NONE
            else -> {
                externalStudioId = null
                tab = PTab.TODAY
            }
''',
    "back clears stale studio expansion",
)

# Control Center should describe outcomes, not implementation details.
control_copy = {
    'PControlRow("Quick Capture", "Save an idea to Idea Vault in seconds", Icons.Outlined.Bolt, onQuickCapture)':
        'PControlRow("Quick Capture", "Type or speak an idea", Icons.Outlined.Bolt, onQuickCapture)',
    'PControlRow("Daily Brief", "Focus, risk and the next 7 days", Icons.Outlined.Today, onDailyBrief)':
        'PControlRow("Daily Brief", "What needs your attention today", Icons.Outlined.Today, onDailyBrief)',
    'PControlRow("Content Calendar", "Projects + weekly plan for 14 days", Icons.Outlined.CalendarMonth, onCalendar)':
        'PControlRow("Content Calendar", "Upcoming projects and publishing", Icons.Outlined.CalendarMonth, onCalendar)',
    'PControlRow("Automation", "Auto planning and regular reminders", Icons.Outlined.AutoAwesome, onAutomation)':
        'PControlRow("Automation", "Recurring planning and reminders", Icons.Outlined.AutoAwesome, onAutomation)',
    '"Level ${creatorProgress.level} · ${creatorProgress.totalXp} XP · ${creatorProgress.weeklyMomentum} momentum",':
        '"Your consistency and milestones",',
}
for old, new in control_copy.items():
    text = replace_once_or_done(text, old, new, f"control copy: {old[:34]}")

# Settings: collapse the four permission rows behind one creator-friendly summary.
text = replace_once_or_done(
    text,
    '''    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
''',
    '''    val context = LocalContext.current
    val reminderSetupReady = permissions.notifications && permissions.preciseTiming && permissions.fullScreen && permissions.batteryAccess
    var showReminderSetup by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
''',
    "settings reminder disclosure state",
)

text = replace_once_or_done(
    text,
    '''            Spacer(Modifier.height(22.dp))
            PSettingsHeading("REMINDER SETUP", "Set this once. Project creation stays clean.")
            Spacer(Modifier.height(9.dp))
            PPermissionRow("Notifications", permissions.notifications, onNotifications)
            PPermissionRow("Exact reminder timing", permissions.preciseTiming, onPreciseTiming)
            PPermissionRow("Full-screen alerts", permissions.fullScreen, onFullScreen)
            PPermissionRow("Allow background reminders", permissions.batteryAccess, onBattery)

            Spacer(Modifier.height(22.dp))
''',
    '''            Spacer(Modifier.height(22.dp))
            PSettingsHeading("REMINDER SETUP", "Permissions used by project reminders.")
            Spacer(Modifier.height(9.dp))
            Surface(
                onClick = { showReminderSetup = !showReminderSetup },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, if (reminderSetupReady) CinemaLine else MutedGold.copy(alpha = .35f)),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (reminderSetupReady) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsActive,
                        null,
                        tint = if (reminderSetupReady) SuccessGreen else MutedGold,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Project reminders", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(if (reminderSetupReady) "Ready" else "Needs setup", color = if (reminderSetupReady) SuccessGreen else MutedGold, fontSize = 8.8.sp)
                    }
                    Icon(if (showReminderSetup) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MutedText)
                }
            }
            AnimatedVisibility(visible = showReminderSetup) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    PPermissionRow("Notifications", permissions.notifications, onNotifications)
                    PPermissionRow("Exact reminder timing", permissions.preciseTiming, onPreciseTiming)
                    PPermissionRow("Full-screen alerts", permissions.fullScreen, onFullScreen)
                    PPermissionRow("Allow background reminders", permissions.batteryAccess, onBattery)
                }
            }

            Spacer(Modifier.height(22.dp))
''',
    "collapsed reminder setup",
)

copy_updates = {
    'PSettingsHeading("PROFILE & ACCOUNT", "Identity, creator setup, connected services and account data.")':
        'PSettingsHeading("PROFILE & ACCOUNT", "Your creator profile and account.")',
    'PSettingsHeading("VOICE", "Preview the voices your phone can actually provide.")':
        'PSettingsHeading("VOICE", "Choose how reminder voices sound.")',
    'PSettingsHeading("BACKUP & SYNC", "Keep a cloud copy while your phone remains the main copy.")':
        'PSettingsHeading("BACKUP & SYNC", "Keep a safe cloud copy of your work.")',
}
for old, new in copy_updates.items():
    text = replace_once_or_done(text, old, new, f"settings copy: {old[:34]}")

save(path, text)


# ---------------------------------------------------------------------------
# Idea Vault: common capture/editing stays simple; advanced organization is
# progressive disclosure instead of a wall of chips.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt"
text = load(path)
if "import androidx.compose.animation.AnimatedVisibility" not in text:
    text = text.replace(
        "import android.app.TimePickerDialog\n",
        "import android.app.TimePickerDialog\nimport androidx.compose.animation.AnimatedVisibility\n",
        1,
    )
if "import androidx.compose.material.icons.outlined.ExpandLess" not in text:
    text = text.replace(
        "import androidx.compose.material.icons.outlined.Archive\n",
        "import androidx.compose.material.icons.outlined.Archive\nimport androidx.compose.material.icons.outlined.ExpandLess\nimport androidx.compose.material.icons.outlined.ExpandMore\n",
        1,
    )

text = replace_once_or_done(
    text,
    '''    var notes by remember(idea.id) { mutableStateOf(idea.notes) }
    val formats = v09Formats(platform)
''',
    '''    var notes by remember(idea.id) { mutableStateOf(idea.notes) }
    var showOrganize by rememberSaveable(idea.id) { mutableStateOf(false) }
    val formats = v09Formats(platform)
''',
    "idea organize disclosure state",
)

old_editor = '''                OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Idea title") })
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
'''
new_editor = '''                OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Idea title") })
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 95.dp), label = { Text("Notes · optional") })
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { showOrganize = !showOrganize },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CinemaSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ORGANIZE IDEA · OPTIONAL", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                            Text("Topic, status and publishing hints", color = MutedText, fontSize = 8.5.sp)
                        }
                        Icon(if (showOrganize) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MutedGold)
                    }
                }
                AnimatedVisibility(visible = showOrganize) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(topic, { topic = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Topic · optional") })
                        Spacer(Modifier.height(12.dp)); V09VaultLabel("CATEGORY")
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
                            IdeaPotential.entries.forEach { value ->
                                FilterChip(potential == value, { potential = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) })
                            }
                        }
                        Spacer(Modifier.height(12.dp)); V09VaultLabel("LIKELY PLATFORM")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            platformOptions.forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 10.sp) }) }
                        }
                        Spacer(Modifier.height(10.dp)); V09VaultLabel("LIKELY FORMAT")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            formats.forEach { value -> FilterChip(format == value, { format = value }, { Text(value, fontSize = 10.sp) }) }
                        }
                    }
                }
'''
text = replace_once_or_done(text, old_editor, new_editor, "idea editor progressive disclosure")
save(path, text)


# ---------------------------------------------------------------------------
# Project management wording: never expose recurrence implementation details.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt"
text = load(path)
plain_copy = {
    "This removes the selected project data from your creator workspace. Weekly generated occurrences are suppressed so they do not immediately come back.":
        "This permanently deletes the selected projects. Recurring schedule entries stay safe.",
    "This permanently deletes the selected projects. Weekly-plan occurrences are protected from being recreated immediately.":
        "This permanently deletes the selected projects. Recurring schedule entries stay safe.",
    "This permanently deletes the project. If it came from your Weekly Plan, this one will not be added again.":
        "This permanently deletes this project. It won't immediately come back from a recurring schedule.",
}
for old, new in plain_copy.items():
    text = text.replace(old, new)
save(path, text)


# ---------------------------------------------------------------------------
# Voice capture reliability: UI must stop immediately even if a recognizer
# delays or omits its terminal callback. Final results may still arrive.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/voice/IdeaVoiceTranscriber.kt"
text = load(path)
text = replace_once_or_done(
    text,
    '''    fun stop() {
        keepListening = false
        restartScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.stopListening() }
    }
''',
    '''    fun stop() {
        keepListening = false
        restartScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
        listener.onListeningChanged(false)
        runCatching { recognizer?.stopListening() }
    }
''',
    "voice stop state",
)
save(path, text)


# Keep generated Kotlin clean under strict diff checking.
for path in [
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt",
    "app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt",
    "app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt",
    "app/src/main/java/com/framebynavin/app/voice/IdeaVoiceTranscriber.kt",
]:
    value = load(path)
    had_newline = value.endswith("\n")
    value = "\n".join(line.rstrip() for line in value.splitlines())
    save(path, value + ("\n" if had_newline else ""))

print("v118 Beta 2 UX + reliability audit materialized")
