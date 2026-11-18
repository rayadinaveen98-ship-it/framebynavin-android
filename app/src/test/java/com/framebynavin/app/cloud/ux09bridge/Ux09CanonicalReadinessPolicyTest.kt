package com.framebynavin.app.cloud.ux09bridge

import com.framebynavin.app.cloud.ux09.Ux09LabFeatureGate
import com.framebynavin.app.cloud.ux09bridge.Ux09CanonicalReadinessPolicy.Blocker
import com.framebynavin.app.cloud.ux09bridge.Ux09CanonicalReadinessPolicy.Result
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.IdeaCaptureType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure synthetic policy tests. These never access real Android DataStore. */
class Ux09CanonicalReadinessPolicyTest {
    private val projectId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val ideaId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"

    private fun project() = CreatorTask(
        id = projectId, title = "Original synthetic project",
        platform = "YouTube", contentType = "Video", dueLabel = "Today",
    )
    private fun idea() = CreatorIdea(
        id = ideaId, title = "Original synthetic text idea",
        createdAtMillis = 150L, updatedAtMillis = 150L,
    )

    private fun fixtureProject(task: CreatorTask = project()): String {
        val item = JSONObject()
        Ux09CanonicalReadinessPolicy.projectFieldKeys.forEach { key ->
            item.put(key, when (key) {
                "id" -> task.id
                "title" -> task.title
                "platform" -> task.platform
                "contentType" -> task.contentType
                "dueLabel" -> task.dueLabel
                "status" -> task.status.name
                "progress" -> task.progress
                "workflowStageIndex" -> task.workflowStageIndex
                "alarmSoundUri" -> "content://private-alarm"
                "snoozeCount" -> 3
                "workingUntilMillis" -> 900L
                "reminderEnabled", "voiceEnabled", "smartEscalationEnabled",
                "autoStageReminder", "pulseManagedReminder", "autoRetryEnabled",
                "publicationIsLegacy" -> false
                "contentDna", "workspace" -> JSONObject()
                else -> ""
            })
        }
        return JSONArray().put(item).toString()
    }

    private fun fixtureIdea(idea: CreatorIdea = idea()): String {
        val item = JSONObject()
        Ux09CanonicalReadinessPolicy.ideaFieldKeys.forEach { key ->
            item.put(key, when (key) {
                "id" -> idea.id
                "title" -> idea.title
                "topic" -> idea.topic
                "createdAtMillis" -> idea.createdAtMillis
                "updatedAtMillis" -> idea.updatedAtMillis
                "captureType" -> idea.captureType.name
                "category" -> idea.category.name
                "status" -> idea.status.name
                "potential" -> idea.potential.name
                "transcriptionState" -> idea.transcriptionState.name
                "audioSyncState" -> "NONE"
                "tags" -> JSONArray()
                else -> ""
            })
        }
        return JSONArray().put(item).toString()
    }

    private fun inspectProject(
        task: CreatorTask = project(),
        raw: String = fixtureProject(task),
        roundtrip: (String) -> CreatorTask = {
            task.copy(alarmSoundUri = "", snoozeCount = 0, workingUntilMillis = 0)
        },
    ) = Ux09CanonicalReadinessPolicy.inspectProject(task, raw, roundtrip)

    private fun inspectIdea(
        idea: CreatorIdea = idea(),
        raw: String = fixtureIdea(idea),
        roundtrip: (String) -> CreatorIdea = { idea },
    ) = Ux09CanonicalReadinessPolicy.inspectIdea(idea, raw, roundtrip)

    @Test fun featureRemainsDisabledAndInspectionCannotActivateSync() {
        assertFalse(Ux09LabFeatureGate.ENABLED)
    }

    @Test fun exactly47ProjectAnd26IdeaSourceKeysAreRequired() {
        assertEquals(47, Ux09CanonicalReadinessPolicy.projectFieldKeys.size)
        assertEquals(26, Ux09CanonicalReadinessPolicy.ideaFieldKeys.size)
    }

