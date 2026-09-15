package com.backlot.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.backlot.desktop.data.BacklotDesktopStore
import com.backlot.desktop.model.CreatorProject
import com.backlot.desktop.model.DesktopDestination
import com.backlot.desktop.model.DesktopSnapshot
import com.backlot.desktop.model.Idea
import com.backlot.desktop.model.ProjectStage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val BacklotNight = Color(0xFF070A0F)
private val BacklotPanel = Color(0xFF10151D)
private val BacklotPanelRaised = Color(0xFF151C26)
private val BacklotBorder = Color(0xFF253040)
private val BacklotGold = Color(0xFFF0B35D)
private val BacklotBlue = Color(0xFF72AFFF)
private val BacklotIvory = Color(0xFFF5F1E8)
private val BacklotMuted = Color(0xFF8E9AAA)
private val BacklotGreen = Color(0xFF73D2A6)

private val backlotColors = darkColorScheme(
    primary = BacklotGold,
    secondary = BacklotBlue,
    background = BacklotNight,
    surface = BacklotPanel,
    surfaceVariant = BacklotPanelRaised,
    onPrimary = Color(0xFF1A1208),
    onBackground = BacklotIvory,
    onSurface = BacklotIvory,
)

@Composable
fun BacklotDesktopApp() {
    val store = remember { BacklotDesktopStore() }
    val initial = remember { store.load() }
    val ideas = remember { mutableStateListOf<Idea>().apply { addAll(initial.ideas) } }
    val projects = remember { mutableStateListOf<CreatorProject>().apply { addAll(initial.projects) } }

    var destination by remember { mutableStateOf(DesktopDestination.TODAY) }
    var showIdeaCapture by remember { mutableStateOf(false) }
    var showProjectCapture by remember { mutableStateOf(false) }

    fun persist() {
        store.save(DesktopSnapshot(ideas = ideas.toList(), projects = projects.toList()))
    }

    MaterialTheme(colorScheme = backlotColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = BacklotNight) {
            Row(Modifier.fillMaxSize()) {
                BacklotSidebar(
                    destination = destination,
                    onDestination = { destination = it },
                    onQuickIdea = { showIdeaCapture = true },
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(BacklotBorder),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0A0E14), BacklotNight),
                            )
                        ),
                ) {
                    BacklotTopBar(
                        destination = destination,
                        onNewIdea = { showIdeaCapture = true },
                        onNewProject = { showProjectCapture = true },
                    )

                    when (destination) {
                        DesktopDestination.TODAY -> TodayPage(
                            ideas = ideas,
                            projects = projects,
                            onNewIdea = { showIdeaCapture = true },
                            onNewProject = { showProjectCapture = true },
                            onAdvanceProject = { id ->
                                val index = projects.indexOfFirst { it.id == id }
                                if (index >= 0) {
                                    projects[index] = projects[index].advance()
                                    persist()
                                }
                            },
                        )

                        DesktopDestination.IDEAS -> IdeasPage(
                            ideas = ideas,
                            onNewIdea = { showIdeaCapture = true },
                            onPromote = { idea ->
                                projects += CreatorProject(
                                    title = idea.title,
                                    note = idea.note,
                                    stage = ProjectStage.RESEARCH,
                                )
                                ideas.removeAll { it.id == idea.id }
                                persist()
                            },
                        )

                        DesktopDestination.PROJECTS -> ProjectsPage(
                            projects = projects,
                            onNewProject = { showProjectCapture = true },
                            onAdvanceProject = { id ->
                                val index = projects.indexOfFirst { it.id == id }
                                if (index >= 0) {
                                    projects[index] = projects[index].advance()
                                    persist()
                                }
                            },
                        )

                        DesktopDestination.CALENDAR -> CalendarPage(projects)
                        DesktopDestination.INSIGHTS -> InsightsPage(ideas, projects)
                    }
                }
            }
        }

        if (showIdeaCapture) {
            IdeaCaptureDialog(
                onDismiss = { showIdeaCapture = false },
                onSave = { title, note ->
                    ideas += Idea(title = title.trim(), note = note.trim())
                    persist()
                    showIdeaCapture = false
                },
            )
        }

        if (showProjectCapture) {
            ProjectCaptureDialog(
                onDismiss = { showProjectCapture = false },
                onSave = { title, dueDate, note ->
                    projects += CreatorProject(
                        title = title.trim(),
                        dueDate = dueDate.trim(),
                        note = note.trim(),
                    )
                    persist()
                    showProjectCapture = false
                },
            )
        }
    }
}

