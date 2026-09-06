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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class LearningHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { QuietConcertStudioTheme { LearningHub(onBack = ::finish) } }
    }
}

@Composable
private fun LearningHub(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LessonProgressStore(context.applicationContext) }
    val progress by store.progress.collectAsState(initial = emptyMap())
    val lessons = LessonCatalog.foundations
    val completedCount = lessons.count { progress[it.id]?.completed == true }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(StudioBlack).systemBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                Column {
                    Text("Learn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("Build real musicianship.", color = StudioMuted)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = StudioCharcoal), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CURRENT COURSE", color = StudioGold, style = MaterialTheme.typography.labelMedium)
                    Text("Piano Foundations", style = MaterialTheme.typography.titleLarge)
                    Text("One clear path. Explore when you want.", color = StudioMuted)
                    LinearProgressIndicator(
                        progress = { if (lessons.isEmpty()) 0f else completedCount.toFloat() / lessons.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("$completedCount of ${lessons.size} lessons complete", color = StudioMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("YOUR JOURNEY", color = StudioGold, style = MaterialTheme.typography.labelMedium) }
        itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
            val p = progress[lesson.id] ?: LessonProgress()
            val unlocked = index == 0 || progress[lessons[index - 1].id]?.completed == true
            CourseLessonCard(
                number = index + 1,
                lesson = lesson,
                progress = p,
                unlocked = unlocked,
                onClick = {
                    if (unlocked) {
                        context.startActivity(
                            Intent(context, LandscapeLessonActivity::class.java)
                                .putExtra(LandscapeLessonActivity.EXTRA_LESSON_ID, lesson.id),
                        )
                    }
                },
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Progress without pressure", style = MaterialTheme.typography.titleMedium)
            Text(
                "Mastery, useful feedback and real musical milestones — no XP explosions or streak anxiety.",
                color = StudioMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CourseLessonCard(
    number: Int,
    lesson: PianoLesson,
    progress: LessonProgress,
    unlocked: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = unlocked, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (unlocked) StudioCarbon else StudioCharcoal.copy(alpha = .6f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(StudioCharcoal, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    progress.completed -> Icon(Icons.Rounded.CheckCircle, null, tint = StudioSuccess)
                    !unlocked -> Icon(Icons.Rounded.Lock, null, tint = StudioMuted)
                    else -> Text(number.toString(), color = StudioGold, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, color = if (unlocked) StudioWhite else StudioMuted)
                Text(lesson.subtitle, color = StudioMuted, style = MaterialTheme.typography.bodySmall)
                val status = when {
                    progress.completed -> "Complete · best ${progress.bestAccuracy}%"
                    progress.stepIndex > 0 -> "Continue · step ${progress.stepIndex + 1}/${lesson.steps.size}"
                    else -> "${lesson.minutes} min"
                }
                Text(status, color = if (progress.completed) StudioSuccess else StudioMuted, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Rounded.MusicNote, null, tint = if (unlocked) StudioGold else StudioMuted)
        }
    }
}
