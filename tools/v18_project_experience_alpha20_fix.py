from pathlib import Path

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing anchor: {label}')
    if text.count(old) != 1:
        raise SystemExit(f'anchor not unique ({text.count(old)}): {label}')
    return text.replace(old, new, 1)

# Preserve the actual stage when repairing an Alpha19 false whole-project completion.
p = Path('app/src/main/java/com/framebynavin/app/data/ProjectPulse.kt')
s = p.read_text()
s = replace_once(
    s,
    '        val template = CreatorWorkflowEngine.templateFor(task)\n        val stageIndex = CreatorWorkflowEngine.stageIndex(task)\n        val restored = task.copy(\n',
    '''        val template = CreatorWorkflowEngine.templateFor(task)\n        val stageIndex = when {\n            task.workflowStageIndex >= 0 -> task.workflowStageIndex.coerceIn(0, template.stages.lastIndex)\n            task.checkpointStageId.isNotBlank() -> template.stages.indexOfFirst { it.id == task.checkpointStageId }.coerceAtLeast(0)\n            else -> 0\n        }\n        val restored = task.copy(\n''',
    'repair stage index',
)
p.write_text(s)

# Back exits Studio selection mode before navigating away.
p = Path('app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt')
s = p.read_text()
s = replace_once(
    s,
    '    val archiveEligible = selectionMode && selected.all { id -> projects.firstOrNull { it.id == id }?.status == TaskStatus.DONE }\n\n    LaunchedEffect(externalExpandNonce) {\n',
    '    val archiveEligible = selectionMode && selected.all { id -> projects.firstOrNull { it.id == id }?.status == TaskStatus.DONE }\n    BackHandler(enabled = selectionMode) { selected = emptySet() }\n\n    LaunchedEffect(externalExpandNonce) {\n',
    'studio selection BackHandler',
)
# Keep older UI tests/source call sites source-compatible.
s = replace_once(
    s,
    '    onArchiveSelected: (Set<String>) -> Unit,\n    onUnarchive: (String) -> Unit,\n    onDelete: (String) -> Unit,\n    onDeleteSelected: (Set<String>) -> Unit,\n    onEdit: (String) -> Unit,\n',
    '    onArchiveSelected: (Set<String>) -> Unit = {},\n    onUnarchive: (String) -> Unit,\n    onDelete: (String) -> Unit,\n    onDeleteSelected: (Set<String>) -> Unit = {},\n    onEdit: (String) -> Unit = {},\n',
    'studio callback defaults',
)
p.write_text(s)

p = Path('app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt')
s = p.read_text()
s = replace_once(
    s,
    '    onFocus: (String) -> Unit,\n    onOpenProject: (String) -> Unit,\n)',
    '    onFocus: (String) -> Unit,\n    onOpenProject: (String) -> Unit = {},\n)',
    'Today open project default',
)
p.write_text(s)

print('Alpha20 compatibility fix applied')
