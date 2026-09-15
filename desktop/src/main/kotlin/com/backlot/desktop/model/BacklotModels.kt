package com.backlot.desktop.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class DesktopDestination(val label: String) {
    TODAY("Today"),
    IDEAS("Idea Vault"),
    PROJECTS("Projects"),
    CALENDAR("Calendar"),
    INSIGHTS("Insights"),
}

enum class ProjectStage(val label: String) {
    IDEA("Idea"),
    RESEARCH("Research"),
    SCRIPT("Script"),
    RECORD("Record"),
    EDIT("Edit"),
    READY("Ready"),
    PUBLISHED("Published");

    fun next(): ProjectStage = entries.getOrElse(ordinal + 1) { PUBLISHED }

    fun progress(): Float = when (this) {
        IDEA -> 0.10f
        RESEARCH -> 0.25f
        SCRIPT -> 0.42f
        RECORD -> 0.58f
        EDIT -> 0.74f
        READY -> 0.90f
        PUBLISHED -> 1f
    }
}

data class Idea(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val createdAt: String = Instant.now().toString(),
)

data class CreatorProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val stage: ProjectStage = ProjectStage.IDEA,
    val dueDate: String = "",
    val note: String = "",
    val updatedAt: String = Instant.now().toString(),
) {
    fun advance(): CreatorProject = copy(stage = stage.next(), updatedAt = Instant.now().toString())

    fun dueDateOrNull(): LocalDate? = runCatching {
        dueDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
    }.getOrNull()
}

data class DesktopSnapshot(
    val ideas: List<Idea> = emptyList(),
    val projects: List<CreatorProject> = emptyList(),
)
