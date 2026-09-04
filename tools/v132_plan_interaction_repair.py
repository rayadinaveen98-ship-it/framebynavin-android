from pathlib import Path

ui_path = Path("app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt")
app_path = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
ui = ui_path.read_text()
app = app_path.read_text()

# Plan screen: normal tap edits/opens the project; long-press enters selection.
ui = ui.replace(
    "    onAdd: () -> Unit,\n    onStart: (String) -> Unit,\n",
    "    onAdd: () -> Unit,\n    onEdit: (String) -> Unit,\n    onStart: (String) -> Unit,\n",
    1,
)
ui = ui.replace(
    'Text("Tap a project to select it. Long-press also works.", color = MutedText, fontSize = 9.4.sp)',
    'Text("Tap a project to edit it. Long-press to select one or more.", color = MutedText, fontSize = 9.4.sp)',
)
# Four V131PlanSection calls: insert onEdit before onStart/onDone.
ui = ui.replace("            }, onStart, onDone)", "            }, onEdit, onStart, onDone)")

ui = ui.replace(
    "    onHold: (String) -> Unit,\n    onStart: (String) -> Unit,\n",
    "    onHold: (String) -> Unit,\n    onEdit: (String) -> Unit,\n    onStart: (String) -> Unit,\n",
    1,
)
ui = ui.replace(
    "                onClick = { onToggleSelection(task.id) },\n                onLongClick = { onHold(task.id) },",
    "                onClick = { if (selectionMode) onToggleSelection(task.id) else onEdit(task.id) },\n                onLongClick = { onHold(task.id) },",
    1,
)

# Wire Plan tap to the existing project composer in the app shell.
needle = """                PTab.PLAN -> V131PlanScreen(\n                    tasks = vm.tasks,\n                    onAdd = { openComposer() },\n                    onStart = vm::startTask,\n"""
replacement = """                PTab.PLAN -> V131PlanScreen(\n                    tasks = vm.tasks,\n                    onAdd = { openComposer() },\n                    onEdit = { openComposer(it) },\n                    onStart = vm::startTask,\n"""
if needle not in app:
    raise SystemExit("Plan app wiring block not found")
app = app.replace(needle, replacement, 1)

# Sanity checks so the workflow fails rather than silently committing a partial repair.
required_ui = [
    "onEdit: (String) -> Unit",
    "if (selectionMode) onToggleSelection(task.id) else onEdit(task.id)",
    "Tap a project to edit it. Long-press to select one or more.",
]
for value in required_ui:
    if value not in ui:
        raise SystemExit(f"missing repaired UI marker: {value}")
if "onEdit = { openComposer(it) }" not in app:
    raise SystemExit("missing Plan composer wiring")

ui_path.write_text(ui)
app_path.write_text(app)
