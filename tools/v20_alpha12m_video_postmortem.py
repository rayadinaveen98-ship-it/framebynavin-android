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
    'versionCode = 101\n        versionName = "2.0.0-alpha1.2l.1-stage-timeline"',
    'versionCode = 102\n        versionName = "2.0.0-alpha1.2m-video-postmortem"',
    "version bump",
)

ui = "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt"
replace_once(
    ui,
    '''import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.layout.*''',
    '''import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.layout.*''',
    "scroll imports",
)

replace_once(
    ui,
    '''private fun V172VideoDetailDialog(\n    performance: YouTubeVideoPerformance,\n    linkedTask: CreatorTask?,\n    windowDays: Int,\n    onDismiss: () -> Unit,\n    onLink: () -> Unit,\n) {\n    val video = performance.video\n    AlertDialog(''',
    '''private fun V172VideoDetailDialog(\n    performance: YouTubeVideoPerformance,\n    linkedTask: CreatorTask?,\n    windowDays: Int,\n    onDismiss: () -> Unit,\n    onLink: () -> Unit,\n) {\n    val video = performance.video\n    AlertDialog(''',
    "video detail anchor",
)

replace_once(
    ui,
    '''        text = {\n            Column {\n                val baseline = if (performance.baselineMultiple > 0) {''',
    '''        text = {\n            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {\n                val baseline = if (performance.baselineMultiple > 0) {''',
    "scrollable postmortem dialog",
)

project_card = '''                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF1C1C1E)) {\n                    Column(Modifier.padding(12.dp)) {\n                        Text("CONNECTED PROJECT", color = MutedText, fontSize = 7.7.sp, fontWeight = FontWeight.Bold)\n                        Spacer(Modifier.height(3.dp))\n                        Text(linkedTask?.title ?: "Not connected yet", color = if (linkedTask != null) ProjectorIvory else RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)\n                    }\n                }'''
replace_once(
    ui,
    project_card,
    project_card + '''\n                linkedTask?.let { task ->\n                    Spacer(Modifier.height(12.dp))\n                    V20VideoPostmortemCard(task = task, video = video, windowDays = windowDays)\n                }''',
    "postmortem detail integration",
)

print("Materialized v102 Video Postmortem integration")
