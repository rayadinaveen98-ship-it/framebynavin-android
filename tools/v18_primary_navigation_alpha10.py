from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
APP = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
IDEAS = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt'
CALENDAR = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V15ContextUi.kt'
CREATE = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt'
TEST = ROOT / 'app/src/androidTest/java/com/framebynavin/app/ui/V18PrimaryNavigationAlpha10UiTest.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 49', 'versionCode = 50', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha9"',
    'versionName = "1.8.0-product-alpha10"',
    'versionName',
)
BUILD.write_text(build)

app = APP.read_text()
app = replace_once(
    app,
    'private enum class PTab { TODAY, PLAN, STUDIO, INSIGHTS }\nprivate enum class POverlay { NONE, WEEK, RELEASE, IDEAS, DAILY_BRIEF, CALENDAR, AUTOMATION, SETTINGS }',
    'internal enum class PTab { TODAY, IDEAS, CREATE, CALENDAR, INSIGHTS }\nprivate enum class POverlay { NONE, WEEK, RELEASE, DAILY_BRIEF, AUTOMATION, SETTINGS }',
    'primary navigation enums',
)
app = replace_once(
    app,
    'CreatorWidgetContract.ACTION_OPEN_STUDIO -> {\n                overlay = POverlay.NONE\n                tab = PTab.STUDIO',
    'CreatorWidgetContract.ACTION_OPEN_STUDIO -> {\n                overlay = POverlay.NONE\n                tab = PTab.CREATE',
    'widget studio route',
)
app = replace_once(
    app,
    'CreatorWidgetContract.ACTION_CONTENT_CALENDAR -> overlay = POverlay.CALENDAR\n            CreatorWidgetContract.ACTION_IDEA_VAULT -> overlay = POverlay.IDEAS',
    'CreatorWidgetContract.ACTION_CONTENT_CALENDAR -> { overlay = POverlay.NONE; tab = PTab.CALENDAR }\n            CreatorWidgetContract.ACTION_IDEA_VAULT -> { overlay = POverlay.NONE; tab = PTab.IDEAS }',
    'widget first-class routes',
)
old_tabs = '''                PTab.PLAN -> V131PlanScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onEdit = { openComposer(it) },
                    onStart = vm::startTask,
                    onDone = vm::completeTask,
                    onDeleteSelected = vm::deleteTasks,
                )
                PTab.STUDIO -> V131StudioScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onAdvance = vm::advanceWorkflow,
                    onBack = vm::moveWorkflowBack,
                    onFocus = { focusTaskId = it },
                    onArchive = vm::archiveTask,
                    onUnarchive = vm::unarchiveTask,
                    onDelete = vm::deleteTask,
                    externalExpandId = externalStudioId,
                    externalExpandNonce = externalStudioNonce,
                )
                PTab.INSIGHTS -> V11InsightsScreen(vm.tasks, vm.ideas, { openComposer() })'''
new_tabs = '''                PTab.IDEAS -> V09IdeaVaultScreen(
                    ideas = vm.ideas,
                    onClose = null,
                    onSave = vm::saveIdea,
                    onDelete = vm::deleteIdea,
                    onArchive = vm::archiveIdea,
                    onConvert = vm::convertIdeaToProject,
                )
                PTab.CREATE -> V131StudioScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onAdvance = vm::advanceWorkflow,
                    onBack = vm::moveWorkflowBack,
                    onFocus = { focusTaskId = it },
                    onArchive = vm::archiveTask,
                    onUnarchive = vm::unarchiveTask,
                    onDelete = vm::deleteTask,
                    externalExpandId = externalStudioId,
                    externalExpandNonce = externalStudioNonce,
                )
                PTab.CALENDAR -> V15ContentCalendarScreen(
                    tasks = vm.tasks,
                    weeklySlots = vm.weeklySlots,
                    onClose = null,
                )
                PTab.INSIGHTS -> V11InsightsScreen(vm.tasks, vm.ideas, { openComposer() })'''
app = replace_once(app, old_tabs, new_tabs, 'primary tab destinations')
app = replace_once(
    app,
    '''            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                onCreate = { openComposer() },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {''',
    '''            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {
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

            if (overlay == POverlay.NONE && settings.onboardingComplete && !showControl) {''',
    'universal capture action',
)
app = replace_once(
    app,
    '''            POverlay.IDEAS -> V09IdeaVaultScreen(
                ideas = vm.ideas,
                onClose = { overlay = POverlay.NONE },
                onSave = vm::saveIdea,
                onDelete = vm::deleteIdea,
                onArchive = vm::archiveIdea,
                onConvert = vm::convertIdeaToProject,
            )
''',
    '',
    'remove ideas overlay',
)
app = replace_once(
    app,
    '''            POverlay.CALENDAR -> V15ContentCalendarScreen(
                tasks = vm.tasks,
                weeklySlots = vm.weeklySlots,
                onClose = { overlay = POverlay.NONE },
            )
''',
    '',
    'remove calendar overlay',
)
app = replace_once(
    app,
    'onCalendar = { showControl = false; overlay = POverlay.CALENDAR },',
    'onCalendar = { showControl = false; overlay = POverlay.NONE; tab = PTab.CALENDAR },',
    'control calendar route',
)
app = replace_once(
    app,
    'onIdeas = { showControl = false; overlay = POverlay.IDEAS },',
    'onIdeas = { showControl = false; overlay = POverlay.NONE; tab = PTab.IDEAS },',
    'control ideas route',
)
start = app.index('@Composable\nprivate fun PBottomNav(')
end = app.index('\n@Composable\nprivate fun PBottomNavItem(', start)
new_bottom = '''@Composable
internal fun PBottomNav(
    selected: PTab,
    onSelect: (PTab) -> Unit,
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
            PBottomNavItem(PTab.CREATE, Icons.Outlined.MovieEdit, "Create", selected == PTab.CREATE, onSelect, Modifier.weight(1f))
            PBottomNavItem(PTab.CALENDAR, Icons.Outlined.CalendarMonth, "Calendar", selected == PTab.CALENDAR, onSelect, Modifier.weight(1f))
            PBottomNavItem(PTab.INSIGHTS, Icons.Outlined.Insights, "Insights", selected == PTab.INSIGHTS, onSelect, Modifier.weight(1f))
        }
    }
}
'''
app = app[:start] + new_bottom + app[end:]
APP.write_text(app)

