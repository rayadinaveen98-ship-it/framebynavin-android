from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
ROOT_UI = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
TODAY = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt'
INSIGHTS = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt'
PERSONALIZATION = ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorPersonalization.kt'
UNIT_TEST = ROOT / 'app/src/test/java/com/framebynavin/app/data/CreatorPersonalizationAlpha11Test.kt'
UI_TEST = ROOT / 'app/src/androidTest/java/com/framebynavin/app/ui/V18CoreInteractionUiTest.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


def replace_exact_count(text: str, old: str, new: str, expected: int, label: str) -> str:
    count = text.count(old)
    if count != expected:
        raise SystemExit(f'{label}: expected {expected} matches, found {count}')
    return text.replace(old, new)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 50', 'versionCode = 51', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha10"',
    'versionName = "1.8.0-product-alpha11"',
    'versionName',
)
BUILD.write_text(build)

PERSONALIZATION.parent.mkdir(parents=True, exist_ok=True)
PERSONALIZATION.write_text(r'''package com.framebynavin.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class CreatorPersonalizationSnapshot(
    val category: String,
    val primaryPlatform: String,
    val platformSummary: String,
    val goal: String,
    val weeklyTarget: Int,
    val publishedThisWeek: Int,
    val weeklyProgress: Float,
    val focusTitle: String,
    val focusBody: String,
    val insightTitle: String,
    val insightBody: String,
    val emptyProjectBody: String,
)

/**
 * Turns the creator-selected onboarding profile into small, deterministic product guidance.
 * This remains local and rule-based in v1.8; it is not an AI recommendation system.
 */
object CreatorPersonalizationEngine {
    private val platformPriority = listOf(
        "YouTube",
        "Instagram",
        "X",
        "Facebook",
        "LinkedIn",
        "Podcast",
        "Blog / Newsletter",
        "Other",
    )

    fun snapshot(
        profile: CreatorProfile,
        tasks: List<CreatorTask>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CreatorPersonalizationSnapshot {
        val normalized = profile.normalized()
        val category = normalized.category.ifBlank { "Creator" }
        val platforms = orderedPlatforms(normalized.platforms)
        val primaryPlatform = platforms.firstOrNull() ?: "your main platform"
        val platformSummary = platforms.take(3).joinToString(" · ").ifBlank { "Your platforms" }
        val goal = normalized.primaryGoal.ifBlank { "Publish consistently" }
        val target = normalized.weeklyPublishingTarget.coerceIn(1, 14)
        val weekStart = weekStartMillis(nowMillis, zoneId)
        val published = tasks.count { task ->
            task.status == TaskStatus.DONE &&
                task.completedAtMillis >= weekStart &&
                task.completedAtMillis in 1L..nowMillis
        }
        val progress = (published.toFloat() / target.toFloat()).coerceIn(0f, 1f)

        val focus = focusCopy(goal, primaryPlatform, category, published, target)
        val insight = insightCopy(goal, primaryPlatform, published, target)
        val categoryForSentence = category.replace("&", "and").lowercase()
        val emptyBody = "Start a $categoryForSentence project for $primaryPlatform, or capture an idea first."

        return CreatorPersonalizationSnapshot(
            category = category,
            primaryPlatform = primaryPlatform,
            platformSummary = platformSummary,
            goal = goal,
            weeklyTarget = target,
            publishedThisWeek = published,
            weeklyProgress = progress,
            focusTitle = focus.first,
            focusBody = focus.second,
            insightTitle = insight.first,
            insightBody = insight.second,
            emptyProjectBody = emptyBody,
        )
    }

    private fun orderedPlatforms(platforms: Set<String>): List<String> {
        val clean = platforms.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        return clean.sortedWith(
            compareBy<String> { value ->
                platformPriority.indexOfFirst { it.equals(value, ignoreCase = true) }.let { if (it < 0) Int.MAX_VALUE else it }
            }.thenBy { it.lowercase() },
        )
    }

    private fun weekStartMillis(nowMillis: Long, zoneId: ZoneId): Long {
        val date = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun focusCopy(
        goal: String,
        platform: String,
        category: String,
        published: Int,
        target: Int,
    ): Pair<String, String> = when (goal.lowercase()) {
        "publish consistently" -> "Protect your publishing rhythm." to
            "$published / $target published this week. Move the next $platform project toward done."
        "grow an audience" -> "Create for reach, then learn." to
            "On $platform, lead with a clear hook and one idea your ${category.replace("&", "and").lowercase()} audience will want to share."
        "improve content quality" -> "Raise one craft bar today." to
            "Use the next $platform project to improve one thing on purpose: story, clarity, sound, visuals or delivery."
        "build a creator business" -> "Create toward an outcome." to
            "Make the next $platform project serve the audience and the business instead of publishing just to stay busy."
        "launch a project" -> "Protect the launch path." to
            "Keep the next launch-critical step visible and move it forward before opening another project."
        "stay organized" -> "Keep the pipeline clear." to
            "Capture loose ideas, finish the next active step and keep $platform work out of your head."
        else -> "Keep your goal visible." to
            "$goal is the priority. Use $platform as the clearest lane for the next meaningful move."
    }

    private fun insightCopy(
        goal: String,
        platform: String,
        published: Int,
        target: Int,
    ): Pair<String, String> = when (goal.lowercase()) {
        "publish consistently" -> "Are you keeping your publishing promise?" to
            "$published of $target planned publishes are complete this week. Use the numbers below to protect the rhythm."
        "grow an audience" -> "Is your work creating audience momentum?" to
            "Read $platform performance alongside your $published / $target weekly publishing rhythm, then repeat what earns attention."
        "improve content quality" -> "Is better work becoming repeatable?" to
            "Use performance as feedback, not a score. Compare what you shipped with the craft choices you changed."
        "build a creator business" -> "Is your content supporting the bigger goal?" to
            "Track output and $platform response together so effort stays connected to useful creator outcomes."
        "launch a project" -> "Is the launch moving forward?" to
            "Use creator metrics as context while keeping launch progress and weekly output in view."
        "stay organized" -> "Is the system reducing creator friction?" to
            "Watch active work, finished work and $platform results together so the pipeline stays understandable."
        else -> "Is your creator system serving the goal?" to
            "$goal stays the lens. Compare output, active work and $platform response before deciding what to make next."
    }
}
''')

