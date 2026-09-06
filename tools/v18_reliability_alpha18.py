from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
TASK_STORE = ROOT / 'app/src/main/java/com/framebynavin/app/data/TaskStore.kt'
NOTIFICATIONS = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/ReminderNotifications.kt'
MISSED = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/MissedReminderRecovery.kt'
REGULAR_RECEIVER = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/ReminderReceiver.kt'
SMART_RECEIVER = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/EscalationReceiver.kt'
SMART_SCHEDULER = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/SmartEscalationScheduler.kt'
ACTION_RECEIVER = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/ReminderActionReceiver.kt'
ALARM = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/AlarmActivity.kt'
VOICE = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/VoiceReminderActivity.kt'
RECOVERY_RECEIVER = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/ReminderRecoveryReceiver.kt'
MAIN = ROOT / 'app/src/main/java/com/framebynavin/app/MainActivity.kt'
VIEWMODEL = ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt'
POLICY = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/ReminderDeliveryPolicy.kt'
TEST = ROOT / 'app/src/test/java/com/framebynavin/app/reminders/ReminderReliabilityAlpha18Test.kt'
ANDROID_TEST = ROOT / 'app/src/androidTest/java/com/framebynavin/app/data/V18ReliabilityAlpha18InstrumentationTest.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)

# Version
build = BUILD.read_text()
build = replace_once(build, 'versionCode = 57', 'versionCode = 58', 'versionCode')
build = replace_once(build, 'versionName = "1.8.0-product-alpha17"', 'versionName = "1.8.0-product-alpha18"', 'versionName')
BUILD.write_text(build)

# Process-wide serialization for TaskStore read-modify-write operations plus a last-known-good backup.
task_store = TASK_STORE.read_text()
task_store = replace_once(
    task_store,
    'import kotlinx.coroutines.flow.map\n',
    'import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.sync.Mutex\nimport kotlinx.coroutines.sync.withLock\n',
    'TaskStore mutex imports',
)
task_store = replace_once(
    task_store,
    'private val Context.creatorDataStore by preferencesDataStore(name = "creator_v0")\n',
    'private val Context.creatorDataStore by preferencesDataStore(name = "creator_v0")\nprivate val creatorTaskMutationMutex = Mutex()\n',
    'TaskStore shared mutex',
)
task_store = replace_once(
    task_store,
    '    private val tasksKey = stringPreferencesKey("tasks_json")\n',
    '    private val tasksKey = stringPreferencesKey("tasks_json")\n    private val tasksBackupKey = stringPreferencesKey("tasks_json_last_good")\n',
    'TaskStore backup key',
)
task_store = replace_once(
    task_store,
    '''    val tasksFlow: Flow<List<CreatorTask>> = context.creatorDataStore.data.map { prefs ->\n        val raw = prefs[tasksKey] ?: return@map emptyList()\n        runCatching { decode(raw) }.getOrDefault(emptyList())\n    }\n''',
    '''    val tasksFlow: Flow<List<CreatorTask>> = context.creatorDataStore.data.map { prefs ->\n        val raw = prefs[tasksKey] ?: return@map emptyList()\n        runCatching { decode(raw) }.getOrElse {\n            val backup = prefs[tasksBackupKey] ?: return@getOrElse emptyList()\n            runCatching { decode(backup) }.getOrDefault(emptyList())\n        }\n    }\n''',
    'TaskStore last-good fallback',
)
task_store = replace_once(
    task_store,
    '''    suspend fun save(tasks: List<CreatorTask>) {\n        context.creatorDataStore.edit { prefs -> prefs[tasksKey] = encode(tasks) }\n        CreatorWidgetUpdater.updateAll(context, tasks)\n    }\n''',
    '''    suspend fun save(tasks: List<CreatorTask>) = creatorTaskMutationMutex.withLock {\n        saveUnlocked(tasks)\n    }\n\n    private suspend fun saveUnlocked(tasks: List<CreatorTask>) {\n        val encoded = encode(tasks)\n        context.creatorDataStore.edit { prefs ->\n            val current = prefs[tasksKey]\n            if (current != null && runCatching { decode(current) }.isSuccess) {\n                prefs[tasksBackupKey] = current\n            } else if (prefs[tasksBackupKey] == null) {\n                prefs[tasksBackupKey] = encoded\n            }\n            prefs[tasksKey] = encoded\n        }\n        CreatorWidgetUpdater.updateAll(context, tasks)\n    }\n''',
    'TaskStore serialized save',
)
task_store = replace_once(
    task_store,
    '''    suspend fun updateTask(id: String, transform: (CreatorTask) -> CreatorTask): CreatorTask? {\n        val current = load().toMutableList()\n        val index = current.indexOfFirst { it.id == id }\n        if (index == -1) return null\n        val updated = transform(current[index])\n        current[index] = updated\n        save(current)\n        return updated\n    }\n''',
    '''    suspend fun updateTask(id: String, transform: (CreatorTask) -> CreatorTask): CreatorTask? = creatorTaskMutationMutex.withLock {\n        val current = load().toMutableList()\n        val index = current.indexOfFirst { it.id == id }\n        if (index == -1) return@withLock null\n        val updated = transform(current[index])\n        current[index] = updated\n        saveUnlocked(current)\n        updated\n    }\n''',
    'TaskStore atomic updateTask',
)
task_store = replace_once(
    task_store,
    '''    private fun decode(raw: String): List<CreatorTask> {\n        val array = JSONArray(raw)\n        return buildList {\n''',
    '''    private fun decode(raw: String): List<CreatorTask> {\n        val array = JSONArray(raw)\n        val seenIds = mutableSetOf<String>()\n        return buildList {\n''',
    'TaskStore duplicate id set',
)
task_store = replace_once(
    task_store,
    '''                require(id.isNotBlank()) { "Task $i has no id" }\n                require(title.isNotBlank()) { "Task $i has no title" }\n''',
    '''                require(id.isNotBlank()) { "Task $i has no id" }\n                require(title.isNotBlank()) { "Task $i has no title" }\n                require(seenIds.add(id)) { "Duplicate task id: $id" }\n''',
    'TaskStore duplicate id guard',
)
TASK_STORE.write_text(task_store)

