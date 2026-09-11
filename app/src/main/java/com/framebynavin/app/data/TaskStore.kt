package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.widget.CreatorWidgetUpdater
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.creatorDataStore by preferencesDataStore(name = "creator_v0")
private val creatorTaskMutationMutex = Mutex()

class TaskStore(private val context: Context) {
    private val tasksKey = stringPreferencesKey("tasks_json")
    private val tasksBackupKey = stringPreferencesKey("tasks_json_last_good")

    val tasksFlow: Flow<List<CreatorTask>> = context.creatorDataStore.data.map { prefs ->
        val raw = prefs[tasksKey] ?: return@map emptyList()
        try {
            decode(raw)
        } catch (cause: Exception) {
            throw IllegalStateException(
                "Creator data could not be read. The original and recovery copies have been retained. Open backup tools before making changes.",
                cause,
            )
        }
    }

    suspend fun load(): List<CreatorTask> = tasksFlow.first()

    suspend fun save(tasks: List<CreatorTask>) = CreatorDataGate.transaction {
        creatorTaskMutationMutex.withLock { saveUnlocked(tasks) }
    }

    suspend fun mutate(transform: (List<CreatorTask>) -> List<CreatorTask>): List<CreatorTask> = CreatorDataGate.transaction {
        creatorTaskMutationMutex.withLock {
            val latest = load()
            val updated = transform(latest)
            if (updated != latest) saveUnlocked(updated)
            updated
        }
    }

    suspend fun applyDelta(
        base: List<CreatorTask>,
        desired: List<CreatorTask>,
        expectedGeneration: Long,
    ): List<CreatorTask> = CreatorDataGate.transaction {
        creatorTaskMutationMutex.withLock {
            if (CreatorDataGate.generation(context) != expectedGeneration)
                throw CreatorWriteConflict("The app data changed during restore. Your older edit was not applied.")
            val updated = CreatorDeltaEngine.merge(base, desired, load()) { it.id }
            val keys = updated.map { it.scheduleOccurrenceKey }.filter { it.isNotBlank() }
            require(keys.size == keys.toSet().size) { "Duplicate weekly occurrence" }
            if (updated != load()) saveUnlocked(updated)
            updated
        }
    }

    private suspend fun saveUnlocked(tasks: List<CreatorTask>) {
        val encoded = encode(tasks)
        context.creatorDataStore.edit { prefs ->
            val current = prefs[tasksKey]
            if (current != null) {
                decode(current) // Never overwrite a damaged primary with an empty or stale snapshot.
                prefs[tasksBackupKey] = current
            } else if (prefs[tasksBackupKey] == null) {
                prefs[tasksBackupKey] = encoded
            }
            prefs[tasksKey] = encoded
        }
        CreatorWidgetUpdater.updateAll(context, tasks)
    }

    /**
     * Portable export also folds in the Alpha4/5 sidecar Script Studio when an older project has
     * not been reopened in Alpha6 yet. Deleted weekly tombstones are intentionally scrubbed and
     * never receive sidecar content during export.
     */
    suspend fun exportJson(): String {
        val legacyStore = ScriptStudioStore(context)
        val portable = load().map { task ->
            if (task.archivedAtMillis < 0L) return@map task.copy(workspace = CreatorContentWorkspace())
            if (task.workspace.scriptStudio != null) return@map task
            val legacy = legacyStore.load(task.id, task.workspace.hook, task.workspace.script)
                .normalized(projectId = task.id)
            if (legacy.isEmpty()) task
            else task.copy(workspace = task.workspace.copy(scriptStudio = legacy))
        }
        return encode(portable)
    }

    suspend fun importJson(raw: String): List<CreatorTask> {
        val decoded = decode(raw)
        save(decoded)
        return decoded
    }

    fun validateJson(raw: String): Int = decode(raw).size