UNIT_TEST.parent.mkdir(parents=True, exist_ok=True)
UNIT_TEST.write_text(r'''package com.framebynavin.app.data

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPersonalizationAlpha11Test {
    private val utc = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 9, 3, 12, 0, 0, 0, utc).toInstant().toEpochMilli()

    @Test
    fun weeklyProgressCountsOnlyCurrentWeekObservedCompletions() {
        val thisWeek = ZonedDateTime.of(2026, 9, 2, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val lastWeek = ZonedDateTime.of(2026, 8, 30, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val tasks = listOf(
            task("current", TaskStatus.DONE, thisWeek),
            task("old", TaskStatus.DONE, lastWeek),
            task("legacy", TaskStatus.DONE, 0L),
            task("active", TaskStatus.WORKING, thisWeek),
        )

        val result = CreatorPersonalizationEngine.snapshot(profile(target = 3), tasks, now, utc)

        assertEquals(1, result.publishedThisWeek)
        assertEquals(3, result.weeklyTarget)
        assertEquals(1f / 3f, result.weeklyProgress, 0.0001f)
    }

    @Test
    fun weeklyProgressCapsAtCompleteWhenCreatorBeatsTarget() {
        val thisWeek = ZonedDateTime.of(2026, 9, 2, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val tasks = (1..4).map { task("done-$it", TaskStatus.DONE, thisWeek + it) }

        val result = CreatorPersonalizationEngine.snapshot(profile(target = 2), tasks, now, utc)

        assertEquals(4, result.publishedThisWeek)
        assertEquals(1f, result.weeklyProgress, 0f)
    }

    @Test
    fun platformPriorityKeepsYouTubePrimaryForMultiPlatformCreator() {
        val result = CreatorPersonalizationEngine.snapshot(
            profile(platforms = setOf("Instagram", "YouTube", "X")),
            emptyList(),
            now,
            utc,
        )

        assertEquals("YouTube", result.primaryPlatform)
        assertEquals("YouTube · Instagram · X", result.platformSummary)
    }

    @Test
    fun growAudienceGuidanceUsesCreatorCategoryAndPlatform() {
        val result = CreatorPersonalizationEngine.snapshot(
            profile(goal = "Grow an audience", category = "Gaming", platforms = setOf("YouTube")),
            emptyList(),
            now,
            utc,
        )

        assertEquals("Create for reach, then learn.", result.focusTitle)
        assertTrue(result.focusBody.contains("YouTube"))
        assertTrue(result.focusBody.contains("gaming audience"))
        assertTrue(result.insightTitle.contains("audience momentum"))
    }

    @Test
    fun customGoalStillProducesUsefulCreatorSpecificFallback() {
        val result = CreatorPersonalizationEngine.snapshot(
            profile(goal = "Build a documentary portfolio", platforms = setOf("Instagram")),
            emptyList(),
            now,
            utc,
        )

        assertEquals("Keep your goal visible.", result.focusTitle)
        assertTrue(result.focusBody.contains("Build a documentary portfolio"))
        assertTrue(result.insightBody.contains("Instagram"))
    }

    private fun profile(
        target: Int = 2,
        goal: String = "Publish consistently",
        category: String = "Film & Entertainment",
        platforms: Set<String> = setOf("YouTube"),
    ) = CreatorProfile(
        displayName = "Test Creator",
        category = category,
        platforms = platforms,
        primaryGoal = goal,
        weeklyPublishingTarget = target,
    )

    private fun task(id: String, status: TaskStatus, completedAt: Long) = CreatorTask(
        id = id,
        title = id,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "This week",
        status = status,
        completedAtMillis = completedAt,
    )
}
''')

