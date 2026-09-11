from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def save(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    s = text.find(start)
    if s < 0:
        raise RuntimeError(f"{label}: start marker missing")
    e = text.find(end, s)
    if e < 0:
        raise RuntimeError(f"{label}: end marker missing")
    return text[:s] + replacement + text[e:]


# ---------------------------------------------------------------------------
# v116 identity
# ---------------------------------------------------------------------------
path = "app/build.gradle.kts"
text = load(path)
text = replace_once(text, "versionCode = 115", "versionCode = 116", "versionCode")
text = replace_once(
    text,
    'versionName = "2.0.0-alpha1.4e-brain-explainability-controls"',
    'versionName = "2.0.0-beta1-ux-stabilization"',
    "versionName",
)
save(path, text)


# ---------------------------------------------------------------------------
# 1) Finish-project navigation: finishing must stay in Create, not bounce
#    Insights -> Quick Capture -> Ideas.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V18CreatorJourney.kt"
text = load(path)
text = replace_once(
    text,
    "V18JourneyDestination.INSIGHTS else null",
    "V18JourneyDestination.CREATE else null",
    "finish route",
)
save(path, text)

path = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
text = load(path)
old_advance = '''    fun advanceWorkflowWithJourney(id: String) {
        val taskBeforeAdvance = vm.tasks.firstOrNull { it.id == id }
        val destination = taskBeforeAdvance?.let(V18CreatorJourney::afterWorkflowAdvance)
        val loopAction = taskBeforeAdvance?.let { task ->
            if (destination == V18JourneyDestination.INSIGHTS) {
                V18CreatorLoop.afterPublished(task, hasIdeas = vm.ideas.isNotEmpty())
            } else null
        }
        vm.advanceWorkflow(id)
        routeJourney(destination)
        when (loopAction) {
            V18CreatorLoopAction.CAPTURE_NEXT_IDEA -> showQuickCapture = true
            V18CreatorLoopAction.REVIEW_INSIGHTS,
            V18CreatorLoopAction.START_NEXT_PROJECT,
            null -> Unit
        }
    }
'''
new_advance = '''    fun advanceWorkflowWithJourney(id: String) {
        val taskBeforeAdvance = vm.tasks.firstOrNull { it.id == id }
        val destination = taskBeforeAdvance?.let(V18CreatorJourney::afterWorkflowAdvance)
        vm.advanceWorkflow(id)
        routeJourney(destination)
    }
'''
text = replace_once(text, old_advance, new_advance, "remove post-publish route bounce")

# 7) Center + is now New Project, not Quick Capture.
text = replace_once(
    text,
    "onCapture = { showQuickCapture = true },",
    "onCapture = { openComposer() },",
    "bottom nav center action",
)
save(path, text)


# ---------------------------------------------------------------------------
# 3 & 6) Today: no Opportunity Engine. Creator Focus is compact by default.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt"
text = load(path)
opp_block = '''            Spacer(Modifier.height(14.dp))
            V20OpportunityEngineCard(
                tasks = tasks,
                ideas = ideas,
                onOpenIdeaVault = onOpenIdeaVault,
                onOpenInsights = onOpenInsights,
                onOpenProject = onOpenProject,
            )
            Spacer(Modifier.height(18.dp))
'''
text = replace_once(
    text,
    opp_block,
    '''            Spacer(Modifier.height(18.dp))
''',
    "remove opportunity from Today",
)
focus_start = '''@Composable
private fun V18CreatorFocusCard('''
focus_end = '''@Composable
private fun V20WeeklyFocusDialog('''
new_focus = '''@Composable
private fun V18CreatorFocusCard(
    profile: CreatorProfile,
    personalization: CreatorPersonalizationSnapshot,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF171310),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .22f)),
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WEEKLY FOCUS", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(profile.primaryGoal, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${personalization.publishedThisWeek}/${personalization.weeklyTarget}",
                    color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Outlined.ChevronRight, "Open weekly focus", tint = MutedGold, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { personalization.weeklyProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                trackColor = CinemaLine,
            )
        }
    }
}

'''
text = replace_between(text, focus_start, focus_end, new_focus, "compact Creator Focus")
save(path, text)


# ---------------------------------------------------------------------------
# 8) Studio: compact cards in selection mode and completed cards compact by
#    default. Important information remains visible.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V18CoreScreens.kt"
text = load(path)
func_start = text.find("internal fun PStudioProject(")
func_end = text.find("@Composable\nprivate fun V21PostPublishSection(", func_start)
if func_start < 0 or func_end < 0:
    raise RuntimeError("PStudioProject function span missing")
head, func, tail = text[:func_start], text[func_start:func_end], text[func_end:]
func = replace_once(
    func,
    "    val done = task.status == TaskStatus.DONE\n",
    "    val done = task.status == TaskStatus.DONE\n    val compactCard = selectionMode || (done && !expanded)\n",
    "studio compact state",
)
func = replace_once(
    func,
    "            Column(Modifier.padding(15.dp)) {",
    "            Column(Modifier.padding(if (compactCard) 10.dp else 15.dp)) {",
    "studio compact padding",
)
func = replace_once(
    func,
    "fontSize = 15.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis",
    "fontSize = if (compactCard) 13.sp else 15.5.sp, fontWeight = FontWeight.Bold, maxLines = if (compactCard) 1 else 2, overflow = TextOverflow.Ellipsis",
    "studio title compact",
)
detail_start = '''                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(100.dp), color = pulseAccent.copy(alpha = .11f)) {'''
detail_end = '''                PStageRail(task)
'''
ds = func.find(detail_start)
de = func.find(detail_end, ds)
if ds < 0 or de < 0:
    raise RuntimeError("studio detail block missing")
de += len(detail_end)
original_details = func[ds:de]
compact_details = '''                if (compactCard) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (done) "COMPLETED" else pulse.stateLabel,
                            color = if (done) SuccessGreen else pulseAccent,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.weight(1f))
                        Text("$progress%", color = if (done) SuccessGreen else MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
''' + original_details.replace("\n", "\n    ") + '''                }
'''
func = func[:ds] + compact_details + func[de:]
text = head + func + tail
save(path, text)


# ---------------------------------------------------------------------------
# 2) Project Composer: keyboard-safe, essentials-first, creative setup hidden
#    under progressive disclosure instead of dumping every chip at once.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt"
text = load(path)
if "import androidx.compose.animation.AnimatedVisibility" not in text:
    text = replace_once(
        text,
        "import androidx.activity.result.contract.ActivityResultContracts\n",
        "import androidx.activity.result.contract.ActivityResultContracts\nimport androidx.compose.animation.AnimatedVisibility\n",
        "composer animation import",
    )
text = replace_once(
    text,
    "    var alarmTimeout by rememberSaveable(task?.id) { mutableIntStateOf(task?.alarmTimeoutSeconds ?: defaults.defaultAlarmTimeoutSeconds) }\n",
    "    var alarmTimeout by rememberSaveable(task?.id) { mutableIntStateOf(task?.alarmTimeoutSeconds ?: defaults.defaultAlarmTimeoutSeconds) }\n    var showCreativeSetup by rememberSaveable(task?.id) { mutableStateOf(false) }\n",
    "creative disclosure state",
)
text = replace_once(
    text,
    "Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {",
    "Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {",
    "composer ime padding",
)
creative_start = '''                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("CONTENT DNA",'''
creative_end = '''                    Spacer(Modifier.height(22.dp))
                    PComposerLabel("PUBLISH ON")'''
cs = text.find(creative_start)
ce = text.find(creative_end, cs)
if cs < 0 or ce < 0:
    raise RuntimeError("composer creative setup block missing")
creative_block = text[cs:ce]
creative_block = creative_block.replace('Text("CONTENT DNA",', 'Text("CREATIVE OPTIONS",', 1)
creative_block = creative_block.replace('Text("What this project is and how you are making it.", color = MutedText, fontSize = 8.5.sp)', 'Text("Optional details that help FrameByNavin learn what works for you.", color = MutedText, fontSize = 8.5.sp)', 1)
new_creative = '''                    Surface(
                        onClick = { showCreativeSetup = !showCreativeSetup },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("CREATIVE OPTIONS · OPTIONAL", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                                Text("Mode, content type and production style", color = MutedText, fontSize = 8.5.sp)
                            }
                            Icon(
                                if (showCreativeSetup) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                null,
                                tint = MutedGold,
                            )
                        }
                    }
                    AnimatedVisibility(visible = showCreativeSetup) {
                        Column {
                            Spacer(Modifier.height(8.dp))
''' + creative_block.replace("\n", "\n            ") + '''
                        }
                    }
'''
text = text[:cs] + new_creative + text[ce:]
text = text.replace('Text("TURN OFF PROJECT ATTENTION", color = MutedText, fontSize = 8.5.sp)', 'Text("TURN OFF REMINDERS", color = MutedText, fontSize = 8.5.sp)')
save(path, text)


# ---------------------------------------------------------------------------
# 4 & 5) Insights: user-facing signal language; no engineering vocabulary on
#    the primary surface. Reach state trusts an imported reach summary.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V20InsightsFoundationUi.kt"
text = load(path)
text = replace_once(text, 'Text("INSIGHTS FOUNDATION 2.0",', 'Text("CHANNEL SIGNALS",', "insights title")
text = replace_once(text, 'Text("Deeper signals are now being collected",', 'Text("What your audience is telling you",', "insights subtitle")
text = text.replace('Text("${foundation.readyDatasetCount()} READY",', 'Text("${foundation.readyDatasetCount()} ready",')
text = text.replace('"Engaged views: ${compactFoundation(foundation.periodEngagedViews)} · stored separately from public views"', '"Engaged views: ${compactFoundation(foundation.periodEngagedViews)}"')
text = text.replace('Text("METRIC SAFETY", color = RecRed, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)\n                Text(foundation.metricContract.note, color = MutedText, fontSize = 8.2.sp, lineHeight = 12.sp)', 'Text("YouTube changed how some views are counted. FrameByNavin handles older comparisons carefully.", color = MutedText, fontSize = 8.2.sp, lineHeight = 12.sp)')
text = text.replace('"Reach/CTR is intentionally not guessed. It will appear only from YouTube\'s official reach reports."', '"Reach will appear after YouTube finishes its first reach report."')
# If reach rows have already been imported, do not let an older NOT_CONFIGURED health flag hide them.
text = replace_once(
    text,
    "    val reach = reachHealth?.state\n",
    "    val reachState = reachHealth?.state\n",
    "reach state rename",
)
text = replace_once(
    text,
    "    val reachSummary = remember(snapshot.channel.channelId, snapshot.startDate, snapshot.endDate, snapshot.fetchedAtMillis) {\n        YouTubeReachStore(context).summary(snapshot.startDate, snapshot.endDate)\n    }\n",
    "    val reachSummary = remember(snapshot.channel.channelId, snapshot.startDate, snapshot.endDate, snapshot.fetchedAtMillis) {\n        YouTubeReachStore(context).summary(snapshot.startDate, snapshot.endDate)\n    }\n    val reach = if (reachSummary != null) YouTubeDatasetState.READY else reachState\n",
    "effective reach state",
)
save(path, text)


# ---------------------------------------------------------------------------
# 3 & 4) Opportunity Engine lives in Insights only, and technical reasoning is
#    collapsed behind 'Why this?'.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt"
text = load(path)
text = text.replace("/** Read-only Opportunity Engine surface inside Insights. Today owns the actionable version. */", "/** Opportunity guidance belongs in Insights; Today stays focused on current work. */")
text = replace_once(
    text,
    "    val primary = snapshot.now.firstOrNull() ?: snapshot.primary ?: return\n",
    "    val primary = snapshot.now.firstOrNull() ?: snapshot.primary ?: return\n    var showDetails by rememberSaveable(primary.id) { mutableStateOf(false) }\n",
    "opportunity disclosure state",
)
text = text.replace('Text("OPPORTUNITY ENGINE",', 'Text("NEXT MOVE",', 1)
text = text.replace('Text("NOW · NEXT · LATER",', 'Text("What to do next",', 1)
text = text.replace('Text("DECISION", color = MutedText, fontSize = 6.5.sp, fontWeight = FontWeight.Black)', 'Text("FIT", color = MutedText, fontSize = 6.5.sp, fontWeight = FontWeight.Black)', 1)
text = replace_once(
    text,
    'Text(primary.body, color = MutedText, fontSize = 9.4.sp, lineHeight = 14.sp)',
    'Text(primary.body, color = MutedText, fontSize = 9.4.sp, lineHeight = 14.sp, maxLines = if (showDetails) 8 else 3, overflow = TextOverflow.Ellipsis)',
    "opportunity compact body",
)
# Hide technical evidence/scorecard unless requested.
evidence_start = '''            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {'''
evidence_end = '''            V20DecisionScorecard(primary.scorecard)
'''
es = text.find(evidence_start)
ee = text.find(evidence_end, es)
if es < 0 or ee < 0:
    raise RuntimeError("opportunity evidence block missing")
ee += len(evidence_end)
evidence_block = text[es:ee]
wrapped_evidence = '''            TextButton(onClick = { showDetails = !showDetails }, modifier = Modifier.padding(top = 4.dp)) {
                Text(if (showDetails) "HIDE DETAILS" else "WHY THIS?", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            AnimatedVisibility(visible = showDetails) {
                Column {
''' + evidence_block.replace("\n", "\n    ") + '''                }
            }
'''
text = text[:es] + wrapped_evidence + text[ee:]
for condition in [
    "if (learningSummary.acted > 0)",
    "if (playbook.patterns.isNotEmpty())",
    "if (brainGuidance.isNotEmpty())",
    "if (brainExplainability.explanations.isNotEmpty())",
    "if (brain.patterns.isNotEmpty())",
]:
    text = text.replace(condition, condition.replace("if (", "if (showDetails && "), 1)
# Remove always-on bottom technical explainer by showing it only with details.
marker = '''            Spacer(Modifier.height(10.dp))
            Text(
                when {'''
text = replace_once(text, marker, '''            if (showDetails) {
                Spacer(Modifier.height(10.dp))
                Text(
                    when {''', "technical footer gate")
footer_end = '''                lineHeight = 10.sp,
            )
        }
    }
}

@Composable
private fun V20DecisionScorecard'''
text = replace_once(
    text,
    footer_end,
    '''                    lineHeight = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun V20DecisionScorecard''',
    "technical footer close",
)
save(path, text)


# ---------------------------------------------------------------------------
# 5) Reporting failures: never dump developer/API setup text at the creator.
# Normal Insights remains useful when optional reach reporting is unavailable.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/framebynavin/app/youtube/YouTubeReachReporting.kt"
text = load(path)
start = text.find("    private fun friendlyError(")
if start >= 0:
    brace = text.find("{", start)
    depth = 0
    end = -1
    for i in range(brace, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end < 0:
        raise RuntimeError("friendlyError closing brace missing")
    friendly = '''    private fun friendlyError(error: Throwable): String {
        val message = error.message.orEmpty().lowercase(Locale.US)
        return when {
            "disabled" in message || "has not been used" in message || "accessnotconfigured" in message ->
                "Reach data needs a one-time YouTube Reporting setup. Normal Insights still works."
            "permission" in message || "forbidden" in message ->
                "YouTube reach data is not available for this account yet. Normal Insights still works."
            else -> "Reach data could not be refreshed right now. Normal Insights still works."
        }
    }'''
    text = text[:start] + friendly + text[end:]
save(path, text)

print("v116 UX stabilization materialized")
