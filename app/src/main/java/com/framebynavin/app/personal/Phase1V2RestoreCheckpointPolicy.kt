package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * A DIFFERENT v2 restore journal format, not an extension of the legacy
 * v1 enum. Never insert entries into StabilizationRestoreWriteStep: older
 * encrypted rollback escrows and durable v1 journals must remain readable.
 *
 * This is a pure policy/codec ONLY: it does not create a journal directory,
 * write any owner data, open a restore permit, or mark restoration complete.
 */
internal enum class Phase1V2RestoreStep {
    PREPARED,
    BEFORE_PERSONAL, PERSONAL_WRITTEN,
    BEFORE_LEGACY_CHALLENGES, LEGACY_CHALLENGES_WRITTEN,
    BEFORE_SINGLE_ACTIVITY, SINGLE_ACTIVITY_WRITTEN,
    BEFORE_GROUPS, GROUPS_WRITTEN,
    BEFORE_SPACES, SPACES_WRITTEN,
    BEFORE_PHOTOS, PHOTOS_WRITTEN,
    VERIFYING,
}

internal data class Phase1V2RestoreCheckpoint(
    val transactionId: String,
    val preparedId: String,
    val escrowId: String,
    val originalDigest: String,
    val targetDigest: String,
    val step: Phase1V2RestoreStep,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val canRestoreLiveData: Boolean get() = false
    val canFinalizeOrReleaseJournal: Boolean get() = false
    val mayRecoverAutomatically: Boolean get() = false
}

/**
 * Fixed, checksum-validated v2 journal record. It is NOT an authorization
 * token. A real future writer must still persist/fsync before/after phases,
 * authenticate BOTH ZIPs, hold the recovery gate and get owner approval.
 */
internal object Phase1V2RestoreJournalCodec {
    private const val FORMAT = "backlot.personal.live-restore-journal.v2"
    private const val MAX_BYTES = 4096
    private val uuidPattern = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val hashPattern = Regex("[a-f0-9]{64}")

    fun validate(value: Phase1V2RestoreCheckpoint): Phase1V2RestoreCheckpoint {
        for (id in listOf(value.transactionId, value.preparedId, value.escrowId)) {
            require(uuidPattern.matches(id) && UUID.fromString(id).toString() == id) {
                "Invalid v2 recovery identity"
            }
        }
        require(hashPattern.matches(value.originalDigest) &&
            hashPattern.matches(value.targetDigest)) {
            "Invalid v2 snapshot fingerprint"
        }
        require(value.createdAtMillis > 0L &&
            value.updatedAtMillis >= value.createdAtMillis) {
            "Invalid v2 recovery chronology"
        }
        return value
    }

    fun encode(value: Phase1V2RestoreCheckpoint): ByteArray {
        val record = validate(value)
        val payload = listOf(
            FORMAT, record.transactionId, record.preparedId, record.escrowId,
            record.originalDigest, record.targetDigest, record.step.name,
            record.createdAtMillis.toString(), record.updatedAtMillis.toString(),
        ).joinToString("\n", postfix = "\n")
        val bytes = (payload + sha256(payload.toByteArray(Charsets.UTF_8)) + "\n")
            .toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..MAX_BYTES)
        return bytes
    }

    fun decode(bytes: ByteArray): Phase1V2RestoreCheckpoint {
        require(bytes.size in 1..MAX_BYTES) { "Invalid v2 journal length" }
        val lines = bytes.toString(Charsets.UTF_8).split('\n')
        require(lines.size == 11 && lines.last().isEmpty() && lines[0] == FORMAT) {
            "Unsupported or incomplete v2 recovery checkpoint"
        }
        val payload = lines.take(9).joinToString("\n", postfix = "\n")
        require(lines[9] == sha256(payload.toByteArray(Charsets.UTF_8))) {
            "V2 recovery checkpoint checksum mismatch"
        }
        return validate(Phase1V2RestoreCheckpoint(
            transactionId = lines[1], preparedId = lines[2], escrowId = lines[3],
            originalDigest = lines[4], targetDigest = lines[5],
            step = Phase1V2RestoreStep.valueOf(lines[6]),
            createdAtMillis = lines[7].toLong(), updatedAtMillis = lines[8].toLong(),
        ))
    }

    /** Model of an adjacent fsync checkpoint, never a disk mutation. */
    fun advance(
        old: Phase1V2RestoreCheckpoint,
        next: Phase1V2RestoreStep,
        atMillis: Long,
    ): Phase1V2RestoreCheckpoint {
        validate(old)
        require(next.ordinal == old.step.ordinal + 1 &&
            atMillis >= old.updatedAtMillis) {
            "V2 recovery must not skip, repeat or rewind a checkpoint"
        }
        return old.copy(step = next, updatedAtMillis = atMillis)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}

