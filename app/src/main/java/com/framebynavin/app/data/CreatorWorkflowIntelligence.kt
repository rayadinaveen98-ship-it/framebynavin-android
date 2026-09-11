package com.framebynavin.app.data

private const val WORKFLOW_DAY_MS = 24L * 60L * 60L * 1000L

data class CreatorWorkflowStageEvidence(
    val stageId: String,
    val stageLabel: String,
    val completionSamples: Int,
    val medianObservedSpanMillis: Long,
    val responseCount: Int,
)

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
}

/**
 * Local-only workflow intelligence derived from creator-owned Project Pulse history.
 *
 * A Project Pulse event is not a work timer. Therefore duration samples are deliberately named
 * `observedSpan`: the lower-bound span between the first recorded check-in for a stage and the
 * creator's Stage Done response. We never present this as active editing/research hours.
 */
object CreatorWorkflowIntelligenceEngine {
    fun snapshot(
        tasks: List<CreatorTask>,
        events: List<ProjectPulseHistoryEvent>,
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorWorkflowIntelligenceSnapshot {
        val validEvents = events.filter { it.atMillis > 0L && it.atMillis <= nowMillis }.sortedBy { it.atMillis }
        val weekStart = nowMillis - 7L * WORKFLOW_DAY_MS
        val recent = validEvents.filter { it.atMillis >= weekStart }

        val samplesByStage = linkedMapOf<Pair<String, String>, MutableList<Long>>()
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
                    // Ignore pathological/corrupt spans without pretending they are creator behavior.
                    if (span in 1..(90L * WORKFLOW_DAY_MS)) {
                        samplesByStage.getOrPut(stageKey) { mutableListOf() }.add(span)
                    }
                }
            } else {
                segmentStarts.putIfAbsent(segmentKey, event.atMillis)
            }
        }

        val evidence = (responsesByStage.keys + samplesByStage.keys).distinct().map { key ->
            val samples = samplesByStage[key].orEmpty().sorted()
            CreatorWorkflowStageEvidence(
                stageId = key.first,
                stageLabel = key.second.ifBlank { key.first.ifBlank { "Stage" } },
                completionSamples = samples.size,
                medianObservedSpanMillis = median(samples),
                responseCount = responsesByStage[key] ?: 0,
            )
        }.sortedWith(compareByDescending<CreatorWorkflowStageEvidence> { it.completionSamples }
            .thenByDescending { it.medianObservedSpanMillis })

        // Do not call one isolated completion a consistent bottleneck.
        val historicalBottleneck = evidence
            .filter { it.completionSamples >= 2 && it.medianObservedSpanMillis > 0L }
            .maxByOrNull { it.medianObservedSpanMillis }

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
