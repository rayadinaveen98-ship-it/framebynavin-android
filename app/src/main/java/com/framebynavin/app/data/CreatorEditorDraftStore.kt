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
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("creator_editor_drafts_v19", Context.MODE_PRIVATE)
    /** Bound to the editor instance, not recomputed after a backup restore. */
    val openedGeneration: Long = CreatorDataGate.generation(appContext)

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
        // Never erase a draft written by a newer editor after restore.
        listOf(projectKey(projectId), scriptKey(projectId), publishKey(projectId)).forEach(::remove)
    }

    /** True when a pre-restore or legacy draft is preserved but not auto-applied. */
    fun hasHeldDraft(projectId: String): Boolean =
        listOf(projectKey(projectId), scriptKey(projectId), publishKey(projectId)).any { key ->
            val record = prefs.getString(key, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
            (record != null && !matchesGeneration(record)) ||
                prefs.all.keys.any { it.startsWith("held:$key:") }
        }

    /**
     * C3 P6: a lossless *user-owned* copy of one earlier draft. The JSON is
     * intentionally raw: text-only export cannot preserve old publication
     * metadata, output relationships, statuses or other structured fields.
     * Never display this JSON or use it to overwrite current saved data.
     */
    data class HeldRawSnapshot(
        val sourceId: String,
        val editor: String,
        val rawJson: String,
        val sha256: String,
    )

    private fun historicalRawLocked(
        projectId: String,
        editor: String,
        sourceId: String,
    ): String? {
        val primary = when (editor) {
            "project" -> projectKey(projectId)
            "script" -> scriptKey(projectId)
            "publish" -> publishKey(projectId)
            else -> return null
        }
        if (projectId.isBlank() || (sourceId != primary && !sourceId.startsWith("held:$primary:"))) {
            return null
        }
        val raw = prefs.getString(sourceId, null) ?: return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (root.optInt("version", -1) != VERSION || root.optString("projectId") != projectId) {
            return null
        }
        // Primary current-generation editor content is never eligible.
        if (sourceId == primary && matchesGeneration(root)) return null
        return raw
    }

    private fun sha256(raw: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    fun heldRawSnapshotForExport(
        projectId: String,
        editor: String,
        sourceId: String,
    ): HeldRawSnapshot? = CreatorDataGate.withGeneration(appContext, openedGeneration) {
        val raw = historicalRawLocked(projectId, editor, sourceId) ?: return@withGeneration null
        HeldRawSnapshot(sourceId, editor, raw, sha256(raw))
    }

    /**
     * Destructive and **never** called on text-export completion.
     * The caller needs an explicit second confirmation plus the SHA-256 of a
     * successfully written original JSON copy obtained during this session.
     * Validate & remove atomically within the restore-generation lock.
     * No current editor draft, alternate project or rewritten source can pass.
     */
    fun discardHeldAfterOriginalExport(
        projectId: String,
        editor: String,
        sourceId: String,
        exportedSha256: String,
    ): Boolean = CreatorDataGate.withGeneration(appContext, openedGeneration) {
        if (!Regex("[0-9a-f]{64}").matches(exportedSha256)) return@withGeneration false
        val raw = historicalRawLocked(projectId, editor, sourceId) ?: return@withGeneration false
        if (sha256(raw) != exportedSha256) return@withGeneration false
        check(prefs.edit().remove(sourceId).commit()) {
            "Could not remove selected earlier draft"
        }
        true
    }

    /** Copy-only presentation of a held journal. Never contains an apply action. */
    data class HeldDraftField(
        val label: String,
        val value: String,
        // Identity metadata is never rendered or exported. It only constrains
        // targeted Publish recovery; output list index is never a destination.
        val deliverableId: String? = null,
        val deliverablePlatform: String? = null,
        val deliverableFormat: String? = null,
    )
    data class HeldDraftPreview(
        val sourceId: String,
        val editor: String,
        val savedAtMillis: Long,
        val restoreGeneration: Long?,
        val baseWorkspaceRevision: Long,
        val fields: List<HeldDraftField>,
    )

    /**
     * List ONLY pre-restore/legacy copies belonging to this project and editor.
     * A current-generation draft is not a historical recovery item.
     * Read under the restore-epoch lock: an old editor cannot review data
     * against an epoch which has since been invalidated.
     */
    fun heldDrafts(projectId: String, editor: String): List<HeldDraftPreview> {
        require(editor == "project" || editor == "script" || editor == "publish")
        val key = when (editor) {
            "project" -> projectKey(projectId)
            "script" -> scriptKey(projectId)
            else -> publishKey(projectId)
        }
        return CreatorDataGate.withGeneration(appContext, openedGeneration) {
            prefs.all.entries.asSequence()
                .filter { (name, _) -> name == key || name.startsWith("held:$key:") }
                .mapNotNull { (name, raw) ->
                    val root = (raw as? String)?.let { runCatching { JSONObject(it) }.getOrNull() }
                        ?: return@mapNotNull null
                    // Reject malformed or unrelated copied journals. Never expose
                    // one project's notes inside a different project.
                    if (root.optInt("version", -1) != VERSION ||
                        root.optString("projectId") != projectId) return@mapNotNull null
                    if (name == key && matchesGeneration(root)) return@mapNotNull null
                    val fields = when (editor) {
                        "project" -> previewProjectFields(root.optJSONObject("payload"))
                        "script" -> previewScriptFields(root.optJSONObject("studio"))
                        else -> previewPublishFields(root.optJSONArray("deliverables"))
                    }
                    HeldDraftPreview(
                        sourceId = name,
                        editor = editor,
                        savedAtMillis = root.optLong("updatedAtMillis", 0L),
                        restoreGeneration = if (root.has("restoreGeneration"))
                            root.optLong("restoreGeneration", -1L) else null,
                        baseWorkspaceRevision = root.optLong("baseWorkspaceRevision", -1L),
                        fields = fields,
                    )
                }
                .sortedWith(compareByDescending<HeldDraftPreview> { it.savedAtMillis }.thenBy { it.sourceId })
                .toList()
        }
    }

    /**
     * Confirm this exact earlier record still supplies the selected Plan text.
     * Source, project, label, value and restore epoch must all match.
     */
    fun verifiedHistoricalPlanText(
        projectId: String,
        sourceId: String,
        label: String,
        expectedText: String,
    ): Boolean {
        if (label !in setOf("Audience", "Viewer problem", "Promise", "Angle", "Learnings")) {
            return false
        }
        return heldDrafts(projectId, "project")
            .firstOrNull { it.sourceId == sourceId }
            ?.fields?.any { it.label == label && it.value == expectedText } == true
    }

    /**
     * C3 P4: a selected earlier Script Studio note must still belong to this
     * project and exact held source at the current restore generation.
     * Structured beats, status, publishing and selected titles are excluded.
     */
    fun verifiedHistoricalScriptNotes(
        projectId: String,
        sourceId: String,
        label: String,
        expectedText: String,
    ): Boolean {
        if (label != "Creator notes") return false
        return heldDrafts(projectId, "script")
            .firstOrNull { it.sourceId == sourceId }
            ?.fields?.any { it.label == label && it.value == expectedText } == true
    }

    /**
     * Verify the unique historical output, including its original platform,
     * format and full Description text. Reordered output labels have NO role
     * in this authorization. Duplicate IDs in an older journal fail closed.
     */
    fun verifiedHistoricalPublishDescription(
        projectId: String,
        sourceId: String,
        deliverableId: String,
        platform: String,
        format: String,
        expectedText: String,
    ): Boolean {
        if (deliverableId.isBlank() || platform.isBlank() || format.isBlank() || expectedText.isBlank()) {
            return false
        }
        val descriptions = heldDrafts(projectId, "publish")
            .firstOrNull { it.sourceId == sourceId }?.fields
            ?.filter { it.deliverableId == deliverableId && it.label.endsWith(" · Description") }
            ?: return false
        return descriptions.size == 1 &&
            descriptions.single().deliverablePlatform == platform &&
            descriptions.single().deliverableFormat == format &&
            descriptions.single().value == expectedText
    }

    fun hasHeldDraftForEditor(projectId: String, editor: String): Boolean {
        require(editor == "project" || editor == "script" || editor == "publish")
        val key = "$editor:$projectId"
        return prefs.all.any { (name, raw) ->
            if (name != key && !name.startsWith("held:$key:")) return@any false
            val root = (raw as? String)?.let { runCatching { JSONObject(it) }.getOrNull() }
            // A malformed record also deserves a recovery warning; its raw
            // payload stays untouched, but is never exposed in the UI.
            root == null || name != key || !matchesGeneration(root)
        }
    }

    private fun previewProjectFields(root: JSONObject?): List<HeldDraftField> {
        if (root == null) return emptyList()
        val result = mutableListOf<HeldDraftField>()
        fun field(label: String, value: String) {
            if (value.isNotBlank()) result += HeldDraftField(label, value)
        }
        listOf(
            "Audience" to "audience", "Viewer problem" to "viewerProblem",
            "Promise" to "promise", "Angle" to "angle", "Hook" to "hook",
            "Script" to "script", "Learnings" to "learnings",
        ).forEach { (title, key) -> field(title, root.optString(key)) }
        root.optJSONArray("references").objects().forEachIndexed { index, record ->
            field("Source ${index + 1} label", record.optString("label"))
            field("Source ${index + 1} URL", record.optString("url"))
        }
        root.optJSONArray("checklist").objects().forEachIndexed { index, record ->
            field("Task ${index + 1}", record.optString("title"))
        }
        root.optJSONArray("assets").objects().forEachIndexed { index, record ->
            field("Asset ${index + 1} label", record.optString("label"))
            field("Asset ${index + 1} notes", record.optString("notes"))
        }
        result += previewPublishFields(root.optJSONArray("deliverables"))
        return result
    }

    private fun previewScriptFields(root: JSONObject?): List<HeldDraftField> {
        if (root == null) return emptyList()
        val result = mutableListOf<HeldDraftField>()
        fun field(label: String, value: String) {
            if (value.isNotBlank()) result += HeldDraftField(label, value)
        }
        field("Creator notes", root.optString("creatorNotes"))
        root.optJSONArray("hooks").objects().forEachIndexed { i, obj ->
            field("Hook idea ${i + 1}", obj.optString("text"))
        }
        root.optJSONArray("titles").objects().forEachIndexed { i, obj ->
            field("Title idea ${i + 1}", obj.optString("text"))
        }
        root.optJSONArray("beats").objects().forEachIndexed { i, beat ->
            listOf(
                "label" to "Label", "purpose" to "Purpose",
                "narration" to "Narration", "visualNotes" to "Visual notes",
                "bRollNotes" to "B-roll", "onScreenText" to "On-screen text",
            ).forEach { (key, label) ->
                field("Beat ${i + 1} · $label", beat.optString(key))
            }
        }
        return result
    }

    private fun previewPublishFields(array: JSONArray?): List<HeldDraftField> {
        val result = mutableListOf<HeldDraftField>()
        array.objects().forEachIndexed { i, item ->
            val prefix = "Output ${i + 1}"
            val outputId = item.optString("id").takeIf { it.isNotBlank() }
            val platform = item.optString("platform").takeIf { it.isNotBlank() }
            val format = item.optString("format").takeIf { it.isNotBlank() }
            fun historicalField(label: String, value: String): HeldDraftField = HeldDraftField(
                label = label, value = value, deliverableId = outputId,
                deliverablePlatform = platform, deliverableFormat = format,
            )
            listOf(
                "title" to "Title", "description" to "Description",
                "tags" to "Tags", "thumbnailConcept" to "Cover idea",
            ).forEach { (key, label) ->
                val value = item.optString(key)
                if (value.isNotBlank()) result += historicalField("$prefix · $label", value)
            }
            item.optJSONArray("titleVariants").objects().forEachIndexed { j, variant ->
                val value = variant.optString("text")
                if (value.isNotBlank()) result += historicalField("$prefix · Title option ${j + 1}", value)
            }
            item.optJSONArray("thumbnailVariants").objects().forEachIndexed { j, variant ->
                val value = variant.optString("text")
                if (value.isNotBlank()) result += historicalField("$prefix · Cover option ${j + 1}", value)
            }
        }
        return result
    }

    private fun matchesGeneration(record: JSONObject): Boolean =
        if (record.has("restoreGeneration")) {
            record.optLong("restoreGeneration", -1L) == openedGeneration
        } else {
            // An untagged draft could precede ANY previous restore. Only
            // auto-recover on untouched epoch zero; retain others for review.
            openedGeneration == 0L
        }

    private fun put(key: String, root: JSONObject) {
        root.put("updatedAtMillis", System.currentTimeMillis())
            .put("restoreGeneration", openedGeneration)
        CreatorDataGate.withGeneration(appContext, openedGeneration) {
            val existing = prefs.getString(key, null)
            val previous = existing?.let { runCatching { JSONObject(it) }.getOrNull() }
            val editor = prefs.edit()
            if (existing != null && (previous == null || !matchesGeneration(previous))) {
                // Keep the original content for explicit recovery. Never silently
                // overwrite a legacy/older-generation journal with a new draft.
                editor.putString("held:$key:${System.nanoTime()}", existing)
            }
            check(editor.putString(key, root.toString()).commit()) {
                "Could not preserve editor draft"
            }
        }
    }

    private fun read(key: String): JSONObject? = runCatching {
        CreatorDataGate.withGeneration(appContext, openedGeneration) {
            val raw = prefs.getString(key, null) ?: return@withGeneration null
            val root = runCatching { JSONObject(raw) }.getOrNull() ?: return@withGeneration null
            root.takeIf { it.optInt("version", -1) == VERSION && matchesGeneration(it) }
        }
    }.getOrNull()

    private fun remove(key: String) {
        try {
            CreatorDataGate.withGeneration(appContext, openedGeneration) {
                val raw = prefs.getString(key, null) ?: return@withGeneration
                val record = runCatching { JSONObject(raw) }.getOrNull()
                // Preserve incompatible or undecodable journal payloads.
                if (record == null || !matchesGeneration(record)) return@withGeneration
                check(prefs.edit().remove(key).commit()) { "Could not clear editor draft" }
            }
        } catch (_: CreatorWriteConflict) {
            // A stale editor must never delete a post-restore replacement.
        }
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
