package com.framebynavin.app.phase1

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitDayStatus
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.ui.CreateHabitReview
import com.framebynavin.app.ui.DashboardHabitReview
import com.framebynavin.app.ui.Phase1HabitLocalStoreReview
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * DISPOSABLE Android review only. Uses pure 21-day policy with in-memory data.
 * Never reads/writes V297, Personal reminders, cloud, account or alarm services.
 */
class Phase1HabitInteractiveReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val emulator = Build.HARDWARE.equals("ranchu", true) ||
            Build.HARDWARE.equals("goldfish", true)
        if (!BuildConfig.DEBUG || !emulator ||
            !intent.getBooleanExtra("backlot_ci_visual_review", false)) {
            finish()
            return
        }
        val scene = intent.getStringExtra("habit_review_scene") ?: "create"
        VisualExperiencePrefs.initialize(applicationContext)
        VisualExperiencePrefs.setTheme(FrameTheme.DIRECTORS_CUT)
        setContent {
            FrameByNavinTheme {
                if (scene == "local-store") {
                    // Real DataStore-backed Compose review, CI-emulator only.
                    Phase1HabitLocalStoreReview(onClose = { finish() })
                } else {
                    Phase1HabitInteractiveReview(scene)
                }
            }
        }
    }
}

private val zone = ZoneId.of("Asia/Kolkata")
private val firstNow = Instant.parse("2026-11-07T04:00:00Z")
private val seventhNow = Instant.parse("2026-11-13T04:00:00Z")
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

private fun seeded(scene: String): Phase1HabitChallenge? {
    if (!scene.startsWith("day7")) return null
    val start = LocalDate.of(2026, 11, 7)
    val number = if (scene == "day7-done") 7 else 6
    val receipts = (0 until number).associate { n ->
        val day = start.plusDays(n.toLong())
        day to day.atTime(12, 0).atZone(zone).toInstant()
    }
    return Phase1HabitChallenge(
        id = "preview-session",
        activity = "Read 20 minutes",
        startDate = start,
        zoneId = zone.id,
        alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
        confirmations = receipts,
        paused = scene == "day7-paused",
    ).checked()
}

/** Mark/undo/pause below invoke the REAL pure policy, not fake progress counters. */
@Composable
internal fun Phase1HabitInteractiveReview(scene: String = "create") {
    val now = if (scene.startsWith("day7")) seventhNow else firstNow
    var challenge by remember(scene) { mutableStateOf(seeded(scene)) }
    // The person defines the activity; never seed a prescribed routine.
    var title by remember(scene) { mutableStateOf("") }
    var date by remember(scene) { mutableStateOf(LocalDate.of(2026, 11, 7)) }
    var mode by remember(scene) {
        mutableStateOf<Phase1HabitAlertKind?>(
            if (scene == "create-no-alert") null else Phase1HabitAlertKind.ALARM
        )
    }
    var at by remember(scene) { mutableStateOf(LocalTime.of(7, 30)) }
    var error by remember { mutableStateOf(false) }
    Surface(color = BacklotBackground, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("BACKLOT / PERSONAL", color = BacklotPrimaryText,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("REVIEW ONLY", color = BacklotAccent, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold)
            }
            val current = challenge
            if (current == null) {
                CreateHabitReview(
                    title, { title = it.take(200); error = false },
                    date, { date = it },
                    mode, { mode = it },
                    at, { at = it },
                    error,
                    onStart = {
                        if (title.trim().isEmpty()) error = true
                        else challenge = Phase1HabitChallenge(
                            id = "preview-session", activity = title.trim(),
                            startDate = date, zoneId = zone.id,
                            alertTime = if (mode == null) null else at,
                            alertKind = mode,
                        ).checked()
                    },
                    previewOnly = true,
                )
            } else {
                DashboardHabitReview(
                    current, now,
                    onDone = { challenge = current.confirmToday(now) },
                    onUndo = {
                        challenge = current.undoDay(LocalDate.ofInstant(now, zone))
                    },
                    onPause = { challenge = current.setPaused(!current.paused) },
                    previewOnly = true,
                )
            }
        }
    }
}

