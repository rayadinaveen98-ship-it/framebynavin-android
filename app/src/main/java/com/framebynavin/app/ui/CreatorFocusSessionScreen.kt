package com.framebynavin.app.ui

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorFocusLedger
import com.framebynavin.app.data.CreatorFocusSession
import com.framebynavin.app.data.CreatorFocusSessionPolicy
import com.framebynavin.app.data.CreatorFocusSessionStore
import com.framebynavin.app.data.CreatorFocusState
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Phase 3C owner-review implementation. It reads/writes only Focus session records.
 *
 * A finished timer NEVER marks a CreatorTask stage Done or Published. A working
 * session's clock anchor is stored durably and restored after composition recreation;
 * the current boot identity must be readable before starting/resuming work.
 */
@Composable
internal fun CreatorFocusSessionScreen(task: CreatorTask, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember(context) { CreatorFocusSessionStore(context) }
    // A Creator restore increments this epoch before replacing local records.
    // Reject stale UI writes instead of overwriting newly restored Focus receipts.
    val openGeneration = remember(context) { CreatorDataGate.generation(context) }
    val stage = CreatorWorkflowEngine.currentStage(task)
    val bootCount = remember(context) {
        runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) }
            .getOrDefault(-1)
    }
    var currentElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var ledger by remember { mutableStateOf<CreatorFocusLedger?>(null) }
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var error by remember { mutableStateOf<String?>(null) }
    var discardConfirmation by remember { mutableStateOf(false) }
    var completedSummary by remember { mutableStateOf<CreatorFocusSession?>(null) }

    fun readLedger() {
        runCatching {
            CreatorDataGate.checkGeneration(context, openGeneration)
            val existing = store.load()
            val active = existing.active
            if (active != null && active.state == CreatorFocusState.RUNNING) {
                CreatorDataGate.withGeneration(context, openGeneration) {
                    store.update(active.id) {
                        CreatorFocusSessionPolicy.recover(it, SystemClock.elapsedRealtime(), bootCount)
                    }
                }
            }
            CreatorDataGate.checkGeneration(context, openGeneration)
            store.load()
        }.onSuccess { ledger = it; error = null }
            .onFailure { error = it.message ?: "Could not read Focus session data" }
    }

    fun updateSession(action: (CreatorFocusSession) -> CreatorFocusSession) {
        val session = ledger?.active ?: return
        runCatching {
            val next = CreatorDataGate.withGeneration(context, openGeneration) {
                store.update(session.id, action)
            }
            currentElapsed = SystemClock.elapsedRealtime()
            next
        }.onSuccess { ledger = it; error = null; discardConfirmation = false }
            .onFailure { error = it.message ?: "Could not save Focus session" }
    }

    fun checkpointSession() {
        val current = ledger?.active ?: return
        if (current.state != CreatorFocusState.RUNNING) return
        runCatching {
            CreatorDataGate.withGeneration(context, openGeneration) {
                store.update(current.id) {
                    CreatorFocusSessionPolicy.checkpoint(it, SystemClock.elapsedRealtime(), bootCount)
                }
            }
        }.onSuccess { ledger = it }
            .onFailure { error = it.message ?: "Could not save Focus checkpoint" }
    }

    LaunchedEffect(task.id) { readLedger() }

    LaunchedEffect(ledger?.active?.id, ledger?.active?.state) {
        var ticks = 0
        while (true) {
            delay(1000)
            currentElapsed = SystemClock.elapsedRealtime()
            val live = ledger?.active
            if (live?.state == CreatorFocusState.RUNNING) {
                ticks++
                if (ticks >= 15) {
                    checkpointSession()
                    ticks = 0
                }
            } else {
                ticks = 0
            }
        }
    }

    DisposableEffect(lifecycleOwner, ledger?.active?.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) checkpointSession()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            checkpointSession()
        }
    }

    val active = ledger?.active
    val activeHere = active != null && active.projectId == task.id
    val activeElsewhere = active != null && !activeHere
    val remaining = when {
        active != null -> CreatorFocusSessionPolicy.remainingMillis(active, currentElapsed, bootCount)
        completedSummary != null -> (completedSummary!!.targetMillis - completedSummary!!.accruedActiveMillis).coerceAtLeast(0L)
        else -> selectedMinutes * 60_000L
    }
    val target = active?.targetMillis ?: completedSummary?.targetMillis ?: selectedMinutes * 60_000L
    val elapsed = target - remaining
    val seconds = (remaining + 999L) / 1000L
    val clockText = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60L, seconds % 60L)

    Surface(modifier = Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { checkpointSession(); onClose() }, modifier = Modifier.testTag("phase3-focus-close")) {
                    Text("CLOSE", color = ProjectorIvory, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("FOCUS SESSION", color = RecRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(36.dp))
            Text(stage.label.uppercase(Locale.getDefault()), color = RecRed, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 25.sp, lineHeight = 30.sp,
                textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(stage.action, color = MutedText, fontSize = 13.sp,
                textAlign = TextAlign.Center, lineHeight = 19.sp)
            Spacer(Modifier.height(32.dp))

            if (ledger == null) {
                Text("LOADING SESSION", color = MutedText)
            } else {
                if (activeElsewhere) {
                    Text("A Focus session is already linked to project ${active?.projectId}.", color = ProjectorIvory,
                        textAlign = TextAlign.Center)
                    Text("Finish or discard it from that project's Focus screen before starting another.",
                        color = MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                } else {
                    if (active == null && completedSummary == null) {
                        Text("CHOOSE A SESSION", color = MutedText, fontSize = 11.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CreatorFocusSessionPolicy.durationOptionsMinutes.forEach { minutes ->
                                OutlinedButton(
                                    onClick = { selectedMinutes = minutes },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                        .testTag("phase3-focus-duration-${minutes}"),
                                    border = BorderStroke(1.dp, if (selectedMinutes == minutes) RecRed else CinemaLine),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 1.dp),
                                ) {
                                    Text("${minutes}m", color = ProjectorIvory, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(30.dp))
                    Text(clockText, color = ProjectorIvory, fontSize = 56.sp,
                        fontWeight = FontWeight.Black, modifier = Modifier.testTag("phase3-focus-clock"))
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (elapsed.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = RecRed, trackColor = CinemaLine
                    )
                    Spacer(Modifier.height(18.dp))
                    if (active?.recoveredAfterReboot == true) {
                        Text("Session recovered after an interruption. Unverified time was not counted.",
                            color = MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                    }
                    when {
                        completedSummary != null -> {
                            Text("SESSION FINISHED", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                            val minutes = completedSummary!!.accruedActiveMillis / 60_000L
                            Text("${minutes} verified minutes. Your project stage was not changed.",
                                color = MutedText, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                                Text("RETURN TO PROJECT", color = ProjectorIvory)
                            }
                        }
                        active == null -> {
                            Button(
                                onClick = {
                                    if (bootCount < 0) {
                                        error = "Device boot identity unavailable; Focus cannot safely track time."
                                    } else {
                                        runCatching {
                                            val created = CreatorFocusSessionPolicy.create(task.id, stage.id,
                                                selectedMinutes, System.currentTimeMillis())
                                            CreatorDataGate.withGeneration(context, openGeneration) {
                                                store.create(created)
                                                store.update(created.id) {
                                                    CreatorFocusSessionPolicy.start(it, SystemClock.elapsedRealtime(), bootCount)
                                                }
                                            }
                                        }.onSuccess { ledger = it; error = null }
                                            .onFailure { error = it.message ?: "Unable to start Focus" }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("phase3-focus-start"),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            ) { Text("START FOCUS", fontWeight = FontWeight.Bold) }
                        }
                        active.state == CreatorFocusState.RUNNING -> {
                            Button(
                                onClick = {
                                    updateSession {
                                        CreatorFocusSessionPolicy.pause(it, SystemClock.elapsedRealtime(), bootCount)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("phase3-focus-pause"),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            ) { Text(if (remaining == 0L) "PAUSE — TARGET REACHED" else "PAUSE", fontWeight = FontWeight.Bold) }
                        }
                        active.state == CreatorFocusState.PAUSED || active.state == CreatorFocusState.READY -> {
                            if (remaining > 0L) {
                                Button(
                                    onClick = {
                                        if (bootCount < 0) error = "Device boot identity unavailable"
                                        else updateSession {
                                            CreatorFocusSessionPolicy.start(it, SystemClock.elapsedRealtime(), bootCount)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                        .testTag("phase3-focus-resume"),
                                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                                ) { Text(if (active.state == CreatorFocusState.READY) "START FOCUS" else "RESUME", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    if (activeHere) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val finishing = active ?: return@OutlinedButton
                                runCatching {
                                    val finished = CreatorFocusSessionPolicy.finish(finishing,
                                        SystemClock.elapsedRealtime(), bootCount, System.currentTimeMillis())
                                    CreatorDataGate.withGeneration(context, openGeneration) {
                                        store.update(finishing.id) { old ->
                                            CreatorFocusSessionPolicy.finish(old,
                                                SystemClock.elapsedRealtime(), bootCount, finished.endedAtWallMillis)
                                        }
                                    }
                                    finished
                                }.onSuccess {
                                    completedSummary = it
                                    readLedger()
                                }.onFailure { error = it.message ?: "Unable to finish Focus" }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("phase3-focus-finish"),
                            border = BorderStroke(1.dp, CinemaLine),
                        ) { Text("FINISH SESSION", color = ProjectorIvory) }
                        Spacer(Modifier.height(10.dp))
                        if (discardConfirmation) {
                            Text("Discard this session without counting its minutes?",
                                color = ProjectorIvory, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { discardConfirmation = false }, modifier = Modifier.weight(1f)) {
                                    Text("CANCEL", color = ProjectorIvory)
                                }
                                TextButton(onClick = {
                                    updateSession { CreatorFocusSessionPolicy.discard(it, System.currentTimeMillis()) }
                                }, modifier = Modifier.weight(1f).testTag("phase3-focus-confirm-discard")) {
                                    Text("DISCARD", color = RecRed)
                                }
                            }
                        } else {
                            TextButton(onClick = { discardConfirmation = true },
                                modifier = Modifier.testTag("phase3-focus-discard")) {
                                Text("DISCARD SESSION", color = MutedText)
                            }
                        }
                    }
                }
            }
            error?.let { message ->
                Spacer(Modifier.height(18.dp))
                Surface(color = CinemaSurface, shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RecRed)) {
                    Text(message, color = ProjectorIvory, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("phase3-focus-error"))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Focus sessions track time only. Completing a project stage is a separate action.",
                color = MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}
