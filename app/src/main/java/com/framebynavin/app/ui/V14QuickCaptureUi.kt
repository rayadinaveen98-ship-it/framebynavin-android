package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorQuickCaptureEngine
import com.framebynavin.app.data.IdeaVaultLabels
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V14QuickCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (CreatorIdea) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val suggestion = remember(title, notes) { CreatorQuickCaptureEngine.suggest("$title $notes") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("QUICK CAPTURE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Text("Save it before it disappears", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }

                Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Idea") },
                        placeholder = { Text("Best Pawan Kalyan interval blocks") },
                        singleLine = false,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        label = { Text("Notes · optional") },
                        placeholder = { Text("Angle, scene, hook or anything worth remembering…") },
                        shape = RoundedCornerShape(16.dp),
                    )

                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, null, tint = MutedGold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("AUTO-SORT TO INBOX", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                                Spacer(Modifier.height(3.dp))
                                Text(IdeaVaultLabels.category(suggestion.category), color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                Text("${suggestion.platformHint} · ${suggestion.formatHint}", color = MutedText, fontSize = 8.8.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("You can refine or convert it to a project later from Idea Vault.", color = MutedText, fontSize = 8.8.sp)
                }

                Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
                    Button(
                        onClick = { onSave(CreatorQuickCaptureEngine.toIdea(title, notes)) },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text("SAVE TO INBOX", fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
