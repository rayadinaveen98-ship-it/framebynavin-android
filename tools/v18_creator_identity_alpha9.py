from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
SETTINGS = ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorOsSettings.kt'
PROFILE = ROOT / 'app/src/main/java/com/framebynavin/app/data/CreatorProfile.kt'
ROOT_UI = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
TODAY = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt'
ONBOARDING = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V18CreatorOnboarding.kt'
TEST = ROOT / 'app/src/test/java/com/framebynavin/app/data/CreatorProfileAlpha9Test.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 48', 'versionCode = 49', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-foundation-alpha8"',
    'versionName = "1.8.0-product-alpha9"',
    'versionName',
)
BUILD.write_text(build)

PROFILE.write_text(r'''package com.framebynavin.app.data

/**
 * Small creator identity model used to personalize the local Creator OS without assuming a niche.
 * It deliberately stores only creator-chosen product preferences; account identity stays separate.
 */
data class CreatorProfile(
    val displayName: String = "",
    val category: String = "",
    val platforms: Set<String> = emptySet(),
    val primaryGoal: String = "",
    val weeklyPublishingTarget: Int = 2,
) {
    val isComplete: Boolean
        get() = displayName.isNotBlank() &&
            category.isNotBlank() &&
            platforms.isNotEmpty() &&
            primaryGoal.isNotBlank()

    val safeDisplayName: String
        get() = displayName.trim().ifBlank { "Creator" }

    fun normalized(): CreatorProfile = copy(
        displayName = displayName.trim().take(40),
        category = category.trim().take(60),
        platforms = platforms.map { it.trim() }.filter { it.isNotBlank() }.toSet(),
        primaryGoal = primaryGoal.trim().take(80),
        weeklyPublishingTarget = weeklyPublishingTarget.coerceIn(1, 14),
    )
}
''')

