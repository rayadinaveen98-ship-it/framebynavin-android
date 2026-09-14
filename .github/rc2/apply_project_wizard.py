from pathlib import Path

path = Path("app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt")
text = path.read_text()

anchor = """    val requiresAdvancedPermissions = effectiveReminderMode in setOf(ReminderMode.VOICE, ReminderMode.ALARM, ReminderMode.SMART)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {"""

replacement = """    val requiresAdvancedPermissions = effectiveReminderMode in setOf(ReminderMode.VOICE, ReminderMode.ALARM, ReminderMode.SMART)

    if (task == null) {
        V20NewProjectWizard(
            profile = defaults.creatorProfile,
            title = title,
            onTitleChange = { title = it },
            creatorModeId = creatorModeId,
            onCreatorModeChange = { selectedMode ->
                creatorModeId = selectedMode
                archetypeId = ContentArchetypeRegistry.suggestedForMode(selectedMode).firstOrNull()?.id ?: archetypeId
                if (productionStyles.isEmpty()) {
                    productionStyles = defaults.creatorProfile.productionStyles.ifEmpty {
                        ProductionStyleRegistry.orderedForMode(selectedMode).take(1).toSet()
                    }
                }
            },
            archetypeId = archetypeId,
            onArchetypeChange = { archetypeId = it },
            productionStyles = productionStyles,
            onProductionStylesChange = { productionStyles = it },
            platform = platform,
            onPlatformChange = { selectedPlatform ->
                platform = selectedPlatform
                contentType = CreatorPlatformRegistry.defaultFormat(selectedPlatform)
            },
            contentType = contentType,
            onContentTypeChange = { contentType = it },
            dueAt = dueAt,
            onPickDue = {
                pickDateTime(dueAt) { picked ->
                    dueAt = picked
                    if (customReminderAt >= dueAt) customReminderAt = (dueAt - 30 * 60_000L).coerceAtLeast(now + 60_000L)
                }
            },
            attentionPlan = attentionPlan,
            onAttentionPlanChange = { attentionPlan = it },
            onDismiss = onDismiss,
            onCreate = {
                onSave(
                    PProjectDraft(
                        title = title.trim(),
                        platform = platform,
                        contentType = contentType,
                        contentDna = CreatorContentDna(
                            creatorModeId = creatorModeId,
                            archetypeId = archetypeId,
                            productionStyles = productionStyles,
                            platform = platform,
                            deliveryFormat = contentType,
                            inferredFromLegacy = false,
                        ).normalized(),
                        dueAtMillis = dueAt,
                        attentionPlan = attentionPlan,
                        mode = ReminderMode.NONE,
                        reminderAtMillis = 0L,
                        deliveryPreference = ReminderDeliveryPreference.AUTO,
                        priority = priority,
                        notes = notes.trim(),
                        alarmSoundUri = soundUri,
                        voicePersona = voice,
                        voiceRepeatCount = repeatCount,
                        voiceRepeatIntervalSeconds = repeatGap,
                        alarmTimeoutSeconds = alarmTimeout,
                    )
                )
            },
        )
        return
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {"""

if anchor not in text:
    raise SystemExit("composer insertion anchor not found")

path.write_text(text.replace(anchor, replacement, 1))
