package com.framebynavin.app.data

private const val WORKFLOW_DAY_MS = 24L * 60L * 60L * 1000L

enum class CreatorWorkflowTimingBasis {
    TIMELINE_EXACT,
    TIMELINE_OBSERVED,
    PULSE_OBSERVED,
    NONE,
}

data class CreatorWorkflowStageEvidence(
    val stageId: String,
    val stageLabel: String,
    /** Lower-bound Project Pulse samples: first check-in -> Stage Done. */
    val completionSamples: Int,
    val medianObservedSpanMillis: Long,
    val responseCount: Int,
    /** Stage timeline samples: ENTERED -> EXITED. */
    val timelineSamples: Int = 0,
    /** Timeline samples whose entry was created by project creation or an observed stage transition. */
    val exactTimelineSamples: Int = 0,
    val medianTimeInStageMillis: Long = 0L,
    val timingBasis: CreatorWorkflowTimingBasis = CreatorWorkflowTimingBasis.NONE,
) {
    val preferredTimingMillis: Long
        get() = if (medianTimeInStageMillis > 0L) medianTimeInStageMillis else medianObservedSpanMillis

    val preferredSampleCount: Int
        get() = when (timingBasis) {
            CreatorWorkflowTimingBasis.TIMELINE_EXACT -> exactTimelineSamples
            CreatorWorkflowTimingBasis.TIMELINE_OBSERVED -> timelineSamples
            CreatorWorkflowTimingBasis.PULSE_OBSERVED -> completionSamples
            CreatorWorkflowTimingBasis.NONE -> 0
        }
}

data class CreatorWorkflowIntelligenceSnapshot(
    val responseCount7Days: Int,
    val stageDone7Days: Int,
    val working7Days: Int,
    val snoozed7Days: Int,
    val dismissed7Days: Int,
    val paused7Days: Int,
    val stageEvidence: List<CreatorWorkflowStageEvidence>,
    val historicalBottleneck: CreatorWorkflowStageEvidence?,
    val activeBottleneckLabel: String?,
    val activeBottleneckCount: Int,
) {
    val measuredStageCompletions: Int get() = stageEvidence.sumOf { it.completionSamples }
    val measuredTimelineExits: Int get() = stageEvidence.sumOf { it.timelineSamples }
}

/**
 * Deterministic, local-only workflow intelligence.
 *
 * Project Pulse events are not work timers. Their duration is a lower-bound observed span.
 * The durable stage timeline measures stage residence (ENTERED -> EXITED), which is still elapsed
 * workflow time rather than active hands-on work. Existing stages first seen after an upgrade are
 * marked OBSERVED_CURRENT; stages created or transitioned while FrameByNavin is watching are exact
 * residence samples from that transition onward.
 */
