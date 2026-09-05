from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
ROOT_UI = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
TODAY = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt'
IDEAS = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt'
CREATE = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt'
CALENDAR = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V15ContextUi.kt'
INSIGHTS = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 53', 'versionCode = 54', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha13"',
    'versionName = "1.8.0-product-alpha14"',
    'versionName',
)
BUILD.write_text(build)

root = ROOT_UI.read_text()
root = replace_once(root, 'import androidx.compose.animation.AnimatedVisibility\n', '', 'remove control animation import')
root = replace_once(root, '    var controlExpanded by rememberSaveable { mutableStateOf(false) }\n', '', 'remove control expanded state')
root = replace_once(
    root,
    '''    LaunchedEffect(controlExpanded) {
        if (controlExpanded) {
            delay(180L)
            showControl = true
            controlExpanded = false
        }
    }

''',
    '',
    'remove delayed control launch',
)
root = replace_once(
    root,
    '    val focusTask = vm.tasks.firstOrNull { it.id == focusTaskId }',
    '    val focusTask by remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }',
    'memoize focus task lookup',
)
root = replace_once(
    root,
    '''            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { if (!controlExpanded) controlExpanded = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 98.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed,
                    shadowElevation = 10.dp,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.GridView, "Control", tint = ProjectorIvory, modifier = Modifier.size(19.dp))
                        AnimatedVisibility(visible = controlExpanded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(7.dp))
                                Text("CONTROL", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
''',
    '''            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { showControl = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 96.dp).size(46.dp),
                    shape = CircleShape,
                    color = CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, CinemaLine),
                    shadowElevation = 7.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.GridView, "Open control center", tint = ProjectorIvory, modifier = Modifier.size(19.dp))
                    }
                }
            }
''',
    'simplify control affordance',
)
root = replace_once(
    root,
    '''        modifier = modifier,
    ) {
        Column(
            Modifier.padding(vertical = 7.dp),''',
    '''        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Column(
            Modifier.padding(vertical = 7.dp),''',
    'bottom nav touch target',
)
root = replace_once(
    root,
    '''                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            )''',
    '''                fontSize = 9.5.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )''',
    'bottom nav label resilience',
)
ROOT_UI.write_text(root)

today = TODAY.read_text()
today = replace_once(
    today,
    '''    val queue = remember(tasks.toList()) { pActiveQueue(tasks).take(10) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(queue.map { it.id }) { if (queue.none { it.id == selectedId }) selectedId = queue.firstOrNull()?.id }
    val index = queue.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val selected = queue.getOrNull(index)
    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val personalization = remember(creatorProfile, tasks.toList()) {
        CreatorPersonalizationEngine.snapshot(creatorProfile, tasks)
    }
''',
    '''    val queue by remember { derivedStateOf { pActiveQueue(tasks).take(10) } }
    val queueIds by remember { derivedStateOf { queue.map { it.id } } }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(queueIds) { if (queue.none { it.id == selectedId }) selectedId = queue.firstOrNull()?.id }
    val index = queue.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val selected = queue.getOrNull(index)
    val doneCount by remember { derivedStateOf { tasks.count { it.status == TaskStatus.DONE } } }
    val personalization by remember(creatorProfile) {
        derivedStateOf { CreatorPersonalizationEngine.snapshot(creatorProfile, tasks) }
    }
''',
    'today derived state cache',
)
TODAY.write_text(today)