root = ROOT_UI.read_text()
root = replace_once(
    root,
    '                    creatorName = settings.creatorProfile.safeDisplayName,\n                    tasks = vm.tasks,',
    '                    creatorProfile = settings.creatorProfile,\n                    tasks = vm.tasks,',
    'Today profile wiring',
)
root = replace_once(
    root,
    '                PTab.INSIGHTS -> V11InsightsScreen(vm.tasks, vm.ideas, { openComposer() })',
    '''                PTab.INSIGHTS -> V11InsightsScreen(
                    creatorProfile = settings.creatorProfile,
                    tasks = vm.tasks,
                    ideas = vm.ideas,
                    onAdd = { openComposer() },
                )''',
    'Insights profile wiring',
)
ROOT_UI.write_text(root)

today = TODAY.read_text()
today = replace_once(
    today,
    '''internal fun PTodayScreen(
    creatorName: String,
    tasks: List<CreatorTask>,''',
    '''internal fun PTodayScreen(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,''',
    'Today signature',
)
today = replace_once(
    today,
    '''    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val haptics = LocalHapticFeedback.current''',
    '''    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val personalization = remember(creatorProfile, tasks.toList()) {
        CreatorPersonalizationEngine.snapshot(creatorProfile, tasks)
    }
    val haptics = LocalHapticFeedback.current''',
    'Today personalization state',
)
today = replace_once(today, '            PHomeGreetingHeader(creatorName, onAdd)', '            PHomeGreetingHeader(creatorProfile.safeDisplayName, onAdd)', 'Today greeting')
today = replace_once(
    today,
    '''            Text("Make the next thing.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(
                if (selected == null) "Your project list is clear." else "One clear next move. Everything else can wait.",
                color = MutedText,
                fontSize = 10.5.sp,
            )
            Spacer(Modifier.height(18.dp))

            if (selected == null) {''',
    '''            Text(personalization.focusTitle, color = ProjectorIvory, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black)
            Text(personalization.focusBody, color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            V18CreatorFocusCard(creatorProfile, personalization)
            Spacer(Modifier.height(18.dp))

            if (selected == null) {''',
    'Today personalized hero',
)
today = replace_once(
    today,
    '                    body = "Capture an idea or start your next project when you\'re ready.",',
    '                    body = personalization.emptyProjectBody,',
    'Today personalized empty state',
)
insert_anchor = '''@Composable
private fun PTodayProjectCard(task: CreatorTask) {'''
card = r'''@Composable
private fun V18CreatorFocusCard(
    profile: CreatorProfile,
    personalization: CreatorPersonalizationSnapshot,
) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(19.dp),
        Color(0xFF171310),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CREATOR FOCUS", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(profile.primaryGoal, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${personalization.publishedThisWeek} / ${personalization.weeklyTarget}",
                    color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { personalization.weeklyProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${personalization.publishedThisWeek} / ${personalization.weeklyTarget} published this week",
                    color = MutedText,
                    fontSize = 8.8.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(personalization.platformSummary, color = MutedText, fontSize = 8.3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(5.dp))
            Text(profile.category, color = ProjectorIvory.copy(alpha = .78f), fontSize = 8.5.sp)
        }
    }
}

'''
today = replace_once(today, insert_anchor, card + insert_anchor, 'Today creator focus card')
TODAY.write_text(today)