POLICY.write_text(r'''package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus

enum class SmartDeliveryDecision { DELIVER, DEFER, DROP }

/** Pure reliability rules shared by AlarmManager delivery and recovery paths. */
object ReminderDeliveryPolicy {
    fun canDeliverRegular(task: CreatorTask?, scheduledAtMillis: Long): Boolean {
        if (task == null || scheduledAtMillis <= 0L) return false
        return isActive(task) &&
            task.reminderMode != ReminderMode.SMART &&
            !task.smartEscalationEnabled &&
            task.reminderAtMillis == scheduledAtMillis
    }

    fun smartDecision(
        task: CreatorTask?,
        targetAtMillis: Long,
        stage: SmartEscalationScheduler.Stage,
        nowMillis: Long,
    ): SmartDeliveryDecision {
        if (task == null || targetAtMillis <= 0L) return SmartDeliveryDecision.DROP
        if (!isActive(task)) return SmartDeliveryDecision.DROP
        if (task.reminderMode != ReminderMode.SMART && !task.smartEscalationEnabled) return SmartDeliveryDecision.DROP
        if (task.reminderAtMillis != targetAtMillis) return SmartDeliveryDecision.DROP
        if (task.dueAtMillis > 0L && task.reminderAtMillis > task.dueAtMillis) return SmartDeliveryDecision.DROP
        if (!stageAllowed(task.priority, stage)) return SmartDeliveryDecision.DROP
        if (task.workingUntilMillis > nowMillis) return SmartDeliveryDecision.DEFER
        return SmartDeliveryDecision.DELIVER
    }

    fun smartResumeAt(
        plannedAtMillis: Long,
        workingUntilMillis: Long,
        finalTargetAtMillis: Long,
        nowMillis: Long,
    ): Long? {
        val candidate = maxOf(plannedAtMillis, workingUntilMillis)
        return candidate.takeIf { it > nowMillis && it <= finalTargetAtMillis }
    }

    private fun isActive(task: CreatorTask): Boolean =
        task.reminderEnabled &&
            task.reminderMode != ReminderMode.NONE &&
            task.status != TaskStatus.DONE &&
            task.status != TaskStatus.SKIPPED

    private fun stageAllowed(priority: TaskPriority, stage: SmartEscalationScheduler.Stage): Boolean = when (priority) {
        TaskPriority.NORMAL -> stage == SmartEscalationScheduler.Stage.SOFT
        TaskPriority.IMPORTANT -> stage != SmartEscalationScheduler.Stage.CRITICAL
        TaskPriority.CRITICAL -> true
    }
}
''')