object CreatorWorkflowIntelligenceEngine {
    fun snapshot(
        tasks: List<CreatorTask>,
        events: List<ProjectPulseHistoryEvent>,
        timeline: List<CreatorWorkflowTimelineEvent> = emptyList(),
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorWorkflowIntelligenceSnapshot {
        val validEvents = events.filter { it.atMillis > 0L && it.atMillis <= nowMillis }.sortedBy { it.atMillis }
        val validTimeline = timeline.filter { it.atMillis > 0L && it.atMillis <= nowMillis }.sortedBy { it.atMillis }
        val weekStart = nowMillis - 7L * WORKFLOW_DAY_MS
        val recent = validEvents.filter { it.atMillis >= weekStart }

        val pulseSamplesByStage = linkedMapOf<Pair<String, String>, MutableList<Long>>()
        val responsesByStage = linkedMapOf<Pair<String, String>, Int>()
        val segmentStarts = mutableMapOf<Triple<String, String, String>, Long>()

        validEvents.forEach { event ->
            val stageKey = event.stageId to event.stageLabel
            responsesByStage[stageKey] = (responsesByStage[stageKey] ?: 0) + 1
            val segmentKey = Triple(event.taskId, event.stageId, event.stageLabel)
            if (event.response == ProjectPulseResponse.STAGE_DONE) {
                val startedAt = segmentStarts.remove(segmentKey)
                if (startedAt != null && event.atMillis > startedAt) {
                    val span = event.atMillis - startedAt
                    if (span in 1..(90L * WORKFLOW_DAY_MS)) {
                        pulseSamplesByStage.getOrPut(stageKey) { mutableListOf() }.add(span)
                    }
                }
            } else {
                segmentStarts.putIfAbsent(segmentKey, event.atMillis)
            }
        }

        data class TimelineSample(val millis: Long, val exact: Boolean)
        val timelineSamplesByStage = linkedMapOf<Pair<String, String>, MutableList<TimelineSample>>()
        val openEntries = mutableMapOf<Triple<String, String, String>, CreatorWorkflowTimelineEvent>()
        validTimeline.forEach { event ->
            val segmentKey = Triple(event.taskId, event.stageId, event.stageLabel)
            when (event.type) {
                CreatorWorkflowTimelineType.ENTERED -> openEntries.putIfAbsent(segmentKey, event)
                CreatorWorkflowTimelineType.EXITED -> {
                    val entered = openEntries.remove(segmentKey) ?: return@forEach
                    val span = event.atMillis - entered.atMillis
                    if (span !in 1..(90L * WORKFLOW_DAY_MS)) return@forEach
                    val exact = entered.source == CreatorWorkflowTimelineSource.PROJECT_CREATED ||
                        entered.source == CreatorWorkflowTimelineSource.STAGE_TRANSITION
                    val key = event.stageId to event.stageLabel
                    timelineSamplesByStage.getOrPut(key) { mutableListOf() }.add(TimelineSample(span, exact))
                }
            }
        }

        val stageKeys = (responsesByStage.keys + pulseSamplesByStage.keys + timelineSamplesByStage.keys).distinct()
        val evidence = stageKeys.map { key ->
            val pulseSamples = pulseSamplesByStage[key].orEmpty().sorted()
            val timelineSamples = timelineSamplesByStage[key].orEmpty()
            val exactTimeline = timelineSamples.filter { it.exact }.map { it.millis }.sorted()
            val allTimeline = timelineSamples.map { it.millis }.sorted()
            val basis = when {
                exactTimeline.isNotEmpty() -> CreatorWorkflowTimingBasis.TIMELINE_EXACT
                allTimeline.isNotEmpty() -> CreatorWorkflowTimingBasis.TIMELINE_OBSERVED
                pulseSamples.isNotEmpty() -> CreatorWorkflowTimingBasis.PULSE_OBSERVED
                else -> CreatorWorkflowTimingBasis.NONE
            }
            CreatorWorkflowStageEvidence(
                stageId = key.first,
                stageLabel = key.second.ifBlank { key.first.ifBlank { "Stage" } },
                completionSamples = pulseSamples.size,
                medianObservedSpanMillis = median(pulseSamples),
                responseCount = responsesByStage[key] ?: 0,
                timelineSamples = allTimeline.size,
                exactTimelineSamples = exactTimeline.size,
                medianTimeInStageMillis = when (basis) {
                    CreatorWorkflowTimingBasis.TIMELINE_EXACT -> median(exactTimeline)
                    CreatorWorkflowTimingBasis.TIMELINE_OBSERVED -> median(allTimeline)
                    else -> 0L
                },
                timingBasis = basis,
            )
        }.sortedWith(compareByDescending<CreatorWorkflowStageEvidence> { it.preferredSampleCount }
            .thenByDescending { it.preferredTimingMillis })

        // Prefer true stage-transition residence evidence. Until two such samples exist, preserve
        // v100's explicitly-labelled lower-bound Project Pulse bottleneck instead of inventing certainty.
        val exactCandidates = evidence.filter {
            it.exactTimelineSamples >= 2 && it.medianTimeInStageMillis > 0L
        }
        val historicalBottleneck = if (exactCandidates.isNotEmpty()) {
            exactCandidates.maxByOrNull { it.medianTimeInStageMillis }
        } else {
            evidence.filter { it.completionSamples >= 2 && it.medianObservedSpanMillis > 0L }
                .maxByOrNull { it.medianObservedSpanMillis }
        }

        val active = tasks.asSequence()
            .filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SKIPPED && it.archivedAtMillis == 0L }
            .groupingBy { CreatorWorkflowEngine.currentStage(it).label }
            .eachCount()
            .maxByOrNull { it.value }
            ?.takeIf { it.value >= 2 }

        return CreatorWorkflowIntelligenceSnapshot(
            responseCount7Days = recent.size,
            stageDone7Days = recent.count { it.response == ProjectPulseResponse.STAGE_DONE },
            working7Days = recent.count { it.response == ProjectPulseResponse.WORKING },
            snoozed7Days = recent.count { it.response == ProjectPulseResponse.SNOOZED },
            dismissed7Days = recent.count { it.response == ProjectPulseResponse.DISMISSED },
            paused7Days = recent.count { it.response == ProjectPulseResponse.PAUSED },
            stageEvidence = evidence,
            historicalBottleneck = historicalBottleneck,
            activeBottleneckLabel = active?.key,
            activeBottleneckCount = active?.value ?: 0,
        )
    }

    private fun median(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle]
        else (values[middle - 1] / 2L) + (values[middle] / 2L)
    }
}
