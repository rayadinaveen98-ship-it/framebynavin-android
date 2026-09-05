from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_required(rel: str, old: str, new: str, count: int | None = 1) -> None:
    text = read(rel)
    hits = text.count(old)
    if hits == 0:
        raise SystemExit(f"Missing accessibility target in {rel}: {old!r}")
    if count is not None and hits != count:
        raise SystemExit(f"Expected {count} occurrence(s) in {rel}, found {hits}: {old!r}")
    write(rel, text.replace(old, new))


# Isolated alpha version. Locked v1.7.5 remains untouched.
replace_required("app/build.gradle.kts", "versionCode = 42", "versionCode = 43")
replace_required(
    "app/build.gradle.kts",
    'versionName = "1.8.0-foundation-alpha2"',
    'versionName = "1.8.0-foundation-alpha3"',
)

# ---------------------------------------------------------------------------
# Active app shell: essential navigation/action labels and touch targets.
# ---------------------------------------------------------------------------
root = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
replace_required(root, 'Text("CONTROL", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Black)', 'Text("CONTROL", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)')
replace_required(root, ') { Text(if (selected.status == TaskStatus.WORKING) "WORKING" else "START", color = ProjectorIvory, fontSize = 9.5.sp) }', ') { Text(if (selected.status == TaskStatus.WORKING) "WORKING" else "START", color = ProjectorIvory, fontSize = 10.sp) }')
replace_required(root, 'TextButton(onClick = { onStart(task.id) }) { Text(if (task.status == TaskStatus.WORKING) "CONTINUE" else "START", color = ProjectorIvory, fontSize = 8.5.sp) }', 'TextButton(onClick = { onStart(task.id) }) { Text(if (task.status == TaskStatus.WORKING) "CONTINUE" else "START", color = ProjectorIvory, fontSize = 10.sp) }')
replace_required(root, 'TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Bold) }', 'TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) }')
replace_required(root, 'AssistChip(onClick = { onPublishLate(task.id) }, label = { Text("+30 MIN", fontSize = 7.8.sp) })', 'AssistChip(onClick = { onPublishLate(task.id) }, label = { Text("+30 MIN", fontSize = 10.sp) })')
replace_required(root, 'AssistChip(onClick = { onReschedule(task.id) }, label = { Text("NEW TIME", fontSize = 7.8.sp) })', 'AssistChip(onClick = { onReschedule(task.id) }, label = { Text("NEW TIME", fontSize = 10.sp) })')
replace_required(root, 'AssistChip(onClick = { onSkip(task.id) }, label = { Text("SKIP", fontSize = 7.8.sp) })', 'AssistChip(onClick = { onSkip(task.id) }, label = { Text("SKIP", fontSize = 10.sp) })')
replace_required(root, 'Text("EDIT SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 9.5.sp)', 'Text("EDIT SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 10.sp)')
replace_required(root, 'FilterChip(settings.snoozeMinutes == value, { onSnooze(value) }, { Text("${value}m", fontSize = 9.sp) })', 'FilterChip(settings.snoozeMinutes == value, { onSnooze(value) }, { Text("${value}m", fontSize = 10.sp) })')
replace_required(root, 'FilterChip(settings.defaultAlarmTimeoutSeconds == value, { onAlarmTimeout(value) }, { Text(label, fontSize = 9.sp) })', 'FilterChip(settings.defaultAlarmTimeoutSeconds == value, { onAlarmTimeout(value) }, { Text(label, fontSize = 10.sp) })')
replace_required(root, 'Text("PREVIEW", color = RecRed, fontSize = 8.sp)', 'Text("PREVIEW", color = RecRed, fontSize = 10.sp)')
replace_required(root, 'OutlinedButton(onClick = onRunOnboarding, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, CinemaLine)) { Text("SHOW WELCOME AGAIN", color = ProjectorIvory, fontSize = 8.5.sp) }', 'OutlinedButton(onClick = onRunOnboarding, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, CinemaLine)) { Text("SHOW WELCOME AGAIN", color = ProjectorIvory, fontSize = 10.sp) }')
replace_required(root, 'fontSize = 7.8.sp,\n                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,', 'fontSize = 10.sp,\n                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,')
replace_required(root, 'Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(42.dp))', 'Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(48.dp))', count=2)
replace_required(root, 'Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(14.dp)) { Text(button, fontSize = 9.sp, fontWeight = FontWeight.Black) }', 'Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(14.dp)) { Text(button, fontSize = 10.sp, fontWeight = FontWeight.Black) }')

