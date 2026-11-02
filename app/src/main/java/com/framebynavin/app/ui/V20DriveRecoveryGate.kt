package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.CloudSession
import com.framebynavin.app.cloud.CreatorCloudSyncManager
import com.framebynavin.app.cloud.CreatorCloudSyncResult
import com.framebynavin.app.cloud.DriveVaultLocalStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Returning-creator recovery gate. Despite the historical file name, Supabase creator cloud is now
 * authoritative for automatic backup/recovery. Google Drive is optional manual import/export only.
 */
@Composable
internal fun V20DriveRecoveryGate(
    session: CloudSession,
    onRecovered: () -> Unit,
    onStartNew: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sync = remember { CreatorCloudSyncManager(context.applicationContext) }
    // Keep the existing reviewed marker so upgrades do not re-run the gate unexpectedly.
    val recoveryMarker = remember { DriveVaultLocalStore(context.applicationContext) }
    var busy by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<CreatorCloudSyncResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun finishRecovery() {
        recoveryMarker.markRecoveryReviewed(session.email)
        onRecovered()
    }

    fun runSync() {
        if (busy && result != null) return
        busy = true
        error = null
        scope.launch {
            when (val value = sync.syncNow()) {
                is CreatorCloudSyncResult.Synced,
                is CreatorCloudSyncResult.Restored -> {
                    result = value
                    busy = false
                    finishRecovery()
                }
                is CreatorCloudSyncResult.Conflict,
                is CreatorCloudSyncResult.PersonalConflict -> {
                    result = value
                    busy = false
                }
                is CreatorCloudSyncResult.Skipped -> {
                    result = value
                    error = value.message
                    busy = false
                }
                is CreatorCloudSyncResult.Failure -> {
                    result = value
                    error = value.message
                    busy = false
                }
            }
        }
    }

    fun resolveKeepPhone() {
        busy = true
        error = null
        scope.launch {
            val choice = if (result is CreatorCloudSyncResult.PersonalConflict)
                sync.resolvePersonalConflict(false) else sync.keepThisPhone()
            when (val value = choice) {
                is CreatorCloudSyncResult.Synced,
                is CreatorCloudSyncResult.Restored -> { busy = false; finishRecovery() }
                is CreatorCloudSyncResult.Conflict,
                is CreatorCloudSyncResult.PersonalConflict -> { result = value; busy = false }
                is CreatorCloudSyncResult.Skipped -> { error = value.message; busy = false }
                is CreatorCloudSyncResult.Failure -> { error = value.message; busy = false }
            }
        }
    }

    fun resolveRestoreCloud() {
        busy = true
        error = null
        scope.launch {
            val choice = if (result is CreatorCloudSyncResult.PersonalConflict)
                sync.resolvePersonalConflict(true) else sync.restoreCloud()
            when (val value = choice) {
                is CreatorCloudSyncResult.Synced,
                is CreatorCloudSyncResult.Restored -> { busy = false; finishRecovery() }
                is CreatorCloudSyncResult.Conflict,
                is CreatorCloudSyncResult.PersonalConflict -> { result = value; busy = false }
                is CreatorCloudSyncResult.Skipped -> { error = value.message; busy = false }
                is CreatorCloudSyncResult.Failure -> { error = value.message; busy = false }
            }
        }
    }

    LaunchedEffect(session.userId) { runSync() }
    val conflict = result as? CreatorCloudSyncResult.Conflict
    val personalConflict = result as? CreatorCloudSyncResult.PersonalConflict
    val anyConflict = conflict != null || personalConflict != null

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                if (!anyConflict) Icons.Outlined.CloudDone else Icons.Outlined.SyncProblem,
                null,
                tint = MutedGold,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (!anyConflict) "Recover Backlot" else "Choose which copy to keep",
                color = ProjectorIvory,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(6.dp))
            Text(session.email, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                if (!anyConflict)
                    "We'll check your saved Creator projects and Personal Reminders without silently overwriting either."
                else
                    "Your phone and cloud contain different work. Choose which copy to keep.",
                color = MutedText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(22.dp))

            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = RecRed)
                Spacer(Modifier.height(8.dp))
                Text("Checking saved backups…", color = MutedText)
            } else if (anyConflict) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = CinemaSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MutedGold.copy(alpha = .5f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("THIS PHONE", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(if (conflict != null) "${conflict.localProjectCount} projects · ${conflict.localIdeaCount} ideas"
                            else "${personalConflict?.localCount ?: 0} Personal Reminders", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("CLOUD", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(if (conflict != null) "${conflict.cloudProjectCount} projects · ${conflict.cloudIdeaCount} ideas"
                            else "${personalConflict?.cloudCount ?: 0} Personal Reminders", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                        if (personalConflict != null) Text("Choosing a copy replaces Personal Reminders and completion history only; Creator work is untouched.", color = MutedText, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = ::resolveRestoreCloud,
                    enabled = (conflict?.cloudRevision ?: personalConflict?.cloudRevision ?: 0L) > 0L,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("USE CLOUD COPY", fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = ::resolveKeepPhone,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) { Text("KEEP PHONE COPY", color = ProjectorIvory, fontWeight = FontWeight.Bold) }
            } else {
                error?.let {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Couldn't check your backup", color = ProjectorIvory, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(5.dp))
                            Text(it, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = ::runSync,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("TRY AGAIN", fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        recoveryMarker.markRecoveryReviewed(session.email)
                        onStartNew()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) { Text("KEEP USING THIS PHONE", color = MutedText) }
            }
        }
    }
}
