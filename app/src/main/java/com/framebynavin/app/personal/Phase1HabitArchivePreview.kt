package com.framebynavin.app.personal

import android.content.Context
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only, archive-version-aware comparison for #327.
 *
 * A v1 archive has NO knowledge of the new challenge store; absence of its
 * field is never mistaken for an empty, successfully backed-up 21-day history.
 * Nothing here grants a restore permit, creates challenge Done receipts, or
 * schedules/cancels alarms. Conflicting receipt instants demand owner review.
 */
internal data class Phase1HabitArchiveDiff(
    val archiveHasChallengeCoverage: Boolean,
    val localChallenges: Int,
    val importedChallenges: Int,
    val identical: Int,
    val additions: Int,
    val conflicts: Int,
    val localOnly: Int,
    val importedDoneReceipts: Int,
    val conflictingReceiptDates: Int,
) {
    init {
        require(listOf(localChallenges, importedChallenges, identical, additions,
            conflicts, localOnly, importedDoneReceipts, conflictingReceiptDates)
            .all { it >= 0 })
        require(importedChallenges == identical + additions + conflicts)
        if (!archiveHasChallengeCoverage) {
            require(importedChallenges == 0 && importedDoneReceipts == 0)
        }
    }

    val needsOwnerReview: Boolean get() =
        !archiveHasChallengeCoverage || conflicts > 0 || additions > 0 || localOnly > 0
    val authorizesRestore: Boolean get() = false
    val warning: String get() = when {
        !archiveHasChallengeCoverage ->
            "This legacy ZIP does not contain single-activity challenge history. No restore was applied."
        conflicts > 0 ->
            "Existing 21-day challenge IDs differ, including possible Done receipts. Owner conflict review is required."
        else -> "Read-only 21-day challenge comparison. No data was restored or scheduled."
    }
}

internal object Phase1HabitArchivePreviewPolicy {
    fun compare(
        local: List<Phase1HabitChallenge>,
        incomingEncoded: String?,
    ): Phase1HabitArchiveDiff {
        local.forEach { it.checked() }
        val localById = local.associateBy { it.id }
        require(localById.size == local.size) { "Duplicate local challenge identity" }

        if (incomingEncoded == null) {
            return Phase1HabitArchiveDiff(
                archiveHasChallengeCoverage = false,
                localChallenges = local.size,
                importedChallenges = 0,
                identical = 0, additions = 0, conflicts = 0, localOnly = local.size,
                importedDoneReceipts = 0, conflictingReceiptDates = 0,
            )
        }

        val incoming = Phase1HabitCodec.decode(incomingEncoded)
        val incomingById = incoming.associateBy { it.id }
        require(incomingById.size == incoming.size) { "Duplicate imported challenge identity" }
        var identical = 0
        var additions = 0
        var conflicts = 0
        var alteredDays = 0
        incoming.forEach { imported ->
            val existing = localById[imported.id]
            when {
                existing == null -> additions++
                existing == imported -> identical++
                else -> {
                    conflicts++
                    alteredDays += (existing.confirmations.keys + imported.confirmations.keys)
                        .count { day -> existing.confirmations[day] != imported.confirmations[day] }
                }
            }
        }
        return Phase1HabitArchiveDiff(
            archiveHasChallengeCoverage = true,
            localChallenges = local.size,
            importedChallenges = incoming.size,
            identical = identical, additions = additions, conflicts = conflicts,
            localOnly = localById.keys.count { it !in incomingById },
            importedDoneReceipts = incoming.sumOf { it.confirmations.size },
            conflictingReceiptDates = alteredDays,
        )
    }
}

/**
 * Authenticate every ZIP part and private JPEG first, then compare the new
 * single-activity store under the existing Personal mutation gate.
 *
 * This intentionally does NOT call legacy V301/B03 restore (which is blocked
 * for v2). The caller owns/closes its InputStream and clears the passphrase.
 */
internal class Phase1HabitArchiveConflictPreview(context: Context) {
    private val app = context.applicationContext

    suspend fun check(input: InputStream, password: CharArray): Phase1HabitArchiveDiff =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            withContext(Dispatchers.IO) {
                val local = Phase1HabitStore(app).load()
                V300EncryptedPersonalArchive.stageAndVerify(input, password, app.cacheDir)
                    .use { stage ->
                        Phase1HabitArchivePreviewPolicy.compare(
                            local, stage.components.singleActivityChallenges,
                        )
                    }
            }
        }
}
