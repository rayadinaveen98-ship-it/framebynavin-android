package com.framebynavin.app.phase1

import android.os.Bundle
import android.os.Build
import com.framebynavin.app.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.ui.V157ReminderCenterUi
import com.framebynavin.app.ui.V2232PersonalReminderHub
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.ui.theme.FrameTheme
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/**
 * Real production UI components, mounted in a disposable debug-only Activity.
 * Creator tasks are synthetic, in-memory fixtures; NO creator TaskStore writes.
 * Personal uses a fresh emulator store exactly as a new user would see it.
 *
 * This is NOT a release entrypoint. Callback switches allow QA to verify the
 * existing navigation path without touching real app data.
 */
class Phase1LiveReminderScreenReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug-only is not sufficient: refuse to show real Personal data on any
        // ordinary user phone, even if someone manually installs a debug APK.
        val isDisposableEmulator =
            Build.HARDWARE.equals("ranchu", ignoreCase = true) ||
            Build.HARDWARE.equals("goldfish", ignoreCase = true)
        if (!BuildConfig.DEBUG || !isDisposableEmulator ||
            !intent.getBooleanExtra("backlot_ci_visual_review", false)
        ) {
            finish()
            return
        }
        val initialScene = intent.getStringExtra("phase1_scene") ?: "creator"
        VisualExperiencePrefs.initialize(applicationContext)
        VisualExperiencePrefs.setTheme(
            if (initialScene.endsWith("-light")) FrameTheme.BACKLOT_LIGHT
            else FrameTheme.DIRECTORS_CUT
        )
        setContent {
            FrameByNavinTheme {
                Phase1LiveHubReview(initialScene.startsWith("personal"))
            }
        }
    }
}

@Composable
private fun Phase1LiveHubReview(startPersonal: Boolean) {
    var personal by remember { mutableStateOf(startPersonal) }
    val now = remember { System.currentTimeMillis() }
    val tasks = remember(now) {
        listOf(
            CreatorTask(
                id = "phase1-demo-editor-cut",
                title = "Review rough cut",
                platform = "YouTube",
                contentType = "Film",
                dueLabel = "Today",
                reminderEnabled = true,
                reminderAtMillis = now + 2L * 60L * 60_000L,
                reminderMode = ReminderMode.SIMPLE,
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                dueAtMillis = now + 24L * 60L * 60_000L,
            ),
            CreatorTask(
                id = "phase1-demo-film-release",
                title = "Publish project update",
                platform = "YouTube",
                contentType = "Long",
                dueLabel = "This week",
                reminderEnabled = true,
                reminderAtMillis = now + 36L * 60L * 60_000L,
                reminderMode = ReminderMode.SIMPLE,
                attentionPlan = ProjectAttentionPlan.CUSTOM,
                dueAtMillis = now + 72L * 60L * 60_000L,
            ),
        )
    }
    if (personal) {
        V2232PersonalReminderHub(
            onClose = { personal = false },
            onCreator = { personal = false },
            onOpenActivity = {},
        )
    } else {
        V157ReminderCenterUi(
            tasks = tasks,
            onDismiss = {},
            onNew = {},
            onEdit = {},
            onDeleteReminders = {},
            onOpenReminderSettings = {},
            onSnooze = null,
            onPause = null,
            onReschedule = null,
            snoozeMinutes = 10,
            onPersonal = { personal = true },
        )
    }
}