SETTINGS.write_text(r'''package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CreatorOsSettings(
    val onboardingComplete: Boolean = false,
    val creatorProfile: CreatorProfile = CreatorProfile(),
    val defaultVoicePersona: VoicePersona = VoicePersona.WARM,
    val defaultAlarmTimeoutSeconds: Int = 120,
    val snoozeMinutes: Int = 10,
    val weeklyAutoPlanEnabled: Boolean = false,
    val contextNudgesEnabled: Boolean = false,
)

class CreatorOsSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun snapshot(): CreatorOsSettings = CreatorOsSettings(
        onboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
        creatorProfile = CreatorProfile(
            displayName = prefs.getString(KEY_CREATOR_NAME, "") ?: "",
            category = prefs.getString(KEY_CREATOR_CATEGORY, "") ?: "",
            platforms = (prefs.getStringSet(KEY_CREATOR_PLATFORMS, emptySet()) ?: emptySet()).toSet(),
            primaryGoal = prefs.getString(KEY_CREATOR_GOAL, "") ?: "",
            weeklyPublishingTarget = prefs.getInt(KEY_WEEKLY_PUBLISHING_TARGET, 2).coerceIn(1, 14),
        ).normalized(),
        defaultVoicePersona = runCatching {
            VoicePersona.valueOf(prefs.getString(KEY_DEFAULT_VOICE, VoicePersona.WARM.name) ?: VoicePersona.WARM.name)
        }.getOrDefault(VoicePersona.WARM),
        defaultAlarmTimeoutSeconds = prefs.getInt(KEY_ALARM_TIMEOUT, 120).coerceIn(30, 300),
        snoozeMinutes = prefs.getInt(KEY_SNOOZE_MINUTES, 10).coerceIn(5, 30),
        weeklyAutoPlanEnabled = prefs.getBoolean(KEY_WEEKLY_AUTO_PLAN, false),
        contextNudgesEnabled = prefs.getBoolean(KEY_CONTEXT_NUDGES, false),
    )

    fun exportJson(): String {
        val value = snapshot()
        val profile = value.creatorProfile
        return JSONObject()
            .put("onboardingComplete", value.onboardingComplete)
            .put(
                "creatorProfile",
                JSONObject()
                    .put("displayName", profile.displayName)
                    .put("category", profile.category)
                    .put("platforms", JSONArray(profile.platforms.sorted()))
                    .put("primaryGoal", profile.primaryGoal)
                    .put("weeklyPublishingTarget", profile.weeklyPublishingTarget),
            )
            .put("defaultVoicePersona", value.defaultVoicePersona.name)
            .put("defaultAlarmTimeoutSeconds", value.defaultAlarmTimeoutSeconds)
            .put("snoozeMinutes", value.snoozeMinutes)
            .put("weeklyAutoPlanEnabled", value.weeklyAutoPlanEnabled)
            .put("contextNudgesEnabled", value.contextNudgesEnabled)
            .toString()
    }

    fun importJson(raw: String): CreatorOsSettings {
        val obj = JSONObject(raw)
        val profileObj = obj.optJSONObject("creatorProfile")
        val importedPlatforms = buildSet {
            val array = profileObj?.optJSONArray("platforms")
            if (array != null) {
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        val profile = CreatorProfile(
            displayName = profileObj?.optString("displayName", "").orEmpty(),
            category = profileObj?.optString("category", "").orEmpty(),
            platforms = importedPlatforms,
            primaryGoal = profileObj?.optString("primaryGoal", "").orEmpty(),
            weeklyPublishingTarget = profileObj?.optInt("weeklyPublishingTarget", 2) ?: 2,
        ).normalized()
        val value = CreatorOsSettings(
            onboardingComplete = obj.optBoolean("onboardingComplete", true),
            creatorProfile = profile,
            defaultVoicePersona = runCatching {
                VoicePersona.valueOf(obj.optString("defaultVoicePersona", VoicePersona.WARM.name))
            }.getOrDefault(VoicePersona.WARM),
            defaultAlarmTimeoutSeconds = obj.optInt("defaultAlarmTimeoutSeconds", 120).coerceIn(30, 300),
            snoozeMinutes = obj.optInt("snoozeMinutes", 10).coerceIn(5, 30),
            weeklyAutoPlanEnabled = obj.optBoolean("weeklyAutoPlanEnabled", false),
            contextNudgesEnabled = obj.optBoolean("contextNudgesEnabled", false),
        )
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, value.onboardingComplete)
            .putString(KEY_CREATOR_NAME, profile.displayName)
            .putString(KEY_CREATOR_CATEGORY, profile.category)
            .putStringSet(KEY_CREATOR_PLATFORMS, profile.platforms)
            .putString(KEY_CREATOR_GOAL, profile.primaryGoal)
            .putInt(KEY_WEEKLY_PUBLISHING_TARGET, profile.weeklyPublishingTarget)
            .putString(KEY_DEFAULT_VOICE, value.defaultVoicePersona.name)
            .putInt(KEY_ALARM_TIMEOUT, value.defaultAlarmTimeoutSeconds)
            .putInt(KEY_SNOOZE_MINUTES, value.snoozeMinutes)
            .putBoolean(KEY_WEEKLY_AUTO_PLAN, value.weeklyAutoPlanEnabled)
            .putBoolean(KEY_CONTEXT_NUDGES, value.contextNudgesEnabled)
            .commit()
        return value
    }

    fun validateJson(raw: String) {
        val obj = JSONObject(raw)
        if (obj.has("defaultVoicePersona")) {
            runCatching { VoicePersona.valueOf(obj.getString("defaultVoicePersona")) }.getOrElse {
                throw IllegalArgumentException("Unsupported voice setting")
            }
        }
        obj.optJSONObject("creatorProfile")?.let { profile ->
            val target = profile.optInt("weeklyPublishingTarget", 2)
            require(target in 1..14) { "Unsupported weekly publishing target" }
        }
    }

    fun setOnboardingComplete(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()
    }

    fun setCreatorProfile(value: CreatorProfile) {
        val profile = value.normalized()
        prefs.edit()
            .putString(KEY_CREATOR_NAME, profile.displayName)
            .putString(KEY_CREATOR_CATEGORY, profile.category)
            .putStringSet(KEY_CREATOR_PLATFORMS, profile.platforms)
            .putString(KEY_CREATOR_GOAL, profile.primaryGoal)
            .putInt(KEY_WEEKLY_PUBLISHING_TARGET, profile.weeklyPublishingTarget)
            .apply()
    }

    fun setDefaultVoicePersona(value: VoicePersona) {
        prefs.edit().putString(KEY_DEFAULT_VOICE, value.name).apply()
    }

    fun setDefaultAlarmTimeoutSeconds(value: Int) {
        prefs.edit().putInt(KEY_ALARM_TIMEOUT, value.coerceIn(30, 300)).apply()
    }

    fun setSnoozeMinutes(value: Int) {
        prefs.edit().putInt(KEY_SNOOZE_MINUTES, value.coerceIn(5, 30)).apply()
    }

    fun setWeeklyAutoPlanEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY_AUTO_PLAN, value).apply()
    }

    fun setContextNudgesEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONTEXT_NUDGES, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "creator_os_settings_v1"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_CREATOR_NAME = "creator_display_name"
        private const val KEY_CREATOR_CATEGORY = "creator_category"
        private const val KEY_CREATOR_PLATFORMS = "creator_platforms"
        private const val KEY_CREATOR_GOAL = "creator_primary_goal"
        private const val KEY_WEEKLY_PUBLISHING_TARGET = "creator_weekly_publishing_target"
        private const val KEY_DEFAULT_VOICE = "default_voice_persona"
        private const val KEY_ALARM_TIMEOUT = "default_alarm_timeout_seconds"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_WEEKLY_AUTO_PLAN = "weekly_auto_plan_enabled"
        private const val KEY_CONTEXT_NUDGES = "context_nudges_enabled"
    }
}
''')

