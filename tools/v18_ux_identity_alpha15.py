from pathlib import Path
import re

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
APP = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
WEEKLY = ROOT / 'app/src/main/java/com/framebynavin/app/data/WeeklySchedule.kt'
WEEKLY_STORE = ROOT / 'app/src/main/java/com/framebynavin/app/data/WeeklyScheduleStore.kt'
VM = ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt'
FRAMES = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt'
ALPHA10_UI_TEST = ROOT / 'app/src/androidTest/java/com/framebynavin/app/ui/V18PrimaryNavigationAlpha10UiTest.kt'
ALPHA15_UI_TEST = ROOT / 'app/src/androidTest/java/com/framebynavin/app/ui/V18UxIdentityAlpha15UiTest.kt'
ALPHA15_UNIT_TEST = ROOT / 'app/src/test/java/com/framebynavin/app/data/V18UxIdentityAlpha15Test.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


# Version
build = BUILD.read_text()
build = replace_once(build, 'versionCode = 54', 'versionCode = 55', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha14"',
    'versionName = "1.8.0-product-alpha15"',
    'versionName',
)
BUILD.write_text(build)

# 1) Remove legacy seeded FrameByNavin weekly workload at the source.
weekly = WEEKLY.read_text()
pattern = re.compile(
    r'    fun defaultSlots\(\): List<WeeklyScheduleSlot> = listOf\(.*?\n    \)\n\n    fun upcomingOccurrences',
    re.S,
)
legacy_block = '''    private val legacySeedSlotIds = setOf(
        "mon_x_thought",
        "mon_frame_today",
        "tue_x_poll",
        "tue_scene_works",
        "wed_carousel",
        "wed_cinematic_moment",
        "thu_frame_breakdown_ig",
        "thu_frame_breakdown_yt",
        "fri_release_short",
        "sun_flagship",
        "sun_companion_reel",
    )

    fun isLegacySeedSlot(slotId: String): Boolean = slotId in legacySeedSlotIds

    fun defaultSlots(): List<WeeklyScheduleSlot> = emptyList()

    fun upcomingOccurrences'''
weekly, count = pattern.subn(legacy_block, weekly, count=1)
if count != 1:
    raise SystemExit(f'weekly default seed removal: expected 1 match, found {count}')
WEEKLY.write_text(weekly)

weekly_store = WEEKLY_STORE.read_text()
pattern = re.compile(
    r'    suspend fun loadOrSeed\(\): List<WeeklyScheduleSlot> \{.*?\n    \}\n\n    suspend fun save',
    re.S,
)
replacement = '''    suspend fun loadOrSeed(): List<WeeklyScheduleSlot> {
        val prefs = context.weeklyScheduleDataStore.data.first()
        val raw = prefs[slotsKey] ?: return emptyList()
        val decoded = runCatching { decode(raw) }.getOrElse { emptyList() }
        val cleaned = decoded.filterNot { WeeklyScheduleEngine.isLegacySeedSlot(it.id) }
        if (cleaned.size != decoded.size) save(cleaned)
        return cleaned
    }

    suspend fun save'''
weekly_store, count = pattern.subn(replacement, weekly_store, count=1)
if count != 1:
    raise SystemExit(f'weekly store migration: expected 1 match, found {count}')
WEEKLY_STORE.write_text(weekly_store)

vm = VM.read_text()
vm = replace_once(
    vm,
    '                val cleaned = saved.filterNot { it.id == "starter-frame-breakdown" }\n                tasks.clear()\n                tasks.addAll(cleaned)\n                reconcileSnapshot(cleaned)\n                if (cleaned.size != saved.size) store.save(cleaned)',
    '''                val legacySeeded = saved.filter {
                    it.origin == CreatorTaskOrigin.WEEKLY && WeeklyScheduleEngine.isLegacySeedSlot(it.scheduleSlotId)
                }
                legacySeeded.forEach { cancelTaskAlerts(it.id) }
                val cleaned = saved.filterNot {
                    it.id == "starter-frame-breakdown" ||
                        (it.origin == CreatorTaskOrigin.WEEKLY && WeeklyScheduleEngine.isLegacySeedSlot(it.scheduleSlotId))
                }
                tasks.clear()
                tasks.addAll(cleaned)
                reconcileSnapshot(cleaned)
                if (cleaned.size != saved.size) store.save(cleaned)''',
    'legacy generated task cleanup',
)
VM.write_text(vm)

