package com.framebynavin.app.data

import java.util.UUID

/**
 * UX05.1D: append-only, manual learning snapshots for a single actual output.
 * These are creator observations, NOT fetched platform analytics, publication
 * confirmations or external identity/permissions.
 */
data class CreatorOutputLearning(
    val id: String = UUID.randomUUID().toString(),
    val deliverableId: String,
    val recordedAtMillis: Long,
    val expectation: String = "",
    val observedOutcome: String = "",
    val whatWorked: String = "",
    val improveNext: String = "",
    val nextAction: String = "",
    val metricLabel: String = "",
    val metricValue: String = "",
    val metricSource: String = "",
)

object CreatorOutputLearningPolicy {
    private const val MAX_NARRATIVE = 2500
    private const val MAX_METRIC = 120
    private const val MAX_SOURCE = 300

    fun append(task: CreatorTask, proposed: CreatorOutputLearning): CreatorContentWorkspace {
        val workspace = task.workspace
        require(task.archivedAtMillis == 0L && task.status != TaskStatus.SKIPPED) {
            "Archived or skipped projects cannot receive a new learning."
        }
        val output = workspace.deliverables.singleOrNull { it.id == proposed.deliverableId }
            ?: error("Output changed or is missing. Reopen its current record.")
        require(output.status == CreatorDeliverableStatus.PUBLISHED &&
            output.publishedAtMillis > 0L) {
            "Record actual publication in Publish Studio before adding post-publish learning."
        }
        require(proposed.recordedAtMillis > 0L) { "A positive observation time is required." }
        require(proposed.id.isNotBlank()) { "Learning identity is required." }
        check(workspace.outputLearnings.none { it.id == proposed.id }) {
            "This learning was already recorded. Reopen to view the saved entry."
        }
        val clean = proposed.copy(
            expectation = proposed.expectation.trim(),
            observedOutcome = proposed.observedOutcome.trim(),
            whatWorked = proposed.whatWorked.trim(),
            improveNext = proposed.improveNext.trim(),
            nextAction = proposed.nextAction.trim(),
            metricLabel = proposed.metricLabel.trim(),
            metricValue = proposed.metricValue.trim(),
            metricSource = proposed.metricSource.trim(),
        )
        require(
            listOf(clean.expectation, clean.observedOutcome, clean.whatWorked,
                clean.improveNext, clean.nextAction).any { it.isNotBlank() }
        ) { "Write at least one observation or lesson." }
        require(
            listOf(clean.expectation, clean.observedOutcome, clean.whatWorked,
                clean.improveNext, clean.nextAction).all { it.length <= MAX_NARRATIVE }
        ) { "Each reflection must contain at most $MAX_NARRATIVE characters." }
        require(clean.metricLabel.length <= MAX_METRIC &&
            clean.metricValue.length <= MAX_METRIC &&
            clean.metricSource.length <= MAX_SOURCE
        ) { "Manual metric fields are too long." }
        require(clean.metricLabel.isBlank() == clean.metricValue.isBlank()) {
            "Add both a metric label and its manually observed value, or neither."
        }
        require(clean.metricValue.isNotBlank() || clean.metricSource.isBlank()) {
            "A metric source requires a metric label and value."
        }
        return workspace.copy(outputLearnings = workspace.outputLearnings + clean)
    }
}
