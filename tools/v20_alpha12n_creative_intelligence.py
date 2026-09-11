from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor in {path}, got {count}")
    p.write_text(text.replace(old, new, 1))


replace_once(
    "app/build.gradle.kts",
    'versionCode = 102\n        versionName = "2.0.0-alpha1.2m-video-postmortem"',
    'versionCode = 103\n        versionName = "2.0.0-alpha1.2n-creative-intelligence"',
    "version bump",
)

postmortem = "app/src/main/java/com/framebynavin/app/data/CreatorVideoPostmortem.kt"
replace_once(
    postmortem,
    '''                angle = workspace.angle,\n                hook = workspace.hook,\n                scriptPresent = workspace.script.isNotBlank() || workspace.scriptStudio?.isEmpty() == false,''',
    '''                angle = workspace.angle,\n                hook = workspace.scriptStudio?.selectedHook().orEmpty().ifBlank { workspace.hook },\n                scriptPresent = workspace.script.isNotBlank() || workspace.scriptStudio?.isEmpty() == false,''',
    "selected script studio hook",
)

ui = "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt"
replace_once(
    ui,
    '''    Spacer(Modifier.height(10.dp))\n    V20WorkflowIntelligenceCard(tasks)\n\n    Spacer(Modifier.height(18.dp))\n    val formats = YouTubeInsightEngine.formatPerformance(snapshot, tasks, links)''',
    '''    Spacer(Modifier.height(10.dp))\n    V20WorkflowIntelligenceCard(tasks)\n\n    Spacer(Modifier.height(10.dp))\n    V20CreativeIntelligenceCard(snapshot, tasks, links)\n\n    Spacer(Modifier.height(18.dp))\n    val formats = YouTubeInsightEngine.formatPerformance(snapshot, tasks, links)''',
    "creative intelligence card",
)

print("Materialized v103 Creative Intelligence foundation")
