from pathlib import Path

p = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
s = p.read_text()

replacements = [
    (
        '    onCalendar: () -> Unit,\n    onRelease: () -> Unit,',
        '    onCalendar: () -> Unit,\n    onCopilot: () -> Unit,\n    onRelease: () -> Unit,',
    ),
    (
        '        PControlRow("Content Calendar", "Projects + weekly plan for 14 days", Icons.Outlined.CalendarMonth, onCalendar)\n        PControlRow("Idea Vault",',
        '        PControlRow("Content Calendar", "Projects + weekly plan for 14 days", Icons.Outlined.CalendarMonth, onCalendar)\n        PControlRow("Creator Copilot", "Ideas, scripts, hooks and packaging drafts", Icons.Outlined.AutoAwesome, onCopilot)\n        PControlRow("Idea Vault",',
    ),
    (
        '                onCalendar = { showControl = false; overlay = POverlay.CALENDAR },\n                onRelease =',
        '                onCalendar = { showControl = false; overlay = POverlay.CALENDAR },\n                onCopilot = { showControl = false; context.startActivity(Intent(context, CopilotActivity::class.java)) },\n                onRelease =',
    ),
]

for old, new in replacements:
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'Expected one match, found {count}: {old[:90]}')
    s = s.replace(old, new, 1)

p.write_text(s)
print('v1.8 Copilot wiring applied')
