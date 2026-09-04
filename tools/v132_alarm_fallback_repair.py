from pathlib import Path

ROOT = Path('app/src/main/java/com/framebynavin/app/reminders')

# 1) Shared exact-delivery marker.
p = ROOT / 'ReminderConstants.kt'
s = p.read_text()
needle = '    const val EXTRA_ESCALATION_STAGE = "escalation_stage"\n'
if 'EXTRA_EXACT_DELIVERY' not in s:
    s = s.replace(needle, needle + '    const val EXTRA_EXACT_DELIVERY = "exact_delivery"\n')
p.write_text(s)

# 2) Normal scheduler records whether the PendingIntent is backed by an exact alarm.
p = ROOT / 'ReminderScheduler.kt'
s = p.read_text()
s = s.replace(
    '        val pendingIntent = alarmPendingIntent(task)\n        val isNativeAlarm',
    '        val exactDelivery = canScheduleExact()\n        val pendingIntent = alarmPendingIntent(task, exactDelivery)\n        val isNativeAlarm',
)
s = s.replace('                if (canScheduleExact()) {', '                if (exactDelivery) {', 1)
s = s.replace('            if (canScheduleExact()) {', '            if (exactDelivery) {', 1)
s = s.replace(
    '    private fun alarmPendingIntent(task: CreatorTask): PendingIntent {',
    '    private fun alarmPendingIntent(task: CreatorTask, exactDelivery: Boolean): PendingIntent {',
)
needle = '            .putExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, task.alarmTimeoutSeconds)\n'
if 'EXTRA_EXACT_DELIVERY, exactDelivery' not in s:
    s = s.replace(needle, needle + '            .putExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, exactDelivery)\n', 1)
p.write_text(s)

# 3) Smart scheduler records the same marker per stage.
p = ROOT / 'SmartEscalationScheduler.kt'
s = p.read_text()
s = s.replace(
    '        val pendingIntent = stagePendingIntent(task, stage, atMillis)\n        val key = ledgerKey(task.id, stage)',
    '        val exactDelivery = canScheduleExact()\n        val pendingIntent = stagePendingIntent(task, stage, atMillis, exactDelivery)\n        val key = ledgerKey(task.id, stage)',
)
s = s.replace(
    '            if ((stage == Stage.ALARM || stage == Stage.CRITICAL) && canScheduleExact()) {',
    '            if ((stage == Stage.ALARM || stage == Stage.CRITICAL) && exactDelivery) {',
)
s = s.replace('            } else if (canScheduleExact()) {', '            } else if (exactDelivery) {')
s = s.replace(
    '    private fun stagePendingIntent(task: CreatorTask, stage: Stage, atMillis: Long): PendingIntent {',
    '    private fun stagePendingIntent(task: CreatorTask, stage: Stage, atMillis: Long, exactDelivery: Boolean): PendingIntent {',
)
needle = '            .putExtra(ReminderConstants.EXTRA_ESCALATION_STAGE, stage.name)\n'
if 'EXTRA_EXACT_DELIVERY, exactDelivery' not in s:
    s = s.replace(needle, needle + '            .putExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, exactDelivery)\n', 1)
p.write_text(s)

# 4) Normal receiver: inexact VOICE/ALARM never attempts a background FGS;
# exact delivery tries the rich service and falls back to a normal high-priority notification.
p = ROOT / 'ReminderReceiver.kt'
s = p.read_text()
old = '''        runCatching {
            when (mode) {
                ReminderMode.VOICE -> VoiceReminderService.start(context.applicationContext, task)
                ReminderMode.ALARM -> AlarmRingingService.start(context.applicationContext, task)
                ReminderMode.NONE -> Unit
                else -> ReminderNotifications.show(
                    context = context.applicationContext,
                    task = task,
                    deliveryDelayMillis = if (scheduledAt > 0L) (firedAt - scheduledAt).coerceAtLeast(0L) else null,
                )
            }
        }.onSuccess {
            if (mode != ReminderMode.NONE) ledger.markDelivered(taskId, scheduledAt)
        }
'''
new = '''        val appContext = context.applicationContext
        val exactDelivery = intent.getBooleanExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, false)
        val delayMillis = if (scheduledAt > 0L) (firedAt - scheduledAt).coerceAtLeast(0L) else null

        fun notificationFallback(label: String? = null): Boolean = runCatching {
            ReminderNotifications.show(
                context = appContext,
                task = task,
                deliveryDelayMillis = delayMillis,
                stageLabel = label,
            )
        }.isSuccess

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
            ReminderMode.NONE -> false
            else -> notificationFallback()
        }

        if (delivered) ledger.markDelivered(taskId, scheduledAt)
'''
if old not in s:
    raise SystemExit('ReminderReceiver delivery block not found')
s = s.replace(old, new)
p.write_text(s)

# 5) Smart receiver: same platform-safe fallback for VOICE / ALARM / CRITICAL.
p = ROOT / 'EscalationReceiver.kt'
s = p.read_text()
old = '''        when (stage) {
            SmartEscalationScheduler.Stage.SOFT -> {
                AlarmRingingService.stop(appContext)
                VoiceReminderService.stop(appContext)
                ReminderNotifications.show(appContext, task, stageLabel = "Smart · Gentle")
            }

            SmartEscalationScheduler.Stage.VOICE -> {
                ReminderSurfaceRegistry.closeAll()
                AlarmRingingService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
                VoiceReminderService.start(appContext, task)
            }

            SmartEscalationScheduler.Stage.ALARM -> {
                ReminderSurfaceRegistry.closeAll()
                VoiceReminderService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
                AlarmRingingService.start(appContext, task.copy(voiceEnabled = false), stage)
            }

            SmartEscalationScheduler.Stage.CRITICAL -> {
                ReminderSurfaceRegistry.closeAll()
                VoiceReminderService.stop(appContext)
                AlarmRingingService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
                AlarmRingingService.start(
                    appContext,
                    task.copy(priority = TaskPriority.CRITICAL, voiceEnabled = true),
                    stage,
                )
            }
        }
'''
new = '''        val exactDelivery = intent.getBooleanExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, false)

        fun fallback(label: String): Boolean = runCatching {
            ReminderNotifications.show(appContext, task, stageLabel = label)
        }.isSuccess

        when (stage) {
            SmartEscalationScheduler.Stage.SOFT -> {
                AlarmRingingService.stop(appContext)
                VoiceReminderService.stop(appContext)
                fallback("Smart · Gentle")
            }

            SmartEscalationScheduler.Stage.VOICE -> {
                ReminderSurfaceRegistry.closeAll()
                AlarmRingingService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
                if (!exactDelivery || !runCatching { VoiceReminderService.start(appContext, task) }.isSuccess) {
                    fallback("Smart · Voice fallback")
                }
            }

            SmartEscalationScheduler.Stage.ALARM -> {
                ReminderSurfaceRegistry.closeAll()
                VoiceReminderService.stop(appContext)
                ReminderNotifications.cancel(appContext, taskId)
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
                ReminderNotifications.cancel(appContext, taskId)
                if (!exactDelivery || !runCatching {
                        AlarmRingingService.start(
                            appContext,
                            task.copy(priority = TaskPriority.CRITICAL, voiceEnabled = true),
                            stage,
                        )
                    }.isSuccess) {
                    fallback("Smart · Critical fallback")
                }
            }
        }
'''
if old not in s:
    raise SystemExit('EscalationReceiver stage block not found')
s = s.replace(old, new)
p.write_text(s)
