package com.framebynavin.app.data

import com.backlot.shared.ideas.BacklotIdea
import com.backlot.shared.ideas.BacklotIdeaAudioSyncState
import com.backlot.shared.ideas.BacklotIdeaCaptureType
import com.backlot.shared.ideas.BacklotIdeaPotential
import com.backlot.shared.ideas.BacklotIdeaStatus
import com.backlot.shared.ideas.BacklotIdeaTranscriptionState

/** Android persistence ↔ cross-platform Idea domain mapping. */
internal fun CreatorIdea.toSharedIdea(): BacklotIdea = BacklotIdea(
    id = id,
    title = title,
    topic = topic,
    categoryId = category.name,
    status = BacklotIdeaStatus.valueOf(status.name),
    potential = BacklotIdeaPotential.valueOf(potential.name),
    platformHint = platformHint,
    formatHint = formatHint,
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    projectTaskId = projectTaskId,
    sourceRefId = sourceRefId,
    reminderAtMillis = reminderAtMillis,
    reminderCadenceId = reminderCadence.name,
    captureType = BacklotIdeaCaptureType.valueOf(captureType.name),
    audioRemoteRef = audioRemoteUrl,
    audioDurationMillis = audioDurationMillis,
    audioMimeType = audioMimeType,
    audioSyncState = BacklotIdeaAudioSyncState.valueOf(audioSyncState.name),
    transcript = transcript,
    transcriptionState = BacklotIdeaTranscriptionState.valueOf(transcriptionState.name),
    transcriptionError = transcriptionError,
    tags = tags,
)

/**
 * Recreates Android's durable Idea record from the shared domain model.
 * `audioLocalPath` is supplied by the Android media adapter after cloud restore; it is never part
 * of the cross-platform contract.
 */
internal fun BacklotIdea.toAndroidIdea(audioLocalPath: String = ""): CreatorIdea = CreatorIdea(
    id = id,
    title = title,
    topic = topic,
    category = enumValueOrDefault(categoryId, IdeaCategory.CONTENT_IDEA),
    status = enumValueOrDefault(status.name, IdeaStatus.INBOX),
    potential = enumValueOrDefault(potential.name, IdeaPotential.MEDIUM),
    platformHint = platformHint,
    formatHint = formatHint,
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    projectTaskId = projectTaskId,
    sourceRefId = sourceRefId,
    reminderAtMillis = reminderAtMillis,
    reminderCadence = enumValueOrDefault(reminderCadenceId, IdeaReminderCadence.ONCE),
    captureType = enumValueOrDefault(captureType.name, IdeaCaptureType.TEXT),
    audioLocalPath = audioLocalPath,
    audioRemoteUrl = audioRemoteRef,
    audioDurationMillis = audioDurationMillis,
    audioMimeType = audioMimeType,
    audioSyncState = enumValueOrDefault(audioSyncState.name, IdeaAudioSyncState.NONE),
    transcript = transcript,
    transcriptionState = enumValueOrDefault(transcriptionState.name, IdeaTranscriptionState.NOT_REQUESTED),
    transcriptionError = transcriptionError,
    tags = tags,
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback
