package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.creatorPostPublishDataStore by preferencesDataStore(name = "creator_post_publish_v21")
private val creatorPostPublishMutationMutex = Mutex()

enum class PostPublishCheckpointStatus { PENDING, DONE, SKIPPED }

enum class PostPublishCheckpointKind(val key: String) {
    CROSS_PROMOTE("cross-promote"),
    PERFORMANCE_24H("24h-review"),
    PERFORMANCE_7D("7d-review"),
}

data class PostPublishCheckpointSpec(
    val kind: PostPublishCheckpointKind,
    val title: String,
    val description: String,
    val dueOffsetMinutes: Long,
)

data class PostPublishCheckpoint(
    val id: String,
    val projectId: String,
    val kind: PostPublishCheckpointKind,
    val title: String,
    val description: String,
    val dueAtMillis: Long,
    val status: PostPublishCheckpointStatus = PostPublishCheckpointStatus.PENDING,
    val completedAtMillis: Long = 0L,
)

object CreatorPostPublishEngine {
    fun specs(parent: CreatorTask): List<PostPublishCheckpointSpec> = when (parent.platform.trim().lowercase()) {
        "youtube" -> listOf(
            PostPublishCheckpointSpec(
                kind = PostPublishCheckpointKind.PERFORMANCE_24H,
                title = "24h performance check",
                description = "Review the first-day response and note what is working.",
                dueOffsetMinutes = 24 * 60,
            ),
            PostPublishCheckpointSpec(
                kind = PostPublishCheckpointKind.PERFORMANCE_7D,
                title = "7d performance review",
                description = "Review the full week and save the lesson for the next project.",
                dueOffsetMinutes = 7 * 24 * 60,
            ),
        )
        "instagram" -> listOf(
            PostPublishCheckpointSpec(
                kind = PostPublishCheckpointKind.PERFORMANCE_24H,
                title = "24h performance check",
                description = "Review the first-day response and note what is working.",
                dueOffsetMinutes = 24 * 60,
            ),
        )
        else -> emptyList()
    }

    fun checkpointId(projectId: String, kind: PostPublishCheckpointKind): String = "post-publish:$projectId:${kind.key}"

    fun build(parent: CreatorTask, completedAtMillis: Long = parent.publishedAtMillis): List<PostPublishCheckpoint> {
        val base = completedAtMillis.takeIf { it > 0L } ?: return emptyList()
        return specs(parent).map { spec ->
            PostPublishCheckpoint(
                id = checkpointId(parent.id, spec.kind),
                projectId = parent.id,
                kind = spec.kind,
                title = spec.title,
                description = spec.description,
                dueAtMillis = base + spec.dueOffsetMinutes * 60_000L,
            )
        }
    }

    fun publicationDate(task: CreatorTask): Long = task.publishedAtMillis

    fun isLegacyTask(task: CreatorTask): Boolean = task.sourceRefId.startsWith("post-publish:")

    fun parseLegacySource(sourceRefId: String): Pair<String, PostPublishCheckpointKind>? {
        if (!sourceRefId.startsWith("post-publish:")) return null
        val body = sourceRefId.removePrefix("post-publish:")
        val separator = body.lastIndexOf(':')
        if (separator <= 0 || separator >= body.lastIndex) return null
        val projectId = body.substring(0, separator)
        val key = body.substring(separator + 1)
        val kind = PostPublishCheckpointKind.entries.firstOrNull { it.key == key } ?: return null
        return projectId to kind
    }

    fun fromLegacyTask(task: CreatorTask): PostPublishCheckpoint? {
        val (projectId, kind) = parseLegacySource(task.sourceRefId) ?: return null
        // Alpha20 generated Cross-promote as a second project even though creator workflows already
        // contain a Promote stage. Drop that duplicate during migration instead of preserving clutter.
        if (kind == PostPublishCheckpointKind.CROSS_PROMOTE) return null
        val spec = when (kind) {
            PostPublishCheckpointKind.CROSS_PROMOTE -> PostPublishCheckpointSpec(
                kind, "Cross-promote", "Share the published work where it can reach the right audience.", 30,
            )
            PostPublishCheckpointKind.PERFORMANCE_24H -> PostPublishCheckpointSpec(
                kind, "24h performance check", "Review the first-day response and note what is working.", 24 * 60,
            )
            PostPublishCheckpointKind.PERFORMANCE_7D -> PostPublishCheckpointSpec(
                kind, "7d performance review", "Review the full week and save the lesson for the next project.", 7 * 24 * 60,
            )
        }
        val status = when (task.status) {
            TaskStatus.DONE -> PostPublishCheckpointStatus.DONE
            TaskStatus.SKIPPED -> PostPublishCheckpointStatus.SKIPPED
            TaskStatus.PLANNED, TaskStatus.WORKING -> PostPublishCheckpointStatus.PENDING
        }
        return PostPublishCheckpoint(
            id = checkpointId(projectId, kind),
            projectId = projectId,
            kind = kind,
            title = spec.title,
            description = spec.description,
            dueAtMillis = task.dueAtMillis,
            status = status,
            completedAtMillis = if (status == PostPublishCheckpointStatus.DONE) {
                task.completedAtMillis.takeIf { it > 0L } ?: task.dueAtMillis
            } else 0L,
        )
    }
}

class CreatorPostPublishStore(private val context: Context) {
    private val checkpointsKey = stringPreferencesKey("checkpoints_json")

    val checkpointsFlow: Flow<List<PostPublishCheckpoint>> = context.creatorPostPublishDataStore.data.map { prefs ->
        decode(prefs[checkpointsKey] ?: "[]")
    }