@Composable
private fun BacklotSidebar(
    destination: DesktopDestination,
    onDestination: (DesktopDestination) -> Unit,
    onQuickIdea: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(248.dp)
            .fillMaxHeight()
            .background(Color(0xFF090D13))
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.linearGradient(listOf(BacklotGold, Color(0xFFD47729)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("B", color = Color(0xFF171008), fontWeight = FontWeight.Black, fontSize = 21.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("BACKLOT", color = BacklotIvory, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("CREATE WHAT'S NEXT", color = BacklotGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(30.dp))
        Text("WORKSPACE", color = BacklotMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        DesktopDestination.entries.forEach { item ->
            SidebarItem(
                label = item.label,
                selected = destination == item,
                onClick = { onDestination(item) },
            )
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onQuickIdea,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = BacklotGold, contentColor = Color(0xFF171008)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("+  QUICK IDEA", fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BacklotPanel)
                .padding(14.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(BacklotGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("LOCAL-FIRST", color = BacklotIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your desktop workspace is saved privately on this computer.",
                    color = BacklotMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Desktop v0.1 · Backlot v134", color = Color(0xFF596475), fontSize = 9.sp)
    }
}

@Composable
private fun SidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color(0xFF18202B) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(6.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(if (selected) BacklotGold else Color(0xFF465264))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = if (selected) BacklotIvory else BacklotMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun BacklotTopBar(
    destination: DesktopDestination,
    onNewIdea: () -> Unit,
    onNewProject: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(horizontal = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(destination.label.uppercase(), color = BacklotIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                color = BacklotMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onNewIdea,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("CAPTURE IDEA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onNewProject,
            colors = ButtonDefaults.buttonColors(containerColor = BacklotBlue, contentColor = Color(0xFF07101D)),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("NEW PROJECT", fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BacklotBorder))
}

@Composable
private fun TodayPage(
    ideas: List<Idea>,
    projects: List<CreatorProject>,
    onNewIdea: () -> Unit,
    onNewProject: () -> Unit,
    onAdvanceProject: (String) -> Unit,
) {
    val active = projects.filter { it.stage != ProjectStage.PUBLISHED }
    val ready = projects.count { it.stage == ProjectStage.READY }
    val published = projects.count { it.stage == ProjectStage.PUBLISHED }

    PageScroll {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF171611), Color(0xFF111923), Color(0xFF0D1219))
                    )
                )
                .padding(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR CREATIVE SPACE", color = BacklotGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Make the next thing move.", color = BacklotIvory, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (active.isEmpty()) "Start with an idea or create your next project."
                        else "${active.size} active project${if (active.size == 1) "" else "s"}. Keep one moving forward today.",
                        color = BacklotMuted,
                        fontSize = 13.sp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = onNewProject,
                        colors = ButtonDefaults.buttonColors(containerColor = BacklotGold, contentColor = Color(0xFF171008)),
                        shape = RoundedCornerShape(11.dp),
                    ) {
                        Text("START A PROJECT", fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onNewIdea) { Text("Capture a quick idea") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("IDEAS", ideas.size.toString(), "waiting in your vault", Modifier.weight(1f))
            MetricCard("ACTIVE", active.size.toString(), "projects in motion", Modifier.weight(1f))
            MetricCard("READY", ready.toString(), "ready to publish", Modifier.weight(1f))
            MetricCard("PUBLISHED", published.toString(), "finished projects", Modifier.weight(1f))
        }

        Spacer(Modifier.height(26.dp))
        SectionHeader("IN MOTION", "Your current creator pipeline")
        Spacer(Modifier.height(12.dp))

        if (active.isEmpty()) {
            EmptyPanel(
                title = "Nothing is in motion yet",
                body = "Create a project and Backlot will keep its stage, deadline and progress visible here.",
                action = "CREATE PROJECT",
                onAction = onNewProject,
            )
        } else {
            active.take(6).forEach { project ->
                ProjectCard(project = project, onAdvance = { onAdvanceProject(project.id) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun IdeasPage(
    ideas: List<Idea>,
    onNewIdea: () -> Unit,
    onPromote: (Idea) -> Unit,
) {
    PageScroll {
        SectionHeader("IDEA VAULT", "Capture first. Decide what deserves production later.")
        Spacer(Modifier.height(16.dp))

        if (ideas.isEmpty()) {
            EmptyPanel(
                title = "Your vault is clear",
                body = "Drop in hooks, video concepts, story fragments or anything worth remembering.",
                action = "CAPTURE IDEA",
                onAction = onNewIdea,
            )
        } else {
            ideas.forEach { idea ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BacklotPanel),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(idea.title, color = BacklotIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (idea.note.isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    idea.note,
                                    color = BacklotMuted,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(onClick = { onPromote(idea) }, shape = RoundedCornerShape(10.dp)) {
                            Text("TURN INTO PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ProjectsPage(
    projects: List<CreatorProject>,
    onNewProject: () -> Unit,
    onAdvanceProject: (String) -> Unit,
) {
    PageScroll {
        SectionHeader("PROJECTS", "Every piece of content from first move to published.")
        Spacer(Modifier.height(16.dp))

        if (projects.isEmpty()) {
            EmptyPanel(
                title = "No projects yet",
                body = "Start one directly or promote an idea from your Idea Vault.",
                action = "NEW PROJECT",
                onAction = onNewProject,
            )
        } else {
            projects
                .sortedWith(compareBy<CreatorProject> { it.stage == ProjectStage.PUBLISHED }.thenByDescending { it.updatedAt })
                .forEach { project ->
                    ProjectCard(
                        project = project,
                        onAdvance = if (project.stage == ProjectStage.PUBLISHED) null else { { onAdvanceProject(project.id) } },
                    )
                    Spacer(Modifier.height(10.dp))
                }
        }
    }
}

@Composable
private fun CalendarPage(projects: List<CreatorProject>) {
    val datedProjects = projects.mapNotNull { project ->
        project.dueDateOrNull()?.let { date -> date to project }
    }.sortedBy { it.first }

    PageScroll {
        SectionHeader("CONTENT CALENDAR", "Deadlines across your active creator work.")
        Spacer(Modifier.height(16.dp))

        if (datedProjects.isEmpty()) {
            EmptyPanel(
                title = "No deadlines on the board",
                body = "Add a YYYY-MM-DD deadline when creating a project and it will appear here.",
                action = null,
                onAction = {},
            )
        } else {
            datedProjects.forEach { (date, project) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(BacklotPanel)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(94.dp)) {
                        Text(date.dayOfMonth.toString(), color = BacklotGold, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text(date.month.name.take(3), color = BacklotMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.width(1.dp).height(42.dp).background(BacklotBorder))
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(project.title, color = BacklotIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(project.stage.label, color = BacklotMuted, fontSize = 11.sp)
                    }
                    StagePill(project.stage)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun InsightsPage(ideas: List<Idea>, projects: List<CreatorProject>) {
    val published = projects.count { it.stage == ProjectStage.PUBLISHED }
    val completionRate = if (projects.isEmpty()) 0 else ((published.toFloat() / projects.size) * 100).toInt()
    val stageCounts = ProjectStage.entries.associateWith { stage -> projects.count { it.stage == stage } }
    val activeStageCounts = stageCounts.filterKeys { it != ProjectStage.PUBLISHED }
    val maxStageCount = maxOf(1, activeStageCounts.values.maxOrNull() ?: 1)
    val bottleneck = activeStageCounts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key

    PageScroll {
        SectionHeader("CREATOR INSIGHTS", "Useful signals from your own workflow — not vanity analytics.")
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("IDEA BANK", ideas.size.toString(), "unpromoted ideas", Modifier.weight(1f))
            MetricCard("PROJECTS", projects.size.toString(), "tracked end-to-end", Modifier.weight(1f))
            MetricCard("COMPLETION", "$completionRate%", "projects published", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BacklotPanel),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("WORKFLOW SHAPE", color = BacklotIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(
                    bottleneck?.let { "Most active work is currently sitting in ${it.label}." }
                        ?: "Create a few projects and Backlot will begin showing where work piles up.",
                    color = BacklotMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(18.dp))

                ProjectStage.entries.filter { it != ProjectStage.PUBLISHED }.forEach { stage ->
                    val count = stageCounts[stage] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stage.label, color = BacklotMuted, fontSize = 11.sp, modifier = Modifier.width(82.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF202A37))
                        ) {
                            if (count > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(count.toFloat() / maxStageCount.toFloat())
                                        .fillMaxHeight()
                                        .background(if (stage == bottleneck) BacklotGold else BacklotBlue)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(count.toString(), color = BacklotIvory, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                    }
                    Spacer(Modifier.height(11.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121923)),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("WHAT BACKLOT IS LEARNING", color = BacklotGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(9.dp))
                Text(
                    "This desktop foundation starts with private workflow signals: idea conversion, project stages, deadlines and completion. Cross-device history, YouTube performance and creator postmortems stay reserved for the shared intelligence layer rather than being faked locally.",
                    color = BacklotIvory,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(project: CreatorProject, onAdvance: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BacklotPanel),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.title, color = BacklotIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StagePill(project.stage)
                        if (project.dueDate.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text("Due ${project.dueDate}", color = BacklotMuted, fontSize = 10.sp)
                        }
                    }
                }
                if (onAdvance != null) {
                    Button(
                        onClick = onAdvance,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202B39), contentColor = BacklotIvory),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            if (project.stage == ProjectStage.READY) "PUBLISH" else "MOVE TO ${project.stage.next().label.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { project.stage.progress() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (project.stage == ProjectStage.PUBLISHED) BacklotGreen else BacklotGold,
                trackColor = Color(0xFF202A35),
            )
            if (project.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(project.note, color = BacklotMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StagePill(stage: ProjectStage) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                when (stage) {
                    ProjectStage.PUBLISHED -> Color(0xFF153527)
                    ProjectStage.READY -> Color(0xFF332713)
                    else -> Color(0xFF162338)
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            stage.label.uppercase(),
            color = when (stage) {
                ProjectStage.PUBLISHED -> BacklotGreen
                ProjectStage.READY -> BacklotGold
                else -> BacklotBlue
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BacklotPanel),
        shape = RoundedCornerShape(15.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(label, color = BacklotMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(value, color = BacklotIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(caption, color = Color(0xFF667284), fontSize = 10.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = BacklotIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = BacklotMuted, fontSize = 11.sp)
    }
}

@Composable
private fun EmptyPanel(
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BacklotPanel)
            .padding(28.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(title, color = BacklotIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Text(body, color = BacklotMuted, fontSize = 12.sp)
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onAction, shape = RoundedCornerShape(10.dp)) {
                    Text(action, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PageScroll(content: @Composable Column.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 34.dp, vertical = 28.dp),
        content = content,
    )
}

@Composable
private fun IdeaCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BacklotPanelRaised,
        title = { Text("Capture an idea", color = BacklotIvory, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("Get it out of your head. Shape it later.", color = BacklotMuted, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Idea") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes · optional") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, note) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BacklotGold, contentColor = Color(0xFF171008)),
            ) {
                Text("SAVE IDEA", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProjectCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BacklotPanelRaised,
        title = { Text("Start a project", color = BacklotIvory, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("Give the work a name. Backlot will keep it moving through the pipeline.", color = BacklotMuted, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Deadline · YYYY-MM-DD · optional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes · optional") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, dueDate, note) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BacklotBlue, contentColor = Color(0xFF07101D)),
            ) {
                Text("CREATE PROJECT", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
