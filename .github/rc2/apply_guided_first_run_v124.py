from pathlib import Path

ROOT = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
BUILD = Path("app/build.gradle.kts")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


text = ROOT.read_text()

text = replace_once(
    text,
    '''    var externalStudioNonce by rememberSaveable { mutableLongStateOf(0L) }\n\n    BackHandler(enabled = focusTaskId != null || showComposer || showQuickCapture || showReminders || showControl || overlay != POverlay.NONE || tab != PTab.TODAY) {\n        when {\n            focusTaskId != null -> focusTaskId = null\n''',
    '''    var externalStudioNonce by rememberSaveable { mutableLongStateOf(0L) }\n    var guidedTourStepName by rememberSaveable { mutableStateOf<String?>(null) }\n\n    BackHandler(enabled = guidedTourStepName != null || focusTaskId != null || showComposer || showQuickCapture || showReminders || showControl || overlay != POverlay.NONE || tab != PTab.TODAY) {\n        when {\n            guidedTourStepName != null -> {\n                settingsStore.markGuidedTourComplete()\n                settings = settingsStore.snapshot()\n                guidedTourStepName = null\n            }\n            focusTaskId != null -> focusTaskId = null\n''',
    "guided tour state/back handling",
)

text = replace_once(
    text,
    '''    LaunchedEffect(externalLaunch?.nonce) {\n        val launch = externalLaunch ?: return@LaunchedEffect\n        showControl = false\n''',
    '''    LaunchedEffect(externalLaunch?.nonce) {\n        val launch = externalLaunch ?: return@LaunchedEffect\n        // Deep links and widgets must stay instant. A normal launch can resume the tour later.\n        guidedTourStepName = null\n        showControl = false\n''',
    "external launch bypass",
)

text = replace_once(
    text,
    '''    }\n\n    val focusTaskState = remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }\n''',
    '''    }\n\n    LaunchedEffect(\n        settings.accountOnboardingComplete,\n        settings.onboardingComplete,\n        settings.creatorProfile.isComplete,\n        settings.guidedTourVersion,\n        externalLaunch?.nonce,\n    ) {\n        if (\n            guidedTourStepName == null &&\n            CreatorGuidedTourPolicy.shouldStart(\n                accountOnboardingComplete = settings.accountOnboardingComplete,\n                creatorSetupComplete = settings.onboardingComplete,\n                profileComplete = settings.creatorProfile.isComplete,\n                completedVersion = settings.guidedTourVersion,\n                externalLaunch = externalLaunch != null,\n            )\n        ) {\n            overlay = POverlay.NONE\n            showControl = false\n            showReminders = false\n            externalStudioId = null\n            tab = PTab.TODAY\n            guidedTourStepName = CreatorGuidedTourStep.TODAY.name\n        }\n    }\n\n    val guidedTourStep = guidedTourStepName?.let { saved ->\n        CreatorGuidedTourStep.entries.firstOrNull { it.name == saved }\n    }\n    val focusTaskState = remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }\n''',
    "automatic guided tour start",
)

text = replace_once(
    text,
    '''                onBattery = ::openBatterySettings,\n                onCloudSync = { context.startActivity(Intent(context, CloudSyncActivity::class.java)) },\n''',
    '''                onBattery = ::openBatterySettings,\n                onReplayTour = {\n                    settingsStore.resetGuidedTour()\n                    settings = settingsStore.snapshot()\n                    overlay = POverlay.NONE\n                    showControl = false\n                    showQuickCapture = false\n                    showReminders = false\n                    showComposer = false\n                    focusTaskId = null\n                    externalStudioId = null\n                    tab = PTab.TODAY\n                    guidedTourStepName = CreatorGuidedTourStep.TODAY.name\n                },\n                onCloudSync = { context.startActivity(Intent(context, CloudSyncActivity::class.java)) },\n''',
    "settings replay callback",
)

text = replace_once(
    text,
    '''            onSave = { idea ->\n                vm.saveIdea(idea)\n                showQuickCapture = false\n                routeJourney(V18CreatorJourney.afterCapture())\n            },\n''',
    '''            onSave = { idea ->\n                vm.saveIdea(idea)\n                showQuickCapture = false\n                routeJourney(V18CreatorJourney.afterCapture())\n                if (guidedTourStepName == CreatorGuidedTourStep.IDEAS.name) {\n                    guidedTourStepName = CreatorGuidedTourStep.PROJECT.name\n                }\n            },\n''',
    "idea capture tour progression",
)

text = replace_once(
    text,
    '''                showComposer = false\n                if (task == null && V18CreatorJourney.afterProjectCreated(savedTaskId) == V18JourneyDestination.CREATE) {\n''',
    '''                showComposer = false\n                if (task == null && guidedTourStepName == CreatorGuidedTourStep.PROJECT.name) {\n                    guidedTourStepName = CreatorGuidedTourStep.WORKSPACE.name\n                }\n                if (task == null && V18CreatorJourney.afterProjectCreated(savedTaskId) == V18JourneyDestination.CREATE) {\n''',
    "project wizard tour progression",
)

