from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found")
    path.write_text(text.replace(old, new, 1))


# Idempotent when CI is re-run against an already-materialized source tree.
gradle = Path("app/build.gradle.kts")
gradle_text = gradle.read_text()
if 'versionCode = 106' in gradle_text and 'versionName = "2.0.0-alpha1.3-opportunity-engine"' in gradle_text:
    print("Alpha 1.3 already materialized")
    raise SystemExit(0)

priority = Path("app/src/main/java/com/framebynavin/app/data/CreatorPriorityEngine.kt")
priority.write_text('''package com.framebynavin.app.data

private const val HOUR_MS = 60L * 60_000L
private const val DAY_MS = 24L * HOUR_MS

data class CreatorRecommendation(
    val taskId: String,
    val action: String,
    val urgencyLabel: String,
    val reason: String,
    val score: Int,
    /** Rough planning estimate, not measured creator history. */
    val estimatedMinutes: Int = 30,
    /** Explainable evidence behind the recommendation. */
    val signals: List<String> = emptyList(),
)

object CreatorPriorityEngine {
    fun rankActive(tasks: List<CreatorTask>, now: Long = System.currentTimeMillis()): List<CreatorTask> =
        tasks.asSequence()
            .filter { it.archivedAtMillis <= 0L }
            .filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
            .sortedWith(
                compareByDescending<CreatorTask> { score(it, now) }
                    .thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
            .toList()

    fun recommendation(task: CreatorTask, now: Long = System.currentTimeMillis()): CreatorRecommendation {
        val dueDelta = task.dueAtMillis.takeIf { it > 0L }?.minus(now)
        val urgency = when {
            dueDelta != null && dueDelta < 0L -> "OVERDUE"
            dueDelta != null && dueDelta <= 2 * HOUR_MS -> "NOW"
            dueDelta != null && dueDelta <= DAY_MS -> "TODAY"
            task.priority == TaskPriority.CRITICAL -> "HIGH"
            task.status == TaskStatus.WORKING -> "CONTINUE"
            else -> "NEXT"
        }
        val reason = when {
            dueDelta != null && dueDelta < 0L -> "Deadline passed — finish the current step before lower-priority work."
            dueDelta != null && dueDelta <= 2 * HOUR_MS -> "Publishing is close, so this project has the least schedule buffer."
            task.status == TaskStatus.WORKING -> "You already started this project — keeping momentum reduces context switching."
            task.priority == TaskPriority.CRITICAL -> "Marked Critical, so it outranks Important and Normal work."
            dueDelta != null && dueDelta <= DAY_MS -> "Due today — completing the current step protects the publish window."
            task.priority == TaskPriority.IMPORTANT -> "Important project with the strongest current deadline and progress signal."
            CreatorWorkflowEngine.progress(task) >= 75 -> "Close to publish — finishing it now clears active work faster."
            else -> "Best next step from deadline, priority and current progress."
        }
        return CreatorRecommendation(
            taskId = task.id,
            action = CreatorWorkflowEngine.nextAction(task),
            urgencyLabel = urgency,
            reason = reason,
            score = score(task, now),
            estimatedMinutes = estimateStageMinutes(task),
            signals = evidence(task, now).take(3),
        )
    }

    fun estimateStageMinutes(task: CreatorTask): Int {
        val stage = CreatorWorkflowEngine.currentStage(task).id
        val longWork = task.contentType.trim().lowercase() in setOf("long-form", "video", "episode", "article", "newsletter")
        return when (stage) {
            "idea", "angle", "select" -> 20
            "research" -> if (longWork) 50 else 30
            "outline" -> 30
            "script", "draft" -> if (longWork) 50 else 25
            "voice", "record" -> if (longWork) 35 else 20
            "edit" -> if (longWork) 60 else 35
            "sound_grade" -> 40
            "thumbnail", "create" -> 30
            "caption", "copy", "metadata" -> 20
            "review", "proof", "verify" -> 15
            "upload", "send", "published" -> 15
            "promote", "engage" -> 20
            else -> 30
        }
    }

    fun score(task: CreatorTask, now: Long = System.currentTimeMillis()): Int {
        if (task.archivedAtMillis > 0L || (task.status != TaskStatus.PLANNED && task.status != TaskStatus.WORKING)) return Int.MIN_VALUE
        var score = 0
        if (task.status == TaskStatus.WORKING) score += 260
        score += when (task.priority) {
            TaskPriority.NORMAL -> 0
            TaskPriority.IMPORTANT -> 90
            TaskPriority.CRITICAL -> 180
        }
        val due = task.dueAtMillis
        if (due > 0L) {
            val delta = due - now
            score += when {
                delta < 0L -> 500 + ((-delta / HOUR_MS).coerceAtMost(30L).toInt() * 8)
                delta <= 2 * HOUR_MS -> 360
                delta <= 6 * HOUR_MS -> 280
                delta <= DAY_MS -> 200
                delta <= 2 * DAY_MS -> 120
                delta <= 7 * DAY_MS -> 60
                else -> 20
            }
        }
        val progress = CreatorWorkflowEngine.progress(task)
        score += (progress / 5).coerceAtMost(20)
        if (progress >= 75) score += 40
        if (task.reminderEnabled && task.reminderAtMillis > 0L && task.reminderAtMillis - now <= 2 * HOUR_MS) score += 35
        return score
    }

    private fun evidence(task: CreatorTask, now: Long): List<String> = buildList {
        val due = task.dueAtMillis
        if (due > 0L) {
            val delta = due - now
            add(when {
                delta < 0L -> "Deadline is overdue"
                delta <= 2 * HOUR_MS -> "Deadline is within 2 hours"
                delta <= DAY_MS -> "Due today"
                delta <= 2 * DAY_MS -> "Due within 2 days"
                else -> "Deadline still has buffer"
            })
        }
        if (task.status == TaskStatus.WORKING) add("Already in progress")
        if (task.priority == TaskPriority.CRITICAL) add("Critical priority")
        else if (task.priority == TaskPriority.IMPORTANT) add("Important priority")
        val progress = CreatorWorkflowEngine.progress(task)
        add("${CreatorWorkflowEngine.currentStage(task).label} · $progress% workflow")
        val unfinished = task.workspace.checklist.count { it.status == CreatorChecklistStatus.TODO }
        val requiredPublishChecks = task.workspace.deliverables.sumOf { d -> d.publishGate.count { it.required && it.status == CreatorPublishGateStatus.TODO } }
        val remaining = unfinished + requiredPublishChecks
        if (remaining > 0) add("$remaining unfinished check${if (remaining == 1) "" else "s"}")
        if (task.reminderEnabled && task.reminderAtMillis in 1..(now + 2 * HOUR_MS)) add("Reminder is due soon")
    }
}
''')

