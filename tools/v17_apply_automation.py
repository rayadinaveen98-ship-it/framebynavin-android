from pathlib import Path


def rep(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)

p = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
s = p.read_text()

s = rep(
    s,
    "private enum class POverlay { NONE, WEEK, RELEASE, IDEAS, DAILY_BRIEF, CALENDAR, SETTINGS }",
    "private enum class POverlay { NONE, WEEK, RELEASE, IDEAS, DAILY_BRIEF, CALENDAR, AUTOMATION, SETTINGS }",
    "automation overlay enum",
)

s = rep(
    s,
    '''            CreatorWidgetContract.ACTION_NEW_PROJECT -> { overlay = POverlay.NONE; openComposer() }
            CreatorWidgetContract.ACTION_RELEASE_DAY -> overlay = POverlay.RELEASE
''',
    '''            CreatorWidgetContract.ACTION_NEW_PROJECT -> { overlay = POverlay.NONE; openComposer() }
            CreatorWidgetContract.ACTION_RELEASE_DAY -> overlay = POverlay.RELEASE
            CreatorWidgetContract.ACTION_DAILY_BRIEF -> overlay = POverlay.DAILY_BRIEF
            CreatorWidgetContract.ACTION_CONTENT_CALENDAR -> overlay = POverlay.CALENDAR
            CreatorWidgetContract.ACTION_IDEA_VAULT -> overlay = POverlay.IDEAS
            CreatorWidgetContract.ACTION_OPEN_INSIGHTS -> { overlay = POverlay.NONE; tab = PTab.INSIGHTS }
            CreatorWidgetContract.ACTION_AUTOMATION_CENTER -> overlay = POverlay.AUTOMATION
''',
    "widget automation routes",
)

s = rep(
    s,
    '''            POverlay.CALENDAR -> V15ContentCalendarScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.SETTINGS -> PSettingsScreen(''',
    '''            POverlay.CALENDAR -> V15ContentCalendarScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.AUTOMATION -> V17AutomationCenterScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,
                contextNudgesEnabled = settings.contextNudgesEnabled,
                onClose = { overlay = POverlay.NONE },
                onWeeklyAutoPlanChange = vm::setWeeklyAutoPlanEnabled,
            )
            POverlay.SETTINGS -> PSettingsScreen(''',
    "automation overlay screen",
)

s = rep(
    s,
    '''                onWeek = { showControl = false; overlay = POverlay.WEEK },
                onReminders = { showControl = false; showReminders = true },
                onSettings = { showControl = false; overlay = POverlay.SETTINGS },''',
    '''                onWeek = { showControl = false; overlay = POverlay.WEEK },
                onReminders = { showControl = false; showReminders = true },
                onAutomation = { showControl = false; overlay = POverlay.AUTOMATION },
                onSettings = { showControl = false; overlay = POverlay.SETTINGS },''',
    "control automation callback",
)

s = rep(
    s,
    '''    onWeek: () -> Unit,
    onReminders: () -> Unit,
    onSettings: () -> Unit,''',
    '''    onWeek: () -> Unit,
    onReminders: () -> Unit,
    onAutomation: () -> Unit,
    onSettings: () -> Unit,''',
    "control automation parameter",
)

s = rep(
    s,
    '''        PControlRow("Weekly Plan", if (weeklyAutoPlanEnabled) "Auto Plan on" else "Auto Plan off", Icons.Outlined.CalendarMonth, onWeek)
        PControlRow("Reminders", "See and edit active reminders", Icons.Outlined.Alarm, onReminders)
        PControlRow("Settings", "Voices, reminder setup and defaults", Icons.Outlined.Settings, onSettings)''',
    '''        PControlRow("Weekly Plan", if (weeklyAutoPlanEnabled) "Auto Plan on" else "Auto Plan off", Icons.Outlined.CalendarMonth, onWeek)
        PControlRow("Reminders", "See and edit active reminders", Icons.Outlined.Alarm, onReminders)
        PControlRow("Automation Center", "Background planning, routines and automatic follow-ups", Icons.Outlined.AutoAwesome, onAutomation)
        PControlRow("Settings", "Voices, reminder setup and defaults", Icons.Outlined.Settings, onSettings)''',
    "control automation row",
)

p.write_text(s)

p = Path("app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt")
s = p.read_text()
s = rep(
    s,
    "val occurrences = WeeklyScheduleEngine.upcomingOccurrences(weeklySlots, daysAhead = 8)",
    "val occurrences = WeeklyScheduleEngine.upcomingOccurrences(weeklySlots, daysAhead = CreatorAutoPlanEngine.DEFAULT_HORIZON_DAYS)",
    "foreground auto plan horizon",
)
p.write_text(s)