# Notification posting now reports whether Android can actually show the notification.
notifications = NOTIFICATIONS.read_text()
notifications = replace_once(
    notifications,
    'import android.app.NotificationChannel\n',
    'import android.Manifest\nimport android.app.NotificationChannel\n',
    'notification Manifest import',
)
notifications = replace_once(
    notifications,
    'import android.content.Intent\n',
    'import android.content.Intent\nimport android.content.pm.PackageManager\n',
    'notification PackageManager import',
)
notifications = replace_once(
    notifications,
    'import androidx.core.app.NotificationCompat\n',
    'import androidx.core.app.NotificationCompat\nimport androidx.core.app.NotificationManagerCompat\nimport androidx.core.content.ContextCompat\n',
    'notification compat imports',
)
notifications = replace_once(
    notifications,
    '''    fun show(\n        context: Context,\n        task: CreatorTask,\n        deliveryDelayMillis: Long? = null,\n        stageLabel: String? = null,\n    ) {\n        ensureChannel(context)\n''',
    '''    fun canPost(context: Context): Boolean {\n        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||\n            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED\n        if (!runtimeGranted || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\n            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(ReminderConstants.CHANNEL_ID)\n            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false\n        }\n        return true\n    }\n\n    fun show(\n        context: Context,\n        task: CreatorTask,\n        deliveryDelayMillis: Long? = null,\n        stageLabel: String? = null,\n    ): Boolean {\n        ensureChannel(context)\n        if (!canPost(context)) return false\n''',
    'notification capability return',
)
notifications = replace_once(
    notifications,
    '        manager.notify(task.id.hashCode(), builder.build())\n    }\n',
    '        manager.notify(task.id.hashCode(), builder.build())\n        return true\n    }\n',
    'notification true return',
)
NOTIFICATIONS.write_text(notifications)

# Missed-reminder catch-up uses the same permission/channel capability check.
missed = MISSED.read_text()
for unused_import in [
    'import android.Manifest\n',
    'import android.content.pm.PackageManager\n',
    'import android.os.Build\n',
    'import androidx.core.app.NotificationManagerCompat\n',
    'import androidx.core.content.ContextCompat\n',
]:
    missed = missed.replace(unused_import, '')
missed = replace_once(
    missed,
    '        if (!canPostNotifications(appContext)) return false\n',
    '        if (!ReminderNotifications.canPost(appContext)) return false\n',
    'missed reminder notification capability',
)
start = missed.find('    private fun canPostNotifications(context: Context): Boolean {')
if start == -1:
    raise SystemExit('missed reminder helper start not found')
end = missed.find('\n    }', start)
if end == -1:
    raise SystemExit('missed reminder helper end not found')
missed = missed[:start] + missed[end + len('\n    }'):]
missed = replace_once(
    missed,
    '''        ReminderNotifications.show(
            context = appContext,
            task = task,
            deliveryDelayMillis = nowMillis - task.reminderAtMillis,
            stageLabel = "Recovered $recoveredMode · ${lateMinutes}m late",
        )
        ledger.markDelivered(task.id, task.reminderAtMillis)
        return true
''',
    '''        val shown = runCatching {
            ReminderNotifications.show(
                context = appContext,
                task = task,
                deliveryDelayMillis = nowMillis - task.reminderAtMillis,
                stageLabel = "Recovered $recoveredMode · ${lateMinutes}m late",
            )
        }.getOrDefault(false)
        if (!shown) return false
        ledger.markDelivered(task.id, task.reminderAtMillis)
        return true
''',
    'missed reminder confirmed delivery',
)
MISSED.write_text(missed)

