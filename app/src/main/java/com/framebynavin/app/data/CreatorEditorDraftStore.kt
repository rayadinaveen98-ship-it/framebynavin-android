package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ephemeral local draft journal for Content Project editors.
 *
 * Drafts are never authoritative creator data. They are accepted only while the exact canonical
 * workspace/studio revision they were based on is still current. A successful save or explicit
 * discard clears the matching draft. This protects long edits across Activity/process recreation
 * without weakening CreatorDataGate's stale-write protection.
 */
class CreatorEditorDraftStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)

    fun saveProject(projectId: String, baseWorkspaceRevision: Long, draft: CreatorContentWorkspace) {
        put(
            key = projectKey(projectId),
            root = JSONObject()
                .put("version", VERSION)
                .put("projectId", projectId)
                .put("baseWorkspaceRevision", baseWorkspaceRevision)
                .put("payload", encodeWorkspace(draft)),
        )
    }

    fun loadProject(projectId: String, baseWorkspaceRevision: Long): CreatorContentWorkspace? =
        read(projectKey(projectId))?.takeIf {
            it.optString("projectId") == projectId &&
                it.optLong("baseWorkspaceRevision", -1L) == baseWorkspaceRevision
        }?.optJSONObject("payload")?.let { decodeWorkspace(projectId, it) }

    fun clearProject(projectId: String) = remove(projectKey(projectId))

    fun savePublish(projectId: String, baseWorkspaceRevision: Long, deliverables: List<CreatorDeliverable>) {
        put(
            key = publishKey(projectId),
            root = JSONObject()
                .put("version", VERSION)
                .put("projectId", projectId)
                .put("baseWorkspaceRevision", baseWorkspaceRevision)
                .put("deliverables", encodeDeliverables(deliverables)),
        )
    }

    fun loadPublish(projectId: String, baseWorkspaceRevision: Long): List<CreatorDeliverable>? =
        read(publishKey(projectId))?.takeIf {
            it.optString("projectId") == projectId &&
                it.optLong("baseWorkspaceRevision", -1L) == baseWorkspaceRevision
        }?.optJSONArray("deliverables")?.let(::decodeDeliverables)

    fun clearPublish(projectId: String) = remove(publishKey(projectId))

    fun saveScript(
        projectId: String,
        baseWorkspaceRevision: Long,
        baseStudioRevision: Long,
        draft: CreatorScriptStudio,
    ) {
        put(
            key = scriptKey(projectId),
            root = JSONObject()
                .put("version", VERSION)
                .put("projectId", projectId)
                .put("baseWorkspaceRevision", baseWorkspaceRevision)
                .put("baseStudioRevision", baseStudioRevision)
                .put("studio", encodeScript(draft)),
        )
    }

    fun loadScript(
        projectId: String,
        baseWorkspaceRevision: Long,
        baseStudioRevision: Long,
    ): CreatorScriptStudio? = read(scriptKey(projectId))?.takeIf {
        it.optString("projectId") == projectId &&
            it.optLong("baseWorkspaceRevision", -1L) == baseWorkspaceRevision &&
            it.optLong("baseStudioRevision", -1L) == baseStudioRevision
    }?.optJSONObject("studio")?.let { decodeScript(projectId, it) }

    fun clearScript(projectId: String) = remove(scriptKey(projectId))

    fun clearAllForProject(projectId: String) {
        val ok = prefs.edit()
            .remove(projectKey(projectId))
            .remove(scriptKey(projectId))
            .remove(publishKey(projectId))
            .commit()
        check(ok) { "Could not clear editor drafts" }
    }

    private fun put(key: String, root: JSONObject) {
        root.put("updatedAtMillis", System.currentTimeMillis())
        check(prefs.edit().putString(key, root.toString()).commit()) { "Could not preserve editor draft" }
    }

    private fun read(key: String): JSONObject? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()?.takeIf { it.optInt("version", -1) == VERSION }
    }

    private fun remove(key: String) {
        check(prefs.edit().remove(key).commit()) { "Could not clear editor draft" }
    }

    private fun projectKey(projectId: String) = "project:$projectId"
    private fun scriptKey(projectId: String) = "script:$projectId"
    private fun publishKey(projectId: String) = "publish:$projectId"

    private fun encodeWorkspace(workspace: CreatorContentWorkspace): JSONObject = JSONObject()
        .put("revision", workspace.revision)
        .put("audience", workspace.audience)
        .put("viewerProblem", workspace.viewerProblem)
        .put("promise", workspace.promise)
        .put("angle", workspace.angle)
        .put("hook", workspace.hook)
        .put("script", workspace.script)
        .put("references", JSONArray().apply {
            workspace.references.forEach { value ->
                put(JSONObject().put("id", value.id).put("label", value.label).put("url", value.url))
            }
        })
        .put("checklist", JSONArray().apply {
            workspace.checklist.forEach { value ->
                put(JSONObject().put("id", value.id).put("title", value.title).put("status", value.status.name))
            }
        })
        .put("assets", JSONArray().apply {
            workspace.assets.forEach { value ->
                put(JSONObject()
                    .put("id", value.id)
                    .put("label", value.label)
                    .put("location", value.location)
                    .put("kind", value.kind.name)
                    .put("notes", value.notes))
            }
        })
        .put("deliverables", encodeDeliverables(workspace.deliverables))
        .put("learnings", workspace.learnings)

    private fun decodeWorkspace(projectId: String, root: JSONObject): CreatorContentWorkspace = CreatorContentWorkspace(
        revision = root.optLong("revision", 0L).coerceAtLeast(0L),
        audience = root.optString("audience"),
        viewerProblem = root.optString("viewerProblem"),
        promise = root.optString("promise"),
        angle = root.optString("angle"),
        hook = root.optString("hook"),
        script = root.optString("script"),
        references = root.optJSONArray("references").objects().mapNotNull { item ->
            val id = item.optString("id").trim()
            if (id.isBlank()) null else CreatorProjectReference(id, item.optString("label"), item.optString("url"))
        },
        checklist = root.optJSONArray("checklist").objects().mapNotNull { item ->
            val id = item.optString("id").trim()
            val title = item.optString("title")
            if (id.isBlank()) null else CreatorChecklistItem(
                id = id,
                title = title,
                status = enumOr(item.optString("status"), CreatorChecklistStatus.TODO),
            )
        },
        assets = root.optJSONArray("assets").objects().mapNotNull { item ->
            val id = item.optString("id").trim()
            if (id.isBlank()) null else CreatorProjectAsset(
                id = id,
                label = item.optString("label"),
                location = item.optString("location"),
                kind = enumOr(item.optString("kind"), CreatorAssetKind.OTHER),
                notes = item.optString("notes"),
            )
        },
        deliverables = decodeDeliverables(root.optJSONArray("deliverables") ?: JSONArray()),
        learnings = root.optString("learnings"),
        scriptStudio = null,
    )

    private fun encodeDeliverables(values: List<CreatorDeliverable>): JSONArray = JSONArray().apply {
        values.forEach { value ->
            put(JSONObject()
                .put("id", value.id)
                .put("platform", value.platform)
                .put("format", value.format)
                .put("title", value.title)
                .put("status", value.status.name)
                .put("deadlineLabel", value.deadlineLabel)
                .put("description", value.description)
                .put("tags", value.tags)
                .put("thumbnailConcept", value.thumbnailConcept)
                .put("parentDeliverableId", value.parentDeliverableId)
                .put("publishedAtMillis", value.publishedAtMillis)
                .put("publishedUrl", value.publishedUrl)
                .put("titleVariants", encodeVariants(value.titleVariants))
                .put("thumbnailVariants", encodeVariants(value.thumbnailVariants))
                .put("publishGate", JSONArray().apply {
                    value.publishGate.forEach { gate ->
                        put(JSONObject()
                            .put("id", gate.id)
                            .put("title", gate.title)
                            .put("status", gate.status.name)
                            .put("required", gate.required))
                    }
                })
                .put("publicationHistory", JSONArray().apply {
                    value.publicationHistory.forEach { event ->
                        put(JSONObject()
                            .put("id", event.id)
                            .put("kind", event.kind.name)
                            .put("atMillis", event.atMillis)
                            .put("titleSnapshot", event.titleSnapshot)
                            .put("thumbnailSnapshot", event.thumbnailSnapshot)
                            .put("descriptionSnapshot", event.descriptionSnapshot)
                            .put("tagsSnapshot", event.tagsSnapshot)
                            .put("url", event.url)
                            .put("note", event.note))
                    }
                }))
        }
    }

    private fun decodeDeliverables(array: JSONArray): List<CreatorDeliverable> = array.objects().mapNotNull { item ->
        val id = item.optString("id").trim()
        val platform = item.optString("platform")
        val format = item.optString("format")
        if (id.isBlank() || platform.isBlank() || format.isBlank()) null else CreatorDeliverable(
            id = id,
            platform = platform,
            format = format,
            title = item.optString("title"),
            status = enumOr(item.optString("status"), CreatorDeliverableStatus.PLANNED),
            deadlineLabel = item.optString("deadlineLabel"),
            description = item.optString("description"),
            tags = item.optString("tags"),
            thumbnailConcept = item.optString("thumbnailConcept"),
            parentDeliverableId = item.optString("parentDeliverableId"),
            publishedAtMillis = item.optLong("publishedAtMillis", 0L),
            publishedUrl = item.optString("publishedUrl"),
            titleVariants = decodeVariants(item.optJSONArray("titleVariants")),
            thumbnailVariants = decodeVariants(item.optJSONArray("thumbnailVariants")),
            publishGate = item.optJSONArray("publishGate").objects().mapNotNull { gate ->
                val gateId = gate.optString("id").trim()
                if (gateId.isBlank()) null else CreatorPublishGateItem(
                    id = gateId,
                    title = gate.optString("title"),
                    status = enumOr(gate.optString("status"), CreatorPublishGateStatus.TODO),
                    required = gate.optBoolean("required", true),
                )
            },
            publicationHistory = item.optJSONArray("publicationHistory").objects().mapNotNull { event ->
                val eventId = event.optString("id").trim()
                if (eventId.isBlank()) null else CreatorPublicationEvent(
                    id = eventId,
                    kind = enumOr(event.optString("kind"), CreatorPublicationEventKind.PUBLISHED),
                    atMillis = event.optLong("atMillis", 0L),
                    titleSnapshot = event.optString("titleSnapshot"),
                    thumbnailSnapshot = event.optString("thumbnailSnapshot"),
                    descriptionSnapshot = event.optString("descriptionSnapshot"),
                    tagsSnapshot = event.optString("tagsSnapshot"),
                    url = event.optString("url"),
                    note = event.optString("note"),
                )
            },
        )
    }

    private fun encodeVariants(values: List<CreatorVariantIdea>): JSONArray = JSONArray().apply {
        values.forEach { put(JSONObject().put("id", it.id).put("text", it.text)) }
    }

    private fun decodeVariants(array: JSONArray?): List<CreatorVariantIdea> = array.objects().mapNotNull { item ->
        val id = item.optString("id").trim()
        if (id.isBlank()) null else CreatorVariantIdea(id, item.optString("text"))
    }

    private fun encodeScript(studio: CreatorScriptStudio): JSONObject = JSONObject()
        .put("revision", studio.revision)
        .put("status", studio.status.name)
        .put("hooks", JSONArray().apply {
            studio.hooks.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("selected", it.selected)) }
        })
        .put("titles", JSONArray().apply {
            studio.titles.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("selected", it.selected)) }
        })
        .put("beats", JSONArray().apply {
            studio.beats.forEach { beat ->
                put(JSONObject()
                    .put("id", beat.id)
                    .put("label", beat.label)
                    .put("purpose", beat.purpose)
                    .put("narration", beat.narration)
                    .put("visualNotes", beat.visualNotes)
                    .put("bRollNotes", beat.bRollNotes)
                    .put("onScreenText", beat.onScreenText)
                    .put("status", beat.status.name))
            }
        })
        .put("creatorNotes", studio.creatorNotes)

    private fun decodeScript(projectId: String, root: JSONObject): CreatorScriptStudio = CreatorScriptStudio(
        projectId = projectId,
        revision = root.optLong("revision", 0L).coerceAtLeast(0L),
        status = enumOr(root.optString("status"), CreatorScriptStatus.DRAFTING),
        hooks = root.optJSONArray("hooks").objects().mapNotNull { item ->
            val id = item.optString("id").trim()
            if (id.isBlank()) null else CreatorHookIdea(id, item.optString("text"), item.optBoolean("selected", false))
        },
        titles = root.optJSONArray("titles").objects().mapNotNull { item ->
            val id = item.optString("id").trim()
            if (id.isBlank()) null else CreatorTitleIdea(id, item.optString("text"), item.optBoolean("selected", false))
        },
        beats = root.optJSONArray("beats").objects().mapNotNull { item ->
            val id = item.optString("id").trim()
            if (id.isBlank()) null else CreatorScriptBeat(
                id = id,
                label = item.optString("label"),
                purpose = item.optString("purpose"),
                narration = item.optString("narration"),
                visualNotes = item.optString("visualNotes"),
                bRollNotes = item.optString("bRollNotes"),
                onScreenText = item.optString("onScreenText"),
                status = enumOr(item.optString("status"), CreatorScriptBeatStatus.DRAFT),
            )
        },
        creatorNotes = root.optString("creatorNotes"),
    )

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    companion object { private const val VERSION = 1 }
}
