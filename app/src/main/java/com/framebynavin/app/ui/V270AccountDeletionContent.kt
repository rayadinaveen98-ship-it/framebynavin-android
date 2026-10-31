package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.BacklotAccountDeletionApi
import com.framebynavin.app.cloud.BacklotDeletionReleaseGate
import com.framebynavin.app.cloud.BacklotDeletionResponse
import com.framebynavin.app.cloud.BacklotDeletionReceipt
import com.framebynavin.app.cloud.CloudSession
import com.framebynavin.app.cloud.CloudSyncManager
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Future in-app account deletion. The production switch is OFF in the current
 * candidate; no signed-in account or remote endpoint can be contacted through
 * this component until the backend and external deletion path are accepted.
 *
 * No automatic sign-out, local wipe, Auth deletion or false success message.
 */
@Composable
internal fun V270AccountDeletionContent(manager: CloudSyncManager, session: CloudSession?) {
    val api = remember { BacklotAccountDeletionApi() }
    val scope = rememberCoroutineScope()
    var checking by remember(session?.userId) { mutableStateOf(true) }
    var available by remember(session?.userId) { mutableStateOf(false) }
    var record by remember(session?.userId) { mutableStateOf<BacklotDeletionReceipt?>(null) }
    var confirmation by remember(session?.userId) { mutableStateOf("") }
    var confirmDialog by remember(session?.userId) { mutableStateOf(false) }
    var submitting by remember(session?.userId) { mutableStateOf(false) }
    var notice by remember(session?.userId) { mutableStateOf<String?>(null) }

    LaunchedEffect(session?.userId) {
        checking = true
        available = false
        record = null
        confirmation = ""
        confirmDialog = false
        notice = null
        if (session == null || !BacklotDeletionReleaseGate.LIVE_REQUESTS_ENABLED) {
            checking = false
            return@LaunchedEffect
        }
        try {
            record = api.status(manager)
            available = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            available = false
        } finally {
            checking = false
        }
    }

    if (session == null) {
        Text("Connect your Creator cloud account to view account-deletion options.",
            color = MutedText, fontSize = 12.sp)
        return
    }
    Text(
        "Account deletion removes the Backlot cloud account, Creator profile, cloud backups and app-owned Voice Ideas. It does not delete your Google account, YouTube channel, local Personal Reminders, or separately exported files.",
        color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
    )
    if (checking) {
        Text("Checking secure deletion service…", color = MutedText, fontSize = 12.sp)
        return
    }
    if (!available) {
        Text("Account deletion requests are not available yet. No request has been sent.",
            color = MutedGold, fontSize = 12.sp,
            modifier = Modifier.testTag("v270-deletion-unavailable"))
        return
    }
    if (record != null) {
        Text("Deletion request: ${record?.status.orEmpty()}. This is a status update, not confirmation that your account has been deleted.",
            color = MutedGold, fontSize = 12.sp,
            modifier = Modifier.testTag("v270-deletion-status"))
        return
    }
    Text("Export Creator and Personal backups separately before making an irreversible request. A fresh Google sign-in may be required.",
        color = MutedGold, fontSize = 12.sp, lineHeight = 18.sp)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = confirmation,
        onValueChange = { confirmation = it },
        label = { Text("Type DELETE MY BACKLOT ACCOUNT") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("v270-deletion-phrase"),
    )
    OutlinedButton(
        onClick = { confirmDialog = true },
        enabled = !submitting && confirmation == BacklotDeletionReleaseGate.CONFIRMATION,
        modifier = Modifier.fillMaxWidth().testTag("v270-deletion-review"),
    ) { Text("REVIEW DELETION REQUEST") }
    notice?.let {
        Text(it, color = MutedGold, fontSize = 12.sp, modifier = Modifier.testTag("v270-deletion-notice"))
    }
    if (confirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!submitting) confirmDialog = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Request permanent cloud deletion?", color = ProjectorIvory) },
            text = { Text("Backlot will request deletion of this signed-in cloud account and associated Creator data. Local files are not automatically wiped. This cannot be undone after processing.", color = MutedText) },
            confirmButton = {
                TextButton(
                    enabled = !submitting && confirmation == BacklotDeletionReleaseGate.CONFIRMATION,
                    onClick = {
                        if (submitting) return@TextButton
                        submitting = true
                        scope.launch {
                            try {
                                val response = api.request(manager, confirmation)
                                record = response
                                notice = "Request received by the deletion service; status: ${response.status}. No claim of completed deletion is made."
                                confirmation = ""
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                notice = "Could not verify or submit this request. Nothing is confirmed deleted. Please sign in again or check the service status."
                            } finally {
                                submitting = false
                                confirmDialog = false
                            }
                        }
                    },
                ) { Text("REQUEST DELETION", color = RecRed) }
            },
            dismissButton = {
                TextButton(enabled = !submitting, onClick = { confirmDialog = false }) {
                    Text("CANCEL", color = MutedText)
                }
            },
        )
    }
}