# Regular one-shot delivery re-reads authoritative storage before consuming the alarm ledger.
REGULAR_RECEIVER.write_text(r'''package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L)
        val ledger = AlarmLedger(appContext)
        if (scheduledAt <= 0L || ledger.scheduledAt(taskId) != scheduledAt) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = runCatching { TaskStore(appContext).load().firstOrNull { it.id == taskId } }.getOrNull()
                if (!ReminderDeliveryPolicy.canDeliverRegular(task, scheduledAt)) {
                    ledger.consumeIfCurrent(taskId, scheduledAt)
                    return@launch
                }
                if (!ledger.consumeIfCurrent(taskId, scheduledAt)) return@launch
                deliver(appContext, intent, task!!, scheduledAt, ledger)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun deliver(
        appContext: Context,
        intent: Intent,
        task: CreatorTask,
        scheduledAt: Long,
        ledger: AlarmLedger,
    ) {
        val firedAt = System.currentTimeMillis()
        val exactDelivery = intent.getBooleanExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, false)
        val delayMillis = (firedAt - scheduledAt).coerceAtLeast(0L)
        val mode = task.reminderMode

        fun notificationFallback(label: String? = null): Boolean = runCatching {
            ReminderNotifications.show(
                context = appContext,
                task = task,
                deliveryDelayMillis = delayMillis,
                stageLabel = label,
            )
        }.getOrDefault(false)

        val delivered = when (mode) {
            ReminderMode.VOICE -> {
                if (!exactDelivery) {
                    notificationFallback("Voice reminder · precise timing unavailable")
                } else {
                    runCatching { VoiceReminderService.start(appContext, task) }.isSuccess ||
                        notificationFallback("Voice reminder fallback")
                }
            }
            ReminderMode.ALARM -> {
                if (!exactDelivery) {
                    notificationFallback("Alarm reminder · precise timing unavailable")
                } else {
                    runCatching { AlarmRingingService.start(appContext, task) }.isSuccess ||
                        notificationFallback("Alarm reminder fallback")
                }
            }
            ReminderMode.NONE, ReminderMode.SMART -> false
            else -> notificationFallback()
        }

        if (delivered) ledger.markDelivered(task.id, scheduledAt)
    }
}
''')

# Smart stage delivery uses the same authoritative task snapshot and defers during the Working quiet window.
SMART_RECEIVER.write_text(r'''package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EscalationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val taskId = intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L)
        val targetAt = intent.getLongExtra(ReminderConstants.EXTRA_TARGET_AT, 0L)
        val stage = runCatching {
            SmartEscalationScheduler.Stage.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_ESCALATION_STAGE).orEmpty())
        }.getOrNull() ?: return
        val ledgerKey = "$taskId#${stage.name}"
        val ledger = AlarmLedger(appContext)
        if (scheduledAt <= 0L || ledger.scheduledAt(ledgerKey) != scheduledAt) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = runCatching { TaskStore(appContext).load().firstOrNull { it.id == taskId } }.getOrNull()
                val now = System.currentTimeMillis()
                when (ReminderDeliveryPolicy.smartDecision(task, targetAt, stage, now)) {
                    SmartDeliveryDecision.DROP -> {
                        ledger.consumeIfCurrent(ledgerKey, scheduledAt)
                        SmartEscalationScheduler(appContext).cancel(taskId)
                    }
                    SmartDeliveryDecision.DEFER -> {
                        if (ledger.consumeIfCurrent(ledgerKey, scheduledAt) && task != null) {
                            SmartEscalationScheduler(appContext).recover(task)
                        }
                    }
                    SmartDeliveryDecision.DELIVER -> {
                        if (ledger.consumeIfCurrent(ledgerKey, scheduledAt) && task != null) {
                            deliverStage(appContext, intent, task, stage, now)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun deliverStage(
        appContext: Context,
        intent: Intent,
        sourceTask: CreatorTask,
        stage: SmartEscalationScheduler.Stage,
        firedAt: Long,
    ) {
        val task = sourceTask.copy(
            priority = if (stage == SmartEscalationScheduler.Stage.CRITICAL) TaskPriority.CRITICAL else sourceTask.priority,
            smartEscalationEnabled = true,
            voiceEnabled = true,
        )
        val smart = SmartEscalationScheduler(appContext)
        smart.markStageActive(task.id, stage, firedAt)
        val exactDelivery = intent.getBooleanExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, false)

        fun fallback(label: String): Boolean = runCatching {
            ReminderNotifications.show(appContext, task, stageLabel = label)
        }.getOrDefault(false)

        when (stage) {
            SmartEscalationScheduler.Stage.SOFT -> {
                AlarmRingingService.stop(appContext)
                VoiceReminderService.stop(appContext)
                fallback("Smart · Gentle")
            }
            SmartEscalationScheduler.Stage.VOICE -> {
                ReminderSurfaceRegistry.closeAll()
                AlarmRingingService.stop(appContext)
                ReminderNotifications.cancel(appContext, task.id)
                if (!exactDelivery || !runCatching { VoiceReminderService.start(appContext, task) }.isSuccess) {
                    fallback("Smart · Voice fallback")
                }
            }
            SmartEscalationScheduler.Stage.ALARM -> {
                ReminderSurfaceRegistry.closeAll()
                VoiceReminderService.stop(appContext)
                ReminderNotifications.cancel(appContext, task.id)
                if (!exactDelivery || !runCatching {
                        AlarmRingingService.start(appContext, task.copy(voiceEnabled = false), stage)
                    }.isSuccess) {
                    fallback("Smart · Alarm fallback")
                }
            }
            SmartEscalationScheduler.Stage.CRITICAL -> {
                ReminderSurfaceRegistry.closeAll()
                VoiceReminderService.stop(appContext)
                AlarmRingingService.stop(appContext)
                ReminderNotifications.cancel(appContext, task.id)
                if (!exactDelivery || !runCatching {
                        AlarmRingingService.start(appContext, task.copy(priority = TaskPriority.CRITICAL, voiceEnabled = true), stage)
                    }.isSuccess) {
                    fallback("Smart · Critical fallback")
                }
            }
        }

        smart.scheduleNextIfUnanswered(task, stage, firedAt)
    }
}
''')

