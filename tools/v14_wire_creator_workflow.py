from pathlib import Path

APP = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
COMPOSER = Path('app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: expected exactly 1 match, found {count}')
    return text.replace(old, new, 1)


s = APP.read_text()

s = replace_once(
    s,
    '    var showControl by rememberSaveable { mutableStateOf(false) }\n    var showReminders by rememberSaveable { mutableStateOf(false) }\n    var editTaskId by rememberSaveable { mutableStateOf<String?>(null) }',
    '    var showControl by rememberSaveable { mutableStateOf(false) }\n    var showReminders by rememberSaveable { mutableStateOf(false) }\n    var showQuickCapture by rememberSaveable { mutableStateOf(false) }\n    var editTaskId by rememberSaveable { mutableStateOf<String?>(null) }',
    'quick capture state',
)

s = replace_once(
    s,
    '                onNewProject = { showControl = false; openComposer() },\n                onRelease = { showControl = false; overlay = POverlay.RELEASE },',
    '                onNewProject = { showControl = false; openComposer() },\n                onQuickCapture = { showControl = false; showQuickCapture = true },\n                onRelease = { showControl = false; overlay = POverlay.RELEASE },',
    'control callback',
)

s = replace_once(
    s,
    '''    if (showComposer) {\n        val task = editTaskId?.let { id -> vm.tasks.firstOrNull { it.id == id } }''',
    '''    if (showQuickCapture) {\n        V14QuickCaptureDialog(\n            onDismiss = { showQuickCapture = false },\n            onSave = { idea ->\n                vm.saveIdea(idea)\n                showQuickCapture = false\n            },\n        )\n    }\n\n    if (showComposer) {\n        val task = editTaskId?.let { id -> vm.tasks.firstOrNull { it.id == id } }''',
    'quick capture dialog',
)

s = replace_once(
    s,
    '''    onNewProject: () -> Unit,\n    onRelease: () -> Unit,''',
    '''    onNewProject: () -> Unit,\n    onQuickCapture: () -> Unit,\n    onRelease: () -> Unit,''',
    'control signature',
)

s = replace_once(
    s,
    '''        Spacer(Modifier.height(12.dp))\n        PControlRow("Idea Vault", if (readyIdeas > 0) "$readyIdeas ideas ready to make" else "Capture what you might make later", Icons.Outlined.Lightbulb, onIdeas)''',
    '''        Spacer(Modifier.height(12.dp))\n        PControlRow("Quick Capture", "Save an idea to Inbox in seconds", Icons.Outlined.Bolt, onQuickCapture)\n        PControlRow("Idea Vault", if (readyIdeas > 0) "$readyIdeas ideas ready to make" else "Capture what you might make later", Icons.Outlined.Lightbulb, onIdeas)''',
    'control row',
)

old_next = '''@Composable\nprivate fun PNextMoveCard(task: CreatorTask) {\n    val current = CreatorWorkflowEngine.currentStage(task)\n    val next = CreatorWorkflowEngine.nextStage(task)\n    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {\n        Column(Modifier.padding(16.dp)) {\n            Text("NEXT MOVE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)\n            Spacer(Modifier.height(5.dp))\n            Text(current.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)\n            next?.let {\n                Spacer(Modifier.height(5.dp))\n                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)\n            }\n        }\n    }\n}\n'''
new_next = '''@Composable\nprivate fun PNextMoveCard(task: CreatorTask) {\n    val recommendation = CreatorPriorityEngine.recommendation(task)\n    val next = CreatorWorkflowEngine.nextStage(task)\n    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {\n        Column(Modifier.padding(16.dp)) {\n            Row(verticalAlignment = Alignment.CenterVertically) {\n                Text("NEXT MOVE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)\n                Spacer(Modifier.weight(1f))\n                Text(recommendation.urgencyLabel, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)\n            }\n            Spacer(Modifier.height(5.dp))\n            Text(recommendation.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)\n            Spacer(Modifier.height(5.dp))\n            Text(recommendation.reason, color = MutedText, fontSize = 9.3.sp, lineHeight = 13.sp)\n            next?.let {\n                Spacer(Modifier.height(5.dp))\n                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)\n            }\n        }\n    }\n}\n'''
s = replace_once(s, old_next, new_next, 'next move card')

old_queue = '''private fun pActiveQueue(tasks: List<CreatorTask>): List<CreatorTask> {\n    val now = System.currentTimeMillis()\n    return tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }.sortedWith(\n        compareBy<CreatorTask> { if (it.status == TaskStatus.WORKING) 0 else 1 }\n            .thenBy { if (it.dueAtMillis in 1 until now) 0 else 1 }\n            .thenBy { it.dueAtMillis.takeIf { d -> d > 0 } ?: Long.MAX_VALUE }\n    )\n}\n'''
new_queue = '''private fun pActiveQueue(tasks: List<CreatorTask>): List<CreatorTask> =\n    CreatorPriorityEngine.rankActive(tasks)\n'''
s = replace_once(s, old_queue, new_queue, 'active queue ranking')

APP.write_text(s)

c = COMPOSER.read_text()
old_composer = '''                    Spacer(Modifier.height(10.dp))\n                    PComposerLabel("PROJECT")\n                    OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Project title") }, singleLine = true, shape = RoundedCornerShape(16.dp))\n'''
new_composer = '''                    Spacer(Modifier.height(10.dp))\n                    if (task == null) {\n                        PComposerLabel("START FROM TEMPLATE")\n                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {\n                            CreatorProjectTemplates.presets.forEach { preset ->\n                                AssistChip(\n                                    onClick = {\n                                        platform = preset.platform\n                                        contentType = preset.contentType\n                                        priority = preset.priority\n                                        if (notes.isBlank()) notes = preset.suggestedNotes\n                                    },\n                                    label = { Text(preset.label, fontSize = 8.8.sp) },\n                                )\n                            }\n                        }\n                        Spacer(Modifier.height(18.dp))\n                    }\n                    PComposerLabel("PROJECT")\n                    OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Project title") }, singleLine = true, shape = RoundedCornerShape(16.dp))\n'''
c = replace_once(c, old_composer, new_composer, 'project templates')
COMPOSER.write_text(c)

print('v1.4 wiring applied')
