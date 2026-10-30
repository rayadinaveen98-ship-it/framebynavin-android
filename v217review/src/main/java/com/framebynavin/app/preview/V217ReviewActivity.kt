package com.framebynavin.app.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.CinePulseState
import com.framebynavin.app.ui.V144GuideCharacter
import com.framebynavin.app.ui.V144GuideIdentity
import com.framebynavin.app.ui.V144GuidePicker
import com.framebynavin.app.ui.V144GuidePrefs
import com.framebynavin.app.ui.V217GuideChoiceContent
import com.framebynavin.app.ui.theme.FrameByNavinTheme

/** Isolated, offline phone review that compiles the actual Backlot V217 source.
 *
 * Not the whole Backlot application. This only previews its original guide
 * chooser, Settings selector and 12 semantic states. No creator data is
 * imported, no live reminders or accounts are accessible.
 */
class V217ReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The preview is another package with its OWN empty Android sandbox.
        V144GuidePrefs.initialize(applicationContext)
        setContent {
            FrameByNavinTheme {
                var screen by remember { mutableStateOf("choose") }
                var state by remember { mutableStateOf(CinePulseState.IDLE) }
                if (screen == "choose") {
                    // EXACT same composable as the full Backlot first-run gate.
                    V217GuideChoiceContent(onSelect = {
                        V144GuidePrefs.select(it)
                        screen = "inspect"
                    })
                } else {
                    CompanionReviewScreen(
                        guide = V144GuidePrefs.currentGuide,
                        state = state,
                        onState = { state = it },
                        onChoose = { screen = "choose" },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanionReviewScreen(
    guide: V144GuideIdentity,
    state: CinePulseState,
    onState: (CinePulseState) -> Unit,
    onChoose: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("BACKLOT  ·  V217 REVIEW", fontSize = 13.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary)
            Text("Side-by-side test app · separate from your real Backlot.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Companion Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            // Exact production Settings companion picker and live renderer.
            V144GuidePicker()
            Text("Selected: ${V144GuidePrefs.currentGuide.displayName}",
                fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(205.dp),
                contentAlignment = Alignment.Center) {
                V144GuideCharacter(
                    guide = V144GuidePrefs.currentGuide,
                    state = state,
                    modifier = Modifier.size(185.dp),
                )
            }
            Text("Semantic state: ${state.name}", fontWeight = FontWeight.Bold)
            Text("These are transitional motions of one base illustration per new guide, not finished nine-pose expression artwork.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Start)
            CinePulseState.entries.chunked(3).forEach { states ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    states.forEach { option ->
                        if (state == option) {
                            Button(onClick = { onState(option) }, modifier = Modifier.weight(1f)) {
                                Text(option.name, fontSize = 9.sp, maxLines = 1)
                            }
                        } else {
                            OutlinedButton(onClick = { onState(option) }, modifier = Modifier.weight(1f)) {
                                Text(option.name, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
                Text("Review first-run chooser")
            }
            Text("Isolated preview only · no reminders, cloud sync, backup or creator data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
