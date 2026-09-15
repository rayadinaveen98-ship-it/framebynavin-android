from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def read(rel: str) -> str:
    return (ROOT / rel).read_text()

def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text)

def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Missing expected block: {label}")
    return text.replace(old, new, 1)

# Version ---------------------------------------------------------------------
build_path = "app/build.gradle.kts"
build = read(build_path)
build = replace_once(build, 'versionCode = 130', 'versionCode = 131', 'versionCode')
build = replace_once(
    build,
    'versionName = "2.0.0-rc6-cine-pulse-mascot-rebuild"',
    'versionName = "2.0.0-rc7-cine-pulse-motion-pass"',
    'versionName',
)
write(build_path, build)

# Single active mascot renderer + expanded pose vocabulary --------------------
guide_path = "app/src/main/java/com/framebynavin/app/ui/V127GuideCompanion.kt"
guide = read(guide_path)
guide = replace_once(
    guide,
    'enum class FrameGuidePose { IDLE, WALK, POINT, CELEBRATE }',
    'enum class FrameGuidePose { IDLE, WALK, POINT, PRESENT, LOOK, NOD, WAVE, THINK, LISTEN, SUCCESS, CELEBRATE, REST }',
    'FrameGuidePose enum',
)
old_map = '''    val state = when (pose) {
        FrameGuidePose.IDLE -> CinePulseState.IDLE
        FrameGuidePose.WALK -> CinePulseState.WALK
        FrameGuidePose.POINT -> CinePulseState.POINT
        FrameGuidePose.CELEBRATE -> CinePulseState.CELEBRATE
    }
    CinePulseMascotV130(state = state, modifier = modifier, pointRight = pointRight)
'''
new_map = '''    val state = when (pose) {
        FrameGuidePose.IDLE -> CinePulseState.IDLE
        FrameGuidePose.WALK -> CinePulseState.WALK
        FrameGuidePose.POINT -> CinePulseState.POINT
        FrameGuidePose.PRESENT -> CinePulseState.PRESENT
        FrameGuidePose.LOOK -> CinePulseState.LOOK
        FrameGuidePose.NOD -> CinePulseState.NOD
        FrameGuidePose.WAVE -> CinePulseState.WAVE
        FrameGuidePose.THINK -> CinePulseState.THINK
        FrameGuidePose.LISTEN -> CinePulseState.LISTEN
        FrameGuidePose.SUCCESS -> CinePulseState.SUCCESS
        FrameGuidePose.CELEBRATE -> CinePulseState.CELEBRATE
        FrameGuidePose.REST -> CinePulseState.REST
    }
    CinePulseGuide(state = state, modifier = modifier, pointRight = pointRight)
'''
guide = replace_once(guide, old_map, new_map, 'pose mapping')
guide = replace_once(
    guide,
    'val poses = listOf(FrameGuidePose.POINT, FrameGuidePose.WALK, FrameGuidePose.POINT, FrameGuidePose.IDLE, FrameGuidePose.CELEBRATE)',
    'val poses = listOf(FrameGuidePose.PRESENT, FrameGuidePose.LOOK, FrameGuidePose.POINT, FrameGuidePose.THINK, FrameGuidePose.SUCCESS)',
    'setup pose sequence',
)
write(guide_path, guide)

# Guided tour: six steps, six different silhouettes/behaviors -----------------
tour_path = "app/src/main/java/com/framebynavin/app/ui/V20GuidedFirstRun.kt"
tour = read(tour_path)
pattern = re.compile(
    r'private fun guidedCopy\(step: CreatorGuidedTourStep, hasProjects: Boolean\): GuidedCoachCopy = when \(step\) \{.*?\n\}',
    re.S,
)
replacement = '''private fun guidedCopy(step: CreatorGuidedTourStep, hasProjects: Boolean): GuidedCoachCopy = when (step) {
    CreatorGuidedTourStep.TODAY -> GuidedCoachCopy("TODAY", "Your command center", "Today shows the next thing worth your attention.", "SHOW IDEAS", icon = Icons.Outlined.Home, pose = FrameGuidePose.PRESENT)
    CreatorGuidedTourStep.IDEAS -> GuidedCoachCopy("IDEA VAULT", "Capture before it disappears", "Use + for a quick thought. Organize it later.", "CAPTURE IDEA", "NEXT", Icons.Outlined.Lightbulb, FrameGuidePose.POINT)
    CreatorGuidedTourStep.PROJECT -> GuidedCoachCopy("PROJECT", if (hasProjects) "Open a project" else "Build your first project", if (hasProjects) "Open one and I’ll follow you into the workspace." else "Create one now, or continue without one.", if (hasProjects) "OPEN PROJECT" else "CREATE PROJECT", if (hasProjects) null else "NOT NOW", Icons.Outlined.AddCircleOutline, FrameGuidePose.WALK)
    CreatorGuidedTourStep.WORKSPACE -> GuidedCoachCopy("WORKSPACE", "Move work stage by stage", "Your project tools and progress stay together here.", "SHOW INSIGHTS", icon = Icons.Outlined.MovieEdit, pose = FrameGuidePose.WAVE)
    CreatorGuidedTourStep.INSIGHTS -> GuidedCoachCopy("INSIGHTS", "Your creator brain", "Patterns and evidence help you decide what to improve next.", "SHOW CONTROL", icon = Icons.Outlined.Insights, pose = FrameGuidePose.THINK)
    CreatorGuidedTourStep.CONTROL -> GuidedCoachCopy("CONTROL", "Fast actions live here", "Create, capture and manage the system from one place.", "FINISH TOUR", icon = Icons.Outlined.GridView, pose = FrameGuidePose.CELEBRATE)
}'''
tour, count = pattern.subn(replacement, tour, count=1)
if count != 1:
    raise SystemExit(f"Expected one guidedCopy block, replaced {count}")
write(tour_path, tour)

# Retire the duplicate v130 renderer after proving nobody else calls it. -------
legacy_name = "CinePulseMascotV130"
legacy_path = ROOT / "app/src/main/java/com/framebynavin/app/ui/V130CinePulseMascot.kt"
remaining_refs = []
for path in (ROOT / "app/src/main/java").rglob("*.kt"):
    if path == legacy_path:
        continue
    if legacy_name in path.read_text():
        remaining_refs.append(str(path.relative_to(ROOT)))
if remaining_refs:
    raise SystemExit(f"Cannot retire duplicate renderer; references remain: {remaining_refs}")
if legacy_path.exists():
    legacy_path.unlink()

print("v131 Cine Pulse motion pass applied")