# Smart scheduling/recovery honors the explicit Working quiet window instead of restarting early.
smart_scheduler = SMART_SCHEDULER.read_text()
smart_scheduler = replace_once(
    smart_scheduler,
    '''        val firstAt = SmartEscalationPolicy.firstStageAtMillis(task.priority, task.reminderAtMillis, config)\n        if (firstAt <= now) return\n        scheduleStage(task, Stage.SOFT, firstAt)\n''',
    '''        val firstAt = SmartEscalationPolicy.firstStageAtMillis(task.priority, task.reminderAtMillis, config)\n        val eligibleAt = ReminderDeliveryPolicy.smartResumeAt(\n            plannedAtMillis = firstAt,\n            workingUntilMillis = task.workingUntilMillis,\n            finalTargetAtMillis = task.reminderAtMillis,\n            nowMillis = now,\n        ) ?: return\n        scheduleStage(task, Stage.SOFT, eligibleAt)\n''',
    'Smart schedule working quiet',
)
smart_scheduler = replace_once(
    smart_scheduler,
    '''        val now = System.currentTimeMillis()\n        val session = sessions.current(task.id)\n''',
    '''        val now = System.currentTimeMillis()\n        if (task.workingUntilMillis > now) {\n            val config = configStore.get(task)\n            val firstAt = SmartEscalationPolicy.firstStageAtMillis(task.priority, task.reminderAtMillis, config)\n            val resumeAt = ReminderDeliveryPolicy.smartResumeAt(\n                plannedAtMillis = firstAt,\n                workingUntilMillis = task.workingUntilMillis,\n                finalTargetAtMillis = task.reminderAtMillis,\n                nowMillis = now,\n            )\n            cancelPending(task.id, clearSession = true)\n            if (resumeAt != null) scheduleStage(task, Stage.SOFT, resumeAt)\n            return\n        }\n        val session = sessions.current(task.id)\n''',
    'Smart recover working quiet',
)
SMART_SCHEDULER.write_text(smart_scheduler)

# Notification/voice/alarm "Working" actions schedule the Smart resume immediately; Done gets a durable completion timestamp.
action = ACTION_RECEIVER.read_text()
action = replace_once(
    action,
    '''                        if (updated?.reminderEnabled == true && updated.reminderMode != ReminderMode.SMART) {\n                            scheduler.schedule(updated)\n                        }\n''',
    '''                        if (updated?.reminderEnabled == true) {\n                            if (updated.reminderMode == ReminderMode.SMART || updated.smartEscalationEnabled) smart.recover(updated)\n                            else scheduler.schedule(updated)\n                        }\n''',
    'action working Smart resume',
)
action = replace_once(
    action,
    '''                                reminderMode = ReminderMode.NONE,\n                                workingUntilMillis = 0L,\n''',
    '''                                reminderMode = ReminderMode.NONE,\n                                workingUntilMillis = 0L,\n                                completedAtMillis = task.completedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),\n''',
    'action completion timestamp',
)
ACTION_RECEIVER.write_text(action)