ONBOARDING.write_text(r'''package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.ui.theme.*

private val creatorCategories = listOf(
    "Film & Entertainment",
    "Gaming",
    "Education",
    "Tech",
    "Lifestyle",
    "Business",
    "Music",
    "Art & Design",
    "News & Commentary",
    "Other",
)

private val creatorPlatforms = listOf(
    "YouTube",
    "Instagram",
    "X",
    "Facebook",
    "LinkedIn",
    "Podcast",
    "Blog / Newsletter",
    "Other",
)

private val creatorGoals = listOf(
    "Publish consistently",
    "Grow an audience",
    "Improve content quality",
    "Build a creator business",
    "Launch a project",
    "Stay organized",
)

@Composable
internal fun V18CreatorOnboarding(
    profile: CreatorProfile,
    notificationsReady: Boolean,
    preciseTimingReady: Boolean,
    fullScreenReady: Boolean,
    batteryReady: Boolean,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onFinish: (CreatorProfile) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var displayName by rememberSaveable(profile.displayName) { mutableStateOf(profile.displayName) }
    var category by rememberSaveable(profile.category) { mutableStateOf(profile.category) }
    var platforms by remember(profile.platforms) { mutableStateOf(profile.platforms) }
    var goal by rememberSaveable(profile.primaryGoal) { mutableStateOf(profile.primaryGoal) }
    var weeklyTarget by rememberSaveable(profile.weeklyPublishingTarget) {
        mutableIntStateOf(profile.weeklyPublishingTarget.coerceIn(1, 14))
    }

    val canContinue = when (page) {
        0 -> displayName.isNotBlank() && category.isNotBlank()
        1 -> platforms.isNotEmpty()
        2 -> goal.isNotBlank()
        else -> true
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FRAMEBYNAVIN", color = RecRed, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black)
                    Text("Creator setup", color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Text("${page + 1}/4", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { (page + 1) / 4f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = RecRed,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(20.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (page) {
                    0 -> {
                        Icon(Icons.Outlined.Person, "Creator identity", tint = RecRed, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Make this your creator system.", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("A few choices help Today, planning and Insights speak your language instead of assuming one kind of creator.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(40) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("What should we call you?") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.height(18.dp))
                        V18OnboardingLabel("WHAT DO YOU CREATE?")
                        Spacer(Modifier.height(9.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            creatorCategories.forEach { item ->
                                FilterChip(
                                    selected = category == item,
                                    onClick = { category = item },
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    1 -> {
                        Icon(Icons.Outlined.Hub, "Creator platforms", tint = MutedGold, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Where do you publish?", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("Choose every platform you actively create for. You can change this later.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            creatorPlatforms.forEach { item ->
                                val selected = item in platforms
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        platforms = if (selected) platforms - item else platforms + item
                                    },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    2 -> {
                        Icon(Icons.Outlined.TrackChanges, "Creator goal", tint = RecRed, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("What matters most right now?", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("This becomes the default lens for planning. It is a preference, not a permanent label.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(18.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            creatorGoals.forEach { item ->
                                FilterChip(
                                    selected = goal == item,
                                    onClick = { goal = item },
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        V18OnboardingLabel("REALISTIC PUBLISHING TARGET")
                        Spacer(Modifier.height(6.dp))
                        Text("How many pieces would you like to publish in a normal week?", color = MutedText, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 5, 7).forEach { value ->
                                FilterChip(
                                    selected = weeklyTarget == value,
                                    onClick = { weeklyTarget = value },
                                    label = { Text("$value / week", fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    else -> {
                        Icon(Icons.Outlined.NotificationsActive, "Reminder setup", tint = MutedGold, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Make reminders dependable.", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("These Android permissions are optional, but enabling them gives project reminders their full reliability.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(18.dp))
                        V18OnboardingPermissionRow("Notifications", notificationsReady, onNotifications)
                        V18OnboardingPermissionRow("Exact reminder timing", preciseTimingReady, onPreciseTiming)
                        V18OnboardingPermissionRow("Full-screen alerts", fullScreenReady, onFullScreen)
                        V18OnboardingPermissionRow("Allow background reminders", batteryReady, onBattery)
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("YOUR SETUP", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(displayName.trim(), color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("$category · ${platforms.sorted().joinToString()}", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                                Text("$goal · $weeklyTarget / week", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) {
                    TextButton(onClick = { page-- }) {
                        Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("BACK", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (page < 3) {
                            page++
                        } else {
                            onFinish(
                                CreatorProfile(
                                    displayName = displayName,
                                    category = category,
                                    platforms = platforms,
                                    primaryGoal = goal,
                                    weeklyPublishingTarget = weeklyTarget,
                                ).normalized(),
                            )
                        }
                    },
                    enabled = canContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.height(50.dp),
                ) {
                    Text(if (page < 3) "CONTINUE" else "ENTER CREATOR OS", fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun V18OnboardingLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
}

@Composable
private fun V18OnboardingPermissionRow(label: String, ready: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (ready) SuccessGreen.copy(alpha = .45f) else CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                null,
                tint = if (ready) SuccessGreen else MutedText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(label, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (ready) "READY" else "SET UP", color = if (ready) SuccessGreen else RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}
''')