insights = INSIGHTS.read_text()
insights = replace_once(
    insights,
    '''internal fun V11InsightsScreen(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,''',
    '''internal fun V11InsightsScreen(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,''',
    'Insights signature',
)
insights = replace_once(
    insights,
    '''    var selectedVideo by remember { mutableStateOf<YouTubeVideoSnapshot?>(null) }
    var links by remember { mutableStateOf(store.links()) }''',
    '''    var selectedVideo by remember { mutableStateOf<YouTubeVideoSnapshot?>(null) }
    var links by remember { mutableStateOf(store.links()) }
    val personalization = remember(creatorProfile, tasks.toList()) {
        CreatorPersonalizationEngine.snapshot(creatorProfile, tasks)
    }''',
    'Insights personalization state',
)
insights = replace_once(
    insights,
    '''            Text("What matters — and what next?", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("See what changed, what worked and what to do next.", color = MutedText, fontSize = 10.5.sp)
            Spacer(Modifier.height(18.dp))

            if (snapshot == null) {''',
    '''            Text(personalization.insightTitle, color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
            Text(personalization.insightBody, color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            YTProfileFocusCard(creatorProfile, personalization)
            Spacer(Modifier.height(18.dp))

            if (snapshot == null) {''',
    'Insights personalized hero',
)
insights = replace_once(insights, '                YTLocalCreatorSection(tasks, ideas)', '                YTLocalCreatorSection(tasks, ideas, personalization)', 'Insights local section call')
insights = replace_once(
    insights,
    '''@Composable
private fun YTConnectCard(''',
    r'''@Composable
private fun YTProfileFocusCard(
    profile: CreatorProfile,
    personalization: CreatorPersonalizationSnapshot,
) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(19.dp),
        Color(0xFF171310),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR CREATOR LENS", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(profile.primaryGoal, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${personalization.publishedThisWeek} / ${personalization.weeklyTarget}",
                    color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { personalization.weeklyProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "${profile.category} · ${personalization.platformSummary} · ${personalization.publishedThisWeek} / ${personalization.weeklyTarget} published this week",
                color = MutedText,
                fontSize = 8.7.sp,
                lineHeight = 12.sp,
            )
        }
    }
}

@Composable
private fun YTConnectCard(''',
    'Insights creator focus card',
)
insights = replace_once(
    insights,
    '''private fun YTLocalCreatorSection(tasks: List<CreatorTask>, ideas: List<CreatorIdea>) {
    val done = tasks.count { it.status == TaskStatus.DONE }
    val active = tasks.count { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    Text("YOUR CREATOR PROGRESS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Your project progress matters alongside your channel numbers.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        YTMetric("DONE", done.toString(), SuccessGreen, Modifier.weight(1f))
        YTMetric("ACTIVE", active.toString(), RecRed, Modifier.weight(1f))
        YTMetric("IDEAS READY", readyIdeas.toString(), MutedGold, Modifier.weight(1f))
    }
}''',
    '''private fun YTLocalCreatorSection(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    personalization: CreatorPersonalizationSnapshot,
) {
    val active = tasks.count { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    Text("YOUR CREATOR PROGRESS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Your publishing rhythm matters alongside channel numbers.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        YTMetric("THIS WEEK", personalization.publishedThisWeek.toString(), SuccessGreen, Modifier.weight(1f))
        YTMetric("TARGET", personalization.weeklyTarget.toString(), MutedGold, Modifier.weight(1f))
        YTMetric("ACTIVE", active.toString(), RecRed, Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    Text("$readyIdeas idea${if (readyIdeas == 1) "" else "s"} ready to produce · ${personalization.primaryPlatform} is your primary publishing lane.", color = MutedText, fontSize = 8.7.sp)
}''',
    'Insights local profile metrics',
)
INSIGHTS.write_text(insights)

