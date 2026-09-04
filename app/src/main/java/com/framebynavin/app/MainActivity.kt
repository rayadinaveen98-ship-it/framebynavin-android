package com.framebynavin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.framebynavin.app.cloud.CloudSyncScheduler
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.ui.V131LaunchGate
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.widget.CreatorWidgetContract
import com.framebynavin.app.widget.CreatorWidgetLaunch

class MainActivity : ComponentActivity() {
    private var externalLaunch by mutableStateOf<CreatorWidgetLaunch?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotifications.ensureChannel(this)
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
