package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import com.framebynavin.app.reminders.ReminderMusicMode

/** V2 source-bound crash forensics. NO live writes, replay or authority. */
internal enum class Phase1V2RecoveryDomain {
    PERSONAL, LEGACY_CHALLENGES, SINGLE_ACTIVITY, GROUPS, SPACES,
}
internal enum class Phase1V2RecoveryDomainState { ORIGINAL, TARGET, UNCHANGED }

internal data class Phase1V2RestoreForensicAssessment(
    val checkpoint: Phase1V2RestoreStep,
    val domains: Map<Phase1V2RecoveryDomain, Phase1V2RecoveryDomainState>,
    val requiresIndependentPhotoProof: Boolean,
) {
    val authorizesLiveWrite: Boolean get() = false
    val mayAutoRestore: Boolean get() = false
    val authorizesJournalRelease: Boolean get() = false
}

/**
 * Every store prior to its BEFORE checkpoint must be original, every store
 * after its BEFORE checkpoint must be target, and the currently marked
 * BEFORE store may be either whole atomic revision, never invented history.
 * Separate JPEG byte integrity still needs an independently attested stage.
 */
internal object Phase1V2RestoreDomainForensics {
    fun inspect(
        journal: Phase1V2RestoreCheckpoint,
        baseline: V300PersonalComponents,
        target: V300PersonalComponents,
        observed: V300PersonalComponents,
        originalPhotos: List<V290SpacePhotoEntity>,
        targetPhotos: List<V290SpacePhotoEntity>,
        observedPhotos: List<V290SpacePhotoEntity>,
    ): Phase1V2RestoreForensicAssessment {
        Phase1V2RestoreJournalCodec.validate(journal)
        require(journal.originalDigest ==
            Phase1V2RestoreFingerprint.calculate(baseline, originalPhotos) &&
            journal.targetDigest ==
                Phase1V2RestoreFingerprint.calculate(target, targetPhotos)) {
            "V2 journal does not match authenticated original and target"
        }
        val observedHabits = requireNotNull(observed.singleActivityChallenges) {
            "Live 21-day history is uncovered, not an empty snapshot"
        }
        val states = linkedMapOf<Phase1V2RecoveryDomain, Phase1V2RecoveryDomainState>()
        states[Phase1V2RecoveryDomain.PERSONAL] = classify(
            Phase1V2RecoveryDomain.PERSONAL,
            Phase1V2RestoreStep.BEFORE_PERSONAL, journal.step,
            personal(baseline.personal), personal(target.personal),
            personal(observed.personal),
        )
        states[Phase1V2RecoveryDomain.LEGACY_CHALLENGES] = classify(
            Phase1V2RecoveryDomain.LEGACY_CHALLENGES,
            Phase1V2RestoreStep.BEFORE_LEGACY_CHALLENGES, journal.step,
            V297ChallengeCodec.decode(baseline.challenges).sortedBy { it.id },
            V297ChallengeCodec.decode(target.challenges).sortedBy { it.id },
            V297ChallengeCodec.decode(observed.challenges).sortedBy { it.id },
        )
        states[Phase1V2RecoveryDomain.SINGLE_ACTIVITY] = classify(
            Phase1V2RecoveryDomain.SINGLE_ACTIVITY,
            Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY, journal.step,
            Phase1HabitCodec.decode(requireNotNull(baseline.singleActivityChallenges))
                .sortedBy { it.id },
            Phase1HabitCodec.decode(requireNotNull(target.singleActivityChallenges))
                .sortedBy { it.id },
            Phase1HabitCodec.decode(observedHabits).sortedBy { it.id },
        )
        states[Phase1V2RecoveryDomain.GROUPS] = classify(
            Phase1V2RecoveryDomain.GROUPS,
            Phase1V2RestoreStep.BEFORE_GROUPS, journal.step,
            V298ReminderGroupCodec.decode(baseline.groups).sortedBy { it.id },
            V298ReminderGroupCodec.decode(target.groups).sortedBy { it.id },
            V298ReminderGroupCodec.decode(observed.groups).sortedBy { it.id },
        )
        states[Phase1V2RecoveryDomain.SPACES] = classify(
            Phase1V2RecoveryDomain.SPACES,
            Phase1V2RestoreStep.BEFORE_SPACES, journal.step,
            spaces(baseline), spaces(target), spaces(observed),
        )
        val needsPhotos =
            journal.step.ordinal >= Phase1V2RestoreStep.BEFORE_PHOTOS.ordinal
        if (!needsPhotos) {
            require(photos(observedPhotos) == photos(originalPhotos)) {
                "Private photos changed before their durable checkpoint"
            }
        }
        // At and after BEFORE_PHOTOS, records can be partially applied; only
        // authenticated staged JPEG bytes may prove the final photo state.
        return Phase1V2RestoreForensicAssessment(journal.step, states, needsPhotos)
    }

    private fun personal(raw: String):
        Pair<List<PersonalReminder>, List<PersonalCompletionEvent>> {
        val snapshot = PersonalCompletionBackupCodec.decode(raw)
        val records = PersonalBackupPolicy.resetDeviceLeases(snapshot.reminders)
            .map { entry ->
                if (entry.voiceMusicMode == ReminderMusicMode.CUSTOM) {
                    entry.copy(voiceMusicMode = ReminderMusicMode.PIANO,
                        voiceMusicUri = "").checked()
                } else entry.copy(voiceMusicUri = "").checked()
            }.sortedBy { it.id }
        return records to snapshot.completions.sortedBy { it.stableKey }
    }

    private fun spaces(data: V300PersonalComponents): V289SpaceState =
        V303SpaceReceiptArchiveCodec.decode(
            data.spaces, PersonalCompletionBackupCodec.decode(data.personal),
        ).let { state ->
            state.copy(
                spaces = state.spaces.sortedBy { it.id },
                links = state.links.sortedWith(
                    compareBy({ it.spaceId }, { it.reminderId })),
                entries = state.entries.sortedBy { it.id },
            )
        }

    private fun photos(data: List<V290SpacePhotoEntity>): List<V290SpacePhotoEntity> {
        data.forEach { V290PhotoTimelinePolicy.validate(it) }
        require(data.map { it.id }.distinct().size == data.size &&
            data.map { it.spaceId to it.fileName }.distinct().size == data.size) {
            "Duplicate or conflicting photo metadata"
        }
        return data.sortedBy { it.id }
    }

    private fun <T> classify(
        domain: Phase1V2RecoveryDomain,
        before: Phase1V2RestoreStep,
        current: Phase1V2RestoreStep,
        original: T, target: T, observed: T,
    ): Phase1V2RecoveryDomainState {
        val isOriginal = observed == original
        val isTarget = observed == target
        require(isOriginal || isTarget) {
            "Unrecognized $domain state: manual owner review required"
        }
        when {
            current.ordinal < before.ordinal -> require(isOriginal) {
                "$domain changed before v2 checkpoint"
            }
            current.ordinal > before.ordinal -> require(isTarget) {
                "$domain missing target after v2 checkpoint"
            }
        }
        return when {
            isOriginal && isTarget -> Phase1V2RecoveryDomainState.UNCHANGED
            isTarget -> Phase1V2RecoveryDomainState.TARGET
            else -> Phase1V2RecoveryDomainState.ORIGINAL
        }
    }
}