# 2) Rebuild primary navigation around a larger center Capture action; Calendar returns to Control Center.
app = APP.read_text()
app = replace_once(
    app,
    'internal enum class PTab { TODAY, IDEAS, CREATE, CALENDAR, INSIGHTS }\nprivate enum class POverlay { NONE, WEEK, RELEASE, DAILY_BRIEF, AUTOMATION, SETTINGS }',
    'internal enum class PTab { TODAY, IDEAS, CREATE, CALENDAR, INSIGHTS }\nprivate enum class POverlay { NONE, WEEK, RELEASE, DAILY_BRIEF, CALENDAR, AUTOMATION, SETTINGS }',
    'restore calendar overlay',
)
app = replace_once(
    app,
    'CreatorWidgetContract.ACTION_CONTENT_CALENDAR -> { overlay = POverlay.NONE; tab = PTab.CALENDAR }',
    'CreatorWidgetContract.ACTION_CONTENT_CALENDAR -> overlay = POverlay.CALENDAR',
    'widget calendar overlay route',
)
app = replace_once(
    app,
    'onCalendar = { showControl = false; overlay = POverlay.NONE; tab = PTab.CALENDAR },',
    'onCalendar = { showControl = false; overlay = POverlay.CALENDAR },',
    'control calendar overlay route',
)
calendar_anchor = '''            POverlay.DAILY_BRIEF -> V15DailyBriefScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.AUTOMATION ->'''
calendar_insert = '''            POverlay.DAILY_BRIEF -> V15DailyBriefScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.CALENDAR -> V15ContentCalendarScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
            POverlay.AUTOMATION ->'''
app = replace_once(app, calendar_anchor, calendar_insert, 'calendar overlay content')

# Remove the floating Capture pill added in Alpha10.
floating_capture = '''            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { showQuickCapture = true },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed,
                    shadowElevation = 11.dp,
                ) {
                    Row(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Add, "Capture idea", tint = ProjectorIvory, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("CAPTURE", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    }
                }
            }

'''
app = replace_once(app, floating_capture, '', 'remove floating capture')
app = replace_once(
    app,
    '''            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )''',
    '''            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                onCapture = { showQuickCapture = true },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )''',
    'wire center capture',
)
start = app.index('@Composable\ninternal fun PBottomNav(')
end = app.index('\n@Composable\nprivate fun PBottomNavItem(', start)
new_bottom = '''@Composable
internal fun PBottomNav(
    selected: PTab,
    onSelect: (PTab) -> Unit,
    onCapture: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier,
        RoundedCornerShape(24.dp),
        Color(0xF2161618),
        border = BorderStroke(1.dp, CinemaLine),
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PBottomNavItem(PTab.TODAY, Icons.Outlined.Home, "Today", selected == PTab.TODAY, onSelect, Modifier.weight(1f))
            PBottomNavItem(PTab.IDEAS, Icons.Outlined.Lightbulb, "Ideas", selected == PTab.IDEAS, onSelect, Modifier.weight(1f))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = onCapture,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = RecRed,
                    shadowElevation = 9.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Capture idea",
                            tint = ProjectorIvory,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
            PBottomNavItem(PTab.CREATE, Icons.Outlined.MovieEdit, "Create", selected == PTab.CREATE, onSelect, Modifier.weight(1f))
            PBottomNavItem(PTab.INSIGHTS, Icons.Outlined.Insights, "Insights", selected == PTab.INSIGHTS, onSelect, Modifier.weight(1f))
        }
    }
}
'''
app = app[:start] + new_bottom + app[end:]