/**
 * Do NOT change V304RestorePlanPolicy.fingerprint: it is part of the v1
 * on-disk journal/rollback protocol and excludes this new DataStore.
 *
 * The v2 SHA-256 uses domain separation, length-prefixed fields and exact
 * single-activity Done receipt instants. Null is never interpreted as empty.
 */
internal object Phase1V2RestoreFingerprint {
    fun calculate(
        data: V300PersonalComponents,
        photos: List<V290SpacePhotoEntity>,
    ): String {
        val habits = requireNotNull(data.singleActivityChallenges) {
            "Legacy Personal archive does not cover new 21-day challenge history"
        }
        Phase1HabitCodec.decode(habits)
        val personal = PersonalCompletionBackupCodec.decode(data.personal)
        V297ChallengeCodec.decode(data.challenges)
        V298ReminderGroupCodec.decode(data.groups)
        val spaces = V303SpaceReceiptArchiveCodec.decode(data.spaces, personal)
        val spaceIds = spaces.spaces.mapTo(HashSet()) { it.id }
        val sortedPhotos = photos.sortedBy { it.id }
        require(sortedPhotos.map { it.id }.distinct().size == sortedPhotos.size) {
            "Duplicate private photo record in v2 recovery fingerprint"
        }
        require(sortedPhotos.map { it.spaceId to it.fileName }.distinct().size == sortedPhotos.size) {
            "Conflicting private photo path in v2 recovery fingerprint"
        }
        sortedPhotos.forEach {
            V290PhotoTimelinePolicy.validate(it)
            require(it.spaceId in spaceIds) { "Orphan private photo in v2 recovery" }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        fun add(field: String) {
            val bytes = field.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        add("backlot.personal.restore.snapshot.v2")
        add(data.personal)
        add(data.challenges)
        add(habits)
        add(data.groups)
        add(data.spaces)
        add(sortedPhotos.size.toString())
        sortedPhotos.forEach { p ->
            add(p.id); add(p.spaceId); add(p.fileName); add(p.sha256Hex)
            add(p.takenAtMillis.toString()); add(p.addedAtMillis.toString())
            add(p.widthPx.toString()); add(p.heightPx.toString()); add(p.note)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

internal enum class Phase1V2HabitsRecoveryState { ORIGINAL, TARGET, UNCHANGED }

internal data class Phase1V2HabitsRecoveryAssessment(
    val state: Phase1V2HabitsRecoveryState,
    val checkpoint: Phase1V2RestoreStep,
) {
    val mayWrite: Boolean get() = false
    val mayAutoResume: Boolean get() = false
}

/**
 * Read-only interrupted-write forensics for the NEW atomic DataStore domain.
 * At BEFORE_SINGLE_ACTIVITY the live value may be either the entire original
 * or the entire target, never a mixture, unknown record or invented Done.
 */
internal object Phase1V2HabitsRecoveryPolicy {
    fun inspect(
        checkpoint: Phase1V2RestoreCheckpoint,
        original: V300PersonalComponents,
        target: V300PersonalComponents,
        observed: V300PersonalComponents,
        originalPhotos: List<V290SpacePhotoEntity>,
        targetPhotos: List<V290SpacePhotoEntity>,
    ): Phase1V2HabitsRecoveryAssessment {
        Phase1V2RestoreJournalCodec.validate(checkpoint)
        require(checkpoint.originalDigest ==
            Phase1V2RestoreFingerprint.calculate(original, originalPhotos) &&
            checkpoint.targetDigest ==
                Phase1V2RestoreFingerprint.calculate(target, targetPhotos)) {
            "V2 restore snapshots differ from the authenticated checkpoint"
        }
        val old = Phase1HabitCodec.decode(requireNotNull(original.singleActivityChallenges))
            .sortedBy { it.id }
        val intended = Phase1HabitCodec.decode(requireNotNull(target.singleActivityChallenges))
            .sortedBy { it.id }
        val actual = Phase1HabitCodec.decode(requireNotNull(observed.singleActivityChallenges) {
            "Live new challenge data cannot be silently treated as empty"
        }).sortedBy { it.id }
        val isOriginal = actual == old
        val isTarget = actual == intended
        require(isOriginal || isTarget) {
            "Unknown live 21-day challenge history; owner recovery required"
        }
        val before = Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY.ordinal
        when {
            checkpoint.step.ordinal < before -> require(isOriginal) {
                "21-day challenge changed before its durable v2 restore checkpoint"
            }
            checkpoint.step.ordinal > before -> require(isTarget) {
                "21-day challenge target missing after its durable v2 checkpoint"
            }
        }
        return Phase1V2HabitsRecoveryAssessment(
            state = when {
                isOriginal && isTarget -> Phase1V2HabitsRecoveryState.UNCHANGED
                isOriginal -> Phase1V2HabitsRecoveryState.ORIGINAL
                else -> Phase1V2HabitsRecoveryState.TARGET
            },
            checkpoint = checkpoint.step,
        )
    }
}