    suspend fun load(): List<PostPublishCheckpoint> = checkpointsFlow.first()

    suspend fun save(checkpoints: List<PostPublishCheckpoint>) = CreatorDataGate.transaction { creatorPostPublishMutationMutex.withLock {
        saveUnlocked(checkpoints)
    } }

    /** Rebase pending review dates; retain completed/skipped history and unrelated projects. */
    suspend fun reconcilePublication(parent: CreatorTask): List<PostPublishCheckpoint> =
        reconcilePublications(listOf(parent))

    suspend fun reconcilePublications(parents: List<CreatorTask>): List<PostPublishCheckpoint> = CreatorDataGate.transaction {
        creatorPostPublishMutationMutex.withLock {
            val current = load()
            val updated = CreatorPostPublishReconciliation.reconcile(current, parents)
            if (updated != current) saveUnlocked(updated)
            updated.sortedWith(compareBy<PostPublishCheckpoint> { it.status != PostPublishCheckpointStatus.PENDING }.thenBy { it.dueAtMillis })
        }
    }

    suspend fun ensureFor(parent: CreatorTask): List<PostPublishCheckpoint> = reconcilePublication(parent)

    suspend fun migrateLegacy(legacyTasks: List<CreatorTask>): List<PostPublishCheckpoint> = CreatorDataGate.transaction { creatorPostPublishMutationMutex.withLock {
        if (legacyTasks.isEmpty()) return@withLock load()
        val current = load().toMutableList()
        val byId = current.associateBy { it.id }.toMutableMap()
        legacyTasks.mapNotNull(CreatorPostPublishEngine::fromLegacyTask).forEach { migrated ->
            val existing = byId[migrated.id]
            val merged = when {
                existing == null -> migrated
                existing.status == PostPublishCheckpointStatus.PENDING && migrated.status != PostPublishCheckpointStatus.PENDING -> migrated
                else -> existing
            }
            byId[migrated.id] = merged
        }
        val updated = byId.values.sortedWith(compareBy<PostPublishCheckpoint> { it.projectId }.thenBy { it.dueAtMillis })
        saveUnlocked(updated)
        updated
    } }

    suspend fun updateStatus(
        checkpointId: String,
        status: PostPublishCheckpointStatus,
        atMillis: Long = System.currentTimeMillis(),
    ): PostPublishCheckpoint? = CreatorDataGate.transaction { creatorPostPublishMutationMutex.withLock {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.id == checkpointId }
        if (index == -1) return@withLock null
        if (current[index].status == status) return@withLock current[index]
        val updated = current[index].copy(
            status = status,
            completedAtMillis = if (status == PostPublishCheckpointStatus.DONE) atMillis else 0L,
        )
        current[index] = updated
        saveUnlocked(current)
        updated
    } }

    suspend fun deleteForProjects(projectIds: Set<String>) = CreatorDataGate.transaction { creatorPostPublishMutationMutex.withLock {
        if (projectIds.isEmpty()) return@withLock
        val current = load()
        val updated = current.filterNot { it.projectId in projectIds }
        if (updated.size != current.size) saveUnlocked(updated)
    } }

    suspend fun exportJson(): String = encode(load())

    suspend fun importJson(raw: String): List<PostPublishCheckpoint> {
        val decoded = decode(raw)
        save(decoded)
        return decoded
    }

    fun validateJson(raw: String): Int = decode(raw).size

    private suspend fun saveUnlocked(checkpoints: List<PostPublishCheckpoint>) {
        val encoded = encode(checkpoints)
        context.creatorPostPublishDataStore.edit { prefs -> prefs[checkpointsKey] = encoded }
    }

    private fun encode(checkpoints: List<PostPublishCheckpoint>): String {
        val array = JSONArray()
        checkpoints.forEach { checkpoint ->
            array.put(
                JSONObject()
                    .put("id", checkpoint.id)
                    .put("projectId", checkpoint.projectId)
                    .put("kind", checkpoint.kind.name)
                    .put("title", checkpoint.title)
                    .put("description", checkpoint.description)
                    .put("dueAtMillis", checkpoint.dueAtMillis)
                    .put("status", checkpoint.status.name)
                    .put("completedAtMillis", checkpoint.completedAtMillis)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<PostPublishCheckpoint> {
        val array = JSONArray(raw)
        val seen = mutableSetOf<String>()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.optString("id").trim()
                val projectId = item.optString("projectId").trim()
                require(id.isNotBlank()) { "Post-publish checkpoint $i has no id" }
                require(projectId.isNotBlank()) { "Post-publish checkpoint $i has no project id" }
                require(seen.add(id)) { "Duplicate post-publish checkpoint id: $id" }
                add(
                    PostPublishCheckpoint(
                        id = id,
                        projectId = projectId,
                        kind = runCatching {
                            PostPublishCheckpointKind.valueOf(item.optString("kind"))
                        }.getOrDefault(PostPublishCheckpointKind.PERFORMANCE_24H),
                        title = item.optString("title", "Post-publish check"),
                        description = item.optString("description", "Review this published project."),
                        dueAtMillis = item.optLong("dueAtMillis", 0L),
                        status = runCatching {
                            PostPublishCheckpointStatus.valueOf(item.optString("status"))
                        }.getOrDefault(PostPublishCheckpointStatus.PENDING),
                        completedAtMillis = item.optLong("completedAtMillis", 0L),
                    )
                )
            }
        }
    }
}