ui_test = UI_TEST.read_text()
ui_test = replace_once(
    ui_test,
    'import com.framebynavin.app.data.CreatorIdea\nimport com.framebynavin.app.data.CreatorTask',
    'import com.framebynavin.app.data.CreatorIdea\nimport com.framebynavin.app.data.CreatorProfile\nimport com.framebynavin.app.data.CreatorTask',
    'UI test CreatorProfile import',
)
ui_test = replace_exact_count(
    ui_test,
    '                creatorName = "Test Creator",',
    '                creatorProfile = testCreatorProfile(),',
    2,
    'Today UI test profile args',
)
new_test_anchor = '''    @Test
    fun plan_createStartAndDoneDispatchCorrectProject() {'''
new_test = r'''    @Test
    fun today_showsCreatorGoalAndWeeklyPublishingTarget() {
        val now = System.currentTimeMillis()
        val completed = projectTask("weekly-done", "Published this week").copy(
            status = TaskStatus.DONE,
            completedAtMillis = now,
        )

        composeRule.setContent {
            PTodayScreen(
                creatorProfile = testCreatorProfile(goal = "Grow an audience", target = 3),
                tasks = listOf(completed),
                onAdd = {},
                onStart = {},
                onAdvance = {},
                onViewAllReminders = {},
                onFocus = {},
            )
        }

        composeRule.onNodeWithText("CREATOR FOCUS").assertIsDisplayed()
        composeRule.onNodeWithText("Grow an audience").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 3 published this week").assertIsDisplayed()
    }

'''
ui_test = replace_once(ui_test, new_test_anchor, new_test + new_test_anchor, 'Today profile-aware UI test')
helper_anchor = '''    private fun projectTask(id: String, title: String) = CreatorTask('''
helper = r'''    private fun testCreatorProfile(
        goal: String = "Publish consistently",
        target: Int = 2,
    ) = CreatorProfile(
        displayName = "Test Creator",
        category = "Film & Entertainment",
        platforms = setOf("YouTube", "Instagram"),
        primaryGoal = goal,
        weeklyPublishingTarget = target,
    )

'''
ui_test = replace_once(ui_test, helper_anchor, helper + helper_anchor, 'UI test creator profile helper')
UI_TEST.write_text(ui_test)

print('v1.8 product alpha11 profile personalization applied')
