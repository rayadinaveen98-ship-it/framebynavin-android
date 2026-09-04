from pathlib import Path
import re

ROOT = Path('.')


def rep(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: expected 1 match, found {count}')
    return text.replace(old, new, 1)

# 1) CreatorTask completion timestamp
p = ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorTask.kt'
s = p.read_text()
s = rep(
    s,
    '    val sourceRefId: String = "",\n    /** Non-zero means the project is hidden from active Plan/Studio but retained for history. */\n    val archivedAtMillis: Long = 0L,',
    '    val sourceRefId: String = "",\n    /** Set when FrameByNavin itself observes completion. Legacy completed projects remain 0. */\n    val completedAtMillis: Long = 0L,\n    /** Non-zero means the project is hidden from active Plan/Studio but retained for history. */\n    val archivedAtMillis: Long = 0L,',
    'CreatorTask completedAtMillis',
)
p.write_text(s)

# 2) Persist completion timestamp
p = ROOT / 'app/src/main/java/com/framebynavin/app/data/TaskStore.kt'
s = p.read_text()
s = rep(
    s,
    '                    .put("sourceRefId", task.sourceRefId)\n                    .put("archivedAtMillis", task.archivedAtMillis)',
    '                    .put("sourceRefId", task.sourceRefId)\n                    .put("completedAtMillis", task.completedAtMillis)\n                    .put("archivedAtMillis", task.archivedAtMillis)',
    'TaskStore encode completion',
)
s = rep(
    s,
    '                        sourceRefId = item.optString("sourceRefId", ""),\n                        archivedAtMillis = item.optLong("archivedAtMillis", 0L),',
    '                        sourceRefId = item.optString("sourceRefId", ""),\n                        completedAtMillis = item.optLong("completedAtMillis", 0L),\n                        archivedAtMillis = item.optLong("archivedAtMillis", 0L),',
    'TaskStore decode completion',
)
p.write_text(s)

# 3) Post-publish follow-ups in live ViewModel
p = ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt'
s = p.read_text()
old = '''    fun advanceWorkflow(id: String) = updateTask(id) { task ->
        val template = CreatorWorkflowEngine.templateFor(task)
        val currentIndex = CreatorWorkflowEngine.stageIndex(task)
        if (currentIndex >= template.stages.lastIndex) {
            cancelTaskAlerts(task.id)
            return@updateTask task.copy(
                status = TaskStatus.DONE,
                progress = 100,
                workflowStageIndex = template.stages.lastIndex,
                reminderEnabled = false,
                smartEscalationEnabled = false,
                voiceEnabled = false,
                reminderMode = ReminderMode.NONE,
                workingUntilMillis = 0L,
                autoStageReminder = false,
            )
        }

        val nextIndex = currentIndex + 1
        var updated = task.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = nextIndex,
            progress = CreatorWorkflowEngine.progressForStage(nextIndex, template.stages.size),
        )
        updated = applyAutoStageReminder(updated, nextIndex)
        scheduleTask(updated)
        updated
    }
'''
new = '''    fun advanceWorkflow(id: String) {
        var completedParent: CreatorTask? = null
        updateTask(id) { task ->
            val template = CreatorWorkflowEngine.templateFor(task)
            val currentIndex = CreatorWorkflowEngine.stageIndex(task)
            if (currentIndex >= template.stages.lastIndex) {
                cancelTaskAlerts(task.id)
                val completed = task.copy(
                    status = TaskStatus.DONE,
                    progress = 100,
                    workflowStageIndex = template.stages.lastIndex,
                    reminderEnabled = false,
                    smartEscalationEnabled = false,
                    voiceEnabled = false,
                    reminderMode = ReminderMode.NONE,
                    workingUntilMillis = 0L,
                    autoStageReminder = false,
                    completedAtMillis = task.completedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
                )
                completedParent = completed
                completed
            } else {
                val nextIndex = currentIndex + 1
                var updated = task.copy(
                    status = TaskStatus.WORKING,
                    workflowStageIndex = nextIndex,
                    progress = CreatorWorkflowEngine.progressForStage(nextIndex, template.stages.size),
                )
                updated = applyAutoStageReminder(updated, nextIndex)
                scheduleTask(updated)
                updated
            }
        }
        completedParent?.let(::ensurePostPublishFollowUps)
    }
'''
s = rep(s, old, new, 'advanceWorkflow')
old = '''    fun completeTask(id: String) = updateTask(id) { task ->
        cancelTaskAlerts(task.id)
        val template = CreatorWorkflowEngine.templateFor(task)
        task.copy(
            status = TaskStatus.DONE,
            progress = 100,
            workflowStageIndex = template.stages.lastIndex,
            reminderEnabled = false,
            smartEscalationEnabled = false,
            voiceEnabled = false,
            reminderMode = ReminderMode.NONE,
            workingUntilMillis = 0L,
            autoStageReminder = false,
        )
    }
'''
new = '''    fun completeTask(id: String) {
        var completedParent: CreatorTask? = null
        updateTask(id) { task ->
            cancelTaskAlerts(task.id)
            val template = CreatorWorkflowEngine.templateFor(task)
            val completed = task.copy(
                status = TaskStatus.DONE,
                progress = 100,
                workflowStageIndex = template.stages.lastIndex,
                reminderEnabled = false,
                smartEscalationEnabled = false,
                voiceEnabled = false,
                reminderMode = ReminderMode.NONE,
                workingUntilMillis = 0L,
                autoStageReminder = false,
                completedAtMillis = task.completedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            )
            completedParent = completed
            completed
        }
        completedParent?.let(::ensurePostPublishFollowUps)
    }
'''
s = rep(s, old, new, 'completeTask')
anchor = '    fun reconcileReminders() = reconcileSnapshot(tasks.toList())\n\n'
helper = '''    fun reconcileReminders() = reconcileSnapshot(tasks.toList())

    private fun ensurePostPublishFollowUps(parent: CreatorTask) {
        val specs = CreatorPostPublishEngine.specs(parent)
        if (specs.isEmpty()) return
        val baseTime = parent.completedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        val created = mutableListOf<CreatorTask>()
        specs.forEach { spec ->
            val sourceRef = CreatorPostPublishEngine.sourceRef(parent.id, spec.key)
            if (tasks.any { it.sourceRefId == sourceRef }) return@forEach
            val due = baseTime + spec.dueOffsetMinutes * 60_000L
            val template = CreatorWorkflowEngine.templateFor(spec.platform, spec.contentType)
            created += CreatorTask(
                id = UUID.randomUUID().toString(),
                title = spec.title,
                platform = spec.platform,
                contentType = spec.contentType,
                dueLabel = WeeklyScheduleEngine.dueLabel(due),
                dueAtMillis = due,
                status = TaskStatus.PLANNED,
                progress = 0,
                workflowStageIndex = 0.coerceAtMost(template.stages.lastIndex),
                reminderEnabled = false,
                reminderAtMillis = 0L,
                priority = spec.priority,
                notes = "Auto follow-up from published project · ${parent.title}",
                reminderMode = ReminderMode.NONE,
                origin = CreatorTaskOrigin.MANUAL,
                sourceRefId = sourceRef,
            )
        }
        if (created.isNotEmpty()) {
            created.asReversed().forEach { tasks.add(0, it) }
            persist()
        }
    }

'''
s = rep(s, anchor, helper, 'post-publish helper anchor')
p.write_text(s)

# 4) YouTube store can recover any cached window without calling the account disconnected
p = ROOT / 'app/src/main/java/com/framebynavin/app/youtube/YouTubeAnalyticsStore.kt'
s = p.read_text()
s = rep(
    s,
    '    fun hasConnection(): Boolean = prefs.getString(KEY_CHANNEL_ID, null).isNullOrBlank().not()\n',
    '    fun hasConnection(): Boolean = prefs.getString(KEY_CHANNEL_ID, null).isNullOrBlank().not()\n\n    fun loadAny(): YouTubeAnalyticsSnapshot? = listOf(28, 7, 90).firstNotNullOfOrNull { load(it) }\n',
    'YouTube loadAny',
)
p.write_text(s)

# 5) Fix YouTube range auth state and add Creator Intelligence card
p = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt'
s = p.read_text()
s = rep(
    s,
    '    var snapshot by remember(windowDays) { mutableStateOf(store.load(windowDays)) }\n',
    '    var snapshot by remember { mutableStateOf(store.load(windowDays) ?: store.loadAny()) }\n    var pendingSyncDays by rememberSaveable { mutableIntStateOf(windowDays) }\n',
    'YT snapshot state',
)
s = rep(
    s,
    '''    LaunchedEffect(windowDays) {
        snapshot = store.load(windowDays)
        authError = null
    }
''',
    '''    LaunchedEffect(windowDays) {
        store.load(windowDays)?.let { snapshot = it }
        authError = null
    }
''',
    'YT window effect',
)
s = rep(s, '    fun syncWithToken(token: String) {\n', '    fun syncWithToken(token: String, days: Int = windowDays) {\n', 'YT sync signature')
s = rep(s, '                withContext(Dispatchers.IO) { api.sync(token, windowDays) }\n', '                withContext(Dispatchers.IO) { api.sync(token, days) }\n', 'YT sync days')
s = rep(s, '            syncWithToken(token)\n', '            syncWithToken(token, pendingSyncDays)\n', 'YT resolution token')
s = rep(s, '    fun authorize(selectAccount: Boolean = false) {\n', '    fun authorize(selectAccount: Boolean = false, days: Int = windowDays) {\n', 'YT authorize signature')
s = rep(
    s,
    '        syncing = true\n        authError = null\n        client.authorize(YouTubeAuthorization.request(selectAccount))',
    '        syncing = true\n        authError = null\n        pendingSyncDays = days\n        client.authorize(YouTubeAuthorization.request(selectAccount))',
    'YT pending days',
)
s = rep(s, '                        syncWithToken(token)\n', '                        syncWithToken(token, pendingSyncDays)\n', 'YT direct token')
s = rep(
    s,
    '                    onWindow = { windowDays = it },\n                    onSync = { authorize(false) },\n                    onSwitchAccount = { authorize(true) },',
    '''                    onWindow = { days ->
                        if (days != windowDays) {
                            windowDays = days
                            val cached = store.load(days)
                            if (cached != null) snapshot = cached else authorize(false, days)
                        }
                    },
                    onSync = { authorize(false, windowDays) },
                    onSwitchAccount = { authorize(true, windowDays) },''',
    'YT header callbacks',
)
s = rep(
    s,
    '                YTFormatSignal(data, tasks, links)\n                Spacer(Modifier.height(18.dp))\n                YTRecentVideos(data, tasks, links) { selectedVideo = it }',
    '                YTFormatSignal(data, tasks, links)\n                Spacer(Modifier.height(18.dp))\n                V16CreatorIntelligenceCard(tasks, ideas, data, links)\n                Spacer(Modifier.height(18.dp))\n                YTRecentVideos(data, tasks, links) { selectedVideo = it }',
    'YT intelligence card',
)
p.write_text(s)

# 6) Home: icon-only Control at rest, IST greeting, TODAY moved into work section
p = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
s = p.read_text()
s = rep(s, '    var showControl by rememberSaveable { mutableStateOf(false) }\n', '    var showControl by rememberSaveable { mutableStateOf(false) }\n    var controlExpanded by rememberSaveable { mutableStateOf(false) }\n', 'control expanded state')
s = rep(
    s,
    '''    fun openComposer(id: String? = null) {
        editTaskId = id
        showComposer = true
    }

    LaunchedEffect(externalLaunch?.nonce) {''',
    '''    fun openComposer(id: String? = null) {
        editTaskId = id
        showComposer = true
    }

    LaunchedEffect(controlExpanded) {
        if (controlExpanded) {
            delay(180L)
            showControl = true
            controlExpanded = false
        }
    }

    LaunchedEffect(externalLaunch?.nonce) {''',
    'control expansion effect',
)
old_control = '''            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { showControl = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 98.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed,
                    shadowElevation = 10.dp,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.GridView, "Control", tint = ProjectorIvory, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("CONTROL", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
'''
new_control = '''            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
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
                                Text("CONTROL", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
'''
s = rep(s, old_control, new_control, 'control FAB')
s = rep(s, '            PTopBar("TODAY", onAdd)\n            Spacer(Modifier.height(14.dp))', '            PHomeGreetingHeader(onAdd)\n            Spacer(Modifier.height(12.dp))', 'home header')
s = rep(
    s,
    '''            V131HomeHeroSlideshow()
            Spacer(Modifier.height(22.dp))
            Text("Make the next thing.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)''',
    '''            V131HomeHeroSlideshow()
            Spacer(Modifier.height(18.dp))
            Text("TODAY", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(4.dp))
            Text("Make the next thing.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)''',
    'TODAY section placement',
)
anchor = '''@Composable
private fun PTopBar(label: String, onAdd: () -> Unit) {'''
header = '''@Composable
private fun PHomeGreetingHeader(onAdd: () -> Unit) {
    val hour = remember { java.time.ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).hour }
    val greeting = when (hour) {
        in 5..11 -> "Good Morning, Navin"
        in 12..16 -> "Good Afternoon, Navin"
        in 17..20 -> "Good Evening, Navin"
        else -> "Good Night, Navin"
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(greeting, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Create project", tint = ProjectorIvory, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun PTopBar(label: String, onAdd: () -> Unit) {'''
s = rep(s, anchor, header, 'home greeting function')
p.write_text(s)

# 7) Slideshow becomes image-only and more compact
p = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt'
s = p.read_text()
pattern = re.compile(r'@Composable\ninternal fun V131HomeHeroSlideshow\(\) \{.*?\n\}\n\n@Composable\ninternal fun V131PlanScreen', re.S)
replacement = '''@Composable
internal fun V131HomeHeroSlideshow() {
    val resourceIds = remember {
        listOf(
            R.drawable.hero_frame_01,
            R.drawable.hero_frame_02,
            R.drawable.hero_frame_03,
            R.drawable.hero_frame_04,
            R.drawable.hero_frame_05,
            R.drawable.hero_frame_06,
            R.drawable.hero_frame_07,
            R.drawable.hero_frame_08,
            R.drawable.hero_frame_09,
            R.drawable.hero_frame_10,
        )
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resourceIds.size) {
        while (resourceIds.size > 1) {
            delay(5_000L)
            index = (index + 1) % resourceIds.size
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(206.dp),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine.copy(alpha = .40f)),
        shadowElevation = 8.dp,
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
            AnimatedContent(
                targetState = index.coerceIn(0, resourceIds.lastIndex),
                transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
                label = "cinemaHeroImageOnly",
            ) { visibleIndex ->
                Image(
                    painter = painterResource(resourceIds[visibleIndex]),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
internal fun V131PlanScreen'''
s2, count = pattern.subn(replacement, s, count=1)
if count != 1:
    raise RuntimeError(f'slideshow replacement: expected 1, found {count}')
p.write_text(s2)

print('v1.6 creator intelligence wiring applied')
