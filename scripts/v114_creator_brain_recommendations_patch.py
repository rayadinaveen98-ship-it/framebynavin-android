from pathlib import Path

path = Path("app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global text
    if old not in text:
        raise SystemExit(f"v114 patch anchor not found:\n{old[:180]}")
    text = text.replace(old, new, 1)


replace_once(
    "    val brain = remember(outcomes, tasks) { CreatorBrainEngine.build(outcomes, tasks) }\n"
    "    val snapshot = remember(tasks, ideas, analytics, playbook, brain) {",
    "    val brain = remember(outcomes, tasks) { CreatorBrainEngine.build(outcomes, tasks) }\n"
    "    val brainGuidance = remember(brain, tasks) { CreatorBrainRecommendationEngine.build(brain, tasks) }\n"
    "    val snapshot = remember(tasks, ideas, analytics, playbook, brain) {",
)

replace_once(
    "    V20OpportunitySurface(snapshot, learningSummary, playbook, brain) { opportunity ->",
    "    V20OpportunitySurface(snapshot, learningSummary, playbook, brain, brainGuidance, onOpenProject) { opportunity ->",
)

replace_once(
    "    val brain = remember(outcomeState, local.tasks) { CreatorBrainEngine.build(outcomeState, local.tasks) }\n"
    "    val snapshot = remember(local, analytics, playbook, brain) {",
    "    val brain = remember(outcomeState, local.tasks) { CreatorBrainEngine.build(outcomeState, local.tasks) }\n"
    "    val brainGuidance = remember(brain, local.tasks) { CreatorBrainRecommendationEngine.build(brain, local.tasks) }\n"
    "    val snapshot = remember(local, analytics, playbook, brain) {",
)

replace_once(
    "    V20OpportunitySurface(snapshot, learningSummary, playbook, brain, onAction = null)",
    "    V20OpportunitySurface(snapshot, learningSummary, playbook, brain, brainGuidance, onBrainProject = null, onAction = null)",
)

replace_once(
    "    brain: CreatorBrainSnapshot,\n"
    "    onAction: ((CreatorOpportunity) -> Unit)?,",
    "    brain: CreatorBrainSnapshot,\n"
    "    brainGuidance: List<CreatorBrainRecommendation>,\n"
    "    onBrainProject: ((String) -> Unit)?,\n"
    "    onAction: ((CreatorOpportunity) -> Unit)?,",
)

replace_once(
    "            if (brain.patterns.isNotEmpty()) {\n"
    "                Spacer(Modifier.height(12.dp))\n"
    "                Text(\"CREATOR BRAIN · EARLY MEMORY\"",
    "            if (brainGuidance.isNotEmpty()) {\n"
    "                Spacer(Modifier.height(14.dp))\n"
    "                HorizontalDivider(color = MutedGold.copy(alpha = .14f))\n"
    "                Spacer(Modifier.height(10.dp))\n"
    "                Text(\"CREATOR BRAIN · GUIDANCE\", color = MutedGold, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)\n"
    "                Spacer(Modifier.height(5.dp))\n"
    "                brainGuidance.take(3).forEach { recommendation ->\n"
    "                    V20CreatorBrainRecommendationRow(recommendation, onBrainProject)\n"
    "                }\n"
    "                Spacer(Modifier.height(3.dp))\n"
    "                Text(\n"
    "                    \"Guidance comes only from repeated evaluated creator outcomes. It can suggest a tailwind, a controlled retest, a warning or a rework — never a guaranteed result.\",\n"
    "                    color = MutedText.copy(alpha = .78f),\n"
    "                    fontSize = 6.8.sp,\n"
    "                    lineHeight = 10.sp,\n"
    "                )\n"
    "            }\n\n"
    "            if (brain.patterns.isNotEmpty()) {\n"
    "                Spacer(Modifier.height(12.dp))\n"
    "                Text(\"CREATOR BRAIN · MEMORY\"",
)

replace_once(
    "                    \"Creator Brain learns Content DNA, content type, platform, hook style and workflow patterns only from evaluated outcomes. Three results can become Emerging; four are required for Proven or Caution.\",",
    "                    \"Memory is time-aware: fresh patterns can guide, aging patterns are discounted, and stale patterns stay visible without changing ranking until new evidence refreshes them.\",",
)

anchor = "@Composable\nprivate fun V20CreatorBrainPatternRow(pattern: CreatorBrainPattern) {"
recommendation_row = '''@Composable
private fun V20CreatorBrainRecommendationRow(
    recommendation: CreatorBrainRecommendation,
    onOpenProject: ((String) -> Unit)?,
) {
    val accent = when (recommendation.kind) {
        CreatorBrainRecommendationKind.LEAN_IN -> SuccessGreen
        CreatorBrainRecommendationKind.TEST_MORE -> MutedGold
        CreatorBrainRecommendationKind.WATCH -> MutedGold
        CreatorBrainRecommendationKind.AVOID_FOR_NOW -> RecRed
        CreatorBrainRecommendationKind.REFRESH_EVIDENCE -> MutedText
    }
    val label = when (recommendation.kind) {
        CreatorBrainRecommendationKind.LEAN_IN -> "LEAN IN"
        CreatorBrainRecommendationKind.TEST_MORE -> "TEST"
        CreatorBrainRecommendationKind.WATCH -> "WATCH"
        CreatorBrainRecommendationKind.AVOID_FOR_NOW -> "REWORK"
        CreatorBrainRecommendationKind.REFRESH_EVIDENCE -> "REFRESH"
    }
    val clickable = recommendation.taskId.isNotBlank() && onOpenProject != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = clickable) { onOpenProject?.invoke(recommendation.taskId) }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .12f)) {
            Text(label, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = accent, fontSize = 5.8.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(recommendation.title, color = ProjectorIvory.copy(alpha = .9f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(recommendation.body, color = MutedText, fontSize = 6.6.sp, lineHeight = 9.5.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("${recommendation.confidence}% evidence confidence · ${recommendation.patternIds.size} Brain pattern${if (recommendation.patternIds.size == 1) "" else "s"}", color = accent.copy(alpha = .86f), fontSize = 6.sp, fontWeight = FontWeight.Bold)
        }
        if (clickable) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.ArrowForward, null, tint = accent, modifier = Modifier.size(14.dp))
        }
    }
}

'''
replace_once(anchor, recommendation_row + anchor)

path.write_text(text, encoding="utf-8")