    suspend fun updateTask(id: String, expectedGeneration: Long? = null, transform: (CreatorTask) -> CreatorTask): CreatorTask? = CreatorDataGate.transaction {
        creatorTaskMutationMutex.withLock {
            expectedGeneration?.let { CreatorDataGate.checkGeneration(context, it) }
            val current = load().toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index == -1) return@withLock null
            val updated = transform(current[index])
            if (updated != current[index]) {
                current[index] = updated
                saveUnlocked(current)
            }
            updated
        }
    }

    private fun encode(tasks: List<CreatorTask>): String {
        val array = JSONArray()
        tasks.forEach { task ->
            val portableWorkspace = if (task.archivedAtMillis < 0L) CreatorContentWorkspace() else task.workspace
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("platform", task.platform)
                    .put("contentType", task.contentType)
                    .put("dueLabel", task.dueLabel)
                    .put("dueAtMillis", task.dueAtMillis)
                    .put("status", task.status.name)
                    .put("progress", task.progress)
                    .put("workflowStageIndex", task.workflowStageIndex)
                    .put("reminderEnabled", task.reminderEnabled)
                    .put("reminderAtMillis", task.reminderAtMillis)
                    .put("priority", task.priority.name)
                    .put("notes", task.notes)
                    .put("alertType", task.alertType.name)
                    .put("alarmSoundUri", task.alarmSoundUri)
                    .put("voiceEnabled", task.voiceEnabled)
                    .put("smartEscalationEnabled", task.smartEscalationEnabled)
                    .put("snoozeCount", task.snoozeCount)
                    .put("workingUntilMillis", task.workingUntilMillis)
                    .put("reminderMode", task.reminderMode.name)
                    .put("deliveryPreference", task.deliveryPreference.name)
                    .put("voicePersona", task.voicePersona.name)
                    .put("voiceRepeatCount", task.voiceRepeatCount)
                    .put("voiceRepeatIntervalSeconds", task.voiceRepeatIntervalSeconds)
                    .put("alarmTimeoutSeconds", task.alarmTimeoutSeconds)
                    .put("scheduleSlotId", task.scheduleSlotId)
                    .put("scheduleOccurrenceKey", task.scheduleOccurrenceKey)
                    .put("autoStageReminder", task.autoStageReminder)
                    .put("origin", task.origin.name)
                    .put("sourceRefId", task.sourceRefId)
                    .put("attentionPlan", task.attentionPlan.name)
                    .put("pulseManagedReminder", task.pulseManagedReminder)
                    .put("checkpointStageId", task.checkpointStageId)
                    .put("checkpointAtMillis", task.checkpointAtMillis)
                    .put("completedAtMillis", task.completedAtMillis)
                    .put("archivedAtMillis", task.archivedAtMillis)
                    .put("publishedAtMillis", task.publishedAtMillis)
                    .put("publishedUrl", task.publishedUrl)
                    .put("publicationIsLegacy", task.publicationIsLegacy)
                    .put("acknowledgedCheckpointStageId", task.acknowledgedCheckpointStageId)
                    .put("acknowledgedCheckpointDueAtMillis", task.acknowledgedCheckpointDueAtMillis)
                    .put("contentDna", encodeContentDna(task.contentDna))
                    .put("workspace", encodeWorkspace(portableWorkspace))
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<CreatorTask> {
        val array = JSONArray(raw)
        val seenIds = mutableSetOf<String>()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.optString("id").trim()
                val title = item.optString("title").trim()
                require(id.isNotBlank()) { "Task $i has no id" }
                require(title.isNotBlank()) { "Task $i has no title" }
                require(seenIds.add(id)) { "Duplicate task id: $id" }

                val legacyAlertType = runCatching {
                    ReminderAlertType.valueOf(item.optString("alertType", ReminderAlertType.NOTIFICATION.name))
                }.getOrDefault(ReminderAlertType.NOTIFICATION)
                val legacyVoice = item.optBoolean("voiceEnabled", false)
                val legacySmart = item.optBoolean("smartEscalationEnabled", false)
                val reminderEnabled = item.optBoolean("reminderEnabled", false)
                val migratedMode = when {
                    !reminderEnabled -> ReminderMode.NONE
                    legacySmart -> ReminderMode.SMART
                    legacyAlertType == ReminderAlertType.ALARM -> ReminderMode.ALARM
                    legacyVoice -> ReminderMode.VOICE
                    else -> ReminderMode.SIMPLE
                }
                val reminderMode = runCatching {
                    ReminderMode.valueOf(item.optString("reminderMode", migratedMode.name))
                }.getOrDefault(migratedMode)
                val reminderAt = item.optLong("reminderAtMillis", 0L)
                val migratedAttentionPlan = if (reminderEnabled && reminderMode != ReminderMode.NONE)
                    ProjectAttentionPlan.CUSTOM else ProjectAttentionPlan.OFF
                val attentionPlan = runCatching {
                    ProjectAttentionPlan.valueOf(item.optString("attentionPlan", migratedAttentionPlan.name))
                }.getOrDefault(migratedAttentionPlan)
                val deliveryPreference = runCatching {
                    ReminderDeliveryPreference.valueOf(item.optString("deliveryPreference"))
                }.getOrElse {
                    // Old Custom reminders represented an explicit creator choice; managed presets were automatic.
                    if (attentionPlan == ProjectAttentionPlan.CUSTOM && reminderEnabled) {
                        when (reminderMode) {
                            ReminderMode.SIMPLE -> ReminderDeliveryPreference.NOTIFICATION
                            ReminderMode.VOICE -> ReminderDeliveryPreference.VOICE
                            ReminderMode.ALARM -> ReminderDeliveryPreference.ALARM
                            ReminderMode.SMART -> ReminderDeliveryPreference.SMART
                            ReminderMode.NONE -> ReminderDeliveryPreference.AUTO
                        }
                    } else ReminderDeliveryPreference.AUTO
                }
                val pulseManagedReminder = item.optBoolean("pulseManagedReminder", false) &&
                    attentionPlan != ProjectAttentionPlan.OFF
                val progress = item.optInt("progress", 0).coerceIn(0, 100)
                val platform = item.optString("platform", "Instagram")
                val contentType = item.optString("contentType", "Content")
                val storedStage = if (item.has("workflowStageIndex")) {
                    item.optInt("workflowStageIndex", -1)
                } else {
                    val template = CreatorWorkflowEngine.templateFor(platform, contentType)
                    CreatorWorkflowEngine.stageIndexFromProgress(progress, template.stages.size)
                }
                val scheduleSlotId = item.optString("scheduleSlotId", "")
                val migratedOrigin = if (scheduleSlotId.isNotBlank()) CreatorTaskOrigin.WEEKLY else CreatorTaskOrigin.MANUAL
                val origin = runCatching {
                    CreatorTaskOrigin.valueOf(item.optString("origin", migratedOrigin.name))
                }.getOrDefault(migratedOrigin)

                add(
                    CreatorTask(
                        id = id,
                        title = title,
                        platform = platform,
                        contentType = contentType,
                        dueLabel = item.optString("dueLabel", "Today"),
                        dueAtMillis = item.optLong("dueAtMillis", reminderAt),
                        status = runCatching {
                            TaskStatus.valueOf(item.optString("status", TaskStatus.PLANNED.name))
                        }.getOrDefault(TaskStatus.PLANNED),
                        progress = progress,
                        workflowStageIndex = storedStage,
                        reminderEnabled = reminderEnabled && reminderMode != ReminderMode.NONE,
                        reminderAtMillis = reminderAt,
                        priority = runCatching {
                            TaskPriority.valueOf(item.optString("priority", TaskPriority.IMPORTANT.name))
                        }.getOrDefault(TaskPriority.IMPORTANT),
                        notes = item.optString("notes", ""),
                        alertType = legacyAlertType,
                        alarmSoundUri = item.optString("alarmSoundUri", ""),
                        voiceEnabled = legacyVoice,
                        smartEscalationEnabled = legacySmart,
                        snoozeCount = item.optInt("snoozeCount", 0).coerceAtLeast(0),
                        workingUntilMillis = item.optLong("workingUntilMillis", 0L),
                        reminderMode = reminderMode,
                        deliveryPreference = deliveryPreference,
                        voicePersona = runCatching {
                            VoicePersona.valueOf(item.optString("voicePersona", VoicePersona.WARM.name))
                        }.getOrDefault(VoicePersona.WARM),
                        voiceRepeatCount = item.optInt("voiceRepeatCount", 3).coerceIn(1, 3),
                        voiceRepeatIntervalSeconds = item.optInt("voiceRepeatIntervalSeconds", 10).coerceIn(5, 60),
                        alarmTimeoutSeconds = item.optInt("alarmTimeoutSeconds", 120).coerceIn(30, 300),
                        scheduleSlotId = scheduleSlotId,
                        scheduleOccurrenceKey = item.optString("scheduleOccurrenceKey", ""),
                        autoStageReminder = item.optBoolean("autoStageReminder", false),
                        origin = origin,
                        sourceRefId = item.optString("sourceRefId", ""),
                        attentionPlan = attentionPlan,
                        pulseManagedReminder = pulseManagedReminder,
                        checkpointStageId = item.optString("checkpointStageId", ""),
                        checkpointAtMillis = item.optLong("checkpointAtMillis", 0L),
                        completedAtMillis = item.optLong("completedAtMillis", 0L),
                        archivedAtMillis = item.optLong("archivedAtMillis", 0L),
                        publishedAtMillis = item.optLong("publishedAtMillis", 0L),
                        publishedUrl = item.optString("publishedUrl", ""),
                        acknowledgedCheckpointStageId = item.optString("acknowledgedCheckpointStageId", ""),
                        acknowledgedCheckpointDueAtMillis = item.optLong("acknowledgedCheckpointDueAtMillis", 0L),
                        publicationIsLegacy = item.optBoolean("publicationIsLegacy", false) ||
                            (!item.has("publishedAtMillis") && item.optString("status") == TaskStatus.DONE.name),
                        contentDna = decodeContentDna(item.optJSONObject("contentDna")),
                        workspace = decodeWorkspace(id, item.optJSONObject("workspace")),
                    )
                )
            }
        }
    }

    private fun encodeContentDna(dna: CreatorContentDna): JSONObject {
        val normalized = dna.normalized()
        val styles = JSONArray()
        normalized.productionStyles.forEach(styles::put)
        return JSONObject()
            .put("creatorModeId", normalized.creatorModeId)
            .put("archetypeId", normalized.archetypeId)
            .put("productionStyles", styles)
            .put("platform", normalized.platform)
            .put("deliveryFormat", normalized.deliveryFormat)
            .put("legacyContentType", normalized.legacyContentType)
            .put("inferredFromLegacy", normalized.inferredFromLegacy)
    }

    private fun decodeContentDna(item: JSONObject?): CreatorContentDna {
        if (item == null) return CreatorContentDna()
        val styles = item.optJSONArray("productionStyles") ?: JSONArray()
        val decodedStyles = buildSet {
            for (i in 0 until styles.length()) {
                styles.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
        return CreatorContentDna(
            creatorModeId = item.optString("creatorModeId"),
            archetypeId = item.optString("archetypeId"),
            productionStyles = decodedStyles,
            platform = item.optString("platform"),
            deliveryFormat = item.optString("deliveryFormat"),
            legacyContentType = item.optString("legacyContentType"),
            inferredFromLegacy = item.optBoolean("inferredFromLegacy", false),
        ).normalized()
    }

    private fun encodeWorkspace(workspace: CreatorContentWorkspace): JSONObject {
        val references = JSONArray()
        workspace.references.forEach { reference ->
            references.put(JSONObject()
                .put("id", reference.id)
                .put("label", reference.label)
                .put("url", reference.url))
        }
        val checklist = JSONArray()
        workspace.checklist.forEach { item ->
            checklist.put(JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("status", item.status.name))
        }
        val assets = JSONArray()
        workspace.assets.forEach { asset ->
            assets.put(JSONObject()
                .put("id", asset.id)
                .put("label", asset.label)
                .put("location", asset.location)
                .put("kind", asset.kind.name)
                .put("notes", asset.notes))
        }
        val deliverables = JSONArray()
        workspace.deliverables.forEach { deliverable ->
            val titleVariants = JSONArray()
            deliverable.titleVariants.forEach { variant ->
                titleVariants.put(JSONObject().put("id", variant.id).put("text", variant.text))
            }
            val thumbnailVariants = JSONArray()
            deliverable.thumbnailVariants.forEach { variant ->
                thumbnailVariants.put(JSONObject().put("id", variant.id).put("text", variant.text))
            }
            val publishGate = JSONArray()
            deliverable.publishGate.forEach { gate ->
                publishGate.put(JSONObject()
                    .put("id", gate.id)
                    .put("title", gate.title)
                    .put("status", gate.status.name)
                    .put("required", gate.required))
            }
            val publicationHistory = JSONArray()
            deliverable.publicationHistory.forEach { event ->
                publicationHistory.put(JSONObject()
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
            deliverables.put(JSONObject()
                .put("id", deliverable.id)
                .put("platform", deliverable.platform)
                .put("format", deliverable.format)
                .put("title", deliverable.title)
                .put("status", deliverable.status.name)
                .put("deadlineLabel", deliverable.deadlineLabel)
                .put("description", deliverable.description)
                .put("tags", deliverable.tags)
                .put("thumbnailConcept", deliverable.thumbnailConcept)
                .put("parentDeliverableId", deliverable.parentDeliverableId)
                .put("publishedAtMillis", deliverable.publishedAtMillis)
                .put("publishedUrl", deliverable.publishedUrl)
                .put("titleVariants", titleVariants)
                .put("thumbnailVariants", thumbnailVariants)
                .put("publishGate", publishGate)
                .put("publicationHistory", publicationHistory))
        }
        return JSONObject()
            .put("revision", workspace.revision)
            .put("audience", workspace.audience)
            .put("viewerProblem", workspace.viewerProblem)
            .put("promise", workspace.promise)
            .put("angle", workspace.angle)
            .put("hook", workspace.hook)
            .put("script", workspace.script)
            .put("references", references)
            .put("checklist", checklist)
            .put("assets", assets)
            .put("deliverables", deliverables)
            .put("learnings", workspace.learnings)
            .put("scriptStudio", workspace.scriptStudio?.let(::encodeScriptStudio) ?: JSONObject.NULL)
    }

    private fun encodeScriptStudio(studio: CreatorScriptStudio): JSONObject {
        val normalized = studio.normalized()
        val hooks = JSONArray()
        normalized.hooks.forEach { hook ->
            hooks.put(JSONObject().put("id", hook.id).put("text", hook.text).put("selected", hook.selected))
        }
        val titles = JSONArray()
        normalized.titles.forEach { title ->
            titles.put(JSONObject().put("id", title.id).put("text", title.text).put("selected", title.selected))
        }
        val beats = JSONArray()
        normalized.beats.forEach { beat ->
            beats.put(
                JSONObject()
                    .put("id", beat.id)
                    .put("label", beat.label)
                    .put("purpose", beat.purpose)
                    .put("narration", beat.narration)
                    .put("visualNotes", beat.visualNotes)
                    .put("bRollNotes", beat.bRollNotes)
                    .put("onScreenText", beat.onScreenText)
                    .put("status", beat.status.name)
            )
        }
        return JSONObject()
            .put("revision", normalized.revision)
            .put("status", normalized.status.name)
            .put("hooks", hooks)
            .put("titles", titles)
            .put("beats", beats)
            .put("creatorNotes", normalized.creatorNotes)
    }

    private fun decodeScriptStudio(projectId: String, item: JSONObject?): CreatorScriptStudio? {
        if (item == null) return null
        val hooksArray = item.optJSONArray("hooks") ?: JSONArray()
        val hooks = buildList {
            for (i in 0 until hooksArray.length()) {
                val entry = hooksArray.optJSONObject(i) ?: continue
                val id = entry.optString("id").trim()
                val text = entry.optString("text").trim()
                if (id.isBlank() || text.isBlank()) continue
                add(CreatorHookIdea(id = id, text = text, selected = entry.optBoolean("selected", false)))
            }
        }
        val titlesArray = item.optJSONArray("titles") ?: JSONArray()
        val titles = buildList {
            for (i in 0 until titlesArray.length()) {
                val entry = titlesArray.optJSONObject(i) ?: continue
                val id = entry.optString("id").trim()
                val text = entry.optString("text").trim()
                if (id.isBlank() || text.isBlank()) continue
                add(CreatorTitleIdea(id = id, text = text, selected = entry.optBoolean("selected", false)))
            }
        }
        val beatsArray = item.optJSONArray("beats") ?: JSONArray()
        val beats = buildList {
            for (i in 0 until beatsArray.length()) {
                val entry = beatsArray.optJSONObject(i) ?: continue
                val id = entry.optString("id").trim()
                if (id.isBlank()) continue
                add(
                    CreatorScriptBeat(
                        id = id,
                        label = entry.optString("label"),
                        purpose = entry.optString("purpose"),
                        narration = entry.optString("narration"),
                        visualNotes = entry.optString("visualNotes"),
                        bRollNotes = entry.optString("bRollNotes"),
                        onScreenText = entry.optString("onScreenText"),
                        status = runCatching {
                            CreatorScriptBeatStatus.valueOf(entry.optString("status", CreatorScriptBeatStatus.DRAFT.name))
                        }.getOrDefault(CreatorScriptBeatStatus.DRAFT),
                    )
                )
            }
        }
        val studio = CreatorScriptStudio(
            projectId = projectId,
            revision = item.optLong("revision", 0L).coerceAtLeast(0L),
            status = runCatching {
                CreatorScriptStatus.valueOf(item.optString("status", CreatorScriptStatus.DRAFTING.name))
            }.getOrDefault(CreatorScriptStatus.DRAFTING),
            hooks = hooks,
            titles = titles,
            beats = beats,
            creatorNotes = item.optString("creatorNotes"),
        ).normalized(projectId = projectId)
        return studio.takeUnless { it.isEmpty() }
    }

    private fun decodeWorkspace(projectId: String, item: JSONObject?): CreatorContentWorkspace {
        if (item == null) return CreatorContentWorkspace()
        val references = item.optJSONArray("references") ?: JSONArray()
        val decodedReferences = buildList {
            for (i in 0 until references.length()) {
                val reference = references.optJSONObject(i) ?: continue
                val id = reference.optString("id").trim()
                val url = reference.optString("url").trim()
                if (id.isBlank() || url.isBlank()) continue
                add(CreatorProjectReference(id = id, label = reference.optString("label").trim(), url = url))
            }
        }
        val checklist = item.optJSONArray("checklist") ?: JSONArray()
        val decodedChecklist = buildList {
            for (i in 0 until checklist.length()) {
                val entry = checklist.optJSONObject(i) ?: continue
                val id = entry.optString("id").trim()
                val title = entry.optString("title").trim()
                if (id.isBlank() || title.isBlank()) continue
                add(CreatorChecklistItem(
                    id = id,
                    title = title,
                    status = runCatching {
                        CreatorChecklistStatus.valueOf(entry.optString("status", CreatorChecklistStatus.TODO.name))
                    }.getOrDefault(CreatorChecklistStatus.TODO),
                ))
            }
        }
        val assets = item.optJSONArray("assets") ?: JSONArray()
        val decodedAssets = buildList {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val id = asset.optString("id").trim()
                val location = asset.optString("location").trim()
                if (id.isBlank() || location.isBlank()) continue
                add(CreatorProjectAsset(
                    id = id,
                    label = asset.optString("label").trim(),
                    location = location,
                    kind = runCatching {
                        CreatorAssetKind.valueOf(asset.optString("kind", CreatorAssetKind.OTHER.name))
                    }.getOrDefault(CreatorAssetKind.OTHER),
                    notes = asset.optString("notes").trim(),
                ))
            }
        }
        val deliverables = item.optJSONArray("deliverables") ?: JSONArray()
        val decodedDeliverables = buildList {
            for (i in 0 until deliverables.length()) {
                val deliverable = deliverables.optJSONObject(i) ?: continue
                val id = deliverable.optString("id").trim()
                val platform = deliverable.optString("platform").trim()
                val format = deliverable.optString("format").trim()
                if (id.isBlank() || platform.isBlank() || format.isBlank()) continue

                fun variants(key: String): List<CreatorVariantIdea> {
                    val array = deliverable.optJSONArray(key) ?: JSONArray()
                    return buildList {
                        for (index in 0 until array.length()) {
                            val value = array.optJSONObject(index) ?: continue
                            val variantId = value.optString("id").trim()
                            val text = value.optString("text").trim()
                            if (variantId.isNotBlank() && text.isNotBlank()) add(CreatorVariantIdea(variantId, text))
                        }
                    }
                }

                val gateArray = deliverable.optJSONArray("publishGate") ?: JSONArray()
                val gate = buildList {
                    for (index in 0 until gateArray.length()) {
                        val value = gateArray.optJSONObject(index) ?: continue
                        val gateId = value.optString("id").trim()
                        val title = value.optString("title").trim()
                        if (gateId.isBlank() || title.isBlank()) continue
                        add(CreatorPublishGateItem(
                            id = gateId,
                            title = title,
                            status = runCatching {
                                CreatorPublishGateStatus.valueOf(value.optString("status", CreatorPublishGateStatus.TODO.name))
                            }.getOrDefault(CreatorPublishGateStatus.TODO),
                            required = value.optBoolean("required", true),
                        ))
                    }
                }

                val historyArray = deliverable.optJSONArray("publicationHistory") ?: JSONArray()
                val history = buildList {
                    for (index in 0 until historyArray.length()) {
                        val value = historyArray.optJSONObject(index) ?: continue
                        val eventId = value.optString("id").trim()
                        val atMillis = value.optLong("atMillis", 0L)
                        if (eventId.isBlank() || atMillis <= 0L) continue
                        add(CreatorPublicationEvent(
                            id = eventId,
                            kind = runCatching {
                                CreatorPublicationEventKind.valueOf(value.optString("kind", CreatorPublicationEventKind.PUBLISHED.name))
                            }.getOrDefault(CreatorPublicationEventKind.PUBLISHED),
                            atMillis = atMillis,
                            titleSnapshot = value.optString("titleSnapshot").trim(),
                            thumbnailSnapshot = value.optString("thumbnailSnapshot").trim(),
                            descriptionSnapshot = value.optString("descriptionSnapshot"),
                            tagsSnapshot = value.optString("tagsSnapshot").trim(),
                            url = value.optString("url").trim(),
                            note = value.optString("note").trim(),
                        ))
                    }
                }

                add(CreatorDeliverable(
                    id = id,
                    platform = platform,
                    format = format,
                    title = deliverable.optString("title").trim(),
                    status = runCatching {
                        CreatorDeliverableStatus.valueOf(deliverable.optString("status", CreatorDeliverableStatus.PLANNED.name))
                    }.getOrDefault(CreatorDeliverableStatus.PLANNED),
                    deadlineLabel = deliverable.optString("deadlineLabel").trim(),
                    description = deliverable.optString("description").trim(),
                    tags = deliverable.optString("tags").trim(),
                    thumbnailConcept = deliverable.optString("thumbnailConcept").trim(),
                    parentDeliverableId = deliverable.optString("parentDeliverableId").trim(),
                    publishedAtMillis = deliverable.optLong("publishedAtMillis", 0L),
                    publishedUrl = deliverable.optString("publishedUrl").trim(),
                    titleVariants = variants("titleVariants"),
                    thumbnailVariants = variants("thumbnailVariants"),
                    publishGate = gate,
                    publicationHistory = history,
                ))
            }
        }
        return CreatorContentWorkspace(
            revision = item.optLong("revision", 0L).coerceAtLeast(0L),
            audience = item.optString("audience").trim(),
            viewerProblem = item.optString("viewerProblem").trim(),
            promise = item.optString("promise").trim(),
            angle = item.optString("angle").trim(),
            hook = item.optString("hook").trim(),
            script = item.optString("script"),
            references = decodedReferences,
            checklist = decodedChecklist,
            assets = decodedAssets,
            deliverables = decodedDeliverables,
            learnings = item.optString("learnings"),
            scriptStudio = decodeScriptStudio(projectId, item.optJSONObject("scriptStudio")),
        )
    }
}
