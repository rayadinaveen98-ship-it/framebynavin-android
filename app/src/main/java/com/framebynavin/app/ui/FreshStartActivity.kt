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

/** Device-only data reset. Never deletes cloud user accounts, snapshots or media. */
internal object FreshStartPolicy {
    const val CONFIRM_PHRASE = "ERASE THIS PHONE"
    fun allowed(phrase: String, lossAcknowledged: Boolean, cloudAcknowledged: Boolean) =
        phrase.trim() == CONFIRM_PHRASE && lossAcknowledged && cloudAcknowledged
}

class FreshStartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrameByNavinTheme {
                var phrase by remember { mutableStateOf("") }
                var lossAcknowledged by remember { mutableStateOf(false) }
                var cloudAcknowledged by remember { mutableStateOf(false) }
                var armed by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf(false) }
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
                            "Permanently erases data inside this Android app: projects, ideas, " +
                                "Personal Reminders, progress, private recordings, Home photos, " +
                                "settings, sign-in session, and local recovery copies.",
                            color = ProjectorIvory,
                        )
                        Text(
                            "CLOUD IS NOT DELETED. Supabase accounts, cloud backups and external files " +
                                "remain. Signing in again may make previous cloud records available.",
                            color = MutedGold,
                        )
                        Text(
                            "Export Creator and Personal backups only if you want to keep any copies. " +
                                "Android closes Backlot after erasing; reopen it for fresh setup.",
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
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth().testTag("fresh-start-confirm-phrase"),
                            value = phrase,
                            onValueChange = { phrase = it; armed = false },
                            label = { Text("Type ERASE THIS PHONE") },
                            singleLine = true,
                        )
                        val enabled = FreshStartPolicy.allowed(phrase, lossAcknowledged, cloudAcknowledged)
                        Button(
                            enabled = enabled && !armed,
                            onClick = { armed = true },
                            modifier = Modifier.fillMaxWidth().testTag("fresh-start-review"),
                        ) { Text("REVIEW DEVICE RESET") }
                        if (error) {
                            Text("Android could not erase the app data; no reset was confirmed.",
                                color = RecRed)
                        }
                    }
                    if (armed) {
                        AlertDialog(
                            onDismissRequest = { armed = false },
                            title = { Text("Erase Backlot data on this phone?") },
                            text = { Text(
                                "This is irreversible. Android removes all Backlot app-private " +
                                    "data and closes the app. Your cloud account and cloud backups remain."
                            ) },
                            dismissButton = {
                                TextButton(onClick = { armed = false }) { Text("CANCEL") }
                            },
                            confirmButton = {
                                Button(
                                    modifier = Modifier.testTag("fresh-start-final-erase"),
                                    onClick = {
                                        armed = false
                                        val manager = getSystemService(ActivityManager::class.java)
                                        error = manager?.clearApplicationUserData() != true
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