# 3) Restore the pre-Alpha14 expanding Control effect exactly.
app = replace_once(
    app,
    'import androidx.compose.animation.AnimatedContent\n',
    'import androidx.compose.animation.AnimatedContent\nimport androidx.compose.animation.AnimatedVisibility\n',
    'restore AnimatedVisibility import',
)
app = replace_once(
    app,
    '    var showControl by rememberSaveable { mutableStateOf(false) }\n',
    '    var showControl by rememberSaveable { mutableStateOf(false) }\n    var controlExpanded by rememberSaveable { mutableStateOf(false) }\n',
    'restore control expanded state',
)
app = replace_once(
    app,
    '    LaunchedEffect(externalLaunch?.nonce) {',
    '''    LaunchedEffect(controlExpanded) {
        if (controlExpanded) {
            delay(180L)
            showControl = true
            controlExpanded = false
        }
    }

    LaunchedEffect(externalLaunch?.nonce) {''',
    'restore delayed control reveal',
)
current_control = '''            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { showControl = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 96.dp).size(46.dp),
                    shape = CircleShape,
                    color = CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, CinemaLine),
                    shadowElevation = 7.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.GridView, "Open control center", tint = ProjectorIvory, modifier = Modifier.size(19.dp))
                    }
                }
            }
'''
restored_control = '''            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
                Surface(
                    onClick = { if (!controlExpanded) controlExpanded = true },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 98.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed,
                    shadowElevation = 10.dp,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.GridView, "Control", tint = ProjectorIvory, modifier = Modifier.size(19.dp))
                        AnimatedVisibility(visible = controlExpanded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(7.dp))
                                Text("CONTROL", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
'''
app = replace_once(app, current_control, restored_control, 'restore control effect')
APP.write_text(app)

# Keep Alpha10 instrumentation expectations aligned with the new four-destination nav.
alpha10_test = ALPHA10_UI_TEST.read_text()
alpha10_test = alpha10_test.replace('            "Calendar" to PTab.CALENDAR,\n', '')
ALPHA10_UI_TEST.write_text(alpha10_test)

# 4) Slideshow surface becomes pure imagery: no labels, counters, dots, gradients or arrows on/around the frame.
frames = FRAMES.read_text()
surface_start = frames.index('                Surface(\n                    modifier = Modifier.fillMaxWidth().height(236.dp),')
management_marker = '''                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = selectOriginals) {'''
management_start = frames.index(management_marker, surface_start)
image_only = '''                Surface(
                    modifier = Modifier.fillMaxWidth().height(236.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, CinemaLine.copy(alpha = .52f)),
                    shadowElevation = 7.dp,
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CinemaBlack)
                    ) {
                        AnimatedContent(
                            targetState = frames.getOrNull(index)?.absolutePath.orEmpty(),
                            transitionSpec = { fadeIn(tween(480)) togetherWith fadeOut(tween(480)) },
                            label = "v175OriginalFrame",
                        ) { path ->
                            val frame = frames.firstOrNull { it.absolutePath == path }
                            val bitmap = remember(path, frame?.lastModified()) {
                                frame?.let(::v175DecodeForDisplay)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }

'''
frames = frames[:surface_start] + image_only + frames[management_start:]
FRAMES.write_text(frames)

# Alpha15 product tests.
ALPHA15_UNIT_TEST.parent.mkdir(parents=True, exist_ok=True)
ALPHA15_UNIT_TEST.write_text(r'''package com.framebynavin.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V18UxIdentityAlpha15Test {
    @Test
    fun freshWeeklyScheduleHasNoFrameByNavinSeedData() {
        assertTrue(WeeklyScheduleEngine.defaultSlots().isEmpty())
    }

    @Test
    fun legacySeedIdsAreRecognizedWithoutMatchingUserSlots() {
        assertTrue(WeeklyScheduleEngine.isLegacySeedSlot("sun_flagship"))
        assertTrue(WeeklyScheduleEngine.isLegacySeedSlot("mon_frame_today"))
        assertFalse(WeeklyScheduleEngine.isLegacySeedSlot("custom-my-real-project"))
    }
}
''')

ALPHA15_UI_TEST.parent.mkdir(parents=True, exist_ok=True)
ALPHA15_UI_TEST.write_text(r'''package com.framebynavin.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18UxIdentityAlpha15UiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureLivesInCenterNavAndCalendarIsNotPrimary() {
        var captured = false
        composeRule.setContent {
            PBottomNav(
                selected = PTab.TODAY,
                onSelect = {},
                onCapture = { captured = true },
            )
        }

        composeRule.onAllNodesWithText("Calendar").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Capture idea").performClick()
        composeRule.runOnIdle { assertTrue(captured) }
    }
}
''')

print('Applied v1.8 Product Alpha15 UX/identity corrections')