alarm = ALARM.read_text()
alarm = replace_once(
    alarm,
    '''                    reminderMode = ReminderMode.NONE,\n                    workingUntilMillis = 0L,\n''',
    '''                    reminderMode = ReminderMode.NONE,\n                    workingUntilMillis = 0L,\n                    completedAtMillis = task.completedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),\n''',
    'alarm completion timestamp',
)
alarm = replace_once(
    alarm,
    '            if (updated?.reminderEnabled == true && updated.reminderMode != ReminderMode.SMART) scheduler.schedule(updated)\n',
    '''            if (updated?.reminderEnabled == true) {\n                if (updated.reminderMode == ReminderMode.SMART || updated.smartEscalationEnabled) smartScheduler.recover(updated)\n                else scheduler.schedule(updated)\n            }\n''',
    'alarm working Smart resume',
)
ALARM.write_text(alarm)

voice = VOICE.read_text()
voice = replace_once(
    voice,
    '            if (updated?.reminderEnabled == true && updated.reminderMode != ReminderMode.SMART) scheduler.schedule(updated)\n',
    '''            if (updated?.reminderEnabled == true) {\n                if (updated.reminderMode == ReminderMode.SMART || updated.smartEscalationEnabled) smartScheduler.recover(updated)\n                else scheduler.schedule(updated)\n            }\n''',
    'voice working Smart resume',
)
voice = replace_once(
    voice,
    '''                    reminderMode = ReminderMode.NONE,\n                    workingUntilMillis = 0L,\n''',
    '''                    reminderMode = ReminderMode.NONE,\n                    workingUntilMillis = 0L,\n                    completedAtMillis = task.completedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),\n''',
    'voice completion timestamp',
)
VOICE.write_text(voice)

# App resume uses the full idempotent reconciliation path, not only missed-notification catch-up.
main = MAIN.read_text()
main = replace_once(main, 'import com.framebynavin.app.reminders.MissedReminderRecovery\n', 'import com.framebynavin.app.reminders.ReminderRecoveryEngine\n', 'MainActivity recovery import')
main = replace_once(
    main,
    '''        // Covers the case where Android missed a one-shot reminder and the creator opens the app\n        // later, including immediately after granting notification permission.\n        lifecycleScope.launch(Dispatchers.IO) {\n            MissedReminderRecovery.deliverMissedFromStore(applicationContext)\n        }\n''',
    '''        // Reassert future alarms, recover Smart state and catch up actionable missed reminders.\n        // The shared recovery engine is idempotent, so repeated resumes do not create duplicates.\n        lifecycleScope.launch(Dispatchers.IO) {\n            ReminderRecoveryEngine.reconcile(applicationContext)\n        }\n''',
    'MainActivity full resume reconcile',
)
MAIN.write_text(main)

# Boot/package/time recovery also guarantees the low-frequency health worker remains registered.
recovery = RECOVERY_RECEIVER.read_text()
recovery = replace_once(
    recovery,
    '''            try {\n                ReminderRecoveryEngine.reconcile(context.applicationContext)\n            } finally {\n''',
    '''            try {\n                val appContext = context.applicationContext\n                ReminderHealthScheduler.ensurePeriodic(appContext)\n                ReminderRecoveryEngine.reconcile(appContext)\n            } finally {\n''',
    'recovery worker re-registration',
)
RECOVERY_RECEIVER.write_text(recovery)

# Completing/deleting/editing a task also removes any visible reminder notification.
viewmodel = VIEWMODEL.read_text()
viewmodel = replace_once(
    viewmodel,
    'import com.framebynavin.app.reminders.ReminderRecoveryEngine\n',
    'import com.framebynavin.app.reminders.ReminderNotifications\nimport com.framebynavin.app.reminders.ReminderRecoveryEngine\n',
    'ViewModel notification import',
)
viewmodel = replace_once(
    viewmodel,
    '''    private fun cancelTaskAlerts(taskId: String) {\n        scheduler.cancel(taskId)\n        smartScheduler.cancel(taskId)\n    }\n''',
    '''    private fun cancelTaskAlerts(taskId: String) {\n        scheduler.cancel(taskId)\n        smartScheduler.cancel(taskId)\n        ReminderNotifications.cancel(getApplication<Application>(), taskId)\n    }\n''',
    'ViewModel notification cleanup',
)
VIEWMODEL.write_text(viewmodel)

