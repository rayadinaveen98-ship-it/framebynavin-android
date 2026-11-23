#!/usr/bin/env bash
# R2 source-contract preflight ONLY. Run once at founder-approved consolidated
# integration QA gate, never automatically after every staged source edit.
# No Gradle, no APK, no network, no user data, no Supabase mutation.
set -euo pipefail

gradle=app/build.gradle.kts
cloud=app/src/main/java/com/framebynavin/app/cloud/CloudConfig.kt
app=app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt
settings=app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt
ideas=app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt
alerts=app/src/main/java/com/framebynavin/app/reminders/ReminderCheckInScreen.kt
navtest=app/src/androidTest/java/com/framebynavin/app/ui/P10JRealFirstRunNavigationUiTest.kt

grep -Fq 'if (backlotLabMode) "com.framebynavin.app.lab"' "$gradle"
grep -Fq 'buildConfigField("boolean", "BACKLOT_LAB"' "$gradle"
grep -Fq 'BuildConfig.BACKLOT_LAB' "$cloud"
grep -Fq 'vasleafrmsovdbdlrcsw.supabase.co' "$cloud"
grep -Fq 'kukkqgpzxnfanynbddiw.supabase.co' "$cloud"
grep -Fq 'P6aUnifiedFirstRunPolicy.route(' "$app"
grep -Fq 'V18CreatorOnboarding(' "$app"
grep -Fq 'personalReminderSpace by rememberSaveable { mutableStateOf(true) }' "$app"
grep -Fq 'V144GuideChoiceGate()' "$app"
grep -Fq 'P6CreatorEntryTransition(' "$app"
grep -Fq 'V167SettingsSection.HOME_PHOTOS' "$settings"
grep -Fq 'P1GalleryBackgroundLaunchCard()' "$settings"
grep -Fq 'V403VoicePreviewPlayer.play(context, it)' "$settings"
grep -Fq 'P8IdeaVaultOverview(vaultSummary)' "$ideas"
grep -Fq 'ux05-open-linked-project' "$ideas"
grep -Fq 'P10ReminderIdentityEmblem(' "$alerts"
grep -Fq 'V407ReminderCompanionMoment(' "$alerts"
grep -Fq 'smartStageSnooze: Boolean = false' "$alerts"
grep -Fq 'REMIND IN ' "$alerts"
grep -Fq 'Reminder Hub enters Personal first' "$navtest"

# The old palettes and duplicate onboarding screens must NOT sneak back.
for retired in \
 app/src/main/java/com/framebynavin/app/reminders/V391ReminderSceneEngine.kt \
 app/src/main/java/com/framebynavin/app/reminders/V394PremiereExperience.kt \
 app/src/main/java/com/framebynavin/app/reminders/V336DirectorReminderVisuals.kt \
 app/src/main/java/com/framebynavin/app/reminders/V195ReminderParticleScene.kt \
 app/src/main/java/com/framebynavin/app/ui/V393FirstRunQuickStart.kt \
 app/src/main/java/com/framebynavin/app/ui/V401WelcomeCollection.kt \
 app/src/main/java/com/framebynavin/app/ui/V401WelcomePicker.kt \
 app/src/main/java/com/framebynavin/app/reminders/V406CinematicReminderScenes.kt
do
  test ! -e "$retired" || { echo "STOP: retired file resurfaced: $retired" >&2; exit 1; }
done
! grep -Fq 'V167SettingsSection.REMINDER_SCENES' "$settings"
! grep -Fq 'V394PremiereReminderScreen(' "$alerts"
! grep -Fq 'V336DirectorSignalArt(' "$alerts"
! grep -Fq 'onReturningAccount =' "$app"
! grep -Fq 'v402PermissionGuideComplete' "$app"

echo "PASS: only static source-contract preflight; NOT a Kotlin build or runtime certification."