ideas = IDEAS.read_text()
ideas = replace_once(
    ideas,
    'import androidx.compose.runtime.*\n',
    'import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable\n',
    'idea saveable import',
)
ideas = replace_once(
    ideas,
    '''    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<IdeaStatus?>(null) }
    var categoryFilter by remember { mutableStateOf<IdeaCategory?>(null) }
''',
    '''    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<IdeaStatus?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<IdeaCategory?>(null) }
''',
    'idea filter state persistence',
)
ideas = replace_once(
    ideas,
    '''    val opportunityAlerts = remember(ideas.toList(), YouTubeAnalyticsStore.latest24HourReport) {
        YouTubeOpportunityEngine.build(YouTubeAnalyticsStore.latest24HourReport, ideas)
    }
    val opportunityIdeaIds = remember(opportunityAlerts) { opportunityAlerts.mapNotNull { it.ideaId }.toSet() }
    val opportunityMatch = opportunityAlerts.firstOrNull { it.ideaId != null }

    val filtered = remember(ideas.toList(), query, statusFilter, categoryFilter, opportunityIdeaIds) {
        ideas.filter { idea ->
            val textMatch = query.isBlank() || listOf(idea.title, idea.topic, idea.notes)
                .any { it.contains(query, ignoreCase = true) }
            val statusMatch = statusFilter == null || idea.status == statusFilter
            val categoryMatch = categoryFilter == null || idea.category == categoryFilter
            textMatch && statusMatch && categoryMatch
        }.sortedWith(
            compareByDescending<CreatorIdea> { it.id in opportunityIdeaIds }
                .thenBy { it.status == IdeaStatus.ARCHIVED }
                .thenByDescending { it.updatedAtMillis }
        )
    }
''',
    '''    val opportunityReport = YouTubeAnalyticsStore.latest24HourReport
    val opportunityAlerts by remember(opportunityReport) {
        derivedStateOf { YouTubeOpportunityEngine.build(opportunityReport, ideas) }
    }
    val opportunityIdeaIds by remember { derivedStateOf { opportunityAlerts.mapNotNull { it.ideaId }.toSet() } }
    val opportunityMatch by remember { derivedStateOf { opportunityAlerts.firstOrNull { it.ideaId != null } } }
    val readyCount by remember { derivedStateOf { ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE } } }

    val filtered by remember {
        derivedStateOf {
            ideas.filter { idea ->
                val textMatch = query.isBlank() || listOf(idea.title, idea.topic, idea.notes)
                    .any { it.contains(query, ignoreCase = true) }
                val statusMatch = statusFilter == null || idea.status == statusFilter
                val categoryMatch = categoryFilter == null || idea.category == categoryFilter
                textMatch && statusMatch && categoryMatch
            }.sortedWith(
                compareByDescending<CreatorIdea> { it.id in opportunityIdeaIds }
                    .thenBy { it.status == IdeaStatus.ARCHIVED }
                    .thenByDescending { it.updatedAtMillis }
            )
        }
    }
''',
    'idea derived state cache',
)
ideas = replace_once(
    ideas,
    'Text("${ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }} READY", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)',
    'Text("$readyCount READY", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)',
    'idea ready count cache',
)
IDEAS.write_text(ideas)

create = CREATE.read_text()
create = replace_once(
    create,
    '''    val projects = tasks.filter { it.status != TaskStatus.SKIPPED && it.archivedAtMillis <= 0L }
        .sortedWith(compareBy<CreatorTask> { it.status == TaskStatus.DONE }.thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE })
    val archived = tasks.filter { it.archivedAtMillis > 0L }.sortedByDescending { it.archivedAtMillis }
''',
    '''    val projects by remember {
        derivedStateOf {
            tasks.filter { it.status != TaskStatus.SKIPPED && it.archivedAtMillis <= 0L }
                .sortedWith(compareBy<CreatorTask> { it.status == TaskStatus.DONE }.thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE })
        }
    }
    val archived by remember {
        derivedStateOf { tasks.filter { it.archivedAtMillis > 0L }.sortedByDescending { it.archivedAtMillis } }
    }
''',
    'studio derived projects',
)
CREATE.write_text(create)

calendar = CALENDAR.read_text()
calendar = replace_once(
    calendar,
    '''    val items = CreatorContentCalendarEngine.upcoming(tasks, weeklySlots, daysAhead = 14)
    val grouped = CreatorContentCalendarEngine.groupedByDate(items)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
''',
    '''    val grouped by remember {
        derivedStateOf {
            CreatorContentCalendarEngine.groupedByDate(
                CreatorContentCalendarEngine.upcoming(tasks, weeklySlots, daysAhead = 14),
            )
        }
    }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(zone) { LocalDate.now(zone) }
''',
    'calendar derived groups',
)
CALENDAR.write_text(calendar)

insights = INSIGHTS.read_text()
insights = replace_once(
    insights,
    '''    val personalization = remember(creatorProfile, tasks.toList()) {
        CreatorPersonalizationEngine.snapshot(creatorProfile, tasks)
    }
''',
    '''    val personalization by remember(creatorProfile) {
        derivedStateOf { CreatorPersonalizationEngine.snapshot(creatorProfile, tasks) }
    }
''',
    'insights personalization cache',
)
INSIGHTS.write_text(insights)

print('Applied v1.8 Product Alpha14 UX and performance pass')