ideas = IDEAS.read_text()
ideas = replace_once(ideas, '    onClose: () -> Unit,', '    onClose: (() -> Unit)?,', 'ideas nullable close')
ideas = replace_once(
    ideas,
    '                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }',
    '                if (onClose != null) IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }',
    'ideas optional back',
)
ideas = replace_once(ideas, 'bottom = 34.dp', 'bottom = 124.dp', 'ideas primary nav bottom inset')
IDEAS.write_text(ideas)

calendar = CALENDAR.read_text()
calendar = replace_once(calendar, '    onClose: () -> Unit,\n) {\n    val items = CreatorContentCalendarEngine.upcoming', '    onClose: (() -> Unit)?,\n) {\n    val items = CreatorContentCalendarEngine.upcoming', 'calendar nullable close')
calendar = replace_once(
    calendar,
    '    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {\n        Column(\n            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()\n                .padding(horizontal = 20.dp).padding(bottom = 42.dp)\n        ) {\n            V15BackHeader("CONTENT CALENDAR", "The next 14 days", onClose)',
    '    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {\n        Column(\n            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()\n                .padding(horizontal = 20.dp).padding(bottom = 124.dp)\n        ) {\n            V15BackHeader("CONTENT CALENDAR", "The next 14 days", onClose)',
    'calendar primary nav bottom inset',
)
calendar = replace_once(
    calendar,
    'private fun V15BackHeader(kicker: String, title: String, onBack: () -> Unit) {\n    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {\n        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }\n        Spacer(Modifier.width(4.dp))',
    'private fun V15BackHeader(kicker: String, title: String, onBack: (() -> Unit)?) {\n    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {\n        if (onBack != null) {\n            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }\n            Spacer(Modifier.width(4.dp))\n        }',
    'calendar optional back',
)
CALENDAR.write_text(calendar)

create = CREATE.read_text()
create = replace_once(create, 'Text("STUDIO", color = RecRed', 'Text("CREATE", color = RecRed', 'create screen kicker')
create = replace_once(create, 'Text("Build the thing.", color = ProjectorIvory', 'Text("Turn ideas into finished work.", color = ProjectorIvory', 'create screen title')
create = replace_once(create, 'Text("Open a project to see what’s done and what comes next.", color = MutedText', 'Text("Create a project, move through its steps, and finish what you started.", color = MutedText', 'create screen subtitle')
CREATE.write_text(create)

TEST.write_text(r'''package com.framebynavin.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18PrimaryNavigationAlpha10UiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryNavigationDispatchesEveryDestination() {
        var selected = PTab.TODAY
        composeRule.setContent {
            PBottomNav(selected = selected, onSelect = { selected = it })
        }

        listOf(
            "Ideas" to PTab.IDEAS,
            "Create" to PTab.CREATE,
            "Calendar" to PTab.CALENDAR,
            "Insights" to PTab.INSIGHTS,
            "Today" to PTab.TODAY,
        ).forEach { (label, expected) ->
            composeRule.onNodeWithText(label).performClick()
            composeRule.runOnIdle { assertEquals(expected, selected) }
        }
    }

    @Test
    fun ideasRenderAsFirstClassScreenWithoutBackAffordance() {
        composeRule.setContent {
            V09IdeaVaultScreen(
                ideas = emptyList(),
                onClose = null,
                onSave = { null },
                onDelete = {},
                onArchive = {},
                onConvert = { _, _, _, _ -> null },
            )
        }
        composeRule.onNodeWithText("Capture now. Produce later.").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    }

    @Test
    fun calendarRendersAsFirstClassScreenWithoutBackAffordance() {
        composeRule.setContent {
            V15ContentCalendarScreen(tasks = emptyList(), weeklySlots = emptyList(), onClose = null)
        }
        composeRule.onNodeWithText("The next 14 days").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    }

    @Test
    fun createScreenUsesCreatorNeutralProductLanguage() {
        composeRule.setContent {
            V131StudioScreen(
                tasks = emptyList(),
                onAdd = {},
                onAdvance = {},
                onBack = {},
                onFocus = {},
                onArchive = {},
                onUnarchive = {},
                onDelete = {},
            )
        }
        composeRule.onNodeWithText("CREATE").assertIsDisplayed()
        composeRule.onNodeWithText("Turn ideas into finished work.").assertIsDisplayed()
        composeRule.onNodeWithText("CREATE PROJECT").assertIsDisplayed()
    }
}
''')

print('Applied v1.8 Product Alpha10 primary Creator OS navigation')
