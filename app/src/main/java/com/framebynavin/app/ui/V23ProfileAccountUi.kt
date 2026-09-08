package com.framebynavin.app.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.CloudOperationResult
import com.framebynavin.app.cloud.CloudSyncActivity
import com.framebynavin.app.cloud.CloudSyncManager
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V23ProfileAccountScreen(
    creatorProfile: CreatorProfile,
    onClose: () -> Unit,
    onEditCreatorSetup: () -> Unit,
    onOpenYouTube: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { CloudSyncManager(context.applicationContext) }
    var session by remember { mutableStateOf(manager.localState().session) }
    var accountProfile by remember { mutableStateOf(manager.cachedCreatorProfile()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showIdentityEditor by remember { mutableStateOf(false) }
    var identityName by remember { mutableStateOf("") }
    var identityUsername by remember { mutableStateOf("") }

    fun reload() {
        session = manager.localState().session
        accountProfile = manager.cachedCreatorProfile()
    }

    LaunchedEffect(Unit) {
        if (session != null) {
            manager.refreshCreatorProfile()
            reload()
        }
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Column(Modifier.weight(1f)) {
                    Text("IDENTITY", color = RecRed, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text("Profile & Account", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, MutedGold.copy(alpha = .24f)),
            ) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(52.dp), shape = CircleShape, color = MutedGold.copy(alpha = .13f)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = MutedGold, modifier = Modifier.size(25.dp)) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                accountProfile?.displayName.orEmpty().ifBlank { session?.displayName.orEmpty() }.ifBlank { creatorProfile.safeDisplayName },
                                color = ProjectorIvory,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                            )
                            val username = accountProfile?.username.orEmpty()
                            Text(if (username.isNotBlank()) "@$username" else if (session != null) "Username not set" else "Local creator", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            session?.email?.takeIf { it.isNotBlank() }?.let { Text(it, color = MutedText, fontSize = 12.sp) }
                        }
                    }
                    accountProfile?.createdAtMillis?.takeIf { it > 0L }?.let { joined ->
                        Spacer(Modifier.height(10.dp))
                        Text("Joined ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(joined))}", color = MutedText, fontSize = 12.sp)
                    }
                    if (session != null) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                identityName = accountProfile?.displayName.orEmpty().ifBlank { session?.displayName.orEmpty() }
                                identityUsername = accountProfile?.username.orEmpty()
                                showIdentityEditor = true
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (accountProfile?.username.isNullOrBlank()) "CREATE CREATOR ID" else "EDIT CREATOR ID", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            V23SectionTitle("CREATOR SETUP", "These choices drive project formats, workflows and planning defaults.")
            Spacer(Modifier.height(9.dp))
            V23InfoRow("Creator type", creatorProfile.category.ifBlank { "Not set" }, Icons.Outlined.AutoAwesome)
            V23InfoRow("Platforms", creatorProfile.platforms.sorted().joinToString().ifBlank { "Not set" }, Icons.Outlined.Hub)
            V23InfoRow("Primary goal", creatorProfile.primaryGoal.ifBlank { "Not set" }, Icons.Outlined.TrackChanges)
            V23InfoRow("Publishing target", "${creatorProfile.weeklyPublishingTarget} / week", Icons.Outlined.CalendarMonth)
            OutlinedButton(onClick = onEditCreatorSetup, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, CinemaLine)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("EDIT CREATOR SETUP", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
            V23SectionTitle("CONNECTED SERVICES", "Accounts that add backup or creator performance data.")
            Spacer(Modifier.height(9.dp))
            V23ActionRow(
                title = if (session == null) "Google account" else "Google account connected",
                subtitle = if (session == null) "Sign in, claim a username and create manual backups" else session?.email.orEmpty(),
                icon = Icons.Outlined.AccountCircle,
                onClick = { context.startActivity(Intent(context, CloudSyncActivity::class.java)) },
            )
            V23ActionRow(
                title = "YouTube",
                subtitle = "Channel analytics and performance intelligence",
                icon = Icons.Outlined.SmartDisplay,
                onClick = onOpenYouTube,
            )

            Spacer(Modifier.height(24.dp))
            V23SectionTitle("ACCOUNT & DATA", "FrameByNavin stays offline-first even when an account is connected.")
            Spacer(Modifier.height(9.dp))
            V23ActionRow("Cloud Backup", "Manual restore points and safe recovery", Icons.Outlined.CloudSync) {
                context.startActivity(Intent(context, CloudSyncActivity::class.java))
            }

            if (session != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        if (busy) return@OutlinedButton
                        busy = true
                        scope.launch {
                            val result = manager.signOut()
                            message = when (result) {
                                is CloudOperationResult.Success -> result.message
                                is CloudOperationResult.Skipped -> result.message
                                is CloudOperationResult.Failure -> result.message
                            }
                            busy = false
                            reload()
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Icon(Icons.Outlined.Logout, null, tint = MutedText, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SIGN OUT", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
            }
        }
    }


    if (showIdentityEditor) {
        AlertDialog(
            onDismissRequest = { if (!busy) showIdentityEditor = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Creator identity", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Your username is unique. Changing it does not make your projects public.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = identityName,
                        onValueChange = { identityName = it.take(40) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Display name") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = identityUsername,
                        onValueChange = { raw -> identityUsername = raw.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(24) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        prefix = { Text("@") },
                        supportingText = { Text("3-24 letters, numbers or underscore") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        scope.launch {
                            when (val result = manager.claimUsername(identityUsername, identityName)) {
                                is CloudOperationResult.Success -> {
                                    manager.refreshCreatorProfile()
                                    reload()
                                    message = result.message
                                    showIdentityEditor = false
                                }
                                is CloudOperationResult.Skipped -> message = result.message
                                is CloudOperationResult.Failure -> message = result.message
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && identityUsername.length in 3..24,
                ) { Text("SAVE") }
            },
            dismissButton = { TextButton(onClick = { showIdentityEditor = false }, enabled = !busy) { Text("CANCEL", color = MutedText) } },
        )
    }

}

@Composable
private fun V23SectionTitle(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
}

@Composable
private fun V23InfoRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 7.dp), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MutedGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, color = MutedText, fontSize = 9.sp, modifier = Modifier.weight(.8f))
            Text(value, color = ProjectorIvory, fontSize = 9.3.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun V23ActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp), shape = RoundedCornerShape(17.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MutedGold, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText, fontSize = 12.sp, lineHeight = 11.sp)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}
