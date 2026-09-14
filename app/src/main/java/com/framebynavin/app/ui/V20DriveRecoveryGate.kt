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
                is CreatorCloudSyncResult.Conflict -> {
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
            when (val value = sync.keepThisPhone()) {
                is CreatorCloudSyncResult.Synced,
                is CreatorCloudSyncResult.Restored -> { busy = false; finishRecovery() }
                is CreatorCloudSyncResult.Conflict -> { result = value; busy = false }
                is CreatorCloudSyncResult.Skipped -> { error = value.message; busy = false }
                is CreatorCloudSyncResult.Failure -> { error = value.message; busy = false }
            }
        }
    }

    fun resolveRestoreCloud() {
        busy = true
        error = null
        scope.launch {
            when (val value = sync.restoreCloud()) {
                is CreatorCloudSyncResult.Synced,
                is CreatorCloudSyncResult.Restored -> { busy = false; finishRecovery() }
                is CreatorCloudSyncResult.Conflict -> { result = value; busy = false }
                is CreatorCloudSyncResult.Skipped -> { error = value.message; busy = false }
                is CreatorCloudSyncResult.Failure -> { error = value.message; busy = false }
            }
        }
    }

    LaunchedEffect(session.userId) { runSync() }
    val conflict = result as? CreatorCloudSyncResult.Conflict

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                if (conflict == null) Icons.Outlined.CloudDone else Icons.Outlined.SyncProblem,
                null,
                tint = MutedGold,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (conflict == null) "Recover your creator workspace" else "Choose which workspace to keep",
                color = ProjectorIvory,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(6.dp))
            Text(session.email, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                if (conflict == null)
                    "FrameByNavin checks your private creator cloud automatically. Your phone stays the working copy; cloud recovery only replaces it when that is provably safe."
                else
                    "This phone and your creator cloud both contain different work. Nothing will be overwritten automatically.",
                color = MutedText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(22.dp))

            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = RecRed)
                Spacer(Modifier.height(8.dp))
                Text("Checking creator cloud…", color = MutedText)
            } else if (conflict != null) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = CinemaSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MutedGold.copy(alpha = .5f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("THIS PHONE", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("${conflict.localProjectCount} projects · ${conflict.localIdeaCount} ideas", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("CLOUD", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("${conflict.cloudProjectCount} projects · ${conflict.cloudIdeaCount} ideas", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = ::resolveRestoreCloud,
                    enabled = conflict.cloudRevision > 0L,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("RESTORE CLOUD WORKSPACE", fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = ::resolveKeepPhone,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) { Text("KEEP THIS PHONE'S WORK", color = ProjectorIvory, fontWeight = FontWeight.Bold) }
            } else {
                error?.let {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Creator cloud needs attention", color = ProjectorIvory, fontWeight = FontWeight.Black)
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
                ) { Text("RETRY CLOUD CHECK", fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        recoveryMarker.markRecoveryReviewed(session.email)
                        onStartNew()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) { Text("CONTINUE LOCALLY FOR NOW", color = MutedText) }
            }
        }
    }
}
