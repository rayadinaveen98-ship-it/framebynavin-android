package com.framebynavin.app.ui

import android.app.ActivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.*

/** Device-only erase. Explicit owner consent includes app-owned media and escrow. */
internal object FreshStartPolicy {
    const val CONFIRM_PHRASE = "ERASE THIS PHONE"
    fun allowed(
        phrase: String,
        lossAcknowledged: Boolean,
        cloudAcknowledged: Boolean,
        mediaAcknowledged: Boolean,
        recoveryAcknowledged: Boolean,
        backupAcknowledged: Boolean,
        preflight: V383DeviceResetInspection,
    ): Boolean =
        phrase.trim() == CONFIRM_PHRASE &&
            lossAcknowledged && cloudAcknowledged && mediaAcknowledged &&
            recoveryAcknowledged && backupAcknowledged && preflight.mayOfferReset
}

class FreshStartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrameByNavinTheme {
                var phrase by remember { mutableStateOf("") }
                var lossAcknowledged by remember { mutableStateOf(false) }
                var cloudAcknowledged by remember { mutableStateOf(false) }
                var mediaAcknowledged by remember { mutableStateOf(false) }
                var recoveryAcknowledged by remember { mutableStateOf(false) }
                var backupAcknowledged by remember { mutableStateOf(false) }
                var armed by remember { mutableStateOf(false) }
                var submitting by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf<String?>(null) }
                var preflight by remember {
                    mutableStateOf(V383UnifiedDeviceResetPreflight.inspect(filesDir))
                }
                Surface(color = CinemaBlack, modifier = Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .verticalScroll(rememberScrollState())
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        TextButton(onClick = { finish() }) { Text("BACK TO BACKUPS", color = ProjectorIvory) }
                        Text("Start fresh on this phone", color = ProjectorIvory,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                        Text(
                            "Permanently erases Backlot data on this phone: Creator projects, scripts, " +
                                "ideas, workspaces, Voice Ideas, Best Frames, Personal Reminders, " +
                                "Done history, 21-day challenges, Spaces, journals, private photos, " +
                                "settings, saved sign-in and local encrypted recovery copies.",
                            color = ProjectorIvory,
                        )
                        Text(
                            "CLOUD IS NOT DELETED. Supabase/Google accounts, cloud backups, separately " +
                                "exported files and original external project media remain. " +
                                "Signing in again may make previous cloud records available.",
                            color = MutedGold,
                        )
                        Text(
                            "Before erasing, save and verify a unified encrypted backup off-device if " +
                                "you want to restore this work later. A backup still stored only " +
                                "inside Backlot will also be erased. Android closes the app after reset.",
                            color = MutedText,
                        )
                        Row {
                            Checkbox(checked = lossAcknowledged,
                                onCheckedChange = { lossAcknowledged = it; armed = false })
                            Text("I understand all local Creator and Personal data will be lost, including recovery copies.",
                                color = ProjectorIvory, modifier = Modifier.padding(top = 10.dp))
                        }
                        Row {
                            Checkbox(checked = cloudAcknowledged,
                                onCheckedChange = { cloudAcknowledged = it; armed = false })
                            Text("I understand this resets this phone only, not cloud data.",
                                color = ProjectorIvory, modifier = Modifier.padding(top = 10.dp))
                        }
                        Row {
                            Checkbox(checked = mediaAcknowledged,
                                onCheckedChange = { mediaAcknowledged = it; armed = false })
                            Text("Erase app-owned recordings, Best Frames, Home photos, Space photos and other private media.",
                                color = ProjectorIvory, modifier = Modifier.padding(top = 10.dp))
                        }
                        Row {
                            Checkbox(checked = recoveryAcknowledged,
                                onCheckedChange = { recoveryAcknowledged = it; armed = false })
                            Text("Erase local encrypted recovery copies and restore history too. They cannot be recovered after reset.",
                                color = ProjectorIvory, modifier = Modifier.padding(top = 10.dp))
                        }
                        Row {
                            Checkbox(checked = backupAcknowledged,
                                onCheckedChange = { backupAcknowledged = it; armed = false })
                            Text("I have saved and checked any backup I need outside this app, or I accept permanent loss.",
                                color = ProjectorIvory, modifier = Modifier.padding(top = 10.dp))
                        }
                        if (!preflight.mayOfferReset) {
                            Text(
                                "Reset blocked: unfinished restore evidence or unreadable private " +
                                    "storage needs recovery review first. Backlot will not silently " +
                                    "erase recovery evidence.",
                                color = RecRed,
                                modifier = Modifier.testTag("fresh-start-recovery-blocked"),
                            )
                            TextButton(onClick = {
                                preflight = V383UnifiedDeviceResetPreflight.inspect(filesDir)
                                message = null
                            }) { Text("RECHECK RECOVERY STATUS", color = MutedGold) }
                        }
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth().testTag("fresh-start-confirm-phrase"),
                            value = phrase,
                            onValueChange = { phrase = it; armed = false },
                            label = { Text("Type ERASE THIS PHONE") },
                            singleLine = true,
                        )
                        val enabled = FreshStartPolicy.allowed(
                            phrase, lossAcknowledged, cloudAcknowledged,
                            mediaAcknowledged, recoveryAcknowledged,
                            backupAcknowledged, preflight,
                        )
                        Button(
                            enabled = enabled && !armed && !submitting,
                            onClick = {
                                preflight = V383UnifiedDeviceResetPreflight.inspect(filesDir)
                                if (preflight.mayOfferReset) armed = true
                                else message = "An unfinished restore requires review before device reset."
                            },
                            modifier = Modifier.fillMaxWidth().testTag("fresh-start-review"),
                        ) { Text("REVIEW DEVICE RESET") }
                        if (message != null) {
                            Text(message.orEmpty(), color = RecRed,
                                modifier = Modifier.testTag("fresh-start-error"))
                        }
                    }
                    if (armed) {
                        AlertDialog(
                            onDismissRequest = { armed = false },
                            title = { Text("Erase Backlot data on this phone?") },
                            text = { Text(
                                "This is irreversible. Android erases Creator and Personal records, " +
                                    "private media, settings and local recovery copies, then " +
                                    "closes Backlot. Only separately saved backups, external files " +
                                    "and cloud account data remain."
                            ) },
                            dismissButton = {
                                TextButton(onClick = { armed = false }) { Text("CANCEL") }
                            },
                            confirmButton = {
                                Button(
                                    modifier = Modifier.testTag("fresh-start-final-erase"),
                                    onClick = {
                                        armed = false
                                        // Re-inspect at the final irreversible click. This
                                        // check never repairs or deletes a recovery journal.
                                        val latest = V383UnifiedDeviceResetPreflight.inspect(filesDir)
                                        preflight = latest
                                        if (!FreshStartPolicy.allowed(
                                                phrase, lossAcknowledged, cloudAcknowledged,
                                                mediaAcknowledged, recoveryAcknowledged,
                                                backupAcknowledged, latest,
                                            )
                                        ) {
                                            message = "Confirmation changed or recovery is pending. No reset requested."
                                        } else {
                                            submitting = true
                                            val manager = getSystemService(ActivityManager::class.java)
                                            val requested = runCatching {
                                                manager?.clearApplicationUserData() == true
                                            }.getOrDefault(false)
                                            if (!requested) {
                                                submitting = false
                                                message = "Android did not accept the erase request. " +
                                                    "Your data has not been confirmed erased."
                                            }
                                        }
                                    },
                                ) { Text("ERASE DEVICE DATA") }
                            },
                        )
                    }
                }
            }
        }
    }
}
