package com.pianostudio.alpha

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class StudioHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { QuietConcertStudioTheme { StudioHome() } }
    }
}

@Composable
private fun StudioHome() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Good evening", color = StudioMuted, style = MaterialTheme.typography.bodyMedium)
                Text("Your piano, waiting for you.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier.size(44.dp).background(StudioCarbon, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("P", color = StudioGold, fontWeight = FontWeight.Bold) }
        }

        Text("TODAY'S PRACTICE", color = StudioGold, style = MaterialTheme.typography.labelMedium)
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioCharcoal),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("18 minutes to move forward", style = MaterialTheme.typography.titleLarge)
                        Text("A balanced session built around learning and practice.", color = StudioMuted)
                    }
                    Icon(Icons.Rounded.AutoAwesome, null, tint = StudioGold, modifier = Modifier.size(30.dp))
                }
                SessionRow("Warm up", "3 min")
                SessionRow("Chords", "4 min")
                SessionRow("Lesson", "5 min")
                SessionRow("Smart practice", "6 min")
                Button(
                    onClick = { context.startActivity(Intent(context, PracticeCoachActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Practice")
                }
            }
        }

        Text("CONTINUE", color = StudioGold, style = MaterialTheme.typography.labelMedium)
        StudioNavCard(
            icon = Icons.Rounded.MenuBook,
            title = "Piano Foundations",
            subtitle = "Continue your structured learning journey",
            trailing = "Learn",
            onClick = { context.startActivity(Intent(context, LearningHubActivity::class.java)) },
        )
        StudioNavCard(
            icon = Icons.Rounded.Piano,
            title = "Free Piano",
            subtitle = "Play without a lesson — full landscape keyboard",
            trailing = "Play",
            onClick = { context.startActivity(Intent(context, MainActivity::class.java)) },
        )
        StudioNavCard(
            icon = Icons.Rounded.Tune,
            title = "Smart Practice Coach",
            subtitle = "Accuracy, timing and focused retries",
            trailing = "Practice",
            onClick = { context.startActivity(Intent(context, PracticeCoachActivity::class.java)) },
        )

        Text("THIS WEEK", color = StudioGold, style = MaterialTheme.typography.labelMedium)
        Card(colors = CardDefaults.cardColors(containerColor = StudioCarbon), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Practice time", color = StudioMuted)
                    Text("92 min", fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(progress = { .68f }, modifier = Modifier.fillMaxWidth())
                Text("Focus on consistency, not streak pressure.", color = StudioMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SessionRow(label: String, duration: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = StudioWhite)
        Text(duration, color = StudioMuted)
    }
}

@Composable
private fun StudioNavCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = StudioCarbon),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(StudioCharcoal, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = StudioGold) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = StudioMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(trailing, color = StudioGold, style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Rounded.KeyboardArrowRight, null, tint = StudioMuted)
        }
    }
}