# Current Plan / Reminder Center / Studio actions.
polish = "app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt"
replace_required(polish, 'TextButton(onClick = { onStart(task.id) }) { Text(if (task.status == TaskStatus.WORKING) "CONTINUE" else "START", color = ProjectorIvory, fontSize = 8.5.sp) }', 'TextButton(onClick = { onStart(task.id) }) { Text(if (task.status == TaskStatus.WORKING) "CONTINUE" else "START", color = ProjectorIvory, fontSize = 10.sp) }')
replace_required(polish, 'TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen, fontSize = 8.5.sp) }', 'TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen, fontSize = 10.sp) }')
replace_required(polish, 'Text("ARCHIVE", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)', 'Text("ARCHIVE", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)')
replace_required(polish, 'Text("DELETE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)', 'Text("DELETE", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)')
replace_required(polish, 'TextButton(onClick = { onUnarchive(task.id) }) { Text("RESTORE", color = MutedGold, fontSize = 8.sp) }', 'TextButton(onClick = { onUnarchive(task.id) }) { Text("RESTORE", color = MutedGold, fontSize = 10.sp) }')
replace_required(polish, 'TextButton(onClick = onSelectAll) { Text("SELECT ALL", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold) }', 'TextButton(onClick = onSelectAll) { Text("SELECT ALL", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold) }')
replace_required(polish, 'Text("CREATE PROJECT", fontSize = 8.5.sp, fontWeight = FontWeight.Black)', 'Text("CREATE PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Black)')

# YouTube: icon-only overflow semantics + action/filter readability.
yt = "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt"
replace_required(yt, 'Text("CONNECT YOUTUBE", fontWeight = FontWeight.Black, fontSize = 9.5.sp)', 'Text("CONNECT YOUTUBE", fontWeight = FontWeight.Black, fontSize = 10.sp)')
replace_required(yt, 'else Text("REFRESH", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)', 'else Text("REFRESH", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)')
replace_required(yt, 'label = { Text("${days}D", fontSize = 8.5.sp) }', 'label = { Text("${days}D", fontSize = 10.sp) }')
replace_required(yt, 'IconButton(onClick = { menu = true }, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.MoreVert, null, tint = MutedText, modifier = Modifier.size(18.dp)) }', 'IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.MoreVert, "YouTube account options", tint = MutedText, modifier = Modifier.size(20.dp)) }')
replace_required(yt, 'Text(linked?.let { "Connected · ${it.title}" } ?: "Connect project", color = if (linked != null) MutedGold else RecRed, fontSize = 8.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)', 'Text(linked?.let { "Connected · ${it.title}" } ?: "Connect project", color = if (linked != null) MutedGold else RecRed, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)')

# Weekly Plan: controls are now at least 10sp.
week = "app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt"
for old, new in [
    ('Text("REFRESH 8 DAYS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)', 'Text("REFRESH 8 DAYS", fontSize = 10.sp, fontWeight = FontWeight.Bold)'),
    ('Text("RESET TO DEFAULT WEEK", color = MutedText, fontSize = 9.5.sp)', 'Text("RESET TO DEFAULT WEEK", color = MutedText, fontSize = 10.sp)'),
    ('Text(value.name.take(3), fontSize = 8.5.sp)', 'Text(value.name.take(3), fontSize = 10.sp)'),
    ('Text("CHANGE", color = RecRed, fontSize = 8.sp)', 'Text("CHANGE", color = RecRed, fontSize = 10.sp)'),
    ('Text(value, fontSize = 9.sp)', 'Text(value, fontSize = 10.sp)'),
    ('Text(value, fontSize = 8.5.sp)', 'Text(value, fontSize = 10.sp)'),
    ('Text("Every week", fontSize = 9.sp)', 'Text("Every week", fontSize = 10.sp)'),
    ('Text("Weeks 1 + 3", fontSize = 9.sp)', 'Text("Weeks 1 + 3", fontSize = 10.sp)'),
    ('Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 8.7.sp)', 'Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp)'),
    ('Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 8.5.sp)', 'Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp)'),
    ('Text("DELETE SLOT", color = Color(0xFFE87A73), fontSize = 9.sp)', 'Text("DELETE SLOT", color = Color(0xFFE87A73), fontSize = 10.sp)'),
]:
    replace_required(week, old, new)

