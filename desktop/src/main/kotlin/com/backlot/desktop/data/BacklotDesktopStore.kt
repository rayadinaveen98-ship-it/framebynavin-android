package com.backlot.desktop.data

import com.backlot.desktop.model.CreatorProject
import com.backlot.desktop.model.DesktopSnapshot
import com.backlot.desktop.model.Idea
import com.backlot.desktop.model.ProjectStage
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Base64

class BacklotDesktopStore(
    private val root: Path = Paths.get(System.getProperty("user.home"), ".backlot"),
) {
    private val dataFile: Path = root.resolve("desktop-v1.db")

    fun load(): DesktopSnapshot {
        if (!Files.exists(dataFile)) return DesktopSnapshot()

        val ideas = mutableListOf<Idea>()
        val projects = mutableListOf<CreatorProject>()

        Files.readAllLines(dataFile, StandardCharsets.UTF_8).forEach { line ->
            val parts = line.split('|')
            when (parts.firstOrNull()) {
                "I" -> if (parts.size >= 5) {
                    runCatching {
                        ideas += Idea(
                            id = decode(parts[1]),
                            title = decode(parts[2]),
                            note = decode(parts[3]),
                            createdAt = decode(parts[4]),
                        )
                    }
                }

                "P" -> if (parts.size >= 8) {
                    runCatching {
                        projects += CreatorProject(
                            id = decode(parts[1]),
                            title = decode(parts[2]),
                            stage = ProjectStage.valueOf(parts[3]),
                            dueDate = decode(parts[4]),
                            note = decode(parts[5]),
                            updatedAt = decode(parts[6]),
                        )
                    }
                }
            }
        }

        return DesktopSnapshot(ideas = ideas, projects = projects)
    }

    fun save(snapshot: DesktopSnapshot) {
        Files.createDirectories(root)
        val temporary = root.resolve("desktop-v1.db.tmp")
        val lines = buildList {
            snapshot.ideas.forEach { idea ->
                add(
                    listOf(
                        "I",
                        encode(idea.id),
                        encode(idea.title),
                        encode(idea.note),
                        encode(idea.createdAt),
                    ).joinToString("|")
                )
            }
            snapshot.projects.forEach { project ->
                add(
                    listOf(
                        "P",
                        encode(project.id),
                        encode(project.title),
                        project.stage.name,
                        encode(project.dueDate),
                        encode(project.note),
                        encode(project.updatedAt),
                        "1",
                    ).joinToString("|")
                )
            }
        }

        Files.write(temporary, lines, StandardCharsets.UTF_8)
        runCatching {
            Files.move(
                temporary,
                dataFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.getOrElse {
            Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun dataPath(): Path = dataFile

    private fun encode(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )
}
