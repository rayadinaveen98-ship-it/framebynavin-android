#!/usr/bin/env bash
set -euo pipefail
root=app/src/main/java/com/framebynavin/app/reminders
screen="$root/ReminderCheckInScreen.kt"
scene="$root/V195ReminderParticleScene.kt"
ambient="$root/V197AmbientGeometry.kt"
test -f "$ambient"
test -f app/src/test/java/com/framebynavin/app/reminders/V197ImmersiveParticlePolicyTest.kt
grep -Fq 'const val NORMAL_COUNT = NORMAL_COLS * NORMAL_ROWS' "$ambient"
grep -Fq 'const val LOW_POWER_COUNT = LOW_POWER_COLS * LOW_POWER_ROWS' "$ambient"
grep -Fq 'V197AmbientGeometry.build(lowPower)' "$scene"
grep -Fq 'for (dot in ambient)' "$scene"
grep -Fq 'for (dot in glyphDots)' "$scene"
grep -Fq 'drawCircle(color = shade.copy(alpha = ambientHaloAlpha)' "$scene"
grep -Fq 'glyphColor.copy(alpha = opacity.coerceIn(0f, 1f))' "$scene"
grep -Fq 'if (revealProgress < 1f)' "$scene"
grep -Fq 'if (revealed) Modifier else Modifier' "$scene"
! grep -Fq 'delay(2_100L)' "$screen"
grep -Fq 'mutableStateOf(V197ReminderRevealPolicy.startRevealed(motionEnabled))' "$screen"
grep -Fq 'V195ReminderParticleScene(' "$screen"
grep -Fq 'motionEnabled = motionEnabled' "$screen"
echo 'V195.2 full-screen ambient, symbol and manual reveal invariants passed.'
