package com.framebynavin.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine

/** After Custom Stage Done, ask rather than inventing the next stage's check-in time. */
object ProjectPulseNextStagePrompt {
    fun show(context: Context, task: CreatorTask) {
        val app = context.applicationContext
        if (!ReminderNotifications.canPost(app)) return
        ensureChannel(app)
        val stage = CreatorWorkflowEngine.currentStage(task)
        val generation = CreatorDataGate.generation(app)
        val builder = NotificationCompat.Builder(app, ReminderConstants.PULSE_PROMPT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${stage.label} is next")
            .setContentText("When should FrameByNavin check this stage?")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "${stage.label} is now active. Choose a quick check-in below, or tap to open the project and choose an exact time."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openProject(app, task.id))
            .addAction(0, "1 HOUR", choice(app, task, stage.id, generation, 60, 1))
            .addAction(0, "2 HOURS", choice(app, task, stage.id, generation, 120, 2))
            .addAction(0, "TOMORROW", choice(app, task, stage.id, generation, 24 * 60, 3))
        app.getSystemService(NotificationManager::class.java).notify(promptId(task.id), builder.build())
    }

    fun cancel(context: Context, taskId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(promptId(taskId))
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderConstants.PULSE_PROMPT_CHANNEL_ID,
                "Stage check-in choices",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Choose when Project Pulse should check the next workflow stage" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun choice(
        context: Context,
        task: CreatorTask,
        stageId: String,
        generation: Long,
        minutes: Int,
        salt: Int,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderConstants.ACTION_SET_NEXT_STAGE_CHECKIN)
            .setData(Uri.parse("framebynavin://pulse-next/${Uri.encode(task.id)}/${Uri.encode(stageId)}/$minutes"))
            .putExtra(ReminderConstants.EXTRA_TASK_ID, task.id)
            .putExtra(ReminderConstants.EXTRA_EXPECTED_STAGE, stageId)
            .putExtra(ReminderConstants.EXTRA_DATA_GENERATION, generation)
            .putExtra(ReminderConstants.EXTRA_DELAY_MINUTES, minutes)
        return PendingIntent.getBroadcast(
            context,
            task.id.hashCode() xor (0x7300 + salt),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openProject(context: Context, taskId: String): PendingIntent = PendingIntent.getActivity(
        context,
        taskId.hashCode() xor 0x7399,
        Intent(context, MainActivity::class.java)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun promptId(taskId: String): Int = taskId.hashCode() xor 0x73A0
}