    @Test fun simpleProjectIsCandidateButDeviceLocalValuesNeverLeaveDevice() {
        val r = inspectProject() as Result.Ready
        assertEquals(projectId, r.entityId)
        assertTrue(r.bytes <= 16_384)
        assertFalse(r.payloadJson.contains("content://"))
        for (key in listOf("alarmSoundUri", "snoozeCount", "workingUntilMillis")) {
            assertFalse(JSONObject(r.payloadJson).has(key))
        }
        assertEquals("Original synthetic project", JSONObject(r.payloadJson).getString("title"))
    }

    @Test fun simpleTextIdeaIsCandidateButLocalAudioAndOwnerReferencesAreExcluded() {
        val r = inspectIdea() as Result.Ready
        val payload = JSONObject(r.payloadJson)
        assertEquals(ideaId, r.entityId)
        for (key in listOf("audioLocalPath", "audioSyncState",
            "transcriptionError", "audioRemoteUrl", "projectTaskId")) {
            assertFalse(payload.has(key))
        }
        assertEquals(150L, payload.getLong("createdAtMillis"))
    }

    @Test fun invalidOrLegacyProjectAndIdeaIdsCannotBeUploaded() {
        assertEquals(Result.Blocked(Blocker.NON_UUID_ID),
            inspectProject(task = project().copy(id = "legacy-id")))
        assertEquals(Result.Blocked(Blocker.NON_UUID_ID),
            inspectIdea(idea = idea().copy(id = "not-a-uuid")))
    }

    @Test fun richProjectWorkspaceAndContentDnaCannotBeSilentlyTruncated() {
        val rich = project().copy(workspace = CreatorContentWorkspace(script = "Important script"))
        assertEquals(Result.Blocked(Blocker.RICH_WORKSPACE_OR_DNA),
            inspectProject(task = rich))
    }

    @Test fun unmergedLegacyScriptStudioSidecarCannotBeMistakenForEmptyWorkspace() {
        val source = inspectProject()
        assertTrue(source is Result.Ready)
        assertEquals(
            Result.Blocked(Blocker.LEGACY_SCRIPT_STUDIO),
            Ux09CanonicalReadinessPolicy.vetoLegacyStudio(
                source, hasUnmigratedScript = true,
            ),
        )
        assertEquals(
            source,
            Ux09CanonicalReadinessPolicy.vetoLegacyStudio(
                source, hasUnmigratedScript = false,
            ),
        )
    }

    @Test fun priorBlockedRecordsStayBlockedRegardlessOfLegacySidecar() {
        val blocked = inspectProject(task = project().copy(
            workspace = CreatorContentWorkspace(script = "Private unsynced script"),
        ))
        assertEquals(
            Result.Blocked(Blocker.RICH_WORKSPACE_OR_DNA),
            Ux09CanonicalReadinessPolicy.vetoLegacyStudio(
                blocked, hasUnmigratedScript = true,
            ),
        )
    }

    @Test fun activeRemindersCannotAccidentallyScheduleOnOtherDevices() {
        val reminder = project().copy(reminderEnabled = true, reminderAtMillis = 1234L)
        assertEquals(Result.Blocked(Blocker.ACTIVE_REMINDER_OR_SCHEDULE),
            inspectProject(task = reminder))
    }

    @Test fun linkedProjectAndFullStageWorkRemainOnPhone() {
        assertEquals(Result.Blocked(Blocker.LINKED_OR_LEGACY_PROJECT),
            inspectProject(task = project().copy(sourceRefId = "linked-plan")))
        assertEquals(Result.Blocked(Blocker.NOT_SIMPLE_PROJECT),
            inspectProject(task = project().copy(progress = 60)))
    }

    @Test fun voiceOrLinkedIdeasRemainLocalUntilMediaAndIdentityMapped() {
        assertEquals(Result.Blocked(Blocker.VOICE_OR_UNPORTABLE_IDEA),
            inspectIdea(idea = idea().copy(captureType = IdeaCaptureType.VOICE)))
        assertEquals(Result.Blocked(Blocker.VOICE_OR_UNPORTABLE_IDEA),
            inspectIdea(idea = idea().copy(projectTaskId = projectId)))
        assertEquals(Result.Blocked(Blocker.VOICE_OR_UNPORTABLE_IDEA),
            inspectIdea(idea = idea().copy(audioRemoteUrl = "https://example.invalid/voice")))
    }

