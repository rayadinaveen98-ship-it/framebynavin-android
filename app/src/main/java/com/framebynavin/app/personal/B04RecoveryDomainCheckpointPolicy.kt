package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.reminders.ReminderMusicMode

/**
 * One store can be durable even when the next BEFORE checkpoint has not yet
 * been recorded. An interrupted B3 transaction may therefore contain some
 * ORIGINAL stores and some TARGET stores, but never an arbitrary third value.
 *
 * This policy is an independent read-only check for B4 recovery planning.
 * It does not authenticate the caller's archive, inspect JPEG bytes, grant a
 * mutation permit, finalize a journal or authorize replay. Future B4 write
 * code MUST bind its inputs to a freshly authenticated V305 rollback archive,
 * an authenticated incoming V300 archive and the exclusive owner gate.
 */
internal enum class B04RecoveryDomain { PERSONAL, CHALLENGES, GROUPS, SPACES }
internal enum class B04RecoveryDomainState { ORIGINAL, TARGET, UNCHANGED }

internal data class B04RecoveryDomainAssessment(
    val checkpoint: StabilizationRestoreWriteStep,
    val states: Map<B04RecoveryDomain, B04RecoveryDomainState>,
) {
    val grantsRestoreAuthority: Boolean get() = false
    val mayAutomaticallyResume: Boolean get() = false
    val provesPrivatePhotosSafe: Boolean get() = false
    val needsIndependentPhotoAssessment: Boolean get() =
        checkpoint.ordinal >= StabilizationRestoreWriteStep.BEFORE_PHOTOS.ordinal
}

/**
 * Strict checkpoint chronology: completed earlier stores MUST match target;
 * future stores MUST match baseline; only the store with an active BEFORE
 * marker may be either baseline or target (crash during/after atomic write).
 */
internal object B04RecoveryDomainCheckpointPolicy {
    fun inspect(
        journal: StabilizationRestoreJournalRecord,
        baseline: V300PersonalComponents,
        target: V300PersonalComponents,
        observed: V300PersonalComponents,
    ): B04RecoveryDomainAssessment {
        StabilizationRestoreJournalCodec.validate(journal)
        val oldPersonal = personal(baseline.personal)
        val newPersonal = personal(target.personal)
        val livePersonal = personal(observed.personal)
        val statuses = linkedMapOf<B04RecoveryDomain, B04RecoveryDomainState>()
        statuses[B04RecoveryDomain.PERSONAL] = classify(
            B04RecoveryDomain.PERSONAL,
            StabilizationRestoreWriteStep.BEFORE_PERSONAL,
            journal.step, oldPersonal, newPersonal, livePersonal,
        )
        statuses[B04RecoveryDomain.CHALLENGES] = classify(
            B04RecoveryDomain.CHALLENGES,
            StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
            journal.step,
            V297ChallengeCodec.decode(baseline.challenges).sortedBy { it.id },
            V297ChallengeCodec.decode(target.challenges).sortedBy { it.id },
            V297ChallengeCodec.decode(observed.challenges).sortedBy { it.id },
        )
        statuses[B04RecoveryDomain.GROUPS] = classify(
            B04RecoveryDomain.GROUPS,
            StabilizationRestoreWriteStep.BEFORE_GROUPS,
            journal.step,
            V298ReminderGroupCodec.decode(baseline.groups).sortedBy { it.id },
            V298ReminderGroupCodec.decode(target.groups).sortedBy { it.id },
            V298ReminderGroupCodec.decode(observed.groups).sortedBy { it.id },
        )
        statuses[B04RecoveryDomain.SPACES] = classify(
            B04RecoveryDomain.SPACES,
            StabilizationRestoreWriteStep.BEFORE_SPACES,
            journal.step,
            spaces(baseline), spaces(target), spaces(observed),
        )
        return B04RecoveryDomainAssessment(journal.step, statuses)
    }

    private fun personal(raw: String): Pair<List<PersonalReminder>, List<PersonalCompletionEvent>> {
        val parsed = PersonalCompletionBackupCodec.decode(raw)
        val sanitized = PersonalBackupPolicy.resetDeviceLeases(parsed.reminders)
            .map { reminder ->
                if (reminder.voiceMusicMode == ReminderMusicMode.CUSTOM)
                    reminder.copy(voiceMusicMode = ReminderMusicMode.PIANO, voiceMusicUri = "").checked()
                else reminder.copy(voiceMusicUri = "").checked()
            }
            .sortedBy { it.id }
        return sanitized to parsed.completions.sortedBy { it.stableKey }
    }

    private fun spaces(data: V300PersonalComponents): V289SpaceState {
        val snapshot = V303SpaceReceiptArchiveCodec.decode(
            data.spaces, PersonalCompletionBackupCodec.decode(data.personal),
        )
        return snapshot.copy(
            spaces = snapshot.spaces.sortedBy { it.id },
            links = snapshot.links.sortedWith(compareBy({ it.spaceId }, { it.reminderId })),
            entries = snapshot.entries.sortedBy { it.id },
        )
    }

    private fun <T> classify(
        domain: B04RecoveryDomain,
        before: StabilizationRestoreWriteStep,
        step: StabilizationRestoreWriteStep,
        original: T,
        target: T,
        observed: T,
    ): B04RecoveryDomainState {
        val isOriginal = observed == original
        val isTarget = observed == target
        require(isOriginal || isTarget) {
            "Unrecognized live recovery state in $domain; refuse automatic replay"
        }
        when {
            step.ordinal < before.ordinal ->
                require(isOriginal) { "$domain changed before its journal checkpoint" }
            step.ordinal > before.ordinal ->
                require(isTarget) { "$domain did not reach its durable target checkpoint" }
            // BEFORE_* permits a crash either side of one atomic store write.
        }
        return when {
            isOriginal && isTarget -> B04RecoveryDomainState.UNCHANGED
            isTarget -> B04RecoveryDomainState.TARGET
            else -> B04RecoveryDomainState.ORIGINAL
        }
    }
}
