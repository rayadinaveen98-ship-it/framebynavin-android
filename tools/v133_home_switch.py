from pathlib import Path

p = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
s = p.read_text()

if 'PTab.TODAY -> V133CreatorHome(' in s:
    raise SystemExit(0)

marker = 'PTab.TODAY -> PTodayScreen('
start = s.find(marker)
if start < 0:
    raise SystemExit('Today screen marker not found; refusing unsafe patch')
line_start = s.rfind('\n', 0, start) + 1
end_marker = '                PTab.PLAN ->'
end = s.find(end_marker, start)
if end < 0:
    raise SystemExit('Plan marker not found; refusing unsafe patch')

new = '''                PTab.TODAY -> V133CreatorHome(
                    tasks = vm.tasks,
                    onNewProject = { openComposer() },
                    onIdeas = { overlay = POverlay.IDEAS },
                    onReminders = { showReminders = true },
                    onStudio = { tab = PTab.STUDIO },
                    onStart = vm::startTask,
                    onAdvance = vm::advanceWorkflow,
                    onFocus = { focusTaskId = it },
                )
'''

p.write_text(s[:line_start] + new + s[end:])
