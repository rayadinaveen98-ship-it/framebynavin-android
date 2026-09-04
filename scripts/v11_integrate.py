from pathlib import Path

path = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
text = path.read_text()


def replace_once(old: str, new: str):
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected one match, got {count}: {old[:160]!r}")
    text = text.replace(old, new, 1)


replace_once(
    "                PTab.INSIGHTS -> PInsightsScreen(vm.tasks, vm.ideas, { openComposer() })",
    "                PTab.INSIGHTS -> V11InsightsScreen(vm.tasks, vm.ideas, { openComposer() })",
)

replace_once(
    "                onBattery = ::openBatterySettings,\n                onRunOnboarding = {",
    "                onBattery = ::openBatterySettings,\n                onYouTube = { overlay = POverlay.NONE; tab = PTab.INSIGHTS },\n                onRunOnboarding = {",
)

replace_once(
    "    onBattery: () -> Unit,\n    onRunOnboarding: () -> Unit,",
    "    onBattery: () -> Unit,\n    onYouTube: () -> Unit,\n    onRunOnboarding: () -> Unit,",
)

marker = '            PSettingsHeading("DATA & BACKUP", "Export or restore your local Creator OS data.")'
block = '''            PSettingsHeading("YOUTUBE", "Real channel performance, cached locally after each sync.")
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onYouTube,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(RecRed.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SmartDisplay, null, tint = RecRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YouTube Analytics", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Connect, sync and link published videos from Insights", color = MutedText, fontSize = 8.7.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
''' + marker
replace_once(marker, block)

path.write_text(text)
print("v1.1 Insights integration applied")
