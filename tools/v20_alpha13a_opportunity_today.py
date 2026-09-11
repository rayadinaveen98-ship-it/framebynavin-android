from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found; refusing broad rewrite")
    path.write_text(text.replace(old, new, 1))


# Surface the already-validated Opportunity Engine where the creator makes the daily decision.
today = Path("app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt")
replace_once(
    today,
    '''internal fun PTodayScreen(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onStart: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onViewAllReminders: () -> Unit,
    onFocus: (String) -> Unit,
    onOpenProject: (String) -> Unit = {},
) {''',
    '''internal fun PTodayScreen(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onAdd: () -> Unit,
    onStart: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onViewAllReminders: () -> Unit,
    onFocus: (String) -> Unit,
    onOpenIdeaVault: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenProject: (String) -> Unit = {},
) {''',
    "Today arguments",
)
replace_once(
    today,
    '''            V18CreatorFocusCard(creatorProfile, personalization, onClick = { showWeeklyFocus = true })
            Spacer(Modifier.height(18.dp))

            if (selected == null) {''',
    '''            V18CreatorFocusCard(creatorProfile, personalization, onClick = { showWeeklyFocus = true })
            Spacer(Modifier.height(14.dp))
            V20OpportunityEngineCard(
                tasks = tasks,
                ideas = ideas,
                onOpenIdeaVault = onOpenIdeaVault,
                onOpenInsights = onOpenInsights,
                onOpenProject = onOpenProject,
            )
            Spacer(Modifier.height(18.dp))

            if (selected == null) {''',
    "Today Opportunity Engine surface",
)

router = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
replace_once(
    router,
    '''                PTab.TODAY -> PTodayScreen(
                    creatorProfile = settings.creatorProfile,
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onStart = vm::startTask,
                    onAdvance = ::advanceWorkflowWithJourney,
                    onViewAllReminders = { showReminders = true },
                    onFocus = { focusTaskId = it },
                    onOpenProject = ::openProject,
                )''',
    '''                PTab.TODAY -> PTodayScreen(
                    creatorProfile = settings.creatorProfile,
                    tasks = vm.tasks,
                    ideas = vm.ideas,
                    onAdd = { openComposer() },
                    onStart = vm::startTask,
                    onAdvance = ::advanceWorkflowWithJourney,
                    onViewAllReminders = { showReminders = true },
                    onFocus = { focusTaskId = it },
                    onOpenIdeaVault = { tab = PTab.IDEAS },
                    onOpenInsights = { tab = PTab.INSIGHTS },
                    onOpenProject = ::openProject,
                )''',
    "Today routing",
)

# New installable milestone. Keep v106 intact and move forward one version code.
gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 106' not in build or 'versionName = "2.0.0-alpha1.3-opportunity-engine"' not in build:
    raise SystemExit("Unexpected v106 build identity")
build = build.replace('versionCode = 106', 'versionCode = 107', 1)
build = build.replace(
    'versionName = "2.0.0-alpha1.3-opportunity-engine"',
    'versionName = "2.0.0-alpha1.3a-opportunity-today"',
    1,
)
gradle.write_text(build)