# Idea Vault: filters/actions are essential interaction text, not decorative metadata.
ideas = "app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt"
for old, new in [
    ('label = { Text("All", fontSize = 8.5.sp) }', 'label = { Text("All", fontSize = 10.sp) }'),
    ('label = { Text(IdeaVaultLabels.status(status), fontSize = 8.5.sp) }', 'label = { Text(IdeaVaultLabels.status(status), fontSize = 10.sp) }'),
    ('label = { Text("All topics", fontSize = 8.3.sp) }', 'label = { Text("All topics", fontSize = 10.sp) }'),
    ('label = { Text(IdeaVaultLabels.category(category), fontSize = 8.3.sp) }', 'label = { Text(IdeaVaultLabels.category(category), fontSize = 10.sp) }'),
    ('Text("TURN INTO PROJECT", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)', 'Text("TURN INTO PROJECT", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)'),
    ('Text(IdeaVaultLabels.category(value), fontSize = 8.sp)', 'Text(IdeaVaultLabels.category(value), fontSize = 10.sp)'),
    ('Text(IdeaVaultLabels.status(value), fontSize = 8.sp)', 'Text(IdeaVaultLabels.status(value), fontSize = 10.sp)'),
    ('Text(value.name, fontSize = 8.5.sp)', 'Text(value.name, fontSize = 10.sp)'),
    ('Text(value, fontSize = 8.5.sp)', 'Text(value, fontSize = 10.sp)'),
    ('Text("DATE", fontSize = 9.sp)', 'Text("DATE", fontSize = 10.sp)'),
    ('Text("TIME", fontSize = 9.sp)', 'Text("TIME", fontSize = 10.sp)'),
]:
    replace_required(ideas, old, new, count=None)
replace_required(ideas, 'IconButton(onClick = onArchive, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.Archive, "Archive", tint = MutedText, modifier = Modifier.size(16.dp)) }', 'IconButton(onClick = onArchive, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Archive, "Archive idea", tint = MutedText, modifier = Modifier.size(20.dp)) }')

# Automation: primary action/toggle state labels.
auto = "app/src/main/java/com/framebynavin/app/ui/V17AutomationCenter.kt"
replace_required(auto, 'Text("UPDATE PLAN", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)', 'Text("UPDATE PLAN", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)')
replace_required(auto, 'Text(if (enabled) "ON" else "OFF", color = if (enabled) SuccessGreen else MutedText, fontSize = 8.sp, fontWeight = FontWeight.Black)', 'Text(if (enabled) "ON" else "OFF", color = if (enabled) SuccessGreen else MutedText, fontSize = 10.sp, fontWeight = FontWeight.Black)')

# Quick Capture and Today-reminder primary actions.
quick = "app/src/main/java/com/framebynavin/app/ui/V14QuickCaptureUi.kt"
replace_required(quick, 'Text("AUTO-SORT IN IDEA VAULT", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)', 'Text("AUTO-SORT IN IDEA VAULT", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)')
reminders = "app/src/main/java/com/framebynavin/app/ui/V102TodayReminders.kt"
replace_required(reminders, 'Text("VIEW ALL", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)', 'Text("VIEW ALL", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)')

# Backup and cloud primary action text + icon-only dismiss semantics/touch target.
backup = "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt"
replace_required(backup, 'Text(button, color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)', 'Text(button, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)')
cloud = "app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt"
replace_required(cloud, 'Text("CONTINUE WITH GOOGLE", fontSize = 9.5.sp, fontWeight = FontWeight.Black)', 'Text("CONTINUE WITH GOOGLE", fontSize = 10.sp, fontWeight = FontWeight.Black)')
replace_required(cloud, ') { Text(if (busy) "WORKING…" else "BACK UP NOW", fontSize = 8.5.sp, fontWeight = FontWeight.Black) }', ') { Text(if (busy) "WORKING…" else "BACK UP NOW", fontSize = 10.sp, fontWeight = FontWeight.Black) }')
replace_required(cloud, 'IconButton(onClick = { message = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Close, null, tint = MutedText, modifier = Modifier.size(14.dp)) }', 'IconButton(onClick = { message = null }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Close, "Dismiss message", tint = MutedText, modifier = Modifier.size(18.dp)) }')

print("v1.8 foundation alpha3 accessibility pass applied")
