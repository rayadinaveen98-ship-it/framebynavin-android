package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import com.framebynavin.app.reminders.ReminderMusicMode

/**
 * Testable target-state calculation for the already-pending complete recovery.
 *
 * This class does NOT write or restore active DataStore, Room, files, or alarms.
 * It never authorizes live application of a plan. Only a future, separately
 * verified journaled commit/rollback engine may consume these values.
 */
internal data class StabilizationRestoreTarget(
    val merged: V300PersonalComponents,
    val photos: List<V290SpacePhotoEntity>,
    val startingDigest: String,
    val targetDigest: String,
    val preview: V301RestoreDryRunReport,
) {
    val authorizedToWriteLive: Boolean get() = false
}

internal object StabilizationRestoreTargetPolicy {
    fun calculate(
        verifiedLocal: V301LocalRecoverySnapshot,
        authenticatedIncoming: V300PersonalComponents,
        authenticatedIncomingPhotos: List<V290SpacePhotoEntity>,
    ): StabilizationRestoreTarget {
        // The upstream caller must authenticate incoming archive and all image
        // bytes (V300). Never transform an incomplete, unverified phone.
        require(verifiedLocal.localIntegrityVerified) {
            "Local Personal data has not been verified"
        }
        val report = V301RestoreDryRun.compare(
            verifiedLocal, authenticatedIncoming, authenticatedIncomingPhotos,
        )
        require(!report.requiresConflictReview && report.conflicts == 0) {
            "Conflicting or damaged records cannot be merged automatically"
        }
        val localPersonal = PersonalCompletionBackupCodec.decode(
            verifiedLocal.components.personal)
        val incomingPersonal = PersonalCompletionBackupCodec.decode(
            authenticatedIncoming.personal)
        val safeIncomingReminders = PersonalBackupPolicy.resetDeviceLeases(
            incomingPersonal.reminders,
        ).map { reminder ->
            // A remote document URI is not transferable to another phone.
            if (reminder.voiceMusicMode == ReminderMusicMode.CUSTOM) {
                reminder.copy(voiceMusicMode = ReminderMusicMode.PIANO, voiceMusicUri = "").checked()
            } else reminder.copy(voiceMusicUri = "").checked()
        }
        val targetPersonal = PersonalCompletionBackupSnapshot(
            reminders = appendMissing(
                localPersonal.reminders, safeIncomingReminders, { it.id },
            ),
            completions = appendMissing(
                localPersonal.completions, incomingPersonal.completions, { it.stableKey },
            ),
        )
        val localSpaces = V303SpaceReceiptArchiveCodec.decode(
            verifiedLocal.components.spaces, localPersonal,
        )
        val remoteSpaces = V303SpaceReceiptArchiveCodec.decode(
            authenticatedIncoming.spaces, incomingPersonal,
        )
        val targetSpaceState = V289SpaceState(
            spaces = appendMissing(localSpaces.spaces, remoteSpaces.spaces, { it.id }),
            links = appendMissing(localSpaces.links, remoteSpaces.links, {
                it.spaceId to it.reminderId
            }),
            entries = appendMissing(localSpaces.entries, remoteSpaces.entries, { it.id }),
        )
        val targetPhotos = appendMissing(
            verifiedLocal.photos, authenticatedIncomingPhotos, { it.id },
        )
        targetPhotos.forEach { V290PhotoTimelinePolicy.validate(it) }
        // Different photo IDs that target the same private path would destroy
        // bytes during a subsequent installer rename. Reject before any write.
        require(targetPhotos.map { it.spaceId to it.fileName }.distinct().size ==
            targetPhotos.size) {
            "Two private photo records point to the same destination file"
        }
        val targetComponents = V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(
                targetPersonal.reminders, targetPersonal.completions,
            ),
            challenges = V297ChallengeCodec.encode(appendMissing(
                V297ChallengeCodec.decode(verifiedLocal.components.challenges),
                V297ChallengeCodec.decode(authenticatedIncoming.challenges),
                { it.id },
            )),
            groups = V298ReminderGroupCodec.encode(appendMissing(
                V298ReminderGroupCodec.decode(verifiedLocal.components.groups),
                V298ReminderGroupCodec.decode(authenticatedIncoming.groups),
                { it.id },
            )),
            spaces = V303SpaceReceiptArchiveCodec.encode(
                targetSpaceState, targetPersonal,
            ),
        )
        // Re-decode whole target, checking every group/Space link against
        // merged reminders and every Done Space entry against exact receipt.
        val reconstructed = PersonalCompletionBackupCodec.decode(targetComponents.personal)
        val validReminderIds = reconstructed.reminders.mapTo(HashSet()) { it.id }
        require(V298ReminderGroupCodec.decode(targetComponents.groups).all { group ->
            group.reminderIds.all { it in validReminderIds }
        }) { "Merged group refers to missing Personal reminder" }
        require(V303SpaceReceiptArchiveCodec.decode(targetComponents.spaces, reconstructed)
            .links.all { it.reminderId in validReminderIds }) {
            "Merged Space links refer to missing Personal reminder"
        }
        val mergedSpaceIds = targetSpaceState.spaces.mapTo(HashSet()) { it.id }
        require(targetPhotos.all { it.spaceId in mergedSpaceIds }) {
            "Merged private photo has no Space"
        }
        // This is a proposed target only, never a write permit.
        return StabilizationRestoreTarget(
            merged = targetComponents,
            photos = targetPhotos.sortedBy { it.id },
            startingDigest = V304RestorePlanPolicy.fingerprint(
                verifiedLocal.components, verifiedLocal.photos,
            ),
            targetDigest = V304RestorePlanPolicy.fingerprint(
                targetComponents, targetPhotos,
            ),
            preview = report,
        )
    }

    private fun <T, K> appendMissing(
        local: List<T>,
        incoming: List<T>,
        identity: (T) -> K,
    ): List<T> {
        val ids = local.map(identity)
        val incomingIds = incoming.map(identity)
        require(ids.toSet().size == ids.size &&
            incomingIds.toSet().size == incomingIds.size) {
            "Duplicate imported or local identity"
        }
        val retainedIds = ids.toHashSet()
        return local + incoming.filter { identity(it) !in retainedIds }
    }
}
