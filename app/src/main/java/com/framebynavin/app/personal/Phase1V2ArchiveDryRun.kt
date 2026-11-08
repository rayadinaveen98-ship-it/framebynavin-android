package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full nine-domain owner conflict preview, but NEVER an import permit. */
internal data class Phase1V2ArchiveDryRunReport(
    val eightDomains: V301RestoreDryRunReport,
    val newChallenges: Phase1HabitArchiveDiff,
) {
    init {
        require(newChallenges.archiveHasChallengeCoverage) {
            "Missing v2 challenge data must not be treated as empty"
        }
    }
    val proposedAdditions: Int get() = eightDomains.adds + newChallenges.additions
    val conflicts: Int get() = eightDomains.conflicts + newChallenges.conflicts
    val needsOwnerReview: Boolean get() =
        eightDomains.requiresConflictReview || newChallenges.needsOwnerReview
    val canRestore: Boolean get() = false
    val canScheduleAlarms: Boolean get() = false
}

/**
 * V301.compare() is deliberately still blocked for ANY v2 ZIP. This
 * separate read-only path requires both sides to explicitly carry v2 data.
 */
internal object Phase1V2ArchiveDryRunPolicy {
    fun compare(
        local: V301LocalRecoverySnapshot,
        imported: V300PersonalComponents,
        photos: List<V290SpacePhotoEntity>,
    ): Phase1V2ArchiveDryRunReport {
        val original = requireNotNull(local.components.singleActivityChallenges) {
            "Local v2 challenge history is uncovered"
        }
        val received = requireNotNull(imported.singleActivityChallenges) {
            "Imported ZIP lacks v2 challenge coverage"
        }
        val challengeDiff = Phase1HabitArchivePreviewPolicy.compare(
            Phase1HabitCodec.decode(original), received,
        )
        val others = V301RestoreDryRun.compareKnownDomains(local, imported, photos)
        return Phase1V2ArchiveDryRunReport(others, challengeDiff)
    }
}

/**
 * Both read-only sources are inspected inside one owner lock. Authenticate
 * ALL encrypted ZIP parts and private JPEGs; destroy staging on exit. This
 * class cannot authorize actual writes, recovery or alarm scheduling.
 */
internal class Phase1V2ArchiveDryRun(context: Context) {
    private val app = context.applicationContext

    suspend fun check(input: InputStream, password: CharArray):
        Phase1V2ArchiveDryRunReport =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            withContext(Dispatchers.IO) {
                val local = V301LocalRecoveryReader(app).readV2()
                V300EncryptedPersonalArchive.stageAndVerify(
                    input, password, app.cacheDir,
                ).use { archive ->
                    Phase1V2ArchiveDryRunPolicy.compare(
                        local, archive.components, archive.photos.map { it.photo },
                    )
                }
            }
        }
}
