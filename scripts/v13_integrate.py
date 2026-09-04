from pathlib import Path


def replace_once(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"anchor missing in {path}: {old[:100]!r}")
    text = text.replace(old, new, 1)
    p.write_text(text)

# MainActivity: schedule cloud work, which safely no-ops when signed out/off.
path = "app/src/main/java/com/framebynavin/app/MainActivity.kt"
replace_once(
    path,
    "import com.framebynavin.app.reminders.ReminderNotifications\n",
    "import com.framebynavin.app.cloud.CloudSyncScheduler\nimport com.framebynavin.app.reminders.ReminderNotifications\n",
)
replace_once(
    path,
    "        ReminderNotifications.ensureChannel(this)\n        externalLaunch = widgetLaunch(intent)\n",
    "        ReminderNotifications.ensureChannel(this)\n        CloudSyncScheduler.ensurePeriodic(this)\n        CloudSyncScheduler.enqueueNow(this)\n        externalLaunch = widgetLaunch(intent)\n",
)

# Settings: expose Sync & Backup as a first-class Creator OS setting.
path = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
replace_once(
    path,
    "import com.framebynavin.app.BuildConfig\n",
    "import com.framebynavin.app.BuildConfig\nimport com.framebynavin.app.cloud.CloudSyncActivity\n",
)
replace_once(
    path,
    "                onBattery = ::openBatterySettings,\n                onYouTube = { overlay = POverlay.NONE; tab = PTab.INSIGHTS },\n",
    "                onBattery = ::openBatterySettings,\n                onCloudSync = { context.startActivity(Intent(context, CloudSyncActivity::class.java)) },\n                onYouTube = { overlay = POverlay.NONE; tab = PTab.INSIGHTS },\n",
)
replace_once(
    path,
    "    onBattery: () -> Unit,\n    onYouTube: () -> Unit,\n",
    "    onBattery: () -> Unit,\n    onCloudSync: () -> Unit,\n    onYouTube: () -> Unit,\n",
)
anchor = '''            Spacer(Modifier.height(22.dp))
            PSettingsHeading("YOUTUBE", "Real channel performance, cached locally after each sync.")
'''
cloud_section = '''            Spacer(Modifier.height(22.dp))
            PSettingsHeading("SYNC & BACKUP", "Optional Google account protection. Local data remains primary.")
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onCloudSync,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).background(RecRed.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CloudSync, null, tint = RecRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cloud Sync", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Google account · restore points · local-first", color = MutedText, fontSize = 8.7.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            PSettingsHeading("YOUTUBE", "Real channel performance, cached locally after each sync.")
'''
replace_once(path, anchor, cloud_section)

# Theme compatibility: warning/destructive accents use existing locked tokens.
p = Path("app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt")
text = p.read_text().replace("WarningAmber", "MutedGold").replace("CriticalRed", "RecRed")
p.write_text(text)
