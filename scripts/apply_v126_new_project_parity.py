from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

wizard_src = ROOT / ".github" / "v126" / "V20NewProjectWizard.kt.txt"
wizard_dst = ROOT / "app" / "src" / "main" / "java" / "com" / "framebynavin" / "app" / "ui" / "V20NewProjectWizard.kt"
wizard_dst.write_text(wizard_src.read_text())

reminder_path = ROOT / "app" / "src" / "main" / "java" / "com" / "framebynavin" / "app" / "ui" / "V101BReminderUi.kt"
reminder = reminder_path.read_text()
start_marker = "    if (task == null) {\n"
end_marker = "    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {\n"
start = reminder.find(start_marker)
if start < 0:
    raise SystemExit("new-project composer start marker not found")
end = reminder.find(end_marker, start)
if end < 0:
    raise SystemExit("new-project composer end marker not found")
replacement = '''    if (task == null) {
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
            deliveryPreference = deliveryPreference,
            onDeliveryPreferenceChange = { deliveryPreference = it },
            customReminderAt = customReminderAt,
            onPickCustomReminder = {
                pickDateTime(customReminderAt) { picked -> customReminderAt = picked }
            },
            priority = priority,
            onPriorityChange = { priority = it },
            notes = notes,
            onNotesChange = { notes = it },
            reminderSetupReady = reminderSetupReady,
            requiresAdvancedPermissions = requiresAdvancedPermissions,
            onOpenSettings = onOpenSettings,
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
                        mode = if (attentionPlan == ProjectAttentionPlan.CUSTOM) pDeliveryMode(deliveryPreference) else ReminderMode.NONE,
                        reminderAtMillis = if (attentionPlan == ProjectAttentionPlan.CUSTOM) customReminderAt else 0L,
                        deliveryPreference = deliveryPreference,
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

'''
reminder = reminder[:start] + replacement + reminder[end:]
reminder_path.write_text(reminder)

vm_path = ROOT / "app" / "src" / "main" / "java" / "com" / "framebynavin" / "app" / "data" / "CreatorViewModel.kt"
vm = vm_path.read_text()
create_start = vm.find("        if (id == null) {\n")
create_end = vm.find("            val task = ProjectPulseEngine.applyAttentionPlan(baseTask, attentionPlan)\n", create_start)
if create_start < 0 or create_end < 0:
    raise SystemExit("CreatorViewModel create block not found")
create_block = vm[create_start:create_end]
needle = "                reminderMode = reminderMode,\n                voicePersona = voicePersona,\n"
if needle not in create_block:
    if "                deliveryPreference = deliveryPreference,\n                voicePersona = voicePersona,\n" not in create_block:
        raise SystemExit("new-task delivery preference insertion anchor not found")
else:
    create_block = create_block.replace(
        needle,
        "                reminderMode = reminderMode,\n                deliveryPreference = deliveryPreference,\n                voicePersona = voicePersona,\n",
        1,
    )
    vm = vm[:create_start] + create_block + vm[create_end:]
    vm_path.write_text(vm)

build_path = ROOT / "app" / "build.gradle.kts"
build = build_path.read_text()
build = build.replace('versionCode = 125', 'versionCode = 126', 1)
build = build.replace('versionName = "2.0.0-rc2-progressive-project-tour-fix"', 'versionName = "2.0.0-rc2-new-project-feature-parity"', 1)
if 'versionCode = 126' not in build or '2.0.0-rc2-new-project-feature-parity' not in build:
    raise SystemExit("version bump failed")
build_path.write_text(build)

test_path = ROOT / "app" / "src" / "test" / "java" / "com" / "framebynavin" / "app" / "ui" / "NewProjectParityContractTest.kt"
test_path.parent.mkdir(parents=True, exist_ok=True)
test_path.write_text("""package com.framebynavin.app.ui

import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewProjectParityContractTest {
    @Test
    fun projectSupportKeepsAllFivePlans() {
        assertEquals(
            listOf(
                ProjectAttentionPlan.OFF,
                ProjectAttentionPlan.LIGHT,
                ProjectAttentionPlan.GUIDED,
                ProjectAttentionPlan.URGENT,
                ProjectAttentionPlan.CUSTOM,
            ),
            V20NewProjectSupportPlans,
        )
    }

    @Test
    fun reminderStyleKeepsEveryDeliveryChoice() {
        assertEquals(ReminderDeliveryPreference.entries, V20NewProjectReminderStyles)
    }

    @Test
    fun onlyCustomRequiresAValidManualReminderTime() {
        val now = 1_000L
        val due = 10_000L
        assertTrue(v20NewProjectSupportReady(ProjectAttentionPlan.GUIDED, 0L, due, now))
        assertFalse(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, 0L, due, now))
        assertFalse(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, 11_000L, due, now))
        assertTrue(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, 5_000L, due, now))
    }
}
""")