root = ROOT_UI.read_text()
root = replace_once(
    root,
    '                PTab.TODAY -> PTodayScreen(\n                    tasks = vm.tasks,',
    '                PTab.TODAY -> PTodayScreen(\n                    creatorName = settings.creatorProfile.safeDisplayName,\n                    tasks = vm.tasks,',
    'Today creator name',
)
root = replace_once(
    root,
    '    if (!settings.onboardingComplete) {\n        POnboarding(\n            permissions = permissions,\n            onNotifications = ::requestNotifications,\n            onPreciseTiming = ::requestPreciseTiming,\n            onFullScreen = ::requestFullScreen,\n            onBattery = ::openBatterySettings,\n            onFinish = {\n                settingsStore.setOnboardingComplete(true)\n                settings = settingsStore.snapshot()\n            },\n        )\n    }',
    '    if (!settings.onboardingComplete || !settings.creatorProfile.isComplete) {\n        V18CreatorOnboarding(\n            profile = settings.creatorProfile,\n            notificationsReady = permissions.notifications,\n            preciseTimingReady = permissions.preciseTiming,\n            fullScreenReady = permissions.fullScreen,\n            batteryReady = permissions.batteryAccess,\n            onNotifications = ::requestNotifications,\n            onPreciseTiming = ::requestPreciseTiming,\n            onFullScreen = ::requestFullScreen,\n            onBattery = ::openBatterySettings,\n            onFinish = { profile ->\n                settingsStore.setCreatorProfile(profile)\n                settingsStore.setOnboardingComplete(true)\n                settings = settingsStore.snapshot()\n            },\n        )\n    }',
    'creator onboarding call',
)
root = root.replace('PControlRow("Quick Capture", "Save an idea to Inbox in seconds"', 'PControlRow("Quick Capture", "Save an idea to Idea Vault in seconds"')

old_header = '''@Composable\ninternal fun PHomeGreetingHeader(onAdd: () -> Unit) {\n    val hour = remember { java.time.ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).hour }\n    val greeting = when (hour) {\n        in 5..11 -> "Good Morning, Navin"\n        in 12..16 -> "Good Afternoon, Navin"\n        in 17..20 -> "Good Evening, Navin"\n        else -> "Good Night, Navin"\n    }'''
new_header = '''@Composable\ninternal fun PHomeGreetingHeader(creatorName: String, onAdd: () -> Unit) {\n    val hour = remember { java.time.ZonedDateTime.now().hour }\n    val name = creatorName.trim().ifBlank { "Creator" }\n    val greeting = when (hour) {\n        in 5..11 -> "Good Morning, $name"\n        in 12..16 -> "Good Afternoon, $name"\n        in 17..20 -> "Good Evening, $name"\n        else -> "Good Night, $name"\n    }'''
root = replace_once(root, old_header, new_header, 'personalized greeting')