today = Path("app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt")
old_card = '''@Composable
private fun PNextMoveCard(task: CreatorTask) {
    val recommendation = CreatorPriorityEngine.recommendation(task)
    val next = CreatorWorkflowEngine.nextStage(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT MOVE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.weight(1f))
                Text(recommendation.urgencyLabel, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            }
            Spacer(Modifier.height(5.dp))
            Text(recommendation.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(recommendation.reason, color = MutedText, fontSize = 9.3.sp, lineHeight = 13.sp)
            next?.let {
                Spacer(Modifier.height(5.dp))
                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)
            }
        }
    }
}'''
new_card = '''@Composable
private fun PNextMoveCard(task: CreatorTask) {
    val recommendation = CreatorPriorityEngine.recommendation(task)
    val next = CreatorWorkflowEngine.nextStage(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, MutedGold.copy(alpha = .24f))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT BEST ACTION", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .10f)) {
                    Text("ROUGH ${recommendation.estimatedMinutes} MIN", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MutedGold, fontSize = 7.6.sp, fontWeight = FontWeight.Black, letterSpacing = .55.sp)
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recommendation.urgencyLabel, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Spacer(Modifier.width(7.dp)); Text("·", color = MutedText, fontSize = 8.sp); Spacer(Modifier.width(7.dp))
                Text(CreatorWorkflowEngine.currentStage(task).label.uppercase(), color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(recommendation.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(recommendation.reason, color = MutedText, fontSize = 9.3.sp, lineHeight = 13.sp)
            if (recommendation.signals.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Text("WHY NOW", color = ProjectorIvory.copy(alpha = .72f), fontSize = 7.7.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Spacer(Modifier.height(3.dp))
                Text(recommendation.signals.joinToString(" · "), color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
            }
            next?.let {
                Spacer(Modifier.height(8.dp))
                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)
            }
        }
    }
}'''
replace_once(today, old_card, new_card, "Next best action card")

if 'versionCode = 105' not in gradle_text or 'versionName = "2.0.0-alpha1.2p-live-video-autopsy"' not in gradle_text:
    raise SystemExit("Unexpected v105 build identity")
gradle_text = gradle_text.replace('versionCode = 105', 'versionCode = 106', 1)
gradle_text = gradle_text.replace('versionName = "2.0.0-alpha1.2p-live-video-autopsy"', 'versionName = "2.0.0-alpha1.3-opportunity-engine"', 1)
gradle.write_text(gradle_text)
