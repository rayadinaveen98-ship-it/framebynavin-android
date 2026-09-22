from pathlib import Path

p = Path('app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt')
text = p.read_text()


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 match, found {count}')
    text = text.replace(old, new, 1)


replace_once(
    'import com.framebynavin.app.reminders.ReminderConstants\n',
    'import com.framebynavin.app.reminders.IdeaReminderScheduler\nimport com.framebynavin.app.reminders.ReminderConstants\n',
    'scheduler import',
)
replace_once(
    '    private val ideaStore = IdeaVaultStore(application)\n',
    '    private val ideaStore = IdeaVaultStore(application)\n    private val ideaReminderScheduler = IdeaReminderScheduler(application)\n',
    'scheduler field',
)
replace_once(
    '''        val normalized = idea.copy(
            id = idea.id.ifBlank { UUID.randomUUID().toString() },
            title = idea.title.trim(),
            topic = idea.topic.trim(),
            notes = idea.notes.trim(),
            updatedAtMillis = now,
            createdAtMillis = idea.createdAtMillis.takeIf { it > 0L } ?: now,
        )''',
    '''        val normalized = IdeaReminderPolicy.normalize(
            idea.copy(
                id = idea.id.ifBlank { UUID.randomUUID().toString() },
                title = idea.title.trim(),
                topic = idea.topic.trim(),
                notes = idea.notes.trim(),
                updatedAtMillis = now,
                createdAtMillis = idea.createdAtMillis.takeIf { it > 0L } ?: now,
            ),
            now = now,
        )''',
    'save normalization',
)
replace_once(
    '        ideas[index] = ideas[index].copy(status = IdeaStatus.ARCHIVED, updatedAtMillis = System.currentTimeMillis())\n',
    '''        ideas[index] = ideas[index].copy(
            status = IdeaStatus.ARCHIVED,
            reminderAtMillis = 0L,
            reminderCadence = IdeaReminderCadence.ONCE,
            updatedAtMillis = System.currentTimeMillis(),
        )
''',
    'archive cancellation state',
)
replace_once(
    '''        ideas[ideaIndex] = idea.copy(
            status = IdeaStatus.CONVERTED,
            projectTaskId = taskId,
            platformHint = platform,
            formatHint = contentType,
            updatedAtMillis = now,
        )''',
    '''        ideas[ideaIndex] = idea.copy(
            status = IdeaStatus.CONVERTED,
            projectTaskId = taskId,
            platformHint = platform,
            formatHint = contentType,
            reminderAtMillis = 0L,
            reminderCadence = IdeaReminderCadence.ONCE,
            updatedAtMillis = now,
        )''',
    'conversion cancellation state',
)
replace_once(
    '        enqueueWrite { epoch -> ideaStore.applyDelta(base, desired, epoch) }\n',
    '''        enqueueWrite { epoch ->
            ideaStore.applyDelta(base, desired, epoch)
            ideaReminderScheduler.reconcile()
        }
''',
    'persist reconciliation',
)

p.write_text(text)
