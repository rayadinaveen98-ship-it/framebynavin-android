package com.framebynavin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.cloud.CloudSyncScheduler
import com.framebynavin.app.reminders.CreatorContextNudgeWorker
import com.framebynavin.app.reminders.MissedReminderRecovery
import com.framebynavin.app.reminders.ReminderHealthScheduler
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.ui.V131LaunchGate
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.widget.CreatorWidgetContract
import com.framebynavin.app.widget.CreatorWidgetLaunch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var externalLaunch by mutableStateOf<CreatorWidgetLaunch?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotifications.ensureChannel(this)
        ReminderHealthScheduler.ensurePeriodic(this)
        CreatorContextNudgeWorker.ensurePeriodic(this)
        CloudSyncScheduler.ensurePeriodic(this)
        CloudSyncScheduler.enqueueNow(this)
        externalLaunch = widgetLaunch(intent)
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme {
                V131LaunchGate(externalLaunch = externalLaunch)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Covers the case where Android missed a one-shot reminder and the creator opens the app
        // later, including immediately after granting notification permission.
        lifecycleScope.launch(Dispatchers.IO) {
            MissedReminderRecovery.deliverMissedFromStore(applicationContext)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalLaunch = widgetLaunch(intent)
    }

    private fun widgetLaunch(intent: Intent?): CreatorWidgetLaunch? {
        val action = intent?.action ?: return null
        val supported = setOf(
            CreatorWidgetContract.ACTION_OPEN_TODAY,
            CreatorWidgetContract.ACTION_OPEN_STUDIO,
            CreatorWidgetContract.ACTION_NEW_PROJECT,
            CreatorWidgetContract.ACTION_RELEASE_DAY,
        )
        if (action !in supported) return null
        return CreatorWidgetLaunch(
            action = action,
            taskId = intent.getStringExtra(CreatorWidgetContract.EXTRA_TASK_ID).orEmpty(),
        )
    }
}
