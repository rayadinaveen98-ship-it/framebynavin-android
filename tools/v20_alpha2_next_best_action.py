from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found; refusing broad rewrite")
    path.write_text(text.replace(old, new, 1))


# Preserve the accepted cinematic Home. Upgrade only its existing NEXT MOVE card.
today = Path("app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt")
old_card = '''@Composable
private fun PNextMoveCard(task: CreatorTask) {
    val recommendation = CreatorPriorityEngine.recommendation(task)
    val next = CreatorWorkflowEngine.nextStage(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT MOVE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.weight(1f))
                Text(recommendation.urgencyLabel, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            }
            Spacer(Modifier.height(5.dp))
            Text(recommendation.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(recommendation.reason, color = MutedText, fontSize = 9.3.sp, lineHeight = 13.sp)
            next?.let {
                Spacer(Modifier.height(5.dp))
                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)
            }
        }
    }
}'''
new_card = '''@Composable
private fun PNextMoveCard(task: CreatorTask) {
    val recommendation = CreatorPriorityEngine.recommendation(task)
    val next = CreatorWorkflowEngine.nextStage(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT BEST ACTION", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .10f)) {
                    Text(
                        "ROUGH ${recommendation.estimatedMinutes} MIN",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MutedGold,
                        fontSize = 7.6.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .55.sp,
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recommendation.urgencyLabel, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Spacer(Modifier.width(7.dp))
                Text("·", color = MutedText, fontSize = 8.sp)
                Spacer(Modifier.width(7.dp))
                Text(CreatorWorkflowEngine.currentStage(task).label.uppercase(), color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(recommendation.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(recommendation.reason, color = MutedText, fontSize = 9.3.sp, lineHeight = 13.sp)
            if (recommendation.signals.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Text("WHY NOW", color = ProjectorIvory.copy(alpha = .72f), fontSize = 7.7.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Spacer(Modifier.height(3.dp))
                Text(recommendation.signals.joinToString(" · "), color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
            }
            next?.let {
                Spacer(Modifier.height(8.dp))
                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)
            }
        }
    }
}'''
replace_once(today, old_card, new_card, "Next best action card")

# New installable milestone; never silently replace the Drive-vault build.
gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 88' not in build or 'versionName = "2.0.0-alpha1.2b-drive-vault"' not in build:
    raise SystemExit("Unexpected Drive-vault build identity")
build = build.replace('versionCode = 88', 'versionCode = 89', 1)
build = build.replace('versionName = "2.0.0-alpha1.2b-drive-vault"', 'versionName = "2.0.0-alpha2-next-best-action"', 1)
gradle.write_text(build)
