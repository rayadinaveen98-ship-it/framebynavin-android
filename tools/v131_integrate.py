from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path, old, new, count=1):
    p = ROOT / path
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"marker not found in {path}: {old[:120]!r}")
    text = text.replace(old, new, count)
    p.write_text(text)

# 1) First install must be genuinely empty; also clean only the historical demo seed by stable id.
vm = "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt"
old_seed = '''            store.tasksFlow.collectLatest { saved ->
                if (saved.isEmpty() && tasks.isEmpty()) {
                    val starterTemplate = CreatorWorkflowEngine.templateFor("Instagram", "Reel")
                    val starterStage = CreatorWorkflowEngine.stageIndexFromProgress(72, starterTemplate.stages.size)
                    val starter = CreatorTask(
                        id = "starter-frame-breakdown",
                        title = "Frame Breakdown",
                        platform = "Instagram",
                        contentType = "Reel",
                        dueLabel = "Today · 7:00 PM",
                        status = TaskStatus.WORKING,
                        progress = CreatorWorkflowEngine.progressForStage(starterStage, starterTemplate.stages.size),
                        workflowStageIndex = starterStage,
                        reminderMode = ReminderMode.NONE,
                    )
                    tasks += starter
                    store.save(tasks.toList())
                } else {
                    tasks.clear()
                    tasks.addAll(saved)
                    reconcileSnapshot(saved)
                }
                tasksLoaded = true
                if (weeklyLoaded) {
                    if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal() else removeUnstartedWeeklyProjects()
                }
            }
'''
new_seed = '''            store.tasksFlow.collectLatest { saved ->
                val cleaned = saved.filterNot { it.id == "starter-frame-breakdown" }
                tasks.clear()
                tasks.addAll(cleaned)
                reconcileSnapshot(cleaned)
                if (cleaned.size != saved.size) store.save(cleaned)
                tasksLoaded = true
                if (weeklyLoaded) {
                    if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal() else removeUnstartedWeeklyProjects()
                }
            }
'''
patch(vm, old_seed, new_seed)

marker = '''    fun saveIdea(idea: CreatorIdea): String? {
'''
methods = '''    /** Remove alert configuration only. Project/task data remains intact. */
    fun cancelReminders(ids: Set<String>) {
        if (ids.isEmpty()) return
        ids.forEach(::cancelTaskAlerts)
        var changed = false
        tasks.indices.forEach { index ->
            val task = tasks[index]
            if (task.id in ids && task.reminderEnabled) {
                tasks[index] = task.copy(
                    reminderEnabled = false,
                    reminderAtMillis = 0L,
                    smartEscalationEnabled = false,
                    voiceEnabled = false,
                    snoozeCount = 0,
                    workingUntilMillis = 0L,
                    reminderMode = ReminderMode.NONE,
                    autoStageReminder = false,
                )
                changed = true
            }
        }
        if (changed) persist()
    }

    fun archiveTask(id: String) = updateTask(id) { task ->
        if (task.status != TaskStatus.DONE) task
        else task.copy(archivedAtMillis = System.currentTimeMillis())
    }

    fun unarchiveTask(id: String) = updateTask(id) { task ->
        task.copy(archivedAtMillis = 0L)
    }

    /**
     * Manual/release/idea projects are hard-deleted. A weekly occurrence is retained as a hidden
     * tombstone (SKIPPED + archivedAtMillis=-1) so the weekly engine cannot immediately recreate
     * the same occurrence after the creator deliberately deletes it.
     */
    fun deleteTasks(ids: Set<String>) {
        if (ids.isEmpty()) return
        ids.forEach(::cancelTaskAlerts)
        val now = System.currentTimeMillis()
        val hardDelete = mutableSetOf<String>()
        tasks.indices.forEach { index ->
            val task = tasks[index]
            if (task.id !in ids) return@forEach
            if (task.origin == CreatorTaskOrigin.WEEKLY && task.scheduleOccurrenceKey.isNotBlank()) {
                tasks[index] = task.copy(
                    status = TaskStatus.SKIPPED,
                    reminderEnabled = false,
                    reminderAtMillis = 0L,
                    smartEscalationEnabled = false,
                    voiceEnabled = false,
                    reminderMode = ReminderMode.NONE,
                    workingUntilMillis = 0L,
                    autoStageReminder = false,
                    archivedAtMillis = -1L,
                )
            } else {
                hardDelete += task.id
            }
        }
        if (hardDelete.isNotEmpty()) tasks.removeAll { it.id in hardDelete }
        var ideasChanged = false
        ideas.indices.forEach { index ->
            if (ideas[index].projectTaskId in ids) {
                ideas[index] = ideas[index].copy(projectTaskId = "", updatedAtMillis = now)
                ideasChanged = true
            }
        }
        persist()
        if (ideasChanged) persistIdeas()
    }

    fun deleteTask(id: String) = deleteTasks(setOf(id))

'''
patch(vm, marker, methods + marker)