TEST.parent.mkdir(parents=True, exist_ok=True)
TEST.write_text(r'''package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderReliabilityAlpha18Test {
    private val now = 1_000_000L

    @Test
    fun regularDelivery_requiresCurrentActiveReminderSnapshot() {
        val task = regularTask(reminderAt = now + 60_000L)
        assertTrue(ReminderDeliveryPolicy.canDeliverRegular(task, now + 60_000L))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task, now + 61_000L))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task.copy(status = TaskStatus.DONE), now + 60_000L))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task.copy(reminderEnabled = false), now + 60_000L))
    }

    @Test
    fun regularDelivery_rejectsSmartPendingIntentPath() {
        val task = regularTask(reminderAt = now + 60_000L).copy(
            reminderMode = ReminderMode.SMART,
            smartEscalationEnabled = true,
        )
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task, now + 60_000L))
    }

    @Test
    fun smartDelivery_defersDuringWorkingQuietWindow() {
        val task = smartTask(targetAt = now + 60 * 60_000L).copy(workingUntilMillis = now + 15 * 60_000L)
        assertEquals(
            SmartDeliveryDecision.DEFER,
            ReminderDeliveryPolicy.smartDecision(task, task.reminderAtMillis, SmartEscalationScheduler.Stage.SOFT, now),
        )
    }

    @Test
    fun smartDelivery_rejectsStaleTargetAndImpossibleStage() {
        val task = smartTask(targetAt = now + 60 * 60_000L).copy(priority = TaskPriority.IMPORTANT)
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(task, task.reminderAtMillis + 1L, SmartEscalationScheduler.Stage.SOFT, now),
        )
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(task, task.reminderAtMillis, SmartEscalationScheduler.Stage.CRITICAL, now),
        )
    }

    @Test
    fun smartResume_neverBreaksQuietWindowOrFinalTarget() {
        val target = now + 60 * 60_000L
        val planned = now + 30 * 60_000L
        val quietUntil = now + 40 * 60_000L
        assertEquals(
            quietUntil,
            ReminderDeliveryPolicy.smartResumeAt(planned, quietUntil, target, now),
        )
        assertNull(
            ReminderDeliveryPolicy.smartResumeAt(planned, target + 1L, target, now),
        )
    }

    private fun regularTask(reminderAt: Long) = CreatorTask(
        id = "regular",
        title = "Regular",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today",
        dueAtMillis = reminderAt + 60_000L,
        status = TaskStatus.PLANNED,
        reminderEnabled = true,
        reminderAtMillis = reminderAt,
        reminderMode = ReminderMode.SIMPLE,
    )

    private fun smartTask(targetAt: Long) = CreatorTask(
        id = "smart",
        title = "Smart",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today",
        dueAtMillis = targetAt + 60_000L,
        status = TaskStatus.PLANNED,
        reminderEnabled = true,
        reminderAtMillis = targetAt,
        reminderMode = ReminderMode.SMART,
        smartEscalationEnabled = true,
        priority = TaskPriority.CRITICAL,
    )
}
''')

ANDROID_TEST.parent.mkdir(parents=True, exist_ok=True)
ANDROID_TEST.write_text(r'''package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18ReliabilityAlpha18InstrumentationTest {
    @Test
    fun taskStore_serializesConcurrentReadModifyWrite() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val storeA = TaskStore(context)
        val storeB = TaskStore(context)
        storeA.save(listOf(task()))

        coroutineScope {
            launch(Dispatchers.Default) {
                storeA.updateTask("atomic") { it.copy(progress = 45) }
            }
            launch(Dispatchers.Default) {
                storeB.updateTask("atomic") { it.copy(notes = "second mutation survived") }
            }
        }

        val result = storeA.load().single { it.id == "atomic" }
        assertEquals(45, result.progress)
        assertEquals("second mutation survived", result.notes)
        storeA.save(emptyList())
    }

    @Test
    fun taskStore_rejectsDuplicateIdsOnImportValidation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val store = TaskStore(context)
        val raw = "[{\"id\":\"dup\",\"title\":\"A\"},{\"id\":\"dup\",\"title\":\"B\"}]"
        assertThrows(IllegalArgumentException::class.java) { store.validateJson(raw) }
    }

    private fun task() = CreatorTask(
        id = "atomic",
        title = "Atomic update",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today",
        status = TaskStatus.PLANNED,
    )
}
''')

print('Applied FrameByNavin v1.8 Alpha18 reliability and data-integrity pass')
