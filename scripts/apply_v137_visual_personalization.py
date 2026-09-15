from pathlib import Path
import re

ROOT = Path('app/src/main')
APP = ROOT / 'java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'

text = APP.read_text(encoding='utf-8')

anchor = '''    val guidedTourStep = guidedTourStepName?.let { saved ->
        CreatorGuidedTourStep.entries.firstOrNull { it.name == saved }
    }
    val focusTaskState = remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }
'''
replacement = '''    val guidedTourStep = guidedTourStepName?.let { saved ->
        CreatorGuidedTourStep.entries.firstOrNull { it.name == saved }
    }
    val v137GuideLayer = rememberV137GuideLayer()
    val focusTaskState = remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }
'''
if 'val v137GuideLayer = rememberV137GuideLayer()' not in text:
    if anchor not in text:
        raise SystemExit('Could not find guided-tour layer anchor')
    text = text.replace(anchor, replacement, 1)

box_old = '    Box(Modifier.fillMaxSize().background(CinemaBlack)) {'
box_new = '    Box(Modifier.fillMaxSize().background(CinemaBlack).v137GuideCaptureAndBlur(v137GuideLayer, guidedTourStep != null)) {'
if box_new not in text:
    if box_old not in text:
        raise SystemExit('Could not find main Backlot root box')
    text = text.replace(box_old, box_new, 1)

coach_old = '''    ) {
        V20GuidedFirstRunCoach(
            step = guidedTourStep,
'''
coach_new = '''    ) {
        V137GuideSharpWindow(
            layer = v137GuideLayer,
            step = guidedTourStep,
            modifier = Modifier.fillMaxSize(),
        )
        V20GuidedFirstRunCoach(
            step = guidedTourStep,
'''
if 'layer = v137GuideLayer' not in text:
    if coach_old not in text:
        raise SystemExit('Could not find guided coach integration anchor')
    text = text.replace(coach_old, coach_new, 1)

APP.write_text(text, encoding='utf-8')

# User-visible legacy brand cleanup. Technical compatibility values are intentionally preserved.
protected = (
    'FrameByNavinCloudBackup',
    'FrameByNavinBackup',
    'Theme.FrameByNavin',
    'com.framebynavin.app',
)
string_re = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')

for path in ROOT.rglob('*'):
    if path.suffix.lower() not in {'.kt', '.java', '.xml'}:
        continue
    source = path.read_text(encoding='utf-8', errors='ignore')

    def rewrite(match: re.Match[str]) -> str:
        body = match.group(1)
        if any(token in body for token in protected):
            return match.group(0)
        cleaned = body
        cleaned = cleaned.replace('FRAMEBYNAVIN', 'BACKLOT')
        cleaned = cleaned.replace('FrameByNavin', 'Backlot')
        cleaned = cleaned.replace('Frame by Navin', 'Backlot')
        cleaned = cleaned.replace('Frame By Navin', 'Backlot')
        cleaned = cleaned.replace('CINE PULSE', 'BACKLOT GUIDE')
        cleaned = cleaned.replace('Cine Pulse', 'Backlot guide')
        return '"' + cleaned + '"'

    updated = string_re.sub(rewrite, source)
    if updated != source:
        path.write_text(updated, encoding='utf-8')

print('Applied v137 guide integration and visible Backlot brand cleanup.')
