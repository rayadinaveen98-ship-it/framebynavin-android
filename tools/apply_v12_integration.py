from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    return text.replace(old, new, 1)


# 1) Keep home-screen widgets fresh whenever task persistence changes.
task_path = Path("app/src/main/java/com/framebynavin/app/data/TaskStore.kt")
task = task_path.read_text()
if "CreatorWidgetUpdater" not in task:
    task = replace_once(
        task,
        "import android.content.Context\n",
        "import android.content.Context\nimport com.framebynavin.app.widget.CreatorWidgetUpdater\n",
        "TaskStore import",
    )
    task = replace_once(
        task,
        "    suspend fun save(tasks: List<CreatorTask>) {\n        context.creatorDataStore.edit { prefs -> prefs[tasksKey] = encode(tasks) }\n    }",
        "    suspend fun save(tasks: List<CreatorTask>) {\n        context.creatorDataStore.edit { prefs -> prefs[tasksKey] = encode(tasks) }\n        CreatorWidgetUpdater.updateAll(context, tasks)\n    }",
        "TaskStore save",
    )
    task_path.write_text(task)


# 2) Capture first-at-or-after 24h / 7d / 28d local checkpoints after successful YouTube sync.
yt_path = Path("app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt")
yt = yt_path.read_text()
if "val milestoneStore" not in yt:
    yt = replace_once(
        yt,
        "    val store = remember { YouTubeAnalyticsStore(context.applicationContext) }\n    val api = remember { YouTubeApiClient() }",
        "    val store = remember { YouTubeAnalyticsStore(context.applicationContext) }\n    val milestoneStore = remember { YouTubeMilestoneStore(context.applicationContext) }\n    val api = remember { YouTubeApiClient() }",
        "YouTube milestone store",
    )
    yt = replace_once(
        yt,
        "            }.onSuccess { fresh ->\n                store.save(fresh)\n                snapshot = fresh",
        "            }.onSuccess { fresh ->\n                store.save(fresh)\n                milestoneStore.captureFrom(fresh, store.links())\n                snapshot = fresh",
        "YouTube milestone capture",
    )
    yt_path.write_text(yt)


# 3) Deep-link widget actions into the existing Creator OS shell without changing its normal navigation.
ui_path = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
ui = ui_path.read_text()
if "CreatorWidgetLaunch" not in ui:
    ui = replace_once(
        ui,
        "import com.framebynavin.app.ui.theme.*\n",
        "import com.framebynavin.app.ui.theme.*\nimport com.framebynavin.app.widget.CreatorWidgetContract\nimport com.framebynavin.app.widget.CreatorWidgetLaunch\n",
        "Widget imports",
    )
    ui = replace_once(
        ui,
        "fun FrameByNavinV101BApp(vm: CreatorViewModel = viewModel()) {",
        "fun FrameByNavinV101BApp(vm: CreatorViewModel = viewModel(), externalLaunch: CreatorWidgetLaunch? = null) {",
        "App signature",
    )
    ui = replace_once(
        ui,
        "    var focusTaskId by rememberSaveable { mutableStateOf<String?>(null) }\n",
        "    var focusTaskId by rememberSaveable { mutableStateOf<String?>(null) }\n    var externalStudioId by rememberSaveable { mutableStateOf<String?>(null) }\n    var externalStudioNonce by rememberSaveable { mutableLongStateOf(0L) }\n",
        "External studio state",
    )
    ui = replace_once(
        ui,
        "    val focusTask = vm.tasks.firstOrNull { it.id == focusTaskId }\n",
        "    LaunchedEffect(externalLaunch?.nonce) {\n        val launch = externalLaunch ?: return@LaunchedEffect\n        showControl = false\n        showReminders = false\n        when (launch.action) {\n            CreatorWidgetContract.ACTION_OPEN_TODAY -> { overlay = POverlay.NONE; tab = PTab.TODAY }\n            CreatorWidgetContract.ACTION_OPEN_STUDIO -> {\n                overlay = POverlay.NONE\n                tab = PTab.STUDIO\n                externalStudioId = launch.taskId.ifBlank { null }\n                externalStudioNonce = launch.nonce\n            }\n            CreatorWidgetContract.ACTION_NEW_PROJECT -> { overlay = POverlay.NONE; openComposer() }\n            CreatorWidgetContract.ACTION_RELEASE_DAY -> overlay = POverlay.RELEASE\n        }\n    }\n\n    val focusTask = vm.tasks.firstOrNull { it.id == focusTaskId }\n",
        "External launch effect",
    )
    ui = replace_once(
        ui,
        "                PTab.STUDIO -> PStudioScreen(vm.tasks, { openComposer() }, vm::advanceWorkflow, vm::moveWorkflowBack) { focusTaskId = it }",
        "                PTab.STUDIO -> PStudioScreen(\n                    tasks = vm.tasks,\n                    onAdd = { openComposer() },\n                    onAdvance = vm::advanceWorkflow,\n                    onBack = vm::moveWorkflowBack,\n                    onFocus = { focusTaskId = it },\n                    externalExpandId = externalStudioId,\n                    externalExpandNonce = externalStudioNonce,\n                )",
        "Studio call",
    )
    ui = replace_once(
        ui,
        "private fun PStudioScreen(\n    tasks: List<CreatorTask>,\n    onAdd: () -> Unit,\n    onAdvance: (String) -> Unit,\n    onBack: (String) -> Unit,\n    onFocus: (String) -> Unit,\n) {",
        "private fun PStudioScreen(\n    tasks: List<CreatorTask>,\n    onAdd: () -> Unit,\n    onAdvance: (String) -> Unit,\n    onBack: (String) -> Unit,\n    onFocus: (String) -> Unit,\n    externalExpandId: String? = null,\n    externalExpandNonce: Long = 0L,\n) {",
        "Studio signature",
    )
    ui = replace_once(
        ui,
        "    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }\n    val haptics = LocalHapticFeedback.current\n\n    LaunchedEffect(projects.map { it.id })",
        "    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }\n    val haptics = LocalHapticFeedback.current\n\n    LaunchedEffect(externalExpandNonce) {\n        if (!externalExpandId.isNullOrBlank()) expandedId = externalExpandId\n    }\n    LaunchedEffect(projects.map { it.id })",
        "Studio external expand",
    )
    ui_path.write_text(ui)

print("v1.2 integration applied")