text = replace_once(
    text,
    '''    } else if (!settings.onboardingComplete || !settings.creatorProfile.isComplete) {\n        V18CreatorOnboarding(\n            profile = settings.creatorProfile,\n            notificationsReady = permissions.notifications,\n            preciseTimingReady = permissions.preciseTiming,\n            fullScreenReady = permissions.fullScreen,\n            batteryReady = permissions.batteryAccess,\n            onNotifications = ::requestNotifications,\n            onPreciseTiming = ::requestPreciseTiming,\n            onFullScreen = ::requestFullScreen,\n            onBattery = ::openBatterySettings,\n            onFinish = { profile ->\n                settingsStore.setCreatorProfile(profile)\n                settingsStore.setOnboardingComplete(true)\n                settings = settingsStore.snapshot()\n            },\n        )\n    }\n}\n''',
    '''    } else if (!settings.onboardingComplete || !settings.creatorProfile.isComplete) {\n        V18CreatorOnboarding(\n            profile = settings.creatorProfile,\n            notificationsReady = permissions.notifications,\n            preciseTimingReady = permissions.preciseTiming,\n            fullScreenReady = permissions.fullScreen,\n            batteryReady = permissions.batteryAccess,\n            onNotifications = ::requestNotifications,\n            onPreciseTiming = ::requestPreciseTiming,\n            onFullScreen = ::requestFullScreen,\n            onBattery = ::openBatterySettings,\n            onFinish = { profile ->\n                settingsStore.setCreatorProfile(profile)\n                settingsStore.setOnboardingComplete(true)\n                settings = settingsStore.snapshot()\n            },\n        )\n    }\n\n    if (\n        guidedTourStep != null &&\n        settings.accountOnboardingComplete &&\n        settings.onboardingComplete &&\n        settings.creatorProfile.isComplete &&\n        !showComposer &&\n        !showQuickCapture &&\n        !showControl &&\n        focusTaskId == null &&\n        overlay == POverlay.NONE\n    ) {\n        V20GuidedFirstRunCoach(\n            step = guidedTourStep,\n            hasProjects = vm.tasks.any { it.archivedAtMillis == 0L },\n            onPrimary = {\n                when (guidedTourStep) {\n                    CreatorGuidedTourStep.TODAY -> {\n                        externalStudioId = null\n                        tab = PTab.IDEAS\n                        guidedTourStepName = CreatorGuidedTourStep.IDEAS.name\n                    }\n                    CreatorGuidedTourStep.IDEAS -> {\n                        showQuickCapture = true\n                    }\n                    CreatorGuidedTourStep.PROJECT -> {\n                        val project = vm.tasks.firstOrNull { it.archivedAtMillis == 0L && it.status != TaskStatus.SKIPPED }\n                            ?: vm.tasks.firstOrNull { it.archivedAtMillis == 0L }\n                            ?: vm.tasks.firstOrNull()\n                        if (project == null) {\n                            openComposer()\n                        } else {\n                            openProject(project.id)\n                            guidedTourStepName = CreatorGuidedTourStep.WORKSPACE.name\n                        }\n                    }\n                    CreatorGuidedTourStep.WORKSPACE -> {\n                        externalStudioId = null\n                        tab = PTab.INSIGHTS\n                        guidedTourStepName = CreatorGuidedTourStep.INSIGHTS.name\n                    }\n                    CreatorGuidedTourStep.INSIGHTS -> {\n                        guidedTourStepName = CreatorGuidedTourStep.CONTROL.name\n                    }\n                    CreatorGuidedTourStep.CONTROL -> {\n                        settingsStore.markGuidedTourComplete()\n                        settings = settingsStore.snapshot()\n                        guidedTourStepName = null\n                        showControl = true\n                    }\n                }\n            },\n            onSecondary = {\n                if (guidedTourStep == CreatorGuidedTourStep.IDEAS) {\n                    guidedTourStepName = CreatorGuidedTourStep.PROJECT.name\n                }\n            },\n            onSkip = {\n                settingsStore.markGuidedTourComplete()\n                settings = settingsStore.snapshot()\n                guidedTourStepName = null\n            },\n        )\n    }\n}\n''',
    "guided coach host",
)

text = replace_once(
    text,
    '''    onBattery: () -> Unit,\n    onCloudSync: () -> Unit,\n''',
    '''    onBattery: () -> Unit,\n    onReplayTour: () -> Unit,\n    onCloudSync: () -> Unit,\n''',
    "settings replay parameter",
)

text = replace_once(
    text,
    '''            Spacer(Modifier.height(22.dp))\n            PSettingsHeading("BACKUP & SYNC", "Keep a safe cloud copy of your work.")\n''',
    '''            Spacer(Modifier.height(22.dp))\n            PSettingsHeading("GUIDED TOUR", "Replay the creator journey whenever you want.")\n            Spacer(Modifier.height(8.dp))\n            Surface(\n                onClick = onReplayTour,\n                modifier = Modifier.fillMaxWidth(),\n                shape = RoundedCornerShape(16.dp),\n                color = CinemaSurface,\n                border = BorderStroke(1.dp, CinemaLine),\n            ) {\n                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {\n                    Box(Modifier.size(34.dp).background(MutedGold.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {\n                        Icon(Icons.Outlined.School, null, tint = MutedGold, modifier = Modifier.size(18.dp))\n                    }\n                    Spacer(Modifier.width(11.dp))\n                    Column(Modifier.weight(1f)) {\n                        Text("Replay guided first run", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)\n                        Text("Today → Ideas → Project → Workspace → Insights → Control", color = MutedText, fontSize = 8.7.sp)\n                    }\n                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))\n                }\n            }\n\n            Spacer(Modifier.height(22.dp))\n            PSettingsHeading("BACKUP & SYNC", "Keep a safe cloud copy of your work.")\n''',
    "guided tour settings row",
)

ROOT.write_text(text)

build = BUILD.read_text()
build = replace_once(build, 'versionCode = 123', 'versionCode = 124', 'version code')
build = replace_once(
    build,
    'versionName = "2.0.0-rc2-account-cloud-sync"',
    'versionName = "2.0.0-rc2-guided-first-run"',
    'version name',
)
BUILD.write_text(build)

print("Applied guided first-run integration and v124 version bump")
