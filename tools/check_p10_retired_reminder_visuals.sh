#!/usr/bin/env bash
# P10E source-only certification gate. STAGED, NEVER EXECUTED until founder QA approval.
set -euo pipefail

root=app/src/main/java/com/framebynavin/app
reminders="$root/reminders"
settings="$root/ui/P1GalleryBackgroundSettings.kt"

for obsolete in \
  V391ReminderSceneEngine.kt \
  V394PremiereExperience.kt \
  V336DirectorReminderVisuals.kt \
  V195ReminderParticleScene.kt \
  V195ParticleGeometry.kt \
  V197AmbientGeometry.kt \
  V198ParticleMotion.kt \
  V202PremiumParticleMotion.kt \
  V218UnifiedWavePolicy.kt \
  V231CinematicWaveBackdrop.kt; do
  test ! -e "$reminders/$obsolete" || {
    echo "Retired visual source still present: $obsolete" >&2
    exit 1
  }
done

# Source-level ban on production imports/calls; historical Markdown and old
# release notes are intentionally excluded from executable app checks.
if grep -RInE 'V391Reminder(Scene|Backdrop|Prefs|SceneSettings)|V394Premiere|V336Director(Backdrop|SignalArt|Palette)|V195ReminderParticleScene|V231CinematicWaveBackdrop|P1PersonalGalleryEntrance|P1CreatorEntranceUnderlay' "$root" --include='*.kt'; then
  echo 'Retired reminder visuals are still referenced by active Android Kotlin source' >&2
  exit 1
fi

grep -Fq 'P10ReminderEntranceStage(' "$reminders/ReminderCheckInScreen.kt"
grep -Fq 'P10PersonalReminderExperience(' "$root/personal/PersonalAlertActivity.kt"
grep -Fq 'P10ReminderEntranceStage(' "$reminders/P1ReminderEntranceHosts.kt"
grep -Fq 'P10ReminderSceneBackground(' "$reminders/ReminderCheckInScreen.kt"
grep -Fq 'P10ReminderSceneBackground(' "$settings"
grep -Fq 'P10ReminderFamily.entries.forEach' "$settings"
grep -Fq 'P10ReminderBackgroundSource.GALLERY' "$settings"
grep -Fq 'P10ReminderVisualPolicy.startRevealed' "$reminders/ReminderCheckInScreen.kt"

# Pure source stage check only, not proof of Android compilation, compatibility,
# lock-screen security, device rendering, animation quality or reliable alarms.
echo 'P10 retired reminder visual source references: source gate satisfied'
