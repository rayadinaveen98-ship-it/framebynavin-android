#!/usr/bin/env bash
set -euo pipefail
src=app/src/main/java/com/framebynavin/app/reminders
test -f "$src/V195ParticleGeometry.kt"
test -f "$src/V195ReminderParticleScene.kt"
grep -Fq 'const val DEFAULT_DOTS = 300' "$src/V195ParticleGeometry.kt"
grep -Fq 'V195ReminderParticleScene(' "$src/ReminderCheckInScreen.kt"
grep -Fq 'delay(2_100L)' "$src/ReminderCheckInScreen.kt"
grep -Fq 'ValueAnimator.areAnimatorsEnabled()' "$src/ReminderCheckInScreen.kt"
grep -Fq 'CinePulseGuide(' "$src/ReminderCheckInScreen.kt"
test -f app/src/test/java/com/framebynavin/app/reminders/V195ParticleGeometryTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/reminders/V195ReminderSceneUiTest.kt
echo 'V195 particle/scene/reveal contracts passed.'