start = root.find('@Composable\nprivate fun POnboarding(')
end = root.find('@Composable\nprivate fun PFocusScreen', start)
if start < 0 or end < 0:
    raise SystemExit('old onboarding block not found')
root = root[:start] + root[end:]

settings_anchor = '''            PBackHeader("SETTINGS", "Keep the app working your way", onClose)\n\n            Spacer(Modifier.height(20.dp))\n            PSettingsHeading("REMINDER SETUP", "Set this once. Project creation stays clean.")'''
settings_insert = '''            PBackHeader("SETTINGS", "Keep the app working your way", onClose)\n\n            Spacer(Modifier.height(20.dp))\n            PSettingsHeading("CREATOR PROFILE", "The context used to personalize your local creator system.")\n            Spacer(Modifier.height(8.dp))\n            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {\n                Column(Modifier.padding(14.dp)) {\n                    Text(settings.creatorProfile.safeDisplayName, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)\n                    Text(\n                        listOf(settings.creatorProfile.category, settings.creatorProfile.platforms.sorted().joinToString()).filter { it.isNotBlank() }.joinToString(" · "),\n                        color = MutedText,\n                        fontSize = 9.5.sp,\n                        lineHeight = 14.sp,\n                    )\n                    Spacer(Modifier.height(4.dp))\n                    Text(\n                        "${settings.creatorProfile.primaryGoal} · ${settings.creatorProfile.weeklyPublishingTarget} / week",\n                        color = MutedGold,\n                        fontSize = 9.5.sp,\n                        fontWeight = FontWeight.Bold,\n                    )\n                    Spacer(Modifier.height(10.dp))\n                    OutlinedButton(onClick = onRunOnboarding, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, CinemaLine)) {\n                        Text("EDIT CREATOR PROFILE", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)\n                    }\n                }\n            }\n\n            Spacer(Modifier.height(22.dp))\n            PSettingsHeading("REMINDER SETUP", "Set this once. Project creation stays clean.")'''
root = replace_once(root, settings_anchor, settings_insert, 'settings creator profile')
root = replace_once(
    root,
    '                    Spacer(Modifier.height(10.dp))\n                    OutlinedButton(onClick = onRunOnboarding, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, CinemaLine)) { Text("SHOW WELCOME AGAIN", color = ProjectorIvory, fontSize = 10.sp) }',
    '',
    'remove duplicate welcome action',
)
ROOT_UI.write_text(root)

today = TODAY.read_text()
today = replace_once(
    today,
    'internal fun PTodayScreen(\n    tasks: List<CreatorTask>,',
    'internal fun PTodayScreen(\n    creatorName: String,\n    tasks: List<CreatorTask>,',
    'Today signature',
)
today = replace_once(today, '            PHomeGreetingHeader(onAdd)', '            PHomeGreetingHeader(creatorName, onAdd)', 'Today greeting call')
TODAY.write_text(today)

TEST.write_text(r'''package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorProfileAlpha9Test {
    @Test
    fun emptyProfileIsNotComplete() {
        assertFalse(CreatorProfile().isComplete)
        assertEquals("Creator", CreatorProfile().safeDisplayName)
    }

    @Test
    fun normalizationKeepsCreatorChosenContextSafeAndBounded() {
        val normalized = CreatorProfile(
            displayName = "  Maya  ",
            category = "  Gaming ",
            platforms = setOf(" YouTube ", "", "Instagram"),
            primaryGoal = "  Grow an audience  ",
            weeklyPublishingTarget = 99,
        ).normalized()

        assertEquals("Maya", normalized.displayName)
        assertEquals("Gaming", normalized.category)
        assertEquals(setOf("YouTube", "Instagram"), normalized.platforms)
        assertEquals("Grow an audience", normalized.primaryGoal)
        assertEquals(14, normalized.weeklyPublishingTarget)
    }

    @Test
    fun completeProfileWorksForAnyCreatorCategory() {
        val profile = CreatorProfile(
            displayName = "Arjun",
            category = "Education",
            platforms = setOf("YouTube", "LinkedIn"),
            primaryGoal = "Publish consistently",
            weeklyPublishingTarget = 3,
        )

        assertTrue(profile.isComplete)
        assertEquals("Arjun", profile.safeDisplayName)
    }
}
''')

print('Applied v1.8 Product Alpha9 creator identity and onboarding')
