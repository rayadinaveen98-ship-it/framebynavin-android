from pathlib import Path

APP = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: expected exactly 1 match, found {count}')
    return text.replace(old, new, 1)


s = APP.read_text()

s = replace_once(
    s,
    'private enum class POverlay { NONE, WEEK, RELEASE, IDEAS, SETTINGS }',
    'private enum class POverlay { NONE, WEEK, RELEASE, IDEAS, DAILY_BRIEF, CALENDAR, SETTINGS }',
    'overlay enum',
)

s = replace_once(
    s,
    '''            POverlay.IDEAS -> V09IdeaVaultScreen(
                ideas = vm.ideas,
                onClose = { overlay = POverlay.NONE },
                onSave = vm::saveIdea,
                onDelete = vm::deleteIdea,
                onArchive = vm::archiveIdea,
                onConvert = vm::convertIdeaToProject,
            )
            POverlay.SETTINGS -> PSettingsScreen(''',
    '''            POverlay.IDEAS -> V09IdeaVaultScreen(
                ideas = vm.ideas,
                onClose = { overlay = POverlay.NONE },
                onSave = vm::saveIdea,
                onDelete = vm::deleteIdea,
                onArchive = vm::archiveIdea,
                onConvert = vm::convertIdeaToProject,
            )
            POverlay.DAILY_BRIEF -> V15DailyBriefScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.CALENDAR -> V15ContentCalendarScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.SETTINGS -> PSettingsScreen(''',
    'overlay screens',
)

s = replace_once(
    s,
    '''                onWeeklyAutoPlan = vm::setWeeklyAutoPlanEnabled,
                onNotifications = ::requestNotifications,''',
    '''                onWeeklyAutoPlan = vm::setWeeklyAutoPlanEnabled,
                onContextNudges = {
                    settingsStore.setContextNudgesEnabled(it)
                    settings = settingsStore.snapshot()
                },
                onNotifications = ::requestNotifications,''',
    'settings callback',
)

s = replace_once(
    s,
    '''                onQuickCapture = { showControl = false; showQuickCapture = true },
                onRelease = { showControl = false; overlay = POverlay.RELEASE },''',
    '''                onQuickCapture = { showControl = false; showQuickCapture = true },
                onDailyBrief = { showControl = false; overlay = POverlay.DAILY_BRIEF },
                onCalendar = { showControl = false; overlay = POverlay.CALENDAR },
                onRelease = { showControl = false; overlay = POverlay.RELEASE },''',
    'control callbacks',
)

s = replace_once(
    s,
    '''    onNewProject: () -> Unit,
    onQuickCapture: () -> Unit,
    onRelease: () -> Unit,''',
    '''    onNewProject: () -> Unit,
    onQuickCapture: () -> Unit,
    onDailyBrief: () -> Unit,
    onCalendar: () -> Unit,
    onRelease: () -> Unit,''',
    'control signature',
)

s = replace_once(
    s,
    '''        PControlRow("Quick Capture", "Save an idea to Inbox in seconds", Icons.Outlined.Bolt, onQuickCapture)
        PControlRow("Idea Vault", if (readyIdeas > 0) "$readyIdeas ideas ready to make" else "Capture what you might make later", Icons.Outlined.Lightbulb, onIdeas)''',
    '''        PControlRow("Quick Capture", "Save an idea to Inbox in seconds", Icons.Outlined.Bolt, onQuickCapture)
        PControlRow("Daily Brief", "Focus, risk and the next 7 days", Icons.Outlined.Today, onDailyBrief)
        PControlRow("Content Calendar", "Projects + weekly plan for 14 days", Icons.Outlined.CalendarMonth, onCalendar)
        PControlRow("Idea Vault", if (readyIdeas > 0) "$readyIdeas ideas ready to make" else "Capture what you might make later", Icons.Outlined.Lightbulb, onIdeas)''',
    'control rows',
)

s = replace_once(
    s,
    '''    onSnooze: (Int) -> Unit,
    onWeeklyAutoPlan: (Boolean) -> Unit,
    onNotifications: () -> Unit,''',
    '''    onSnooze: (Int) -> Unit,
    onWeeklyAutoPlan: (Boolean) -> Unit,
    onContextNudges: (Boolean) -> Unit,
    onNotifications: () -> Unit,''',
    'settings signature',
)

s = replace_once(
    s,
    '''            Spacer(Modifier.height(22.dp))
            PSettingsHeading("VOICE", "Preview the voices your phone can actually provide.")''',
    '''            Spacer(Modifier.height(22.dp))
            PSettingsHeading("CONTEXT NUDGES", "Optional gentle alerts when active creator work is at risk.")
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Creator context nudges", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                !permissions.notifications -> "Enable notification permission above first."
                                settings.contextNudgesEnabled -> "On · checks periodically for overdue or at-risk active work."
                                else -> "Off · exact reminders still work normally."
                            },
                            color = MutedText,
                            fontSize = 8.8.sp,
                        )
                    }
                    Switch(
                        checked = settings.contextNudgesEnabled,
                        onCheckedChange = onContextNudges,
                        enabled = permissions.notifications,
                        colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("VOICE", "Preview the voices your phone can actually provide.")''',
    'context settings section',
)

APP.write_text(s)
print('v1.5 context wiring applied')
