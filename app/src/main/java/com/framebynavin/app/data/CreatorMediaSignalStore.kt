package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.creatorMediaSignalDataStore by preferencesDataStore(name = "creator_media_signals_v147")
private val creatorMediaSignalMutationMutex = Mutex()

/**
 * Local cache for evidence-backed media observations used by Radar and OTT surfaces.
 *
 * Refreshes are merged with the latest cache instead of overwriting it so corroborating evidence
 * from separate source passes survives. Every write runs through the same canonical merger and
 * retention policy used by the UI engines.
 */
class CreatorMediaSignalStore(private val context: Context) {
    private val signalsKey = stringPreferencesKey("signals_json")
    private val backupKey = stringPreferencesKey("signals_json_last_good")

    val signalsFlow: Flow<List<CreatorMediaSignal>> = context.creatorMediaSignalDataStore.data.map { prefs ->
        val raw = prefs[signalsKey] ?: return@map emptyList()
        runCatching { CreatorMediaSignalCodec.decode(raw) }.getOrElse { cause ->
            val backup = prefs[backupKey]
                ?: throw IllegalStateException("Media signal cache is unreadable. The original has been retained.", cause)
            runCatching { CreatorMediaSignalCodec.decode(backup) }
                .getOrElse { throw IllegalStateException("Both media signal cache copies are unreadable.", it) }
        }
    }

    suspend fun load(): List<CreatorMediaSignal> = signalsFlow.first()

    suspend fun mergeRefresh(
        observations: List<CreatorMediaSignal>,
        today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis(),
    ): List<CreatorMediaSignal> = creatorMediaSignalMutationMutex.withLock {
        val normalized = CreatorMediaSignalRetentionPolicy.normalize(
            signals = load() + observations,
            today = today,
            nowMillis = nowMillis,
        )
        saveUnlocked(normalized)
        normalized
    }

    suspend fun prune(
        today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis(),
    ): List<CreatorMediaSignal> = creatorMediaSignalMutationMutex.withLock {
        val normalized = CreatorMediaSignalRetentionPolicy.normalize(load(), today, nowMillis)
        saveUnlocked(normalized)
        normalized
    }

    suspend fun clear() = creatorMediaSignalMutationMutex.withLock {
        context.creatorMediaSignalDataStore.edit { prefs ->
            prefs.remove(signalsKey)
            prefs.remove(backupKey)
        }
    }

    private suspend fun saveUnlocked(signals: List<CreatorMediaSignal>) {
        val encoded = CreatorMediaSignalCodec.encode(signals)
        context.creatorMediaSignalDataStore.edit { prefs ->
            prefs[signalsKey]?.let { previous ->
                if (runCatching { CreatorMediaSignalCodec.decode(previous) }.isSuccess) {
                    prefs[backupKey] = previous
                }
            }
            prefs[signalsKey] = encoded
        }
    }
}

object CreatorMediaSignalRetentionPolicy {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val STALE_NEWS_DAYS = 14L
    private const val RECENT_RELEASE_DAYS = 7L
    const val DEFAULT_MAX_SIGNALS = 250

    fun normalize(
        signals: List<CreatorMediaSignal>,
        today: LocalDate,
        nowMillis: Long,
        maxSignals: Int = DEFAULT_MAX_SIGNALS,
    ): List<CreatorMediaSignal> {
        val futureClockTolerance = 5L * 60L * 1000L
        val safeMax = maxSignals.coerceIn(25, 1000)
        return CreatorMediaSignalMerger.merge(signals)
            .asSequence()
            .filter { signal ->
                signal.publishedAtMillis <= nowMillis + futureClockTolerance
            }
            .filter { signal ->
                val releaseDate = signal.releaseDate
                when {
                    releaseDate != null && !releaseDate.isBefore(today) -> true
                    releaseDate != null && !releaseDate.isBefore(today.minusDays(RECENT_RELEASE_DAYS)) -> true
                    signal.publishedAtMillis <= 0L -> false
                    else -> nowMillis - signal.publishedAtMillis <= STALE_NEWS_DAYS * DAY_MILLIS
                }
            }
            .sortedWith(
                compareByDescending<CreatorMediaSignal> { it.releaseDate != null && !it.releaseDate.isBefore(today) }
                    .thenByDescending { it.publishedAtMillis }
                    .thenBy { it.title.lowercase() }
            )
            .take(safeMax)
            .toList()
    }
}

