package com.framebynavin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.framebynavin.app.reminders.CreatorAutoPlanWorker
import com.framebynavin.app.reminders.CreatorContextNudgeWorker
import com.framebynavin.app.reminders.CreatorRoutineWorker
import com.framebynavin.app.reminders.ReminderRecoveryEngine
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
    private var startupReady by mutableStateOf(false)
    private var startupError by mutableStateOf<String?>(null)
    private var startupRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Install the variant-specific Firebase App Check provider before any optional AI request.
        // Debug builds use Firebase's debug provider; release builds use Play Integrity.
        CreatorAppCheck.install(applicationContext)
        splash.setKeepOnScreenCondition { !startupReady && startupError == null }
        externalLaunch = widgetLaunch(intent)
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme {
                when {
                    startupReady -> V131LaunchGate(externalLaunch = externalLaunch)
                    startupError != null -> Surface(Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text("Recovery needs attention", color = Color.White, fontSize = 24.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(startupError.orEmpty(), color = Color.LightGray, fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { beginStartup() }) { Text("RETRY RECOVERY") }
                            TextButton(onClick = { startActivity(Intent(this@MainActivity, com.framebynavin.app.ui.BackupActivity::class.java)) }) {
                                Text("OPEN BACKUP TOOLS")
                            }
                        }
                    }
                    else -> Surface(Modifier.fillMaxSize(), color = Color(0xFF101010)) { }
                }
            }
        }
        beginStartup()
    }

    private fun beginStartup() {
        if (startupRunning || startupReady) return
        startupRunning = true
        startupError = null
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                CreatorBackupManager(applicationContext).recoverPendingRestore()
                ReminderNotifications.ensureChannel(applicationContext)
                ReminderHealthScheduler.ensurePeriodic(applicationContext)
                CreatorContextNudgeWorker.ensurePeriodic(applicationContext)
                CreatorAutoPlanWorker.ensurePeriodic(applicationContext)
                CreatorRoutineWorker.ensurePeriodic(applicationContext)
                ReminderRecoveryEngine.reconcile(applicationContext)
            }
            withContext(Dispatchers.Main) {
                startupRunning = false
                result.onSuccess { startupReady = true }
                    .onFailure { startupError = it.message ?: "Could not safely recover the previous data. Your recovery files have been retained." }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (startupReady) lifecycleScope.launch(Dispatchers.IO) {
            ReminderRecoveryEngine.reconcile(applicationContext)
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
            CreatorWidgetContract.ACTION_DAILY_BRIEF,
            CreatorWidgetContract.ACTION_CONTENT_CALENDAR,
            CreatorWidgetContract.ACTION_IDEA_VAULT,
            CreatorWidgetContract.ACTION_OPEN_INSIGHTS,
            CreatorWidgetContract.ACTION_AUTOMATION_CENTER,
        )
        if (action !in supported) return null
        return CreatorWidgetLaunch(
            action = action,
            taskId = intent.getStringExtra(CreatorWidgetContract.EXTRA_TASK_ID).orEmpty(),
        )
    }
}
