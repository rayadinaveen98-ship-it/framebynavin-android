from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exact patch target once, found {count}")
    p.write_text(text.replace(old, new, 1))
    print(f"patched {path}")


replace_once(
    "app/src/main/java/com/framebynavin/app/reminders/SmartEscalationScheduler.kt",
    '''    private fun finalStage(priority: TaskPriority): Stage = when (priority) {\n        TaskPriority.NORMAL -> Stage.SOFT\n        TaskPriority.IMPORTANT -> Stage.ALARM\n        TaskPriority.CRITICAL -> Stage.CRITICAL\n    }\n\n    private fun scheduleBounded(task: CreatorTask, preferredStage: Stage, candidateAtMillis: Long) {\n        val now = System.currentTimeMillis()\n        val target = task.reminderAtMillis\n        if (target <= now) return\n        if (candidateAtMillis <= target) {\n            scheduleStage(task, preferredStage, candidateAtMillis)\n        } else {\n            scheduleStage(task, finalStage(task.priority), target)\n        }\n    }''',
    '''    private fun scheduleBounded(task: CreatorTask, preferredStage: Stage, candidateAtMillis: Long) {\n        val now = System.currentTimeMillis()\n        val target = task.reminderAtMillis\n        if (target <= now) return\n        val bounded = SmartTargetBoundary.bound(task.priority, preferredStage, candidateAtMillis, target) ?: return\n        scheduleStage(task, bounded.stage, bounded.atMillis)\n    }''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/reminders/SmartEscalationScheduler.kt",
    '''        val bounded = resumeAtMillis.coerceAtMost(task.reminderAtMillis)\n        val scheduledStage = if (resumeAtMillis > task.reminderAtMillis) finalStage(task.priority) else stage\n        sessions.markSnoozed(task.id, scheduledStage, bounded)\n        scheduleStage(task, scheduledStage, bounded)''',
    '''        val bounded = SmartTargetBoundary.bound(task.priority, stage, resumeAtMillis, task.reminderAtMillis) ?: return\n        sessions.markSnoozed(task.id, bounded.stage, bounded.atMillis)\n        scheduleStage(task, bounded.stage, bounded.atMillis)''',
)

print("RC3 Smart boundary patch complete")