    @Test fun trashedIdeasAndIdeaRemindersAreNotMistakenForDeletes() {
        assertEquals(Result.Blocked(Blocker.IDEA_REMINDER_OR_TRASH),
            inspectIdea(idea = idea().copy(trashedAtMillis = 123)))
        assertEquals(Result.Blocked(Blocker.IDEA_REMINDER_OR_TRASH),
            inspectIdea(idea = idea().copy(reminderAtMillis = 50)))
    }

    @Test fun sourceKeyDriftIsDeniedInsteadOfSilentlyDroppingFields() {
        val item = JSONArray(fixtureProject()).getJSONObject(0)
        item.remove("workspace")
        assertEquals(Result.Blocked(Blocker.INCOMPLETE_SOURCE),
            inspectProject(raw = JSONArray().put(item).toString()))
        item.put("workspace", JSONObject())
        item.put("unreviewedSource", "do not lose")
        assertEquals(Result.Blocked(Blocker.INCOMPLETE_SOURCE),
            inspectProject(raw = JSONArray().put(item).toString()))
    }

    @Test fun wrongSourceIdentityOrMultiRecordSnapshotIsDenied() {
        val raw = fixtureIdea()
        val obj = JSONArray(raw).getJSONObject(0).put("id", projectId)
        assertEquals(Result.Blocked(Blocker.INCOMPLETE_SOURCE),
            inspectIdea(raw = JSONArray().put(obj).toString()))
        val projectItem = JSONArray(fixtureProject()).getJSONObject(0)
        assertEquals(Result.Blocked(Blocker.INCOMPLETE_SOURCE),
            inspectProject(raw = JSONArray().put(projectItem).put(projectItem).toString()))
    }

    @Test fun corruptedOrUnexpectedDecoderRoundtripIsDenied() {
        assertEquals(Result.Blocked(Blocker.ROUND_TRIP_CHANGED),
            inspectProject(roundtrip = { project().copy(title = "Changed after decoding") }))
        assertEquals(Result.Blocked(Blocker.ROUND_TRIP_CHANGED),
            inspectIdea(roundtrip = { idea().copy(notes = "Unexpected mutation") }))
    }

    @Test fun tooLargeRecordStaysLocalAndIsNotTruncated() {
        val o = JSONArray(fixtureProject()).getJSONObject(0)
        o.put("notes", "x".repeat(17_500))
        assertEquals(Result.Blocked(Blocker.OVER_SERVER_LIMIT),
            inspectProject(raw = JSONArray().put(o).toString()))
    }

    @Test fun nestedLocalFileUrisAreNeverEligible() {
        val obj = JSONArray(fixtureProject()).getJSONObject(0)
        obj.put("workspace", JSONObject().put("script", "content://media/secret"))
        assertEquals(Result.Blocked(Blocker.UNSAFE_OR_NONCANONICAL_JSON),
            inspectProject(raw = JSONArray().put(obj).toString()))
    }

    @Test fun forbiddenOwnerTokenFieldInPayloadIsRejected() {
        val ideaObject = JSONArray(fixtureIdea()).getJSONObject(0)
        ideaObject.put("access_token", "bad")
        assertEquals(Result.Blocked(Blocker.INCOMPLETE_SOURCE),
            inspectIdea(raw = JSONArray().put(ideaObject).toString()))
    }

    @Test fun malformedJsonDoesNotTurnIntoAnEmptyList() {
        assertEquals(Result.Blocked(Blocker.INCOMPLETE_SOURCE),
            inspectProject(raw = "not-json"))
        assertEquals(Result.Blocked(Blocker.INCOMPLETE_SOURCE),
            inspectIdea(raw = "{}"))
    }
}
