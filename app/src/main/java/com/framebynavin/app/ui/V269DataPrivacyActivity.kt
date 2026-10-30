package com.framebynavin.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.CloudOperationResult
import com.framebynavin.app.cloud.CloudSyncActivity
import com.framebynavin.app.cloud.CloudSyncManager
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Build269 owner-preview privacy hub. This does NOT submit an account-deletion
 * request: the verified server-side auth/storage deletion pipeline and public
 * request webpage are not live. No unverified privacy-policy link is shown.
 */
class V269DataPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme { V269DataPrivacyScreen(onClose = ::finish) }
        }
    }
}

internal object V269DataPrivacyPolicy {
    const val CREATOR_SCOPE = "Creator exports cover supported projects and app-owned recordings. Keep original project files and external media separately."
    const val PERSONAL_SCOPE = "Personal Reminders use a separate backup. The exported JSON contains reminder names and notes and is not encrypted."
    const val CLOUD_SCOPE = "Signed-in Creator backup may use Supabase cloud storage. Signing out does not erase stored cloud copies or the work on this phone."
    const val DELETION_STATUS = "Full Backlot account deletion is not available in this preview. No deletion request can be submitted here. Signing out or deleting a Creator backup is not the same as deleting an account."
    const val NO_DELETION_SENT = "No account deletion request was sent."
}

@Composable
internal fun V269DataPrivacyScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context) { CloudSyncManager(context.applicationContext) }
    var session by remember { mutableStateOf(manager.localState().session) }
    var signingOut by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize().testTag("v269-privacy-root"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, "Back to Backlot", tint = ProjectorIvory)
                }
                Column(Modifier.weight(1f)) {
                    Text("YOUR DATA", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Data & Privacy", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Outlined.Shield, null, tint = MutedGold)
            }

            V269PrivacyCard("Your data stays yours") {
                Text(
                    "Backlot keeps your projects and personal reminders on this device. Optional connected features may send Creator information to their providers.",
                    color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                )
            }

            V269PrivacyCard("Back up before changing devices") {
                Text(V269DataPrivacyPolicy.CREATOR_SCOPE, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(context, BackupActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth().testTag("v269-creator-backup"),
                ) {
                    Icon(Icons.Outlined.FolderOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text("CREATOR BACKUP")
                }
                Spacer(Modifier.height(6.dp))
                Text(V269DataPrivacyPolicy.PERSONAL_SCOPE, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(context, PersonalBackupActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth().testTag("v269-personal-backup"),
                ) {
                    Icon(Icons.Outlined.Lock, null)
                    Spacer(Modifier.width(8.dp))
                    Text("PERSONAL BACKUP")
                }
            }

            V269PrivacyCard("Connected account & cloud") {
                Text(
                    session?.email?.let { "Connected: $it" } ?: "No Creator cloud account is connected.",
                    color = ProjectorIvory, fontSize = 12.sp,
                )
                Text(V269DataPrivacyPolicy.CLOUD_SCOPE, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(context, CloudSyncActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth().testTag("v269-cloud-account"),
                ) {
                    Icon(Icons.Outlined.Cloud, null)
                    Spacer(Modifier.width(8.dp))
                    Text("MANAGE CREATOR CLOUD")
                }
                if (session != null) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = { showSignOutConfirmation = true },
                        enabled = !signingOut,
                        modifier = Modifier.fillMaxWidth().testTag("v269-sign-out"),
                    ) {
                        Icon(Icons.Outlined.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SIGN OUT (KEEP LOCAL DATA)")
                    }
                }
            }

            V269PrivacyCard("Delete Backlot account") {
                Text(
                    V269DataPrivacyPolicy.DELETION_STATUS,
                    color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.testTag("v269-deletion-unavailable"),
                )
                Text(
                    "Your Google account is separate. Personal Reminders and separately exported files are not deleted by a Creator cloud operation.",
                    color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp,
                )
            }

            if (message != null) {
                Text(message.orEmpty(), color = ProjectorIvory, fontSize = 12.sp,
                    modifier = Modifier.testTag("v269-privacy-message"))
            }
        }
    }

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!signingOut) showSignOutConfirmation = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Sign out of Creator cloud?", color = ProjectorIvory) },
            text = {
                Text(
                    "Your local Creator work and Personal Reminders remain. Signing out does not delete the account, cloud backups, or Google account.",
                    color = MutedText,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !signingOut,
                    onClick = {
                        if (signingOut) return@TextButton
                        signingOut = true
                        scope.launch {
                            val result = manager.signOut()
                            message = when (result) {
                                is CloudOperationResult.Success -> result.message
                                is CloudOperationResult.Failure -> result.message
                                is CloudOperationResult.Skipped -> result.message
                            }
                            session = manager.localState().session
                            showSignOutConfirmation = false
                            signingOut = false
                        }
                    },
                ) { Text("SIGN OUT", color = RecRed) }
            },
            dismissButton = {
                TextButton(enabled = !signingOut, onClick = { showSignOutConfirmation = false }) {
                    Text("CANCEL", color = MutedText)
                }
            },
        )
    }
}

@Composable
private fun V269PrivacyCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
