package com.framebynavin.app.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class QuickIdeaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme {
                QuickIdeaScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
private fun QuickIdeaScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { IdeaVaultStore(context.applicationContext) }
    var title by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Quick Idea", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory) }
            }

            Spacer(Modifier.height(30.dp))
            Text("Catch it before it disappears.", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Text("It lands in Idea Vault as an Inbox idea. You can shape it properly later.", color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(180) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("Movie, scene, thought, hook…", color = MutedText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ProjectorIvory,
                    unfocusedTextColor = ProjectorIvory,
                    focusedBorderColor = RecRed,
                    unfocusedBorderColor = CinemaLine,
                    cursorColor = RecRed,
                ),
                shape = RoundedCornerShape(18.dp),
            )

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val clean = title.trim()
                    if (clean.isBlank() || saving) return@Button
                    saving = true
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val current = store.load()
                                val idea = CreatorIdea(id = UUID.randomUUID().toString(), title = clean)
                                store.save(listOf(idea) + current)
                            }
                        }.onSuccess {
                            Toast.makeText(context, "Saved to Idea Vault", Toast.LENGTH_SHORT).show()
                            onClose()
                        }.onFailure {
                            saving = false
                            Toast.makeText(context, "Couldn't save idea", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = title.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed, disabledContainerColor = Color(0xFF402424)),
                shape = RoundedCornerShape(17.dp),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                else Text("SAVE IDEA", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