private object CreatorMediaSignalCodec {
    fun encode(signals: List<CreatorMediaSignal>): String {
        val array = JSONArray()
        signals.forEach { signal ->
            val evidence = JSONArray()
            signal.evidence.forEach { item ->
                evidence.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("label", item.label)
                        .put("url", item.url)
                        .put("tier", item.tier.name)
                )
            }
            val languages = JSONArray()
            signal.languages.sorted().forEach(languages::put)
            array.put(
                JSONObject()
                    .put("id", signal.id)
                    .put("title", signal.title)
                    .put("summary", signal.summary)
                    .put("kind", signal.kind.name)
                    .put("publishedAtMillis", signal.publishedAtMillis)
                    .put("verification", signal.verification.name)
                    .put("evidence", evidence)
                    .put("releaseDate", signal.releaseDate?.toString().orEmpty())
                    .put("platform", signal.platform)
                    .put("languages", languages)
                    .put("relevanceScore", signal.relevanceScore)
                    .put("audienceImpactScore", signal.audienceImpactScore)
            )
        }
        return array.toString()
    }

    fun decode(raw: String): List<CreatorMediaSignal> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.optString("id").trim()
                val title = item.optString("title").trim()
                require(id.isNotBlank()) { "Media signal $index has no id" }
                require(title.isNotBlank()) { "Media signal $index has no title" }

                val evidenceArray = item.optJSONArray("evidence") ?: JSONArray()
                val evidence = buildList {
                    for (evidenceIndex in 0 until evidenceArray.length()) {
                        val rawEvidence = evidenceArray.optJSONObject(evidenceIndex) ?: continue
                        val evidenceId = rawEvidence.optString("id").trim()
                        val label = rawEvidence.optString("label").trim()
                        if (evidenceId.isBlank() && label.isBlank()) continue
                        add(
                            CreatorMediaEvidence(
                                id = evidenceId,
                                label = label,
                                url = rawEvidence.optString("url", ""),
                                tier = enumOrDefault(
                                    rawEvidence.optString("tier"),
                                    CreatorMediaEvidenceTier.SECONDARY,
                                ),
                            )
                        )
                    }
                }

                val languageArray = item.optJSONArray("languages") ?: JSONArray()
                val languages = buildSet {
                    for (languageIndex in 0 until languageArray.length()) {
                        languageArray.optString(languageIndex).trim().takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                val releaseDate = item.optString("releaseDate")
                    .trim()
                    .takeIf(String::isNotBlank)
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                add(
                    CreatorMediaVerificationPolicy.normalize(
                        CreatorMediaSignal(
                            id = id,
                            title = title,
                            summary = item.optString("summary", ""),
                            kind = enumOrDefault(
                                item.optString("kind"),
                                CreatorMediaSignalKind.INDUSTRY_NEWS,
                            ),
                            publishedAtMillis = item.optLong("publishedAtMillis", 0L),
                            verification = enumOrDefault(
                                item.optString("verification"),
                                CreatorMediaVerification.DEVELOPING,
                            ),
                            evidence = evidence,
                            releaseDate = releaseDate,
                            platform = item.optString("platform", ""),
                            languages = languages,
                            relevanceScore = item.optInt("relevanceScore", 50).coerceIn(0, 100),
                            audienceImpactScore = item.optInt("audienceImpactScore", 50).coerceIn(0, 100),
                        )
                    )
                )
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, fallback: T): T =
        runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
}
