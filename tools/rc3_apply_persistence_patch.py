from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exact patch target once, found {count}")
    p.write_text(text.replace(old, new, 1))
    print(f"patched {path}")


# 1) RC3 Custom stage management must survive TaskStore decode / app restart.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/TaskStore.kt",
    '''                val pulseManagedReminder = item.optBoolean("pulseManagedReminder", false) &&\n                    attentionPlan != ProjectAttentionPlan.CUSTOM && attentionPlan != ProjectAttentionPlan.OFF''',
    '''                val pulseManagedReminder = item.optBoolean("pulseManagedReminder", false) &&\n                    attentionPlan != ProjectAttentionPlan.OFF''',
)

# 2) New and edited Custom projects enter the same Project Pulse lifecycle as every other plan.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '''            val task = if (attentionPlan == ProjectAttentionPlan.CUSTOM) {\n                baseTask.copy(attentionPlan = if (enabled) ProjectAttentionPlan.CUSTOM else ProjectAttentionPlan.OFF)\n            } else ProjectPulseEngine.applyAttentionPlan(baseTask, attentionPlan)''',
    '''            val task = ProjectPulseEngine.applyAttentionPlan(baseTask, attentionPlan)''',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '''        val updated = if (attentionPlan == ProjectAttentionPlan.CUSTOM) configured\n        else ProjectPulseEngine.applyAttentionPlan(configured, attentionPlan)''',
    '''        val updated = ProjectPulseEngine.applyAttentionPlan(configured, attentionPlan)''',
)

# The legacy setReminder entry point also creates a stage-aware Custom reminder in RC3.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '''        scheduleTask(updated)\n        updated\n    }\n\n    fun cancelReminder(id: String)''',
    '''        val managed = ProjectPulseEngine.applyAttentionPlan(updated, ProjectAttentionPlan.CUSTOM)\n        scheduleTask(managed)\n        managed\n    }\n\n    fun cancelReminder(id: String)''',
)

# 3) Preserve Custom delivery choice while clearing the previous stage's check-in.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/ProjectPulse.kt",
    '''        // Custom deliberately asks the creator for the next-stage time instead of inventing one.\n        if (advanced.attentionPlan == ProjectAttentionPlan.CUSTOM) {\n            return clearReminder(advanced).copy(attentionPlan = ProjectAttentionPlan.CUSTOM, pulseManagedReminder = true)\n        }\n        return refreshManagedReminder(advanced, nowMillis)\n    }\n\n    fun needsCustomNextStagePrompt''',
    '''        // Custom deliberately asks the creator for the next-stage time instead of inventing one.\n        if (advanced.attentionPlan == ProjectAttentionPlan.CUSTOM) {\n            val customMode = managed.reminderMode.takeIf { it != ReminderMode.NONE } ?: ReminderMode.SIMPLE\n            return clearReminder(advanced).copy(\n                attentionPlan = ProjectAttentionPlan.CUSTOM,\n                pulseManagedReminder = true,\n                reminderMode = customMode,\n                alertType = managed.alertType,\n            )\n        }\n        return refreshManagedReminder(advanced, nowMillis)\n    }\n\n    /** Keep manual Studio stage changes on the same Project Pulse lifecycle as reminder Stage Done. */\n    fun afterWorkflowStageChanged(\n        before: CreatorTask,\n        after: CreatorTask,\n        nowMillis: Long = System.currentTimeMillis(),\n    ): CreatorTask {\n        if (after.status == TaskStatus.DONE || after.status == TaskStatus.SKIPPED) return after\n        if (!isStageCheckIn(before)) return after\n        val managed = ensureStageManaged(after.copy(attentionPlan = before.attentionPlan))\n        val changed = CreatorWorkflowEngine.stageIndex(before) != CreatorWorkflowEngine.stageIndex(after)\n        if (changed && before.attentionPlan == ProjectAttentionPlan.CUSTOM) {\n            val customMode = before.reminderMode.takeIf { it != ReminderMode.NONE } ?: ReminderMode.SIMPLE\n            return clearReminder(managed).copy(\n                attentionPlan = ProjectAttentionPlan.CUSTOM,\n                pulseManagedReminder = true,\n                reminderMode = customMode,\n                alertType = before.alertType,\n            )\n        }\n        return refreshManagedReminder(managed, nowMillis)\n    }\n\n    fun needsCustomNextStagePrompt''',
)

# 4) Manual forward movement must not rebind a previous-stage Custom checkpoint to the next stage.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '''            val updated = CreatorPublicationEngine.advance(task, now)\n            if (updated.status == TaskStatus.DONE) {\n                cancelTaskAlerts(task.id)\n                updated\n            } else {\n                val nextIndex = CreatorWorkflowEngine.stageIndex(updated)\n                val scheduled = if (updated.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(updated)\n                else applyAutoStageReminder(updated, nextIndex)''',
    '''            val advanced = CreatorPublicationEngine.advance(task, now)\n            val updated = ProjectPulseEngine.afterWorkflowStageChanged(task, advanced, now)\n            if (updated.status == TaskStatus.DONE) {\n                cancelTaskAlerts(task.id)\n                updated\n            } else {\n                val nextIndex = CreatorWorkflowEngine.stageIndex(updated)\n                val scheduled = if (updated.pulseManagedReminder) updated\n                else applyAutoStageReminder(updated, nextIndex)''',
)

# 5) Moving backward also invalidates the previous stage's Custom checkpoint.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '''    fun moveWorkflowBack(id: String) = updateTask(id) { task ->\n        if (task.status == TaskStatus.DONE) return@updateTask task\n        val template = CreatorWorkflowEngine.templateFor(task)\n        val currentIndex = CreatorWorkflowEngine.stageIndex(task)\n        val previous = (currentIndex - 1).coerceAtLeast(0)\n        var updated = task.copy(\n            status = TaskStatus.WORKING,\n            workflowStageIndex = previous,\n            progress = CreatorWorkflowEngine.progressForStage(previous, template.stages.size),\n            workingUntilMillis = 0L,\n        )\n        updated = if (updated.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(updated)\n        else applyAutoStageReminder(updated, previous)''',
    '''    fun moveWorkflowBack(id: String) = updateTask(id) { task ->\n        if (task.status == TaskStatus.DONE) return@updateTask task\n        val now = System.currentTimeMillis()\n        val template = CreatorWorkflowEngine.templateFor(task)\n        val currentIndex = CreatorWorkflowEngine.stageIndex(task)\n        val previous = (currentIndex - 1).coerceAtLeast(0)\n        val moved = task.copy(\n            status = TaskStatus.WORKING,\n            workflowStageIndex = previous,\n            progress = CreatorWorkflowEngine.progressForStage(previous, template.stages.size),\n            workingUntilMillis = 0L,\n        )\n        var updated = if (ProjectPulseEngine.isStageCheckIn(task))\n            ProjectPulseEngine.afterWorkflowStageChanged(task, moved, now)\n        else applyAutoStageReminder(moved, previous)''',
)

print("RC3 persistence/manual-stage patch complete")
