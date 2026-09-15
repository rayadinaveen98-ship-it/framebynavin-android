package com.backlot.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.util.Locale

private val CinemaBlack = Color(0xFF070707)
private val CinemaSurface = Color(0xFF101010)
private val CinemaSurfaceRaised = Color(0xFF151515)
private val CinemaLine = Color(0xFF292929)
private val ProjectorIvory = Color(0xFFF3EFE7)
private val MutedText = Color(0xFF918C85)
private val RecRed = Color(0xFFFF3D3D)
private val RecRedDeep = Color(0xFF311010)
private val MutedGold = Color(0xFFD8B56B)
private val SuccessGreen = Color(0xFF6BAF83)
private val CoolBlue = Color(0xFF4B86C6)

private val backlotColors = darkColorScheme(
    primary = RecRed,
    secondary = MutedGold,
    background = CinemaBlack,
    surface = CinemaSurface,
    surfaceVariant = CinemaSurfaceRaised,
    outline = CinemaLine,
    onPrimary = ProjectorIvory,
    onBackground = ProjectorIvory,
    onSurface = ProjectorIvory,
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
    var showControl by remember { mutableStateOf(false) }

    fun persist() {
        store.save(DesktopSnapshot(ideas = ideas.toList(), projects = projects.toList()))
    }

    fun advanceProject(id: String) {
        val index = projects.indexOfFirst { it.id == id }
        if (index >= 0) {
            projects[index] = projects[index].advance()
            persist()
        }
    }

    MaterialTheme(colorScheme = backlotColors) {
        Box(Modifier.fillMaxSize().background(CinemaBlack)) {
            BacklotBackdrop(Modifier.matchParentSize())

            Column(Modifier.fillMaxSize()) {
                DesktopBrandHeader(
                    projects = projects,
                    onCaptureIdea = { showIdeaCapture = true },
                    onNewProject = { showProjectCapture = true },
                )

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (destination) {
                        DesktopDestination.TODAY -> TodayControlRoom(
                            ideas = ideas,
                            projects = projects,
                            onNewIdea = { showIdeaCapture = true },
                            onNewProject = { showProjectCapture = true },
                            onAdvanceProject = ::advanceProject,
                            onOpenIdeas = { destination = DesktopDestination.IDEAS },
                            onOpenCreate = { destination = DesktopDestination.PROJECTS },
                            onOpenCalendar = { destination = DesktopDestination.CALENDAR },
                            onOpenInsights = { destination = DesktopDestination.INSIGHTS },
                        )

                        DesktopDestination.IDEAS -> IdeaVaultRoom(
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
                                destination = DesktopDestination.PROJECTS
                            },
                        )

                        DesktopDestination.PROJECTS -> StudioRoom(
                            projects = projects,
                            onNewProject = { showProjectCapture = true },
                            onAdvanceProject = ::advanceProject,
                        )

                        DesktopDestination.CALENDAR -> CalendarRoom(projects)
                        DesktopDestination.INSIGHTS -> InsightsRoom(ideas, projects)
                    }
                }

                CreatorDock(
                    selected = destination,
                    onSelect = { destination = it },
                    onCapture = { showProjectCapture = true },
                )
            }

            Surface(
                onClick = { showControl = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 28.dp),
                shape = RoundedCornerShape(18.dp),
                color = RecRed,
                shadowElevation = 12.dp,
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("◫", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(7.dp))
                    Text("CONTROL", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
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

        if (showControl) {
            ControlRoomDialog(
                onDismiss = { showControl = false },
                onIdea = { showControl = false; showIdeaCapture = true },
                onProject = { showControl = false; showProjectCapture = true },
                onCalendar = { showControl = false; destination = DesktopDestination.CALENDAR },
                onInsights = { showControl = false; destination = DesktopDestination.INSIGHTS },
            )
        }
    }
}

@Composable
private fun BacklotBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF080808), CinemaBlack, Color(0xFF050505)),
            )
        )
        drawCircle(
            Brush.radialGradient(
                listOf(RecRed.copy(alpha = .12f), RecRedDeep.copy(alpha = .07f), Color.Transparent),
                center = Offset(size.width * .15f, size.height * .08f),
                radius = size.width * .58f,
            ),
            radius = size.width * .58f,
            center = Offset(size.width * .15f, size.height * .08f),
        )
        drawCircle(
            Brush.radialGradient(
                listOf(MutedGold.copy(alpha = .045f), Color.Transparent),
                center = Offset(size.width * .84f, size.height * .86f),
                radius = size.width * .44f,
            ),
            radius = size.width * .44f,
            center = Offset(size.width * .84f, size.height * .86f),
        )
    }
}

