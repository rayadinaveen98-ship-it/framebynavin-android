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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Context.creatorRewardDataStore by preferencesDataStore(name = "creator_rewards_v21")
private val creatorRewardMutationMutex = Mutex()

enum class CreatorRewardEventType {
    IDEA_DAILY_CAPTURE,
    IDEA_CONVERTED,
    PROJECT_STAGE_COMPLETED,
    PROJECT_PUBLISHED,
    POST_PUBLISH_COMPLETED,
}

data class CreatorRewardLedgerEntry(
    val eventKey: String,
    val type: CreatorRewardEventType,
    val subjectId: String,
    val projectId: String = "",
    val label: String,
    val xp: Int,
    val momentum: Int,
    val occurredAtMillis: Long,
)

data class CreatorRewardSummary(
    val totalXp: Int,
    val weeklyMomentum: Int,
    val transactionCount: Int,
)

object CreatorRewardEngine {
    fun ideaDailyCapture(ideaId: String, atMillis: Long): CreatorRewardLedgerEntry = CreatorRewardLedgerEntry(
        eventKey = "idea-day:${localDateKey(atMillis)}",
        type = CreatorRewardEventType.IDEA_DAILY_CAPTURE,
        subjectId = ideaId,
        label = "Captured an idea",
        xp = 5,
        momentum = 5,
        occurredAtMillis = atMillis,
    )

    fun ideaConverted(ideaId: String, projectId: String, atMillis: Long): CreatorRewardLedgerEntry = CreatorRewardLedgerEntry(
        eventKey = "idea-converted:$ideaId",
        type = CreatorRewardEventType.IDEA_CONVERTED,
        subjectId = ideaId,
        projectId = projectId,
        label = "Turned an idea into a project",
        xp = 10,
        momentum = 10,
        occurredAtMillis = atMillis,
    )

    fun stageCompleted(projectId: String, stageId: String, stageLabel: String, atMillis: Long): CreatorRewardLedgerEntry = CreatorRewardLedgerEntry(
        eventKey = "stage:$projectId:$stageId",
        type = CreatorRewardEventType.PROJECT_STAGE_COMPLETED,
        subjectId = stageId,
        projectId = projectId,
        label = "Completed $stageLabel",
        xp = 10,
        momentum = 10,
        occurredAtMillis = atMillis,
    )

    fun projectPublished(projectId: String, title: String, atMillis: Long): CreatorRewardLedgerEntry = CreatorRewardLedgerEntry(
        eventKey = "published:$projectId",
        type = CreatorRewardEventType.PROJECT_PUBLISHED,
        subjectId = projectId,
        projectId = projectId,
        label = "Published $title",
        xp = 40,
        momentum = 40,
        occurredAtMillis = atMillis,
    )

    fun postPublishCompleted(checkpoint: PostPublishCheckpoint, atMillis: Long): CreatorRewardLedgerEntry {
        val reward = when (checkpoint.kind) {
            PostPublishCheckpointKind.CROSS_PROMOTE -> 5
            PostPublishCheckpointKind.PERFORMANCE_24H -> 10
            PostPublishCheckpointKind.PERFORMANCE_7D -> 15
        }
        return CreatorRewardLedgerEntry(
            eventKey = "post-publish:${checkpoint.id}",
            type = CreatorRewardEventType.POST_PUBLISH_COMPLETED,
            subjectId = checkpoint.id,
            projectId = checkpoint.projectId,
            label = checkpoint.title,
            xp = reward,
            momentum = reward,
            occurredAtMillis = atMillis,
        )
    }

    fun summary(entries: List<CreatorRewardLedgerEntry>, nowMillis: Long = System.currentTimeMillis()): CreatorRewardSummary {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        return CreatorRewardSummary(
            totalXp = entries.sumOf { it.xp },
            weeklyMomentum = entries.filter { it.occurredAtMillis >= weekStartMillis }.sumOf { it.momentum },
            transactionCount = entries.size,
        )
    }

    private fun localDateKey(atMillis: Long): String = Instant.ofEpochMilli(atMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

class CreatorRewardStore(private val context: Context) {
    private val ledgerKey = stringPreferencesKey("reward_ledger_json")

    val ledgerFlow: Flow<List<CreatorRewardLedgerEntry>> = context.creatorRewardDataStore.data.map { prefs ->
        decode(prefs[ledgerKey] ?: "[]")
    }

    suspend fun load(): List<CreatorRewardLedgerEntry> = ledgerFlow.first()

    suspend fun record(entry: CreatorRewardLedgerEntry): Boolean = CreatorDataGate.transaction { creatorRewardMutationMutex.withLock {
        val current = load().toMutableList()
        if (current.any { it.eventKey == entry.eventKey }) return@withLock false
        current += entry
        saveUnlocked(current)
        true
    } }

    /** Reconcile a creator-confirmed publication without changing unrelated reward history. */
    suspend fun reconcilePublication(task: CreatorTask): Boolean = CreatorDataGate.transaction {
        creatorRewardMutationMutex.withLock {
            val current = load()
            val key = "published:${task.id}"
            val previous = current.firstOrNull { it.eventKey == key }
            val expected = if (task.publishedAtMillis > 0L)
                CreatorRewardEngine.projectPublished(task.id, task.title, task.publishedAtMillis)
            else null
            val updated = current.filterNot { it.eventKey == key }.toMutableList()
            expected?.let { updated += it }
            if (updated != current) saveUnlocked(updated)
            expected != null && previous == null
        }
    }

    suspend fun exportJson(): String = encode(load())

    suspend fun importJson(raw: String): List<CreatorRewardLedgerEntry> {
        val decoded = decode(raw)
        CreatorDataGate.transaction { creatorRewardMutationMutex.withLock { saveUnlocked(decoded) } }
        return decoded
    }

    fun validateJson(raw: String): Int = decode(raw).size

    private suspend fun saveUnlocked(entries: List<CreatorRewardLedgerEntry>) {
        val encoded = encode(entries)
        context.creatorRewardDataStore.edit { prefs -> prefs[ledgerKey] = encoded }
    }

    private fun encode(entries: List<CreatorRewardLedgerEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("eventKey", entry.eventKey)
                    .put("type", entry.type.name)
                    .put("subjectId", entry.subjectId)
                    .put("projectId", entry.projectId)
                    .put("label", entry.label)
                    .put("xp", entry.xp)
                    .put("momentum", entry.momentum)
                    .put("occurredAtMillis", entry.occurredAtMillis)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<CreatorRewardLedgerEntry> {
        val array = JSONArray(raw)
        val seen = mutableSetOf<String>()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val key = item.optString("eventKey").trim()
                require(key.isNotBlank()) { "Reward entry $i has no event key" }
                require(seen.add(key)) { "Duplicate reward event key: $key" }
                add(
                    CreatorRewardLedgerEntry(
                        eventKey = key,
                        type = runCatching { CreatorRewardEventType.valueOf(item.optString("type")) }
                            .getOrDefault(CreatorRewardEventType.PROJECT_STAGE_COMPLETED),
                        subjectId = item.optString("subjectId", ""),
                        projectId = item.optString("projectId", ""),
                        label = item.optString("label", "Creator progress"),
                        xp = item.optInt("xp", 0).coerceAtLeast(0),
                        momentum = item.optInt("momentum", 0).coerceAtLeast(0),
                        occurredAtMillis = item.optLong("occurredAtMillis", 0L),
                    )
                )
            }
        }.sortedByDescending { it.occurredAtMillis }
    }
}