# 2) Shell routes Plan / Studio / Reminder Center to the management-aware v1.3.1 surfaces.
ui = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
patch(
    ui,
    '                PTab.PLAN -> PPlanScreen(vm.tasks, { openComposer() }, vm::startTask, vm::completeTask)\n',
    '''                PTab.PLAN -> V131PlanScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onStart = vm::startTask,
                    onDone = vm::completeTask,
                    onDeleteSelected = vm::deleteTasks,
                )
'''
)
patch(
    ui,
    '''                PTab.STUDIO -> PStudioScreen(
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onAdvance = vm::advanceWorkflow,
                    onBack = vm::moveWorkflowBack,
                    onFocus = { focusTaskId = it },
                    externalExpandId = externalStudioId,
                    externalExpandNonce = externalStudioNonce,
                )
''',
    '''                PTab.STUDIO -> V131StudioScreen(
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
'''
)
patch(
    ui,
    '''        PReminderCenter(
            tasks = vm.tasks,
            onDismiss = { showReminders = false },
            onNew = { showReminders = false; openComposer() },
            onEdit = { id -> showReminders = false; openComposer(id) },
        )
''',
    '''        V131ReminderCenter(
            tasks = vm.tasks,
            onDismiss = { showReminders = false },
            onNew = { showReminders = false; openComposer() },
            onEdit = { id -> showReminders = false; openComposer(id) },
            onDeleteReminders = vm::cancelReminders,
        )
'''
)

# Preserve accepted Studio accordion by reusing its exact project component from the new shell.
patch(ui, 'private fun PStudioProject(\n', 'internal fun PStudioProject(\n')

# Netflix-like cinema wall sits at the top of Today. Until the creator supplies the ten frames,
# the component intentionally renders a premium placeholder instead of fetching random imagery.
patch(
    ui,
    '''            PTopBar("TODAY", onAdd)
            Spacer(Modifier.height(18.dp))
            Text("Make the next thing.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
''',
    '''            PTopBar("TODAY", onAdd)
            Spacer(Modifier.height(14.dp))
            V131HomeHeroSlideshow()
            Spacer(Modifier.height(22.dp))
            Text("Make the next thing.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
'''
)

# 3) Main launcher uses the three-second cinematic gate. Widget/deep-link launches are skipped by the gate itself.
main = "app/src/main/java/com/framebynavin/app/MainActivity.kt"
patch(main, 'import com.framebynavin.app.ui.FrameByNavinV101BApp\n', 'import com.framebynavin.app.ui.V131LaunchGate\n')
patch(main, '                FrameByNavinV101BApp(externalLaunch = externalLaunch)\n', '                V131LaunchGate(externalLaunch = externalLaunch)\n')

# 4) Launcher icon registration.
manifest = "app/src/main/AndroidManifest.xml"
patch(
    manifest,
    '''    <application
        android:allowBackup="true"
        android:label="FrameByNavin"
''',
    '''    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_framebynavin_launcher"
        android:roundIcon="@drawable/ic_framebynavin_launcher"
        android:label="FrameByNavin"
'''
)

# 5) Version bump and CI artifact label.
gradle = "app/build.gradle.kts"
patch(gradle, '        versionCode = 21\n        versionName = "1.3.0-cloud-sync"\n', '        versionCode = 22\n        versionName = "1.3.1-cinematic-management"\n')
workflow = ".github/workflows/android-apk.yml"
patch(workflow, '          name: FrameByNavin-v1.3.0-cloud-sync\n', '          name: FrameByNavin-v1.3.1-cinematic-management\n')

print("v1.3.1 integration complete")