@Composable
private fun DesktopBrandHeader(
    projects: List<CreatorProject>,
    onCaptureIdea: () -> Unit,
    onNewProject: () -> Unit,
) {
    val attention = projects.count { project ->
        project.stage != ProjectStage.PUBLISHED &&
            project.dueDateOrNull()?.let { !it.isAfter(LocalDate.now()) } == true
    }

    Surface(color = CinemaBlack.copy(alpha = .94f), border = BorderStroke(0.dp, Color.Transparent)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(94.dp).padding(horizontal = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BacklotLayerMark(Modifier.size(52.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("BACKLOT", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.2.sp)
                Text("CREATOR CONTROL ROOM", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEE · MMM d", Locale.ENGLISH)).uppercase(Locale.ENGLISH),
                    color = MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (attention == 0) "CLEAR TODAY" else "$attention NEED ATTENTION",
                    color = if (attention == 0) SuccessGreen else RecRed,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.width(22.dp))
            OutlinedButton(
                onClick = onCaptureIdea,
                border = BorderStroke(1.dp, CinemaLine),
                shape = RoundedCornerShape(13.dp),
            ) {
                Text("CAPTURE IDEA", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(9.dp))
            Button(
                onClick = onNewProject,
                colors = ButtonDefaults.buttonColors(containerColor = RecRed, contentColor = ProjectorIvory),
                shape = RoundedCornerShape(13.dp),
            ) {
                Text("CREATE PROJECT", fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(CinemaLine.copy(alpha = .8f)))
}

@Composable
private fun TodayControlRoom(
    ideas: List<Idea>,
    projects: List<CreatorProject>,
    onNewIdea: () -> Unit,
    onNewProject: () -> Unit,
    onAdvanceProject: (String) -> Unit,
    onOpenIdeas: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    val active = projects.filter { it.stage != ProjectStage.PUBLISHED }
        .sortedWith(compareBy<CreatorProject> { it.dueDateOrNull() ?: LocalDate.MAX }.thenByDescending { it.updatedAt })
    val primary = active.firstOrNull()
    val nextUp = active.drop(1).take(3)
    val dueToday = active.count { it.dueDateOrNull() == LocalDate.now() }
    val published = projects.count { it.stage == ProjectStage.PUBLISHED }

    PageScroll {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1.45f)) {
                TodaysFrame(
                    project = primary,
                    onNewProject = onNewProject,
                    onAdvance = onAdvanceProject,
                )

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusMetric("ACTIVE", active.size.toString(), Modifier.weight(1f))
                    StatusMetric("DUE TODAY", dueToday.toString(), Modifier.weight(1f))
                    StatusMetric("PUBLISHED", published.toString(), Modifier.weight(1f))
                }

                if (nextUp.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("NEXT UP", "Keep the next moves visible, not noisy.")
                    Spacer(Modifier.height(10.dp))
                    nextUp.forEach { project ->
                        CompactNextCard(project, onAdvanceProject)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Column(Modifier.weight(.72f)) {
                CompanionPanel(
                    activeCount = active.size,
                    ideaCount = ideas.size,
                    onNewIdea = onNewIdea,
                    onNewProject = onNewProject,
                )

                Spacer(Modifier.height(16.dp))
                SectionLabel("QUICK CAPTURE", "Move it into the system before it disappears.")
                Spacer(Modifier.height(10.dp))
                QuickActionGrid(
                    onProject = onNewProject,
                    onIdea = onOpenIdeas,
                    onCalendar = onOpenCalendar,
                    onInsights = onOpenInsights,
                )

                Spacer(Modifier.height(16.dp))
                MomentumCard(published)
            }
        }
    }
}

@Composable
private fun TodaysFrame(
    project: CreatorProject?,
    onNewProject: () -> Unit,
    onAdvance: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = CinemaSurface.copy(alpha = .96f),
        border = BorderStroke(1.dp, CinemaLine),
        shadowElevation = 8.dp,
    ) {
        if (project == null) {
            Row(Modifier.padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY'S FRAME", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("The room is clear.", color = ProjectorIvory, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Capture the next story when you're ready.", color = MutedText, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onNewProject,
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("＋  CREATE PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                CinePulseCompanion(Modifier.size(210.dp))
            }
        } else {
            val overdue = project.dueDateOrNull()?.isBefore(LocalDate.now()) == true
            Column(Modifier.padding(26.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TODAY'S FRAME", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (overdue) RecRed.copy(alpha = .13f) else CinemaSurfaceRaised,
                    ) {
                        Text(
                            when {
                                overdue -> "OVERDUE"
                                project.dueDate.isBlank() -> "NO DEADLINE"
                                else -> "DUE ${project.dueDate}"
                            },
                            color = if (overdue) RecRed else MutedGold,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(Modifier.height(13.dp))
                Text(
                    project.title,
                    color = ProjectorIvory,
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.note.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(project.note, color = MutedText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.height(20.dp))
                Text("CURRENT STEP", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(project.stage.label, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                ProgressRail(project.stage.progress())
                Spacer(Modifier.height(8.dp))
                Text("${(project.stage.progress() * 100).toInt()}% complete", color = MutedText, fontSize = 9.sp)

                Spacer(Modifier.height(17.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("NEXT MOVE", color = RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(stageAction(project.stage), color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onAdvance(project.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            shape = RoundedCornerShape(13.dp),
                        ) {
                            Text(if (project.stage == ProjectStage.READY) "PUBLISH" else "MARK STAGE DONE  →", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionPanel(
    activeCount: Int,
    ideaCount: Int,
    onNewIdea: () -> Unit,
    onNewProject: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CinemaSurface.copy(alpha = .90f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CinePulseCompanion(Modifier.size(175.dp))
            Spacer(Modifier.height(4.dp))
            Text("CINE PULSE", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    activeCount > 0 -> "$activeCount project${if (activeCount == 1) "" else "s"} in motion. Keep one moving."
                    ideaCount > 0 -> "$ideaCount idea${if (ideaCount == 1) " is" else "s are"} waiting for a decision."
                    else -> "The room is quiet. Capture the next spark."
                },
                color = ProjectorIvory,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNewIdea, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, CinemaLine)) {
                    Text("IDEA", color = ProjectorIvory, fontSize = 9.sp)
                }
                Button(onClick = onNewProject, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                    Text("PROJECT", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun IdeaVaultRoom(
    ideas: List<Idea>,
    onNewIdea: () -> Unit,
    onPromote: (Idea) -> Unit,
) {
    PageScroll {
        SectionHero(
            kicker = "IDEA VAULT",
            title = "Keep the spark. Decide later.",
            body = "Hooks, concepts, observations and fragments stay out of the production queue until you choose them.",
            action = "CAPTURE IDEA",
            onAction = onNewIdea,
        )
        Spacer(Modifier.height(18.dp))

        if (ideas.isEmpty()) {
            EmptyCinematicState(
                title = "Your vault is clear.",
                body = "A good idea should never disappear because you were busy.",
                action = "CAPTURE THE NEXT IDEA",
                onAction = onNewIdea,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    ideas.forEachIndexed { index, idea ->
                        IdeaCard(idea, onPromote)
                        if (index != ideas.lastIndex) Spacer(Modifier.height(10.dp))
                    }
                }
                Surface(
                    modifier = Modifier.width(330.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = CinemaSurface.copy(alpha = .90f),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CinePulseCompanion(Modifier.size(150.dp))
                        Text("DECIDE WHEN IT'S READY", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Spacer(Modifier.height(7.dp))
                        Text("Promoting an idea moves it into Research. Nothing gets forced into production.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioRoom(
    projects: List<CreatorProject>,
    onNewProject: () -> Unit,
    onAdvanceProject: (String) -> Unit,
) {
    val active = projects.filter { it.stage != ProjectStage.PUBLISHED }
    val published = projects.filter { it.stage == ProjectStage.PUBLISHED }

    PageScroll {
        SectionHero(
            kicker = "STUDIO",
            title = "Where the work actually moves.",
            body = "Every project keeps one visible current step and one clear next move.",
            action = "NEW PROJECT",
            onAction = onNewProject,
        )
        Spacer(Modifier.height(18.dp))

        if (active.isEmpty()) {
            EmptyCinematicState(
                title = "No active project.",
                body = "Start from an idea or create something directly.",
                action = "CREATE PROJECT",
                onAction = onNewProject,
            )
        } else {
            SectionLabel("IN PRODUCTION", "${active.size} active project${if (active.size == 1) "" else "s"}")
            Spacer(Modifier.height(10.dp))
            active.forEach { project ->
                StudioProjectCard(project, onAdvanceProject)
                Spacer(Modifier.height(10.dp))
            }
        }

        if (published.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionLabel("PUBLISHED", "Finished work stays part of your creative history.")
            Spacer(Modifier.height(10.dp))
            published.forEach { project ->
                PublishedCard(project)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CalendarRoom(projects: List<CreatorProject>) {
    val scheduled = projects.mapNotNull { project -> project.dueDateOrNull()?.let { it to project } }.sortedBy { it.first }

    PageScroll {
        SectionHero(
            kicker = "CONTENT CALENDAR",
            title = "See pressure before it becomes pressure.",
            body = "Deadlines from your creator pipeline stay visible here without becoming a generic calendar app.",
        )
        Spacer(Modifier.height(18.dp))

        if (scheduled.isEmpty()) {
            EmptyCinematicState(
                title = "No deadlines on the board.",
                body = "Add a YYYY-MM-DD deadline when creating a project and it will appear here.",
            )
        } else {
            scheduled.forEach { (date, project) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CinemaSurface.copy(alpha = .92f),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(118.dp)) {
                            Text(date.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)).uppercase(Locale.ENGLISH), color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text(date.dayOfMonth.toString(), color = ProjectorIvory, fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Text(date.dayOfWeek.name.take(3), color = MutedText, fontSize = 8.sp)
                        }
                        Box(Modifier.width(1.dp).height(56.dp).background(CinemaLine))
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(project.title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(project.stage.label.uppercase(Locale.ENGLISH), color = RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            when {
                                date.isBefore(LocalDate.now()) -> "OVERDUE"
                                date == LocalDate.now() -> "TODAY"
                                else -> "UPCOMING"
                            },
                            color = when {
                                date.isBefore(LocalDate.now()) -> RecRed
                                date == LocalDate.now() -> MutedGold
                                else -> MutedText
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun InsightsRoom(ideas: List<Idea>, projects: List<CreatorProject>) {
    val active = projects.count { it.stage != ProjectStage.PUBLISHED }
    val published = projects.count { it.stage == ProjectStage.PUBLISHED }
    val completion = if (projects.isEmpty()) 0 else (published * 100 / projects.size)
    val stageCounts = ProjectStage.entries.associateWith { stage -> projects.count { it.stage == stage } }

    PageScroll {
        SectionHero(
            kicker = "CREATOR INTELLIGENCE",
            title = "Learn how you make things.",
            body = "Backlot should explain your workflow, not bury you in vanity charts.",
        )
        Spacer(Modifier.height(18.dp))

        Text("3 THINGS YOU SHOULD KNOW", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightSignal("PIPELINE", if (active == 0) "Clear" else "$active active", if (active == 0) "You have room to start something." else "Your current production load.", Modifier.weight(1f))
            InsightSignal("IDEA BANK", ideas.size.toString(), if (ideas.isEmpty()) "No ideas waiting." else "Ideas still waiting for a decision.", Modifier.weight(1f))
            InsightSignal("FINISH RATE", "$completion%", "$published of ${projects.size} projects published.", Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = CinemaSurface.copy(alpha = .92f),
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Column(Modifier.padding(22.dp)) {
                SectionLabel("WORKFLOW SHAPE", "Where your current work is sitting right now.")
                Spacer(Modifier.height(16.dp))
                ProjectStage.entries.filter { it != ProjectStage.PUBLISHED }.forEach { stage ->
                    WorkflowRow(stage.label, stageCounts[stage] ?: 0, projects.size.coerceAtLeast(1))
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = RecRedDeep.copy(alpha = .48f),
            border = BorderStroke(1.dp, RecRed.copy(alpha = .22f)),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                CinePulseCompanion(Modifier.size(120.dp))
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("WHAT BACKLOT IS LEARNING", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "This desktop build learns only from local workflow signals for now. Cross-device history, YouTube performance and creator postmortems belong to the shared intelligence layer and are not faked here.",
                        color = ProjectorIvory,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorDock(
    selected: DesktopDestination,
    onSelect: (DesktopDestination) -> Unit,
    onCapture: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().height(86.dp).background(
            Brush.verticalGradient(listOf(Color.Transparent, CinemaBlack.copy(alpha = .98f)))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(25.dp),
            color = CinemaSurface.copy(alpha = .98f),
            border = BorderStroke(1.dp, CinemaLine),
            shadowElevation = 10.dp,
        ) {
            Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                DockItem("TODAY", selected == DesktopDestination.TODAY) { onSelect(DesktopDestination.TODAY) }
                DockItem("IDEAS", selected == DesktopDestination.IDEAS) { onSelect(DesktopDestination.IDEAS) }
                Surface(
                    onClick = onCapture,
                    modifier = Modifier.padding(horizontal = 6.dp).size(48.dp),
                    shape = CircleShape,
                    color = RecRed,
                    shadowElevation = 8.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("＋", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                }
                DockItem("CREATE", selected == DesktopDestination.PROJECTS) { onSelect(DesktopDestination.PROJECTS) }
                DockItem("CALENDAR", selected == DesktopDestination.CALENDAR) { onSelect(DesktopDestination.CALENDAR) }
                DockItem("INSIGHTS", selected == DesktopDestination.INSIGHTS) { onSelect(DesktopDestination.INSIGHTS) }
            }
        }
    }
}

@Composable
private fun DockItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 17.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (selected) RecRed else Color.Transparent))
        Spacer(Modifier.height(5.dp))
        Text(label, color = if (selected) ProjectorIvory else MutedText, fontSize = 8.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, letterSpacing = .7.sp)
    }
}

@Composable
private fun QuickActionGrid(
    onProject: () -> Unit,
    onIdea: () -> Unit,
    onCalendar: () -> Unit,
    onInsights: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAction("PROJECT", "＋", onProject, Modifier.weight(1f))
        QuickAction("IDEA", "◇", onIdea, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAction("CALENDAR", "▦", onCalendar, Modifier.weight(1f))
        QuickAction("INSIGHTS", "↗", onInsights, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(label: String, symbol: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = CinemaSurface.copy(alpha = .94f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, color = MutedGold, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(9.dp))
            Text(label, color = ProjectorIvory, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
        }
    }
}

@Composable
private fun MomentumCard(published: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = CinemaSurface.copy(alpha = .90f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(SuccessGreen.copy(alpha = .11f), CircleShape), contentAlignment = Alignment.Center) {
                Text("✓", color = SuccessGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("CREATOR MOMENTUM", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                Text(
                    if (published == 0) "Finish one project and the room starts keeping score." else "$published project${if (published == 1) "" else "s"} completed so far.",
                    color = MutedText,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun CompactNextCard(project: CreatorProject, onAdvance: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface.copy(alpha = .92f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(RecRedDeep.copy(alpha = .75f), CircleShape), contentAlignment = Alignment.Center) {
                Text(project.stage.label.take(1), color = RecRed, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(project.title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(project.stage.label + if (project.dueDate.isBlank()) "" else " · Due ${project.dueDate}", color = MutedText, fontSize = 9.sp)
            }
            TextButton(onClick = { onAdvance(project.id) }) {
                Text("NEXT STEP  →", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StudioProjectCard(project: CreatorProject, onAdvance: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface.copy(alpha = .95f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(project.stage.label.uppercase(Locale.ENGLISH), color = RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(project.title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    if (project.note.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text(project.note, color = MutedText, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (project.dueDate.isNotBlank()) {
                    Text("DUE ${project.dueDate}", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(14.dp))
            ProgressRail(project.stage.progress())
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NEXT MOVE", color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Text(stageAction(project.stage), color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onAdvance(project.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (project.stage == ProjectStage.READY) "PUBLISH" else "STAGE DONE  →", fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun PublishedCard(project: CreatorProject) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = CinemaSurface.copy(alpha = .78f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✓", color = SuccessGreen, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(11.dp))
            Text(project.title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("PUBLISHED", color = SuccessGreen, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun IdeaCard(idea: Idea, onPromote: (Idea) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface.copy(alpha = .94f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(MutedGold.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
                Text("◇", color = MutedGold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(idea.title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (idea.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(idea.note, color = MutedText, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Button(
                onClick = { onPromote(idea) },
                colors = ButtonDefaults.buttonColors(containerColor = CinemaSurfaceRaised, contentColor = MutedGold),
                border = BorderStroke(1.dp, CinemaLine),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("MOVE TO RESEARCH  →", fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SectionHero(
    kicker: String,
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = CinemaSurface.copy(alpha = .92f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(25.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(kicker, color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                Spacer(Modifier.height(8.dp))
                Text(title, color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(body, color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
            }
            if (action != null && onAction != null) {
                Spacer(Modifier.width(18.dp))
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(13.dp)) {
                    Text(action, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun EmptyCinematicState(
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CinemaSurface.copy(alpha = .88f),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
            CinePulseCompanion(Modifier.size(150.dp))
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(body, color = MutedText, fontSize = 11.sp)
                if (action != null && onAction != null) {
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(12.dp)) {
                        Text(action, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightSignal(label: String, value: String, body: String, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(20.dp), CinemaSurface.copy(alpha = .94f), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(body, color = MutedText, fontSize = 9.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun WorkflowRow(label: String, count: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MutedText, fontSize = 10.sp, modifier = Modifier.width(92.dp))
        Box(Modifier.weight(1f).height(7.dp).background(CinemaLine, RoundedCornerShape(10.dp))) {
            if (count > 0) {
                Box(
                    Modifier.fillMaxWidth((count.toFloat() / total.toFloat()).coerceIn(.04f, 1f))
                        .height(7.dp)
                        .background(Brush.horizontalGradient(listOf(MutedGold, RecRed)), RoundedCornerShape(10.dp))
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(count.toString(), color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
    }
}

@Composable
private fun StatusMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(17.dp), CinemaSurface.copy(alpha = .92f), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
        }
    }
}

@Composable
private fun ProgressRail(progress: Float) {
    Box(Modifier.fillMaxWidth().height(6.dp).background(CinemaLine, RoundedCornerShape(10.dp))) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(.03f, 1f))
                .height(6.dp)
                .background(Brush.horizontalGradient(listOf(MutedGold, RecRed)), RoundedCornerShape(10.dp))
        )
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column {
        Text(title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = MutedText, fontSize = 9.sp)
    }
}

@Composable
private fun PageScroll(content: @Composable Column.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 34.dp, vertical = 24.dp).padding(bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun BacklotLayerMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF08111B), Color(0xFF020304), Color(0xFF061426))),
            cornerRadius = CornerRadius(s * .21f, s * .21f),
        )
        drawRoundRect(
            color = CoolBlue.copy(alpha = .55f),
            style = Stroke(width = s * .015f),
            cornerRadius = CornerRadius(s * .21f, s * .21f),
        )
        val beam = Path().apply {
            moveTo(s * .58f, s * .72f)
            lineTo(s * .12f, s * .96f)
            lineTo(s * .91f, s * .96f)
            close()
        }
        drawPath(beam, Brush.linearGradient(listOf(Color.Transparent, MutedGold.copy(alpha = .95f), ProjectorIvory.copy(alpha = .65f))))
        drawRect(
            Brush.linearGradient(listOf(ProjectorIvory, MutedGold, Color(0xFFB76625))),
            topLeft = Offset(s * .27f, s * .16f),
            size = Size(s * .12f, s * .67f),
        )
        repeat(5) { i ->
            drawRoundRect(
                Color(0xFF11100E),
                topLeft = Offset(s * .295f, s * (.23f + i * .11f)),
                size = Size(s * .07f, s * .06f),
                cornerRadius = CornerRadius(s * .012f),
            )
        }
        val b = Path().apply {
            moveTo(s * .42f, s * .18f)
            lineTo(s * .61f, s * .22f)
            cubicTo(s * .78f, s * .25f, s * .83f, s * .34f, s * .83f, s * .44f)
            cubicTo(s * .83f, s * .52f, s * .78f, s * .57f, s * .70f, s * .60f)
            cubicTo(s * .82f, s * .64f, s * .87f, s * .72f, s * .87f, s * .80f)
            cubicTo(s * .87f, s * .92f, s * .78f, s * .97f, s * .60f, s * .97f)
            lineTo(s * .42f, s * .97f)
            close()
        }
        drawPath(b, Brush.linearGradient(listOf(ProjectorIvory, Color(0xFFFFD27A), Color(0xFFE3A04A), Color(0xFF7896B4))))
        val doorway = Path().apply {
            moveTo(s * .42f, s * .32f)
            lineTo(s * .59f, s * .42f)
            lineTo(s * .59f, s * .77f)
            lineTo(s * .42f, s * .86f)
            close()
        }
        drawPath(doorway, Color(0xFF040506))
    }
}

@Composable
private fun CinePulseCompanion(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 100f
        val cx = size.width * .50f
        val faceY = size.height * .30f
        val faceW = 58f * u
        val faceH = 37f * u
        val hotPink = Color(0xFFFF4B9A)
        val orange = Color(0xFFFF8A4C)
        val aqua = Color(0xFF52DED2)

        drawCircle(
            Brush.radialGradient(listOf(hotPink.copy(alpha = .14f), aqua.copy(alpha = .10f), Color.Transparent)),
            radius = size.width * .56f,
            center = Offset(cx, size.height * .42f),
        )

        val haloSize = Size(faceW * 1.48f, faceH * 1.58f)
        val topLeft = Offset(cx - haloSize.width / 2f, faceY - haloSize.height / 2f)
        drawArc(hotPink.copy(alpha = .82f), 194f, 154f, false, topLeft, haloSize, style = Stroke(8.1f * u, cap = StrokeCap.Round))
        drawArc(RecRed.copy(alpha = .74f), 230f, 105f, false, topLeft + Offset(1f * u, 1f * u), haloSize, style = Stroke(5.7f * u, cap = StrokeCap.Round))
        drawArc(aqua.copy(alpha = .80f), 8f, 150f, false, topLeft + Offset(0f, 1.5f * u), haloSize, style = Stroke(6f * u, cap = StrokeCap.Round))
        drawArc(orange.copy(alpha = .60f), 326f, 76f, false, topLeft + Offset(0f, 2f * u), haloSize, style = Stroke(3.2f * u, cap = StrokeCap.Round))

        drawOval(Color(0xFF030409), Offset(cx - faceW / 2f, faceY - faceH / 2f), Size(faceW, faceH))
        drawOval(
            Brush.linearGradient(listOf(hotPink, orange, aqua)),
            Offset(cx - faceW / 2f, faceY - faceH / 2f),
            Size(faceW, faceH),
            style = Stroke(1.7f * u),
        )

        val eyeY = faceY + .5f * u
        drawRoundRect(ProjectorIvory, Offset(cx - 14f * u, eyeY - 6f * u), Size(5f * u, 12f * u), CornerRadius(2.5f * u))
        drawRoundRect(ProjectorIvory, Offset(cx + 9f * u, eyeY - 6f * u), Size(5f * u, 12f * u), CornerRadius(2.5f * u))

        val bodyTop = size.height * .49f
        val body = Path().apply {
            moveTo(cx - 17f * u, bodyTop)
            cubicTo(cx - 23f * u, bodyTop + 12f * u, cx - 18f * u, bodyTop + 33f * u, cx, bodyTop + 38f * u)
            cubicTo(cx + 18f * u, bodyTop + 33f * u, cx + 23f * u, bodyTop + 12f * u, cx + 17f * u, bodyTop)
            close()
        }
        drawPath(body, Brush.verticalGradient(listOf(Color(0xFF12141A), Color(0xFF050609))))
        drawPath(body, Brush.linearGradient(listOf(hotPink.copy(alpha = .65f), aqua.copy(alpha = .5f))), style = Stroke(1.2f * u))

        drawLine(MutedGold.copy(alpha = .75f), Offset(cx - 8f * u, bodyTop + 15f * u), Offset(cx - 24f * u, bodyTop + 29f * u), 2.2f * u, StrokeCap.Round)
        drawLine(aqua.copy(alpha = .75f), Offset(cx + 8f * u, bodyTop + 15f * u), Offset(cx + 24f * u, bodyTop + 25f * u), 2.2f * u, StrokeCap.Round)
    }
}

@Composable
private fun IdeaCaptureDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text("Capture an idea", color = ProjectorIvory, fontWeight = FontWeight.Black, fontSize = 23.sp) },
        text = {
            Column {
                Text("Get it out of your head. Shape it later.", color = MutedText, fontSize = 10.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Idea") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth().height(110.dp), label = { Text("Notes · optional") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, note) }, enabled = title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(12.dp)) {
                Text("SAVE IDEA", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedGold) } },
    )
}

@Composable
private fun ProjectCaptureDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text("Create a project", color = ProjectorIvory, fontWeight = FontWeight.Black, fontSize = 23.sp) },
        text = {
            Column {
                Text("Name the work. Backlot will keep the next move visible.", color = MutedText, fontSize = 10.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Project title") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = due, onValueChange = { due = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Deadline · YYYY-MM-DD · optional") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth().height(110.dp), label = { Text("Notes · optional") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, due, note) }, enabled = title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(12.dp)) {
                Text("CREATE PROJECT", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedGold) } },
    )
}

@Composable
private fun ControlRoomDialog(
    onDismiss: () -> Unit,
    onIdea: () -> Unit,
    onProject: () -> Unit,
    onCalendar: () -> Unit,
    onInsights: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text("Backlot Control", color = ProjectorIvory, fontWeight = FontWeight.Black, fontSize = 22.sp) },
        text = {
            Column {
                Text("Fast routes into the creator system.", color = MutedText, fontSize = 10.sp)
                Spacer(Modifier.height(14.dp))
                ControlAction("CAPTURE IDEA", "Save a spark before it disappears.", onIdea)
                Spacer(Modifier.height(8.dp))
                ControlAction("CREATE PROJECT", "Start work directly in the pipeline.", onProject)
                Spacer(Modifier.height(8.dp))
                ControlAction("CONTENT CALENDAR", "See deadlines across active work.", onCalendar)
                Spacer(Modifier.height(8.dp))
                ControlAction("CREATOR INTELLIGENCE", "See what Backlot is learning locally.", onInsights)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = MutedGold, fontWeight = FontWeight.Black) } },
    )
}

@Composable
private fun ControlAction(title: String, body: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                Text(body, color = MutedText, fontSize = 9.sp)
            }
            Text("→", color = RecRed, fontSize = 17.sp)
        }
    }
}

private fun stageAction(stage: ProjectStage): String = when (stage) {
    ProjectStage.IDEA -> "Define what this project is really about."
    ProjectStage.RESEARCH -> "Gather the material you need before writing."
    ProjectStage.SCRIPT -> "Turn the research into a clear structure and script."
    ProjectStage.RECORD -> "Record the voice, camera or source material."
    ProjectStage.EDIT -> "Shape the final piece and remove what does not serve it."
    ProjectStage.READY -> "Package it, check it and publish."
    ProjectStage.PUBLISHED -> "Capture what worked and what you would change next time."
}
