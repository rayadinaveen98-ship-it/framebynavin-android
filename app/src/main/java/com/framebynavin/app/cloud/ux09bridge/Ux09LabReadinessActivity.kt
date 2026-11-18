package com.framebynavin.app.cloud.ux09bridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Non-exported and Lab-only manual QA entry point (Settings > Advanced).
 * This screen cannot write an outbox, hit Supabase, backup, restore or delete.
 */
internal class Ux09LabReadinessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.BACKLOT_LAB) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme {
                ReadinessScreen(onClose = ::finish)
            }
        }
    }

    @Composable
    private fun ReadinessScreen(onClose: () -> Unit) {
        val scope = rememberCoroutineScope()
        val inspection = remember { Ux09LabCanonicalReadiness(applicationContext) }
        var busy by remember { mutableStateOf(false) }
        var failure by remember { mutableStateOf(false) }
        var summary by remember { mutableStateOf<Ux09LabCanonicalReadiness.Summary?>(null) }

        Surface(modifier = Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                TextButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = ProjectorIvory)
                    Spacer(Modifier.width(8.dp))
                    Text("BACK", color = ProjectorIvory)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "BACKLOT LAB / UX09",
                    color = RecRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Local sync readiness",
                    color = ProjectorIvory,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "An inspection of the Projects and Ideas already saved on this device. " +
                        "No content, recordings or account data is uploaded, changed or deleted.",
                    color = MutedText, fontSize = 13.sp, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        if (busy) return@OutlinedButton
                        busy = true
                        failure = false
                        summary = null
                        scope.launch {
                            try {
                                summary = withContext(Dispatchers.Default) {
                                    inspection.inspect()
                                }
                            } catch (_: Exception) {
                                // Never display data-store exception strings: they may
                                // contain local record identifiers or private paths.
                                failure = true
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "CHECKING…" else "CHECK READINESS")
                }
                Spacer(Modifier.height(14.dp))
                if (failure) {
                    Text(
                        "Inspection safely stopped: check local data or restore readiness. " +
                            "No records were changed.",
                        color = MutedGold, fontSize = 13.sp, lineHeight = 19.sp,
                    )
                }
                summary?.let { result ->
                    Text(
                        "Projects: " + result.projectCandidates +
                            " simple candidates / " + result.projectCount + " total",
                        color = ProjectorIvory, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ideas: " + result.ideaCandidates +
                            " text candidates / " + result.ideaCount + " total",
                        color = ProjectorIvory, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Excluded safely: " + result.blocked +
                            ". Complex workflows, active reminders, media, linked work, " +
                            "older IDs and large records remain entirely local.",
                        color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                    result.reasons.forEach { (reason, count) ->
                        Spacer(Modifier.height(6.dp))
                        Text(reasonLabel(reason) + ": " + count,
                            color = MutedGold, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "QA ONLY · Not a completed cloud backup, restore or multi-device test. " +
                        "No automatic synchronization has been activated.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                )
            }
        }
    }

    private fun reasonLabel(reason: Ux09CanonicalReadinessPolicy.Blocker): String = when (reason) {
        Ux09CanonicalReadinessPolicy.Blocker.NON_UUID_ID -> "Older record ID"
        Ux09CanonicalReadinessPolicy.Blocker.NOT_SIMPLE_PROJECT -> "Advanced project state"
        Ux09CanonicalReadinessPolicy.Blocker.ACTIVE_REMINDER_OR_SCHEDULE -> "Reminder or schedule"
        Ux09CanonicalReadinessPolicy.Blocker.RICH_WORKSPACE_OR_DNA -> "Content workspace or DNA"
        Ux09CanonicalReadinessPolicy.Blocker.LINKED_OR_LEGACY_PROJECT -> "Linked project"
        Ux09CanonicalReadinessPolicy.Blocker.VOICE_OR_UNPORTABLE_IDEA -> "Linked or voice idea"
        Ux09CanonicalReadinessPolicy.Blocker.IDEA_REMINDER_OR_TRASH -> "Idea reminder or trash"
        Ux09CanonicalReadinessPolicy.Blocker.INCOMPLETE_SOURCE -> "Source format"
        Ux09CanonicalReadinessPolicy.Blocker.OVER_SERVER_LIMIT -> "Exceeds server limit"
        Ux09CanonicalReadinessPolicy.Blocker.UNSAFE_OR_NONCANONICAL_JSON -> "Unsafe portable data"
        Ux09CanonicalReadinessPolicy.Blocker.ROUND_TRIP_CHANGED -> "Lossless round-trip mismatch"
    }
}
